package fes.smartown.backend.lanterns.model;

import jakarta.validation.constraints.NotNull;

/**
 * REST-Request fuer einen expliziten Moduswechsel.
 */
public record LanternModeRequest(
        @NotNull LanternMode mode
) {
}
