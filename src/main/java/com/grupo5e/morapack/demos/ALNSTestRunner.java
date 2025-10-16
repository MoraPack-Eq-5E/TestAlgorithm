package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.algorithm.alns.ALNSSolver;
import com.grupo5e.morapack.service.AeropuertoService;
import com.grupo5e.morapack.service.PedidoService;
import com.grupo5e.morapack.service.VueloService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ALNSTestRunner implements CommandLineRunner {

    private final AeropuertoService aeropuertoService;
    private final PedidoService pedidoService;
    private final VueloService vueloService;

    // Constructor con dependencias inyectadas
    public ALNSTestRunner(AeropuertoService aeropuertoService, PedidoService pedidoService,VueloService vueloService) {
        this.aeropuertoService = aeropuertoService;
        this.pedidoService = pedidoService;
        this.vueloService = vueloService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 INICIANDO PRUEBA DEL ALNSSOLVER 🚀");

        try {
            // Verificar que los servicios estén disponibles
            System.out.println("\n=== VERIFICANDO SERVICIOS ===");
            System.out.println("AeropuertoService: " + (aeropuertoService != null ? "✅ DISPONIBLE" : "❌ NO DISPONIBLE"));
            System.out.println("PedidoService: " + (pedidoService != null ? "✅ DISPONIBLE" : "❌ NO DISPONIBLE"));

            if (aeropuertoService == null || pedidoService == null || vueloService == null) {
                throw new RuntimeException("Servicios no disponibles");
            }

//            // Verificar datos
//            System.out.println("\n=== VERIFICANDO DATOS ===");
//            var aeropuertos = aeropuertoService.listar();
//            var pedidos = pedidoService.listar();
//
//            System.out.println("Aeropuertos en BD: " + aeropuertos.size());
//            System.out.println("Pedidos en BD: " + pedidos.size());

//            if (aeropuertos.isEmpty() || pedidos.isEmpty()) {
//                System.out.println("⚠️  ADVERTENCIA: Pocos datos en la BD");
//                System.out.println("   - Considera agregar datos de prueba");
//            }

//            // Mostrar muestra de datos
//            System.out.println("\n=== MUESTRA DE DATOS ===");
//            System.out.println("Primeros 3 aeropuertos:");
//            aeropuertos.stream().limit(3).forEach(a ->
//                    System.out.println("  - " + a.getCodigoIATA() + " - " +
//                            (a.getCiudad() != null ? a.getCiudad().getNombre() : "Sin ciudad")));
//
//            System.out.println("\nPrimeros 3 pedidos:");
//            pedidos.stream().limit(3).forEach(p ->
//                    System.out.println("  - Pedido " + p.getId() + ": " +
//                            p.getAeropuertoOrigenCodigo() + " → " + p.getAeropuertoDestinoCodigo()));

            // Crear solver
            System.out.println("\n=== INICIALIZANDO ALNSSOLVER ===");
            ALNSSolver solver = new ALNSSolver(aeropuertoService, pedidoService,vueloService);

            // Ejecutar algoritmo
            System.out.println("\n=== EJECUTANDO ALGORITMO ===");
            long startTime = System.currentTimeMillis();

            solver.resolver();

            long endTime = System.currentTimeMillis();
            System.out.println("⏱️  Tiempo de ejecución: " + (endTime - startTime) + "ms");

            // Validar resultados
            System.out.println("\n=== VALIDACIÓN ===");
            boolean valida = solver.esSolucionValida();
            boolean capacidadValida = solver.esSolucionCapacidadValida();

            System.out.println("Solución válida: " + (valida ? "✅ SÍ" : "❌ NO"));
            System.out.println("Capacidades respetadas: " + (capacidadValida ? "✅ SÍ" : "❌ NO"));

            if (valida && capacidadValida) {
                System.out.println("🎉 ¡PRUEBA EXITOSA!");
            }

        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}