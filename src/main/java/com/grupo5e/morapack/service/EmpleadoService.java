package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Empleado;
import com.grupo5e.morapack.core.enums.Rol;

import java.util.List;
import java.util.Optional;

public interface EmpleadoService {
    List<Empleado> listar();
    Long insertar(Empleado empleado);
    Empleado actualizar(Long id, Empleado empleado);
    Empleado buscarPorId(Long id);
    List<Empleado> buscarPorRol(Rol rol);
    Optional<Empleado> buscarPorUsername(String username);
    void eliminar(Long id);
    boolean existePorId(Long id);
    List<Empleado> insertarBulk(List<Empleado> empleados);
}
