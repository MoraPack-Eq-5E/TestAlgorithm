package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Aeropuerto;

import java.util.List;
import java.util.Optional;

public interface AeropuertoService {
    List<Aeropuerto> listar();
    Long insertar(Aeropuerto aeropuerto);
    Aeropuerto actualizar(Long id, Aeropuerto aeropuerto);
    Aeropuerto buscarPorId(Long id);
    Optional<Aeropuerto> buscarPorCodigoIATA(String codigoIATA);
    void eliminar(Long id);
    boolean existePorId(Long id);
    List<Aeropuerto> insertarBulk(List<Aeropuerto> aeropuertos);
}
