package com.grupo5e.morapack.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request para controlar una simulación (pause/resume/stop/setSpeed)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Comando de control de simulación")
public class SimulationControlRequest {
    
    @NotBlank(message = "La acción es obligatoria")
    @Schema(description = "Acción a ejecutar", example = "pause", 
            allowableValues = {"pause", "resume", "stop", "setSpeed"})
    private String action;
    
    @Min(value = 1, message = "La velocidad debe ser al menos 1")
    @Schema(description = "Nueva velocidad (solo para action=setSpeed)", example = "112")
    private Integer newSpeed;
}

