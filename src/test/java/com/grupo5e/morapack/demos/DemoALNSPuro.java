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
 * Demo de ALNS puro - sin SedeSelector.
 * El algoritmo decide dinámicamente desde qué sede MoraPack enviar cada paquete.
 * Muestra progreso por iteración y mejor resultado final.
 */
public class DemoALNSPuro {
    
    private static final String PEDIDOS_CSV_FILE = "data/pedidos_generados.csv";
    
    public static void main(String[] args) {
        System.out.println("🚀 === DEMO: ALNS PURO MULTI-DEPOT ===");
        System.out.println("El algoritmo decide dinámicamente desde qué sede enviar cada paquete\n");
        
        try {
            // 1. CARGAR DATOS
            System.out.println("📂 1. Cargando datos...");
            DatosCompletos datos = cargarDatos();
            
            // 2. CREAR PAQUETES SIN SEDE PRE-ASIGNADA
            System.out.println("\n📦 2. Creando paquetes para ALNS puro...");
            List<Paquete> paquetesSinSede = crearPaquetesSinSede(datos);
            
            // 3. CREAR CONTEXTO PARA ALNS
            System.out.println("\n🧠 3. Preparando contexto ALNS...");
            ContextoProblema contexto = new ContextoProblema(
                paquetesSinSede, datos.aeropuertos, datos.vuelos, datos.continentes
            );
            
            // 4. EJECUTAR ALNS CON PROGRESO
            System.out.println("\n⚡ 4. EJECUTANDO ALNS PURO");
            ejecutarALNSConProgreso(contexto);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static DatosCompletos cargarDatos() {
        List<Aeropuerto> aeropuertos = MoraPackDataLoader.cargarAeropuertos();
        List<Vuelo> vuelos = MoraPackDataLoader.cargarVuelos(aeropuertos);
        Set<Continente> continentes = MoraPackDataLoader.crearContinentes(aeropuertos);
        
        // USAR CAPACIDADES ORIGINALES (no reducidas)
        System.out.printf("   ✅ %d aeropuertos, %d vuelos, %d continentes%n", 
                         aeropuertos.size(), vuelos.size(), continentes.size());
        System.out.println("   🎯 Capacidades ORIGINALES (comportamiento natural)");
        
        return new DatosCompletos(aeropuertos, vuelos, continentes);
    }
    
    /**
     * Reduce las capacidades de vuelos solo para esta demo, para forzar conexiones naturales
     */
    private static List<Vuelo> reducirCapacidadesParaDemo(List<Vuelo> vuelosOriginales) {
        List<Vuelo> vuelosReducidos = new ArrayList<>();
        
        for (Vuelo vueloOriginal : vuelosOriginales) {
            // Crear copia con capacidad reducida
            int capacidadOriginal = vueloOriginal.getCapacidadMaxima();
            int capacidadReducida = Math.min(capacidadOriginal, 120); // Máx 120 paquetes
            
            Vuelo vueloReducido = new Vuelo(
                vueloOriginal.getNumeroVuelo(),
                vueloOriginal.getAeropuertoOrigen(),
                vueloOriginal.getAeropuertoDestino(),
                vueloOriginal.isMismoContinente(),
                capacidadReducida // ← CAPACIDAD REDUCIDA
            );
            
            // Copiar otros atributos
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
            
            // USAR DATA ORIGINAL SIN SATURACIÓN ARTIFICIAL - LIMITADO PARA DEBUG
            int totalPedidos = Math.min(10, lineas.size() - 1); // ← REDUCIDO PARA DEBUG
            System.out.printf("   📦 DATA ORIGINAL DEBUG: %d pedidos%n", totalPedidos);
            
            for (int i = 1; i <= totalPedidos; i++) {
                String linea = lineas.get(i).trim();
                if (linea.isEmpty()) continue;
                
                String[] partes = linea.split(",");
                if (partes.length != 6) continue;
                
                String destino = partes[3].trim();
                String clienteId = partes[5].trim();
                int cantidad = Integer.parseInt(partes[4].trim());
                
                // USAR CANTIDAD ORIGINAL SIN MULTIPLICADORES
                for (int j = 0; j < cantidad; j++) {
                    String paqueteId = String.format("PKG_%s_%03d_%02d", clienteId, cantidad, j + 1);
                    
                    // ⚡ CLAVE: Paquete SIN aeropuertoOrigen fijo - ALNS decidirá
                    Paquete paquete = new Paquete(paqueteId, null, destino, clienteId);
                    paquete.setPrioridad((j % 3) + 1);
                    paquete.setFechaLimiteEntrega(LocalDateTime.now().plusDays(3));
                    paquetes.add(paquete);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error leyendo pedidos: " + e.getMessage());
            paquetes.addAll(crearPaquetesRespaldo());
        }
        
        System.out.printf("   ✅ %d paquetes creados (DATA ORIGINAL)%n", paquetes.size());
        System.out.printf("   🎯 Capacidades naturales → Conexiones cuando sea necesario%n");
        
        return paquetes;
    }
    
    private static boolean esDestinoPopular(String destino, String[] destinosPopulares) {
        for (String popular : destinosPopulares) {
            if (popular.equals(destino)) return true;
        }
        return false;
    }
    
    private static void ejecutarALNSConProgreso(ContextoProblema contexto) {
        // Configurar ALNS para DEBUG - parámetros mínimos
        ALNSSolver solver = new ALNSSolver(10, 1000.0, 0.95); // 3 iteraciones para debug
        
        solver.configurarProblema(
            new ArrayList<>(contexto.getTodosPaquetes()),
            new ArrayList<>(contexto.getTodosAeropuertos()),
            new ArrayList<>(contexto.getTodosVuelos()),
            contexto.getContinentes()
        );
        
        System.out.println("   🎯 Configuración DEBUG: 3 iteraciones, 10 pedidos (30 seg)");
        System.out.println("   💡 Identificando problema de rendimiento...");
        System.out.println("   🔄 Iniciando optimización ALNS...\n");
        
        // Ejecutar con seguimiento de progreso
        long inicio = System.currentTimeMillis();
        Solucion resultado = solver.resolver();
        long fin = System.currentTimeMillis();
        
        // Mostrar progreso del historial
        mostrarProgresoIteraciones(solver.getHistorialFitness());
        
        // Mostrar resultado final CON fitness experimental
        mostrarResultadoCompleto(resultado, contexto, fin - inicio);
    }
    
    private static void mostrarProgresoIteraciones(List<Double> historial) {
        System.out.println("📈 PROGRESO OPTIMIZACIÓN (Función Objetivo):");
        for (int i = 0; i < historial.size(); i++) {
            System.out.printf("Iteracion %d: FObjetivo %.2f%n", i + 1, historial.get(i));
        }
        System.out.println();
    }
    
    private static void mostrarResultadoCompleto(Solucion resultado, ContextoProblema contexto, long tiempo) {
        System.out.println("🏆 RESULTADO FINAL ALNS:");
        System.out.println("   ┌─────────────────────────────────────────────────────────────┐");
        System.out.printf("   │ ⏱️  Tiempo total: %d ms                                 │%n", tiempo);
        System.out.printf("   │ 📦 Paquetes ruteados: %d/%d                              │%n", 
                         resultado.getRutasPaquetes().size(), contexto.getTodosPaquetes().size());
        System.out.printf("   │ ✅ Solución factible: %-3s                              │%n", 
                         resultado.isEsFactible() ? "Sí" : "No");
        System.out.printf("   │ 🎯 Función Objetivo: %.2f                              │%n", resultado.getFuncionObjetivo());
        System.out.printf("   │ ⚠️  Violaciones: %d                                       │%n", 
                         resultado.getViolacionesRestricciones());
        System.out.println("   └─────────────────────────────────────────────────────────────┘");
        
        // AQUÍ se calcula el fitness experimental AL FINAL de la ejecución completa
        System.out.println();
        System.out.println("🔬 EVALUACIÓN EXPERIMENTAL (Para comparación académica):");
        try {
            double fitnessExperimental = FitnessExperimental.calcular(resultado, contexto);
            System.out.printf("   📊 Fitness Experimental: %.4f%n", fitnessExperimental);
            System.out.println("   ℹ️  (Este valor se usa para comparar con otros algoritmos)");
            
            // Mostrar desglose detallado
            System.out.println();
            System.out.println("📈 DESGLOSE DETALLADO:");
            String detalle = FitnessExperimental.calcularDetallado(resultado, contexto);
            System.out.println(detalle);
            
        } catch (Exception e) {
            System.out.println("   ❌ Error calculando fitness experimental: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    private static List<Paquete> crearPaquetesRespaldo() {
        List<Paquete> paquetes = new ArrayList<>();
        String[] destinos = {"EDDI", "SKBO", "VIDP", "SBBR", "EHAM"};
        
        for (int i = 0; i < 10; i++) {
            String paqueteId = "PKG_RESPALDO_" + (i + 1);
            String destino = destinos[i % destinos.length];
            String clienteId = "CLI_" + (1000 + i);
            
            // Paquete SIN sede pre-asignada
            Paquete paquete = new Paquete(paqueteId, null, destino, clienteId);
            paquete.setPrioridad((i % 3) + 1);
            paquetes.add(paquete);
        }
        
        return paquetes;
    }
    
    /**
     * Clase para contener datos cargados
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
