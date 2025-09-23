package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Pedido;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Parser para archivos de pedidos mensuales
 * Formato: dd-hh-mm-dest-###-IdClien
 * 
 * Donde:
 * - dd: día (01-31)
 * - hh: hora (00-23) 
 * - mm: minutos (00-59)
 * - dest: código aeropuerto destino (ej: SVMI, SBBR)
 * - ###: cantidad de productos (001-999)
 * - IdClien: ID del cliente (ej: CLI001)
 */
public class ParserPedidos {
    
    private static final String FORMATO_ESPERADO = "dd-hh-mm-dest-###-IdClien";
    
    /**
     * Parsea un archivo de pedidos mensuales
     * @param rutaArchivo Ruta al archivo de pedidos
     * @return Lista de pedidos parseados
     */
    public static List<Pedido> parsearArchivo(String rutaArchivo) {
        List<Pedido> pedidos = new ArrayList<>();
        int numeroLinea = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            
            System.out.println("Parseando archivo: " + rutaArchivo);
            System.out.println("Formato esperado: " + FORMATO_ESPERADO);
            System.out.println("=" .repeat(60));
            
            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                
                // Saltar líneas vacías y comentarios
                if (linea.trim().isEmpty() || linea.trim().startsWith("#")) {
                    continue;
                }
                
                try {
                    // Usar el método estático del modelo Pedido para crear desde archivo
                    Pedido pedido = Pedido.crearDesdeArchivo(linea, rutaArchivo, numeroLinea);
                    pedidos.add(pedido);
                    
                    System.out.println("Línea " + numeroLinea + ": " + pedido.toString());
                    
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
        System.out.println("   Pedidos válidos parseados: " + pedidos.size());
        System.out.println("   Errores encontrados: " + (numeroLinea - pedidos.size()));
        
        return pedidos;
    }
    
    /**
     * Valida un archivo completo antes de parsearlo
     * @param rutaArchivo Ruta al archivo
     * @return Resultado de la validación
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
                
                if (Pedido.validarFormatoArchivo(linea)) {
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
     * Genera un reporte detallado de los pedidos parseados
     */
    public static void generarReporte(List<Pedido> pedidos) {
        if (pedidos.isEmpty()) {
            System.out.println("📋 No hay pedidos para reportar");
            return;
        }
        
        System.out.println("\nREPORTE DETALLADO DE PEDIDOS");
        System.out.println("=" .repeat(80));
        
        // Estadísticas generales
        System.out.println("Estadísticas Generales:");
        System.out.println("   Total de pedidos: " + pedidos.size());
        
        // Agrupar por cliente
        Map<String, List<Pedido>> pedidosPorCliente = new HashMap<>();
        for (Pedido pedido : pedidos) {
            pedidosPorCliente.computeIfAbsent(pedido.getClienteId(), k -> new ArrayList<>()).add(pedido);
        }
        
        System.out.println("   Clientes únicos: " + pedidosPorCliente.size());
        
        // Agrupar por destino
        Map<String, List<Pedido>> pedidosPorDestino = new HashMap<>();
        for (Pedido pedido : pedidos) {
            pedidosPorDestino.computeIfAbsent(pedido.getAeropuertoDestino(), k -> new ArrayList<>()).add(pedido);
        }
        
        System.out.println("   Destinos únicos: " + pedidosPorDestino.size());
        
        // Estadísticas de productos
        int totalProductos = pedidos.stream().mapToInt(Pedido::getCantidadProductosMPE).sum();
        int promedioProductos = totalProductos / pedidos.size();
        int maxProductos = pedidos.stream().mapToInt(Pedido::getCantidadProductosMPE).max().orElse(0);
        int minProductos = pedidos.stream().mapToInt(Pedido::getCantidadProductosMPE).min().orElse(0);
        
        System.out.println("\nEstadísticas de Productos:");
        System.out.println("   Total de productos: " + totalProductos);
        System.out.println("   Promedio por pedido: " + promedioProductos);
        System.out.println("   Máximo por pedido: " + maxProductos);
        System.out.println("   Mínimo por pedido: " + minProductos);
        
        // Top 5 destinos más solicitados
        System.out.println("\n🌍 Top 5 Destinos Más Solicitados:");
        pedidosPorDestino.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
            .limit(5)
            .forEach(entry -> {
                System.out.println("   " + entry.getKey() + ": " + entry.getValue().size() + " pedidos");
            });
        
        // Top 5 clientes con más pedidos
        System.out.println("\n👥 Top 5 Clientes con Más Pedidos:");
        pedidosPorCliente.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
            .limit(5)
            .forEach(entry -> {
                System.out.println("   " + entry.getKey() + ": " + entry.getValue().size() + " pedidos");
            });
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
            System.out.println("ParserPedidos - Uso:");
            System.out.println("   java ParserPedidos <ruta_archivo>");
            System.out.println("   java ParserPedidos validar <ruta_archivo>");
            System.out.println("\n📋 Formato esperado: " + FORMATO_ESPERADO);
            System.out.println("   Ejemplo: 01-08-30-SVMI-150-CLI001");
            return;
        }
        
        String comando = args[0];
        String rutaArchivo = args.length > 1 ? args[1] : "data/pedidos_ejemplo.txt";
        
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
            List<Pedido> pedidos = parsearArchivo(rutaArchivo);
            
            if (!pedidos.isEmpty()) {
                generarReporte(pedidos);
            }
        }
    }
}
