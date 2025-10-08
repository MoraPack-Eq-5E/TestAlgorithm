package com.grupo5e.morapack.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lector de cancelaciones de vuelos desde archivo.
 * 
 * Formato esperado del archivo:
 * dd.ORIGEN-DESTINO-HH:MM
 * 
 * Donde:
 * - dd: Día de la cancelación (dos dígitos)
 * - ORIGEN: Código IATA del aeropuerto de origen
 * - DESTINO: Código IATA del aeropuerto de destino
 * - HH:MM: Hora de salida del vuelo
 * 
 * Ejemplo: 01.SKBO-SEQM-03:34
 * 
 * Patrón: Consistent con LectorVuelos y LectorAeropuerto
 */
public class LectorCancelaciones {
    
    private final String rutaArchivo;
    
    public LectorCancelaciones(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }
    
    /**
     * Lee el archivo de cancelaciones y retorna un mapa estructurado.
     * 
     * @return Map<identificadorVuelo, Set<diasCancelados>>
     *         donde identificadorVuelo tiene formato "ORIGEN-DESTINO-HH:MM"
     */
    public Map<String, Set<Integer>> leerCancelaciones() {
        Map<String, Set<Integer>> cancelaciones = new HashMap<>();
        int lineasLeidas = 0;
        int lineasValidas = 0;
        int lineasInvalidas = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            
            while ((linea = reader.readLine()) != null) {
                lineasLeidas++;
                
                // Saltar líneas vacías o comentarios
                if (linea.trim().isEmpty() || linea.trim().startsWith("#")) {
                    continue;
                }
                
                try {
                    // Parsear línea: dd.ORIGEN-DESTINO-HH:MM
                    String[] partes = linea.trim().split("\\.", 2);
                    
                    if (partes.length != 2) {
                        System.err.println("Formato inválido en línea " + lineasLeidas + ": " + linea);
                        System.err.println("  Esperado: dd.ORIGEN-DESTINO-HH:MM");
                        lineasInvalidas++;
                        continue;
                    }
                    
                    // Extraer día
                    int dia = Integer.parseInt(partes[0].trim());
                    
                    // Validar día razonable (1-365)
                    if (dia < 1 || dia > 365) {
                        System.err.println("Día fuera de rango en línea " + lineasLeidas + ": " + dia);
                        lineasInvalidas++;
                        continue;
                    }
                    
                    // Extraer identificador de vuelo
                    String identificadorVuelo = partes[1].trim(); // "ORIGEN-DESTINO-HH:MM"
                    
                    // Validar formato básico del identificador
                    if (!validarFormatoIdentificador(identificadorVuelo)) {
                        System.err.println("Identificador de vuelo inválido en línea " + lineasLeidas + ": " + identificadorVuelo);
                        System.err.println("  Esperado formato: ORIGEN-DESTINO-HH:MM");
                        lineasInvalidas++;
                        continue;
                    }
                    
                    // Registrar cancelación
                    cancelaciones
                        .computeIfAbsent(identificadorVuelo, k -> new HashSet<>())
                        .add(dia);
                    
                    lineasValidas++;
                    
                } catch (NumberFormatException e) {
                    System.err.println("Día inválido (no numérico) en línea " + lineasLeidas + ": " + linea);
                    lineasInvalidas++;
                } catch (Exception e) {
                    System.err.println("Error procesando línea " + lineasLeidas + ": " + linea);
                    System.err.println("  Error: " + e.getMessage());
                    lineasInvalidas++;
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error leyendo archivo de cancelaciones: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Reporte de lectura
        System.out.println("=== Lectura de Cancelaciones ===");
        System.out.println("Archivo: " + rutaArchivo);
        System.out.println("Líneas procesadas: " + lineasLeidas);
        System.out.println("Cancelaciones válidas: " + lineasValidas);
        System.out.println("Líneas inválidas: " + lineasInvalidas);
        System.out.println("Vuelos afectados: " + cancelaciones.size());
        
        return cancelaciones;
    }
    
    /**
     * Valida que el identificador de vuelo tenga el formato esperado.
     * Formato: CODIGO-CODIGO-HH:MM
     * 
     * @param identificador Identificador a validar
     * @return true si el formato es válido
     */
    private boolean validarFormatoIdentificador(String identificador) {
        if (identificador == null || identificador.isEmpty()) {
            return false;
        }
        
        // Debe tener al menos 2 guiones (ORIGEN-DESTINO-HH:MM)
        String[] partes = identificador.split("-");
        if (partes.length < 3) {
            return false;
        }
        
        // Validar que tenga formato de hora al final (HH:MM)
        String ultimaParte = partes[partes.length - 1];
        if (!ultimaParte.matches("\\d{2}:\\d{2}")) {
            return false;
        }
        
        // Validar que los códigos IATA no estén vacíos
        if (partes[0].trim().isEmpty() || partes[1].trim().isEmpty()) {
            return false;
        }
        
        return true;
    }
}
