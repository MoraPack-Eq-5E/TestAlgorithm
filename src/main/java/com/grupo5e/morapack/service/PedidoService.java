package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.enums.EstadoPedido;

import java.util.List;

public interface PedidoService {
    List<Pedido> listar();
    Long insertar(Pedido pedido);
    Pedido actualizar(Long id, Pedido pedido);
    Pedido buscarPorId(Long id);
    List<Pedido> buscarPorCliente(Long clienteId);
    List<Pedido> buscarPorEstado(EstadoPedido estado);
    Pedido actualizarEstado(Long id, EstadoPedido nuevoEstado);
    void eliminar(Long id);
    boolean existePorId(Long id);
    List<Pedido> insertarBulk(List<Pedido> pedidos);
}
