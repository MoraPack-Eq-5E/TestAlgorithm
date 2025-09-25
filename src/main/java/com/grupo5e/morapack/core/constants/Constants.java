package com.grupo5e.morapack.core.constants;

/**
 * Constantes del sistema MoraPack para el algoritmo ALNS
 * Adaptadas al modelo actual del proyecto
 */
public class Constants {
    
    // ================= RUTAS DE ARCHIVOS =================
    public static final String AIRPORT_INFO_FILE_PATH = "data/aeropuertosinfo.txt";
    public static final String FLIGHTS_FILE_PATH = "data/vuelos.txt";
    public static final String PRODUCTS_FILE_PATH = "data/productos.txt";
    
    // ================= CONSTANTES DEL ALGORITMO ALNS =================
    
    // Espacio de soluciones
    public static final int LOWERBOUND_SOLUTION_SPACE = 100;
    public static final int UPPERBOUND_SOLUTION_SPACE = 200;
    
    // Parámetros de destrucción ALNS
    public static final double DESTRUCTION_RATIO = 0.25; // 25% de paquetes a destruir
    public static final int DESTRUCTION_MIN_PACKAGES = 1; // Mínimo 1 paquete
    public static final int DESTRUCTION_MAX_PACKAGES = 50; // Máximo 50 paquetes por destrucción
    
    // ================= CONSTANTES DE TIEMPO DE ENTREGA =================
    
    // Tiempos máximos de entrega (en días)
    public static final double SAME_CONTINENT_MAX_DELIVERY_TIME = 2.0; // 2 días mismo continente
    public static final double DIFFERENT_CONTINENT_MAX_DELIVERY_TIME = 3.0; // 3 días diferentes continentes
    
    // Tiempos de transporte (en días)
    public static final double SAME_CONTINENT_TRANSPORT_TIME = 0.5; // 0.5 días mismo continente
    public static final double DIFFERENT_CONTINENT_TRANSPORT_TIME = 1.0; // 1 día diferentes continentes
    
    // ================= CONSTANTES DE CAPACIDAD =================
    
    // Capacidad de vuelos (paquetes)
    public static final int SAME_CONTINENT_MIN_CAPACITY = 200; // Mínimo mismo continente
    public static final int SAME_CONTINENT_MAX_CAPACITY = 300; // Máximo mismo continente
    public static final int DIFFERENT_CONTINENT_MIN_CAPACITY = 250; // Mínimo diferentes continentes
    public static final int DIFFERENT_CONTINENT_MAX_CAPACITY = 400; // Máximo diferentes continentes
    
    // Capacidad de almacenes (paquetes)
    public static final int WAREHOUSE_MIN_CAPACITY = 600; // Mínimo almacén
    public static final int WAREHOUSE_MAX_CAPACITY = 1000; // Máximo almacén
    
    // Tiempo máximo de pickup del cliente (horas)
    public static final int CUSTOMER_PICKUP_MAX_HOURS = 2; // 2 horas para pickup
    
    // ================= CONFIGURACIÓN DE SOLUCIÓN INICIAL =================
    
    // Control de tipo de solución inicial
    public static final boolean USE_GREEDY_INITIAL_SOLUTION = false; // true=greedy, false=random
    public static final double RANDOM_ASSIGNMENT_PROBABILITY = 0.3; // Para solución random: 30% de asignación
    
    // ================= SEDES MORAPACK =================
    
    // Sedes principales de MoraPack
    public static final String LIMA_WAREHOUSE = "Lima, Peru";
    public static final String BRUSSELS_WAREHOUSE = "Brussels, Belgium";
    public static final String BAKU_WAREHOUSE = "Baku, Azerbaijan";
    
    // Códigos IATA de las sedes principales
    public static final String LIMA_AIRPORT_CODE = "SPIM";
    public static final String BRUSSELS_AIRPORT_CODE = "EBBR";
    public static final String BAKU_AIRPORT_CODE = "UBBB";
    
    // ================= CONSTANTES DE ALGORITMO ALNS =================
    
    // Parámetros de temperatura para Simulated Annealing
    public static final double INITIAL_TEMPERATURE = 1000.0;
    public static final double COOLING_RATE = 0.995;
    public static final double MIN_TEMPERATURE = 0.1;
    
    // Parámetros de iteraciones
    public static final int MAX_ITERATIONS = 1000;
    public static final int MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 50;
    
    // Parámetros de operadores ALNS
    public static final int NUM_DESTRUCTION_OPERATORS = 4; // random, geographic, timeBased, congestedRoute
    public static final int NUM_REPAIR_OPERATORS = 4; // greedy, regret, timeBased, capacityBased
    
    // Parámetros de actualización de pesos
    public static final int UPDATE_INTERVAL = 10; // Actualizar pesos cada 10 iteraciones
    public static final double REACTION_FACTOR = 0.1; // Factor de reacción para actualización de pesos
    
    // ================= CONSTANTES DE VALIDACIÓN =================
    
    // Tiempo de conexión entre vuelos (horas)
    public static final double CONNECTION_TIME_HOURS = 2.0;
    
    // Tiempo de procesamiento en aeropuerto (horas)
    public static final double AIRPORT_PROCESSING_TIME_HOURS = 2.0;
    
    // Margen de seguridad para entregas (horas)
    public static final double DELIVERY_SAFETY_MARGIN_HOURS = 2.0;
    
    // ================= CONSTANTES DE SCORING =================
    
    // Scores para operadores ALNS
    public static final double SIGMA1 = 100.0; // Mejor solución global
    public static final double SIGMA2 = 50.0;  // Mejor solución actual
    public static final double SIGMA3 = 10.0;   // Solución aceptada por SA
    
    // ================= CONSTANTES DE CONTINENTES =================
    
    // Mapeo de continentes para validaciones
    public static final String CONTINENT_AMERICA = "America";
    public static final String CONTINENT_EUROPE = "Europe";
    public static final String CONTINENT_ASIA = "Asia";
    public static final String CONTINENT_AFRICA = "Africa";
    public static final String CONTINENT_OCEANIA = "Oceania";
    
    // ================= CONSTANTES DE PRIORIDAD =================
    
    // Niveles de prioridad de paquetes
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_MEDIUM = 2;
    public static final int PRIORITY_LOW = 3;
    
    // ================= CONSTANTES DE ESTADO =================
    
    // Estados de paquetes
    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_IN_TRANSIT = "IN_TRANSIT";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    
    // Estados de vuelos
    public static final String FLIGHT_STATUS_OPERATIONAL = "OPERATIONAL";
    public static final String FLIGHT_STATUS_CANCELLED = "CANCELLED";
    public static final String FLIGHT_STATUS_DELAYED = "DELAYED";
    
    // ================= CONSTANTES DE CAPACIDAD TEMPORAL =================
    
    // Horizonte temporal para validación (días)
    public static final int TEMPORAL_HORIZON_DAYS = 4; // 4 días de horizonte temporal
    
    // Resolución temporal (minutos)
    public static final int TEMPORAL_RESOLUTION_MINUTES = 1;
    
    // ================= CONSTANTES DE LOGGING =================
    
    // Niveles de logging
    public static final boolean ENABLE_VERBOSE_LOGGING = true;
    public static final boolean ENABLE_DEBUG_LOGGING = false;
    
    // Intervalos de logging
    public static final int LOG_PROGRESS_INTERVAL = 100; // Log cada 100 iteraciones
    public static final int LOG_DETAILED_INTERVAL = 10; // Log detallado cada 10 iteraciones
    
    // ================= CONSTANTES DE MEMORIA =================
    
    // Gestión de memoria para ALNS
    public static final int MAX_VISITED_SOLUTIONS = 1000; // Máximo soluciones visitadas en memoria
    public static final int CLEAR_MEMORY_INTERVAL = 50; // Limpiar memoria cada 50 iteraciones
    
    // ================= CONSTANTES DE RESTART =================
    
    // Parámetros de restart
    public static final boolean ENABLE_RESTART = true;
    public static final int RESTART_THRESHOLD = 50; // Restart después de 50 iteraciones sin mejora
    
    // ================= CONSTANTES DE DIVERSIFICACIÓN =================
    
    // Parámetros de diversificación
    public static final double DIVERSIFICATION_PROBABILITY = 0.1; // 10% probabilidad de diversificación
    public static final int DIVERSIFICATION_INTERVAL = 20; // Diversificar cada 20 iteraciones
    
    // ================= CONSTANTES DE PENALIZACIÓN =================
    
    // Penalizaciones por violaciones
    public static final double CAPACITY_VIOLATION_PENALTY = 1000.0;
    public static final double DEADLINE_VIOLATION_PENALTY = 500.0;
    public static final double WAREHOUSE_VIOLATION_PENALTY = 750.0;
    
    // Penalizaciones por paquetes no ruteados
    public static final double UNROUTED_PACKAGE_PENALTY = 100.0;
    public static final double UNROUTED_PACKAGE_INCREMENT = 50.0; // Incremento por cada paquete no ruteado
}
