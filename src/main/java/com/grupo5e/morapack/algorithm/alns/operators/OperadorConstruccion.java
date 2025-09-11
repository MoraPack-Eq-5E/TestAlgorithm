package com.grupo5e.morapack.algorithm.alns.operators;

import com.grupo5e.morapack.core.model.Solucion;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;
import com.grupo5e.morapack.algorithm.validation.ValidadorRestricciones;
import java.util.List;

/**
 * Interfaz para operadores de construcción del algoritmo ALNS.
 * Los operadores de construcción reinsertan paquetes removidos
 * creando nuevas rutas optimizadas.
 */
public interface OperadorConstruccion {
    
    /**
     * Construye una nueva solución insertando los paquetes removidos
     * en la solución parcial.
     * 
     * @param solucionParcial La solución después de la fase de destrucción
     * @param paquetesRemovidos Lista de IDs de paquetes a reinsertar
     * @param contexto Contexto completo del problema con toda la información necesaria
     * @param validador Validador de restricciones
     * @return Nueva solución con todos los paquetes insertados
     */
    Solucion construir(Solucion solucionParcial, 
                      List<String> paquetesRemovidos,
                      ContextoProblema contexto,
                      ValidadorRestricciones validador);
    
    /**
     * @return El nombre del operador de construcción
     */
    String getNombre();
    
    /**
     * @return Una descripción de la estrategia del operador
     */
    String getDescripcion();
}
