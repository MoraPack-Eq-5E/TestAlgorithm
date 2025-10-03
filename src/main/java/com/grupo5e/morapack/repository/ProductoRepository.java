package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
