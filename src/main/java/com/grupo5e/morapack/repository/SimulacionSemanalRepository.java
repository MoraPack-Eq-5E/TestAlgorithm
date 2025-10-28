package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.enums.EstadoSimulacion;
import com.grupo5e.morapack.core.model.SimulacionSemanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SimulacionSemanalRepository extends JpaRepository<SimulacionSemanal, Long> {
    
    List<SimulacionSemanal> findByEstado(EstadoSimulacion estado);
    
    List<SimulacionSemanal> findByOrderByFechaInicioDesc();
    
    Optional<SimulacionSemanal> findFirstByEstadoOrderByFechaInicioDesc(EstadoSimulacion estado);
}

