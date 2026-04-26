package fes.smartown.backend.mqtt;

import fes.smartown.backend.lanterns.model.LanternMode;
import fes.smartown.backend.lanterns.service.LanternCommandPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "smartown.mqtt.enabled", havingValue = "false")
/**
 * Fallback fuer Tests oder lokale Laeufe ohne aktive MQTT-Integration.
 */
public class NoopLanternCommandPublisher implements LanternCommandPublisher {

    @Override
    /**
     * Signalisiert bewusst, dass ohne MQTT kein echter Moduswechsel versendet werden kann.
     */
    public void publishModeCommand(LanternMode mode) {
        throw new IllegalStateException("MQTT integration disabled");
    }
}
