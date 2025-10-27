package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.enums.EstadoVuelo;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.repository.VueloRepository;
import com.grupo5e.morapack.service.VueloService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
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
    @Transactional
    public int insertar(Vuelo vuelo) {
        return vueloRepository.save(vuelo).getId();
    }

    @Override
    @Transactional
    public Vuelo actualizar(int id, Vuelo vuelo) {
        Vuelo existente = buscarPorId((long) id);
        if (existente == null) {
            throw new ResourceNotFoundException("Vuelo", "id", id);
        }
        vuelo.setId(id);
        return vueloRepository.save(vuelo);
    }

    @Override
    public Vuelo buscarPorId(Long id) {
        return vueloRepository.findById(id.intValue()).orElse(null);
    }

    @Override
    public List<Vuelo> buscarPorRuta(Long origenId, Long destinoId) {
        return vueloRepository.findByAeropuertoOrigenIdAndAeropuertoDestinoId(origenId, destinoId);
    }

    @Override
    public List<Vuelo> buscarPorEstado(EstadoVuelo estado) {
        return vueloRepository.findByEstado(estado);
    }

    @Override
    public List<Vuelo> buscarDisponibles(int capacidadMinima) {
        return vueloRepository.findByCapacidadMaximaGreaterThanEqual(capacidadMinima);
    }

    @Override
    public Optional<Vuelo> buscarPorIdentificador(String identificador) {
        // Implementación básica, puede mejorarse con query personalizada
        return vueloRepository.findAll().stream()
                .filter(v -> identificador.equals(v.getIdentificadorVuelo()))
                .findFirst();
    }

    @Override
    @Transactional
    public void eliminar(int id) {
        if (!existePorId(id)) {
            throw new ResourceNotFoundException("Vuelo", "id", id);
        }
        vueloRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(int id) {
        return vueloRepository.existsById(id);
    }

    @Override
    @Transactional
    public List<Vuelo> insertarBulk(List<Vuelo> vuelos) {
        return vueloRepository.saveAll(vuelos).stream().collect(Collectors.toList());
    }
}
