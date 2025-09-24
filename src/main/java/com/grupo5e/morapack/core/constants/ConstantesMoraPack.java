package com.grupo5e.morapack.core.constants;

/**
 * Constantes del negocio MoraPack según las especificaciones del problema.
 */
public final class ConstantesMoraPack {
    
    // Sedes de MoraPack (códigos ICAO reales)
    public static final String SEDE_LIMA = "SPIM";        // Lima, Perú
    public static final String SEDE_BRUSELAS = "EBCI";    // Bruselas, Bélgica  
    public static final String SEDE_BAKU = "UBBB";        // Baku, Azerbaiyán
    public static final String[] SEDES_MORAPACK = {SEDE_LIMA, SEDE_BRUSELAS, SEDE_BAKU};
    
    // Plazos de entrega (en días)
    public static final int PLAZO_MISMO_CONTINENTE_DIAS = 2;
    public static final int PLAZO_DISTINTO_CONTINENTE_DIAS = 3;
    
    // Tiempos de vuelo (en horas)
    public static final double TIEMPO_VUELO_MISMO_CONTINENTE_HORAS = 12.0; // 0.5 días
    public static final double TIEMPO_VUELO_DISTINTO_CONTINENTE_HORAS = 24.0; // 1 día
    
    // Capacidades de vuelos (número de paquetes)
    public static final int CAPACIDAD_MIN_VUELO_MISMO_CONTINENTE = 200;
    public static final int CAPACIDAD_MAX_VUELO_MISMO_CONTINENTE = 300;
    public static final int CAPACIDAD_MIN_VUELO_DISTINTO_CONTINENTE = 250;
    public static final int CAPACIDAD_MAX_VUELO_DISTINTO_CONTINENTE = 400;
    
    // Capacidades de almacenamiento en aeropuertos (número de paquetes)
    public static final int CAPACIDAD_MIN_ALMACEN_AEROPUERTO = 600;
    public static final int CAPACIDAD_MAX_ALMACEN_AEROPUERTO = 1000;
    
    // Tiempo máximo de recojo por parte del cliente
    public static final double TIEMPO_MAX_RECOJO_HORAS = 2.0;
    
    // Frecuencias de vuelos
    public static final int FRECUENCIA_MIN_VUELOS_MISMO_CONTINENTE = 1; // mínimo 1 vez al día
    public static final int FRECUENCIA_MAX_VUELOS_MISMO_CONTINENTE = 5; // máximo 5 veces al día
    public static final int FRECUENCIA_VUELOS_DISTINTO_CONTINENTE = 1; // al menos 1 vez al día
    
    // Configuraciones por defecto del algoritmo ALNS
    public static final int ITERACIONES_DEFAULT = 5000;
    public static final double TEMPERATURA_INICIAL_DEFAULT = 1000.0;
    public static final double FACTOR_ENFRIAMIENTO_DEFAULT = 0.995;
    public static final double PORCENTAJE_DESTRUCCION_DEFAULT = 0.25; // 25%
    
    // Pesos para función objetivo
    public static final double PESO_COSTO = 0.4;
    public static final double PESO_TIEMPO = 0.3;
    public static final double PESO_VIOLACIONES = 0.3;
    public static final double PENALIZACION_VIOLACION_CRITICA = 1000.0;
    public static final double PENALIZACION_VIOLACION_MODERADA = 100.0;
    
    // Continentes y sus códigos
    public static final String CONTINENTE_AMERICA = "AM";
    public static final String CONTINENTE_EUROPA = "EU"; 
    public static final String CONTINENTE_ASIA = "AS";
    
    // Costos base estimados (pueden ser refinados según datos reales)
    public static final double COSTO_BASE_VUELO_MISMO_CONTINENTE = 100.0;
    public static final double COSTO_BASE_VUELO_DISTINTO_CONTINENTE = 200.0;
    public static final double COSTO_ALMACENAMIENTO_POR_HORA = 1.0;
    public static final double COSTO_RETRASO_POR_HORA = 50.0;
    
    // Escenarios de evaluación
    public enum EscenarioEvaluacion {
        TIEMPO_REAL("Operaciones día a día en tiempo real"),
        SIMULACION_SEMANAL("Simulación semanal del traslado de productos MPE"),
        SIMULACION_COLAPSO("Simulación hasta el colapso de las operaciones");
        
        private final String descripcion;
        
        EscenarioEvaluacion(String descripcion) {
            this.descripcion = descripcion;
        }
        
        public String getDescripcion() {
            return descripcion;
        }
    }
    
    // Rangos de tiempo para simulación semanal
    public static final int MIN_TIEMPO_SIMULACION_SEMANAL_MINUTOS = 30;
    public static final int MAX_TIEMPO_SIMULACION_SEMANAL_MINUTOS = 90;
    
    // Configuraciones de calidad de servicio
    public static final double PORCENTAJE_PUNTUALIDAD_OBJETIVO = 95.0; // 95% de entregas a tiempo
    public static final double PORCENTAJE_UTILIZACION_OPTIMA_VUELOS = 80.0; // 80% de utilización
    public static final double PORCENTAJE_UTILIZACION_OPTIMA_ALMACENES = 70.0; // 70% de utilización
    
    // Constructor privado para evitar instanciación
    private ConstantesMoraPack() {
        throw new IllegalStateException("Clase de constantes no debe ser instanciada");
    }
    
    /**
     * Obtiene el plazo en días según si es el mismo continente o no.
     */
    public static int obtenerPlazoDias(boolean mismoContinente) {
        return mismoContinente ? PLAZO_MISMO_CONTINENTE_DIAS : PLAZO_DISTINTO_CONTINENTE_DIAS;
    }
    
    /**
     * Obtiene el tiempo de vuelo en horas según si es el mismo continente o no.
     */
    public static double obtenerTiempoVueloHoras(boolean mismoContinente) {
        return mismoContinente ? TIEMPO_VUELO_MISMO_CONTINENTE_HORAS : TIEMPO_VUELO_DISTINTO_CONTINENTE_HORAS;
    }
    
    /**
     * Obtiene la capacidad mínima de vuelo según el tipo.
     */
    public static int obtenerCapacidadMinimaVuelo(boolean mismoContinente) {
        return mismoContinente ? CAPACIDAD_MIN_VUELO_MISMO_CONTINENTE : CAPACIDAD_MIN_VUELO_DISTINTO_CONTINENTE;
    }
    
    /**
     * Obtiene la capacidad máxima de vuelo según el tipo.
     */
    public static int obtenerCapacidadMaximaVuelo(boolean mismoContinente) {
        return mismoContinente ? CAPACIDAD_MAX_VUELO_MISMO_CONTINENTE : CAPACIDAD_MAX_VUELO_DISTINTO_CONTINENTE;
    }
    
    /**
     * Obtiene el costo base de vuelo según el tipo.
     */
    public static double obtenerCostoBaseVuelo(boolean mismoContinente) {
        return mismoContinente ? COSTO_BASE_VUELO_MISMO_CONTINENTE : COSTO_BASE_VUELO_DISTINTO_CONTINENTE;
    }
    
    /**
     * Verifica si un aeropuerto es sede de MoraPack.
     */
    public static boolean esSedeMoraPack(String codigoIATA) {
        for (String sede : SEDES_MORAPACK) {
            if (sede.equals(codigoIATA)) {
                return true;
            }
        }
        return false;
    }
}
