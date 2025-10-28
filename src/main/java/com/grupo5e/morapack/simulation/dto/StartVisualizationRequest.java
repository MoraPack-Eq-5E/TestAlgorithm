package com.grupo5e.morapack.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 * Request para iniciar la visualización de una simulación ya completada por ALNS
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Configuración para iniciar visualización de simulación")
public class StartVisualizationRequest {
    
    @Min(value = 1, message = "El factor de aceleración debe ser al menos 1")
    @Schema(description = "Factor de aceleración del tiempo (default: 112)", example = "112")
    private Integer timeScale;
    
    @Schema(description = "Iniciar automáticamente o en pausa", example = "true")
    private Boolean autoStart;
}

