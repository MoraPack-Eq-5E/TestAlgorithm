package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Ciudad;

import java.util.List;

public interface CiudadService {
    List<Ciudad> listar();
    Long insertar(Ciudad aeropuerto);
    Ciudad buscarPorId(Long id);
    void eliminar(Long id);
}
