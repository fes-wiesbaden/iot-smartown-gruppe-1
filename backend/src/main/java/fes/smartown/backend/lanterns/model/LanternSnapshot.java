package fes.smartown.backend.lanterns.model;

import java.time.Instant;

/**
 * Kombiniert aktuellen Zustand, letztes Event und Broker-Status fuer REST und WebSocket.
 */
public record LanternSnapshot(
        LanternStatePayload state,
        LanternEventPayload lastEvent,
        boolean brokerConnected,
        Instant updatedAt
) {
}
