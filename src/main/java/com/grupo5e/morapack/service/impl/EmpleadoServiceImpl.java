package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.enums.Rol;
import com.grupo5e.morapack.core.model.Empleado;
import com.grupo5e.morapack.repository.EmpleadoRepository;
import com.grupo5e.morapack.service.EmpleadoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmpleadoServiceImpl implements EmpleadoService {

    private final EmpleadoRepository empleadoRepository;

    public EmpleadoServiceImpl(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    @Override
    public List<Empleado> listar() {
        return empleadoRepository.findAll();
    }

    @Override
    @Transactional
    public Long insertar(Empleado empleado) {
        return empleadoRepository.save(empleado).getId();
    }

    @Override
    @Transactional
    public Empleado actualizar(Long id, Empleado empleado) {
        Empleado existente = buscarPorId(id);
        if (existente == null) {
            throw new ResourceNotFoundException("Empleado", "id", id);
        }
        empleado.setId(id);
        return empleadoRepository.save(empleado);
    }

    @Override
    public Empleado buscarPorId(Long id) {
        return empleadoRepository.findById(id).orElse(null);
    }

    @Override
    public List<Empleado> buscarPorRol(Rol rol) {
        return empleadoRepository.findByRol(rol);
    }

    @Override
    public Optional<Empleado> buscarPorUsername(String username) {
        return empleadoRepository.findByUsernameOrEmail(username);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!existePorId(id)) {
            throw new ResourceNotFoundException("Empleado", "id", id);
        }
        empleadoRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(Long id) {
        return empleadoRepository.existsById(id);
    }

    @Override
    @Transactional
    public List<Empleado> insertarBulk(List<Empleado> empleados) {
        return empleadoRepository.saveAll(empleados).stream().collect(Collectors.toList());
    }
}
