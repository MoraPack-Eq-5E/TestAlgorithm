package com.grupo5e.morapack.core.constants;

public class Constantes {
    // Rutas de archivos
    public static final String RUTA_ARCHIVO_INFO_AEROPUERTOS = "data/aeropuertosinfo.txt";
    public static final String RUTA_ARCHIVO_VUELOS = "data/vuelos.txt";
    public static final String RUTA_ARCHIVO_PRODUCTOS = "data/productos.txt";
    
    // Constantes del algoritmo
    public static final int LIMITE_INFERIOR_ESPACIO_SOLUCION = 100;
    public static final int LIMITE_SUPERIOR_ESPACIO_SOLUCION = 200;
    public static final int MAX_VECINOS_TS = 20; // o el número que prefieras


    // Parámetros de destrucción ALNS optimizados para MoraPack
    public static final double RATIO_DESTRUCCION = 0.15;        // 15% - Ratio moderado para ALNS
    public static final int DESTRUCCION_MIN_PAQUETES = 10;      // Mínimo 10 paquetes
    public static final int DESTRUCCION_MAX_PAQUETES = 500;     // Máximo 500 paquetes (ajustable según problema)
    public static final int DESTRUCCION_MAX_PAQUETES_EXPANSION = 100;  // Para expansiones más controladas
    
    // Constantes de tiempo de entrega
    public static final double TIEMPO_MAX_ENTREGA_MISMO_CONTINENTE = 2.0;
    public static final double TIEMPO_MAX_ENTREGA_DIFERENTE_CONTINENTE = 3.0;
    
    public static final double TIEMPO_TRANSPORTE_MISMO_CONTINENTE = 0.5;
    public static final double TIEMPO_TRANSPORTE_DIFERENTE_CONTINENTE = 1.0;
    
    public static final int CAPACIDAD_MIN_MISMO_CONTINENTE = 200;
    public static final int CAPACIDAD_MAX_MISMO_CONTINENTE = 300;
    public static final int CAPACIDAD_MIN_DIFERENTE_CONTINENTE = 250;
    public static final int CAPACIDAD_MAX_DIFERENTE_CONTINENTE = 400;
    
    public static final int CAPACIDAD_MIN_ALMACEN = 600;
    public static final int CAPACIDAD_MAX_ALMACEN = 1000;
    
    public static final int HORAS_MAX_RECOGIDA_CLIENTE = 2;
    
    // NUEVO: Control de tipo de solución inicial
    public static final boolean USAR_SOLUCION_INICIAL_CODICIOSA = false; // true=codiciosa, false=aleatoria
    public static final double PROBABILIDAD_ASIGNACION_ALEATORIA = 0.3; // Para solución aleatoria: 30% de asignación
    
    // Control de logs
    public static final boolean LOGGING_VERBOSO = false; // true=logs detallados, false=logs mínimos
    public static final int INTERVALO_LOG_ITERACION = 100; // Mostrar solo cada X iteraciones
    
    // NEW: Diversificación extrema / Restart inteligente
    public static final int UMBRAL_ESTANCAMIENTO_PARA_RESTART = 50; // Iteraciones sin mejora significativa para restart
    public static final double UMBRAL_MEJORA_SIGNIFICATIVA = 0.1; // 0.1% mínimo para considerar mejora significativa
    public static final double RATIO_DESTRUCCION_EXTREMA = 0.8; // 80% destrucción para restart
    public static final int MAX_RESTARTS = 3; // Máximo número de restarts por ejecución

    public static final String ALMACEN_LIMA = "Lima, Peru";
    public static final String ALMACEN_BRUSELAS = "Brussels, Belgium";
    public static final String ALMACEN_BAKU = "Baku, Azerbaijan";
}
