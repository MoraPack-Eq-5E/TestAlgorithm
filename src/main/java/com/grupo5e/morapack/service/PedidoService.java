package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Pedido;

import java.util.List;

public interface PedidoService {
    List<Pedido> listar();
    Long insertar(Pedido pedido);
    Pedido buscarPorId(Long id);
    void eliminar(Long id);
}
