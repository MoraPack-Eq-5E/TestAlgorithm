package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Aeropuerto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AeropuertoRepository extends JpaRepository<Aeropuerto, Long> {
    Optional<Aeropuerto> findByCodigo(String codigo);
}
