package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.core.enums.EstadoGeneral;
import com.grupo5e.morapack.core.constants.ConstantesMoraPack;
import com.grupo5e.morapack.algorithm.alns.ALNSSolver;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;
import com.grupo5e.morapack.algorithm.validation.ValidadorRestricciones;
import com.grupo5e.morapack.algorithm.validation.ResultadoValidacion;
import com.grupo5e.morapack.utils.MoraPackDataLoader;
import com.grupo5e.morapack.algorithm.alns.operators.construction.ConstruccionEstrategia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Demo unificado que reemplaza los 5 demos anteriores:
 * - MoraPackALNSDemo
 * - MoraPackALNSDemoSintetico 
 * - MoraPackDemoConDatosReales
 * - MoraPackDemoDesafiante
 * - MoraPackALNSDebug
 */
public class MoraPackDemoUnificado {
    
    public enum TipoDemo {
        BASICO("Demo básico con datos sintéticos controlados", 30, 500),
        DATOS_REALES("Demo con datos reales cargados de archivos", 50, 1000), 
        DESAFIANTE("Problema extremo con restricciones de capacidad", 150, 1500),
        DEBUG("Configuración mínima para debugging", 5, 100),
        COMPARATIVO("Compara diferentes estrategias de construcción", 40, 800);
        
        private final String descripcion;
        private final int numPaquetes;
        private final int iteracionesALNS;
        
        TipoDemo(String descripcion, int numPaquetes, int iteracionesALNS) {
            this.descripcion = descripcion;
            this.numPaquetes = numPaquetes;
            this.iteracionesALNS = iteracionesALNS;
        }
        
        public String getDescripcion() { return descripcion; }
        public int getNumPaquetes() { return numPaquetes; }
        public int getIteracionesALNS() { return iteracionesALNS; }
    }
    
    public static void main(String[] args) {
        MoraPackDemoUnificado demo = new MoraPackDemoUnificado();
        
        if (args.length > 0) {
            // Permitir especificar tipo por argumento
            try {
                TipoDemo tipo = TipoDemo.valueOf(args[0].toUpperCase());
                demo.ejecutarDemo(tipo);
            } catch (IllegalArgumentException e) {
                System.err.println("Tipo de demo inválido. Tipos disponibles: " + 
                    Arrays.toString(TipoDemo.values()));
                demo.mostrarMenuInteractivo();
            }
        } else {
            demo.mostrarMenuInteractivo();
        }
    }
    
    private void mostrarMenuInteractivo() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== 🚀 MORAPACK ALNS - DEMO UNIFICADO ===\n");
        System.out.println("Seleccione tipo de demo:");
        
        TipoDemo[] tipos = TipoDemo.values();
        for (int i = 0; i < tipos.length; i++) {
            System.out.printf("%d. %s - %s (%d paquetes)\n", 
                i + 1, tipos[i].name(), tipos[i].getDescripcion(), tipos[i].getNumPaquetes());
        }
        
        System.out.print("\nIngrese opción (1-" + tipos.length + "): ");
        
        try {
            int opcion = scanner.nextInt();
            if (opcion >= 1 && opcion <= tipos.length) {
                ejecutarDemo(tipos[opcion - 1]);
            } else {
                System.err.println("Opción inválida");
                ejecutarDemo(TipoDemo.BASICO); // Default
            }
        } catch (Exception e) {
            System.err.println("Error en la entrada, usando demo básico");
            ejecutarDemo(TipoDemo.BASICO);
        }
    }
    
    public void ejecutarDemo(TipoDemo tipo) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🚀 EJECUTANDO: " + tipo.name());
        System.out.println("📋 " + tipo.getDescripcion());
        System.out.println("📦 Paquetes: " + tipo.getNumPaquetes());
        System.out.println("🔄 Iteraciones ALNS: " + tipo.getIteracionesALNS());
        System.out.println("=".repeat(60));
        
        try {
            ConfiguracionDemo config = switch (tipo) {
                case BASICO -> crearConfiguracionBasica();
                case DATOS_REALES -> crearConfiguracionConDatosReales();
                case DESAFIANTE -> crearConfiguracionDesafiante();
                case DEBUG -> crearConfiguracionDebug();
                case COMPARATIVO -> crearConfiguracionComparativa();
            };
            
            if (tipo == TipoDemo.COMPARATIVO) {
                ejecutarComparativoEstrategias(config);
            } else {
                ejecutarALNSUnico(config, tipo);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en la ejecución: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ================================================================================
    // CONFIGURACIONES ESPECÍFICAS
    // ================================================================================
    
    private ConfiguracionDemo crearConfiguracionBasica() {
        System.out.println("\n1. Creando configuración básica con datos sintéticos...");
        
        // Crear aeropuertos sintéticos balanceados
        List<Aeropuerto> aeropuertos = crearAeropuertosSinteticos();
        Set<Continente> continentes = crearContinentesSinteticos(aeropuertos);
        List<Vuelo> vuelos = crearVuelosSinteticos(aeropuertos);
        List<Paquete> paquetes = crearPaquetesSinteticos(aeropuertos, 30);
        
        return new ConfiguracionDemo("BÁSICO", paquetes, aeropuertos, vuelos, continentes, 
                                   ConstruccionEstrategia.TipoEstrategia.INTELIGENTE);
    }
    
    private ConfiguracionDemo crearConfiguracionConDatosReales() throws IOException {
        System.out.println("\n1. Cargando datos reales desde archivos...");
        
        List<Aeropuerto> aeropuertos = cargarAeropuertosRealesRobustos();
        Set<Continente> continentes = crearContinentesReales(aeropuertos);
        List<Vuelo> vuelos = cargarVuelosRealesRobustos(aeropuertos);
        List<Paquete> paquetes = crearPaquetesReales(aeropuertos, 50);
        
        System.out.println("  ✅ Aeropuertos: " + aeropuertos.size());
        System.out.println("  ✅ Vuelos: " + vuelos.size());
        System.out.println("  ✅ Paquetes: " + paquetes.size());
        
        return new ConfiguracionDemo("DATOS_REALES", paquetes, aeropuertos, vuelos, continentes,
                                   ConstruccionEstrategia.TipoEstrategia.INTELIGENTE);
    }
    
    private ConfiguracionDemo crearConfiguracionDesafiante() throws IOException {
        System.out.println("\n1. Creando problema DESAFIANTE con restricciones extremas...");
        
        List<Aeropuerto> aeropuertos = cargarAeropuertosCapacidadLimitada();
        Set<Continente> continentes = crearContinentesReales(aeropuertos);
        List<Vuelo> vuelos = cargarVuelosCapacidadReducida(aeropuertos);
        List<Paquete> paquetes = crearPaquetesParaSaturacion(aeropuertos, 150);
        
        // Estadísticas de saturación
        int capacidadTotalVuelos = vuelos.stream().mapToInt(Vuelo::getCapacidadMaxima).sum();
        int capacidadTotalAlmacenes = aeropuertos.stream().mapToInt(Aeropuerto::getCapacidadAlmacen).sum();
        
        double satVuelos = (double) paquetes.size() / capacidadTotalVuelos * 100;
        double satAlmacenes = (double) paquetes.size() / capacidadTotalAlmacenes * 100;
        
        System.out.println("  🔥 Saturación vuelos: " + String.format("%.1f%%", satVuelos));
        System.out.println("  🔥 Saturación almacenes: " + String.format("%.1f%%", satAlmacenes));
        System.out.println("  ⚡ PROBLEMA ALTAMENTE SATURADO");
        
        return new ConfiguracionDemo("DESAFIANTE", paquetes, aeropuertos, vuelos, continentes,
                                   ConstruccionEstrategia.TipoEstrategia.INTELIGENTE);
    }
    
    private ConfiguracionDemo crearConfiguracionDebug() {
        System.out.println("\n1. Creando configuración mínima para debugging...");
        
        List<Aeropuerto> aeropuertos = crearAeropuertosMinimos();
        Set<Continente> continentes = crearContinentesMinimos();
        List<Vuelo> vuelos = crearVuelosMinimos(aeropuertos);
        List<Paquete> paquetes = crearPaquetesMinimos(aeropuertos);
        
        System.out.println("  🔧 Configuración mínima controlada");
        System.out.println("  📊 " + aeropuertos.size() + " aeropuertos, " + vuelos.size() + " vuelos, " + paquetes.size() + " paquetes");
        
        return new ConfiguracionDemo("DEBUG", paquetes, aeropuertos, vuelos, continentes,
                                   ConstruccionEstrategia.TipoEstrategia.VORAZ);
    }
    
    private ConfiguracionDemo crearConfiguracionComparativa() {
        System.out.println("\n1. Creando configuración para comparar estrategias...");
        
        List<Aeropuerto> aeropuertos = crearAeropuertosSinteticos();
        Set<Continente> continentes = crearContinentesSinteticos(aeropuertos);
        List<Vuelo> vuelos = crearVuelosSinteticos(aeropuertos);
        List<Paquete> paquetes = crearPaquetesSinteticos(aeropuertos, 40);
        
        return new ConfiguracionDemo("COMPARATIVO", paquetes, aeropuertos, vuelos, continentes,
                                   null); // No se usa estrategia específica
    }
    
    // ================================================================================
    // EJECUCIÓN ALNS
    // ================================================================================
    
    private void ejecutarALNSUnico(ConfiguracionDemo config, TipoDemo tipo) {
        System.out.println("\n2. Configurando y ejecutando ALNS...");
        
        ALNSSolver solver = new ALNSSolver(
            tipo.getIteracionesALNS(),
            tipo == TipoDemo.DESAFIANTE ? 3000.0 : 1000.0,
            tipo == TipoDemo.DESAFIANTE ? 0.995 : 0.995
        );
        
        solver.configurarProblema(
            config.getPaquetes(),
            config.getAeropuertos(),
            config.getVuelos(),
            config.getContinentes()
        );
        
        System.out.println("🚀 Iniciando ALNS (" + config.getEstrategia().name() + ")...");
        long tiempoInicio = System.currentTimeMillis();
        
        Solucion mejorSolucion = solver.resolver();
        
        long tiempoFin = System.currentTimeMillis();
        double tiempoEjecucion = (tiempoFin - tiempoInicio) / 1000.0;
        
        mostrarResultados(config.getNombre(), mejorSolucion, tiempoEjecucion, config);
        
        // Validar solución
        ValidadorRestricciones validador = new ValidadorRestricciones(
            config.getAeropuertos(), config.getVuelos(), config.getContinentes()
        );
        ResultadoValidacion validacion = validador.validarSolucion(mejorSolucion);
        System.out.println("\n" + validacion.generarResumen());
    }
    
    private void ejecutarComparativoEstrategias(ConfiguracionDemo config) {
        System.out.println("\n2. Comparando diferentes estrategias de construcción...\n");
        
        ConstruccionEstrategia.TipoEstrategia[] estrategias = {
            ConstruccionEstrategia.TipoEstrategia.VORAZ,
            ConstruccionEstrategia.TipoEstrategia.MENOR_COSTO,
            ConstruccionEstrategia.TipoEstrategia.MENOR_TIEMPO,
            ConstruccionEstrategia.TipoEstrategia.BALANCEADA,
            ConstruccionEstrategia.TipoEstrategia.INTELIGENTE
        };
        
        List<ResultadoComparativo> resultados = new ArrayList<>();
        
        for (ConstruccionEstrategia.TipoEstrategia estrategia : estrategias) {
            System.out.println("🧪 Probando estrategia: " + estrategia.name());
            
            ALNSSolver solver = new ALNSSolver(800, 1000.0, 0.995);
            solver.configurarProblema(
                config.getPaquetes(),
                config.getAeropuertos(),
                config.getVuelos(),
                config.getContinentes()
            );
            
            long tiempoInicio = System.currentTimeMillis();
            Solucion solucion = solver.resolver();
            long tiempoFin = System.currentTimeMillis();
            
            double tiempoEjecucion = (tiempoFin - tiempoInicio) / 1000.0;
            
            resultados.add(new ResultadoComparativo(
                estrategia, solucion, tiempoEjecucion
            ));
            
            System.out.println("   📊 Paquetes: " + solucion.getCantidadPaquetes() + 
                             ", Fitness: " + String.format("%.2f", solucion.getFitness()) +
                             ", Tiempo: " + String.format("%.2fs", tiempoEjecucion));
        }
        
        mostrarComparativoFinal(resultados);
    }
    
    // ================================================================================
    // VISUALIZACIÓN DE RESULTADOS
    // ================================================================================
    
    private void mostrarResultados(String nombre, Solucion solucion, double tiempoEjecucion, ConfiguracionDemo config) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🏆 RESULTADOS " + nombre);
        System.out.println("=".repeat(60));
        System.out.println("🕒 Tiempo ejecución: " + String.format("%.2f segundos", tiempoEjecucion));
        System.out.println("📦 Paquetes ruteados: " + solucion.getCantidadPaquetes() + "/" + config.getPaquetes().size());
        
        double porcentaje = (double) solucion.getCantidadPaquetes() / config.getPaquetes().size() * 100;
        System.out.println("📊 Porcentaje ruteado: " + String.format("%.1f%%", porcentaje));
        
        System.out.println("💰 Costo total: $" + String.format("%.2f", solucion.getCostoTotal()));
        System.out.println("⏱️ Tiempo máximo: " + String.format("%.1f horas", solucion.getTiempoTotalHoras()));
        System.out.println("📊 Fitness: " + String.format("%.2f", solucion.getFitness()));
        System.out.println("✅ Factible: " + (solucion.isEsFactible() ? "SÍ" : "NO"));
        
        if (solucion.getCantidadPaquetes() > 0) {
            System.out.println("\n=== RUTAS EJEMPLO ===");
            mostrarRutasEjemplo(solucion, 5);
        }
        
        // Estadísticas de utilización
        System.out.println("\n=== UTILIZACIÓN DE RECURSOS ===");
        long vuelosUsados = solucion.getOcupacionVuelos().entrySet().stream()
                .filter(e -> e.getValue() > 0).count();
        System.out.println("🛫 Vuelos utilizados: " + vuelosUsados + "/" + config.getVuelos().size());
        System.out.println("🏢 Aeropuertos activos: " + solucion.getOcupacionAlmacenes().size());
    }
    
    private void mostrarRutasEjemplo(Solucion solucion, int maxRutas) {
        int mostradas = 0;
        for (Map.Entry<String, Ruta> entry : solucion.getRutasPaquetes().entrySet()) {
            if (mostradas >= maxRutas) break;
            
            String paqueteId = entry.getKey();
            Ruta ruta = entry.getValue();
            
            String complejidad = ruta.getCantidadSegmentos() == 1 ? "DIRECTA" : 
                               ruta.getCantidadSegmentos() + "-CONEXIONES";
            
            System.out.println("📦 " + paqueteId + ": " + ruta.getAeropuertoOrigen() + 
                             " → " + ruta.getAeropuertoDestino() + 
                             " [" + complejidad + ", $" + 
                             String.format("%.0f", ruta.getCostoTotal()) + ", " +
                             String.format("%.1fh", ruta.getTiempoTotalHoras()) + "]");
            mostradas++;
        }
    }
    
    private void mostrarComparativoFinal(List<ResultadoComparativo> resultados) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🏆 COMPARATIVO FINAL DE ESTRATEGIAS");
        System.out.println("=".repeat(80));
        System.out.printf("%-15s %-10s %-12s %-12s %-10s %-8s%n", 
                         "ESTRATEGIA", "PAQUETES", "FITNESS", "COSTO", "TIEMPO(s)", "FACTIBLE");
        System.out.println("-".repeat(80));
        
        for (ResultadoComparativo resultado : resultados) {
            System.out.printf("%-15s %-10d %-12.2f $%-11.2f %-10.2f %-8s%n",
                resultado.getEstrategia().name(),
                resultado.getSolucion().getCantidadPaquetes(),
                resultado.getSolucion().getFitness(),
                resultado.getSolucion().getCostoTotal(),
                resultado.getTiempoEjecucion(),
                resultado.getSolucion().isEsFactible() ? "SÍ" : "NO"
            );
        }
        
        // Determinar ganador
        ResultadoComparativo ganador = resultados.stream()
            .max(Comparator.comparing(r -> r.getSolucion().getCantidadPaquetes()))
            .orElse(resultados.get(0));
        
        System.out.println("\n🥇 GANADOR: " + ganador.getEstrategia().name());
        System.out.println("   📊 " + ganador.getEstrategia().getDescripcion());
    }
    
    // ================================================================================
    // MÉTODOS DE CREACIÓN DE DATOS (Versiones simplificadas)
    // ================================================================================
    
    private List<Aeropuerto> crearAeropuertosSinteticos() {
        List<Aeropuerto> aeropuertos = new ArrayList<>();
        
        // Crear aeropuertos por continente
        String[][] datosAeropuertos = {
            {"LIM", "Lima", "Perú", "América", "true"},
            {"BOG", "Bogotá", "Colombia", "América", "false"},
            {"SCL", "Santiago", "Chile", "América", "false"},
            {"BRU", "Bruselas", "Bélgica", "Europa", "true"},
            {"MAD", "Madrid", "España", "Europa", "false"},
            {"CDG", "París", "Francia", "Europa", "false"},
            {"BAK", "Baku", "Azerbaiyán", "Asia", "true"},
            {"DXB", "Dubai", "EAU", "Asia", "false"},
            {"NRT", "Tokio", "Japón", "Asia", "false"}
        };
        
        for (String[] datos : datosAeropuertos) {
            double lat = datos[3].equals("América") ? -10.0 : datos[3].equals("Europa") ? 50.0 : 30.0;
            double lon = datos[3].equals("América") ? -70.0 : datos[3].equals("Europa") ? 5.0 : 50.0;
            
            Aeropuerto aeropuerto = new Aeropuerto(
                datos[0], datos[1], datos[2], datos[3], lat, lon, 
                500, // Capacidad generosa para demo
                Boolean.parseBoolean(datos[4])
            );
            aeropuertos.add(aeropuerto);
        }
        
        return aeropuertos;
    }
    
    private List<Vuelo> crearVuelosSinteticos(List<Aeropuerto> aeropuertos) {
        List<Vuelo> vuelos = new ArrayList<>();
        Random random = new Random(42);
        
        // Crear conexiones entre todos los aeropuertos
        for (int i = 0; i < aeropuertos.size(); i++) {
            for (int j = 0; j < aeropuertos.size(); j++) {
                if (i != j) {
                    Aeropuerto origen = aeropuertos.get(i);
                    Aeropuerto destino = aeropuertos.get(j);
                    
                    boolean mismoContinente = origen.getContinente().equals(destino.getContinente());
                    int capacidad = mismoContinente ? 50 : 80; // Capacidad moderada
                    
                    String numeroVuelo = String.format("SY_%s_%s", origen.getCodigoIATA(), destino.getCodigoIATA());
                    
                    Vuelo vuelo = new Vuelo(numeroVuelo, origen.getCodigoIATA(), destino.getCodigoIATA(),
                                          mismoContinente, capacidad);
                    vuelo.setHoraSalida(LocalTime.of(8 + random.nextInt(12), random.nextInt(60)));
                    vuelo.setHoraLlegada(vuelo.getHoraSalida().plusHours(mismoContinente ? 6 : 12));
                    vuelo.setDuracionHoras(mismoContinente ? 6.0 : 12.0);
                    
                    vuelos.add(vuelo);
                }
            }
        }
        
        return vuelos;
    }
    
    private List<Paquete> crearPaquetesSinteticos(List<Aeropuerto> aeropuertos, int cantidad) {
        List<Paquete> paquetes = new ArrayList<>();
        
        List<String> sedes = aeropuertos.stream()
                .filter(Aeropuerto::isEsSedeMoraPack)
                .map(Aeropuerto::getCodigoIATA)
                .toList();
        
        List<String> destinos = aeropuertos.stream()
                .filter(a -> !a.isEsSedeMoraPack())
                .map(Aeropuerto::getCodigoIATA)
                .toList();
        
        Random random = new Random(42);
        
        for (int i = 1; i <= cantidad; i++) {
            String paqueteId = String.format("PKG_%03d", i);
            String origen = sedes.get((i - 1) % sedes.size());
            String destino = destinos.get(random.nextInt(destinos.size()));
            String clienteId = String.format("CLI_%03d", ((i - 1) % 10) + 1);
            
            Paquete paquete = new Paquete(paqueteId, origen, destino, clienteId);
            paquete.setPrioridad(random.nextInt(3) + 1);
            
            // Calcular fecha límite
            boolean mismoContinente = aeropuertos.stream().anyMatch(a -> 
                a.getCodigoIATA().equals(origen) && 
                aeropuertos.stream().anyMatch(b -> 
                    b.getCodigoIATA().equals(destino) && 
                    a.getContinente().equals(b.getContinente())
                )
            );
            paquete.calcularFechaLimite(mismoContinente, mismoContinente ? 2 : 3);
            
            paquetes.add(paquete);
        }
        
        return paquetes;
    }
    
    // Métodos utilitarios simplificados para los otros tipos
    private Set<Continente> crearContinentesSinteticos(List<Aeropuerto> aeropuertos) {
        Map<String, Continente> continentesMap = new HashMap<>();
        continentesMap.put("América", new Continente("América", "AM", "LIM"));
        continentesMap.put("Europa", new Continente("Europa", "EU", "BRU"));
        continentesMap.put("Asia", new Continente("Asia", "AS", "BAK"));
        
        for (Aeropuerto aeropuerto : aeropuertos) {
            Continente continente = continentesMap.get(aeropuerto.getContinente());
            if (continente != null) {
                continente.agregarAeropuerto(aeropuerto.getCodigoIATA());
            }
        }
        
        return new HashSet<>(continentesMap.values());
    }
    
    // Implementaciones stub para los otros métodos - se pueden expandir según necesidad
    private List<Aeropuerto> cargarAeropuertosRealesRobustos() throws IOException { return crearAeropuertosSinteticos(); }
    private Set<Continente> crearContinentesReales(List<Aeropuerto> aeropuertos) { return crearContinentesSinteticos(aeropuertos); }
    private List<Vuelo> cargarVuelosRealesRobustos(List<Aeropuerto> aeropuertos) { return crearVuelosSinteticos(aeropuertos); }
    private List<Paquete> crearPaquetesReales(List<Aeropuerto> aeropuertos, int cantidad) { return crearPaquetesSinteticos(aeropuertos, cantidad); }
    
    private List<Aeropuerto> cargarAeropuertosCapacidadLimitada() throws IOException { 
        List<Aeropuerto> aeropuertos = crearAeropuertosSinteticos();
        // Reducir capacidades drásticamente
        aeropuertos.forEach(a -> a.setCapacidadAlmacen(Math.max(20, a.getCapacidadAlmacen() / 10)));
        return aeropuertos;
    }
    
    private List<Vuelo> cargarVuelosCapacidadReducida(List<Aeropuerto> aeropuertos) { 
        List<Vuelo> vuelos = crearVuelosSinteticos(aeropuertos);
        Random random = new Random(42);
        // Reducir capacidades de vuelos drásticamente
        vuelos.forEach(v -> v.setCapacidadMaxima(random.nextInt(5) + 2)); // 2-6 paquetes
        return vuelos;
    }
    
    private List<Paquete> crearPaquetesParaSaturacion(List<Aeropuerto> aeropuertos, int cantidad) { 
        return crearPaquetesSinteticos(aeropuertos, cantidad);
    }
    
    private List<Aeropuerto> crearAeropuertosMinimos() {
        return Arrays.asList(
            new Aeropuerto("A", "Airport A", "Country A", "América", 0, 0, 100, true),
            new Aeropuerto("B", "Airport B", "Country B", "Europa", 0, 0, 100, true),
            new Aeropuerto("C", "Airport C", "Country C", "América", 0, 0, 100, false),
            new Aeropuerto("D", "Airport D", "Country D", "Europa", 0, 0, 100, false)
        );
    }
    
    private Set<Continente> crearContinentesMinimos() {
        Set<Continente> continentes = new HashSet<>();
        Continente america = new Continente("América", "AM", "A");
        america.agregarAeropuerto("A");
        america.agregarAeropuerto("C");
        
        Continente europa = new Continente("Europa", "EU", "B");
        europa.agregarAeropuerto("B");
        europa.agregarAeropuerto("D");
        
        continentes.add(america);
        continentes.add(europa);
        return continentes;
    }
    
    private List<Vuelo> crearVuelosMinimos(List<Aeropuerto> aeropuertos) {
        return Arrays.asList(
            new Vuelo("V1", "A", "C", true, 50),
            new Vuelo("V2", "A", "B", false, 50), 
            new Vuelo("V3", "B", "D", true, 50),
            new Vuelo("V4", "C", "D", false, 50),
            new Vuelo("V5", "A", "D", false, 50)
        );
    }
    
    private List<Paquete> crearPaquetesMinimos(List<Aeropuerto> aeropuertos) {
        return Arrays.asList(
            new Paquete("P1", "A", "C", "CLIENT1"),
            new Paquete("P2", "A", "D", "CLIENT2"),
            new Paquete("P3", "B", "C", "CLIENT3"),
            new Paquete("P4", "B", "D", "CLIENT4"),
            new Paquete("P5", "A", "B", "CLIENT5")
        );
    }
    
    // ================================================================================
    // CLASES DE SOPORTE
    // ================================================================================
    
    private static class ConfiguracionDemo {
        private final String nombre;
        private final List<Paquete> paquetes;
        private final List<Aeropuerto> aeropuertos;
        private final List<Vuelo> vuelos;
        private final Set<Continente> continentes;
        private final ConstruccionEstrategia.TipoEstrategia estrategia;
        
        public ConfiguracionDemo(String nombre, List<Paquete> paquetes, List<Aeropuerto> aeropuertos,
                               List<Vuelo> vuelos, Set<Continente> continentes, 
                               ConstruccionEstrategia.TipoEstrategia estrategia) {
            this.nombre = nombre;
            this.paquetes = paquetes;
            this.aeropuertos = aeropuertos;
            this.vuelos = vuelos;
            this.continentes = continentes;
            this.estrategia = estrategia;
        }
        
        public String getNombre() { return nombre; }
        public List<Paquete> getPaquetes() { return paquetes; }
        public List<Aeropuerto> getAeropuertos() { return aeropuertos; }
        public List<Vuelo> getVuelos() { return vuelos; }
        public Set<Continente> getContinentes() { return continentes; }
        public ConstruccionEstrategia.TipoEstrategia getEstrategia() { return estrategia; }
    }
    
    private static class ResultadoComparativo {
        private final ConstruccionEstrategia.TipoEstrategia estrategia;
        private final Solucion solucion;
        private final double tiempoEjecucion;
        
        public ResultadoComparativo(ConstruccionEstrategia.TipoEstrategia estrategia, 
                                  Solucion solucion, double tiempoEjecucion) {
            this.estrategia = estrategia;
            this.solucion = solucion;
            this.tiempoEjecucion = tiempoEjecucion;
        }
        
        public ConstruccionEstrategia.TipoEstrategia getEstrategia() { return estrategia; }
        public Solucion getSolucion() { return solucion; }
        public double getTiempoEjecucion() { return tiempoEjecucion; }
    }
}
