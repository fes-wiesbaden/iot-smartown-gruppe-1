package fes.smartown.backend.lanterns.model;

/**
 * MQTT-Command vom Backend an die Firmware.
 */
public record LanternCommandPayload(
        String action,
        LanternMode mode
) {
}
