package fes.smartown.backend.lanterns.model;

/**
 * MQTT-State-Payload mit allen Werten, die das Frontend anzeigen soll.
 */
public record LanternStatePayload(
        LanternMode mode,
        LightState lightState,
        Double lux,
        boolean online,
        Double thresholdLux
) {
}
