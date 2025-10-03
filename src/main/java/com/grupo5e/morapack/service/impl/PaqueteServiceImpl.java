package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.repository.PaqueteRepository;
import com.grupo5e.morapack.service.PaqueteService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaqueteServiceImpl implements PaqueteService {

    private final PaqueteRepository paqueteRepository;

    public PaqueteServiceImpl(PaqueteRepository paqueteRepository) {
        this.paqueteRepository = paqueteRepository;
    }

    @Override
    public List<Paquete> listar() {
        return paqueteRepository.findAll();
    }

    @Override
    public Long insertar(Paquete paquete) {
        return paqueteRepository.save(paquete).getId();
    }

    @Override
    public Paquete buscarPorId(Long id) {
        return paqueteRepository.findById(id).orElse(null);
    }

    @Override
    public void eliminar(Long id) {
        paqueteRepository.deleteById(id);
    }
}
