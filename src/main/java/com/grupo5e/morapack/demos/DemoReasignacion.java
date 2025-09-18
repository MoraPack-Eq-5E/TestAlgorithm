package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.enums.EstadoGeneral;
import com.grupo5e.morapack.service.GestorReasignacion;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo para mostrar el funcionamiento del sistema de reasignación
 */
public class DemoReasignacion {
    
    public static void main(String[] args) {
        System.out.println("🚀 DEMO: Sistema de Reasignación de Productos");
        System.out.println("=" .repeat(60));
        
        // Crear paquetes y pedidos de ejemplo
        Map<String, Paquete> paquetes = crearPaquetesEjemplo();
        Map<String, Pedido> pedidos = crearPedidosEjemplo();
        
        // Crear gestor de reasignación
        GestorReasignacion gestorReasignacion = new GestorReasignacion(paquetes, pedidos);
        
        System.out.println("📦 Paquetes creados: " + paquetes.size());
        System.out.println("📋 Pedidos creados: " + pedidos.size());
        System.out.println();
        
        // Demo 1: Verificar paquetes reasignables
        System.out.println("🔍 DEMO 1: Verificar Paquetes Reasignables");
        System.out.println("-" .repeat(50));
        
        List<Paquete> paquetesReasignables = gestorReasignacion.getPaquetesReasignables();
        System.out.println("Paquetes reasignables: " + paquetesReasignables.size());
        
        for (Paquete paquete : paquetesReasignables) {
            System.out.println("   " + paquete.getId() + " - " + 
                             paquete.getEstado() + " - " + 
                             paquete.getTipoAlmacen() + " - " +
                             paquete.getAeropuertoActual());
        }
        
        System.out.println();
        
        // Demo 2: Reasignación individual
        System.out.println("🔄 DEMO 2: Reasignación Individual");
        System.out.println("-" .repeat(50));
        
        if (!paquetesReasignables.isEmpty()) {
            Paquete paqueteAReasignar = paquetesReasignables.get(0);
            System.out.println("Reasignando paquete: " + paqueteAReasignar.getId());
            
            GestorReasignacion.ResultadoReasignacion resultado = 
                gestorReasignacion.reasignarPaquete(
                    paqueteAReasignar.getId(),
                    "CLI999",
                    "LATI",
                    "PED_9999"
                );
            
            System.out.println("Resultado: " + resultado.toString());
        }
        
        System.out.println();
        
        // Demo 3: Reasignación masiva
        System.out.println("🔄 DEMO 3: Reasignación Masiva");
        System.out.println("-" .repeat(50));
        
        List<Paquete> paquetesParaReasignar = paquetesReasignables.stream()
            .limit(3)
            .toList();
        
        if (!paquetesParaReasignar.isEmpty()) {
            List<String> paqueteIds = paquetesParaReasignar.stream()
                .map(Paquete::getId)
                .toList();
            
            System.out.println("Reasignando " + paqueteIds.size() + " paquetes masivamente...");
            
            List<GestorReasignacion.ResultadoReasignacion> resultados = 
                gestorReasignacion.reasignarPaquetesMasiva(
                    paqueteIds,
                    "CLI888",
                    "EDDI",
                    "PED_8888"
                );
            
            int exitosas = (int) resultados.stream().filter(GestorReasignacion.ResultadoReasignacion::isExitoso).count();
            System.out.println("Reasignaciones exitosas: " + exitosas + "/" + resultados.size());
        }
        
        System.out.println();
        
        // Demo 4: Reasignación por cancelación de vuelo
        System.out.println("🛫 DEMO 4: Reasignación por Cancelación de Vuelo");
        System.out.println("-" .repeat(50));
        
        List<GestorReasignacion.ResultadoReasignacion> resultadosCancelacion = 
            gestorReasignacion.simularReasignacionPorCancelacionVuelo(
                "LA1234",
                "CLI777",
                "LOWW",
                "PED_7777"
            );
        
        System.out.println("Reasignaciones por cancelación: " + resultadosCancelacion.size());
        for (GestorReasignacion.ResultadoReasignacion resultado : resultadosCancelacion) {
            System.out.println("   " + resultado.toString());
        }
        
        System.out.println();
        
        // Demo 5: Optimización por proximidad
        System.out.println("🌍 DEMO 5: Optimización por Proximidad");
        System.out.println("-" .repeat(50));
        
        List<GestorReasignacion.ResultadoReasignacion> resultadosOptimizacion = 
            gestorReasignacion.optimizarReasignacionesPorProximidad("SKBO");
        
        System.out.println("Reasignaciones optimizadas: " + resultadosOptimizacion.size());
        int exitosasOptimizacion = (int) resultadosOptimizacion.stream()
            .filter(GestorReasignacion.ResultadoReasignacion::isExitoso)
            .count();
        System.out.println("Optimizaciones exitosas: " + exitosasOptimizacion);
        
        System.out.println();
        
        // Demo 6: Reportes
        System.out.println("📊 DEMO 6: Reportes");
        System.out.println("-" .repeat(50));
        
        gestorReasignacion.generarReporte();
        
        System.out.println("\n🎯 Demo completado exitosamente!");
    }
    
    /**
     * Crea paquetes de ejemplo con diferentes estados y ubicaciones
     */
    private static Map<String, Paquete> crearPaquetesEjemplo() {
        Map<String, Paquete> paquetes = new HashMap<>();
        
        // Paquetes en tránsito (reasignables)
        Paquete paquete1 = new Paquete("PAQ001", "SKBO", "SVMI", "CLI001");
        paquete1.setEstado(EstadoGeneral.EN_TRANSITO);
        paquete1.setAeropuertoActual("EN_VUELO_LA1234");
        paquete1.setTipoAlmacen("paso");
        paquete1.setPedidoOriginalId("PED_0001");
        paquetes.put("PAQ001", paquete1);
        
        Paquete paquete2 = new Paquete("PAQ002", "SVMI", "SBBR", "CLI002");
        paquete2.setEstado(EstadoGeneral.EN_TRANSITO);
        paquete2.setAeropuertoActual("EN_VUELO_LA5678");
        paquete2.setTipoAlmacen("paso");
        paquete2.setPedidoOriginalId("PED_0002");
        paquetes.put("PAQ002", paquete2);
        
        // Paquetes en almacén de paso (reasignables)
        Paquete paquete3 = new Paquete("PAQ003", "SBBR", "SPIM", "CLI003");
        paquete3.setEstado(EstadoGeneral.EN_ALMACEN);
        paquete3.setAeropuertoActual("SBBR");
        paquete3.setTipoAlmacen("paso");
        paquete3.setPedidoOriginalId("PED_0003");
        paquetes.put("PAQ003", paquete3);
        
        Paquete paquete4 = new Paquete("PAQ004", "SPIM", "SCEL", "CLI004");
        paquete4.setEstado(EstadoGeneral.EN_ALMACEN);
        paquete4.setAeropuertoActual("SPIM");
        paquete4.setTipoAlmacen("paso");
        paquete4.setPedidoOriginalId("PED_0004");
        paquetes.put("PAQ004", paquete4);
        
        // Paquetes en almacén de entrega (NO reasignables)
        Paquete paquete5 = new Paquete("PAQ005", "SCEL", "SABE", "CLI005");
        paquete5.setEstado(EstadoGeneral.EN_ALMACEN);
        paquete5.setAeropuertoActual("SABE");
        paquete5.setTipoAlmacen("entrega");
        paquete5.setPedidoOriginalId("PED_0005");
        paquetes.put("PAQ005", paquete5);
        
        Paquete paquete6 = new Paquete("PAQ006", "SABE", "SGAS", "CLI006");
        paquete6.setEstado(EstadoGeneral.EN_ALMACEN);
        paquete6.setAeropuertoActual("SGAS");
        paquete6.setTipoAlmacen("entrega");
        paquete6.setPedidoOriginalId("PED_0006");
        paquetes.put("PAQ006", paquete6);
        
        // Paquetes ya entregados (NO reasignables)
        Paquete paquete7 = new Paquete("PAQ007", "SGAS", "SUAA", "CLI007");
        paquete7.setEstado(EstadoGeneral.ENTREGADO);
        paquete7.setAeropuertoActual("SUAA");
        paquete7.setTipoAlmacen("entrega");
        paquete7.setPedidoOriginalId("PED_0007");
        paquetes.put("PAQ007", paquete7);
        
        return paquetes;
    }
    
    /**
     * Crea pedidos de ejemplo
     */
    private static Map<String, Pedido> crearPedidosEjemplo() {
        Map<String, Pedido> pedidos = new HashMap<>();
        
        // Pedidos existentes
        Pedido pedido1 = new Pedido("PED_0001", "CLI001", "SVMI", 1);
        pedido1.setFechaLimiteEntrega(LocalDateTime.now().plusDays(2));
        pedidos.put("PED_0001", pedido1);
        
        Pedido pedido2 = new Pedido("PED_0002", "CLI002", "SBBR", 1);
        pedido2.setFechaLimiteEntrega(LocalDateTime.now().plusDays(3));
        pedidos.put("PED_0002", pedido2);
        
        Pedido pedido3 = new Pedido("PED_0003", "CLI003", "SPIM", 1);
        pedido3.setFechaLimiteEntrega(LocalDateTime.now().plusDays(1));
        pedidos.put("PED_0003", pedido3);
        
        // Pedidos nuevos para reasignación
        Pedido pedidoNuevo1 = new Pedido("PED_9999", "CLI999", "LATI", 1);
        pedidoNuevo1.setFechaLimiteEntrega(LocalDateTime.now().plusDays(3));
        pedidos.put("PED_9999", pedidoNuevo1);
        
        Pedido pedidoNuevo2 = new Pedido("PED_8888", "CLI888", "EDDI", 3);
        pedidoNuevo2.setFechaLimiteEntrega(LocalDateTime.now().plusDays(2));
        pedidos.put("PED_8888", pedidoNuevo2);
        
        return pedidos;
    }
}
