package com.grupo5e.morapack.algorithm.alns.operators.destruction;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorDestruccion;

import java.util.*;

/**
 * Operador de destrucción que remueve paquetes similares geográficamente.
 * Remueve paquetes que comparten rutas o aeropuertos similares.
 */
public class DestruccionSimilitud implements OperadorDestruccion {
    
    private Random random = new Random();
    
    @Override
    public List<String> destruir(Solucion solucion, int cantidadRemover) {
        List<String> paquetesDisponibles = new ArrayList<>(solucion.getPaquetesIds());
        
        if (paquetesDisponibles.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Seleccionar un paquete semilla aleatoriamente
        String paqueteSemilla = paquetesDisponibles.get(random.nextInt(paquetesDisponibles.size()));
        Ruta rutaSemilla = solucion.getRutasPaquetes().get(paqueteSemilla);
        
        // Encontrar paquetes similares basándose en aeropuertos compartidos
        List<String> paquetesSimilares = encontrarPaquetesSimilares(solucion, paqueteSemilla, rutaSemilla);
        
        // Limitar la cantidad a remover
        int cantidadReal = Math.min(cantidadRemover, paquetesSimilares.size());
        List<String> paquetesRemovidos = paquetesSimilares.subList(0, cantidadReal);
        
        // Remover los paquetes de la solución
        for (String paqueteId : paquetesRemovidos) {
            solucion.removerRuta(paqueteId);
        }
        
        return new ArrayList<>(paquetesRemovidos);
    }
    
    private List<String> encontrarPaquetesSimilares(Solucion solucion, String paqueteSemilla, Ruta rutaSemilla) {
        Map<String, Double> similaridades = new HashMap<>();
        Set<String> aeropuertosSemilla = new HashSet<>(rutaSemilla.getAeropuertosEnRuta());
        
        // Calcular similitud con todos los demás paquetes
        for (Map.Entry<String, Ruta> entry : solucion.getRutasPaquetes().entrySet()) {
            String paqueteId = entry.getKey();
            if (paqueteId.equals(paqueteSemilla)) {
                similaridades.put(paqueteId, 1.0); // El paquete semilla tiene similitud máxima
                continue;
            }
            
            Ruta ruta = entry.getValue();
            Set<String> aeropuertosRuta = new HashSet<>(ruta.getAeropuertosEnRuta());
            
            // Calcular similitud de Jaccard (intersección / unión)
            Set<String> interseccion = new HashSet<>(aeropuertosSemilla);
            interseccion.retainAll(aeropuertosRuta);
            
            Set<String> union = new HashSet<>(aeropuertosSemilla);
            union.addAll(aeropuertosRuta);
            
            double similitud = union.isEmpty() ? 0.0 : (double) interseccion.size() / union.size();
            similaridades.put(paqueteId, similitud);
        }
        
        // Ordenar por similitud descendente
        List<String> paquetesOrdenados = new ArrayList<>(similaridades.keySet());
        paquetesOrdenados.sort((p1, p2) -> Double.compare(similaridades.get(p2), similaridades.get(p1)));
        
        return paquetesOrdenados;
    }
    
    @Override
    public String getNombre() {
        return "DestruccionSimilitud";
    }
    
    @Override
    public String getDescripcion() {
        return "Remueve paquetes geográficamente similares que comparten aeropuertos en sus rutas";
    }
}
