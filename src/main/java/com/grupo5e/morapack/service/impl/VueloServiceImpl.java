package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.repository.VueloRepository;
import com.grupo5e.morapack.service.VueloService;

import java.util.List;

public class VueloServiceImpl implements VueloService {

    private final VueloRepository vueloRepository;

    public VueloServiceImpl(VueloRepository vueloRepository) {
        this.vueloRepository = vueloRepository;
    }

    @Override
    public List<Vuelo> listar() {
        return vueloRepository.findAll();
    }

    @Override
    public int insertar(Vuelo vuelo) {
        return vueloRepository.save(vuelo).getId();
    }

    @Override
    public Vuelo buscarPorId(Long id) {
        return vueloRepository.findById(id).orElse(null);
    }

    @Override
    public void eliminar(Vuelo vuelo) {
        vueloRepository.delete(vuelo);
    }
}
