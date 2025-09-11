package com.grupo5e.morapack.algorithm.validation;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.core.enums.TipoViolacion;
import com.grupo5e.morapack.core.constants.ConstantesMoraPack;
import java.util.*;

/**
 * Validador de restricciones para el problema MoraPack.
 * Verifica que las soluciones cumplan con todas las restricciones del negocio.
 */
public class ValidadorRestricciones {
    
    private final Map<String, Aeropuerto> aeropuertos;
    private final Map<String, Vuelo> vuelos;
    private final Set<Continente> continentes;
    private final Map<String, String> aeropuertoAContinente;
    
    public ValidadorRestricciones(List<Aeropuerto> listaAeropuertos, List<Vuelo> listaVuelos, 
                                  Set<Continente> continentes) {
        this.aeropuertos = new HashMap<>();
        this.vuelos = new HashMap<>();
        this.continentes = continentes;
        this.aeropuertoAContinente = new HashMap<>();
        
        // Indexar aeropuertos
        for (Aeropuerto aeropuerto : listaAeropuertos) {
            aeropuertos.put(aeropuerto.getCodigoIATA(), aeropuerto);
        }
        
        // Indexar vuelos
        for (Vuelo vuelo : listaVuelos) {
            vuelos.put(vuelo.getNumeroVuelo(), vuelo);
        }
        
        // Crear mapeo aeropuerto -> continente
        for (Continente continente : continentes) {
            for (String codigoIATA : continente.getCodigosIATAAeropuertos()) {
                aeropuertoAContinente.put(codigoIATA, continente.getCodigo());
            }
        }
    }
    
    /**
     * Valida una solución completa y actualiza sus métricas de violaciones.
     */
    public ResultadoValidacion validarSolucion(Solucion solucion) {
        ResultadoValidacion resultado = new ResultadoValidacion();
        
        for (Map.Entry<String, Ruta> entry : solucion.getRutasPaquetes().entrySet()) {
            String paqueteId = entry.getKey();
            Ruta ruta = entry.getValue();
            
            // Validar ruta individual
            ResultadoValidacion validacionRuta = validarRuta(paqueteId, ruta);
            resultado.combinar(validacionRuta);
        }
        
        // Validar restricciones globales
        validarCapacidadesGlobales(solucion, resultado);
        
        // Actualizar métricas de la solución
        solucion.setViolacionesRestricciones(resultado.getTotalViolaciones());
        solucion.setEsFactible(resultado.esFactible());
        
        return resultado;
    }
    
    /**
     * Valida una ruta individual.
     */
    public ResultadoValidacion validarRuta(String paqueteId, Ruta ruta) {
        ResultadoValidacion resultado = new ResultadoValidacion();
        
        if (ruta.getSegmentos().isEmpty()) {
            resultado.agregarViolacion("Ruta vacía para paquete " + paqueteId, TipoViolacion.RUTA_INVALIDA);
            return resultado;
        }
        
        try {
            ruta.validarRuta(); // Validación básica de conectividad
        } catch (IllegalStateException e) {
            resultado.agregarViolacion("Ruta desconectada: " + e.getMessage(), TipoViolacion.RUTA_INVALIDA);
        }
        
        // Validar cada segmento
        for (int i = 0; i < ruta.getSegmentos().size(); i++) {
            SegmentoRuta segmento = ruta.getSegmentos().get(i);
            validarSegmento(segmento, resultado, paqueteId, i);
        }
        
        // Validar plazos de entrega
        validarPlazosEntrega(paqueteId, ruta, resultado);
        
        return resultado;
    }
    
    private void validarSegmento(SegmentoRuta segmento, ResultadoValidacion resultado, 
                                String paqueteId, int numeroSegmento) {
        
        // Verificar que los aeropuertos existan
        if (!aeropuertos.containsKey(segmento.getAeropuertoOrigen())) {
            resultado.agregarViolacion(
                String.format("Aeropuerto origen %s no existe en segmento %d de paquete %s", 
                            segmento.getAeropuertoOrigen(), numeroSegmento, paqueteId),
                TipoViolacion.AEROPUERTO_INEXISTENTE
            );
        }
        
        if (!aeropuertos.containsKey(segmento.getAeropuertoDestino())) {
            resultado.agregarViolacion(
                String.format("Aeropuerto destino %s no existe en segmento %d de paquete %s", 
                            segmento.getAeropuertoDestino(), numeroSegmento, paqueteId),
                TipoViolacion.AEROPUERTO_INEXISTENTE
            );
        }
        
        // Verificar que el vuelo exista
        Vuelo vuelo = vuelos.get(segmento.getNumeroVuelo());
        if (vuelo == null) {
            resultado.agregarViolacion(
                String.format("Vuelo %s no existe en segmento %d de paquete %s", 
                            segmento.getNumeroVuelo(), numeroSegmento, paqueteId),
                TipoViolacion.VUELO_INEXISTENTE
            );
            return; // No continuar validando este segmento
        }
        
        // Verificar consistencia de aeropuertos con vuelo
        if (!vuelo.getAeropuertoOrigen().equals(segmento.getAeropuertoOrigen()) ||
            !vuelo.getAeropuertoDestino().equals(segmento.getAeropuertoDestino())) {
            resultado.agregarViolacion(
                String.format("Inconsistencia aeropuerto-vuelo en segmento %d de paquete %s", 
                            numeroSegmento, paqueteId),
                TipoViolacion.INCONSISTENCIA_VUELO
            );
        }
        
        // Verificar capacidad del vuelo (simplificado - en implementación real se haría global)
        if (!vuelo.puedeCargar(1)) {
            resultado.agregarViolacion(
                String.format("Vuelo %s sin capacidad para paquete %s", 
                            vuelo.getNumeroVuelo(), paqueteId),
                TipoViolacion.CAPACIDAD_VUELO_EXCEDIDA
            );
        }
        
        // Verificar capacidad de almacén en aeropuerto destino
        Aeropuerto aeropuertoDestino = aeropuertos.get(segmento.getAeropuertoDestino());
        if (aeropuertoDestino != null && !aeropuertoDestino.puedeAlmacenar(1)) {
            resultado.agregarViolacion(
                String.format("Aeropuerto %s sin capacidad de almacén para paquete %s", 
                            aeropuertoDestino.getCodigoIATA(), paqueteId),
                TipoViolacion.CAPACIDAD_ALMACEN_EXCEDIDA
            );
        }
    }
    
    private void validarPlazosEntrega(String paqueteId, Ruta ruta, ResultadoValidacion resultado) {
        // Obtener información del continente para calcular plazo
        String aeropuertoOrigen = ruta.getAeropuertoOrigen();
        String aeropuertoDestino = ruta.getAeropuertoDestino();
        
        if (aeropuertoOrigen == null || aeropuertoDestino == null) {
            return;
        }
        
        String continenteOrigen = aeropuertoAContinente.get(aeropuertoOrigen);
        String continenteDestino = aeropuertoAContinente.get(aeropuertoDestino);
        
        if (continenteOrigen == null || continenteDestino == null) {
            resultado.agregarViolacion(
                String.format("No se puede determinar continente para paquete %s", paqueteId),
                TipoViolacion.CONTINENTE_INDETERMINADO
            );
            return;
        }
        
        boolean mismoContinente = continenteOrigen.equals(continenteDestino);
        int diasPlazoMaximo = mismoContinente ? 2 : 3;
        
        // Verificar si el tiempo total de la ruta excede el plazo
        double horasMaximas = diasPlazoMaximo * 24.0;
        if (ruta.getTiempoTotalHoras() > horasMaximas) {
            resultado.agregarViolacion(
                String.format("Ruta de paquete %s excede plazo de entrega (%.1f > %.1f horas)", 
                            paqueteId, ruta.getTiempoTotalHoras(), horasMaximas),
                TipoViolacion.PLAZO_EXCEDIDO
            );
        }
        
        // Verificar tiempos de vuelo según restricciones MoraPack
        for (SegmentoRuta segmento : ruta.getSegmentos()) {
            double horasEsperadas = segmento.isMismoContinente() ? 12.0 : 24.0;
            if (Math.abs(segmento.getDuracionHoras() - horasEsperadas) > 0.1) {
                resultado.agregarViolacion(
                    String.format("Duración incorrecta en segmento de paquete %s (%.1f != %.1f horas)", 
                                paqueteId, segmento.getDuracionHoras(), horasEsperadas),
                    TipoViolacion.DURACION_VUELO_INCORRECTA
                );
            }
        }
    }
    
    private void validarCapacidadesGlobales(Solucion solucion, ResultadoValidacion resultado) {
        // Validar capacidades globales de vuelos
        Map<String, Integer> usosVuelo = new HashMap<>();
        Map<String, Integer> usosAlmacen = new HashMap<>();
        
        for (Ruta ruta : solucion.getRutasPaquetes().values()) {
            for (SegmentoRuta segmento : ruta.getSegmentos()) {
                // Contar uso de vuelos
                usosVuelo.put(segmento.getNumeroVuelo(), 
                           usosVuelo.getOrDefault(segmento.getNumeroVuelo(), 0) + 1);
                
                // Contar uso de almacenes
                usosAlmacen.put(segmento.getAeropuertoDestino(),
                             usosAlmacen.getOrDefault(segmento.getAeropuertoDestino(), 0) + 1);
            }
        }
        
        // Verificar capacidades de vuelos
        for (Map.Entry<String, Integer> entry : usosVuelo.entrySet()) {
            Vuelo vuelo = vuelos.get(entry.getKey());
            if (vuelo != null && entry.getValue() > vuelo.getCapacidadMaxima()) {
                resultado.agregarViolacion(
                    String.format("Capacidad de vuelo %s excedida (%d > %d)", 
                                entry.getKey(), entry.getValue(), vuelo.getCapacidadMaxima()),
                    TipoViolacion.CAPACIDAD_VUELO_EXCEDIDA
                );
            }
        }
        
        // Verificar capacidades de almacenes
        for (Map.Entry<String, Integer> entry : usosAlmacen.entrySet()) {
            Aeropuerto aeropuerto = aeropuertos.get(entry.getKey());
            if (aeropuerto != null && entry.getValue() > aeropuerto.getCapacidadAlmacen()) {
                resultado.agregarViolacion(
                    String.format("Capacidad de almacén %s excedida (%d > %d)", 
                                entry.getKey(), entry.getValue(), aeropuerto.getCapacidadAlmacen()),
                    TipoViolacion.CAPACIDAD_ALMACEN_EXCEDIDA
                );
            }
        }
    }
    
    /**
     * Verifica si una ruta es factible sin agregarla a la solución.
     */
    public boolean esRutaFactible(String paqueteId, Ruta ruta, Solucion solucionActual) {
        ResultadoValidacion resultado = validarRuta(paqueteId, ruta);
        
        // Verificar impacto en capacidades globales
        if (resultado.esFactible()) {
            // Simular adición temporal de la ruta
            Solucion solucionTemporal = solucionActual.copiar();
            solucionTemporal.agregarRuta(paqueteId, ruta);
            
            ResultadoValidacion validacionGlobal = new ResultadoValidacion();
            validarCapacidadesGlobales(solucionTemporal, validacionGlobal);
            
            return validacionGlobal.esFactible();
        }
        
        return false;
    }
}
