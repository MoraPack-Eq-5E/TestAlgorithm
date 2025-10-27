package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO de respuesta para operaciones bulk")
public class BulkResponseDTO<T> {

    @Schema(description = "Número total de elementos procesados", example = "100")
    private Integer totalProcesados;

    @Schema(description = "Número de elementos exitosos", example = "95")
    private Integer exitosos;

    @Schema(description = "Número de elementos fallidos", example = "5")
    private Integer fallidos;

    @Schema(description = "Lista de IDs de elementos creados/actualizados exitosamente")
    private List<T> idsExitosos;

    @Schema(description = "Lista de errores detallados para elementos fallidos")
    private List<BulkErrorDTO> errores;

    @Schema(description = "Mensaje general de la operación", example = "Operación bulk completada")
    private String mensaje;
}

