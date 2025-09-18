package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.service.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ALNS Solver integrado con las nuevas funcionalidades:
 * - Cancelaciones de vuelos
 * - Demoras de vuelos
 * - Reasignación de productos
 * - Parser de pedidos desde archivos
 */
public class ALNSSolverIntegrado extends ALNSSolver {
    
    // Nuevos servicios integrados
    private GestorCancelaciones gestorCancelaciones;
    private GestorDemoras gestorDemoras;
    private GestorReasignacion gestorReasignacion;
    private GestorProductos gestorProductos;
    
    // Configuración de escenarios
    private boolean simularCancelaciones;
    private boolean simularDemoras;
    private boolean permitirReasignaciones;
    private String archivoPedidos;
    private String archivoCancelaciones;
    
    // Estadísticas de integración
    private int cancelacionesAplicadas;
    private int demorasAplicadas;
    private int reasignacionesRealizadas;
    private List<String> eventosCrisis;
    
    public ALNSSolverIntegrado(int iteracionesMaximas, double temperaturaInicial, double factorEnfriamiento) {
        super(iteracionesMaximas, temperaturaInicial, factorEnfriamiento);
        this.eventosCrisis = new ArrayList<>();
        this.cancelacionesAplicadas = 0;
        this.demorasAplicadas = 0;
        this.reasignacionesRealizadas = 0;
    }
    
    /**
     * Configura el problema con los nuevos servicios integrados
     */
    public void configurarProblemaIntegrado(List<Paquete> paquetes, List<Aeropuerto> aeropuertos, 
                                          List<Vuelo> vuelos, Set<Continente> continentes,
                                          List<Pedido> pedidos) {
        // Configurar problema base
        super.configurarProblema(paquetes, aeropuertos, vuelos, continentes);
        
        // Crear mapas para los servicios
        Map<String, Paquete> mapaPaquetes = paquetes.stream()
            .collect(Collectors.toMap(Paquete::getId, p -> p));
        
        Map<String, Pedido> mapaPedidos = pedidos.stream()
            .collect(Collectors.toMap(Pedido::getId, p -> p));
        
        Map<String, Vuelo> mapaVuelos = vuelos.stream()
            .collect(Collectors.toMap(Vuelo::getNumeroVuelo, v -> v));
        
        // Inicializar servicios
        this.gestorCancelaciones = new GestorCancelaciones(mapaVuelos);
        this.gestorDemoras = new GestorDemoras(mapaVuelos);
        this.gestorReasignacion = new GestorReasignacion(mapaPaquetes, mapaPedidos);
        this.gestorProductos = new GestorProductos(mapaPaquetes, mapaPedidos, mapaVuelos);
        
        System.out.println("✅ Servicios integrados inicializados:");
        System.out.println("   - GestorCancelaciones: " + mapaVuelos.size() + " vuelos");
        System.out.println("   - GestorDemoras: " + mapaVuelos.size() + " vuelos");
        System.out.println("   - GestorReasignacion: " + mapaPaquetes.size() + " paquetes, " + mapaPedidos.size() + " pedidos");
        System.out.println("   - GestorProductos: Orquestador integrado");
    }
    
    /**
     * Configura los escenarios de simulación
     */
    public void configurarEscenarios(boolean simularCancelaciones, boolean simularDemoras, 
                                   boolean permitirReasignaciones, String archivoPedidos, 
                                   String archivoCancelaciones) {
        this.simularCancelaciones = simularCancelaciones;
        this.simularDemoras = simularDemoras;
        this.permitirReasignaciones = permitirReasignaciones;
        this.archivoPedidos = archivoPedidos;
        this.archivoCancelaciones = archivoCancelaciones;
        
        System.out.println("🎯 Escenarios configurados:");
        System.out.println("   - Cancelaciones: " + (simularCancelaciones ? "✅" : "❌"));
        System.out.println("   - Demoras: " + (simularDemoras ? "✅" : "❌"));
        System.out.println("   - Reasignaciones: " + (permitirReasignaciones ? "✅" : "❌"));
        System.out.println("   - Archivo pedidos: " + (archivoPedidos != null ? archivoPedidos : "No especificado"));
        System.out.println("   - Archivo cancelaciones: " + (archivoCancelaciones != null ? archivoCancelaciones : "No especificado"));
    }
    
    /**
     * Resuelve el problema con las nuevas funcionalidades integradas
     */
    @Override
    public Solucion resolver() {
        System.out.println("🚀 Iniciando ALNS Integrado con nuevas funcionalidades...");
        
        // 1. Cargar pedidos desde archivo si se especifica
        if (archivoPedidos != null) {
            cargarPedidosDesdeArchivo();
        }
        
        // 2. Aplicar cancelaciones programadas si se especifica
        if (archivoCancelaciones != null && simularCancelaciones) {
            aplicarCancelacionesProgramadas();
        }
        
        // 3. Ejecutar ALNS base
        Solucion solucion = super.resolver();
        
        // 4. Aplicar reasignaciones si está habilitado
        if (permitirReasignaciones) {
            optimizarConReasignaciones(solucion);
        }
        
        // 5. Simular eventos de crisis durante la ejecución
        if (simularCancelaciones || simularDemoras) {
            simularEventosCrisis();
        }
        
        // 6. Generar reporte final integrado
        generarReporteIntegrado();
        
        return solucion;
    }
    
    /**
     * Carga pedidos desde archivo usando el parser
     */
    private void cargarPedidosDesdeArchivo() {
        System.out.println("📁 Cargando pedidos desde archivo: " + archivoPedidos);
        
        try {
            List<Pedido> pedidosParseados = ParserPedidos.parsearArchivo(archivoPedidos);
            System.out.println("✅ Cargados " + pedidosParseados.size() + " pedidos desde archivo");
            
            // Aquí se podrían integrar los pedidos al contexto del problema
            // Por ahora solo mostramos la información
            
        } catch (Exception e) {
            System.err.println("❌ Error al cargar pedidos: " + e.getMessage());
        }
    }
    
    /**
     * Aplica cancelaciones programadas desde archivo
     */
    private void aplicarCancelacionesProgramadas() {
        System.out.println("🛫 Aplicando cancelaciones programadas...");
        
        try {
            List<GestorCancelaciones.ResultadoCancelacion> resultados = 
                gestorCancelaciones.cancelarDesdeArchivo(archivoCancelaciones);
            
            int exitosas = (int) resultados.stream()
                .filter(GestorCancelaciones.ResultadoCancelacion::isExitoso)
                .count();
            
            this.cancelacionesAplicadas = exitosas;
            System.out.println("✅ " + exitosas + "/" + resultados.size() + " cancelaciones aplicadas");
            
        } catch (Exception e) {
            System.err.println("❌ Error al aplicar cancelaciones: " + e.getMessage());
        }
    }
    
    /**
     * Optimiza la solución usando reasignaciones
     */
    private void optimizarConReasignaciones(Solucion solucion) {
        System.out.println("🔄 Optimizando solución con reasignaciones...");
        
        try {
            List<GestorReasignacion.ResultadoReasignacion> resultados = 
                gestorReasignacion.optimizarReasignacionesPorProximidad("SKBO");
            
            int exitosas = (int) resultados.stream()
                .filter(GestorReasignacion.ResultadoReasignacion::isExitoso)
                .count();
            
            this.reasignacionesRealizadas = exitosas;
            System.out.println("✅ " + exitosas + "/" + resultados.size() + " reasignaciones realizadas");
            
        } catch (Exception e) {
            System.err.println("❌ Error en optimización: " + e.getMessage());
        }
    }
    
    /**
     * Simula eventos de crisis durante la ejecución
     */
    private void simularEventosCrisis() {
        System.out.println("🚨 Simulando eventos de crisis...");
        
        // Simular cancelaciones aleatorias
        if (simularCancelaciones) {
            simularCancelacionesAleatorias();
        }
        
        // Simular demoras aleatorias
        if (simularDemoras) {
            simularDemorasAleatorias();
        }
        
        // Manejar crisis con el gestor integrado
        if (gestorProductos != null) {
            gestorProductos.simularEscenarioCrisis();
        }
    }
    
    /**
     * Simula cancelaciones aleatorias de vuelos
     */
    private void simularCancelacionesAleatorias() {
        List<Vuelo> vuelosCancelables = gestorCancelaciones.getVuelosCancelables();
        
        if (!vuelosCancelables.isEmpty()) {
            // Cancelar hasta 3 vuelos aleatoriamente
            int vuelosACancelar = Math.min(3, vuelosCancelables.size());
            Random random = new Random();
            
            for (int i = 0; i < vuelosACancelar; i++) {
                Vuelo vuelo = vuelosCancelables.get(random.nextInt(vuelosCancelables.size()));
                GestorCancelaciones.ResultadoCancelacion resultado = 
                    gestorCancelaciones.cancelarManual(vuelo.getNumeroVuelo(), "Simulación de crisis");
                
                if (resultado.isExitoso()) {
                    eventosCrisis.add("Cancelación: " + vuelo.getNumeroVuelo());
                    this.cancelacionesAplicadas++;
                }
            }
        }
    }
    
    /**
     * Simula demoras aleatorias de vuelos
     */
    private void simularDemorasAleatorias() {
        List<Vuelo> vuelosDemorables = gestorDemoras.getVuelosDemorables();
        
        if (!vuelosDemorables.isEmpty()) {
            // Demorar hasta 5 vuelos aleatoriamente
            int vuelosADemorar = Math.min(5, vuelosDemorables.size());
            Random random = new Random();
            
            for (int i = 0; i < vuelosADemorar; i++) {
                Vuelo vuelo = vuelosDemorables.get(random.nextInt(vuelosDemorables.size()));
                GestorDemoras.ResultadoDemora resultado = 
                    gestorDemoras.aplicarDemora(vuelo.getNumeroVuelo(), "Simulación meteorológica");
                
                if (resultado.isExitoso()) {
                    eventosCrisis.add("Demora: " + vuelo.getNumeroVuelo());
                    this.demorasAplicadas++;
                }
            }
        }
    }
    
    /**
     * Genera reporte integrado con todas las funcionalidades
     */
    private void generarReporteIntegrado() {
        System.out.println("\n📊 REPORTE INTEGRADO ALNS + NUEVAS FUNCIONALIDADES");
        System.out.println("=" .repeat(80));
        
        // Reporte base de ALNS
        super.imprimirEstadisticas();
        
        // Reporte de nuevas funcionalidades
        System.out.println("\n🆕 NUEVAS FUNCIONALIDADES:");
        System.out.println("   Cancelaciones aplicadas: " + cancelacionesAplicadas);
        System.out.println("   Demoras aplicadas: " + demorasAplicadas);
        System.out.println("   Reasignaciones realizadas: " + reasignacionesRealizadas);
        System.out.println("   Eventos de crisis: " + eventosCrisis.size());
        
        if (!eventosCrisis.isEmpty()) {
            System.out.println("\n🚨 Eventos de Crisis:");
            for (String evento : eventosCrisis) {
                System.out.println("   - " + evento);
            }
        }
        
        // Reportes de servicios individuales
        if (gestorCancelaciones != null) {
            System.out.println("\n" + "=" .repeat(40));
            gestorCancelaciones.generarReporte();
        }
        
        if (gestorDemoras != null) {
            System.out.println("\n" + "=" .repeat(40));
            gestorDemoras.generarReporte();
        }
        
        if (gestorReasignacion != null) {
            System.out.println("\n" + "=" .repeat(40));
            gestorReasignacion.generarReporte();
        }
        
        if (gestorProductos != null) {
            System.out.println("\n" + "=" .repeat(40));
            gestorProductos.generarReporteConsolidado();
        }
    }
    
    /**
     * Obtiene estadísticas integradas
     */
    public EstadisticasIntegradas getEstadisticasIntegradas() {
        return new EstadisticasIntegradas(
            getMejorFitness(),
            getHistorialFitness(),
            cancelacionesAplicadas,
            demorasAplicadas,
            reasignacionesRealizadas,
            eventosCrisis.size(),
            gestorCancelaciones != null ? gestorCancelaciones.getEstadisticas() : null,
            gestorDemoras != null ? gestorDemoras.getEstadisticas() : null,
            gestorReasignacion != null ? gestorReasignacion.getEstadisticas() : null
        );
    }
    
    /**
     * Clase para estadísticas integradas
     */
    public static class EstadisticasIntegradas {
        private final double mejorFitness;
        private final List<Double> historialFitness;
        private final int cancelacionesAplicadas;
        private final int demorasAplicadas;
        private final int reasignacionesRealizadas;
        private final int eventosCrisis;
        private final GestorCancelaciones.EstadisticasCancelaciones statsCancelaciones;
        private final GestorDemoras.EstadisticasDemoras statsDemoras;
        private final GestorReasignacion.EstadisticasReasignacion statsReasignacion;
        
        public EstadisticasIntegradas(double mejorFitness, List<Double> historialFitness,
                                    int cancelacionesAplicadas, int demorasAplicadas,
                                    int reasignacionesRealizadas, int eventosCrisis,
                                    GestorCancelaciones.EstadisticasCancelaciones statsCancelaciones,
                                    GestorDemoras.EstadisticasDemoras statsDemoras,
                                    GestorReasignacion.EstadisticasReasignacion statsReasignacion) {
            this.mejorFitness = mejorFitness;
            this.historialFitness = historialFitness;
            this.cancelacionesAplicadas = cancelacionesAplicadas;
            this.demorasAplicadas = demorasAplicadas;
            this.reasignacionesRealizadas = reasignacionesRealizadas;
            this.eventosCrisis = eventosCrisis;
            this.statsCancelaciones = statsCancelaciones;
            this.statsDemoras = statsDemoras;
            this.statsReasignacion = statsReasignacion;
        }
        
        // Getters
        public double getMejorFitness() { return mejorFitness; }
        public List<Double> getHistorialFitness() { return historialFitness; }
        public int getCancelacionesAplicadas() { return cancelacionesAplicadas; }
        public int getDemorasAplicadas() { return demorasAplicadas; }
        public int getReasignacionesRealizadas() { return reasignacionesRealizadas; }
        public int getEventosCrisis() { return eventosCrisis; }
        public GestorCancelaciones.EstadisticasCancelaciones getStatsCancelaciones() { return statsCancelaciones; }
        public GestorDemoras.EstadisticasDemoras getStatsDemoras() { return statsDemoras; }
        public GestorReasignacion.EstadisticasReasignacion getStatsReasignacion() { return statsReasignacion; }
    }
}
