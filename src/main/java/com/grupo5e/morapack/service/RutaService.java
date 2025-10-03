package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Ruta;

import java.util.List;

public interface RutaService {
    List<Ruta> listar();
    int insertar(Ruta ruta);
    Ruta buscarPorId(Long id);
    void eliminar(Ruta ruta);
}
