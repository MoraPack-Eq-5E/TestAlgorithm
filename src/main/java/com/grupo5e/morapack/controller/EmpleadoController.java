package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.api.dto.BulkResponseDTO;
import com.grupo5e.morapack.api.dto.ErrorResponseDTO;
import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.enums.Rol;
import com.grupo5e.morapack.core.model.Empleado;
import com.grupo5e.morapack.service.EmpleadoService;
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
@RequestMapping("/api/empleados")
@Tag(name = "Empleados", description = "API para gestión de empleados")
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    public EmpleadoController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @Operation(summary = "Listar todos los empleados", description = "Obtiene una lista de todos los empleados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de empleados obtenida exitosamente")
    })
    @GetMapping
    public ResponseEntity<List<Empleado>> listar() {
        return ResponseEntity.ok(empleadoService.listar());
    }

    @Operation(summary = "Obtener empleado por ID", description = "Obtiene un empleado específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Empleado encontrado"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Empleado> obtenerPorId(
            @Parameter(description = "ID del empleado", required = true)
            @PathVariable Long id) {
        Empleado empleado = empleadoService.buscarPorId(id);
        if (empleado == null) {
            throw new ResourceNotFoundException("Empleado", "id", id);
        }
        return ResponseEntity.ok(empleado);
    }

    @Operation(summary = "Obtener empleados por rol", description = "Obtiene todos los empleados con un rol específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de empleados con el rol especificado")
    })
    @GetMapping("/rol/{rol}")
    public ResponseEntity<List<Empleado>> obtenerPorRol(
            @Parameter(description = "Rol del empleado", required = true)
            @PathVariable Rol rol) {
        return ResponseEntity.ok(empleadoService.buscarPorRol(rol));
    }

    @Operation(summary = "Buscar empleado por username", description = "Busca un empleado por su username o email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Empleado encontrado"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado")
    })
    @GetMapping("/username/{username}")
    public ResponseEntity<Empleado> buscarPorUsername(
            @Parameter(description = "Username o email del empleado", required = true)
            @PathVariable String username) {
        Optional<Empleado> empleado = empleadoService.buscarPorUsername(username);
        return empleado.map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "username", username));
    }

    @Operation(summary = "Crear nuevo empleado", description = "Registra un nuevo empleado en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Empleado creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<Long> crear(@Valid @RequestBody Empleado empleado) {
        Long id = empleadoService.insertar(empleado);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @Operation(summary = "Actualizar empleado", description = "Actualiza los datos de un empleado existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Empleado actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Empleado> actualizar(
            @Parameter(description = "ID del empleado", required = true)
            @PathVariable Long id,
            @Valid @RequestBody Empleado empleado) {
        Empleado actualizado = empleadoService.actualizar(id, empleado);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Eliminar empleado", description = "Elimina un empleado del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Empleado eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del empleado", required = true)
            @PathVariable Long id) {
        empleadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crear empleados en bulk", description = "Registra múltiples empleados en una sola operación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Empleados creados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en validación de datos")
    })
    @PostMapping("/bulk")
    public ResponseEntity<BulkResponseDTO<Long>> crearBulk(@Valid @RequestBody List<Empleado> empleados) {
        List<Empleado> creados = empleadoService.insertarBulk(empleados);
        List<Long> ids = creados.stream().map(Empleado::getId).collect(Collectors.toList());
        
        BulkResponseDTO<Long> response = BulkResponseDTO.<Long>builder()
                .totalProcesados(empleados.size())
                .exitosos(ids.size())
                .fallidos(0)
                .idsExitosos(ids)
                .mensaje("Empleados creados exitosamente")
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

