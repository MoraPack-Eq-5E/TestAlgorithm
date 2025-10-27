package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.api.dto.BulkResponseDTO;
import com.grupo5e.morapack.api.dto.ErrorResponseDTO;
import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.enums.EstadoProducto;
import com.grupo5e.morapack.core.model.Producto;
import com.grupo5e.morapack.service.ProductoService;
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
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "API para gestión de productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @Operation(summary = "Listar todos los productos", description = "Obtiene una lista de todos los productos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de productos obtenida exitosamente")
    })
    @GetMapping
    public ResponseEntity<List<Producto>> listar() {
        return ResponseEntity.ok(productoService.listar());
    }

    @Operation(summary = "Obtener producto por ID", description = "Obtiene un producto específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerPorId(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable Long id) {
        Producto producto = productoService.buscarPorId(id);
        if (producto == null) {
            throw new ResourceNotFoundException("Producto", "id", id);
        }
        return ResponseEntity.ok(producto);
    }

    @Operation(summary = "Obtener productos por pedido", description = "Obtiene todos los productos de un pedido específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de productos del pedido")
    })
    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<List<Producto>> obtenerPorPedido(
            @Parameter(description = "ID del pedido", required = true)
            @PathVariable Long pedidoId) {
        return ResponseEntity.ok(productoService.buscarPorPedido(pedidoId));
    }

    @Operation(summary = "Obtener productos por estado", description = "Obtiene todos los productos con un estado específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de productos con el estado especificado")
    })
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Producto>> obtenerPorEstado(
            @Parameter(description = "Estado del producto", required = true)
            @PathVariable EstadoProducto estado) {
        return ResponseEntity.ok(productoService.buscarPorEstado(estado));
    }

    @Operation(summary = "Crear nuevo producto", description = "Registra un nuevo producto en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Producto creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<Long> crear(@Valid @RequestBody Producto producto) {
        Long id = productoService.insertar(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @Operation(summary = "Actualizar producto", description = "Actualiza los datos de un producto existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Producto actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable Long id,
            @Valid @RequestBody Producto producto) {
        Producto actualizado = productoService.actualizar(id, producto);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Eliminar producto", description = "Elimina un producto del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Producto eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crear productos en bulk", description = "Registra múltiples productos en una sola operación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Productos creados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en validación de datos")
    })
    @PostMapping("/bulk")
    public ResponseEntity<BulkResponseDTO<Long>> crearBulk(@Valid @RequestBody List<Producto> productos) {
        List<Producto> creados = productoService.insertarBulk(productos);
        List<Long> ids = creados.stream().map(Producto::getId).collect(Collectors.toList());
        
        BulkResponseDTO<Long> response = BulkResponseDTO.<Long>builder()
                .totalProcesados(productos.size())
                .exitosos(ids.size())
                .fallidos(0)
                .idsExitosos(ids)
                .mensaje("Productos creados exitosamente")
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

