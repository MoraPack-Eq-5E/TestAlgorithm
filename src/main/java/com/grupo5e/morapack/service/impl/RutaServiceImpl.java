package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.model.Ruta;
import com.grupo5e.morapack.repository.RutaRepository;
import com.grupo5e.morapack.service.RutaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
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
    @Transactional
    public int insertar(Ruta ruta) {
        return rutaRepository.save(ruta).getId();
    }

    @Override
    @Transactional
    public Ruta actualizar(int id, Ruta ruta) {
        Ruta existente = buscarPorId((long) id);
        if (existente == null) {
            throw new ResourceNotFoundException("Ruta", "id", id);
        }
        ruta.setId(id);
        return rutaRepository.save(ruta);
    }

    @Override
    public Ruta buscarPorId(Long id) {
        return rutaRepository.findById(id.intValue()).orElse(null);
    }

    @Override
    public List<Ruta> buscarPorAeropuertoOrigen(Long aeropuertoId) {
        return rutaRepository.findByAeropuertoOrigenId(aeropuertoId);
    }

    @Override
    public List<Ruta> buscarPorAeropuertoDestino(Long aeropuertoId) {
        return rutaRepository.findByAeropuertoDestinoId(aeropuertoId);
    }

    @Override
    @Transactional
    public void eliminar(int id) {
        if (!existePorId(id)) {
            throw new ResourceNotFoundException("Ruta", "id", id);
        }
        rutaRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(int id) {
        return rutaRepository.existsById(id);
    }

    @Override
    @Transactional
    public List<Ruta> insertarBulk(List<Ruta> rutas) {
        return rutaRepository.saveAll(rutas).stream().collect(Collectors.toList());
    }
}
