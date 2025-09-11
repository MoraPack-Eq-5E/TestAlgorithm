package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.core.enums.*;
import com.grupo5e.morapack.algorithm.alns.*;
import com.grupo5e.morapack.algorithm.alns.operators.construction.*;
import com.grupo5e.morapack.algorithm.validation.*;
import com.grupo5e.morapack.utils.MoraPackDataLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Demo FUNCIONAL con datos reales - versión corregida con nueva estructura.
 */
public class DemoConDatosReales {
    
    public static void main(String[] args) {
        System.out.println("=== 🔥 DEMO CON RESTRICCIONES REALISTAS DEL CASO DE ESTUDIO 🔥 ===\n");
        
        DemoConDatosReales demo = new DemoConDatosReales();
        demo.ejecutarDemoReal();
    }
    
    public void ejecutarDemoReal() {
        try {
            System.out.println("1. Cargando datos reales con parser robusto...");
            ConfiguracionReal config = cargarDatosReales();
            
            System.out.println("\n2. Validando conectividad...");
            validarConectividad(config);
            
            System.out.println("\n3. Ejecutando ALNS optimizado...");
            ejecutarALNS(config);
            
        } catch (IOException e) {
            System.err.println("❌ Error cargando archivos: " + e.getMessage());
            System.out.println("\n🔄 Ejecutando con datos sintéticos como fallback...");
            ejecutarFallback();
        } catch (Exception e) {
            System.err.println("❌ Error general: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private ConfiguracionReal cargarDatosReales() throws IOException {
        System.out.println("  📁 Cargando desde data/aeropuertosinfo.txt y data/vuelos.txt...");
        
        // Cargar aeropuertos con parser robusto
        List<Aeropuerto> aeropuertos = cargarAeropuertosRobustos();
        System.out.println("  ✅ Aeropuertos: " + aeropuertos.size());
        
        // Crear continentes
        Set<Continente> continentes = crearContinentes(aeropuertos);
        System.out.println("  ✅ Continentes: " + continentes.size());
        
        // Cargar vuelos con filtros
        List<Vuelo> vuelos = cargarVuelosRobustos(aeropuertos);
        System.out.println("  ✅ Vuelos: " + vuelos.size());
        
        // Crear paquetes de prueba con restricciones realistas
        List<Paquete> paquetes = crearPaquetesReales(aeropuertos, 150);
        System.out.println("  ✅ Paquetes: " + paquetes.size());
        
        return new ConfiguracionReal(paquetes, aeropuertos, vuelos, continentes);
    }
    
    private List<Aeropuerto> cargarAeropuertosRobustos() throws IOException {
        List<Aeropuerto> aeropuertos = new ArrayList<>();
        
        if (!Files.exists(Paths.get("data/aeropuertosinfo.txt"))) {
            System.out.println("  ⚠️ Archivo de aeropuertos no encontrado, usando datos sintéticos");
            return crearAeropuertosSinteticos();
        }
        
        List<String> lineas = Files.readAllLines(Paths.get("data/aeropuertosinfo.txt"));
        String continenteActual = "América";
        
        for (String linea : lineas) {
            linea = linea.trim();
            
            if (linea.contains("America del Sur")) {
                continenteActual = "América";
            } else if (linea.contains("Europa")) {
                continenteActual = "Europa";
            } else if (linea.contains("Asia")) {
                continenteActual = "Asia";
            }
            
            if (linea.matches("^\\d+\\s+\\w+\\s+.*")) {
                try {
                    String[] partes = linea.split("\\s+");
                    if (partes.length >= 7) {
                        String codigo = partes[1];
                        String ciudad = partes[2];
                        String pais = partes[3];
                        int capacidad = Integer.parseInt(partes[6]);
                        
                        double lat = continenteActual.equals("América") ? -10.0 : 
                                    continenteActual.equals("Europa") ? 50.0 : 30.0;
                        double lon = continenteActual.equals("América") ? -70.0 : 
                                    continenteActual.equals("Europa") ? 5.0 : 50.0;
                        
                        boolean esSede = codigo.equals("SPIM") || codigo.equals("EBCI") || codigo.equals("UBBB");
                        
                        Aeropuerto aeropuerto = new Aeropuerto(codigo, ciudad, pais, continenteActual,
                                                             lat, lon, capacidad, esSede);
                        aeropuertos.add(aeropuerto);
                        
                        if (esSede) {
                            System.out.println("    🏢 Sede: " + codigo + " (" + ciudad + ")");
                        }
                    }
                } catch (Exception e) {
                    // Ignorar líneas problemáticas
                }
            }
        }
        
        if (aeropuertos.isEmpty()) {
            System.out.println("  ⚠️ No se pudieron cargar aeropuertos, usando datos sintéticos");
            return crearAeropuertosSinteticos();
        }
        
        return aeropuertos;
    }
    
    private List<Aeropuerto> crearAeropuertosSinteticos() {
        return Arrays.asList(
            new Aeropuerto("SPIM", "Lima", "Perú", "América", -12.0, -77.0, 440, true),
            new Aeropuerto("EBCI", "Bruselas", "Bélgica", "Europa", 50.0, 4.0, 450, true),
            new Aeropuerto("UBBB", "Baku", "Azerbaiyán", "Asia", 40.0, 49.0, 420, true),
            new Aeropuerto("SKBO", "Bogotá", "Colombia", "América", 4.0, -74.0, 430, false),
            new Aeropuerto("SCEL", "Santiago", "Chile", "América", -33.0, -70.0, 460, false),
            new Aeropuerto("EGLL", "Londres", "Reino Unido", "Europa", 51.0, 0.0, 480, false),
            new Aeropuerto("VIDP", "Delhi", "India", "Asia", 28.0, 77.0, 470, false)
        );
    }
    
    private List<Vuelo> cargarVuelosRobustos(List<Aeropuerto> aeropuertos) throws IOException {
        List<Vuelo> vuelos = new ArrayList<>();
        
        if (!Files.exists(Paths.get("data/vuelos.txt"))) {
            System.out.println("  ⚠️ Archivo de vuelos no encontrado, creando vuelos sintéticos");
            return crearVuelosSinteticos(aeropuertos);
        }
        
        List<String> lineas = Files.readAllLines(Paths.get("data/vuelos.txt"));
        Map<String, String> aeropuertoContinente = aeropuertos.stream()
                .collect(HashMap::new, (m, a) -> m.put(a.getCodigoIATA(), a.getContinente()), HashMap::putAll);
        
        for (String linea : lineas) {
            if (linea.trim().isEmpty()) continue;
            
            try {
                String[] partes = linea.split("-");
                if (partes.length == 5) {
                    String origen = partes[0];
                    String destino = partes[1];
                    
                    if (!aeropuertoContinente.containsKey(origen) || !aeropuertoContinente.containsKey(destino)) {
                        continue; // Saltar vuelos con aeropuertos no cargados
                    }
                    
                    LocalTime horaSalida = LocalTime.parse(partes[2], DateTimeFormatter.ofPattern("HH:mm"));
                    LocalTime horaLlegada = LocalTime.parse(partes[3], DateTimeFormatter.ofPattern("HH:mm"));
                    int capacidad = Math.min(Integer.parseInt(partes[4]), 50); // Limitar capacidad
                    
                    boolean mismoContinente = aeropuertoContinente.get(origen).equals(aeropuertoContinente.get(destino));
                    
                    String numeroVuelo = String.format("FL_%s_%s_%02d%02d", 
                        origen, destino, horaSalida.getHour(), horaSalida.getMinute());
                    
                    Vuelo vuelo = new Vuelo(numeroVuelo, origen, destino, mismoContinente, capacidad);
                    vuelo.setHoraSalida(horaSalida);
                    vuelo.setHoraLlegada(horaLlegada);
                    
                    // Calcular duración
                    long duracionMinutos = horaLlegada.isBefore(horaSalida) ?
                        1440 - java.time.Duration.between(horaLlegada, horaSalida).toMinutes() :
                        java.time.Duration.between(horaSalida, horaLlegada).toMinutes();
                    
                    vuelo.setDuracionHoras(duracionMinutos / 60.0);
                    vuelos.add(vuelo);
                    
                    if (vuelos.size() >= 200) break; // Limitar para performance
                }
            } catch (Exception e) {
                // Ignorar líneas problemáticas
            }
        }
        
        if (vuelos.isEmpty()) {
            return crearVuelosSinteticos(aeropuertos);
        }
        
        return vuelos;
    }
    
    private List<Vuelo> crearVuelosSinteticos(List<Aeropuerto> aeropuertos) {
        List<Vuelo> vuelos = new ArrayList<>();
        Random random = new Random(42);
        
        for (int i = 0; i < aeropuertos.size(); i++) {
            for (int j = 0; j < aeropuertos.size(); j++) {
                if (i != j) {
                    Aeropuerto origen = aeropuertos.get(i);
                    Aeropuerto destino = aeropuertos.get(j);
                    
                    boolean mismoContinente = origen.getContinente().equals(destino.getContinente());
                    // Capacidades realistas según caso de estudio: 200-300 (mismo continente), 250-400 (distinto continente)
                    int capacidad = mismoContinente ? 
                        random.nextInt(101) + 200 : // 200-300 paquetes
                        random.nextInt(151) + 250;  // 250-400 paquetes
                    
                    String numeroVuelo = String.format("SY_%s_%s", origen.getCodigoIATA(), destino.getCodigoIATA());
                    Vuelo vuelo = new Vuelo(numeroVuelo, origen.getCodigoIATA(), destino.getCodigoIATA(),
                                          mismoContinente, capacidad);
                    
                    vuelo.setHoraSalida(LocalTime.of(8 + random.nextInt(8), 0));
                    vuelo.setHoraLlegada(vuelo.getHoraSalida().plusHours(mismoContinente ? 6 : 12));
                    vuelo.setDuracionHoras(mismoContinente ? 6.0 : 12.0);
                    
                    vuelos.add(vuelo);
                }
            }
        }
        
        return vuelos;
    }
    
    private Set<Continente> crearContinentes(List<Aeropuerto> aeropuertos) {
        Map<String, Continente> continentesMap = new HashMap<>();
        continentesMap.put("América", new Continente("América", "AM", "SPIM"));
        continentesMap.put("Europa", new Continente("Europa", "EU", "EBCI"));
        continentesMap.put("Asia", new Continente("Asia", "AS", "UBBB"));
        
        for (Aeropuerto aeropuerto : aeropuertos) {
            Continente continente = continentesMap.get(aeropuerto.getContinente());
            if (continente != null) {
                continente.agregarAeropuerto(aeropuerto.getCodigoIATA());
            }
        }
        
        return new HashSet<>(continentesMap.values());
    }
    
    private List<Paquete> crearPaquetesReales(List<Aeropuerto> aeropuertos, int cantidad) {
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
            System.out.println("  ⚠️ No hay suficientes aeropuertos, usando configuración mínima");
            return crearPaquetesMinimos();
        }
        
        Random random = new Random(42);
        for (int i = 1; i <= cantidad; i++) {
            String paqueteId = String.format("PKG_%03d", i);
            String origen = sedes.get((i - 1) % sedes.size());
            String destino = destinos.get(random.nextInt(destinos.size()));
            
            Paquete paquete = new Paquete(paqueteId, origen, destino, "CLI_" + String.format("%03d", ((i-1) % 10) + 1));
            paquete.setEstado(EstadoGeneral.CREADO);
            paquete.setPrioridad(random.nextInt(3) + 1);
            
            paquetes.add(paquete);
        }
        
        return paquetes;
    }
    
    private List<Paquete> crearPaquetesMinimos() {
        return Arrays.asList(
            new Paquete("PKG_001", "SPIM", "SKBO", "CLI_001"),
            new Paquete("PKG_002", "EBCI", "EGLL", "CLI_002"),
            new Paquete("PKG_003", "UBBB", "VIDP", "CLI_003")
        );
    }
    
    private void validarConectividad(ConfiguracionReal config) {
        ContextoProblema contexto = new ContextoProblema(
            config.getPaquetes(), config.getAeropuertos(),
            config.getVuelos(), config.getContinentes()
        );
        
        System.out.println("  📊 Aeropuertos cargados: " + config.getAeropuertos().size());
        System.out.println("  📊 Vuelos cargados: " + config.getVuelos().size());
        
        // Verificar sedes
        long sedes = config.getAeropuertos().stream()
                .filter(Aeropuerto::isEsSedeMoraPack)
                .count();
        System.out.println("  🏢 Sedes MoraPack: " + sedes);
        
        // Verificar conectividad de paquetes
        int paquetesConRuta = 0;
        for (Paquete paquete : config.getPaquetes()) {
            List<Vuelo> vuelosDirectos = contexto.getVuelosDirectos(
                paquete.getAeropuertoOrigen(), paquete.getAeropuertoDestino()
            );
            
            if (!vuelosDirectos.isEmpty()) {
                paquetesConRuta++;
            }
        }
        
        System.out.println("  🛫 Paquetes con vuelo directo: " + paquetesConRuta + "/" + config.getPaquetes().size());
        
        if (paquetesConRuta == 0) {
            System.out.println("  ⚠️ Sin conectividad directa, pero ALNS puede encontrar rutas con conexiones");
        }
    }
    
    private void ejecutarALNS(ConfiguracionReal config) {
        // Más iteraciones para problema más complejo con restricciones realistas
        ALNSSolver solver = new ALNSSolver(1000, 1000.0, 0.995);
        solver.configurarProblema(
            config.getPaquetes(), config.getAeropuertos(),
            config.getVuelos(), config.getContinentes()
        );
        
        System.out.println("🚀 Iniciando ALNS con datos reales...");
        long inicio = System.currentTimeMillis();
        Solucion mejorSolucion = solver.resolver();
        long fin = System.currentTimeMillis();
        
        double tiempo = (fin - inicio) / 1000.0;
        
        System.out.println("\n=== RESULTADOS CON RESTRICCIONES REALISTAS ===");
        System.out.println("🕒 Tiempo: " + String.format("%.2f segundos", tiempo));
        System.out.println("📦 Paquetes ruteados: " + mejorSolucion.getCantidadPaquetes() + "/" + config.getPaquetes().size());
        System.out.println("💰 Costo: $" + String.format("%.2f", mejorSolucion.getCostoTotal()));
        System.out.println("📊 Fitness: " + String.format("%.2f", mejorSolucion.getFitness()));
        System.out.println("✅ Factible: " + mejorSolucion.isEsFactible());
        System.out.println("🎯 Capacidades realistas: 200-300 (mismo continente), 250-400 (distinto continente)");
        
        // Validar solución
        ValidadorRestricciones validador = new ValidadorRestricciones(
            config.getAeropuertos(), config.getVuelos(), config.getContinentes()
        );
        ResultadoValidacion validacion = validador.validarSolucion(mejorSolucion);
        System.out.println("🔍 Violaciones: " + validacion.getTotalViolaciones());
        
        if (mejorSolucion.getCantidadPaquetes() > 0) {
            System.out.println("\n=== RUTAS EJEMPLO ===");
            int count = 0;
            for (Map.Entry<String, Ruta> entry : mejorSolucion.getRutasPaquetes().entrySet()) {
                if (count >= 3) break;
                Ruta ruta = entry.getValue();
                System.out.println("📦 " + entry.getKey() + ": " + 
                    ruta.getAeropuertoOrigen() + " → " + ruta.getAeropuertoDestino() +
                    " (" + ruta.getCantidadSegmentos() + " segmentos)");
                count++;
            }
        }
    }
    
    private void ejecutarFallback() {
        System.out.println("Ejecutando demo básico funcional...");
        DemoSimpleFuncional fallback = new DemoSimpleFuncional();
        fallback.ejecutarDemoBasico();
    }
    
    // Clase de configuración
    private static class ConfiguracionReal {
        private final List<Paquete> paquetes;
        private final List<Aeropuerto> aeropuertos;
        private final List<Vuelo> vuelos;
        private final Set<Continente> continentes;
        
        public ConfiguracionReal(List<Paquete> paquetes, List<Aeropuerto> aeropuertos,
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
