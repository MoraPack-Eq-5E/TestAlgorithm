package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.enums.Rol;
import com.grupo5e.morapack.core.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    List<Empleado> findByRol(Rol rol);
    
    @Query("SELECT e FROM Empleado e WHERE LOWER(e.usernameOrEmail) = LOWER(:username)")
    Optional<Empleado> findByUsernameOrEmail(@Param("username") String username);
}
