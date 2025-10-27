package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.api.dto.BulkResponseDTO;
import com.grupo5e.morapack.api.dto.ErrorResponseDTO;
import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.model.Cliente;
import com.grupo5e.morapack.service.ClienteService;
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
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "API para gestión de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @Operation(summary = "Listar todos los clientes", description = "Obtiene una lista de todos los clientes registrados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de clientes obtenida exitosamente")
    })
    @GetMapping
    public ResponseEntity<List<Cliente>> listar() {
        return ResponseEntity.ok(clienteService.listar());
    }

    @Operation(summary = "Obtener cliente por ID", description = "Obtiene un cliente específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Cliente> obtenerPorId(
            @Parameter(description = "ID del cliente", required = true)
            @PathVariable Long id) {
        Cliente cliente = clienteService.buscarPorId(id);
        if (cliente == null) {
            throw new ResourceNotFoundException("Cliente", "id", id);
        }
        return ResponseEntity.ok(cliente);
    }

    @Operation(summary = "Buscar cliente por número de documento", description = "Busca un cliente por su número de documento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/documento/{numeroDocumento}")
    public ResponseEntity<Cliente> buscarPorDocumento(
            @Parameter(description = "Número de documento del cliente", required = true)
            @PathVariable String numeroDocumento) {
        Optional<Cliente> cliente = clienteService.findByNumeroDocumento(numeroDocumento);
        return cliente.map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "numeroDocumento", numeroDocumento));
    }

    @Operation(summary = "Buscar cliente por correo", description = "Busca un cliente por su correo electrónico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/correo/{correo}")
    public ResponseEntity<Cliente> buscarPorCorreo(
            @Parameter(description = "Correo electrónico del cliente", required = true)
            @PathVariable String correo) {
        Optional<Cliente> cliente = clienteService.findByCorreo(correo);
        return cliente.map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "correo", correo));
    }

    @Operation(summary = "Crear nuevo cliente", description = "Registra un nuevo cliente en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<Long> crear(@Valid @RequestBody Cliente cliente) {
        Long id = clienteService.insertar(cliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @Operation(summary = "Actualizar cliente", description = "Actualiza los datos de un cliente existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizar(
            @Parameter(description = "ID del cliente", required = true)
            @PathVariable Long id,
            @Valid @RequestBody Cliente cliente) {
        Cliente actualizado = clienteService.actualizar(id, cliente);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Eliminar cliente", description = "Elimina un cliente del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cliente eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del cliente", required = true)
            @PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crear clientes en bulk", description = "Registra múltiples clientes en una sola operación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Clientes creados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en validación de datos")
    })
    @PostMapping("/bulk")
    public ResponseEntity<BulkResponseDTO<Long>> crearBulk(@Valid @RequestBody List<Cliente> clientes) {
        List<Cliente> creados = clienteService.insertarBulk(clientes);
        List<Long> ids = creados.stream().map(Cliente::getId).collect(Collectors.toList());
        
        BulkResponseDTO<Long> response = BulkResponseDTO.<Long>builder()
                .totalProcesados(clientes.size())
                .exitosos(ids.size())
                .fallidos(0)
                .idsExitosos(ids)
                .mensaje("Clientes creados exitosamente")
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

