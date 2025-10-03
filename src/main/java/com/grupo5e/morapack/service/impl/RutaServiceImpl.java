package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.core.model.Ruta;
import com.grupo5e.morapack.repository.RutaRepository;
import com.grupo5e.morapack.service.RutaService;

import java.util.List;

public class RutaServiceImpl implements RutaService {

    private final RutaRepository rutaRepository;

    public RutaServiceImpl(RutaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
    }

    @Override
    public List<Ruta> listar() {
        return rutaRepository.findAll();
    }

    @Override
    public int insertar(Ruta ruta) {
        return rutaRepository.save(ruta).getId();
    }

    @Override
    public Ruta buscarPorId(Long id) {
        return rutaRepository.findById(id).orElse(null);
    }

    @Override
    public void eliminar(Ruta ruta) {
        rutaRepository.delete(ruta);
    }
}
