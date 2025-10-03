package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.repository.AeropuertoRepository;
import com.grupo5e.morapack.service.AeropuertoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AeropuertoServiceImpl implements AeropuertoService {

    private final AeropuertoRepository aeropuertoRepository; //aqui esta el CRUD predefinido

    public AeropuertoServiceImpl(AeropuertoRepository aeropuertoRepository) {
        this.aeropuertoRepository = aeropuertoRepository;
    }

    @Override
    public List<Aeropuerto> listar() {
        return aeropuertoRepository.findAll();
    }

    @Override
    public Long insertar(Aeropuerto aeropuerto) {
        return aeropuertoRepository.save(aeropuerto).getId();
    }

    @Override
    public Aeropuerto buscarPorId(Long id) {
        return aeropuertoRepository.findById(id).orElse(null);
    }

    @Override
    public void eliminar(Long id) {
        aeropuertoRepository.deleteById(id);
    }
}
