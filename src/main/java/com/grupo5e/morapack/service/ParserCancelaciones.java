package com.grupo5e.morapack.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Parser para archivos de cancelaciones programadas
 * Formato: dd.id-vuelo
 * 
 * Donde:
 * - dd: día del mes (01-31)
 * - id-vuelo: número del vuelo a cancelar
 */
public class ParserCancelaciones {
    
    private static final String FORMATO_ESPERADO = "dd.id-vuelo";
    
    /**
     * Parsea un archivo de cancelaciones programadas
     * @param rutaArchivo Ruta al archivo de cancelaciones
     * @return Lista de cancelaciones programadas
     */
    public static List<CancelacionProgramada> parsearArchivo(String rutaArchivo) {
        List<CancelacionProgramada> cancelaciones = new ArrayList<>();
        int numeroLinea = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            
            System.out.println("📁 Parseando archivo de cancelaciones: " + rutaArchivo);
            System.out.println("📋 Formato esperado: " + FORMATO_ESPERADO);
            System.out.println("=" .repeat(60));
            
            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                
                // Saltar líneas vacías y comentarios
                if (linea.trim().isEmpty() || linea.trim().startsWith("#")) {
                    continue;
                }
                
                try {
                    CancelacionProgramada cancelacion = parsearLinea(linea, numeroLinea);
                    cancelaciones.add(cancelacion);
                    
                    System.out.println("Línea " + numeroLinea + ": " + cancelacion.toString());
                    
                } catch (IllegalArgumentException e) {
                    System.err.println("Error en línea " + numeroLinea + ": " + e.getMessage());
                    System.err.println("   Línea: " + linea);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
            return new ArrayList<>();
        }
        
        System.out.println("=" .repeat(60));
        System.out.println("Resumen del parsing:");
        System.out.println("   Total de líneas procesadas: " + numeroLinea);
        System.out.println("   Cancelaciones válidas parseadas: " + cancelaciones.size());
        System.out.println("   Errores encontrados: " + (numeroLinea - cancelaciones.size()));
        
        return cancelaciones;
    }
    
    /**
     * Parsea una línea individual del archivo
     */
    private static CancelacionProgramada parsearLinea(String linea, int numeroLinea) {
        if (!validarFormato(linea)) {
            throw new IllegalArgumentException("Formato inválido: " + linea);
        }
        
        // Buscar el punto que separa día del número de vuelo
        int puntoIndex = linea.indexOf('.');
        if (puntoIndex == -1) {
            throw new IllegalArgumentException("No se encontró el separador '.' en: " + linea);
        }
        
        String diaStr = linea.substring(0, puntoIndex);
        String numeroVuelo = linea.substring(puntoIndex + 1);
        
        // Validar día
        int dia;
        try {
            dia = Integer.parseInt(diaStr);
            if (dia < 1 || dia > 31) {
                throw new IllegalArgumentException("Día inválido: " + dia + " (debe estar entre 1 y 31)");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Día no es un número válido: " + diaStr);
        }
        
        // Validar número de vuelo
        if (numeroVuelo.trim().isEmpty()) {
            throw new IllegalArgumentException("Número de vuelo no puede estar vacío");
        }
        
        // Crear motivo basado en el día
        String motivo = "Cancelación programada para el día " + dia;
        
        return new CancelacionProgramada(dia, numeroVuelo.trim(), motivo, numeroLinea);
    }
    
    /**
     * Valida el formato de una línea
     */
    public static boolean validarFormato(String linea) {
        if (linea == null || linea.trim().isEmpty()) {
            return false;
        }
        
        // Debe contener exactamente un punto
        int puntoCount = 0;
        for (char c : linea.toCharArray()) {
            if (c == '.') {
                puntoCount++;
            }
        }
        
        if (puntoCount != 1) {
            return false;
        }
        
        // El punto no puede estar al inicio o al final
        if (linea.startsWith(".") || linea.endsWith(".")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Valida un archivo completo antes de parsearlo
     */
    public static ResultadoValidacion validarArchivo(String rutaArchivo) {
        ResultadoValidacion resultado = new ResultadoValidacion();
        int numeroLinea = 0;
        int lineasValidas = 0;
        int lineasConError = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            
            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                
                // Saltar líneas vacías y comentarios
                if (linea.trim().isEmpty() || linea.trim().startsWith("#")) {
                    continue;
                }
                
                if (validarFormato(linea)) {
                    lineasValidas++;
                } else {
                    lineasConError++;
                    resultado.agregarError("Línea " + numeroLinea + ": Formato inválido - " + linea);
                }
            }
            
        } catch (IOException e) {
            resultado.agregarError("Error al leer el archivo: " + e.getMessage());
            return resultado;
        }
        
        resultado.setLineasProcesadas(numeroLinea);
        resultado.setLineasValidas(lineasValidas);
        resultado.setLineasConError(lineasConError);
        resultado.setValido(lineasConError == 0);
        
        return resultado;
    }
    
    /**
     * Genera un reporte detallado de las cancelaciones parseadas
     */
    public static void generarReporte(List<CancelacionProgramada> cancelaciones) {
        if (cancelaciones.isEmpty()) {
            System.out.println("📋 No hay cancelaciones para reportar");
            return;
        }
        
        System.out.println("\nREPORTE DETALLADO DE CANCELACIONES");
        System.out.println("=" .repeat(80));
        
        // Estadísticas generales
        System.out.println("Estadísticas Generales:");
        System.out.println("   Total de cancelaciones: " + cancelaciones.size());
        
        // Agrupar por día
        Map<Integer, List<CancelacionProgramada>> cancelacionesPorDia = new HashMap<>();
        for (CancelacionProgramada cancelacion : cancelaciones) {
            cancelacionesPorDia.computeIfAbsent(cancelacion.getDia(), k -> new ArrayList<>()).add(cancelacion);
        }
        
        System.out.println("   Días con cancelaciones: " + cancelacionesPorDia.size());
        
        // Agrupar por vuelo
        Map<String, List<CancelacionProgramada>> cancelacionesPorVuelo = new HashMap<>();
        for (CancelacionProgramada cancelacion : cancelaciones) {
            cancelacionesPorVuelo.computeIfAbsent(cancelacion.getNumeroVuelo(), k -> new ArrayList<>()).add(cancelacion);
        }
        
        System.out.println("   Vuelos únicos a cancelar: " + cancelacionesPorVuelo.size());
        
        // Top 5 días con más cancelaciones
        System.out.println("\n📅 Top 5 Días con Más Cancelaciones:");
        cancelacionesPorDia.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
            .limit(5)
            .forEach(entry -> {
                System.out.println("   Día " + entry.getKey() + ": " + entry.getValue().size() + " cancelaciones");
            });
        
        // Vuelos más afectados
        System.out.println("\n🛫 Vuelos Más Afectados:");
        cancelacionesPorVuelo.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
            .limit(5)
            .forEach(entry -> {
                System.out.println("   " + entry.getKey() + ": " + entry.getValue().size() + " cancelaciones");
            });
    }
    
    /**
     * Clase para cancelaciones programadas
     */
    public static class CancelacionProgramada {
        private final int dia;
        private final String numeroVuelo;
        private final String motivo;
        private final int numeroLinea;
        
        public CancelacionProgramada(int dia, String numeroVuelo, String motivo, int numeroLinea) {
            this.dia = dia;
            this.numeroVuelo = numeroVuelo;
            this.motivo = motivo;
            this.numeroLinea = numeroLinea;
        }
        
        // Getters
        public int getDia() { return dia; }
        public String getNumeroVuelo() { return numeroVuelo; }
        public String getMotivo() { return motivo; }
        public int getNumeroLinea() { return numeroLinea; }
        
        @Override
        public String toString() {
            return String.format("Cancelación[Día %d: %s - %s]", dia, numeroVuelo, motivo);
        }
    }
    
    /**
     * Clase para resultados de validación
     */
    public static class ResultadoValidacion {
        private boolean valido;
        private int lineasProcesadas;
        private int lineasValidas;
        private int lineasConError;
        private List<String> errores;
        
        public ResultadoValidacion() {
            this.errores = new ArrayList<>();
            this.valido = true;
        }
        
        public void agregarError(String error) {
            this.errores.add(error);
            this.valido = false;
        }
        
        // Getters y setters
        public boolean isValido() { return valido; }
        public void setValido(boolean valido) { this.valido = valido; }
        
        public int getLineasProcesadas() { return lineasProcesadas; }
        public void setLineasProcesadas(int lineasProcesadas) { this.lineasProcesadas = lineasProcesadas; }
        
        public int getLineasValidas() { return lineasValidas; }
        public void setLineasValidas(int lineasValidas) { this.lineasValidas = lineasValidas; }
        
        public int getLineasConError() { return lineasConError; }
        public void setLineasConError(int lineasConError) { this.lineasConError = lineasConError; }
        
        public List<String> getErrores() { return errores; }
        
        @Override
        public String toString() {
            return String.format("Validación: %s | Líneas: %d | Válidas: %d | Errores: %d", 
                               valido ? "VÁLIDO" : "INVÁLIDO", 
                               lineasProcesadas, lineasValidas, lineasConError);
        }
    }
    
    /**
     * Método principal para testing
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("ParserCancelaciones - Uso:");
            System.out.println("   java ParserCancelaciones <ruta_archivo>");
            System.out.println("   java ParserCancelaciones validar <ruta_archivo>");
            System.out.println("\n📋 Formato esperado: " + FORMATO_ESPERADO);
            System.out.println("   Ejemplo: 01.LA1234");
            return;
        }
        
        String comando = args[0];
        String rutaArchivo = args.length > 1 ? args[1] : "data/cancelaciones_ejemplo.txt";
        
        if ("validar".equals(comando)) {
            // Solo validar el archivo
            System.out.println("Validando archivo: " + rutaArchivo);
            ResultadoValidacion resultado = validarArchivo(rutaArchivo);
            System.out.println(resultado);
            
            if (!resultado.isValido()) {
                System.out.println("\nErrores encontrados:");
                for (String error : resultado.getErrores()) {
                    System.out.println("   " + error);
                }
            }
            
        } else {
            // Parsear el archivo completo
            List<CancelacionProgramada> cancelaciones = parsearArchivo(rutaArchivo);
            
            if (!cancelaciones.isEmpty()) {
                generarReporte(cancelaciones);
            }
        }
    }
}
