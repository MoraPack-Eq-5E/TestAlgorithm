package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Producto;

import java.util.List;

public interface ProductoService {
    List<Producto> listar();
    Long insertar(Producto producto);
    Producto buscarPorId(Long id);
    void eliminar(Long id);
}
