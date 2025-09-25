package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.algorithm.alns.ALNSSolver;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;
import com.grupo5e.morapack.algorithm.evaluation.FitnessExperimental;
import com.grupo5e.morapack.utils.MoraPackDataLoader;
import com.grupo5e.morapack.core.model.*;
import java.time.LocalDateTime;
import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Pure ALNS demo - without SedeSelector.
 * Algorithm dynamically decides which MoraPack sede to send each package from.
 * Shows progress per iteration and best final result.
 */
public class DemoALNSPuro {
    
    private static final String PEDIDOS_CSV_FILE = "data/pedidos_generados.csv";
    
    public static void main(String[] args) {
        System.out.println("=== DEMO: PURE ALNS MULTI-DEPOT ===");
        System.out.println("Algorithm dynamically decides which sede to send each package from\n");
        
        try {
            // 1. LOAD DATA
            System.out.println("1. Loading data...");
            DatosCompletos datos = cargarDatos();
            
            // 2. CREATE PACKAGES WITHOUT PRE-ASSIGNED SEDE
            System.out.println("\n2. Creating packages for pure ALNS...");
            List<Paquete> paquetesSinSede = crearPaquetesSinSede(datos);
            
            // 3. CREATE CONTEXT FOR ALNS
            System.out.println("\n3. Preparing ALNS context...");
            ContextoProblema contexto = new ContextoProblema(
                paquetesSinSede, datos.aeropuertos, datos.vuelos, datos.continentes
            );
            
            // 4. EXECUTE ALNS WITH PROGRESS
            System.out.println("\n4. EXECUTING PURE ALNS");
            ejecutarALNSConProgreso(contexto);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static DatosCompletos cargarDatos() {
        List<Aeropuerto> aeropuertos = MoraPackDataLoader.cargarAeropuertos();
        List<Vuelo> vuelos = MoraPackDataLoader.cargarVuelos(aeropuertos);
        Set<Continente> continentes = MoraPackDataLoader.crearContinentes(aeropuertos);
        
        // Use original capacities (not reduced)
        System.out.printf("   %d airports, %d flights, %d continents%n", 
                         aeropuertos.size(), vuelos.size(), continentes.size());
        System.out.println("   Original capacities (natural behavior)");
        
        return new DatosCompletos(aeropuertos, vuelos, continentes);
    }
    
    /**
     * Reduces flight capacities only for this demo, to force natural connections
     */
    private static List<Vuelo> reducirCapacidadesParaDemo(List<Vuelo> vuelosOriginales) {
        List<Vuelo> vuelosReducidos = new ArrayList<>();
        
        for (Vuelo vueloOriginal : vuelosOriginales) {
            // Create copy with reduced capacity
            int capacidadOriginal = vueloOriginal.getCapacidadMaxima();
            int capacidadReducida = Math.min(capacidadOriginal, 120); // Max 120 packages
            
            Vuelo vueloReducido = new Vuelo(
                vueloOriginal.getNumeroVuelo(),
                vueloOriginal.getAeropuertoOrigen(),
                vueloOriginal.getAeropuertoDestino(),
                vueloOriginal.isMismoContinente(),
                capacidadReducida // Reduced capacity
            );
            
            // Copy other attributes
            vueloReducido.setHoraSalida(vueloOriginal.getHoraSalida());
            vueloReducido.setHoraLlegada(vueloOriginal.getHoraLlegada());
            vueloReducido.setDuracionHoras(vueloOriginal.getDuracionHoras());
            
            vuelosReducidos.add(vueloReducido);
        }
        
        return vuelosReducidos;
    }
    
    private static List<Paquete> crearPaquetesSinSede(DatosCompletos datos) {
        List<Paquete> paquetes = new ArrayList<>();
        
        try {
            List<String> lineas = Files.readAllLines(Paths.get(PEDIDOS_CSV_FILE));
            
            // Use original data without artificial saturation - limited for debug
            int totalPedidos = Math.min(10, lineas.size() - 1); // Reduced for debug
            System.out.printf("   Original data debug: %d orders%n", totalPedidos);
            
            for (int i = 1; i <= totalPedidos; i++) {
                String linea = lineas.get(i).trim();
                if (linea.isEmpty()) continue;
                
                String[] partes = linea.split(",");
                if (partes.length != 6) continue;
                
                String destino = partes[3].trim();
                String clienteId = partes[5].trim();
                int cantidad = Integer.parseInt(partes[4].trim());
                
                // Use original quantity without multipliers
                for (int j = 0; j < cantidad; j++) {
                    String paqueteId = String.format("PKG_%s_%03d_%02d", clienteId, cantidad, j + 1);
                    
                    // Key: Package WITHOUT fixed aeropuertoOrigen - ALNS will decide
                    Paquete paquete = new Paquete(paqueteId, null, destino, clienteId);
                    paquete.setPrioridad((j % 3) + 1);
                    paquete.setFechaLimiteEntrega(LocalDateTime.now().plusDays(3));
                    paquetes.add(paquete);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error reading orders: " + e.getMessage());
            paquetes.addAll(crearPaquetesRespaldo());
        }
        
        System.out.printf("   %d packages created (ORIGINAL DATA)%n", paquetes.size());
        System.out.printf("   Natural capacities → Connections when necessary%n");
        
        return paquetes;
    }
    
    private static boolean esDestinoPopular(String destino, String[] destinosPopulares) {
        for (String popular : destinosPopulares) {
            if (popular.equals(destino)) return true;
        }
        return false;
    }
    
    private static void ejecutarALNSConProgreso(ContextoProblema contexto) {
        // Configure ALNS for DEBUG - minimal parameters
        ALNSSolver solver = new ALNSSolver(10, 5000.0, 0.98); // Temperatura mucho más alta para aceptar soluciones peores
        
        solver.configurarProblema(
            new ArrayList<>(contexto.getTodosPaquetes()),
            new ArrayList<>(contexto.getTodosAeropuertos()),
            new ArrayList<>(contexto.getTodosVuelos()),
            contexto.getContinentes()
        );
        
        System.out.println("   DEBUG configuration: 10 iterations, 10 orders (30 sec)");
        System.out.println("   Identifying performance problem...");
        System.out.println("   Starting ALNS optimization...\n");
        
        // Execute with progress tracking
        long inicio = System.currentTimeMillis();
        Solucion resultado = solver.resolver();
        long fin = System.currentTimeMillis();
        
        // Show progress from history
        mostrarProgresoIteraciones(solver.getHistorialFitness());
        
        // Show final result WITH experimental fitness
        mostrarResultadoCompleto(resultado, contexto, fin - inicio);
    }
    
    private static void mostrarProgresoIteraciones(List<Double> historial) {
        System.out.println("OPTIMIZATION PROGRESS (Objective Function):");
        for (int i = 0; i < historial.size(); i++) {
            System.out.printf("Iteration %d: FObjective %.2f%n", i + 1, historial.get(i));
        }
        System.out.println();
    }
    
    private static void mostrarResultadoCompleto(Solucion resultado, ContextoProblema contexto, long tiempo) {
        System.out.println("FINAL ALNS RESULT:");
        System.out.printf("   Total time: %d ms%n", tiempo);
        System.out.printf("   Routed packages: %d/%d%n", 
                         resultado.getRutasPaquetes().size(), contexto.getTodosPaquetes().size());
        System.out.printf("   Feasible solution: %s%n", 
                         resultado.isEsFactible() ? "Yes" : "No");
        System.out.printf("   Objective Function: %.2f%n", resultado.getFuncionObjetivo());
        System.out.printf("   Violations: %d%n", 
                         resultado.getViolacionesRestricciones());
        
        // Calculate experimental fitness at the END of complete execution
        System.out.println();
        System.out.println("EXPERIMENTAL EVALUATION (For academic comparison):");
        try {
            double fitnessExperimental = FitnessExperimental.calcular(resultado, contexto);
            System.out.printf("   Experimental Fitness: %.4f%n", fitnessExperimental);
            System.out.println("   (This value is used to compare with other algorithms)");
            
            // Show detailed breakdown
            System.out.println();
            System.out.println("DETAILED BREAKDOWN:");
            String detalle = FitnessExperimental.calcularDetallado(resultado, contexto);
            System.out.println(detalle);
            
        } catch (Exception e) {
            System.out.println("   Error calculating experimental fitness: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    // AUXILIARY METHODS
    
    private static List<Paquete> crearPaquetesRespaldo() {
        List<Paquete> paquetes = new ArrayList<>();
        String[] destinos = {"EDDI", "SKBO", "VIDP", "SBBR", "EHAM"};
        
        for (int i = 0; i < 10; i++) {
            String paqueteId = "PKG_RESPALDO_" + (i + 1);
            String destino = destinos[i % destinos.length];
            String clienteId = "CLI_" + (1000 + i);
            
            // Package WITHOUT pre-assigned sede
            Paquete paquete = new Paquete(paqueteId, null, destino, clienteId);
            paquete.setPrioridad((i % 3) + 1);
            paquetes.add(paquete);
        }
        
        return paquetes;
    }
    
    /**
     * Class to contain loaded data
     */
    private static class DatosCompletos {
        final List<Aeropuerto> aeropuertos;
        final List<Vuelo> vuelos;
        final Set<Continente> continentes;
        
        DatosCompletos(List<Aeropuerto> aeropuertos, List<Vuelo> vuelos, Set<Continente> continentes) {
            this.aeropuertos = aeropuertos;
            this.vuelos = vuelos;
            this.continentes = continentes;
        }
    }
}
