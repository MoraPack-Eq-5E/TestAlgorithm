package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
}
