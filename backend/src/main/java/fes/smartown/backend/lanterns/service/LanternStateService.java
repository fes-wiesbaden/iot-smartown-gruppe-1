package fes.smartown.backend.lanterns.service;

import fes.smartown.backend.lanterns.model.LanternEventPayload;
import fes.smartown.backend.lanterns.model.LanternMode;
import fes.smartown.backend.lanterns.model.LanternReason;
import fes.smartown.backend.lanterns.model.LanternSnapshot;
import fes.smartown.backend.lanterns.model.LanternStatePayload;
import fes.smartown.backend.lanterns.model.LightState;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Service
/**
 * Haltet den zuletzt bekannten MQTT-Zustand der Laternen thread-sicher im Speicher.
 */
public class LanternStateService {

    private final LanternRealtimeService lanternRealtimeService;
    private final AtomicReference<LanternSnapshot> snapshotReference = new AtomicReference<>(defaultSnapshot());

    public LanternStateService(LanternRealtimeService lanternRealtimeService) {
        this.lanternRealtimeService = lanternRealtimeService;
    }

    /**
     * Liefert jederzeit den zuletzt bekannten Snapshot fuer REST und WebSocket.
     */
    public LanternSnapshot getSnapshot() {
        return snapshotReference.get();
    }

    /**
     * Uebernimmt ein eingehendes State-Payload und erzeugt daraus einen neuen Snapshot.
     */
    public LanternSnapshot handleState(LanternStatePayload statePayload) {
        Objects.requireNonNull(statePayload, "statePayload");
        return updateSnapshot(previous -> new LanternSnapshot(
                statePayload,
                previous.lastEvent(),
                previous.brokerConnected(),
                Instant.now()
        ));
    }

    /**
     * Uebernimmt ein eingehendes Event-Payload und aktualisiert nur den Event-Teil des Snapshots.
     */
    public LanternSnapshot handleEvent(LanternEventPayload eventPayload) {
        Objects.requireNonNull(eventPayload, "eventPayload");
        return updateSnapshot(previous -> new LanternSnapshot(
                previous.state(),
                eventPayload,
                previous.brokerConnected(),
                Instant.now()
        ));
    }

    /**
     * Markiert, ob das Backend aktuell mit dem MQTT-Broker verbunden ist.
     */
    public LanternSnapshot updateBrokerConnection(boolean brokerConnected) {
        return updateSnapshot(previous -> new LanternSnapshot(
                previous.state(),
                previous.lastEvent(),
                brokerConnected,
                Instant.now()
        ));
    }

    /**
     * Aktualisiert den Snapshot atomar und pusht die Aenderung direkt an das Frontend.
     */
    private LanternSnapshot updateSnapshot(java.util.function.UnaryOperator<LanternSnapshot> updater) {
        LanternSnapshot updated = snapshotReference.updateAndGet(updater);
        lanternRealtimeService.broadcast(updated);
        return updated;
    }

    /**
     * Baut den Initialzustand fuer Start, Tests und Broker-Ausfaelle.
     */
    private static LanternSnapshot defaultSnapshot() {
        LanternStatePayload defaultState = new LanternStatePayload(
                LanternMode.AUTO,
                LightState.OFF,
                null,
                false,
                null
        );
        LanternEventPayload defaultEvent = new LanternEventPayload(
                "SYSTEM_START",
                LightState.OFF,
                LanternReason.SYSTEM_START
        );

        return new LanternSnapshot(defaultState, defaultEvent, false, Instant.now());
    }
}
