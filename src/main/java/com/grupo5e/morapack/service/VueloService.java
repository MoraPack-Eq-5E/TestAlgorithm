package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Vuelo;

import java.util.List;

public interface VueloService {
    List<Vuelo> listar();
    int insertar(Vuelo vuelo);
    Vuelo buscarPorId(Long id);
    void eliminar(Vuelo vuelo);
}
