package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.enums.EstadoGeneral;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestor integrado de productos que coordina reasignaciones, cancelaciones y demoras
 * Actúa como orquestador de todos los servicios relacionados con productos
 */
public class GestorProductos {
    
    private final Map<String, Paquete> paquetes;
    private final Map<String, Pedido> pedidos;
    private final GestorReasignacion gestorReasignacion;
    private final GestorCancelaciones gestorCancelaciones;
    private final GestorDemoras gestorDemoras;
    
    public GestorProductos(Map<String, Paquete> paquetes, Map<String, Pedido> pedidos,
                          Map<String, com.grupo5e.morapack.core.model.Vuelo> vuelos) {
        this.paquetes = paquetes;
        this.pedidos = pedidos;
        this.gestorReasignacion = new GestorReasignacion(paquetes, pedidos);
        this.gestorCancelaciones = new GestorCancelaciones(vuelos);
        this.gestorDemoras = new GestorDemoras(vuelos);
    }
    
    /**
     * Maneja el impacto de una cancelación de vuelo en los productos
     */
    public List<GestorReasignacion.ResultadoReasignacion> manejarCancelacionVuelo(String numeroVuelo, 
                                                                                 String motivo) {
        // 1. Cancelar el vuelo
        GestorCancelaciones.ResultadoCancelacion resultadoCancelacion = 
            gestorCancelaciones.cancelarProgramado(numeroVuelo, motivo);
        
        if (!resultadoCancelacion.isExitoso()) {
            return Arrays.asList(new GestorReasignacion.ResultadoReasignacion(false, 
                "Error al cancelar vuelo: " + resultadoCancelacion.getMensaje()));
        }
        
        // 2. Reasignar productos afectados
        List<GestorReasignacion.ResultadoReasignacion> resultadosReasignacion = 
            gestorReasignacion.simularReasignacionPorCancelacionVuelo(
                numeroVuelo, 
                "CLI_EMERGENCIA", 
                "AEROPUERTO_ALTERNATIVO", 
                "PED_EMERGENCIA"
            );
        
        return resultadosReasignacion;
    }
    
    /**
     * Maneja el impacto de una demora de vuelo en los productos
     */
    public List<GestorDemoras.ResultadoDemora> manejarDemoraVuelo(String numeroVuelo, String motivo) {
        // 1. Aplicar demora al vuelo
        GestorDemoras.ResultadoDemora resultadoDemora = 
            gestorDemoras.aplicarDemora(numeroVuelo, motivo);
        
        if (!resultadoDemora.isExitoso()) {
            return Arrays.asList(resultadoDemora);
        }
        
        // 2. Actualizar estados de productos en tránsito
        List<Paquete> productosAfectados = paquetes.values().stream()
            .filter(p -> p.getEstado() == EstadoGeneral.EN_TRANSITO)
            .filter(p -> p.getAeropuertoActual().contains(numeroVuelo))
            .collect(Collectors.toList());
        
        for (Paquete producto : productosAfectados) {
            producto.setEstado(EstadoGeneral.DEMORADO);
        }
        
        return Arrays.asList(resultadoDemora);
    }
    
    /**
     * Optimiza la distribución de productos por proximidad geográfica
     */
    public List<GestorReasignacion.ResultadoReasignacion> optimizarDistribucion() {
        // Optimizando distribución de productos
        
        List<GestorReasignacion.ResultadoReasignacion> resultados = new ArrayList<>();
        
        // Obtener aeropuertos con productos reasignables
        Map<String, List<Paquete>> productosPorAeropuerto = 
            gestorReasignacion.getPaquetesReasignablesPorUbicacion();
        
        for (String aeropuerto : productosPorAeropuerto.keySet()) {
            List<GestorReasignacion.ResultadoReasignacion> resultadosAeropuerto = 
                gestorReasignacion.optimizarReasignacionesPorProximidad(aeropuerto);
            resultados.addAll(resultadosAeropuerto);
        }
        
        // Optimización completada
        return resultados;
    }
    
    /**
     * Simula un escenario de crisis (múltiples cancelaciones y demoras)
     */
    public void simularEscenarioCrisis() {
        // 1. Cancelar varios vuelos
        String[] vuelosACancelar = {"LA1234", "LA5678", "LA9012"};
        for (String vuelo : vuelosACancelar) {
            manejarCancelacionVuelo(vuelo, "Crisis operativa");
        }
        
        // 2. Demorar otros vuelos
        String[] vuelosADemorar = {"LA3456", "LA7890"};
        for (String vuelo : vuelosADemorar) {
            manejarDemoraVuelo(vuelo, "Condiciones meteorológicas adversas");
        }
        
        // 3. Optimizar distribución
        optimizarDistribucion();
    }
    
    /**
     * Obtiene estadísticas consolidadas del sistema
     */
    public EstadisticasConsolidadas getEstadisticasConsolidadas() {
        GestorReasignacion.EstadisticasReasignacion statsReasignacion = 
            gestorReasignacion.getEstadisticas();
        GestorCancelaciones.EstadisticasCancelaciones statsCancelaciones = 
            gestorCancelaciones.getEstadisticas();
        GestorDemoras.EstadisticasDemoras statsDemoras = 
            gestorDemoras.getEstadisticas();
        
        return new EstadisticasConsolidadas(
            statsReasignacion,
            statsCancelaciones,
            statsDemoras,
            paquetes.size(),
            pedidos.size()
        );
    }
    
    /**
     * Genera reporte consolidado del sistema
     */
    public void generarReporteConsolidado() {
        // Generar reportes individuales
        gestorReasignacion.generarReporte();
        gestorCancelaciones.generarReporte();
        gestorDemoras.generarReporte();
    }
    
    /**
     * Clase para estadísticas consolidadas
     */
    public static class EstadisticasConsolidadas {
        private final GestorReasignacion.EstadisticasReasignacion statsReasignacion;
        private final GestorCancelaciones.EstadisticasCancelaciones statsCancelaciones;
        private final GestorDemoras.EstadisticasDemoras statsDemoras;
        private final int totalProductos;
        private final int totalPedidos;
        
        public EstadisticasConsolidadas(GestorReasignacion.EstadisticasReasignacion statsReasignacion,
                                      GestorCancelaciones.EstadisticasCancelaciones statsCancelaciones,
                                      GestorDemoras.EstadisticasDemoras statsDemoras,
                                      int totalProductos, int totalPedidos) {
            this.statsReasignacion = statsReasignacion;
            this.statsCancelaciones = statsCancelaciones;
            this.statsDemoras = statsDemoras;
            this.totalProductos = totalProductos;
            this.totalPedidos = totalPedidos;
        }
        
        // Getters
        public GestorReasignacion.EstadisticasReasignacion getStatsReasignacion() { return statsReasignacion; }
        public GestorCancelaciones.EstadisticasCancelaciones getStatsCancelaciones() { return statsCancelaciones; }
        public GestorDemoras.EstadisticasDemoras getStatsDemoras() { return statsDemoras; }
        public int getTotalProductos() { return totalProductos; }
        public int getTotalPedidos() { return totalPedidos; }
    }
}
