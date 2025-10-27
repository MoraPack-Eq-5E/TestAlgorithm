package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Producto;
import com.grupo5e.morapack.core.enums.EstadoProducto;

import java.util.List;

public interface ProductoService {
    List<Producto> listar();
    Long insertar(Producto producto);
    Producto actualizar(Long id, Producto producto);
    Producto buscarPorId(Long id);
    List<Producto> buscarPorPedido(Long pedidoId);
    List<Producto> buscarPorEstado(EstadoProducto estado);
    void eliminar(Long id);
    boolean existePorId(Long id);
    List<Producto> insertarBulk(List<Producto> productos);
}
