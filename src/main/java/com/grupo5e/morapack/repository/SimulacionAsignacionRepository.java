package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.SimulacionAsignacion;
import com.grupo5e.morapack.core.model.SimulacionSemanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulacionAsignacionRepository extends JpaRepository<SimulacionAsignacion, Long> {
    
    List<SimulacionAsignacion> findBySimulacion(SimulacionSemanal simulacion);
    
    List<SimulacionAsignacion> findBySimulacionOrderByPedidoIdAscSecuenciaAsc(SimulacionSemanal simulacion);
    
    List<SimulacionAsignacion> findByPedido(Pedido pedido);
    
    @Query("SELECT sa FROM SimulacionAsignacion sa WHERE sa.simulacion.id = :simulacionId " +
           "AND sa.minutoInicio <= :minuto AND sa.minutoFin >= :minuto")
    List<SimulacionAsignacion> findAsignacionesActivasEnMinuto(
            @Param("simulacionId") Long simulacionId, 
            @Param("minuto") Integer minuto);
    
    void deleteBySimulacion(SimulacionSemanal simulacion);
}

