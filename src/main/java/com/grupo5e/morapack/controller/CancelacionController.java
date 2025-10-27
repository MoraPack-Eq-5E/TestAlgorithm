package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.api.dto.BulkResponseDTO;
import com.grupo5e.morapack.api.dto.ErrorResponseDTO;
import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.model.Cancelacion;
import com.grupo5e.morapack.service.CancelacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cancelaciones")
@Tag(name = "Cancelaciones", description = "API para gestión de cancelaciones de vuelos")
public class CancelacionController {

    private final CancelacionService cancelacionService;

    public CancelacionController(CancelacionService cancelacionService) {
        this.cancelacionService = cancelacionService;
    }

    @Operation(summary = "Listar todas las cancelaciones", description = "Obtiene una lista de todas las cancelaciones")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de cancelaciones obtenida exitosamente")
    })
    @GetMapping
    public ResponseEntity<List<Cancelacion>> listar() {
        return ResponseEntity.ok(cancelacionService.listar());
    }

    @Operation(summary = "Obtener cancelación por ID", description = "Obtiene una cancelación específica por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cancelación encontrada"),
            @ApiResponse(responseCode = "404", description = "Cancelación no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Cancelacion> obtenerPorId(
            @Parameter(description = "ID de la cancelación", required = true)
            @PathVariable Long id) {
        Cancelacion cancelacion = cancelacionService.buscarPorId(id);
        if (cancelacion == null) {
            throw new ResourceNotFoundException("Cancelacion", "id", id);
        }
        return ResponseEntity.ok(cancelacion);
    }

    @Operation(summary = "Buscar cancelaciones por ruta", description = "Obtiene todas las cancelaciones para una ruta específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cancelaciones encontradas")
    })
    @GetMapping("/ruta")
    public ResponseEntity<List<Cancelacion>> buscarPorRuta(
            @Parameter(description = "Código IATA del aeropuerto origen", required = true)
            @RequestParam String origen,
            @Parameter(description = "Código IATA del aeropuerto destino", required = true)
            @RequestParam String destino) {
        return ResponseEntity.ok(cancelacionService.buscarPorRuta(origen, destino));
    }

    @Operation(summary = "Buscar cancelación por ruta y hora", description = "Busca una cancelación específica por ruta y hora")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cancelación encontrada"),
            @ApiResponse(responseCode = "404", description = "Cancelación no encontrada")
    })
    @GetMapping("/ruta-hora")
    public ResponseEntity<Cancelacion> buscarPorRutaYHora(
            @Parameter(description = "Código IATA del aeropuerto origen", required = true)
            @RequestParam String origen,
            @Parameter(description = "Código IATA del aeropuerto destino", required = true)
            @RequestParam String destino,
            @Parameter(description = "Hora de la cancelación (0-23)", required = true)
            @RequestParam int hora,
            @Parameter(description = "Minuto de la cancelación (0-59)", required = true)
            @RequestParam int minuto) {
        Optional<Cancelacion> cancelacion = cancelacionService.buscarPorRutaYHora(origen, destino, hora, minuto);
        return cancelacion.map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró cancelación para la ruta y hora especificadas"));
    }

    @Operation(summary = "Obtener cancelaciones por aeropuerto", description = "Obtiene todas las cancelaciones que afectan a un aeropuerto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cancelaciones encontradas")
    })
    @GetMapping("/aeropuerto/{codigoIATA}")
    public ResponseEntity<List<Cancelacion>> obtenerPorAeropuerto(
            @Parameter(description = "Código IATA del aeropuerto", required = true)
            @PathVariable String codigoIATA) {
        return ResponseEntity.ok(cancelacionService.buscarPorAeropuerto(codigoIATA));
    }

    @Operation(summary = "Obtener cancelaciones en rango de fechas", description = "Obtiene cancelaciones dentro de un rango de fechas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cancelaciones encontradas")
    })
    @GetMapping("/rango-fechas")
    public ResponseEntity<List<Cancelacion>> obtenerEnRangoFechas(
            @Parameter(description = "Fecha de inicio (formato: yyyy-MM-dd'T'HH:mm:ss)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @Parameter(description = "Fecha de fin (formato: yyyy-MM-dd'T'HH:mm:ss)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(cancelacionService.buscarEnRangoFechas(inicio, fin));
    }

    @Operation(summary = "Obtener cancelaciones por vuelo", description = "Obtiene todas las cancelaciones asociadas a un vuelo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cancelaciones encontradas")
    })
    @GetMapping("/vuelo/{vueloId}")
    public ResponseEntity<List<Cancelacion>> obtenerPorVuelo(
            @Parameter(description = "ID del vuelo", required = true)
            @PathVariable Integer vueloId) {
        return ResponseEntity.ok(cancelacionService.buscarPorVuelo(vueloId));
    }

    @Operation(summary = "Registrar nueva cancelación", description = "Registra una nueva cancelación de vuelo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cancelación registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<Long> crear(@Valid @RequestBody Cancelacion cancelacion) {
        Long id = cancelacionService.insertar(cancelacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @Operation(summary = "Actualizar cancelación", description = "Actualiza los datos de una cancelación existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cancelación actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cancelación no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Cancelacion> actualizar(
            @Parameter(description = "ID de la cancelación", required = true)
            @PathVariable Long id,
            @Valid @RequestBody Cancelacion cancelacion) {
        Cancelacion actualizada = cancelacionService.actualizar(id, cancelacion);
        return ResponseEntity.ok(actualizada);
    }

    @Operation(summary = "Eliminar cancelación", description = "Elimina una cancelación del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cancelación eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cancelación no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la cancelación", required = true)
            @PathVariable Long id) {
        cancelacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Registrar cancelaciones en bulk", description = "Registra múltiples cancelaciones en una sola operación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cancelaciones registradas exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en validación de datos")
    })
    @PostMapping("/bulk")
    public ResponseEntity<BulkResponseDTO<Long>> crearBulk(@Valid @RequestBody List<Cancelacion> cancelaciones) {
        List<Cancelacion> creadas = cancelacionService.insertarBulk(cancelaciones);
        List<Long> ids = creadas.stream().map(Cancelacion::getId).collect(Collectors.toList());
        
        BulkResponseDTO<Long> response = BulkResponseDTO.<Long>builder()
                .totalProcesados(cancelaciones.size())
                .exitosos(ids.size())
                .fallidos(0)
                .idsExitosos(ids)
                .mensaje("Cancelaciones registradas exitosamente")
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

