package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CiudadRepository extends JpaRepository<Ciudad, Integer> {
    Optional<Ciudad> findByCodigo(String codigo);
}
