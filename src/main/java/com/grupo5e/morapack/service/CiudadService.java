package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Ciudad;

import java.util.List;
import java.util.Optional;

public interface CiudadService {
    List<Ciudad> listar();
    Long insertar(Ciudad ciudad);
    Ciudad actualizar(Long id, Ciudad ciudad);
    Ciudad buscarPorId(Long id);
    Optional<Ciudad> buscarPorCodigo(String codigo);
    void eliminar(Long id);
    boolean existePorId(Long id);
    List<Ciudad> insertarBulk(List<Ciudad> ciudades);
}
