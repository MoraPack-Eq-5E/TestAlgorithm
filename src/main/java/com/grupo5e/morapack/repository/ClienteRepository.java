package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
