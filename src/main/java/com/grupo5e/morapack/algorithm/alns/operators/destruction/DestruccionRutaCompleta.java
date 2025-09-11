package com.grupo5e.morapack.algorithm.alns.operators.destruction;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorDestruccion;

import java.util.*;

/**
 * Operador de destrucción que remueve todos los paquetes de rutas completas.
 * Útil para replanificar completamente ciertas rutas que pueden estar mal optimizadas.
 */
public class DestruccionRutaCompleta implements OperadorDestruccion {
    
    
    @Override
    public List<String> destruir(Solucion solucion, int cantidadRemover) {
        if (solucion.getRutasPaquetes().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Agrupar paquetes por ruta similar (mismo origen-destino)
        Map<String, List<String>> gruposRuta = agruparPorRuta(solucion);
        
        List<String> paquetesRemovidos = new ArrayList<>();
        List<String> clavesRuta = new ArrayList<>(gruposRuta.keySet());
        Collections.shuffle(clavesRuta);
        
        // Remover grupos completos de rutas hasta alcanzar la cantidad deseada
        for (String claveRuta : clavesRuta) {
            List<String> paquetesGrupo = gruposRuta.get(claveRuta);
            
            if (paquetesRemovidos.size() + paquetesGrupo.size() <= cantidadRemover) {
                paquetesRemovidos.addAll(paquetesGrupo);
            } else {
                // Si el grupo completo excede la cantidad, tomar parte del grupo
                int espacioRestante = cantidadRemover - paquetesRemovidos.size();
                if (espacioRestante > 0) {
                    Collections.shuffle(paquetesGrupo);
                    paquetesRemovidos.addAll(paquetesGrupo.subList(0, espacioRestante));
                }
                break;
            }
            
            if (paquetesRemovidos.size() >= cantidadRemover) {
                break;
            }
        }
        
        // Remover los paquetes de la solución
        for (String paqueteId : paquetesRemovidos) {
            solucion.removerRuta(paqueteId);
        }
        
        return paquetesRemovidos;
    }
    
    private Map<String, List<String>> agruparPorRuta(Solucion solucion) {
        Map<String, List<String>> grupos = new HashMap<>();
        
        for (Map.Entry<String, Ruta> entry : solucion.getRutasPaquetes().entrySet()) {
            String paqueteId = entry.getKey();
            Ruta ruta = entry.getValue();
            
            // Crear clave basada en origen y destino
            String clave = ruta.getAeropuertoOrigen() + "->" + ruta.getAeropuertoDestino();
            
            grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(paqueteId);
        }
        
        return grupos;
    }
    
    @Override
    public String getNombre() {
        return "DestruccionRutaCompleta";
    }
    
    @Override
    public String getDescripcion() {
        return "Remueve grupos completos de paquetes que comparten la misma ruta origen-destino";
    }
}
