package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Empleado;

import java.util.List;
import java.util.Optional;

public interface EmpleadoService {
    List<Empleado> listar();
    Long insertar(Empleado empleado);
    void eliminar(Long id);
}
