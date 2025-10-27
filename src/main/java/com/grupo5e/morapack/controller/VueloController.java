package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.api.dto.BulkResponseDTO;
import com.grupo5e.morapack.api.dto.ErrorResponseDTO;
import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.enums.EstadoVuelo;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.service.VueloService;
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
@RequestMapping("/api/vuelos")
@Tag(name = "Vuelos", description = "API para gestión de vuelos")
public class VueloController {

    private final VueloService vueloService;

    public VueloController(VueloService vueloService) {
        this.vueloService = vueloService;
    }

    @Operation(summary = "Listar todos los vuelos", description = "Obtiene una lista de todos los vuelos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de vuelos obtenida exitosamente")
    })
    @GetMapping
    public ResponseEntity<List<Vuelo>> listar() {
        return ResponseEntity.ok(vueloService.listar());
    }

    @Operation(summary = "Obtener vuelo por ID", description = "Obtiene un vuelo específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vuelo encontrado"),
            @ApiResponse(responseCode = "404", description = "Vuelo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Vuelo> obtenerPorId(
            @Parameter(description = "ID del vuelo", required = true)
            @PathVariable Long id) {
        Vuelo vuelo = vueloService.buscarPorId(id);
        if (vuelo == null) {
            throw new ResourceNotFoundException("Vuelo", "id", id);
        }
        return ResponseEntity.ok(vuelo);
    }

    @Operation(summary = "Buscar vuelos por ruta", description = "Busca vuelos entre dos aeropuertos específicos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vuelos encontrados")
    })
    @GetMapping("/ruta")
    public ResponseEntity<List<Vuelo>> buscarPorRuta(
            @Parameter(description = "ID del aeropuerto origen", required = true)
            @RequestParam Long origenId,
            @Parameter(description = "ID del aeropuerto destino", required = true)
            @RequestParam Long destinoId) {
        return ResponseEntity.ok(vueloService.buscarPorRuta(origenId, destinoId));
    }

    @Operation(summary = "Obtener vuelos por estado", description = "Obtiene todos los vuelos con un estado específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de vuelos con el estado especificado")
    })
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Vuelo>> obtenerPorEstado(
            @Parameter(description = "Estado del vuelo", required = true)
            @PathVariable EstadoVuelo estado) {
        return ResponseEntity.ok(vueloService.buscarPorEstado(estado));
    }

    @Operation(summary = "Obtener vuelos disponibles", description = "Obtiene vuelos con capacidad disponible")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de vuelos disponibles")
    })
    @GetMapping("/disponibles")
    public ResponseEntity<List<Vuelo>> obtenerDisponibles(
            @Parameter(description = "Capacidad mínima requerida", required = false)
            @RequestParam(defaultValue = "1") int capacidadMinima) {
        return ResponseEntity.ok(vueloService.buscarDisponibles(capacidadMinima));
    }

    @Operation(summary = "Buscar vuelo por identificador", description = "Busca un vuelo por su identificador único (ORIGEN-DESTINO-HH:MM)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vuelo encontrado"),
            @ApiResponse(responseCode = "404", description = "Vuelo no encontrado")
    })
    @GetMapping("/identificador/{identificador}")
    public ResponseEntity<Vuelo> buscarPorIdentificador(
            @Parameter(description = "Identificador del vuelo (ej: SKBO-SEQM-08:30)", required = true)
            @PathVariable String identificador) {
        Optional<Vuelo> vuelo = vueloService.buscarPorIdentificador(identificador);
        return vuelo.map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Vuelo", "identificador", identificador));
    }

    @Operation(summary = "Crear nuevo vuelo", description = "Registra un nuevo vuelo en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vuelo creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<Integer> crear(@Valid @RequestBody Vuelo vuelo) {
        int id = vueloService.insertar(vuelo);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @Operation(summary = "Actualizar vuelo", description = "Actualiza los datos de un vuelo existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vuelo actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Vuelo no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Vuelo> actualizar(
            @Parameter(description = "ID del vuelo", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody Vuelo vuelo) {
        Vuelo actualizado = vueloService.actualizar(id, vuelo);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Eliminar vuelo", description = "Elimina un vuelo del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Vuelo eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Vuelo no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del vuelo", required = true)
            @PathVariable Integer id) {
        vueloService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crear vuelos en bulk", description = "Registra múltiples vuelos en una sola operación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vuelos creados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en validación de datos")
    })
    @PostMapping("/bulk")
    public ResponseEntity<BulkResponseDTO<Integer>> crearBulk(@Valid @RequestBody List<Vuelo> vuelos) {
        List<Vuelo> creados = vueloService.insertarBulk(vuelos);
        List<Integer> ids = creados.stream().map(Vuelo::getId).collect(Collectors.toList());
        
        BulkResponseDTO<Integer> response = BulkResponseDTO.<Integer>builder()
                .totalProcesados(vuelos.size())
                .exitosos(ids.size())
                .fallidos(0)
                .idsExitosos(ids)
                .mensaje("Vuelos creados exitosamente")
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

