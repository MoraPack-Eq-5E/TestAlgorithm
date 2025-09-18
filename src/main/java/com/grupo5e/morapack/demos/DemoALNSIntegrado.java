package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.algorithm.alns.ALNSSolverIntegrado;
import com.grupo5e.morapack.core.model.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Demo completo del ALNS integrado con todas las nuevas funcionalidades
 */
public class DemoALNSIntegrado {
    
    public static void main(String[] args) {
        System.out.println("🚀 DEMO: ALNS Integrado con Nuevas Funcionalidades");
        System.out.println("=" .repeat(80));
        
        // 1. Crear datos de ejemplo
        List<Paquete> paquetes = crearPaquetesEjemplo();
        List<Aeropuerto> aeropuertos = crearAeropuertosEjemplo();
        List<Vuelo> vuelos = crearVuelosEjemplo();
        Set<Continente> continentes = crearContinentesEjemplo();
        List<Pedido> pedidos = crearPedidosEjemplo();
        
        System.out.println("📦 Datos de ejemplo creados:");
        System.out.println("   - Paquetes: " + paquetes.size());
        System.out.println("   - Aeropuertos: " + aeropuertos.size());
        System.out.println("   - Vuelos: " + vuelos.size());
        System.out.println("   - Continentes: " + continentes.size());
        System.out.println("   - Pedidos: " + pedidos.size());
        System.out.println();
        
        // 2. Crear ALNS integrado
        ALNSSolverIntegrado alnsIntegrado = new ALNSSolverIntegrado(100, 100.0, 0.95);
        
        // 3. Configurar problema integrado
        alnsIntegrado.configurarProblemaIntegrado(paquetes, aeropuertos, vuelos, continentes, pedidos);
        
        // 4. Configurar escenarios
        alnsIntegrado.configurarEscenarios(
            true,  // Simular cancelaciones
            true,  // Simular demoras
            true,  // Permitir reasignaciones
            "data/pedidos_mes1.txt",  // Archivo de pedidos
            "data/cancelaciones_ejemplo.txt"  // Archivo de cancelaciones
        );
        
        System.out.println();
        
        // 5. Ejecutar ALNS integrado
        System.out.println("🎯 Ejecutando ALNS Integrado...");
        long tiempoInicio = System.currentTimeMillis();
        
        Solucion solucion = alnsIntegrado.resolver();
        
        long tiempoFin = System.currentTimeMillis();
        double tiempoEjecucion = (tiempoFin - tiempoInicio) / 1000.0;
        
        System.out.println();
        System.out.println("⏱️ Tiempo de ejecución: " + String.format("%.2f", tiempoEjecucion) + " segundos");
        
        // 6. Mostrar resultados
        System.out.println("\n📊 RESULTADOS FINALES:");
        System.out.println("   - Fitness de la solución: " + solucion.getFitness());
        System.out.println("   - Paquetes resueltos: " + solucion.getCantidadPaquetes());
        System.out.println("   - Costo total: " + solucion.getCostoTotal());
        System.out.println("   - Tiempo total: " + solucion.getTiempoTotalHoras() + " horas");
        System.out.println("   - Solución factible: " + solucion.isEsFactible());
        
        // 7. Obtener estadísticas integradas
        ALNSSolverIntegrado.EstadisticasIntegradas stats = alnsIntegrado.getEstadisticasIntegradas();
        
        System.out.println("\n🆕 ESTADÍSTICAS DE NUEVAS FUNCIONALIDADES:");
        System.out.println("   - Cancelaciones aplicadas: " + stats.getCancelacionesAplicadas());
        System.out.println("   - Demoras aplicadas: " + stats.getDemorasAplicadas());
        System.out.println("   - Reasignaciones realizadas: " + stats.getReasignacionesRealizadas());
        System.out.println("   - Eventos de crisis: " + stats.getEventosCrisis());
        
        // 8. Mostrar detalles de servicios
        if (stats.getStatsCancelaciones() != null) {
            System.out.println("\n🛫 CANCELACIONES:");
            System.out.println("   - Total: " + stats.getStatsCancelaciones().getTotalCancelaciones());
            System.out.println("   - Manuales: " + stats.getStatsCancelaciones().getCancelacionesManuales());
            System.out.println("   - Programadas: " + stats.getStatsCancelaciones().getCancelacionesProgramadas());
        }
        
        if (stats.getStatsDemoras() != null) {
            System.out.println("\n⏰ DEMORAS:");
            System.out.println("   - Total: " + stats.getStatsDemoras().getTotalDemoras());
            System.out.println("   - Vuelos con demora: " + stats.getStatsDemoras().getVuelosConDemora());
            System.out.println("   - Promedio horas: " + String.format("%.1f", stats.getStatsDemoras().getPromedioHorasDemora()));
        }
        
        if (stats.getStatsReasignacion() != null) {
            System.out.println("\n🔄 REASIGNACIONES:");
            System.out.println("   - Total: " + stats.getStatsReasignacion().getTotalReasignaciones());
            System.out.println("   - Paquetes reasignables: " + stats.getStatsReasignacion().getPaquetesReasignables());
            System.out.println("   - Paquetes no reasignables: " + stats.getStatsReasignacion().getPaquetesNoReasignables());
        }
        
        System.out.println("\n🎯 Demo completado exitosamente!");
    }
    
    /**
     * Crea paquetes de ejemplo
     */
    private static List<Paquete> crearPaquetesEjemplo() {
        List<Paquete> paquetes = new ArrayList<>();
        
        // Paquetes con diferentes estados y ubicaciones
        for (int i = 1; i <= 20; i++) {
            Paquete paquete = new Paquete(
                "PAQ" + String.format("%03d", i),
                "SKBO",  // Origen fijo
                i % 2 == 0 ? "SVMI" : "SBBR",  // Destinos alternados
                "CLI" + String.format("%03d", (i % 5) + 1)  // Clientes rotativos
            );
            
            // Asignar diferentes estados
            if (i <= 5) {
                paquete.setEstado(com.grupo5e.morapack.core.enums.EstadoGeneral.EN_TRANSITO);
                paquete.setAeropuertoActual("EN_VUELO_LA" + i);
            } else if (i <= 10) {
                paquete.setEstado(com.grupo5e.morapack.core.enums.EstadoGeneral.EN_ALMACEN);
                paquete.setAeropuertoActual("SKBO");
                paquete.setTipoAlmacen("paso");
            } else {
                paquete.setEstado(com.grupo5e.morapack.core.enums.EstadoGeneral.EN_ALMACEN);
                paquete.setAeropuertoActual(paquete.getAeropuertoDestino());
                paquete.setTipoAlmacen("entrega");
            }
            
            paquetes.add(paquete);
        }
        
        return paquetes;
    }
    
    /**
     * Crea aeropuertos de ejemplo
     */
    private static List<Aeropuerto> crearAeropuertosEjemplo() {
        List<Aeropuerto> aeropuertos = new ArrayList<>();
        
        String[][] datosAeropuertos = {
            {"SKBO", "Bogotá", "Colombia", "true"},
            {"SVMI", "Caracas", "Venezuela", "false"},
            {"SBBR", "Brasilia", "Brasil", "false"},
            {"SPIM", "Lima", "Perú", "false"},
            {"SCEL", "Santiago", "Chile", "false"},
            {"LATI", "Bruselas", "Bélgica", "true"},
            {"EDDI", "Berlín", "Alemania", "false"},
            {"LOWW", "Viena", "Austria", "false"}
        };
        
        for (String[] datos : datosAeropuertos) {
            Aeropuerto aeropuerto = new Aeropuerto();
            aeropuerto.setCodigoIATA(datos[0]);
            aeropuerto.setPais(datos[2]);
            aeropuerto.setEsSedeMoraPack(Boolean.parseBoolean(datos[3]));
            aeropuertos.add(aeropuerto);
        }
        
        return aeropuertos;
    }
    
    /**
     * Crea vuelos de ejemplo
     */
    private static List<Vuelo> crearVuelosEjemplo() {
        List<Vuelo> vuelos = new ArrayList<>();
        
        // Vuelos domésticos (mismo continente)
        vuelos.add(new Vuelo("LA1234", "SKBO", "SVMI", true, 250));
        vuelos.add(new Vuelo("LA5678", "SVMI", "SBBR", true, 200));
        vuelos.add(new Vuelo("LA9012", "SBBR", "SPIM", true, 300));
        
        // Vuelos internacionales (distinto continente)
        vuelos.add(new Vuelo("LA3456", "SKBO", "LATI", false, 350));
        vuelos.add(new Vuelo("LA7890", "SVMI", "EDDI", false, 400));
        vuelos.add(new Vuelo("LA1111", "SBBR", "LOWW", false, 300));
        
        // Vuelos de retorno
        vuelos.add(new Vuelo("LA2222", "LATI", "SKBO", false, 350));
        vuelos.add(new Vuelo("LA3333", "EDDI", "SVMI", false, 400));
        vuelos.add(new Vuelo("LA4444", "LOWW", "SBBR", false, 300));
        
        return vuelos;
    }
    
    /**
     * Crea continentes de ejemplo
     */
    private static Set<Continente> crearContinentesEjemplo() {
        Set<Continente> continentes = new HashSet<>();
        
        // América del Sur
        Continente america = new Continente();
        america.setCodigo("AMERICA");
        america.setNombre("América del Sur");
        america.setCodigosIATAAeropuertos(new HashSet<>(Arrays.asList("SKBO", "SVMI", "SBBR", "SPIM", "SCEL")));
        continentes.add(america);
        
        // Europa
        Continente europa = new Continente();
        europa.setCodigo("EUROPA");
        europa.setNombre("Europa");
        europa.setCodigosIATAAeropuertos(new HashSet<>(Arrays.asList("LATI", "EDDI", "LOWW")));
        continentes.add(europa);
        
        return continentes;
    }
    
    /**
     * Crea pedidos de ejemplo
     */
    private static List<Pedido> crearPedidosEjemplo() {
        List<Pedido> pedidos = new ArrayList<>();
        
        for (int i = 1; i <= 10; i++) {
            Pedido pedido = new Pedido(
                "PED" + String.format("%04d", i),
                "CLI" + String.format("%03d", i),
                i % 2 == 0 ? "SVMI" : "LATI",
                (i % 3) + 1  // 1-3 productos por pedido
            );
            
            pedido.setFechaLimiteEntrega(LocalDateTime.now().plusDays(2 + (i % 2)));
            pedidos.add(pedido);
        }
        
        return pedidos;
    }
}
