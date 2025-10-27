package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Ruta;

import java.util.List;

public interface RutaService {
    List<Ruta> listar();
    int insertar(Ruta ruta);
    Ruta actualizar(int id, Ruta ruta);
    Ruta buscarPorId(Long id);
    List<Ruta> buscarPorAeropuertoOrigen(Long aeropuertoId);
    List<Ruta> buscarPorAeropuertoDestino(Long aeropuertoId);
    void eliminar(int id);
    boolean existePorId(int id);
    List<Ruta> insertarBulk(List<Ruta> rutas);
}
