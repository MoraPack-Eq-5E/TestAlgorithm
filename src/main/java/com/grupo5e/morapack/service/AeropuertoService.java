package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Aeropuerto;

import java.util.List;

public interface AeropuertoService {
    List<Aeropuerto> listar();
    Long insertar(Aeropuerto aeropuerto);
    Aeropuerto buscarPorId(Long id);
    void eliminar(Long id);
}
