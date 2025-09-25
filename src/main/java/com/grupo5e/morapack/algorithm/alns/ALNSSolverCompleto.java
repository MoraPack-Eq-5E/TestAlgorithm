package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.utils.MoraPackDataLoader;

import java.util.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Solucionador ALNS completo adaptado del ejemplo Solution
 * Implementa Adaptive Large Neighborhood Search para el problema de logística MoraPack
 */
public class ALNSSolverCompleto {
    
    // Datos del problema
    private List<Aeropuerto> aeropuertos;
    private List<Vuelo> vuelos;
    private List<Paquete> paquetes;
    private List<Paquete> paquetesOriginales;
    
    // Solución actual y mejor
    private Solucion solucionActual;
    private Solucion mejorSolucion;
    
    // Operadores ALNS
    private ALNSDestruction operadoresDestruccion;
    private ALNSRepair operadoresReparacion;
    
    // Parámetros del algoritmo
    private double temperatura;
    private double tasaEnfriamiento;
    private int iteracionesMaximas;
    private int tamanoSegmento;
    
    // Matrices de pesos y puntajes
    private double[][] pesosOperadores;
    private double[][] puntajesOperadores;
    private int[][] usoOperadores;
    
    // Cache y utilidades
    private Map<String, Aeropuerto> cacheAeropuertos;
    private Map<String, Integer> ocupacionAlmacenes;
    private Map<Aeropuerto, int[]> ocupacionTemporalAlmacenes;
    private Random generadorAleatorio;
    
    // Ancla temporal
    private LocalDateTime T0;
    private static final int HORIZONTE_DIAS = 4;
    private static final boolean HABILITAR_UNITIZACION = true;
    
    public ALNSSolverCompleto() {
        inicializarDatos();
        inicializarParametros();
        inicializarOperadores();
        inicializarCache();
        inicializarOcupaciones();
    }
    
    /**
     * Inicializa los datos del problema desde los archivos
     */
    private void inicializarDatos() {
        System.out.println("=== INICIALIZANDO DATOS DEL PROBLEMA ===");
        
        // Cargar datos usando el loader existente
        this.aeropuertos = MoraPackDataLoader.cargarAeropuertos();
        this.vuelos = MoraPackDataLoader.cargarVuelos(this.aeropuertos);
        MoraPackDataLoader.DatosPrueba datos = MoraPackDataLoader.cargarDatosPrueba();
        this.paquetesOriginales = datos.paquetes;
        
        // Aplicar unitización si está habilitada
        if (HABILITAR_UNITIZACION) {
            this.paquetes = expandirPaquetesAUnidades(this.paquetesOriginales);
            System.out.println("UNITIZACIÓN APLICADA: " + this.paquetesOriginales.size() + 
                             " paquetes originales → " + this.paquetes.size() + " unidades de producto");
        } else {
            this.paquetes = new ArrayList<>(this.paquetesOriginales);
            System.out.println("UNITIZACIÓN DESHABILITADA: Usando paquetes originales");
        }
        
        System.out.println("Aeropuertos cargados: " + this.aeropuertos.size());
        System.out.println("Vuelos cargados: " + this.vuelos.size());
        System.out.println("Paquetes cargados: " + this.paquetes.size());
    }
    
    /**
     * Inicializa los parámetros del algoritmo ALNS
     */
    private void inicializarParametros() {
        // Número de operadores
        int numOperadoresDestruccion = 4; // random, geographic, timeBased, congestedRoute
        int numOperadoresReparacion = 4;  // greedy, regret, timeBased, capacityBased
        
        // Inicializar matrices
        this.pesosOperadores = new double[numOperadoresDestruccion][numOperadoresReparacion];
        this.puntajesOperadores = new double[numOperadoresDestruccion][numOperadoresReparacion];
        this.usoOperadores = new int[numOperadoresDestruccion][numOperadoresReparacion];
        
        // Inicializar pesos uniformemente
        for (int i = 0; i < numOperadoresDestruccion; i++) {
            for (int j = 0; j < numOperadoresReparacion; j++) {
                this.pesosOperadores[i][j] = 1.0;
                this.puntajesOperadores[i][j] = 0.0;
                this.usoOperadores[i][j] = 0;
            }
        }
        
        // Parámetros del algoritmo
        this.temperatura = 1000.0;
        this.tasaEnfriamiento = 0.995;
        this.iteracionesMaximas = 100;
        this.tamanoSegmento = 10;
        
        this.generadorAleatorio = new Random(System.currentTimeMillis());
    }
    
    /**
     * Inicializa los operadores ALNS
     */
    private void inicializarOperadores() {
        this.operadoresDestruccion = new ALNSDestruction();
        this.operadoresReparacion = new ALNSRepair(aeropuertos, vuelos, ocupacionAlmacenes);
    }
    
    /**
     * Inicializa el cache de aeropuertos
     */
    private void inicializarCache() {
        this.cacheAeropuertos = new HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (aeropuerto.getCiudad() != null && aeropuerto.getCiudad().getNombre() != null) {
                String claveCiudad = aeropuerto.getCiudad().getNombre().toLowerCase().trim();
                cacheAeropuertos.put(claveCiudad, aeropuerto);
            }
        }
        System.out.println("Cache inicializada: " + cacheAeropuertos.size() + " ciudades");
    }
    
    /**
     * Inicializa las ocupaciones de almacenes
     */
    private void inicializarOcupaciones() {
        this.ocupacionAlmacenes = new HashMap<>();
        this.ocupacionTemporalAlmacenes = new HashMap<>();
        
        for (Aeropuerto aeropuerto : aeropuertos) {
            ocupacionAlmacenes.put(aeropuerto.getCodigoIATA(), 0);
            final int TOTAL_MINUTOS = HORIZONTE_DIAS * 24 * 60;
            ocupacionTemporalAlmacenes.put(aeropuerto, new int[TOTAL_MINUTOS]);
        }
        
        // Inicializar T0
        inicializarT0();
    }
    
    /**
     * Inicializa T0 como el mínimo orderDate o now si está vacío
     */
    private void inicializarT0() {
        T0 = LocalDateTime.now();
        
        if (paquetes != null && !paquetes.isEmpty()) {
            LocalDateTime minFechaPedido = paquetes.stream()
                .filter(pkg -> pkg.getFechaCreacion() != null)
                .map(Paquete::getFechaCreacion)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            T0 = minFechaPedido;
        }
        
        System.out.println("T0 inicializado: " + T0);
    }
    
    /**
     * Expande paquetes a unidades de producto
     */
    private List<Paquete> expandirPaquetesAUnidades(List<Paquete> paquetesOriginales) {
        List<Paquete> unidadesProducto = new ArrayList<>();
        
        for (Paquete paqueteOriginal : paquetesOriginales) {
            // Cada paquete se convierte en una unidad de producto
            Paquete unidad = crearUnidadPaquete(paqueteOriginal);
            unidadesProducto.add(unidad);
        }
        
        return unidadesProducto;
    }
    
    /**
     * Crea una unidad de paquete a partir del paquete original
     */
    private Paquete crearUnidadPaquete(Paquete paqueteOriginal) {
        Paquete unidad = new Paquete();
        
        // ID derivado
        String idUnidad = paqueteOriginal.getId() + "#1";
        unidad.setId(idUnidad);
        
        // Copiar metadatos
        unidad.setClienteId(paqueteOriginal.getClienteId());
        unidad.setAeropuertoDestino(paqueteOriginal.getAeropuertoDestino());
        unidad.setFechaLimiteEntrega(paqueteOriginal.getFechaLimiteEntrega());
        unidad.setFechaCreacion(paqueteOriginal.getFechaCreacion());
        unidad.setEstado(paqueteOriginal.getEstado());
        unidad.setAeropuertoActual(paqueteOriginal.getAeropuertoActual());
        unidad.setPrioridad(paqueteOriginal.getPrioridad());
        unidad.setRutaPlanificada(paqueteOriginal.getRutaPlanificada());
        
        return unidad;
    }
    
    /**
     * Método principal para resolver el problema
     */
    public void resolver() {
        System.out.println("\n=== INICIANDO SOLUCIÓN ALNS ===");
        
        // 1. Generar solución inicial
        System.out.println("\n=== GENERANDO SOLUCIÓN INICIAL ===");
        this.generarSolucionInicial();
        
        // Validar solución generada
        System.out.println("Validando solución...");
        boolean esValida = this.esSolucionValida();
        System.out.println("Solución válida: " + (esValida ? "SÍ" : "NO"));
        
        // Mostrar descripción de la solución inicial
        this.imprimirDescripcionSolucion(1);
        
        // 2. Establecer mejor solución
        mejorSolucion = solucionActual.copiar();
        
        // 3. Ejecutar algoritmo ALNS
        System.out.println("\n=== INICIANDO ALGORITMO ALNS ===");
        ejecutarAlgoritmoALNS();
        
        // 4. Mostrar resultado final
        System.out.println("\n=== RESULTADO FINAL ALNS ===");
        this.imprimirDescripcionSolucion(2);
    }
    
    /**
     * Genera una solución inicial
     */
    private void generarSolucionInicial() {
        System.out.println("=== GENERANDO SOLUCIÓN INICIAL GREEDY ===");
        
        Map<String, Ruta> rutasPaquetes = new HashMap<>();
        int paquetesAsignados = 0;
        
        // Ordenar paquetes por deadline
        List<Paquete> paquetesOrdenados = new ArrayList<>(paquetes);
        paquetesOrdenados.sort(Comparator.comparing(Paquete::getFechaLimiteEntrega));
        
        for (Paquete paquete : paquetesOrdenados) {
            Ruta mejorRuta = encontrarMejorRuta(paquete);
            
            if (mejorRuta != null && esRutaValida(paquete, mejorRuta)) {
                if (puedeAsignarConOptimizacionEspacio(paquete, mejorRuta, rutasPaquetes)) {
                    rutasPaquetes.put(paquete.getId(), mejorRuta);
                    actualizarCapacidadesVuelos(mejorRuta, 1);
                    incrementarOcupacionAlmacen(obtenerAeropuertoPorCodigo(paquete.getAeropuertoDestino()), 1);
                    paquetesAsignados++;
                }
            }
        }
        
        // Crear solución
        this.solucionActual = new Solucion(rutasPaquetes);
        this.solucionActual.calcularFitness(paquetes.size());
        
        System.out.println("Solución inicial generada: " + paquetesAsignados + "/" + paquetes.size() + " paquetes asignados");
        System.out.println("Fitness de la solución: " + this.solucionActual.getFuncionObjetivo());
    }
    
    /**
     * Encuentra la mejor ruta para un paquete
     */
    private Ruta encontrarMejorRuta(Paquete paquete) {
        // Buscar ruta directa primero
        Ruta rutaDirecta = buscarRutaDirecta(paquete);
        if (rutaDirecta != null) {
            return rutaDirecta;
        }
        
        // Buscar ruta con una escala
        Ruta rutaConEscala = buscarRutaConEscala(paquete);
        if (rutaConEscala != null) {
            return rutaConEscala;
        }
        
        return null;
    }
    
    /**
     * Busca una ruta directa para el paquete
     */
    private Ruta buscarRutaDirecta(Paquete paquete) {
        String origen = paquete.getAeropuertoActual();
        String destino = paquete.getAeropuertoDestino();
        
        if (origen.equals(destino)) {
            return new Ruta("directa_" + paquete.getId(), paquete.getId());
        }
        
        // Buscar vuelo directo
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen().getCodigoIATA().equals(origen) && 
                vuelo.getAeropuertoDestino().getCodigoIATA().equals(destino) &&
                vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                
                Ruta ruta = new Ruta("directa_" + paquete.getId(), paquete.getId());
                SegmentoRuta segmento = new SegmentoRuta(
                    "seg_" + vuelo.getId(),
                    origen,
                    destino,
                    "VUELO_" + vuelo.getId(),
                    esMismoContinente(origen, destino)
                );
                segmento.setDuracionHoras(vuelo.getTiempoTransporte());
                segmento.setCosto(vuelo.getCosto());
                ruta.agregarSegmento(segmento);
                return ruta;
            }
        }
        
        return null;
    }
    
    /**
     * Busca una ruta con una escala
     */
    private Ruta buscarRutaConEscala(Paquete paquete) {
        String origen = paquete.getAeropuertoActual();
        String destino = paquete.getAeropuertoDestino();
        
        // Buscar escala intermedia
        for (Aeropuerto aeropuertoIntermedio : aeropuertos) {
            String intermedio = aeropuertoIntermedio.getCodigoIATA();
            if (intermedio.equals(origen) || intermedio.equals(destino)) continue;
            
            // Buscar primer vuelo: origen -> intermedio
            Vuelo primerVuelo = null;
            for (Vuelo vuelo : vuelos) {
                if (vuelo.getAeropuertoOrigen().getCodigoIATA().equals(origen) && 
                    vuelo.getAeropuertoDestino().getCodigoIATA().equals(intermedio) &&
                    vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                    primerVuelo = vuelo;
                    break;
                }
            }
            
            if (primerVuelo == null) continue;
            
            // Buscar segundo vuelo: intermedio -> destino
            Vuelo segundoVuelo = null;
            for (Vuelo vuelo : vuelos) {
                if (vuelo.getAeropuertoOrigen().getCodigoIATA().equals(intermedio) && 
                    vuelo.getAeropuertoDestino().getCodigoIATA().equals(destino) &&
                    vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                    segundoVuelo = vuelo;
                    break;
                }
            }
            
            if (segundoVuelo != null) {
                Ruta ruta = new Ruta("con_escala_" + paquete.getId(), paquete.getId());
                
                // Primer segmento
                SegmentoRuta segmento1 = new SegmentoRuta(
                    "seg1_" + primerVuelo.getId(),
                    origen,
                    intermedio,
                    "VUELO_" + primerVuelo.getId(),
                    esMismoContinente(origen, intermedio)
                );
                segmento1.setDuracionHoras(primerVuelo.getTiempoTransporte());
                segmento1.setCosto(primerVuelo.getCosto());
                ruta.agregarSegmento(segmento1);
                
                // Segundo segmento
                SegmentoRuta segmento2 = new SegmentoRuta(
                    "seg2_" + segundoVuelo.getId(),
                    intermedio,
                    destino,
                    "VUELO_" + segundoVuelo.getId(),
                    esMismoContinente(intermedio, destino)
                );
                segmento2.setDuracionHoras(segundoVuelo.getTiempoTransporte());
                segmento2.setCosto(segundoVuelo.getCosto());
                ruta.agregarSegmento(segmento2);
                
                return ruta;
            }
        }
        
        return null;
    }
    
    /**
     * Verifica si dos aeropuertos están en el mismo continente
     */
    private boolean esMismoContinente(String codigoOrigen, String codigoDestino) {
        Aeropuerto aeropuertoOrigen = obtenerAeropuertoPorCodigo(codigoOrigen);
        Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCodigo(codigoDestino);
        
        if (aeropuertoOrigen == null || aeropuertoDestino == null) return false;
        
        return aeropuertoOrigen.getCiudad().getContinente() == aeropuertoDestino.getCiudad().getContinente();
    }
    
    /**
     * Obtiene un aeropuerto por su código IATA
     */
    private Aeropuerto obtenerAeropuertoPorCodigo(String codigoIATA) {
        return aeropuertos.stream()
            .filter(a -> a.getCodigoIATA().equals(codigoIATA))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Verifica si una ruta es válida
     */
    private boolean esRutaValida(Paquete paquete, Ruta ruta) {
        if (ruta == null || ruta.getSegmentos().isEmpty()) {
            return paquete.getAeropuertoActual().equals(paquete.getAeropuertoDestino());
        }
        
        // Verificar capacidad de vuelos
        for (SegmentoRuta segmento : ruta.getSegmentos()) {
            Vuelo vuelo = obtenerVueloPorNumero(segmento.getNumeroVuelo());
            if (vuelo != null && vuelo.getCapacidadUsada() >= vuelo.getCapacidadMaxima()) {
                return false;
            }
        }
        
        // Verificar que la ruta sea continua
        for (int i = 0; i < ruta.getSegmentos().size() - 1; i++) {
            if (!ruta.getSegmentos().get(i).getAeropuertoDestino()
                    .equals(ruta.getSegmentos().get(i + 1).getAeropuertoOrigen())) {
                return false;
            }
        }
        
        // Verificar restricciones de tiempo
        return respetaDeadline(paquete, ruta);
    }
    
    /**
     * Verifica si la ruta respeta el deadline
     */
    private boolean respetaDeadline(Paquete paquete, Ruta ruta) {
        double tiempoTotal = ruta.getTiempoTotalHoras();
        
        // Añadir tiempo de conexiones
        if (ruta.getSegmentos().size() > 1) {
            tiempoTotal += (ruta.getSegmentos().size() - 1) * 2.0; // 2 horas por conexión
        }
        
        // Verificar promesas MoraPack
        if (!validarPromesasMoraPack(paquete, tiempoTotal)) {
            return false;
        }
        
        // Verificar deadline específico del cliente
        long horasHastaDeadline = ChronoUnit.HOURS.between(paquete.getFechaCreacion(), paquete.getFechaLimiteEntrega());
        return tiempoTotal <= horasHastaDeadline;
    }
    
    /**
     * Valida las promesas de entrega MoraPack
     */
    private boolean validarPromesasMoraPack(Paquete paquete, double tiempoTotalHoras) {
        String origen = paquete.getAeropuertoActual();
        String destino = paquete.getAeropuertoDestino();
        
        boolean mismoContinente = esMismoContinente(origen, destino);
        long promesaMoraPackHoras = mismoContinente ? 48 : 72; // 2 días intra / 3 días inter
        
        return tiempoTotalHoras <= promesaMoraPackHoras;
    }
    
    /**
     * Verifica si se puede asignar considerando optimización de espacio
     */
    private boolean puedeAsignarConOptimizacionEspacio(Paquete paquete, Ruta ruta, Map<String, Ruta> solucionActual) {
        Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCodigo(paquete.getAeropuertoDestino());
        if (aeropuertoDestino == null) return false;
        
        int ocupacionActual = ocupacionAlmacenes.getOrDefault(aeropuertoDestino.getCodigoIATA(), 0);
        int capacidadMaxima = aeropuertoDestino.getCapacidadAlmacen();
        
        return (ocupacionActual + 1) <= capacidadMaxima;
    }
    
    /**
     * Actualiza las capacidades de los vuelos
     */
    private void actualizarCapacidadesVuelos(Ruta ruta, int incremento) {
        for (SegmentoRuta segmento : ruta.getSegmentos()) {
            Vuelo vuelo = obtenerVueloPorNumero(segmento.getNumeroVuelo());
            if (vuelo != null) {
                vuelo.setCapacidadUsada(vuelo.getCapacidadUsada() + incremento);
            }
        }
    }
    
    /**
     * Obtiene un vuelo por su número
     */
    private Vuelo obtenerVueloPorNumero(String numeroVuelo) {
        return vuelos.stream()
            .filter(v -> ("VUELO_" + v.getId()).equals(numeroVuelo))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Incrementa la ocupación de un almacén
     */
    private void incrementarOcupacionAlmacen(Aeropuerto aeropuerto, int cantidad) {
        if (aeropuerto != null) {
            int ocupacionActual = ocupacionAlmacenes.getOrDefault(aeropuerto.getCodigoIATA(), 0);
            ocupacionAlmacenes.put(aeropuerto.getCodigoIATA(), ocupacionActual + cantidad);
        }
    }
    
    /**
     * Ejecuta el algoritmo ALNS principal
     */
    private void ejecutarAlgoritmoALNS() {
        double mejorFitness = solucionActual.getFuncionObjetivo();
        int mejoras = 0;
        int sinMejoras = 0;
        
        System.out.println("Fitness de solución inicial: " + mejorFitness);
        
        // Bucle principal ALNS
        for (int iteracion = 0; iteracion < iteracionesMaximas; iteracion++) {
            System.out.println("ALNS Iteración " + iteracion + "/" + iteracionesMaximas);
            
            // Seleccionar operadores
            int[] operadoresSeleccionados = seleccionarOperadores();
            int operadorDestruccion = operadoresSeleccionados[0];
            int operadorReparacion = operadoresSeleccionados[1];
            
            System.out.println("  Operadores seleccionados: Destrucción=" + operadorDestruccion + ", Reparación=" + operadorReparacion);
            
            // Crear copia de la solución actual
            Solucion solucionTemporal = solucionActual.copiar();
            
            // Aplicar operador de destrucción
            System.out.println("  Aplicando operador de destrucción...");
            ALNSDestruction.DestructionResult resultadoDestruccion = aplicarOperadorDestruccion(
                solucionTemporal, operadorDestruccion);
            
            if (resultadoDestruccion == null || resultadoDestruccion.getDestroyedPackages().isEmpty()) {
                System.out.println("  No se pudo destruir nada, continuando...");
                continue;
            }
            
            System.out.println("  Paquetes destruidos: " + resultadoDestruccion.getDestroyedPackages().size());
            
            // Aplicar operador de reparación
            ALNSRepair.RepairResult resultadoReparacion = aplicarOperadorReparacion(
                solucionTemporal, operadorReparacion, resultadoDestruccion.getDestroyedPackages());
            
            if (resultadoReparacion == null || !resultadoReparacion.isSuccess()) {
                System.out.println("  No se pudo reparar, continuando...");
                continue;
            }
            
            // Evaluar nueva solución
            solucionTemporal.calcularFitness(paquetes.size());
            double fitnessTemporal = solucionTemporal.getFuncionObjetivo();
            
            // Actualizar contador de uso
            usoOperadores[operadorDestruccion][operadorReparacion]++;
            
            // Criterio de aceptación
            boolean aceptada = false;
            if (fitnessTemporal < mejorFitness) {
                solucionActual = solucionTemporal;
                mejorFitness = fitnessTemporal;
                aceptada = true;
                
                if (fitnessTemporal < mejorSolucion.getFuncionObjetivo()) {
                    mejorSolucion = solucionTemporal.copiar();
                    puntajesOperadores[operadorDestruccion][operadorReparacion] += 100;
                    mejoras++;
                    sinMejoras = 0;
                    System.out.println("Iteración " + iteracion + ": ¡Nueva mejor solución! Fitness: " + mejorFitness);
                } else {
                    puntajesOperadores[operadorDestruccion][operadorReparacion] += 50;
                }
            } else {
                double probabilidad = Math.exp((mejorFitness - fitnessTemporal) / temperatura);
                if (generadorAleatorio.nextDouble() < probabilidad) {
                    solucionActual = solucionTemporal;
                    mejorFitness = fitnessTemporal;
                    aceptada = true;
                    puntajesOperadores[operadorDestruccion][operadorReparacion] += 10;
                }
            }
            
            if (!aceptada) {
                sinMejoras++;
            }
            
            // Actualizar pesos cada tamanoSegmento iteraciones
            if ((iteracion + 1) % tamanoSegmento == 0) {
                actualizarPesosOperadores();
                temperatura *= tasaEnfriamiento;
            }
            
            // Parada temprana si no hay mejoras
            if (sinMejoras > 50) {
                System.out.println("Parada temprana en iteración " + iteracion + " (sin mejoras)");
                break;
            }
        }
        
        // Actualizar la solución final
        solucionActual = mejorSolucion;
        
        System.out.println("ALNS completado:");
        System.out.println("  Mejoras encontradas: " + mejoras);
        System.out.println("  Fitness final: " + mejorFitness);
        System.out.println("  Temperatura final: " + temperatura);
    }
    
    /**
     * Selecciona operadores basado en sus pesos
     */
    private int[] seleccionarOperadores() {
        // Selección por ruleta basada en pesos
        double pesoTotal = 0.0;
        for (int i = 0; i < pesosOperadores.length; i++) {
            for (int j = 0; j < pesosOperadores[i].length; j++) {
                pesoTotal += pesosOperadores[i][j];
            }
        }
        
        double valorAleatorio = generadorAleatorio.nextDouble() * pesoTotal;
        double pesoAcumulado = 0.0;
        
        for (int i = 0; i < pesosOperadores.length; i++) {
            for (int j = 0; j < pesosOperadores[i].length; j++) {
                pesoAcumulado += pesosOperadores[i][j];
                if (valorAleatorio <= pesoAcumulado) {
                    return new int[]{i, j};
                }
            }
        }
        
        // Fallback
        return new int[]{0, 0};
    }
    
    /**
     * Aplica el operador de destrucción seleccionado
     */
    private ALNSDestruction.DestructionResult aplicarOperadorDestruccion(Solucion solucion, int indiceOperador) {
        // Convertir Solucion a HashMap<Paquete, Ruta> para compatibilidad
        HashMap<Paquete, Ruta> solucionMap = new HashMap<>();
        for (Map.Entry<String, Ruta> entry : solucion.getRutasPaquetes().entrySet()) {
            String paqueteId = entry.getKey();
            Ruta ruta = entry.getValue();
            Paquete paquete = paquetes.stream()
                .filter(p -> p.getId().equals(paqueteId))
                .findFirst()
                .orElse(null);
            if (paquete != null) {
                solucionMap.put(paquete, ruta);
            }
        }
        
        switch (indiceOperador) {
            case 0: // Random Destroy
                return operadoresDestruccion.randomDestroy(solucionMap, 0.1, 1, 5);
            case 1: // Geographic Destroy
                return operadoresDestruccion.geographicDestroy(solucionMap, 0.1, 1, 5);
            case 2: // Time Based Destroy
                return operadoresDestruccion.timeBasedDestroy(solucionMap, 0.1, 1, 5);
            case 3: // Congested Route Destroy
                return operadoresDestruccion.congestedRouteDestroy(solucionMap, 0.1, 1, 5);
            default:
                return operadoresDestruccion.randomDestroy(solucionMap, 0.1, 1, 5);
        }
    }
    
    /**
     * Aplica el operador de reparación seleccionado
     */
    private ALNSRepair.RepairResult aplicarOperadorReparacion(Solucion solucion, int indiceOperador,
            List<Map.Entry<Paquete, Ruta>> paquetesDestruidos) {
        
        // Convertir Solucion a HashMap<Paquete, Ruta> para compatibilidad
        HashMap<Paquete, Ruta> solucionMap = new HashMap<>();
        for (Map.Entry<String, Ruta> entry : solucion.getRutasPaquetes().entrySet()) {
            String paqueteId = entry.getKey();
            Ruta ruta = entry.getValue();
            Paquete paquete = paquetes.stream()
                .filter(p -> p.getId().equals(paqueteId))
                .findFirst()
                .orElse(null);
            if (paquete != null) {
                solucionMap.put(paquete, ruta);
            }
        }
        
        switch (indiceOperador) {
            case 0: // Greedy Repair
                return operadoresReparacion.greedyRepair(solucionMap, paquetesDestruidos);
            case 1: // Regret Repair
                return operadoresReparacion.regretRepair(solucionMap, paquetesDestruidos, 2);
            case 2: // Time Based Repair
                return operadoresReparacion.timeBasedRepair(solucionMap, paquetesDestruidos);
            case 3: // Capacity Based Repair
                return operadoresReparacion.capacityBasedRepair(solucionMap, paquetesDestruidos);
            default:
                return operadoresReparacion.greedyRepair(solucionMap, paquetesDestruidos);
        }
    }
    
    /**
     * Actualiza los pesos de los operadores
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
     * Verifica si la solución es válida
     */
    private boolean esSolucionValida() {
        if (solucionActual == null || solucionActual.esVacia()) {
            return false;
        }
        
        // Verificar que todas las rutas sean válidas
        for (Map.Entry<String, Ruta> entry : solucionActual.getRutasPaquetes().entrySet()) {
            String paqueteId = entry.getKey();
            Ruta ruta = entry.getValue();
            
            Paquete paquete = paquetes.stream()
                .filter(p -> p.getId().equals(paqueteId))
                .findFirst()
                .orElse(null);
            
            if (paquete != null && !esRutaValida(paquete, ruta)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Imprime una descripción de la solución
     */
    private void imprimirDescripcionSolucion(int nivelDetalle) {
        if (solucionActual == null || solucionActual.esVacia()) {
            System.out.println("No hay solución disponible para mostrar.");
            return;
        }
        
        System.out.println("\n========== DESCRIPCIÓN DE LA SOLUCIÓN ==========");
        System.out.println("Fitness de la solución: " + solucionActual.getFuncionObjetivo());
        System.out.println("Paquetes asignados: " + solucionActual.getCantidadPaquetes() + "/" + paquetes.size());
        System.out.println("Costo total: " + solucionActual.getCostoTotal());
        System.out.println("Tiempo total: " + solucionActual.getTiempoTotalHoras() + " horas");
        System.out.println("Es factible: " + solucionActual.isEsFactible());
        System.out.println("Violaciones: " + solucionActual.getViolacionesRestricciones());
        
        if (nivelDetalle >= 2) {
            System.out.println("\n----- Rutas Asignadas -----");
            int contador = 0;
            for (Map.Entry<String, Ruta> entry : solucionActual.getRutasPaquetes().entrySet()) {
                if (contador >= 10) { // Mostrar solo las primeras 10
                    System.out.println("... y " + (solucionActual.getCantidadPaquetes() - 10) + " rutas más");
                    break;
                }
                
                String paqueteId = entry.getKey();
                Ruta ruta = entry.getValue();
                
                System.out.println("Paquete " + paqueteId + ": " + ruta.toString());
                contador++;
            }
        }
        
        System.out.println("\n=================================================");
    }
    
    // Getters para acceso externo
    public Solucion getSolucionActual() {
        return solucionActual;
    }
    
    public Solucion getMejorSolucion() {
        return mejorSolucion;
    }
    
    public List<Paquete> getPaquetes() {
        return paquetes;
    }
    
    public List<Aeropuerto> getAeropuertos() {
        return aeropuertos;
    }
    
    public List<Vuelo> getVuelos() {
        return vuelos;
    }
}
