package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.api.dto.BulkResponseDTO;
import com.grupo5e.morapack.api.dto.ErrorResponseDTO;
import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.service.CiudadService;
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
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ciudades")
@Tag(name = "Ciudades", description = "API para gestión de ciudades")
public class CiudadController {

    private final CiudadService ciudadService;

    public CiudadController(CiudadService ciudadService) {
        this.ciudadService = ciudadService;
    }

    @Operation(summary = "Listar todas las ciudades", description = "Obtiene una lista de todas las ciudades")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de ciudades obtenida exitosamente")
    })
    @GetMapping
    public ResponseEntity<List<Ciudad>> listar() {
        return ResponseEntity.ok(ciudadService.listar());
    }

    @Operation(summary = "Obtener ciudad por ID", description = "Obtiene una ciudad específica por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ciudad encontrada"),
            @ApiResponse(responseCode = "404", description = "Ciudad no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Ciudad> obtenerPorId(
            @Parameter(description = "ID de la ciudad", required = true)
            @PathVariable Long id) {
        Ciudad ciudad = ciudadService.buscarPorId(id);
        if (ciudad == null) {
            throw new ResourceNotFoundException("Ciudad", "id", id);
        }
        return ResponseEntity.ok(ciudad);
    }

    @Operation(summary = "Buscar ciudad por código", description = "Busca una ciudad por su código único")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ciudad encontrada"),
            @ApiResponse(responseCode = "404", description = "Ciudad no encontrada")
    })
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Ciudad> buscarPorCodigo(
            @Parameter(description = "Código de la ciudad (4 letras)", required = true)
            @PathVariable String codigo) {
        Optional<Ciudad> ciudad = ciudadService.buscarPorCodigo(codigo);
        return ciudad.map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Ciudad", "codigo", codigo));
    }

    @Operation(summary = "Crear nueva ciudad", description = "Registra una nueva ciudad en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ciudad creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<Long> crear(@Valid @RequestBody Ciudad ciudad) {
        Long id = ciudadService.insertar(ciudad);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @Operation(summary = "Actualizar ciudad", description = "Actualiza los datos de una ciudad existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ciudad actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Ciudad no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Ciudad> actualizar(
            @Parameter(description = "ID de la ciudad", required = true)
            @PathVariable Long id,
            @Valid @RequestBody Ciudad ciudad) {
        Ciudad actualizada = ciudadService.actualizar(id, ciudad);
        return ResponseEntity.ok(actualizada);
    }

    @Operation(summary = "Eliminar ciudad", description = "Elimina una ciudad del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ciudad eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Ciudad no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la ciudad", required = true)
            @PathVariable Long id) {
        ciudadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crear ciudades en bulk", description = "Registra múltiples ciudades en una sola operación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ciudades creadas exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en validación de datos")
    })
    @PostMapping("/bulk")
    public ResponseEntity<BulkResponseDTO<Integer>> crearBulk(@Valid @RequestBody List<Ciudad> ciudades) {
        List<Ciudad> creadas = ciudadService.insertarBulk(ciudades);
        List<Integer> ids = creadas.stream().map(Ciudad::getId).collect(Collectors.toList());
        
        BulkResponseDTO<Integer> response = BulkResponseDTO.<Integer>builder()
                .totalProcesados(ciudades.size())
                .exitosos(ids.size())
                .fallidos(0)
                .idsExitosos(ids)
                .mensaje("Ciudades creadas exitosamente")
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

