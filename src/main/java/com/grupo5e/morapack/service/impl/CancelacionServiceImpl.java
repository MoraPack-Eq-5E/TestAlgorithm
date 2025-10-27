package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.core.model.Cancelacion;
import com.grupo5e.morapack.repository.CancelacionRepository;
import com.grupo5e.morapack.service.CancelacionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CancelacionServiceImpl implements CancelacionService {

    private final CancelacionRepository cancelacionRepository;

    public CancelacionServiceImpl(CancelacionRepository cancelacionRepository) {
        this.cancelacionRepository = cancelacionRepository;
    }

    @Override
    public List<Cancelacion> listar() {
        return cancelacionRepository.findAll();
    }

    @Override
    @Transactional
    public Long insertar(Cancelacion cancelacion) {
        return cancelacionRepository.save(cancelacion).getId();
    }

    @Override
    @Transactional
    public Cancelacion actualizar(Long id, Cancelacion cancelacion) {
        Cancelacion existente = buscarPorId(id);
        if (existente != null) {
            cancelacion.setId(id);
            return cancelacionRepository.save(cancelacion);
        }
        return null;
    }

    @Override
    public Cancelacion buscarPorId(Long id) {
        return cancelacionRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        cancelacionRepository.deleteById(id);
    }

    @Override
    public List<Cancelacion> buscarPorRuta(String origen, String destino) {
        return cancelacionRepository.findByCodigoIATAOrigenAndCodigoIATADestino(origen, destino);
    }

    @Override
    public Optional<Cancelacion> buscarPorRutaYHora(String origen, String destino, int hora, int minuto) {
        return cancelacionRepository.findByRutaYHora(origen, destino, hora, minuto);
    }

    @Override
    public List<Cancelacion> buscarPorAeropuerto(String codigoIATA) {
        return cancelacionRepository.findByAeropuertoAfectado(codigoIATA);
    }

    @Override
    public List<Cancelacion> buscarEnRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        return cancelacionRepository.findByFechaHoraCancelacionBetween(inicio, fin);
    }

    @Override
    public List<Cancelacion> buscarPorVuelo(Integer vueloId) {
        return cancelacionRepository.findByVueloId(vueloId);
    }

    @Override
    @Transactional
    public List<Cancelacion> insertarBulk(List<Cancelacion> cancelaciones) {
        return cancelacionRepository.saveAll(cancelaciones).stream().collect(Collectors.toList());
    }
}

