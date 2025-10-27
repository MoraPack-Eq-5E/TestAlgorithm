package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.api.dto.BulkRequestDTO;
import com.grupo5e.morapack.api.dto.BulkResponseDTO;
import com.grupo5e.morapack.api.dto.ErrorResponseDTO;
import com.grupo5e.morapack.api.dto.PedidoDTO;
import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.enums.EstadoPedido;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.service.PedidoService;
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
@RequestMapping("/api/pedidos")
@Tag(name = "Pedidos", description = "API para gestión de pedidos/paquetes")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @Operation(summary = "Listar todos los pedidos", description = "Obtiene una lista de todos los pedidos en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class)))
    })
    @GetMapping
    public ResponseEntity<List<Pedido>> listar() {
        return ResponseEntity.ok(pedidoService.listar());
    }

    @Operation(summary = "Obtener pedido por ID", description = "Obtiene un pedido específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class))),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtenerPorId(
            @Parameter(description = "ID del pedido", required = true)
            @PathVariable Long id) {
        Pedido pedido = pedidoService.buscarPorId(id);
        if (pedido == null) {
            throw new ResourceNotFoundException("Pedido", "id", id);
        }
        return ResponseEntity.ok(pedido);
    }

    @Operation(summary = "Crear nuevo pedido", description = "Crea un nuevo pedido en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedido creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<Long> crear(@Valid @RequestBody Pedido pedido) {
        Long id = pedidoService.insertar(pedido);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @Operation(summary = "Actualizar pedido", description = "Actualiza un pedido existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pedido.class))),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<Pedido> actualizar(
            @Parameter(description = "ID del pedido", required = true)
            @PathVariable Long id,
            @Valid @RequestBody Pedido pedido) {
        Pedido actualizado = pedidoService.actualizar(id, pedido);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Eliminar pedido", description = "Elimina un pedido del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Pedido eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del pedido", required = true)
            @PathVariable Long id) {
        pedidoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener pedidos por cliente", description = "Obtiene todos los pedidos de un cliente específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos del cliente obtenida exitosamente")
    })
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Pedido>> obtenerPorCliente(
            @Parameter(description = "ID del cliente", required = true)
            @PathVariable Long clienteId) {
        return ResponseEntity.ok(pedidoService.buscarPorCliente(clienteId));
    }

    @Operation(summary = "Obtener pedidos por estado", description = "Obtiene todos los pedidos con un estado específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos con el estado especificado")
    })
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Pedido>> obtenerPorEstado(
            @Parameter(description = "Estado del pedido", required = true)
            @PathVariable EstadoPedido estado) {
        return ResponseEntity.ok(pedidoService.buscarPorEstado(estado));
    }

    @Operation(summary = "Actualizar estado de pedido", description = "Actualiza únicamente el estado de un pedido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado del pedido actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Pedido> actualizarEstado(
            @Parameter(description = "ID del pedido", required = true)
            @PathVariable Long id,
            @Parameter(description = "Nuevo estado del pedido", required = true)
            @RequestParam EstadoPedido estado) {
        Pedido actualizado = pedidoService.actualizarEstado(id, estado);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Crear pedidos en bulk", description = "Crea múltiples pedidos en una sola operación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedidos creados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en la validación de datos")
    })
    @PostMapping("/bulk")
    public ResponseEntity<BulkResponseDTO<Long>> crearBulk(@Valid @RequestBody List<Pedido> pedidos) {
        List<Pedido> creados = pedidoService.insertarBulk(pedidos);
        List<Long> ids = creados.stream().map(Pedido::getId).collect(Collectors.toList());
        
        BulkResponseDTO<Long> response = BulkResponseDTO.<Long>builder()
                .totalProcesados(pedidos.size())
                .exitosos(ids.size())
                .fallidos(0)
                .idsExitosos(ids)
                .mensaje("Pedidos creados exitosamente")
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

