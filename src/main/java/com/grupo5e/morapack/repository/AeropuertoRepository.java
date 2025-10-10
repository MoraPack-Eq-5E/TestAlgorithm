package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Aeropuerto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AeropuertoRepository extends JpaRepository<Aeropuerto, Long> {
    Optional<Aeropuerto> findByCodigoIATA(String codigoIATA);
}
