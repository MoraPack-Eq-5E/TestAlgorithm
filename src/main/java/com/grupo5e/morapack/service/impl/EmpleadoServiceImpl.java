package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.core.model.Cliente;
import com.grupo5e.morapack.core.model.Empleado;
import com.grupo5e.morapack.repository.EmpleadoRepository;
import com.grupo5e.morapack.service.EmpleadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmpleadoServiceImpl implements EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Override
    public List<Empleado> listar() {
        return empleadoRepository.findAll();
    }

    @Override
    public Long insertar(Empleado empleado) {
        return empleadoRepository.save(empleado).getId();
    }

    @Override
    public void eliminar(Long id) {
        empleadoRepository.deleteById(id);
    }
}
