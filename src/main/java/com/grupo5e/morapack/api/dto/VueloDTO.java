package com.grupo5e.morapack.api.dto;

import com.grupo5e.morapack.core.enums.EstadoVuelo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para representar un vuelo")
public class VueloDTO {

    @Schema(description = "ID único del vuelo", example = "1")
    private Integer id;

    @NotNull(message = "El ID del aeropuerto origen es obligatorio")
    @Schema(description = "ID del aeropuerto de origen", example = "1", required = true)
    private Long aeropuertoOrigenId;

    @Schema(description = "Código IATA del aeropuerto origen", example = "SKBO")
    private String aeropuertoOrigenCodigo;

    @NotNull(message = "El ID del aeropuerto destino es obligatorio")
    @Schema(description = "ID del aeropuerto de destino", example = "2", required = true)
    private Long aeropuertoDestinoId;

    @Schema(description = "Código IATA del aeropuerto destino", example = "SEQM")
    private String aeropuertoDestinoCodigo;

    @NotNull(message = "La hora de salida es obligatoria")
    @Schema(description = "Hora de salida del vuelo", example = "08:30", required = true)
    private LocalTime horaSalida;

    @NotNull(message = "La hora de llegada es obligatoria")
    @Schema(description = "Hora de llegada del vuelo", example = "12:45", required = true)
    private LocalTime horaLlegada;

    @Min(value = 0, message = "La frecuencia no puede ser negativa")
    @Schema(description = "Frecuencia de vuelos por día", example = "2.0")
    private Double frecuenciaPorDia;

    @Min(value = 1, message = "La capacidad máxima debe ser al menos 1")
    @NotNull(message = "La capacidad máxima es obligatoria")
    @Schema(description = "Capacidad máxima del vuelo", example = "300", required = true)
    private Integer capacidadMaxima;

    @Min(value = 0, message = "La capacidad usada no puede ser negativa")
    @Schema(description = "Capacidad actualmente utilizada", example = "150")
    private Integer capacidadUsada;

    @Min(value = 0, message = "El tiempo de transporte no puede ser negativo")
    @Schema(description = "Tiempo de transporte en horas", example = "4.25")
    private Double tiempoTransporte;

    @Min(value = 0, message = "El costo no puede ser negativo")
    @Schema(description = "Costo del vuelo", example = "1500.50")
    private Double costo;

    @Schema(description = "Latitud actual del vuelo", example = "4.7110")
    private String latitudActual;

    @Schema(description = "Longitud actual del vuelo", example = "-74.0721")
    private String longitudActual;

    @NotNull(message = "El estado del vuelo es obligatorio")
    @Schema(description = "Estado actual del vuelo", example = "CONFIRMADO", required = true)
    private EstadoVuelo estado;

    @Schema(description = "ID de la ruta asignada", example = "1")
    private Integer rutaAsignadaId;

    @Schema(description = "Identificador único del vuelo (ORIGEN-DESTINO-HH:MM)", example = "SKBO-SEQM-08:30")
    private String identificadorVuelo;
}

