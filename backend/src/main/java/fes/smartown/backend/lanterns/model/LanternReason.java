package fes.smartown.backend.lanterns.model;

/**
 * Fachliche Gruende fuer einen Lichtzustandswechsel.
 */
public enum LanternReason {
    LOW_LUX,
    HIGH_LUX,
    MANUAL_OVERRIDE,
    SYSTEM_START
}
