package com.grupo5e.morapack.utils;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.core.enums.TipoVuelo;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad para cargar los datos reales de aeropuertos y vuelos desde los archivos proporcionados.
 * Parsea los archivos aeropuertosinfo.txt y vuelos.txt para crear las estructuras de datos necesarias.
 */
public class MoraPackDataLoader {
    
    private static final String AEROPUERTOS_FILE = "data/aeropuertosinfo.txt";
    private static final String VUELOS_FILE = "data/vuelos.txt";
    
    // Mapeo de continentes según los datos proporcionados
    private static final Map<String, String> CONTINENTE_MAPPING = Map.of(
        "America del Sur", "AM",
        "Europa", "EU",
        "Asia", "AS"
    );
    
    // Códigos IATA de las sedes de MoraPack
    private static final Set<String> SEDES_MORAPACK = Set.of("SPIM", "EBCI", "UBBB");
    
    /**
     * Carga todos los aeropuertos desde el archivo aeropuertosinfo.txt
     */
    public static List<Aeropuerto> cargarAeropuertos() {
        List<Aeropuerto> aeropuertos = new ArrayList<>();
        
        try {
            List<String> lineas = Files.readAllLines(Paths.get(AEROPUERTOS_FILE));
            String continenteActual = "";
            
            for (String linea : lineas) {
                linea = linea.trim();
                
                // Detectar líneas de continente
                if (linea.contains("America del Sur") || linea.contains("Europa") || linea.contains("Asia")) {
                    continenteActual = extraerContinente(linea);
                    continue;
                }
                
                // Parsear líneas de aeropuertos (empiezan con número)
                if (linea.matches("^\\d+\\s+.*")) {
                    Aeropuerto aeropuerto = parsearAeropuerto(linea, continenteActual);
                    if (aeropuerto != null) {
                        aeropuertos.add(aeropuerto);
                    }
                }
            }
            
            System.out.println("Cargados " + aeropuertos.size() + " aeropuertos desde " + AEROPUERTOS_FILE);
            
        } catch (IOException e) {
            System.err.println("Error al cargar aeropuertos: " + e.getMessage());
            // Crear datos de respaldo si no se puede cargar el archivo
            return crearAeropuertosRespaldo();
        }
        
        return aeropuertos;
    }
    
    /**
     * Carga todos los vuelos desde el archivo vuelos.txt
     */
    public static List<Vuelo> cargarVuelos(List<Aeropuerto> aeropuertos) {
        List<Vuelo> vuelos = new ArrayList<>();
        Map<String, String> aeropuertoContinente = crearMapaAeropuertoContinente(aeropuertos);
        
        try {
            List<String> lineas = Files.readAllLines(Paths.get(VUELOS_FILE));
            
            for (String linea : lineas) {
                linea = linea.trim();
                if (!linea.isEmpty()) {
                    Vuelo vuelo = parsearVuelo(linea, aeropuertoContinente);
                    if (vuelo != null) {
                        vuelos.add(vuelo);
                    }
                }
            }
            
            System.out.println("Cargados " + vuelos.size() + " vuelos desde " + VUELOS_FILE);
            
        } catch (IOException e) {
            System.err.println("Error al cargar vuelos: " + e.getMessage());
            // Crear datos de respaldo si no se puede cargar el archivo
            return crearVuelosRespaldo(aeropuertos);
        }
        
        return vuelos;
    }
    
    /**
     * Crea la estructura de continentes basada en los aeropuertos cargados
     */
    public static Set<Continente> crearContinentes(List<Aeropuerto> aeropuertos) {
        Map<String, Continente> continentesMap = new HashMap<>();
        
        // Inicializar continentes
        continentesMap.put("AM", new Continente("América", "AM", "SPIM")); // Lima
        continentesMap.put("EU", new Continente("Europa", "EU", "EBCI"));   // Bruselas
        continentesMap.put("AS", new Continente("Asia", "AS", "UBBB"));     // Baku
        
        // Agregar aeropuertos a sus continentes
        for (Aeropuerto aeropuerto : aeropuertos) {
            String codigoContinente = mapearContinente(aeropuerto.getContinente());
            Continente continente = continentesMap.get(codigoContinente);
            if (continente != null) {
                continente.agregarAeropuerto(aeropuerto.getCodigoIATA());
            }
        }
        
        return new HashSet<>(continentesMap.values());
    }
    
    private static String extraerContinente(String linea) {
        if (linea.contains("America del Sur")) return "America del Sur";
        if (linea.contains("Europa")) return "Europa";
        if (linea.contains("Asia")) return "Asia";
        return "Desconocido";
    }
    
    private static Aeropuerto parsearAeropuerto(String linea, String continente) {
        try {
            // Formato: 01   SKBO   Bogota              Colombia        bogo    -5     430     Latitude: 04° 42' 05" N   Longitude:  74° 08' 49" W
            String[] partes = linea.split("\\s+", 8);
            
            if (partes.length < 7) {
                return null;
            }
            
            String codigoIATA = partes[1];
            String ciudad = partes[2];
            String pais = partes[3];
            // Saltar partes[4] (código corto) y partes[5] (GMT)
            int capacidad = Integer.parseInt(partes[6]);
            
            // Extraer coordenadas si están disponibles
            double latitud = 0.0, longitud = 0.0;
            if (partes.length > 7) {
                String coordenadas = partes[7];
                latitud = extraerLatitud(coordenadas);
                longitud = extraerLongitud(coordenadas);
            }
            
            boolean esSede = SEDES_MORAPACK.contains(codigoIATA);
            
            return new Aeropuerto(codigoIATA, ciudad, pais, continente, 
                                latitud, longitud, capacidad, esSede);
                                
        } catch (Exception e) {
            System.err.println("Error al parsear aeropuerto: " + linea + " - " + e.getMessage());
            return null;
        }
    }
    
    private static double extraerLatitud(String coordenadas) {
        try {
            Pattern pattern = Pattern.compile("Latitude: (\\d+)° (\\d+)' (\\d+)\" ([NS])");
            Matcher matcher = pattern.matcher(coordenadas);
            if (matcher.find()) {
                int grados = Integer.parseInt(matcher.group(1));
                int minutos = Integer.parseInt(matcher.group(2));
                int segundos = Integer.parseInt(matcher.group(3));
                String direccion = matcher.group(4);
                
                double lat = grados + (minutos / 60.0) + (segundos / 3600.0);
                return "S".equals(direccion) ? -lat : lat;
            }
        } catch (Exception e) {
            // Ignorar errores de parsing de coordenadas
        }
        return 0.0;
    }
    
    private static double extraerLongitud(String coordenadas) {
        try {
            Pattern pattern = Pattern.compile("Longitude: (\\d+)° (\\d+)' (\\d+)\" ([EW])");
            Matcher matcher = pattern.matcher(coordenadas);
            if (matcher.find()) {
                int grados = Integer.parseInt(matcher.group(1));
                int minutos = Integer.parseInt(matcher.group(2));
                int segundos = Integer.parseInt(matcher.group(3));
                String direccion = matcher.group(4);
                
                double lon = grados + (minutos / 60.0) + (segundos / 3600.0);
                return "W".equals(direccion) ? -lon : lon;
            }
        } catch (Exception e) {
            // Ignorar errores de parsing de coordenadas
        }
        return 0.0;
    }
    
    private static Vuelo parsearVuelo(String linea, Map<String, String> aeropuertoContinente) {
        try {
            // Formato: SKBO-SEQM-03:34-05:21-0300
            String[] partes = linea.split("-");
            
            if (partes.length != 5) {
                return null;
            }
            
            String origen = partes[0];
            String destino = partes[1];
            LocalTime horaSalida = LocalTime.parse(partes[2], DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime horaLlegada = LocalTime.parse(partes[3], DateTimeFormatter.ofPattern("HH:mm"));
            int capacidad = Integer.parseInt(partes[4]);
            
            // Determinar si es mismo continente
            String continenteOrigen = aeropuertoContinente.get(origen);
            String continenteDestino = aeropuertoContinente.get(destino);
            boolean mismoContinente = continenteOrigen != null && continenteOrigen.equals(continenteDestino);
            
            // Generar número de vuelo único
            String numeroVuelo = String.format("MP%s%s_%02d%02d", 
                                             origen, destino, 
                                             horaSalida.getHour(), horaSalida.getMinute());
            
            Vuelo vuelo = new Vuelo(numeroVuelo, origen, destino, mismoContinente, capacidad);
            vuelo.setHoraSalida(horaSalida);
            vuelo.setHoraLlegada(horaLlegada);
            
            // Calcular duración real
            long duracionMinutos;
            if (horaLlegada.isBefore(horaSalida)) {
                // El vuelo cruza medianoche
                duracionMinutos = java.time.Duration.between(horaSalida, LocalTime.MAX).toMinutes() + 
                                 java.time.Duration.between(LocalTime.MIN, horaLlegada).toMinutes() + 1;
            } else {
                duracionMinutos = java.time.Duration.between(horaSalida, horaLlegada).toMinutes();
            }
            
            vuelo.setDuracionHoras(duracionMinutos / 60.0);
            
            return vuelo;
            
        } catch (Exception e) {
            System.err.println("Error al parsear vuelo: " + linea + " - " + e.getMessage());
            return null;
        }
    }
    
    private static Map<String, String> crearMapaAeropuertoContinente(List<Aeropuerto> aeropuertos) {
        Map<String, String> mapa = new HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            mapa.put(aeropuerto.getCodigoIATA(), mapearContinente(aeropuerto.getContinente()));
        }
        return mapa;
    }
    
    private static String mapearContinente(String continente) {
        return CONTINENTE_MAPPING.getOrDefault(continente, "AM");
    }
    
    /**
     * Crea datos de respaldo de aeropuertos si no se puede cargar el archivo
     */
    private static List<Aeropuerto> crearAeropuertosRespaldo() {
        List<Aeropuerto> aeropuertos = new ArrayList<>();
        
        // Sedes de MoraPack
        aeropuertos.add(new Aeropuerto("SPIM", "Lima", "Perú", "America del Sur", -12.0, -77.0, 440, true));
        aeropuertos.add(new Aeropuerto("EBCI", "Bruselas", "Bélgica", "Europa", 50.9, 4.5, 440, true));
        aeropuertos.add(new Aeropuerto("UBBB", "Baku", "Azerbaiyán", "Asia", 40.5, 50.0, 400, true));
        
        // Otros aeropuertos importantes
        aeropuertos.add(new Aeropuerto("SKBO", "Bogotá", "Colombia", "America del Sur", 4.7, -74.1, 430, false));
        aeropuertos.add(new Aeropuerto("SBBR", "Brasilia", "Brasil", "America del Sur", -15.8, -47.9, 480, false));
        aeropuertos.add(new Aeropuerto("EDDI", "Berlín", "Alemania", "Europa", 52.5, 13.4, 480, false));
        aeropuertos.add(new Aeropuerto("VIDP", "Delhi", "India", "Asia", 28.6, 77.1, 480, false));
        
        System.out.println("Usando " + aeropuertos.size() + " aeropuertos de respaldo");
        return aeropuertos;
    }
    
    /**
     * Crea datos de respaldo de vuelos si no se puede cargar el archivo
     */
    private static List<Vuelo> crearVuelosRespaldo(List<Aeropuerto> aeropuertos) {
        List<Vuelo> vuelos = new ArrayList<>();
        
        // Crear vuelos básicos entre sedes principales
        String[] sedes = {"SPIM", "EBCI", "UBBB"};
        for (int i = 0; i < sedes.length; i++) {
            for (int j = 0; j < sedes.length; j++) {
                if (i != j) {
                    String numeroVuelo = "MP" + sedes[i] + sedes[j];
                    vuelos.add(new Vuelo(numeroVuelo, sedes[i], sedes[j], false, 300));
                }
            }
        }
        
        System.out.println("Usando " + vuelos.size() + " vuelos de respaldo");
        return vuelos;
    }
    
    /**
     * Método de utilidad para imprimir estadísticas de los datos cargados
     */
    public static void imprimirEstadisticas(List<Aeropuerto> aeropuertos, List<Vuelo> vuelos, Set<Continente> continentes) {
        System.out.println("\n=== ESTADÍSTICAS DE DATOS CARGADOS ===");
        
        System.out.println("Aeropuertos por continente:");
        for (Continente continente : continentes) {
            System.out.println("  - " + continente.getNombre() + ": " + continente.getCantidadAeropuertos() + " aeropuertos");
        }
        
        System.out.println("\nSedes MoraPack:");
        aeropuertos.stream()
                .filter(Aeropuerto::isEsSedeMoraPack)
                .forEach(a -> System.out.println("  - " + a.getCodigoIATA() + " (" + a.getCiudad() + ", " + a.getPais() + ")"));
        
        long vuelosMismoContinente = vuelos.stream().mapToLong(v -> v.isMismoContinente() ? 1 : 0).sum();
        long vuelosDistintoContinente = vuelos.size() - vuelosMismoContinente;
        
        System.out.println("\nVuelos:");
        System.out.println("  - Total: " + vuelos.size());
        System.out.println("  - Mismo continente: " + vuelosMismoContinente);
        System.out.println("  - Distinto continente: " + vuelosDistintoContinente);
        
        int capacidadMinima = vuelos.stream().mapToInt(Vuelo::getCapacidadMaxima).min().orElse(0);
        int capacidadMaxima = vuelos.stream().mapToInt(Vuelo::getCapacidadMaxima).max().orElse(0);
        double capacidadPromedio = vuelos.stream().mapToInt(Vuelo::getCapacidadMaxima).average().orElse(0);
        
        System.out.println("  - Capacidad mínima: " + capacidadMinima + " paquetes");
        System.out.println("  - Capacidad máxima: " + capacidadMaxima + " paquetes");
        System.out.println("  - Capacidad promedio: " + String.format("%.1f", capacidadPromedio) + " paquetes");
    }
}
