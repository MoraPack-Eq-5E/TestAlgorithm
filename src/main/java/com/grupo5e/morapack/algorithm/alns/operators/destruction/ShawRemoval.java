package com.grupo5e.morapack.algorithm.alns.operators.destruction;

import com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorDestruccion;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;
import com.grupo5e.morapack.core.model.*;

import java.util.*;

/**
 * Operador de destrucción Shaw basado en el ejemplo VRPTWFL
 * Remueve paquetes similares basándose en proximidad geográfica y temporal
 */
public class ShawRemoval extends AbstractOperator implements OperadorDestruccion {
    
    private final boolean isRandom;
    private final Random random;
    
    public ShawRemoval(boolean isRandom) {
        super("ShawRemoval" + (isRandom ? "Random" : "Deterministic"), "destruction");
        this.isRandom = isRandom;
        this.random = new Random();
    }
    
    @Override
    public List<String> destruir(Solucion solucion, int cantidadRemover) {
        // CORRECCIÓN: Permitir más paquetes pero con límite razonable
        int paquetesDisponibles = solucion.getRutasPaquetes().size();
        int cantidadLimitada = Math.min(cantidadRemover, Math.max(1, Math.min(100, paquetesDisponibles / 5)));
        
        // Usar la implementación con contexto
        return destruirConContexto(solucion, cantidadLimitada, null);
    }
    
    /**
     * Versión mejorada que puede usar el contexto para distancias reales
     */
    public List<String> destruirConContexto(Solucion solucion, int cantidadRemover, ContextoProblema contexto) {
        List<String> paquetesRemovidos = new ArrayList<>();
        
        // Obtener paquetes disponibles en la solución
        List<String> paquetesDisponibles = new ArrayList<>(solucion.getRutasPaquetes().keySet());
        
        if (paquetesDisponibles.isEmpty()) {
            return paquetesRemovidos;
        }
        
        // --- Seleccionar el primer paquete aleatoriamente ---
        String primerPaquete = paquetesDisponibles.get(random.nextInt(paquetesDisponibles.size()));
        paquetesRemovidos.add(primerPaquete);
        paquetesDisponibles.remove(primerPaquete);
        solucion.removerRuta(primerPaquete);
        cantidadRemover--;
        
        // --- Bucle principal: encontrar paquetes similares ---
        while (cantidadRemover > 0 && !paquetesDisponibles.isEmpty()) {
            
            // 1) Seleccionar un paquete de referencia de los ya removidos
            String paqueteReferencia = paquetesRemovidos.get(random.nextInt(paquetesRemovidos.size()));
            
            // 2) Encontrar el paquete más similar al de referencia
            String paqueteMasSimilar = encontrarPaqueteMasSimilar(paqueteReferencia, paquetesDisponibles, solucion, contexto);
            
            if (paqueteMasSimilar != null) {
                paquetesRemovidos.add(paqueteMasSimilar);
                paquetesDisponibles.remove(paqueteMasSimilar);
                solucion.removerRuta(paqueteMasSimilar);
                cantidadRemover--;
            } else {
                // Si no se encuentra un paquete similar, terminar
                break;
            }
        }
        
        return paquetesRemovidos;
    }
    
    
    /**
     * Encuentra el paquete más similar al paquete de referencia
     */
    private String encontrarPaqueteMasSimilar(String paqueteReferencia, List<String> paquetesDisponibles, Solucion solucion, ContextoProblema contexto) {
        if (paquetesDisponibles.isEmpty()) {
            return null;
        }
        
        // Calcular similitud para cada paquete disponible
        List<PaqueteSimilitud> similitudes = new ArrayList<>();
        
        for (String paqueteId : paquetesDisponibles) {
            double similitud = calcularSimilitud(paqueteReferencia, paqueteId, solucion, contexto);
            similitudes.add(new PaqueteSimilitud(paqueteId, similitud));
        }
        
        // Ordenar por similitud (menor distancia = mayor similitud)
        similitudes.sort(Comparator.comparingDouble(ps -> ps.similitud));
        
        // LITERATURA SHAW (1998): Selección probabilística con sesgo hacia similitud
        if (isRandom) {
            // Shaw (1998): Usar ruleta sesgada hacia los más similares
            double totalInverso = 0.0;
            for (PaqueteSimilitud ps : similitudes) {
                totalInverso += 1.0 / (1.0 + ps.similitud); // Inverso de distancia
            }
            
            double rand = random.nextDouble() * totalInverso;
            double acumulado = 0.0;
            
            for (PaqueteSimilitud ps : similitudes) {
                acumulado += 1.0 / (1.0 + ps.similitud);
                if (acumulado >= rand) {
                    return ps.paqueteId;
                }
            }
            
            // Fallback: último elemento
            return similitudes.get(similitudes.size() - 1).paqueteId;
        } else {
            // Determinístico: seleccionar el más similar
            return similitudes.get(0).paqueteId;
        }
    }
    
    /**
     * Calcula la similitud entre dos paquetes basándose en distancia geográfica real
     */
    private double calcularSimilitud(String paquete1Id, String paquete2Id, Solucion solucion, ContextoProblema contexto) {
        try {
            if (contexto == null) {
                // Fallback: usar implementación simplificada si no hay contexto
                return calcularSimilitudSimplificada(paquete1Id, paquete2Id);
            }
            
            // Obtener los paquetes del contexto
            Paquete paquete1 = contexto.getPaquete(paquete1Id);
            Paquete paquete2 = contexto.getPaquete(paquete2Id);
            
            if (paquete1 == null || paquete2 == null) {
                return calcularSimilitudSimplificada(paquete1Id, paquete2Id);
            }
            
            // Obtener aeropuertos de origen y destino
            Aeropuerto origen1 = contexto.getAeropuerto(paquete1.getAeropuertoOrigen());
            Aeropuerto destino1 = contexto.getAeropuerto(paquete1.getAeropuertoDestino());
            Aeropuerto origen2 = contexto.getAeropuerto(paquete2.getAeropuertoOrigen());
            Aeropuerto destino2 = contexto.getAeropuerto(paquete2.getAeropuertoDestino());
            
            if (origen1 == null || destino1 == null || origen2 == null || destino2 == null) {
                return calcularSimilitudSimplificada(paquete1Id, paquete2Id);
            }
            
            // Calcular similitud considerando conectividad y tiempo de vuelo
            double similitudOrigen = calcularSimilitudAeropuertos(origen1, origen2, contexto);
            double similitudDestino = calcularSimilitudAeropuertos(destino1, destino2, contexto);
            
            // Similitud = promedio de similitudes (menor valor = mayor similitud)
            return (similitudOrigen + similitudDestino) / 2.0;
            
        } catch (Exception e) {
            return calcularSimilitudSimplificada(paquete1Id, paquete2Id);
        }
    }
    
    /**
     * Implementación simplificada como fallback
     */
    private double calcularSimilitudSimplificada(String paquete1Id, String paquete2Id) {
        int hash1 = paquete1Id.hashCode();
        int hash2 = paquete2Id.hashCode();
        double distancia = Math.abs(hash1 - hash2) % 1000.0;
        distancia += random.nextDouble() * 100.0;
        return distancia;
    }
    
    /**
     * Calcula la similitud entre dos aeropuertos considerando conectividad y tiempo de vuelo
     */
    private double calcularSimilitudAeropuertos(Aeropuerto aeropuerto1, Aeropuerto aeropuerto2, ContextoProblema contexto) {
        // Si son el mismo aeropuerto, similitud máxima
        if (aeropuerto1.getCodigoIATA().equals(aeropuerto2.getCodigoIATA())) {
            return 0.0;
        }
        
        // Verificar si hay vuelos directos entre los aeropuertos
        List<Vuelo> vuelosDirectos = contexto.getVuelosDesde(aeropuerto1.getCodigoIATA()).stream()
            .filter(v -> v.getAeropuertoDestino().equals(aeropuerto2.getCodigoIATA()))
            .filter(Vuelo::estaOperativo)
            .toList();
        
        if (!vuelosDirectos.isEmpty()) {
            // Hay vuelos directos: usar tiempo de vuelo como similitud
            double tiempoPromedio = vuelosDirectos.stream()
                .mapToDouble(Vuelo::getDuracionHoras)
                .average()
                .orElse(24.0); // Fallback si no hay vuelos
            
            return tiempoPromedio;
        } else {
            // No hay vuelos directos: usar distancia geográfica + penalización
            double distanciaGeografica = calcularDistanciaHaversine(
                aeropuerto1.getLatitud(), aeropuerto1.getLongitud(),
                aeropuerto2.getLatitud(), aeropuerto2.getLongitud()
            );
            
            // Penalizar por falta de conectividad directa
            double penalizacionSinVuelo = 1000.0; // Penalización alta
            return distanciaGeografica + penalizacionSinVuelo;
        }
    }
    
    /**
     * Calcula la distancia entre dos puntos usando la fórmula de Haversine
     * @param lat1 Latitud del primer punto
     * @param lon1 Longitud del primer punto
     * @param lat2 Latitud del segundo punto
     * @param lon2 Longitud del segundo punto
     * @return Distancia en kilómetros
     */
    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la Tierra en kilómetros
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * Clase auxiliar para representar similitud de paquetes
     */
    private static class PaqueteSimilitud {
        String paqueteId;
        double similitud;
        
        PaqueteSimilitud(String paqueteId, double similitud) {
            this.paqueteId = paqueteId;
            this.similitud = similitud;
        }
    }
    
    @Override
    public String getNombre() {
        return getName();
    }

    @Override
    public String getDescripcion() {
        return "Shaw Removal: remueve paquetes basados en similitud considerando conectividad de vuelos y tiempo de vuelo real.";
    }
}
