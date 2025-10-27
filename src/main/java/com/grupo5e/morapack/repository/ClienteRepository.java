package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);
    Optional<Cliente> findByCorreo(String correo);
}
