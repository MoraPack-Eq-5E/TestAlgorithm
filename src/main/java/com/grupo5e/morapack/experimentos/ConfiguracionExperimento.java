package com.grupo5e.morapack.experimentos;

/**
 * Configuración simple para el experimento
 */
public class ConfiguracionExperimento {

    // Cambiar estos valores para controlar qué algoritmo ejecutar
    public static final boolean EJECUTAR_ALNS = true;
    public static final boolean EJECUTAR_TABU = true;

    // Parámetros del experimento
    public static final int NUMERO_REPETICIONES = 1;
    public static final int TIEMPO_MAXIMO_SEGUNDOS = 120;

    // Umbrales para el fitness (basados en el ALNS original)
    public static final int FITNESS_EXCELENTE = 1000000;
    public static final int FITNESS_BUENO = 500000;
    public static final int FITNESS_ACEPTABLE = 100000;
}