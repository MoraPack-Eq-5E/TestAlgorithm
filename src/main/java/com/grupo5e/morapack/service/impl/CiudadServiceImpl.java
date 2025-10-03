package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.repository.AeropuertoRepository;
import com.grupo5e.morapack.repository.CiudadRepository;
import com.grupo5e.morapack.service.CiudadService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CiudadServiceImpl implements CiudadService {

    private final CiudadRepository ciudadRepository; //aqui esta el CRUD predefinido

    public CiudadServiceImpl(CiudadRepository ciudadRepository) {
        this.ciudadRepository = ciudadRepository;
    }
    @Override
    public List<Ciudad> listar() {
        return ciudadRepository.findAll();
    }

    @Override
    public Long insertar(Ciudad ciudad) {
        return (long) ciudadRepository.save(ciudad).getId();
    }

    @Override
    public Ciudad buscarPorId(Long id) {
        return ciudadRepository.findById(id).orElse(null);
    }

    @Override
    public void eliminar(Long id) {
        ciudadRepository.deleteById(id);
    }
}
