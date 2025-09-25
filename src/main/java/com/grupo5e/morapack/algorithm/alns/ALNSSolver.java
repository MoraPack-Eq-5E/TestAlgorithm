package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.core.model.Producto;
import com.grupo5e.morapack.core.constants.Constantes;
import com.grupo5e.morapack.utils.LectorAeropuerto;
import com.grupo5e.morapack.utils.LectorVuelos;
import com.grupo5e.morapack.utils.LectorProductos;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ALNSSolver {
    private HashMap<HashMap<Paquete, ArrayList<Vuelo>>, Integer> solucion;
    private LectorAeropuerto lectorAeropuertos;
    private LectorVuelos lectorVuelos;
    private LectorProductos lectorProductos;
    
    // CHANGED: Cache robusta Ciudad→Aeropuerto por nombre (evita problemas de equals)
    private Map<String, Aeropuerto> cacheNombreCiudadAeropuerto;
    private ArrayList<Aeropuerto> aeropuertos;
    private ArrayList<Vuelo> vuelos;
    private ArrayList<Paquete> paquetes;
    
    // PATCH: Unitización - flag y datos
    private static final boolean HABILITAR_UNITIZACION_PRODUCTO = true; // Flag para activar/desactivar
    private ArrayList<Paquete> paquetesOriginales; // Paquetes originales antes de unitizar
    
    // NEW: Ancla temporal T0 para cálculos consistentes
    private LocalDateTime T0;
    // Mapa para rastrear la ocupación de almacenes por destino
    private HashMap<Aeropuerto, Integer> ocupacionAlmacenes;
    // Matriz temporal para validar capacidad de almacenes por minuto [aeropuerto][minuto_del_dia]
    private HashMap<Aeropuerto, int[]> ocupacionTemporalAlmacenes;
    // Generador de números aleatorios para diversificar soluciones
    private HashMap<HashMap<Paquete, ArrayList<Vuelo>>, Integer> mejorSolucion;
    private Random aleatorio;
    
    // Variables para ALNS
    private ALNSDestruction operadoresDestruccion;
    private ALNSRepair operadoresReparacion;
    private double[][] pesosOperadores; // Pesos de operadores [destrucción][reparación]
    private double[][] puntajesOperadores;  // Puntajes de operadores [destrucción][reparación]
    private int[][] usoOperadores;      // Contador de uso de operadores [destrucción][reparación]
    private double temperatura;
    private double tasaEnfriamiento;
    private int maxIteraciones;
    private int tamanoSegmento;
    
    // Missing constants and fields from example.java
    private static final int HORIZON_DAYS = 4;
    private static final boolean DEBUG_MODE = false;
    
    public ALNSSolver() {
        this.lectorAeropuertos = new LectorAeropuerto(Constantes.RUTA_ARCHIVO_INFO_AEROPUERTOS);
        this.solucion = new HashMap<>();
        this.aeropuertos = lectorAeropuertos.leerAeropuertos();
        this.lectorVuelos = new LectorVuelos(Constantes.RUTA_ARCHIVO_VUELOS, this.aeropuertos);
        this.vuelos = lectorVuelos.leerVuelos();
        this.lectorProductos = new LectorProductos(Constantes.RUTA_ARCHIVO_PRODUCTOS, this.aeropuertos);
        this.paquetesOriginales = lectorProductos.leerProductos();
        
        // PATCH: Aplicar unitización si está habilitada
        if (HABILITAR_UNITIZACION_PRODUCTO) {
            this.paquetes = expandirPaquetesAUnidadesProducto(this.paquetesOriginales);
            System.out.println("UNITIZACIÓN APLICADA: " + this.paquetesOriginales.size() + 
                             " paquetes originales → " + this.paquetes.size() + " unidades de producto");
        } else {
            this.paquetes = new ArrayList<>(this.paquetesOriginales);
            System.out.println("UNITIZACIÓN DESHABILITADA: Usando paquetes originales");
        }
        
        this.ocupacionAlmacenes = new HashMap<>();
        this.ocupacionTemporalAlmacenes = new HashMap<>();
        
        // CHANGED: Inicializar cache robusta y T0
        inicializarCacheCiudadAeropuerto();
        inicializarT0();
        
        // Inicializar generador de números aleatorios con semilla basada en tiempo actual
        this.aleatorio = new Random(System.currentTimeMillis());
        
        // Inicializar operadores ALNS
        this.operadoresDestruccion = new ALNSDestruction();
        this.operadoresReparacion = new ALNSRepair(aeropuertos, vuelos, ocupacionAlmacenes);
        
        // Inicializar parámetros ALNS
        inicializarParametrosALNS();
        
        // Inicializar ocupación de almacenes
        inicializarOcupacionAlmacenes();
        inicializarOcupacionTemporalAlmacenes();
    }
    
    /**
     * Inicializa los parámetros del algoritmo ALNS
     */
    private void inicializarParametrosALNS() {
        // Número de operadores de destrucción y reparación
        int numOperadoresDestruccion = 4; // aleatorio, geografico, basadoEnTiempo, rutaCongestionada
        int numOperadoresReparacion = 4;      // codicioso, arrepentimiento, basadoEnTiempo, basadoEnCapacidad
        
        // Inicializar matrices de pesos, puntajes y uso
        this.pesosOperadores = new double[numOperadoresDestruccion][numOperadoresReparacion];
        this.puntajesOperadores = new double[numOperadoresDestruccion][numOperadoresReparacion];
        this.usoOperadores = new int[numOperadoresDestruccion][numOperadoresReparacion];
        
        // Inicializar pesos uniformemente (1.0 para todos)
        for (int i = 0; i < numOperadoresDestruccion; i++) {
            for (int j = 0; j < numOperadoresReparacion; j++) {
                this.pesosOperadores[i][j] = 1.0;
                this.puntajesOperadores[i][j] = 0.0;
                this.usoOperadores[i][j] = 0;
            }
        }
        
        // Parámetros del algoritmo
        this.temperatura = 1000.0;        // Temperatura inicial
        this.tasaEnfriamiento = 0.995;         // Tasa de enfriamiento
        this.maxIteraciones = 10;          // Máximo número de iteraciones (para demostración)
        this.tamanoSegmento = 10;            // Tamaño del segmento para actualizar pesos
    }

    public void resolver() {
        // 1. Inicialización
        System.out.println("Iniciando solución ALNS");
        System.out.println("Lectura de aeropuertos");
        System.out.println("Aeropuertos leídos: " + this.aeropuertos.size());
        System.out.println("Lectura de vuelos");
        System.out.println("Vuelos leídos: " + this.vuelos.size());
        System.out.println("Lectura de productos");
        System.out.println("Productos leídos: " + this.paquetes.size());
        
        // 2. Generar una solución inicial s_actual
        System.out.println("\n=== GENERANDO SOLUCIÓN INICIAL ===");
        this.generarSolucionInicial();
        
        // Validar solución generada
        System.out.println("Validando solución...");
        boolean esValida = this.esSolucionValida();
        System.out.println("Solución válida: " + (esValida ? "SÍ" : "NO"));
        
        // Mostrar descripción de la solución inicial
        this.imprimirDescripcionSolucion(1);
        
        // 3. Establecer s_mejor = s_actual
        mejorSolucion = new HashMap<>(solucion);
        
        // 4. Ejecutar algoritmo ALNS
        System.out.println("\n=== INICIANDO ALGORITMO ALNS ===");
        ejecutarAlgoritmoALNS();
        
        // 5. Mostrar resultado final
        System.out.println("\n=== RESULTADO FINAL ALNS ===");
        this.imprimirDescripcionSolucion(2);
    }
    
    /**
     * Ejecuta el algoritmo ALNS (Adaptive Large Neighborhood Search)
     */
    private void ejecutarAlgoritmoALNS() {
        // Obtener la solución actual y su peso
        HashMap<Paquete, ArrayList<Vuelo>> solucionActual = null;
        int pesoActual = Integer.MAX_VALUE;
        
        for (Map.Entry<HashMap<Paquete, ArrayList<Vuelo>>, Integer> entrada : solucion.entrySet()) {
            solucionActual = new HashMap<>(entrada.getKey());
            pesoActual = entrada.getValue();
            break;
        }
        
        if (solucionActual == null) {
            System.out.println("Error: No se pudo obtener la solución inicial");
            return;
        }
        
        System.out.println("Peso de solución inicial: " + pesoActual);
        
        int mejorPeso = pesoActual;
        int mejoras = 0;
        int conteoSinMejoras = 0;
        
        // Bucle principal ALNS
        for (int iteracion = 0; iteracion < maxIteraciones; iteracion++) {
            System.out.println("ALNS Iteración " + iteracion + "/" + maxIteraciones);
            
            // Seleccionar operadores basado en pesos
            int[] operadoresSeleccionados = seleccionarOperadores();
            int operadorDestruccion = operadoresSeleccionados[0];
            int operadorReparacion = operadoresSeleccionados[1];
            System.out.println("  Operadores seleccionados: Destrucción=" + operadorDestruccion + ", Reparación=" + operadorReparacion);
            
            // Crear copia de la solución actual
            HashMap<Paquete, ArrayList<Vuelo>> solucionTemporal = new HashMap<>(solucionActual);
            
            // PATCH: Crear snapshots completos antes de modificar
            Map<Vuelo, Integer> snapshotCapacidades = crearSnapshotCapacidades();
            Map<Aeropuerto, Integer> snapshotAlmacenes = crearSnapshotAlmacenes();
            
            // Aplicar operador de destrucción
            System.out.println("  Aplicando operador de destrucción...");
            long tiempoInicio = System.currentTimeMillis();
            ALNSDestruction.ResultadoDestruccion resultadoDestruccion = aplicarOperadorDestruccion(
                solucionTemporal, operadorDestruccion);
            long tiempoFin = System.currentTimeMillis();
            System.out.println("  Operador de destrucción completado en " + (tiempoFin - tiempoInicio) + "ms");
            
            if (resultadoDestruccion == null || resultadoDestruccion.getPaquetesDestruidos().isEmpty()) {
                System.out.println("  No se pudo destruir nada, continuando...");
                continue; // No se pudo destruir nada
            }
            System.out.println("  Paquetes destruidos: " + resultadoDestruccion.getPaquetesDestruidos().size());
            
            // PATCH: Usar solución parcial de destrucción y reconstruir estado
            solucionTemporal = new HashMap<>(resultadoDestruccion.getSolucionParcial());
            reconstruirCapacidadesDesdeSolucion(solucionTemporal);
            reconstruirAlmacenesDesdeSolucion(solucionTemporal);
            
            // Aplicar operador de reparación
            ALNSRepair.ResultadoReparacion resultadoReparacion = aplicarOperadorReparacion(
                solucionTemporal, operadorReparacion, resultadoDestruccion.getPaquetesDestruidos());
            
            if (resultadoReparacion == null || !resultadoReparacion.esExitoso()) {
                // PATCH: Restaurar snapshots si falla la reparación
                restaurarCapacidades(snapshotCapacidades);
                restaurarAlmacenes(snapshotAlmacenes);
                continue; // No se pudo reparar
            }
            
            // PATCH: Usar solución reparada y reconstruir estado
            solucionTemporal = new HashMap<>(resultadoReparacion.getSolucionReparada());
            reconstruirCapacidadesDesdeSolucion(solucionTemporal);
            reconstruirAlmacenesDesdeSolucion(solucionTemporal);
            
            // Evaluar nueva solución
            int pesoTemporal = calcularPesoSolucion(solucionTemporal);
            
            // Actualizar contador de uso
            usoOperadores[operadorDestruccion][operadorReparacion]++;
            
            // Criterio de aceptación
            boolean aceptada = false;
            if (pesoTemporal > pesoActual) {
                solucionActual = solucionTemporal;
                pesoActual = pesoTemporal;
                aceptada = true;

                if (pesoTemporal > mejorPeso) {
                    mejorPeso = pesoTemporal;
                    mejorSolucion.clear();
                    mejorSolucion.put(new HashMap<>(solucionActual), pesoActual);
                    puntajesOperadores[operadorDestruccion][operadorReparacion] += 100;
                    mejoras++;
                    conteoSinMejoras = 0;
                    System.out.println("Iteración " + iteracion + ": ¡Nueva mejor solución! Peso: " + mejorPeso);
                } else {
                    puntajesOperadores[operadorDestruccion][operadorReparacion] += 50;
                }
            } else {
                double probabilidad = Math.exp((pesoTemporal - pesoActual) / temperatura);
                if (aleatorio.nextDouble() < probabilidad) {
                    solucionActual = solucionTemporal;
                    pesoActual = pesoTemporal;
                    aceptada = true;
                    puntajesOperadores[operadorDestruccion][operadorReparacion] += 10;
                }
            }
            
            // PATCH: Restaurar snapshots si no se acepta la solución
            if (!aceptada) {
                restaurarCapacidades(snapshotCapacidades);
                restaurarAlmacenes(snapshotAlmacenes);
                conteoSinMejoras++;
            }
            // NOTA: Si se acepta, solucionTemporal ya tiene el estado correcto reconstruido
            
            // Actualizar pesos cada tamanoSegmento iteraciones
            if ((iteracion + 1) % tamanoSegmento == 0) {
                actualizarPesosOperadores();
                temperatura *= tasaEnfriamiento;
            }
            
            // Parada temprana si no hay mejoras
            if (conteoSinMejoras > 50) {
                System.out.println("Parada temprana en iteración " + iteracion + " (sin mejoras)");
                break;
            }
        }
        
        // Actualizar la solución final
        solucion.clear();
        solucion.putAll(mejorSolucion);
        
        System.out.println("ALNS completado:");
        System.out.println("  Mejoras encontradas: " + mejoras);
        System.out.println("  Peso final: " + mejorPeso);
        System.out.println("  Temperatura final: " + temperatura);
    }
    
    /**
     * Selecciona operadores de destrucción y reparación basado en sus pesos
     */
    private int[] seleccionarOperadores() {
        try {
            System.out.println("    Seleccionando operadores...");
            // Selección por ruleta basada en pesos
            double pesoTotal = 0.0;
            for (int i = 0; i < pesosOperadores.length; i++) {
                for (int j = 0; j < pesosOperadores[i].length; j++) {
                    pesoTotal += pesosOperadores[i][j];
                }
            }
            
            System.out.println("    Peso total: " + pesoTotal);
            double valorAleatorio = aleatorio.nextDouble() * pesoTotal;
            double pesoAcumulado = 0.0;
            
            for (int i = 0; i < pesosOperadores.length; i++) {
                for (int j = 0; j < pesosOperadores[i].length; j++) {
                    pesoAcumulado += pesosOperadores[i][j];
                    if (valorAleatorio <= pesoAcumulado) {
                        System.out.println("    Operadores seleccionados: " + i + ", " + j);
                        return new int[]{i, j};
                    }
                }
            }
            
            // Fallback: seleccionar el primero
            System.out.println("    Usando fallback: 0, 0");
            return new int[]{0, 0};
        } catch (Exception e) {
            System.out.println("    Error en selección de operadores: " + e.getMessage());
            e.printStackTrace();
            return new int[]{0, 0};
        }
    }
    
    /**
     * Aplica el operador de destrucción seleccionado
     */
    private ALNSDestruction.ResultadoDestruccion aplicarOperadorDestruccion(
            HashMap<Paquete, ArrayList<Vuelo>> solucion, int indiceOperador) {
        try {
            switch (indiceOperador) {
                case 0: // Destrucción Aleatoria
                    System.out.println("    Ejecutando destruccionAleatoria...");
                    return operadoresDestruccion.destruccionAleatoria(solucion, Constantes.RATIO_DESTRUCCION, Constantes.DESTRUCCION_MIN_PAQUETES, Constantes.DESTRUCCION_MAX_PAQUETES);
                case 1: // Destrucción Geográfica
                    System.out.println("    Ejecutando destruccionGeografica...");
                    return operadoresDestruccion.destruccionGeografica(solucion, Constantes.RATIO_DESTRUCCION, Constantes.DESTRUCCION_MIN_PAQUETES, Constantes.DESTRUCCION_MAX_PAQUETES);
                case 2: // Destrucción Basada en Tiempo
                    System.out.println("    Ejecutando destruccionBasadaEnTiempo...");
                    return operadoresDestruccion.destruccionBasadaEnTiempo(solucion, Constantes.RATIO_DESTRUCCION, Constantes.DESTRUCCION_MIN_PAQUETES, Constantes.DESTRUCCION_MAX_PAQUETES);
                case 3: // Destrucción Ruta Congestionada - OPTIMIZADO
                    System.out.println("    Ejecutando destruccionRutaCongestionada (optimizado)...");
                    return operadoresDestruccion.destruccionRutaCongestionada(solucion, Constantes.RATIO_DESTRUCCION, Constantes.DESTRUCCION_MIN_PAQUETES, Constantes.DESTRUCCION_MAX_PAQUETES);
                default:
                    System.out.println("    Ejecutando destruccionAleatoria (default)...");
                    return operadoresDestruccion.destruccionAleatoria(solucion, Constantes.RATIO_DESTRUCCION, Constantes.DESTRUCCION_MIN_PAQUETES, Constantes.DESTRUCCION_MAX_PAQUETES);
            }
        } catch (Exception e) {
            System.out.println("    Error en operador de destrucción: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Aplica el operador de reparación seleccionado
     */
    private ALNSRepair.ResultadoReparacion aplicarOperadorReparacion(
            HashMap<Paquete, ArrayList<Vuelo>> solucion, int indiceOperador,
            ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> paquetesDestruidos) {
        
        // Los operadores de reparación esperan los Map.Entry completos
        switch (indiceOperador) {
            case 0: // Reparación Codiciosa
                return operadoresReparacion.reparacionCodiciosa(solucion, paquetesDestruidos);
            case 1: // Reparación por Arrepentimiento
                return operadoresReparacion.reparacionArrepentimiento(solucion, paquetesDestruidos, 2); // nivelArrepentimiento = 2
            case 2: // Reparación Basada en Tiempo
                return operadoresReparacion.reparacionPorTiempo(solucion, paquetesDestruidos);
            case 3: // Reparación Basada en Capacidad
                return operadoresReparacion.reparacionPorCapacidad(solucion, paquetesDestruidos);
            default:
                return operadoresReparacion.reparacionCodiciosa(solucion, paquetesDestruidos);
        }
    }
    
    /**
     * Actualiza los pesos de los operadores basado en sus puntajes
     */
    private void actualizarPesosOperadores() {
        double lambda = 0.1; // Factor de aprendizaje
        
        for (int i = 0; i < puntajesOperadores.length; i++) {
            for (int j = 0; j < puntajesOperadores[i].length; j++) {
                if (usoOperadores[i][j] > 0) {
                    double puntajePromedio = puntajesOperadores[i][j] / usoOperadores[i][j];
                    pesosOperadores[i][j] = (1 - lambda) * pesosOperadores[i][j] + 
                                          lambda * puntajePromedio;
                    
                    // Reiniciar puntajes y contador
                    puntajesOperadores[i][j] = 0.0;
                    usoOperadores[i][j] = 0;
                }
            }
        }
    }
    
    /**
     * CORRECCIÓN: Crear snapshot de capacidades de vuelos
     */
    private Map<Vuelo, Integer> crearSnapshotCapacidades() {
        Map<Vuelo, Integer> snapshot = new HashMap<>();
        for (Vuelo f : vuelos) {
            snapshot.put(f, f.getCapacidadUsada());
        }
        return snapshot;
    }
    
    /**
     * CORRECCIÓN: Restaurar capacidades desde snapshot
     */
    private void restaurarCapacidades(Map<Vuelo, Integer> snapshot) {
        for (Vuelo f : vuelos) {
            f.setCapacidadUsada(snapshot.getOrDefault(f, 0));
        }
    }
    
    /**
     * CORRECCIÓN: Reconstruir capacidades limpiamente desde una solución
     */
    private void reconstruirCapacidadesDesdeSolucion(HashMap<Paquete, ArrayList<Vuelo>> solucion) {
        // Primero resetear todas las capacidades
        for (Vuelo f : vuelos) {
            f.setCapacidadUsada(0);
        }
        
        // Luego reconstruir desde la solución
        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : solucion.entrySet()) {
            Paquete paquete = entrada.getKey();
            ArrayList<Vuelo> ruta = entrada.getValue();
            int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
            
            for (Vuelo f : ruta) {
                f.setCapacidadUsada(f.getCapacidadUsada() + conteoProductos);
            }
        }
    }
    
    /**
     * PATCH: Snapshot/restore completo de almacenes para ALNS
     */
    private Map<Aeropuerto, Integer> crearSnapshotAlmacenes() {
        Map<Aeropuerto, Integer> snapshot = new HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            snapshot.put(aeropuerto, ocupacionAlmacenes.getOrDefault(aeropuerto, 0));
        }
        return snapshot;
    }
    
    /**
     * PATCH: Restaurar ocupación de almacenes desde snapshot
     */
    private void restaurarAlmacenes(Map<Aeropuerto, Integer> snapshot) {
        ocupacionAlmacenes.clear();
        ocupacionAlmacenes.putAll(snapshot);
    }
    
    /**
     * PATCH: Reconstruir almacenes limpiamente desde una solución
     */
    private void reconstruirAlmacenesDesdeSolucion(HashMap<Paquete, ArrayList<Vuelo>> solucion) {
        // Resetear todas las ocupaciones
        inicializarOcupacionAlmacenes();
        
        // Reconstruir desde la solución
        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : solucion.entrySet()) {
            Paquete paquete = entrada.getKey();
            ArrayList<Vuelo> ruta = entrada.getValue();
            int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
            
            if (ruta == null || ruta.isEmpty()) {
                // Paquete ya en destino final
                Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
                if (aeropuertoDestino != null) {
                    incrementarOcupacionAlmacen(aeropuertoDestino, conteoProductos);
                }
            } else {
                // Paquete en ruta - ocupa almacén de destino del último vuelo
                Vuelo ultimoVuelo = ruta.get(ruta.size() - 1);
                incrementarOcupacionAlmacen(ultimoVuelo.getAeropuertoDestino(), conteoProductos);
            }
        }
    }
    
    /**
     * PATCH: Helper para validar capacidad por cantidad de productos
     * @param ruta ruta de vuelos a validar
     * @param cantidad cantidad de productos que se quieren asignar
     * @return true si todos los vuelos de la ruta pueden acomodar cantidad productos adicionales
     */
    private boolean cabeEnCapacidad(ArrayList<Vuelo> ruta, int cantidad) {
        if (ruta == null || ruta.isEmpty()) return true;
        
        for (Vuelo vuelo : ruta) {
            if (vuelo.getCapacidadUsada() + cantidad > vuelo.getCapacidadMaxima()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * CHANGED: Cache robusta Ciudad→Aeropuerto por nombre de ciudad
     * Evita problemas de equals/hashCode con objetos Ciudad
     */
    private void inicializarCacheCiudadAeropuerto() {
        cacheNombreCiudadAeropuerto = new HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (aeropuerto.getCiudad() != null && aeropuerto.getCiudad().getNombre() != null) {
                String claveCiudad = aeropuerto.getCiudad().getNombre().toLowerCase().trim();
                cacheNombreCiudadAeropuerto.put(claveCiudad, aeropuerto);
            }
        }
        System.out.println("Cache inicializada: " + cacheNombreCiudadAeropuerto.size() + " ciudades");
    }
    
    /**
     * NEW: Inicializar T0 como mínimo fechaPedido o now si vacío
     */
    private void inicializarT0() {
        T0 = LocalDateTime.now(); // Default fallback
        
        if (paquetes != null && !paquetes.isEmpty()) {
            LocalDateTime minFechaPedido = paquetes.stream()
                .filter(paquete -> paquete.getFechaPedido() != null)
                .map(Paquete::getFechaPedido)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            T0 = minFechaPedido;
        }
        
        System.out.println("T0 inicializado: " + T0);
    }
    
    /**
     * PATCH: Unitización - expandir paquetes a unidades de producto
     * 
     * Estrategia: cada paquete con N productos se convierte en N "package units"
     * independientes, cada uno con 1 producto, permitiendo que viajen en vuelos diferentes.
     * 
     * Para desactivar: cambiar HABILITAR_UNITIZACION_PRODUCTO = false
     * 
     * @param paquetesOriginales lista de paquetes originales
     * @return lista de unidades de producto (1 producto = 1 package unit)
     */
    private ArrayList<Paquete> expandirPaquetesAUnidadesProducto(ArrayList<Paquete> paquetesOriginales) {
        ArrayList<Paquete> unidadesProducto = new ArrayList<>();
        
        for (Paquete paqueteOriginal : paquetesOriginales) {
            int conteoProductos = (paqueteOriginal.getProductos() != null && !paqueteOriginal.getProductos().isEmpty()) 
                             ? paqueteOriginal.getProductos().size() : 1;
            
            // Crear una unidad por cada producto
            for (int i = 0; i < conteoProductos; i++) {
                Paquete unidad = crearUnidadPaquete(paqueteOriginal, i);
                unidadesProducto.add(unidad);
            }
        }
        
        return unidadesProducto;
    }
    
    /**
     * PATCH: Crear una unidad de paquete (1 producto) a partir del paquete original
     * 
     * @param paqueteOriginal paquete original
     * @param indiceUnidad índice de la unidad (0, 1, 2, ...)
     * @return nueva unidad de paquete con ID derivado y 1 producto
     */
    private Paquete crearUnidadPaquete(Paquete paqueteOriginal, int indiceUnidad) {
        Paquete unidad = new Paquete();
        
        // PATCH: ID derivado usando hash para compatibilidad con int
        String idUnidadString = paqueteOriginal.getId() + "#" + indiceUnidad;
        unidad.setId(idUnidadString.hashCode());
        
        // Copiar todos los metadatos del paquete original
        unidad.setCliente(paqueteOriginal.getCliente());
        unidad.setCiudadDestino(paqueteOriginal.getCiudadDestino());
        unidad.setFechaPedido(paqueteOriginal.getFechaPedido());
        unidad.setFechaLimiteEntrega(paqueteOriginal.getFechaLimiteEntrega());
        unidad.setEstado(paqueteOriginal.getEstado());
        unidad.setUbicacionActual(paqueteOriginal.getUbicacionActual());
        unidad.setPrioridad(paqueteOriginal.getPrioridad());
        unidad.setRutaAsignada(paqueteOriginal.getRutaAsignada());
        
        // CRÍTICO: Crear lista con exactamente 1 producto
        ArrayList<Producto> productoUnico = new ArrayList<>();
        if (paqueteOriginal.getProductos() != null && indiceUnidad < paqueteOriginal.getProductos().size()) {
            // Copiar el producto específico del paquete original
            Producto productoOriginal = paqueteOriginal.getProductos().get(indiceUnidad);
            Producto copiaProducto = new Producto();
            copiaProducto.setId(productoOriginal.getId());
            copiaProducto.setVueloAsignado(productoOriginal.getVueloAsignado());
            copiaProducto.setEstado(productoOriginal.getEstado());
            productoUnico.add(copiaProducto);
        } else {
            // Crear un producto genérico si no existe
            Producto productoGenerico = new Producto();
            String idProductoString = paqueteOriginal.getId() + "_P" + indiceUnidad;
            productoGenerico.setId(idProductoString.hashCode());
            productoUnico.add(productoGenerico);
        }
        
        unidad.setProductos(productoUnico);
        
        return unidad;
    }
    
    /**
     * CHANGED: obtenerAeropuertoPorCiudad usando cache robusta por nombre
     * Eliminada dependencia de equals/hashCode de objetos Ciudad
     */
    private Aeropuerto obtenerAeropuertoPorCiudad(Ciudad ciudad) {
        if (ciudad == null || ciudad.getNombre() == null) return null;
        String claveCiudad = ciudad.getNombre().toLowerCase().trim();
        return cacheNombreCiudadAeropuerto.get(claveCiudad);
    }
    
    /**
/**
 * PATCH: Implementar encontrarMejorRutaConVentanasTiempo (método crítico faltante)
 */
private ArrayList<Vuelo> encontrarMejorRutaConVentanasTiempo(
        Paquete paquete, HashMap<Paquete, ArrayList<Vuelo>> solucionActual) {
    // Implementación simplificada: buscar vuelos directos principalmente
    Ciudad origen = paquete.getUbicacionActual();
    Ciudad destino = paquete.getCiudadDestino();

    if (origen == null || destino == null) return null;

    // Buscar vuelo directo
    ArrayList<Vuelo> rutaDirecta = encontrarRutaDirecta(origen, destino);
    if (rutaDirecta != null && !rutaDirecta.isEmpty()) {
        int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
        if (cabeEnCapacidad(rutaDirecta, conteoProductos)) {
            return rutaDirecta;
        }
    }

    return null; // Por simplicidad, solo vuelos directos por ahora
}

/**
 * PATCH: Implementar encontrarRutaDirecta (método crítico faltante)
 */
private ArrayList<Vuelo> encontrarRutaDirecta(Ciudad origen, Ciudad destino) {
    Aeropuerto aeropuertoOrigen = obtenerAeropuertoPorCiudad(origen);
    Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(destino);

    if (aeropuertoOrigen == null || aeropuertoDestino == null) return null;

    // Buscar vuelo directo entre aeropuertos (respetando capacidad)
    for (Vuelo vuelo : vuelos) {
        if (vuelo.getAeropuertoOrigen().equals(aeropuertoOrigen) &&
            vuelo.getAeropuertoDestino().equals(aeropuertoDestino)) {
            ArrayList<Vuelo> ruta = new ArrayList<>();
            ruta.add(vuelo);
            return ruta;
        }
    }

    return null; // No hay vuelo directo
}

/**
 * PATCH: Implementar esRutaValida (método crítico faltante)
 */
private boolean esRutaValida(Paquete paquete, ArrayList<Vuelo> ruta) {
    if (ruta == null || ruta.isEmpty()) return false;
    if (paquete == null ) return false;
    

    int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;

    // Validar capacidad de todos los vuelos en la ruta
    if (!cabeEnCapacidad(ruta, conteoProductos)) return false;

    // Validar que el primer vuelo salga del aeropuerto correcto
    if (!ruta.get(0).getAeropuertoOrigen().equals(obtenerAeropuertoPorCiudad(paquete.getUbicacionActual()))) {
        return false;
    }

    return true;
}

/**
 * PATCH: Implementar puedeAsignarConOptimizacionEspacio (método crítico faltante)
 * (versión simple: solo almacén final; si prefieres la validación temporal estricta,
 * usa la versión más abajo "puedeAsignarRespetandoFlujoTemporal")
 */
private boolean puedeAsignarConOptimizacionEspacio(Paquete paquete, ArrayList<Vuelo> ruta,
                                                   HashMap<Paquete, ArrayList<Vuelo>> solucionActual) {
    Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
    if (aeropuertoDestino == null) return false;

    int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
    int ocupacionActual = ocupacionAlmacenes.getOrDefault(aeropuertoDestino, 0);
    int capacidadMaxima = aeropuertoDestino.getAlmacen().getCapacidadMaxima();

    return (ocupacionActual + conteoProductos) <= capacidadMaxima;
}

/**
 * PATCH: Implementar actualizarCapacidadesVuelos (método crítico faltante)
 */
private void actualizarCapacidadesVuelos(ArrayList<Vuelo> ruta, int conteoProductos) {
    for (Vuelo vuelo : ruta) {
        vuelo.setCapacidadUsada(vuelo.getCapacidadUsada() + conteoProductos);
    }
}

/**
 * PATCH: Implementar incrementarOcupacionAlmacen (método crítico faltante)
 */
private void incrementarOcupacionAlmacen(Aeropuerto aeropuerto, int conteoProductos) {
    int ocupacionActual = ocupacionAlmacenes.getOrDefault(aeropuerto, 0);
    ocupacionAlmacenes.put(aeropuerto, ocupacionActual + conteoProductos);
}

/**
 * NEW: obtenerTiempoInicioPaquete corregido con ancla T0 y clamp
 */
private int obtenerTiempoInicioPaquete(Paquete paquete) {
    if (paquete == null || paquete.getFechaPedido() == null || T0 == null) {
        return 0;
    }
    long minutosDesdeT0 = ChronoUnit.MINUTES.between(T0, paquete.getFechaPedido());
    int offset = Math.floorMod(paquete.getId(), 60); // Offset por ID
    int minutoInicio = (int) (minutosDesdeT0 + offset);

    // Clamp a rango válido [0, TOTAL_MINUTOS-1]
    final int TOTAL_MINUTOS = HORIZON_DAYS * 24 * 60;
    return Math.max(0, Math.min(minutoInicio, TOTAL_MINUTOS - 1));
}

/**
 * CHANGED: calcularMargenTiempoRuta unificado sin doble conteo
 * Solo tiempoTransporte + 2h conexiones, margen vs fechaPedido-deadline
 */
private double calcularMargenTiempoRuta(Paquete paquete, ArrayList<Vuelo> ruta) {
    if (paquete == null || ruta == null) return 0.0;

    // Tiempo total de la ruta
    double tiempoTotal = 0.0;
    for (Vuelo vuelo : ruta) {
        tiempoTotal += vuelo.getTiempoTransporte();
    }
    // Añadir 2 horas por conexión
    if (ruta.size() > 1) {
        tiempoTotal += (ruta.size() - 1) * 2.0;
    }

    long horasDisponibles = ChronoUnit.HOURS.between(paquete.getFechaPedido(), paquete.getFechaLimiteEntrega());
    double margen = horasDisponibles - tiempoTotal;

    return Math.max(margen, 0.0) + 1.0; // +1 para evitar margen 0
}

/**
 * NEW: Usar flag de Constantes para decidir tipo de solución inicial
 */
public void generarSolucionInicial() {
    if (Constantes.USAR_SOLUCION_INICIAL_CODICIOSA) {
        generarSolucionInicialCodiciosa();
    } else {
        generarSolucionInicialAleatoria();
    }
}

/**
 * NEW: Generar solución inicial completamente aleatoria para probar ALNS
 */
private void generarSolucionInicialAleatoria() {
    System.out.println("=== GENERANDO SOLUCIÓN INICIAL ALEATORIA ===");
    System.out.println("Probabilidad de asignación: " + (Constantes.PROBABILIDAD_ASIGNACION_ALEATORIA * 100) + "%");

    HashMap<Paquete, ArrayList<Vuelo>> solucionActual = new HashMap<>();
    int paquetesAsignados = 0;

    // Barajar paquetes para orden aleatorio
    ArrayList<Paquete> paquetesBarajados = new ArrayList<>(paquetes);
    Collections.shuffle(paquetesBarajados, aleatorio);

    for (Paquete paquete : paquetesBarajados) {
        // Asignación aleatoria basada en probabilidad
        if (aleatorio.nextDouble() < Constantes.PROBABILIDAD_ASIGNACION_ALEATORIA) {
            ArrayList<Vuelo> rutaAleatoria = generarRutaAleatoria(paquete);

            if (rutaAleatoria != null && !rutaAleatoria.isEmpty()) {
                int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;

                // Validación básica de capacidad
                if (cabeEnCapacidad(rutaAleatoria, conteoProductos)) {
                    Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
                    if (aeropuertoDestino != null &&
                        puedeAsignarConOptimizacionEspacio(paquete, rutaAleatoria, solucionActual)) {

                        solucionActual.put(paquete, rutaAleatoria);
                        actualizarCapacidadesVuelos(rutaAleatoria, conteoProductos);
                        incrementarOcupacionAlmacen(aeropuertoDestino, conteoProductos);
                        paquetesAsignados++;
                    }
                }
            }
        }
    }

    // Calcular el peso/costo de esta solución
    int pesoSolucion = calcularPesoSolucion(solucionActual);

    // Almacenar la solución con su peso
    solucion.put(solucionActual, pesoSolucion);

    System.out.println("Solución inicial aleatoria generada: " + paquetesAsignados + "/" + paquetes.size() + " paquetes asignados");
    System.out.println("Peso de la solución: " + pesoSolucion);
}

/**
 * NEW: Generar una ruta completamente aleatoria para testing
 */
private ArrayList<Vuelo> generarRutaAleatoria(Paquete paquete) {
    Ciudad origen = paquete.getUbicacionActual();
    Ciudad destino = paquete.getCiudadDestino();

    if (origen == null || destino == null) return null;

    Aeropuerto aeropuertoOrigen = obtenerAeropuertoPorCiudad(origen);
    Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(destino);

    if (aeropuertoOrigen == null || aeropuertoDestino == null) return null;

    // Intentar encontrar cualquier ruta válida (directo prioritario)
    ArrayList<Vuelo> rutaDirecta = encontrarRutaDirecta(origen, destino);
    if (rutaDirecta != null && !rutaDirecta.isEmpty()) {
        return rutaDirecta;
    }

    // Si no hay directo, intentar ruta con 1 escala aleatoria
    ArrayList<Aeropuerto> aeropuertosBarajados = new ArrayList<>(aeropuertos);
    Collections.shuffle(aeropuertosBarajados, aleatorio);

    for (int i = 0; i < Math.min(5, aeropuertosBarajados.size()); i++) { // Máximo 5 intentos
        Aeropuerto intermedio = aeropuertosBarajados.get(i);
        if (intermedio.equals(aeropuertoOrigen) || intermedio.equals(aeropuertoDestino)) continue;

        ArrayList<Vuelo> tramo1 = encontrarRutaDirecta(origen, intermedio.getCiudad());
        ArrayList<Vuelo> tramo2 = encontrarRutaDirecta(intermedio.getCiudad(), destino);

        if (tramo1 != null && tramo2 != null && !tramo1.isEmpty() && !tramo2.isEmpty()) {
            ArrayList<Vuelo> ruta = new ArrayList<>();
            ruta.addAll(tramo1);
            ruta.addAll(tramo2);
            return ruta;
        }
    }

    return null; // No se pudo generar ruta
}

/**
 * RENAMED: Método greedy original (antes generateInitialSolution)
 */
private void generarSolucionInicialCodiciosa() {
    System.out.println("=== GENERANDO SOLUCIÓN INICIAL GREEDY ===");

    // Crear estructura de solución temporal
    HashMap<Paquete, ArrayList<Vuelo>> solucionActual = new HashMap<>();

    // Ordenar paquetes con un componente aleatorio
    ArrayList<Paquete> paquetesOrdenados = new ArrayList<>(paquetes);

    // Estrategia de ordenamiento (0 = por deadline)
    int estrategiaOrdenamiento = 0;

    switch (estrategiaOrdenamiento) {
        case 0:
            System.out.println("Estrategia de ordenamiento: Por deadline optimizado");
            paquetesOrdenados.sort((p1, p2) -> p1.getFechaLimiteEntrega().compareTo(p2.getFechaLimiteEntrega()));
            break;
        case 1:
            System.out.println("Estrategia de ordenamiento: Por prioridad");
            paquetesOrdenados.sort((p1, p2) -> Double.compare(p2.getPrioridad(), p1.getPrioridad()));
            break;
        case 2:
            System.out.println("Estrategia de ordenamiento: Por distancia entre continentes");
            paquetesOrdenados.sort((p1, p2) -> {
                boolean p1ContinenteDiferente = p1.getUbicacionActual().getContinente() != p1.getCiudadDestino().getContinente();
                boolean p2ContinenteDiferente = p2.getUbicacionActual().getContinente() != p2.getCiudadDestino().getContinente();
                return Boolean.compare(p1ContinenteDiferente, p2ContinenteDiferente);
            });
            break;
        case 3:
            System.out.println("Estrategia de ordenamiento: Por margen de tiempo");
            paquetesOrdenados.sort((p1, p2) -> {
                LocalDateTime ahora = LocalDateTime.now();
                long margenP1 = ChronoUnit.HOURS.between(ahora, p1.getFechaLimiteEntrega());
                long margenP2 = ChronoUnit.HOURS.between(ahora, p2.getFechaLimiteEntrega());
                return Long.compare(margenP1, margenP2);
            });
            break;
        case 4:
            System.out.println("Estrategia de ordenamiento: Aleatorio");
            Collections.shuffle(paquetesOrdenados, aleatorio);
            break;
    }

    // Usar algoritmo optimizado con ventanas de tiempo y reasignación dinámica
    int paquetesAsignados = generarSolucionOptimizada(solucionActual, paquetesOrdenados);

    // Calcular el peso/costo de esta solución
    int pesoSolucion = calcularPesoSolucion(solucionActual);

    // Almacenar la solución con su peso
    solucion.put(solucionActual, pesoSolucion);

    System.out.println("Solución inicial generada: " + paquetesAsignados + "/" + paquetes.size() + " paquetes asignados");
    System.out.println("Peso de la solución: " + pesoSolucion);
}

/**
 * Genera una solución optimizada usando ventanas de tiempo y reasignación dinámica
 */
private int generarSolucionOptimizada(HashMap<Paquete, ArrayList<Vuelo>> solucionActual,
                                      ArrayList<Paquete> paquetesOrdenados) {
    int paquetesAsignados = 0;
    int maxIteraciones = 3; // Máximo número de iteraciones para reasignación

    System.out.println("Iniciando algoritmo optimizado con " + maxIteraciones + " iteraciones...");

    for (int iteracion = 0; iteracion < maxIteraciones; iteracion++) {
        if (iteracion > 0) {
            System.out.println("Iteración " + iteracion + " - Reasignación dinámica...");
            // En iteraciones posteriores, intentar reasignar paquetes no asignados
            ArrayList<Paquete> noAsignados = new ArrayList<>();
            for (Paquete paquete : paquetesOrdenados) {
                if (!solucionActual.containsKey(paquete)) noAsignados.add(paquete);
            }
            paquetesOrdenados = noAsignados;
        }

        int asignadosIteracion = 0;

        for (Paquete paquete : paquetesOrdenados) {
            Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
            if (aeropuertoDestino == null) continue;

            int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;

            // Intentar asignar el paquete usando diferentes estrategias
            ArrayList<Vuelo> mejorRuta = encontrarMejorRutaConVentanasTiempo(paquete, solucionActual);

            if (mejorRuta != null && esRutaValida(paquete, mejorRuta)) {
                // Primero validar sin actualizar capacidades
                if (puedeAsignarConOptimizacionEspacio(paquete, mejorRuta, solucionActual)) {
                    // Commit
                    solucionActual.put(paquete, mejorRuta);
                    paquetesAsignados++;
                    asignadosIteracion++;

                    actualizarCapacidadesVuelos(mejorRuta, conteoProductos);
                    incrementarOcupacionAlmacen(aeropuertoDestino, conteoProductos);

                    if (iteracion > 0) {
                        System.out.println("  Reasignado paquete " + paquete.getId() + " en iteración " + iteracion);
                    }
                }
            }
        }

        System.out.println("  Iteración " + iteracion + " completada: " + asignadosIteracion + " paquetes asignados");

        // Si no se asignaron paquetes en esta iteración, salimos
        if (asignadosIteracion == 0) break;
    }

    return paquetesAsignados;
}

/**
 * Encuentra la mejor ruta considerando ventanas de tiempo y liberación de espacio con fallback
 */
private ArrayList<Vuelo> encontrarMejorRutaConVentanasTiempoAvanzada(
        Paquete paquete, HashMap<Paquete, ArrayList<Vuelo>> solucionActual, boolean fallback) {
    // Intentar con el método general existente
    ArrayList<Vuelo> rutaOriginal = encontrarMejorRuta(paquete);

    // Si no funciona, intentar con salidas retrasadas
    if (rutaOriginal == null || !puedeAsignarConOptimizacionEspacio(paquete, rutaOriginal, solucionActual)) {
        return encontrarRutaConSalidaRetrasada(paquete, solucionActual);
    }
    return rutaOriginal;
}
 
/**
 * Encuentra una ruta con horarios de salida retrasados para aprovechar liberación de espacio
 */
private ArrayList<Vuelo> encontrarRutaConSalidaRetrasada(Paquete paquete,
                                                         HashMap<Paquete, ArrayList<Vuelo>> solucionActual) {
    // Intentar con diferentes horarios de salida (cada 2 horas)
    for (int delayHours = 2; delayHours <= 12; delayHours += 2) {
        Paquete paqueteRetrasado = crearPaqueteRetrasado(paquete, delayHours);
        if (paqueteRetrasado == null) continue;

        ArrayList<Vuelo> ruta = encontrarMejorRuta(paqueteRetrasado);
        if (ruta != null && puedeAsignarConOptimizacionEspacio(paqueteRetrasado, ruta, solucionActual)) {
            return ruta;
        }
    }
    return null;
}

/**
 * Crea un paquete con horario de salida retrasado para probar diferentes ventanas de tiempo
 */
private Paquete crearPaqueteRetrasado(Paquete original, int horasRetraso) {
    LocalDateTime fechaPedidoRetrasada = original.getFechaPedido().plusHours(horasRetraso);
    if (fechaPedidoRetrasada.isAfter(original.getFechaLimiteEntrega())) {
        return null; // El retraso violaría el deadline
    }

    Paquete retrasado = new Paquete();
    retrasado.setId(original.getId());
    retrasado.setCliente(original.getCliente());
    retrasado.setCiudadDestino(original.getCiudadDestino());
    retrasado.setFechaPedido(fechaPedidoRetrasada);
    retrasado.setFechaLimiteEntrega(original.getFechaLimiteEntrega());
    retrasado.setUbicacionActual(original.getUbicacionActual());
    retrasado.setProductos(original.getProductos());
    retrasado.setPrioridad(original.getPrioridad());
    return retrasado;
}

/**
 * Validación temporal completa de un único paquete (sin tocar la matriz global)
 */
private boolean validarFlujoTemporalPaquete(Paquete paquete, ArrayList<Vuelo> ruta) {
    if (ruta == null || ruta.isEmpty()) {
        Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
        if (aeropuertoDestino == null) return false;

        int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
        int ocupacionActual = ocupacionAlmacenes.getOrDefault(aeropuertoDestino, 0);
        int capacidadMaxima = aeropuertoDestino.getAlmacen().getCapacidadMaxima();

        return (ocupacionActual + conteoProductos) <= capacidadMaxima;
    }
    return puedeAsignarRespetandoFlujoTemporal(paquete, ruta);
}

/**
 * Simula el flujo temporal del paquete en un snapshot sin alterar las matrices reales
 */
private boolean puedeAsignarRespetandoFlujoTemporal(Paquete paquete, ArrayList<Vuelo> ruta) {
    int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
    int minutoActual = obtenerTiempoInicioPaquete(paquete);

    Map<Aeropuerto, int[]> snapshot = crearSnapshotTemporal();
    Aeropuerto aeropuertoActual = obtenerAeropuertoPorCiudad(paquete.getUbicacionActual());
    if (aeropuertoActual == null) return false;

    // 1) Espera 2h en origen
    int espera = Constantes.HORAS_MAX_RECOGIDA_CLIENTE * 60; // 2h
    if (!agregarOcupacionTemporalASnapshot(snapshot, aeropuertoActual, minutoActual, espera, conteoProductos)) {
        return false;
    }
    minutoActual += espera;

    // 2) Vuelos y escalas
    for (int i = 0; i < ruta.size(); i++) {
        Vuelo vuelo = ruta.get(i);

        // Capacidad de vuelo
        if (vuelo.getCapacidadUsada() + conteoProductos > vuelo.getCapacidadMaxima()) return false;

        // Tiempo de vuelo
        int duracionVuelo = (int) (vuelo.getTiempoTransporte() * 60);
        minutoActual += duracionVuelo;

        Aeropuerto llegada = vuelo.getAeropuertoDestino();

        if (i < ruta.size() - 1) {
            int conexion = Constantes.HORAS_MAX_RECOGIDA_CLIENTE * 60; // 2h de conexión
            if (!agregarOcupacionTemporalASnapshot(snapshot, llegada, minutoActual, conexion, conteoProductos)) {
                return false;
            }
            minutoActual += conexion;
        } else {
            // Destino final: 2h de espera para retiro del cliente
            int pickup = Constantes.HORAS_MAX_RECOGIDA_CLIENTE * 60;
            if (!agregarOcupacionTemporalASnapshot(snapshot, llegada, minutoActual, pickup, conteoProductos)) {
                return false;
            }
        }
        aeropuertoActual = llegada;
    }
    return true;
}

/**
 * Crear snapshot temporal de ocupación
 */
private Map<Aeropuerto, int[]> crearSnapshotTemporal() {
    Map<Aeropuerto, int[]> snapshot = new HashMap<>();
    final int TOTAL_MINUTOS = HORIZON_DAYS * 24 * 60;

    for (Aeropuerto a : aeropuertos) {
        int[] original = ocupacionTemporalAlmacenes.get(a);
        int[] copia = new int[TOTAL_MINUTOS];
        System.arraycopy(original, 0, copia, 0, Math.min(original.length, TOTAL_MINUTOS));
        snapshot.put(a, copia);
    }
    return snapshot;
}

/**
 * Agregar ocupación temporal en snapshot
 */
private boolean agregarOcupacionTemporalASnapshot(Map<Aeropuerto, int[]> snapshot, Aeropuerto aeropuerto,
                                                  int minutoInicio, int duracionMinutos, int conteoProductos) {
    if (aeropuerto == null || aeropuerto.getAlmacen() == null) return false;

    int[] array = snapshot.get(aeropuerto);
    if (array == null) return false;

    int capacidadMaxima = aeropuerto.getAlmacen().getCapacidadMaxima();
    final int TOTAL_MINUTOS = HORIZON_DAYS * 24 * 60;

    for (int m = minutoInicio; m < Math.min(minutoInicio + duracionMinutos, TOTAL_MINUTOS); m++) {
        if (m < 0 || m >= array.length) continue;
        if (array[m] + conteoProductos > capacidadMaxima) return false;
        array[m] += conteoProductos;
    }
    return true;
}
/**
 * Encuentra la mejor ruta posible para un paquete,
 * considerando rutas directas, con una escala y con dos escalas.
 */
private ArrayList<Vuelo> encontrarMejorRuta(Paquete paquete) {
    Ciudad origen = paquete.getUbicacionActual();
    Ciudad destino = paquete.getCiudadDestino();
    
    // Si ya está en la ciudad destino, no necesita vuelos
    if (origen.equals(destino)) {
        return new ArrayList<>();
    }
    
    // Introducir aleatoriedad en el orden de búsqueda de rutas
    ArrayList<ArrayList<Vuelo>> rutasValidas = new ArrayList<>();
    ArrayList<String> tiposRuta = new ArrayList<>();
    ArrayList<Double> puntajesRuta = new ArrayList<>(); // Puntajes para cada ruta
    
    // 1. Buscar ruta directa
    ArrayList<Vuelo> rutaDirecta = encontrarRutaDirecta(origen, destino);
    if (rutaDirecta != null && esRutaValida(paquete, rutaDirecta)) {
        rutasValidas.add(rutaDirecta);
        tiposRuta.add("directa");
        
        double puntajeDirecta = calcularMargenTiempoRuta(paquete, rutaDirecta);
        puntajesRuta.add(puntajeDirecta);
    }
    
    // 2. Buscar ruta con una escala
    ArrayList<Vuelo> rutaUnaEscala = encontrarRutaUnaEscala(origen, destino);
    if (rutaUnaEscala != null && esRutaValida(paquete, rutaUnaEscala)) {
        rutasValidas.add(rutaUnaEscala);
        tiposRuta.add("una escala");
        
        double puntajeUnaEscala = calcularMargenTiempoRuta(paquete, rutaUnaEscala);
        puntajesRuta.add(puntajeUnaEscala);
    }
    
    // 3. Buscar ruta con dos escalas
    ArrayList<Vuelo> rutaDosEscalas = encontrarRutaDosEscalas(origen, destino);
    if (rutaDosEscalas != null && esRutaValida(paquete, rutaDosEscalas)) {
        rutasValidas.add(rutaDosEscalas);
        tiposRuta.add("dos escalas");
        
        double puntajeDosEscalas = calcularMargenTiempoRuta(paquete, rutaDosEscalas);
        puntajesRuta.add(puntajeDosEscalas);
    }
    
    // Si no hay rutas válidas
    if (rutasValidas.isEmpty()) {
        return null;
    }
    
    // Seleccionar una ruta basada en probabilidad ponderada por margen de tiempo
    int totalRutas = rutasValidas.size();
    int indiceSeleccionado;
    
    if (totalRutas > 1) {
        double puntajeTotal = 0;
        for (double p : puntajesRuta) {
            puntajeTotal += p;
        }
        
        if (puntajeTotal > 0) {
            double rand = aleatorio.nextDouble() * puntajeTotal;
            double acumulado = 0;
            indiceSeleccionado = 0;
            
            for (int i = 0; i < puntajesRuta.size(); i++) {
                acumulado += puntajesRuta.get(i);
                if (rand <= acumulado) {
                    indiceSeleccionado = i;
                    break;
                }
            }
        } else {
            indiceSeleccionado = aleatorio.nextInt(totalRutas);
        }
    } else {
        indiceSeleccionado = 0;
    }
    
    return rutasValidas.get(indiceSeleccionado);
}

/**
 * Busca una ruta con exactamente una escala intermedia.
 */
private ArrayList<Vuelo> encontrarRutaUnaEscala(Ciudad origen, Ciudad destino) {
    Aeropuerto aeropuertoOrigen = obtenerAeropuertoPorCiudad(origen);
    Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(destino);
    
    if (aeropuertoOrigen == null || aeropuertoDestino == null) return null;
    
    ArrayList<Aeropuerto> posiblesEscalas = new ArrayList<>();
    for (Aeropuerto a : aeropuertos) {
        if (!a.equals(aeropuertoOrigen) && !a.equals(aeropuertoDestino)) {
            posiblesEscalas.add(a);
        }
    }
    Collections.shuffle(posiblesEscalas, aleatorio);
    
    for (Aeropuerto escala : posiblesEscalas) {
        Vuelo primerVuelo = null;
        for (Vuelo v : vuelos) {
            if (v.getAeropuertoOrigen().equals(aeropuertoOrigen) &&
                v.getAeropuertoDestino().equals(escala) &&
                v.getCapacidadUsada() < v.getCapacidadMaxima()) {
                primerVuelo = v;
                break;
            }
        }
        if (primerVuelo == null) continue;
        
        Vuelo segundoVuelo = null;
        for (Vuelo v : vuelos) {
            if (v.getAeropuertoOrigen().equals(escala) &&
                v.getAeropuertoDestino().equals(aeropuertoDestino) &&
                v.getCapacidadUsada() < v.getCapacidadMaxima()) {
                segundoVuelo = v;
                break;
            }
        }
        
        if (segundoVuelo != null) {
            ArrayList<Vuelo> ruta = new ArrayList<>();
            ruta.add(primerVuelo);
            ruta.add(segundoVuelo);
            return ruta;
        }
    }
    return null;
}

/**
 * Busca una ruta con exactamente dos escalas intermedias.
 */
private ArrayList<Vuelo> encontrarRutaDosEscalas(Ciudad origen, Ciudad destino) {
    Aeropuerto aeropuertoOrigen = obtenerAeropuertoPorCiudad(origen);
    Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(destino);
    
    if (aeropuertoOrigen == null || aeropuertoDestino == null) return null;
    
    ArrayList<Aeropuerto> primerasEscalas = new ArrayList<>();
    for (Aeropuerto a : aeropuertos) {
        if (!a.equals(aeropuertoOrigen) && !a.equals(aeropuertoDestino)) {
            primerasEscalas.add(a);
        }
    }
    Collections.shuffle(primerasEscalas, aleatorio);
    int maxPrimeras = Math.min(10, primerasEscalas.size());
    
    for (int i = 0; i < maxPrimeras; i++) {
        Aeropuerto primeraEscala = primerasEscalas.get(i);
        
        ArrayList<Aeropuerto> segundasEscalas = new ArrayList<>();
        for (Aeropuerto a : aeropuertos) {
            if (!a.equals(aeropuertoOrigen) && 
                !a.equals(aeropuertoDestino) &&
                !a.equals(primeraEscala)) {
                segundasEscalas.add(a);
            }
        }
        Collections.shuffle(segundasEscalas, aleatorio);
        int maxSegundas = Math.min(10, segundasEscalas.size());
        
        for (int j = 0; j < maxSegundas; j++) {
            Aeropuerto segundaEscala = segundasEscalas.get(j);
            
            Vuelo primerVuelo = null;
            for (Vuelo v : vuelos) {
                if (v.getAeropuertoOrigen().equals(aeropuertoOrigen) &&
                    v.getAeropuertoDestino().equals(primeraEscala) &&
                    v.getCapacidadUsada() < v.getCapacidadMaxima()) {
                    primerVuelo = v;
                    break;
                }
            }
            if (primerVuelo == null) continue;
            
            Vuelo segundoVuelo = null;
            for (Vuelo v : vuelos) {
                if (v.getAeropuertoOrigen().equals(primeraEscala) &&
                    v.getAeropuertoDestino().equals(segundaEscala) &&
                    v.getCapacidadUsada() < v.getCapacidadMaxima()) {
                    segundoVuelo = v;
                    break;
                }
            }
            if (segundoVuelo == null) continue;
            
            Vuelo tercerVuelo = null;
            for (Vuelo v : vuelos) {
                if (v.getAeropuertoOrigen().equals(segundaEscala) &&
                    v.getAeropuertoDestino().equals(aeropuertoDestino) &&
                    v.getCapacidadUsada() < v.getCapacidadMaxima()) {
                    tercerVuelo = v;
                    break;
                }
            }
            
            if (tercerVuelo != null) {
                ArrayList<Vuelo> ruta = new ArrayList<>();
                ruta.add(primerVuelo);
                ruta.add(segundoVuelo);
                ruta.add(tercerVuelo);
                
                double tiempoTotal = primerVuelo.getTiempoTransporte() +
                                     segundoVuelo.getTiempoTransporte() +
                                     tercerVuelo.getTiempoTransporte();
                tiempoTotal += 2.0; // penalización por conexiones
                
                if (tiempoTotal > Constantes.TIEMPO_MAX_ENTREGA_DIFERENTE_CONTINENTE * 24) {
                    continue;
                }
                return ruta;
            }
        }
    }
    return null;
}

    /**
     * Inicializa el mapa de ocupación de almacenes.
     * Cada aeropuerto de destino inicia con 0 paquetes asignados.
     */
    private void inicializarOcupacionAlmacenes() {
        for (Aeropuerto aeropuerto : aeropuertos) {
            ocupacionAlmacenes.put(aeropuerto, 0);
        }
    }
    
    /**
     * Inicializa la matriz temporal de ocupación de almacenes.
     * Cada aeropuerto tiene un array para 4 días (HORIZON_DAYS * 24h * 60min).
     */
    private void inicializarOcupacionTemporalAlmacenes() {
        final int TOTAL_MINUTOS = HORIZON_DAYS * 24 * 60; // 5760 minutos (4 días)
        for (Aeropuerto aeropuerto : aeropuertos) {
            ocupacionTemporalAlmacenes.put(aeropuerto, new int[TOTAL_MINUTOS]);
        }
    }
    
    /**
     * Valida si la solución actual es válida
     */
    public boolean esSolucionValida() {
        if (solucion.isEmpty()) {
            return false;
        }
        
        // Obtener la solución actual
        HashMap<Paquete, ArrayList<Vuelo>> solucionActual = solucion.keySet().iterator().next();
        
        // Verificar que todos los paquetes asignados tengan rutas válidas
        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : solucionActual.entrySet()) {
            Paquete paquete = entrada.getKey();
            ArrayList<Vuelo> ruta = entrada.getValue();
            
            if (!esRutaValida(paquete, ruta)) {
                return false;
            }
        }
        
        // Validación temporal de capacidades de almacenes
        if (!esSolucionTemporalValida(solucionActual)) {
            System.out.println("La solución viola las restricciones de capacidad temporal de almacenes");
            return false;
        }
        
        return true;
    }
    
    /**
     * Calcula el peso/puntaje de una solución
     */
    private int calcularPesoSolucion(HashMap<Paquete, ArrayList<Vuelo>> mapaSolucion) {
        // El peso de la solución considera múltiples factores:
        // 1. Número total de paquetes asignados (maximizar)
        // 2. Número total de productos transportados (maximizar)
        // 3. Tiempo total de entrega (minimizar)
        // 4. Utilización de capacidad de vuelos (maximizar)
        // 5. Cumplimiento de deadlines (maximizar)
        // 6. Margen de seguridad antes de deadline (maximizar)
        
        int totalPaquetes = mapaSolucion.size();
        int totalProductos = 0;
        double tiempoTotalEntrega = 0;
        int entregasATiempo = 0;
        double utilizacionCapacidadTotal = 0;
        int totalVuelosUsados = 0;
        double margenEntregaTotal = 0; // Margen total antes del deadline
        
        // Calcular métricas
        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : mapaSolucion.entrySet()) {
            Paquete paquete = entrada.getKey();
            ArrayList<Vuelo> ruta = entrada.getValue();
            
            // Contar productos en este paquete
            int productosEnPaquete = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
            totalProductos += productosEnPaquete;
            
            // Tiempo total de la ruta
            double tiempoRuta = 0;
            for (Vuelo vuelo : ruta) {
                tiempoRuta += vuelo.getTiempoTransporte();
                utilizacionCapacidadTotal += (double) vuelo.getCapacidadUsada() / vuelo.getCapacidadMaxima();
                totalVuelosUsados++;
            }
            
            // Añadir penalización por conexiones
            if (ruta.size() > 1) {
                tiempoRuta += (ruta.size() - 1) * 2.0; // 2 horas por cada conexión
            }
            
            tiempoTotalEntrega += tiempoRuta;
            
            // Verificar si llega a tiempo y calcular margen
            if (seRespetaDeadline(paquete, ruta)) {
                entregasATiempo++;
                
                // Calcular margen de tiempo antes del deadline (en horas)
                LocalDateTime entregaEstimada = paquete.getFechaPedido().plusHours((long)tiempoRuta);
                double horasMargen = ChronoUnit.HOURS.between(entregaEstimada, paquete.getFechaLimiteEntrega());
                margenEntregaTotal += horasMargen;
            }
        }
        
        // Fórmula de peso que combina múltiples objetivos
        double tiempoPromedioEntrega = totalPaquetes > 0 ? tiempoTotalEntrega / totalPaquetes : 0;
        double utilizacionCapacidadPromedio = totalVuelosUsados > 0 ? utilizacionCapacidadTotal / totalVuelosUsados : 0;
        double tasaATiempo = totalPaquetes > 0 ? (double) entregasATiempo / totalPaquetes : 0;
        double margenPromedioEntrega = entregasATiempo > 0 ? margenEntregaTotal / entregasATiempo : 0;
        
        // Peso final con énfasis extremo en entregas a tiempo
        int peso = (int) (
            totalPaquetes * 500 +             // Más paquetes asignados = mejor
            totalProductos * 50 +             // Más productos transportados = mejor
            tasaATiempo * 5000 +              // Entregas a tiempo con MÁXIMA prioridad
            Math.min(margenPromedioEntrega * 100, 1000) + // Premiar margen de seguridad (máx 1000)
            utilizacionCapacidadPromedio * 200 -    // Mayor utilización = mejor
            tiempoPromedioEntrega * 100       // Menos tiempo promedio = mejor
        );
        
        // Penalización SEVERA si hay entregas tardías
        if (tasaATiempo < 1.0) {
            // Reducir drásticamente el peso si hay entregas tardías
            peso = (int)(peso * Math.pow(tasaATiempo, 3)); // Penalización cúbica
        }
        
        return peso;
    }
    
    /**
     * Verifica si se respeta el deadline de un paquete en una ruta
     */
    private boolean seRespetaDeadline(Paquete paquete, ArrayList<Vuelo> ruta) {
        double tiempoTotal = 0;
        
        // Solo usar tiempoTransporte de vuelos
        for (Vuelo vuelo : ruta) {
            tiempoTotal += vuelo.getTiempoTransporte();
        }
        
        // Añadir penalización por conexiones (2 horas por conexión)
        if (ruta.size() > 1) {
            tiempoTotal += (ruta.size() - 1) * 2.0;
        }
        
        // Usar validación de promesas MoraPack
        if (!validarPromesaEntregaMoraPack(paquete, tiempoTotal)) {
            return false; // Excede promesas MoraPack
        }
        
        // Factor de seguridad aleatorio (1-10%) para asegurar entregas a tiempo
        double margenSeguridad = 0.0;
        if (aleatorio != null) {
            Ciudad origen = paquete.getUbicacionActual();
            Ciudad destino = paquete.getCiudadDestino();
            boolean rutaMismoContinente = (origen != null && destino != null) && 
                                        origen.getContinente() == destino.getContinente();
            
            int factorComplejidad = ruta.size() + (rutaMismoContinente ? 0 : 2);
            margenSeguridad = 0.01 * (1 + aleatorio.nextInt(factorComplejidad * 3));
            tiempoTotal = tiempoTotal * (1.0 + margenSeguridad);
        }
        
        // Calcular tiempo límite desde fechaPedido
        long horasHastaDeadline = ChronoUnit.HOURS.between(paquete.getFechaPedido(), paquete.getFechaLimiteEntrega());
        
        return tiempoTotal <= horasHastaDeadline;
    }
    
    /**
     * Valida las promesas de entrega de MoraPack
     */
    private boolean validarPromesaEntregaMoraPack(Paquete paquete, double tiempoTotalHoras) {
        // Verificar promesa MoraPack según continentes
        Ciudad origen = paquete.getUbicacionActual();
        Ciudad destino = paquete.getCiudadDestino();
        
        if (origen == null || destino == null) {
            System.err.println("Error: origen o destino nulo para paquete " + paquete.getId());
            return false;
        }
        
        boolean rutaMismoContinente = origen.getContinente() == destino.getContinente();
        long horasPromesaMoraPack = rutaMismoContinente ? 48 : 72; // 2 días intra / 3 días inter
        
        // Verificar promesa MoraPack
        if (tiempoTotalHoras > horasPromesaMoraPack) {
            if (DEBUG_MODE) {
                System.out.println("VIOLACIÓN PROMESA MORAPACK - Paquete " + paquete.getId() + 
                    ": " + tiempoTotalHoras + "h > " + horasPromesaMoraPack + "h (" + 
                    (rutaMismoContinente ? "mismo continente" : "diferentes continentes") + ")");
            }
            return false;
        }
        
        // Verificar deadline específico del cliente
        long horasHastaDeadline = ChronoUnit.HOURS.between(paquete.getFechaPedido(), paquete.getFechaLimiteEntrega());
        
        if (tiempoTotalHoras > horasHastaDeadline) {
            if (DEBUG_MODE) {
                System.out.println("VIOLACIÓN DEADLINE CLIENTE - Paquete " + paquete.getId() + 
                    ": " + tiempoTotalHoras + "h > " + horasHastaDeadline + "h disponibles");
            }
            return false;
        }
        
        return true; // Cumple todas las promesas
    }
    
    /**
     * Imprime una descripción detallada de la solución actual.
     */
    public void imprimirDescripcionSolucion(int nivelDetalle) {
        if (solucion.isEmpty()) {
            System.out.println("No hay solución disponible para mostrar.");
            return;
        }
        
        // Obtener la solución actual y su peso
        HashMap<Paquete, ArrayList<Vuelo>> solucionActual = solucion.keySet().iterator().next();
        int pesoSolucion = solucion.get(solucionActual);
        
        // Calcular total de productos
        int totalProductosAsignados = 0;
        int totalProductosEnSistema = 0;
        for (Paquete paquete : paquetes) {
            int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
            totalProductosEnSistema += conteoProductos;
            if (solucionActual.containsKey(paquete)) {
                totalProductosAsignados += conteoProductos;
            }
        }
        
        // Estadísticas generales
        System.out.println("\n========== DESCRIPCIÓN DE LA SOLUCIÓN ==========");
        System.out.println("Peso de la solución: " + pesoSolucion);
        System.out.println("Paquetes asignados: " + solucionActual.size() + "/" + paquetes.size());
        System.out.println("Productos transportados: " + totalProductosAsignados + "/" + totalProductosEnSistema);
        
        // Calcular estadísticas adicionales
        int rutasDirectas = 0;
        int rutasUnaEscala = 0;
        int rutasDosEscalas = 0;
        int rutasMismoContinente = 0;
        int rutasDiferentesContinentes = 0;
        int entregasATiempo = 0;
        
        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : solucionActual.entrySet()) {
            Paquete paquete = entrada.getKey();
            ArrayList<Vuelo> ruta = entrada.getValue();
            
            // Contar tipos de rutas
            if (ruta.size() == 1) rutasDirectas++;
            else if (ruta.size() == 2) rutasUnaEscala++;
            else if (ruta.size() == 3) rutasDosEscalas++;
            
            // Contar rutas por continente
            if (paquete.getUbicacionActual().getContinente() == paquete.getCiudadDestino().getContinente()) {
                rutasMismoContinente++;
            } else {
                rutasDiferentesContinentes++;
            }
            
            // Contar entregas a tiempo
            if (seRespetaDeadline(paquete, ruta)) {
                entregasATiempo++;
            }
        }
        
        // Mostrar estadísticas detalladas
        System.out.println("\n----- Estadísticas de Rutas -----");
        System.out.println("Rutas directas: " + rutasDirectas + " (" + formatearPorcentaje(rutasDirectas, solucionActual.size()) + "%)");
        System.out.println("Rutas con 1 escala: " + rutasUnaEscala + " (" + formatearPorcentaje(rutasUnaEscala, solucionActual.size()) + "%)");
        System.out.println("Rutas con 2 escalas: " + rutasDosEscalas + " (" + formatearPorcentaje(rutasDosEscalas, solucionActual.size()) + "%)");
        System.out.println("Rutas en mismo continente: " + rutasMismoContinente + " (" + formatearPorcentaje(rutasMismoContinente, solucionActual.size()) + "%)");
        System.out.println("Rutas entre continentes: " + rutasDiferentesContinentes + " (" + formatearPorcentaje(rutasDiferentesContinentes, solucionActual.size()) + "%)");
        System.out.println("Entregas a tiempo: " + entregasATiempo + " (" + formatearPorcentaje(entregasATiempo, solucionActual.size()) + "% de asignados)");
        System.out.println("Entregas a tiempo del total: " + entregasATiempo + "/" + paquetes.size() + " (" + formatearPorcentaje(entregasATiempo, paquetes.size()) + "%)");
        
        int paquetesNoAsignados = paquetes.size() - solucionActual.size();
        if (paquetesNoAsignados > 0) {
            System.out.println("Paquetes no asignados: " + paquetesNoAsignados + "/" + paquetes.size() + " (" + formatearPorcentaje(paquetesNoAsignados, paquetes.size()) + "%)");
            System.out.println("Razón principal: Capacidad de almacenes insuficiente");
        }
        
        System.out.println("\n==================================================");
    }
    
    /**
     * Formatea un porcentaje
     */
    private String formatearPorcentaje(int valor, int total) {
        if (total == 0) return "0.0";
        return String.format("%.1f", (valor * 100.0) / total);
    }
    
    /**
     * Valida temporalmente si una solución respeta las capacidades de almacenes
     */
    public boolean esSolucionTemporalValida(HashMap<Paquete, ArrayList<Vuelo>> mapaSolucion) {
        // Reinicializar matriz temporal
        inicializarOcupacionTemporalAlmacenes();
        
        // Simular el flujo de cada paquete
        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : mapaSolucion.entrySet()) {
            Paquete paquete = entrada.getKey();
            ArrayList<Vuelo> ruta = entrada.getValue();
            
            if (!simularFlujoPaquete(paquete, ruta)) {
                return false; // Se encontró una violación de capacidad
            }
        }
        
        return true; // No hay violaciones de capacidad
    }
    
    /**
     * Simula el flujo temporal de un paquete a través de su ruta asignada.
     */
    private boolean simularFlujoPaquete(Paquete paquete, ArrayList<Vuelo> ruta) {
        if (ruta == null || ruta.isEmpty()) {
            // El paquete ya está en destino, cliente tiene 2 horas para recoger
            Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
            int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
            return agregarOcupacionTemporal(aeropuertoDestino, 0, Constantes.HORAS_MAX_RECOGIDA_CLIENTE * 60, conteoProductos);
        }
        
        int minutoActual = obtenerTiempoInicioPaquete(paquete);
        int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
        
        for (int i = 0; i < ruta.size(); i++) {
            Vuelo vuelo = ruta.get(i);
            Aeropuerto aeropuertoSalida = vuelo.getAeropuertoOrigen();
            Aeropuerto aeropuertoLlegada = vuelo.getAeropuertoDestino();
            
            // FASE 1: Espera en aeropuerto de origen
            int tiempoEspera = 120; // 2 horas
            if (!agregarOcupacionTemporal(aeropuertoSalida, minutoActual, tiempoEspera, conteoProductos)) {
                System.out.println("Violación de capacidad en " + aeropuertoSalida.getCiudad().getNombre() + 
                                  " en minuto " + minutoActual + " (fase de espera) para paquete " + paquete.getId());
                return false;
            }
            
            // FASE 2: Vuelo despega
            int minutoInicioVuelo = minutoActual + tiempoEspera;
            int duracionVuelo = (int)(vuelo.getTiempoTransporte() * 60);
            
            // FASE 3: Vuelo llega
            int minutoLlegada = minutoInicioVuelo + duracionVuelo;
            
            // FASE 4: Productos en destino
            int duracionEstancia;
            if (i < ruta.size() - 1) {
                duracionEstancia = 120; // 2 horas de conexión
            } else {
                duracionEstancia = Constantes.HORAS_MAX_RECOGIDA_CLIENTE * 60; // 2 horas para pickup del cliente
            }
            
            if (duracionEstancia > 0 && !agregarOcupacionTemporal(aeropuertoLlegada, minutoLlegada, duracionEstancia, conteoProductos)) {
                System.out.println("Violación de capacidad en " + aeropuertoLlegada.getCiudad().getNombre() + 
                                  " en minuto " + minutoLlegada + " (fase de llegada) para paquete " + paquete.getId());
                return false;
            }
            
            // Actualizar tiempo para el siguiente vuelo
            minutoActual = minutoLlegada;
            if (i < ruta.size() - 1) {
                minutoActual += 120; // Tiempo de conexión
            }
        }
        
        return true;
    }
    
    /**
     * Agrega ocupación temporal a un aeropuerto durante un período de tiempo.
     */
    private boolean agregarOcupacionTemporal(Aeropuerto aeropuerto, int minutoInicio, int duracionMinutos, int conteoProductos) {
        if (aeropuerto == null || aeropuerto.getAlmacen() == null) {
            return false;
        }
        
        int[] arrayOcupacion = ocupacionTemporalAlmacenes.get(aeropuerto);
        int capacidadMaxima = aeropuerto.getAlmacen().getCapacidadMaxima();
        
        // Verificar y agregar ocupación para cada minuto del período
        final int TOTAL_MINUTOS = HORIZON_DAYS * 24 * 60;
        for (int minuto = minutoInicio; minuto < Math.min(minutoInicio + duracionMinutos, TOTAL_MINUTOS); minuto++) {
            arrayOcupacion[minuto] += conteoProductos;
            if (arrayOcupacion[minuto] > capacidadMaxima) {
                return false; // Violación de capacidad
            }
        }
        
        return true;
    }

}
