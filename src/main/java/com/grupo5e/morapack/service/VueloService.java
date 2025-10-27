package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.core.enums.EstadoVuelo;

import java.util.List;
import java.util.Optional;

public interface VueloService {
    List<Vuelo> listar();
    int insertar(Vuelo vuelo);
    Vuelo actualizar(int id, Vuelo vuelo);
    Vuelo buscarPorId(Long id);
    List<Vuelo> buscarPorRuta(Long origenId, Long destinoId);
    List<Vuelo> buscarPorEstado(EstadoVuelo estado);
    List<Vuelo> buscarDisponibles(int capacidadMinima);
    Optional<Vuelo> buscarPorIdentificador(String identificador);
    void eliminar(int id);
    boolean existePorId(int id);
    List<Vuelo> insertarBulk(List<Vuelo> vuelos);
}
