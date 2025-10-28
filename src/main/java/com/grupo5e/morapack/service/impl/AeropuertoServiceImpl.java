package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.enums.EstadoAeropuerto;
import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.repository.AeropuertoRepository;
import com.grupo5e.morapack.service.AeropuertoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AeropuertoServiceImpl implements AeropuertoService {

    private final AeropuertoRepository aeropuertoRepository;

    public AeropuertoServiceImpl(AeropuertoRepository aeropuertoRepository) {
        this.aeropuertoRepository = aeropuertoRepository;
    }

    @Override
    public List<Aeropuerto> listar() {
        return aeropuertoRepository.findAll();
    }

    @Override
    public List<Aeropuerto> listarDisponibles() {
        return aeropuertoRepository.findByEstado(EstadoAeropuerto.DISPONIBLE);
    }

    @Override
    @Transactional
    public Long insertar(Aeropuerto aeropuerto) {
        return aeropuertoRepository.save(aeropuerto).getId();
    }

    @Override
    @Transactional
    public Aeropuerto actualizar(Long id, Aeropuerto aeropuerto) {
        Aeropuerto existente = buscarPorId(id);
        if (existente == null) {
            throw new ResourceNotFoundException("Aeropuerto", "id", id);
        }
        aeropuerto.setId(id);
        return aeropuertoRepository.save(aeropuerto);
    }

    @Override
    @Transactional
    public Aeropuerto toggleEstado(Long id) {
        Aeropuerto aeropuerto = buscarPorId(id);
        if (aeropuerto == null) {
            throw new ResourceNotFoundException("Aeropuerto", "id", id);
        }
        
        // Cambiar estado: DISPONIBLE ↔ NO_DISPONIBLE
        if (aeropuerto.getEstado() == EstadoAeropuerto.DISPONIBLE) {
            aeropuerto.setEstado(EstadoAeropuerto.NO_DISPONIBLE);
        } else {
            aeropuerto.setEstado(EstadoAeropuerto.DISPONIBLE);
        }
        
        return aeropuertoRepository.save(aeropuerto);
    }

    @Override
    public Aeropuerto buscarPorId(Long id) {
        return aeropuertoRepository.findById(id).orElse(null);
    }

    @Override
    public Optional<Aeropuerto> buscarPorCodigoIATA(String codigoIATA) {
        return aeropuertoRepository.findByCodigoIATA(codigoIATA);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!existePorId(id)) {
            throw new ResourceNotFoundException("Aeropuerto", "id", id);
        }
        aeropuertoRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(Long id) {
        return aeropuertoRepository.existsById(id);
    }

    @Override
    @Transactional
    public List<Aeropuerto> insertarBulk(List<Aeropuerto> aeropuertos) {
        return aeropuertoRepository.saveAll(aeropuertos).stream().collect(Collectors.toList());
    }
}
