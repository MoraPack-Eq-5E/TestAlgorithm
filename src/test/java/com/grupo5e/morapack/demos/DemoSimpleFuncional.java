package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.core.enums.EstadoGeneral;
import com.grupo5e.morapack.algorithm.alns.ALNSSolver;

import java.time.LocalTime;
import java.util.*;

/**
 * Demo SIMPLE y FUNCIONAL para probar la nueva estructura reorganizada.
 * Este demo debe compilar y ejecutar sin errores para validar la reorganización.
 */
public class DemoSimpleFuncional {
    
    public static void main(String[] args) {
        System.out.println("=== 🧪 DEMO SIMPLE - NUEVA ESTRUCTURA ===\n");
        
        DemoSimpleFuncional demo = new DemoSimpleFuncional();
        demo.ejecutarDemoBasico();
    }
    
    public void ejecutarDemoBasico() {
        try {
            System.out.println("1. Creando problema básico con nueva estructura...");
            ConfiguracionBasica config = crearProblemaBasico();
            
            System.out.println("2. Configurando ALNS con operadores reorganizados...");
            ALNSSolver solver = configurarALNS(config);
            
            System.out.println("3. Ejecutando optimización...");
            long inicio = System.currentTimeMillis();
            Solucion mejor = solver.resolver();
            long fin = System.currentTimeMillis();
            
            System.out.println("4. Mostrando resultados...");
            mostrarResultados(mejor, (fin - inicio) / 1000.0, config);
            
            System.out.println("✅ REORGANIZACIÓN EXITOSA - TODO FUNCIONA!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private ConfiguracionBasica crearProblemaBasico() {
        // Crear aeropuertos con nueva estructura
        List<Aeropuerto> aeropuertos = Arrays.asList(
            new Aeropuerto("LIM", "Lima", "Perú", "América", -12.0, -77.0, 100, true),
            new Aeropuerto("BRU", "Bruselas", "Bélgica", "Europa", 50.0, 4.0, 100, true),
            new Aeropuerto("BOG", "Bogotá", "Colombia", "América", 4.0, -74.0, 80, false),
            new Aeropuerto("MAD", "Madrid", "España", "Europa", 40.0, -3.0, 80, false)
        );
        
        // Crear continentes usando nueva estructura
        Set<Continente> continentes = new HashSet<>();
        Continente america = new Continente("América", "AM", "LIM");
        america.agregarAeropuerto("LIM");
        america.agregarAeropuerto("BOG");
        
        Continente europa = new Continente("Europa", "EU", "BRU");
        europa.agregarAeropuerto("BRU");
        europa.agregarAeropuerto("MAD");
        
        continentes.add(america);
        continentes.add(europa);
        
        // Crear vuelos usando TipoVuelo del nuevo enum
        List<Vuelo> vuelos = Arrays.asList(
            new Vuelo("V1", "LIM", "BOG", true, 30),   // Mismo continente
            new Vuelo("V2", "LIM", "BRU", false, 25),  // Distinto continente
            new Vuelo("V3", "BRU", "MAD", true, 35),   // Mismo continente
            new Vuelo("V4", "BOG", "MAD", false, 20),  // Distinto continente
            new Vuelo("V5", "LIM", "MAD", false, 22)   // Distinto continente
        );
        
        // Configurar horarios
        for (Vuelo vuelo : vuelos) {
            vuelo.setHoraSalida(LocalTime.of(8, 0));
            vuelo.setHoraLlegada(LocalTime.of(vuelo.isMismoContinente() ? 20 : 8, 0));
            vuelo.setDuracionHoras(vuelo.isMismoContinente() ? 12.0 : 24.0);
        }
        
        // Crear paquetes usando nuevo EstadoGeneral
        List<Paquete> paquetes = new ArrayList<>();
        String[] origenes = {"LIM", "BRU"};
        String[] destinos = {"BOG", "MAD"};
        
        for (int i = 1; i <= 10; i++) {
            String paqueteId = String.format("PKG_%03d", i);
            String origen = origenes[(i - 1) % origenes.length];
            String destino = destinos[(i - 1) % destinos.length];
            
            Paquete paquete = new Paquete(paqueteId, origen, destino, "CLI_001");
            paquete.setPrioridad(1);
            
            // Usar EstadoGeneral en lugar de EstadoPaquete
            paquete.setEstado(EstadoGeneral.CREADO);
            
            paquetes.add(paquete);
        }
        
        System.out.println("  ✅ " + aeropuertos.size() + " aeropuertos creados");
        System.out.println("  ✅ " + vuelos.size() + " vuelos configurados");
        System.out.println("  ✅ " + paquetes.size() + " paquetes generados");
        
        return new ConfiguracionBasica(paquetes, aeropuertos, vuelos, continentes);
    }
    
    private ALNSSolver configurarALNS(ConfiguracionBasica config) {
        // Crear solver con nueva estructura
        ALNSSolver solver = new ALNSSolver(200, 500.0, 0.995);
        
        // Configurar problema
        solver.configurarProblema(
            config.getPaquetes(),
            config.getAeropuertos(), 
            config.getVuelos(),
            config.getContinentes()
        );
        
        return solver;
    }
    
    private void mostrarResultados(Solucion solucion, double tiempoEjecucion, ConfiguracionBasica config) {
        System.out.println("\n=== RESULTADOS CON NUEVA ESTRUCTURA ===");
        System.out.println("🕒 Tiempo: " + String.format("%.2f segundos", tiempoEjecucion));
        System.out.println("📦 Paquetes: " + solucion.getCantidadPaquetes() + "/" + config.getPaquetes().size());
        System.out.println("💰 Costo: $" + String.format("%.2f", solucion.getCostoTotal()));
        System.out.println("📊 Fitness: " + String.format("%.2f", solucion.getFitness()));
        System.out.println("✅ Factible: " + solucion.isEsFactible());
        
        if (solucion.getCantidadPaquetes() > 0) {
            System.out.println("\n=== RUTAS GENERADAS ===");
            int count = 0;
            for (Map.Entry<String, Ruta> entry : solucion.getRutasPaquetes().entrySet()) {
                if (count >= 3) break;
                System.out.println("📦 " + entry.getKey() + ": " + 
                    entry.getValue().getAeropuertoOrigen() + " → " + 
                    entry.getValue().getAeropuertoDestino());
                count++;
            }
        }
    }
    
    // Clase de configuración simple
    private static class ConfiguracionBasica {
        private final List<Paquete> paquetes;
        private final List<Aeropuerto> aeropuertos;
        private final List<Vuelo> vuelos;
        private final Set<Continente> continentes;
        
        public ConfiguracionBasica(List<Paquete> paquetes, List<Aeropuerto> aeropuertos,
                                 List<Vuelo> vuelos, Set<Continente> continentes) {
            this.paquetes = paquetes;
            this.aeropuertos = aeropuertos;
            this.vuelos = vuelos;
            this.continentes = continentes;
        }
        
        public List<Paquete> getPaquetes() { return paquetes; }
        public List<Aeropuerto> getAeropuertos() { return aeropuertos; }
        public List<Vuelo> getVuelos() { return vuelos; }
        public Set<Continente> getContinentes() { return continentes; }
    }
}
