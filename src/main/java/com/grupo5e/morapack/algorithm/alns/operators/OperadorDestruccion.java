package com.grupo5e.morapack.algorithm.alns.operators;

import com.grupo5e.morapack.core.model.Solucion;
import java.util.List;

/**
 * Interfaz para operadores de destrucción del algoritmo ALNS.
 * Los operadores de destrucción remueven paquetes de una solución existente
 * para crear espacio para mejoras.
 */
public interface OperadorDestruccion {
    
    /**
     * Destruye (remueve) un número específico de paquetes de la solución.
     * 
     * @param solucion La solución actual de la cual remover paquetes
     * @param cantidadRemover Número de paquetes a remover
     * @return Lista de IDs de paquetes removidos
     */
    List<String> destruir(Solucion solucion, int cantidadRemover);
    
    /**
     * @return El nombre del operador de destrucción
     */
    String getNombre();
    
    /**
     * @return Una descripción de la estrategia del operador
     */
    String getDescripcion();
}
