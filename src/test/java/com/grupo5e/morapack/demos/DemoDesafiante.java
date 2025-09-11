package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.core.enums.*;
import com.grupo5e.morapack.algorithm.alns.*;
import com.grupo5e.morapack.algorithm.alns.operators.construction.*;
import com.grupo5e.morapack.algorithm.validation.*;

import java.time.LocalTime;
import java.util.*;

/**
 * Demo DESAFIANTE - versión corregida con nueva estructura.
 * Problema altamente saturado para forzar optimización real.
 */
public class DemoDesafiante {
    
    public static void main(String[] args) {
        System.out.println("=== 🔥 DEMO DESAFIANTE - NUEVA ESTRUCTURA ===\n");
        
        DemoDesafiante demo = new DemoDesafiante();
        demo.ejecutarDemoDesafiante();
    }
    
    public void ejecutarDemoDesafiante() {
        try {
            System.out.println("1. Creando problema EXTREMADAMENTE desafiante...");
            ConfiguracionDesafiante config = crearProblemaExtremo();
            
            System.out.println("\n2. Analizando saturación del sistema...");
            analizarSaturacion(config);
            
            System.out.println("\n3. Ejecutando ALNS intensivo...");
            ejecutarALNSIntenso(config);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private ConfiguracionDesafiante crearProblemaExtremo() {
        // Aeropuertos con capacidades MUY limitadas
        List<Aeropuerto> aeropuertos = Arrays.asList(
            new Aeropuerto("LIM", "Lima", "Perú", "América", -12.0, -77.0, 25, true),      // Capacidad muy baja
            new Aeropuerto("BRU", "Bruselas", "Bélgica", "Europa", 50.0, 4.0, 30, true),  // Capacidad muy baja
            new Aeropuerto("BOG", "Bogotá", "Colombia", "América", 4.0, -74.0, 15, false), // Capacidad muy baja
            new Aeropuerto("MAD", "Madrid", "España", "Europa", 40.0, -3.0, 20, false),   // Capacidad muy baja
            new Aeropuerto("SCL", "Santiago", "Chile", "América", -33.0, -70.0, 15, false),
            new Aeropuerto("CDG", "París", "Francia", "Europa", 49.0, 2.0, 18, false),
            new Aeropuerto("DXB", "Dubai", "EAU", "Asia", 25.0, 55.0, 22, false)
        );
        
        // Continentes
        Set<Continente> continentes = new HashSet<>();
        Continente america = new Continente("América", "AM", "LIM");
        america.agregarAeropuerto("LIM");
        america.agregarAeropuerto("BOG");
        america.agregarAeropuerto("SCL");
        
        Continente europa = new Continente("Europa", "EU", "BRU");
        europa.agregarAeropuerto("BRU");
        europa.agregarAeropuerto("MAD");
        europa.agregarAeropuerto("CDG");
        
        Continente asia = new Continente("Asia", "AS", "DXB");
        asia.agregarAeropuerto("DXB");
        
        continentes.add(america);
        continentes.add(europa);
        continentes.add(asia);
        
        // Vuelos con capacidades MUY limitadas
        List<Vuelo> vuelos = crearVuelosLimitados(aeropuertos);
        
        // MUCHOS paquetes para saturar el sistema
        List<Paquete> paquetes = crearPaquetesParaSaturacion(aeropuertos, 80); // 80 paquetes vs capacidades minúsculas
        
        System.out.println("  🔥 Configuración extrema creada:");
        System.out.println("    - Aeropuertos: " + aeropuertos.size() + " (capacidad almacén: 15-30)");
        System.out.println("    - Vuelos: " + vuelos.size() + " (capacidad vuelo: 2-8)");
        System.out.println("    - Paquetes: " + paquetes.size() + " (saturación garantizada)");
        
        return new ConfiguracionDesafiante(paquetes, aeropuertos, vuelos, continentes);
    }
    
    private List<Vuelo> crearVuelosLimitados(List<Aeropuerto> aeropuertos) {
        List<Vuelo> vuelos = new ArrayList<>();
        Random random = new Random(42);
        
        // Crear conexiones limitadas entre aeropuertos
        for (int i = 0; i < aeropuertos.size(); i++) {
            for (int j = 0; j < aeropuertos.size(); j++) {
                if (i != j) {
                    Aeropuerto origen = aeropuertos.get(i);
                    Aeropuerto destino = aeropuertos.get(j);
                    
                    boolean mismoContinente = origen.getContinente().equals(destino.getContinente());
                    int capacidad = random.nextInt(5) + 2; // Entre 2-6 paquetes (MUY limitado)
                    
                    String numeroVuelo = String.format("EX_%s_%s", origen.getCodigoIATA(), destino.getCodigoIATA());
                    Vuelo vuelo = new Vuelo(numeroVuelo, origen.getCodigoIATA(), destino.getCodigoIATA(),
                                          mismoContinente, capacidad);
                    
                    // Horarios variables
                    int horaSalida = 6 + random.nextInt(12); // 6-18 horas
                    vuelo.setHoraSalida(LocalTime.of(horaSalida, 0));
                    vuelo.setHoraLlegada(LocalTime.of((horaSalida + (mismoContinente ? 6 : 12)) % 24, 0));
                    vuelo.setDuracionHoras(mismoContinente ? 6.0 : 12.0);
                    
                    vuelos.add(vuelo);
                }
            }
        }
        
        return vuelos;
    }
    
    private List<Paquete> crearPaquetesParaSaturacion(List<Aeropuerto> aeropuertos, int cantidad) {
        List<Paquete> paquetes = new ArrayList<>();
        
        List<String> sedes = aeropuertos.stream()
                .filter(Aeropuerto::isEsSedeMoraPack)
                .map(Aeropuerto::getCodigoIATA)
                .toList();
        
        List<String> destinos = aeropuertos.stream()
                .filter(a -> !a.isEsSedeMoraPack())
                .map(Aeropuerto::getCodigoIATA)
                .toList();
        
        if (sedes.isEmpty() || destinos.isEmpty()) {
            return Arrays.asList(new Paquete("PKG_001", "LIM", "BOG", "CLI_001"));
        }
        
        Random random = new Random(42);
        
        for (int i = 1; i <= cantidad; i++) {
            String paqueteId = String.format("PKG_%03d", i);
            
            // 70% desde la primera sede (crear saturación)
            String origen = random.nextDouble() < 0.7 ? sedes.get(0) : sedes.get(random.nextInt(sedes.size()));
            String destino = destinos.get(random.nextInt(destinos.size()));
            
            Paquete paquete = new Paquete(paqueteId, origen, destino, "CLI_" + String.format("%03d", ((i-1) % 15) + 1));
            paquete.setEstado(EstadoGeneral.CREADO);
            paquete.setPrioridad(random.nextInt(3) + 1);
            
            paquetes.add(paquete);
        }
        
        return paquetes;
    }
    
    private void analizarSaturacion(ConfiguracionDesafiante config) {
        int capacidadTotalVuelos = config.getVuelos().stream()
                .mapToInt(Vuelo::getCapacidadMaxima)
                .sum();
        
        int capacidadTotalAlmacenes = config.getAeropuertos().stream()
                .mapToInt(Aeropuerto::getCapacidadAlmacen)
                .sum();
        
        double satVuelos = (double) config.getPaquetes().size() / capacidadTotalVuelos * 100;
        double satAlmacenes = (double) config.getPaquetes().size() / capacidadTotalAlmacenes * 100;
        
        System.out.println("  📊 Saturación vuelos: " + String.format("%.1f%%", satVuelos));
        System.out.println("  📊 Saturación almacenes: " + String.format("%.1f%%", satAlmacenes));
        
        if (satVuelos > 50 || satAlmacenes > 30) {
            System.out.println("  ⚡ PROBLEMA ALTAMENTE SATURADO - Optimización forzada");
            System.out.println("  🎯 Se espera: paquetes no ruteados, fitness variable, rutas complejas");
        }
    }
    
    private void ejecutarALNSIntenso(ConfiguracionDesafiante config) {
        // ALNS con parámetros intensivos
        ALNSSolver solver = new ALNSSolver(1000, 2000.0, 0.998); // 1000 iteraciones, alta temperatura
        solver.configurarProblema(
            config.getPaquetes(), config.getAeropuertos(),
            config.getVuelos(), config.getContinentes()
        );
        
        System.out.println("🚀 Iniciando ALNS INTENSIVO...");
        long inicio = System.currentTimeMillis();
        Solucion mejorSolucion = solver.resolver();
        long fin = System.currentTimeMillis();
        
        double tiempo = (fin - inicio) / 1000.0;
        
        System.out.println("\n=== 🔥 RESULTADOS DESAFIANTES ===");
        System.out.println("🕒 Tiempo optimización: " + String.format("%.2f segundos", tiempo));
        System.out.println("📦 Paquetes ruteados: " + mejorSolucion.getCantidadPaquetes() + "/" + config.getPaquetes().size());
        
        double porcentaje = (double) mejorSolucion.getCantidadPaquetes() / config.getPaquetes().size() * 100;
        System.out.println("📊 Porcentaje ruteado: " + String.format("%.1f%%", porcentaje));
        
        if (porcentaje < 100) {
            System.out.println("⚠️ SATURACIÓN DETECTADA - No todos los paquetes pudieron ser ruteados");
        }
        
        System.out.println("💰 Costo total: $" + String.format("%.2f", mejorSolucion.getCostoTotal()));
        System.out.println("📊 Fitness: " + String.format("%.2f", mejorSolucion.getFitness()));
        System.out.println("✅ Factible: " + mejorSolucion.isEsFactible());
        
        // Estadísticas de utilización
        long vuelosUsados = mejorSolucion.getOcupacionVuelos().entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .count();
        System.out.println("🛫 Vuelos utilizados: " + vuelosUsados + "/" + config.getVuelos().size());
        
        // Mostrar rutas complejas
        if (mejorSolucion.getCantidadPaquetes() > 0) {
            System.out.println("\n=== RUTAS OPTIMIZADAS ===");
            int mostradas = 0;
            for (Map.Entry<String, Ruta> entry : mejorSolucion.getRutasPaquetes().entrySet()) {
                if (mostradas >= 5) break;
                
                Ruta ruta = entry.getValue();
                String complejidad = ruta.getCantidadSegmentos() == 1 ? "DIRECTA" : 
                                   ruta.getCantidadSegmentos() + "-CONEXIONES";
                
                System.out.println("📦 " + entry.getKey() + ": " + 
                    ruta.getAeropuertoOrigen() + " → " + ruta.getAeropuertoDestino() +
                    " [" + complejidad + ", $" + String.format("%.0f", ruta.getCostoTotal()) + "]");
                mostradas++;
            }
        }
        
        System.out.println("\n🎯 DESAFÍO COMPLETADO - ALNS bajo presión extrema!");
    }
    
    // Clase de configuración
    private static class ConfiguracionDesafiante {
        private final List<Paquete> paquetes;
        private final List<Aeropuerto> aeropuertos;
        private final List<Vuelo> vuelos;
        private final Set<Continente> continentes;
        
        public ConfiguracionDesafiante(List<Paquete> paquetes, List<Aeropuerto> aeropuertos,
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
