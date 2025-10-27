package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RutaRepository extends JpaRepository<Ruta, Integer> {
    List<Ruta> findByAeropuertoOrigenId(Long aeropuertoOrigenId);
    List<Ruta> findByAeropuertoDestinoId(Long aeropuertoDestinoId);
}
