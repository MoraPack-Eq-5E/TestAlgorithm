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
        System.out.println("🛫 Manejando cancelación de vuelo: " + numeroVuelo);
        
        // 1. Cancelar el vuelo
        GestorCancelaciones.ResultadoCancelacion resultadoCancelacion = 
            gestorCancelaciones.cancelarProgramado(numeroVuelo, motivo);
        
        if (!resultadoCancelacion.isExitoso()) {
            System.out.println("❌ Error al cancelar vuelo: " + resultadoCancelacion.getMensaje());
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
        
        System.out.println("✅ Cancelación y reasignación completadas");
        return resultadosReasignacion;
    }
    
    /**
     * Maneja el impacto de una demora de vuelo en los productos
     */
    public List<GestorDemoras.ResultadoDemora> manejarDemoraVuelo(String numeroVuelo, String motivo) {
        System.out.println("⏰ Manejando demora de vuelo: " + numeroVuelo);
        
        // 1. Aplicar demora al vuelo
        GestorDemoras.ResultadoDemora resultadoDemora = 
            gestorDemoras.aplicarDemora(numeroVuelo, motivo);
        
        if (!resultadoDemora.isExitoso()) {
            System.out.println("❌ Error al aplicar demora: " + resultadoDemora.getMensaje());
            return Arrays.asList(resultadoDemora);
        }
        
        // 2. Actualizar estados de productos en tránsito
        List<Paquete> productosAfectados = paquetes.values().stream()
            .filter(p -> p.getEstado() == EstadoGeneral.EN_TRANSITO)
            .filter(p -> p.getAeropuertoActual().contains(numeroVuelo))
            .collect(Collectors.toList());
        
        for (Paquete producto : productosAfectados) {
            producto.setEstado(EstadoGeneral.DEMORADO);
            System.out.println("   Producto " + producto.getId() + " marcado como demorado");
        }
        
        System.out.println("✅ Demora aplicada a " + productosAfectados.size() + " productos");
        return Arrays.asList(resultadoDemora);
    }
    
    /**
     * Optimiza la distribución de productos por proximidad geográfica
     */
    public List<GestorReasignacion.ResultadoReasignacion> optimizarDistribucion() {
        System.out.println("🌍 Optimizando distribución de productos...");
        
        List<GestorReasignacion.ResultadoReasignacion> resultados = new ArrayList<>();
        
        // Obtener aeropuertos con productos reasignables
        Map<String, List<Paquete>> productosPorAeropuerto = 
            gestorReasignacion.getPaquetesReasignablesPorUbicacion();
        
        for (String aeropuerto : productosPorAeropuerto.keySet()) {
            List<GestorReasignacion.ResultadoReasignacion> resultadosAeropuerto = 
                gestorReasignacion.optimizarReasignacionesPorProximidad(aeropuerto);
            resultados.addAll(resultadosAeropuerto);
        }
        
        int exitosas = (int) resultados.stream()
            .filter(GestorReasignacion.ResultadoReasignacion::isExitoso)
            .count();
        
        System.out.println("✅ Optimización completada: " + exitosas + "/" + resultados.size() + " exitosas");
        return resultados;
    }
    
    /**
     * Simula un escenario de crisis (múltiples cancelaciones y demoras)
     */
    public void simularEscenarioCrisis() {
        System.out.println("🚨 SIMULANDO ESCENARIO DE CRISIS");
        System.out.println("=" .repeat(50));
        
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
        
        System.out.println("✅ Escenario de crisis simulado exitosamente");
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
        System.out.println("\n📊 REPORTE CONSOLIDADO DEL SISTEMA");
        System.out.println("=" .repeat(80));
        
        EstadisticasConsolidadas stats = getEstadisticasConsolidadas();
        
        System.out.println("📈 Resumen General:");
        System.out.println("   Total de productos: " + stats.getTotalProductos());
        System.out.println("   Total de pedidos: " + stats.getTotalPedidos());
        System.out.println("   Productos reasignables: " + stats.getStatsReasignacion().getPaquetesReasignables());
        System.out.println("   Cancelaciones totales: " + stats.getStatsCancelaciones().getTotalCancelaciones());
        System.out.println("   Demoras totales: " + stats.getStatsDemoras().getTotalDemoras());
        
        System.out.println("\n🔄 Reasignaciones:");
        System.out.println("   Total: " + stats.getStatsReasignacion().getTotalReasignaciones());
        System.out.println("   Por ubicación: " + stats.getStatsReasignacion().getReasignacionesPorUbicacion());
        
        System.out.println("\n🛫 Cancelaciones:");
        System.out.println("   Manuales: " + stats.getStatsCancelaciones().getCancelacionesManuales());
        System.out.println("   Programadas: " + stats.getStatsCancelaciones().getCancelacionesProgramadas());
        System.out.println("   Paquetes afectados: " + stats.getStatsCancelaciones().getTotalPaquetesAfectados());
        
        System.out.println("\n⏰ Demoras:");
        System.out.println("   Vuelos con demora: " + stats.getStatsDemoras().getVuelosConDemora());
        System.out.println("   Promedio horas: " + String.format("%.1f", stats.getStatsDemoras().getPromedioHorasDemora()));
        
        // Reportes individuales
        System.out.println("\n" + "=" .repeat(80));
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
