package fes.smartown.backend.lanterns.service;

import fes.smartown.backend.lanterns.model.LanternMode;

/**
 * Abstraktion fuer das Versenden von Laternen-Kommandos an die MQTT-Schicht.
 */
public interface LanternCommandPublisher {

    /**
     * Publiziert einen gewuenschten Laternenmodus an den Broker oder einen Testadapter.
     */
    void publishModeCommand(LanternMode mode);
}
