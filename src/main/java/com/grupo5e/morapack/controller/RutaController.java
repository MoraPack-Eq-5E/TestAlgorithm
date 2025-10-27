package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.api.dto.BulkResponseDTO;
import com.grupo5e.morapack.api.dto.ErrorResponseDTO;
import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.model.Ruta;
import com.grupo5e.morapack.service.RutaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rutas")
@Tag(name = "Rutas", description = "API para gestión de rutas de vuelo")
public class RutaController {

    private final RutaService rutaService;

    public RutaController(RutaService rutaService) {
        this.rutaService = rutaService;
    }

    @Operation(summary = "Listar todas las rutas", description = "Obtiene una lista de todas las rutas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de rutas obtenida exitosamente")
    })
    @GetMapping
    public ResponseEntity<List<Ruta>> listar() {
        return ResponseEntity.ok(rutaService.listar());
    }

    @Operation(summary = "Obtener ruta por ID", description = "Obtiene una ruta específica por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ruta encontrada"),
            @ApiResponse(responseCode = "404", description = "Ruta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Ruta> obtenerPorId(
            @Parameter(description = "ID de la ruta", required = true)
            @PathVariable Long id) {
        Ruta ruta = rutaService.buscarPorId(id);
        if (ruta == null) {
            throw new ResourceNotFoundException("Ruta", "id", id);
        }
        return ResponseEntity.ok(ruta);
    }

    @Operation(summary = "Obtener rutas por aeropuerto origen", description = "Obtiene todas las rutas que salen de un aeropuerto específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutas encontradas")
    })
    @GetMapping("/origen/{aeropuertoId}")
    public ResponseEntity<List<Ruta>> obtenerPorOrigen(
            @Parameter(description = "ID del aeropuerto origen", required = true)
            @PathVariable Long aeropuertoId) {
        return ResponseEntity.ok(rutaService.buscarPorAeropuertoOrigen(aeropuertoId));
    }

    @Operation(summary = "Obtener rutas por aeropuerto destino", description = "Obtiene todas las rutas que llegan a un aeropuerto específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutas encontradas")
    })
    @GetMapping("/destino/{aeropuertoId}")
    public ResponseEntity<List<Ruta>> obtenerPorDestino(
            @Parameter(description = "ID del aeropuerto destino", required = true)
            @PathVariable Long aeropuertoId) {
        return ResponseEntity.ok(rutaService.buscarPorAeropuertoDestino(aeropuertoId));
    }

    @Operation(summary = "Crear nueva ruta", description = "Registra una nueva ruta en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ruta creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<Integer> crear(@Valid @RequestBody Ruta ruta) {
        int id = rutaService.insertar(ruta);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @Operation(summary = "Actualizar ruta", description = "Actualiza los datos de una ruta existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ruta actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Ruta no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Ruta> actualizar(
            @Parameter(description = "ID de la ruta", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody Ruta ruta) {
        Ruta actualizada = rutaService.actualizar(id, ruta);
        return ResponseEntity.ok(actualizada);
    }

    @Operation(summary = "Eliminar ruta", description = "Elimina una ruta del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ruta eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Ruta no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la ruta", required = true)
            @PathVariable Integer id) {
        rutaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crear rutas en bulk", description = "Registra múltiples rutas en una sola operación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rutas creadas exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en validación de datos")
    })
    @PostMapping("/bulk")
    public ResponseEntity<BulkResponseDTO<Integer>> crearBulk(@Valid @RequestBody List<Ruta> rutas) {
        List<Ruta> creadas = rutaService.insertarBulk(rutas);
        List<Integer> ids = creadas.stream().map(Ruta::getId).collect(Collectors.toList());
        
        BulkResponseDTO<Integer> response = BulkResponseDTO.<Integer>builder()
                .totalProcesados(rutas.size())
                .exitosos(ids.size())
                .fallidos(0)
                .idsExitosos(ids)
                .mensaje("Rutas creadas exitosamente")
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

