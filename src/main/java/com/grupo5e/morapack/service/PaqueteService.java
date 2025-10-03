package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Paquete;

import java.util.List;

public interface PaqueteService {
    List<Paquete> listar();
    Long insertar(Paquete paquete);
    Paquete buscarPorId(Long id);
    void eliminar(Long id);
}
