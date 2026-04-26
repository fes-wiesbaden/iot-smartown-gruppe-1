package fes.smartown.backend.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fes.smartown.backend.lanterns.model.LanternCommandPayload;
import fes.smartown.backend.lanterns.model.LanternEventPayload;
import fes.smartown.backend.lanterns.model.LanternMode;
import fes.smartown.backend.lanterns.model.LanternStatePayload;
import fes.smartown.backend.lanterns.service.LanternCommandPublisher;
import fes.smartown.backend.lanterns.service.LanternStateService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(name = "smartown.mqtt.enabled", havingValue = "true", matchIfMissing = true)
/**
 * Verbindet Backend und MQTT-Broker fuer den Laternen-MVP in beide Richtungen.
 */
public class MqttLanternBridge implements LanternCommandPublisher, MqttCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttLanternBridge.class);
    private static final String STATE_TOPIC = "smartown/lanterns/state";
    private static final String EVENT_TOPIC = "smartown/lanterns/event";
    private static final String COMMAND_TOPIC = "smartown/lanterns/command";
    private static final String COMMAND_ACTION = "SET_MODE";

    private final LanternMqttProperties properties;
    private final LanternStateService lanternStateService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

    private MqttClient mqttClient;

    public MqttLanternBridge(LanternMqttProperties properties,
                             LanternStateService lanternStateService,
                             ObjectMapper objectMapper) {
        this.properties = properties;
        this.lanternStateService = lanternStateService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    /**
     * Baut direkt nach dem Start die erste Broker-Verbindung auf.
     */
    void connectOnStartup() {
        connectIfNecessary();
    }

    @PreDestroy
    /**
     * Beendet Reconnect-Tasks und trennt die MQTT-Verbindung beim Shutdown.
     */
    void shutdown() {
        reconnectExecutor.shutdownNow();
        disconnectQuietly();
    }

    @Override
    /**
     * Wandelt einen REST-Moduswechsel in ein MQTT-Command-Payload um.
     */
    public void publishModeCommand(LanternMode mode) {
        Objects.requireNonNull(mode, "mode");
        connectIfNecessary();
        if (mqttClient == null || !mqttClient.isConnected()) {
            throw new IllegalStateException("MQTT broker unavailable");
        }

        LanternCommandPayload payload = new LanternCommandPayload(COMMAND_ACTION, mode);

        try {
            mqttClient.publish(COMMAND_TOPIC, toMessage(payload));
        } catch (MqttException | JsonProcessingException exception) {
            throw new IllegalStateException("MQTT command could not be sent", exception);
        }
    }

    @Override
    /**
     * Markiert den Broker als getrennt und plant einen kontrollierten Reconnect.
     */
    public void connectionLost(Throwable cause) {
        lanternStateService.updateBrokerConnection(false);
        scheduleReconnect();
        LOGGER.warn("MQTT connection lost", cause);
    }

    @Override
    /**
     * Ordnet eingehende MQTT-Nachrichten dem passenden State- oder Event-Handler zu.
     */
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

        try {
            if (STATE_TOPIC.equals(topic)) {
                lanternStateService.handleState(objectMapper.readValue(payload, LanternStatePayload.class));
                return;
            }

            if (EVENT_TOPIC.equals(topic)) {
                lanternStateService.handleEvent(objectMapper.readValue(payload, LanternEventPayload.class));
            }
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Could not parse MQTT payload from topic {}: {}", topic, payload, exception);
        }
    }

    @Override
    /**
     * Wird von Paho nach erfolgreicher Zustellung aufgerufen. Mehr Lifecycle braucht das REST-API hier nicht.
     */
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    /**
     * Baut die Broker-Verbindung nur dann auf, wenn aktuell keine nutzbare Verbindung existiert.
     */
    private synchronized void connectIfNecessary() {
        if (isConnected()) {
            return;
        }

        try {
            if (mqttClient == null) {
                mqttClient = new MqttClient(
                        properties.getBrokerUrl(),
                        "smartown-backend-" + UUID.randomUUID(),
                        new MemoryPersistence()
                );
                mqttClient.setCallback(this);
            }

            mqttClient.connect(connectOptions());
            mqttClient.subscribe(STATE_TOPIC, 1);
            mqttClient.subscribe(EVENT_TOPIC, 1);
            lanternStateService.updateBrokerConnection(true);
            reconnectScheduled.set(false);
            LOGGER.info("Connected to MQTT broker at {}", properties.getBrokerUrl());
        } catch (MqttException exception) {
            lanternStateService.updateBrokerConnection(false);
            scheduleReconnect();
            LOGGER.warn("MQTT connect failed", exception);
        }
    }

    /**
     * Prueft kompakt, ob der interne MQTT-Client aktuell verbunden ist.
     */
    private boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    /**
     * Plant genau einen Reconnect-Versuch mit kurzem Delay, um Broker-Ausfaelle abzufedern.
     */
    private void scheduleReconnect() {
        if (!reconnectScheduled.compareAndSet(false, true)) {
            return;
        }

        reconnectExecutor.schedule(() -> {
            reconnectScheduled.set(false);
            connectIfNecessary();
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * Baut die Paho-Connect-Optionen aus den konfigurierten Zugangsdaten.
     */
    private MqttConnectOptions connectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(false);
        options.setCleanSession(true);

        if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            options.setUserName(properties.getUsername());
        }
        if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
            options.setPassword(properties.getPassword().toCharArray());
        }

        return options;
    }

    /**
     * Serialisiert ein Java-Payload in die MQTT-Nachrichtenform des Brokers.
     */
    private MqttMessage toMessage(Object payload) throws JsonProcessingException {
        MqttMessage message = new MqttMessage(objectMapper.writeValueAsBytes(payload));
        message.setQos(1);
        message.setRetained(false);
        return message;
    }

    /**
     * Trennt die Verbindung beim Shutdown ohne weitere Fehler nach oben zu reichen.
     */
    private synchronized void disconnectQuietly() {
        if (mqttClient == null) {
            return;
        }

        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            mqttClient.close();
        } catch (MqttException exception) {
            LOGGER.debug("Ignoring MQTT disconnect failure", exception);
        }
    }
}
