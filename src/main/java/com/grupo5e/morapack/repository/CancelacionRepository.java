package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Cancelacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CancelacionRepository extends JpaRepository<Cancelacion, Long> {
    
    /**
     * Busca cancelaciones por códigos IATA de origen y destino
     */
    List<Cancelacion> findByCodigoIATAOrigenAndCodigoIATADestino(String origen, String destino);
    
    /**
     * Busca cancelaciones por hora específica
     */
    List<Cancelacion> findByHoraAndMinuto(int hora, int minuto);
    
    /**
     * Busca cancelaciones en un rango de fechas
     */
    List<Cancelacion> findByFechaHoraCancelacionBetween(LocalDateTime inicio, LocalDateTime fin);
    
    /**
     * Busca cancelaciones por días cancelados
     */
    List<Cancelacion> findByDiasCanceladoLessThanEqual(int dias);
    
    /**
     * Busca si existe una cancelación para una ruta y hora específica
     */
    @Query("SELECT c FROM Cancelacion c WHERE c.codigoIATAOrigen = :origen " +
           "AND c.codigoIATADestino = :destino AND c.hora = :hora AND c.minuto = :minuto")
    Optional<Cancelacion> findByRutaYHora(@Param("origen") String origen, 
                                          @Param("destino") String destino,
                                          @Param("hora") int hora,
                                          @Param("minuto") int minuto);
    
    /**
     * Busca todas las cancelaciones que afectan a un aeropuerto (origen o destino)
     */
    @Query("SELECT c FROM Cancelacion c WHERE c.codigoIATAOrigen = :codigo OR c.codigoIATADestino = :codigo")
    List<Cancelacion> findByAeropuertoAfectado(@Param("codigo") String codigoIATA);

    /**
     * Busca cancelaciones asociadas a un vuelo específico
     */
    List<Cancelacion> findByVueloId(Integer vueloId);
}

