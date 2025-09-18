package com.grupo5e.morapack.algorithm.alns.operators.destruction;

import com.grupo5e.morapack.core.model.Solucion;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorDestruccion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Operador de destrucción que remueve paquetes de manera aleatoria.
 * Es el operador más simple y sirve para diversificar la búsqueda.
 */
public class DestruccionAleatoria implements OperadorDestruccion {
    
    @Override
    public List<String> destruir(Solucion solucion, int cantidadRemover) {
        List<String> paquetesDisponibles = new ArrayList<>(solucion.getPaquetesIds());
        
        if (paquetesDisponibles.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Limitar la cantidad a remover a los paquetes disponibles
        int cantidadReal = Math.min(cantidadRemover, paquetesDisponibles.size());
        
        // Mezclar y tomar los primeros 'cantidadReal' paquetes
        Collections.shuffle(paquetesDisponibles);
        List<String> paquetesRemovidos = paquetesDisponibles.subList(0, cantidadReal);
        
        // Remover los paquetes de la solución
        for (String paqueteId : paquetesRemovidos) {
            solucion.removerRuta(paqueteId);
        }
        
        return new ArrayList<>(paquetesRemovidos);
    }
    
    @Override
    public String getNombre() {
        return "DestruccionAleatoria";
    }
    
    @Override
    public String getDescripcion() {
        return "Remueve paquetes de manera completamente aleatoria para diversificar la búsqueda";
    }
}
