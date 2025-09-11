package com.grupo5e.morapack.algorithm.alns.operators.destruction;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorDestruccion;

import java.util.*;

/**
 * Operador de destrucción que remueve los paquetes con las rutas más costosas.
 * Útil para intensificar la búsqueda en las partes más problemáticas de la solución.
 */
public class DestruccionPeorCosto implements OperadorDestruccion {
    
    @Override
    public List<String> destruir(Solucion solucion, int cantidadRemover) {
        if (solucion.getRutasPaquetes().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Crear lista de paquetes ordenados por costo descendente
        List<Map.Entry<String, Ruta>> paquetesOrdenados = new ArrayList<>(solucion.getRutasPaquetes().entrySet());
        paquetesOrdenados.sort((e1, e2) -> Double.compare(e2.getValue().getCostoTotal(), e1.getValue().getCostoTotal()));
        
        // Limitar la cantidad a remover
        int cantidadReal = Math.min(cantidadRemover, paquetesOrdenados.size());
        
        List<String> paquetesRemovidos = new ArrayList<>();
        for (int i = 0; i < cantidadReal; i++) {
            String paqueteId = paquetesOrdenados.get(i).getKey();
            paquetesRemovidos.add(paqueteId);
        }
        
        // Remover los paquetes de la solución
        for (String paqueteId : paquetesRemovidos) {
            solucion.removerRuta(paqueteId);
        }
        
        return paquetesRemovidos;
    }
    
    @Override
    public String getNombre() {
        return "DestruccionPeorCosto";
    }
    
    @Override
    public String getDescripcion() {
        return "Remueve los paquetes con las rutas más costosas para optimizar las partes más problemáticas";
    }
}
