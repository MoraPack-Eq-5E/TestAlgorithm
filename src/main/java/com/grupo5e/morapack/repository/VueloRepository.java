package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.enums.EstadoVuelo;
import com.grupo5e.morapack.core.model.Vuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VueloRepository extends JpaRepository<Vuelo, Integer> {
    List<Vuelo> findByAeropuertoOrigenIdAndAeropuertoDestinoId(Long origenId, Long destinoId);
    List<Vuelo> findByEstado(EstadoVuelo estado);
    List<Vuelo> findByCapacidadMaximaGreaterThanEqual(int capacidad);
}
