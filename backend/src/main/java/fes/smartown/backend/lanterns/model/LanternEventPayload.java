package fes.smartown.backend.lanterns.model;

/**
 * MQTT-Event vom ESP32 an Backend und Frontend.
 */
public record LanternEventPayload(
        String type,
        LightState lightState,
        LanternReason reason
) {
}
