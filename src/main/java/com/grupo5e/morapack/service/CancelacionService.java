package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Cancelacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CancelacionService {
    
    List<Cancelacion> listar();
    
    Long insertar(Cancelacion cancelacion);
    
    Cancelacion actualizar(Long id, Cancelacion cancelacion);
    
    Cancelacion buscarPorId(Long id);
    
    void eliminar(Long id);
    
    List<Cancelacion> buscarPorRuta(String origen, String destino);
    
    Optional<Cancelacion> buscarPorRutaYHora(String origen, String destino, int hora, int minuto);
    
    List<Cancelacion> buscarPorAeropuerto(String codigoIATA);
    
    List<Cancelacion> buscarEnRangoFechas(LocalDateTime inicio, LocalDateTime fin);
    
    List<Cancelacion> buscarPorVuelo(Integer vueloId);
    
    List<Cancelacion> insertarBulk(List<Cancelacion> cancelaciones);
}

