package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.algorithm.alns.ALNSSolver;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;
import com.grupo5e.morapack.algorithm.alns.SedeSelector;
import com.grupo5e.morapack.service.ProcesadorPedidosCSV;
import com.grupo5e.morapack.utils.MoraPackDataLoader;
import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.core.constants.ConstantesMoraPack;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Demo que muestra el funcionamiento completo del SedeSelector y ProcesadorPedidosCSV.
 * Demuestra cómo MoraPack decide automáticamente desde qué sede enviar cada pedido.
 */
public class DemoSedeSelector {
    
    public static void main(String[] args) {
        System.out.println("🚀 === DEMO: SEDE SELECTOR MORAPACK ===");
        System.out.println("Demostración de selección inteligente de sedes\n");
        
        try {
            // 1. Cargar datos reales
            System.out.println("📂 1. Cargando datos reales...");
            DatosReales datos = cargarDatos();
            
            // 2. Crear contexto del problema
            System.out.println("🧠 2. Creando contexto del problema...");
            ContextoProblema contexto = new ContextoProblema(
                new ArrayList<>(), // Paquetes se crearán después
                datos.aeropuertos, 
                datos.vuelos, 
                datos.continentes
            );
            
            // 3. Demostrar SedeSelector individual
            System.out.println("🎯 3. Demostrando SedeSelector...");
            demostrarSedeSelector(contexto);
            
            // 4. Procesar pedidos del CSV
            System.out.println("📊 4. Procesando pedidos del CSV...");
            ProcesadorPedidosCSV procesador = new ProcesadorPedidosCSV(contexto);
            ProcesadorPedidosCSV.ResultadoProcesamiento resultado = procesador.procesarPedidos();
            
            System.out.println("✅ " + resultado.resumen);
            
            // 5. Demostrar optimización con ALNS
            System.out.println("⚡ 5. Optimizando con ALNS (muestra)...");
            demostrarOptimizacionALNS(resultado, datos);
            
        } catch (Exception e) {
            System.err.println("❌ Error en demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static DatosReales cargarDatos() {
        List<Aeropuerto> aeropuertos = MoraPackDataLoader.cargarAeropuertos();
        List<Vuelo> vuelos = MoraPackDataLoader.cargarVuelos(aeropuertos);
        Set<Continente> continentes = MoraPackDataLoader.crearContinentes(aeropuertos);
        
        System.out.printf("   - %d aeropuertos cargados%n", aeropuertos.size());
        System.out.printf("   - %d vuelos cargados%n", vuelos.size());
        System.out.printf("   - %d continentes creados%n", continentes.size());
        
        return new DatosReales(aeropuertos, vuelos, continentes);
    }
    
    private static void demostrarSedeSelector(ContextoProblema contexto) {
        SedeSelector selector = new SedeSelector(contexto);
        LocalDateTime ahora = LocalDateTime.now();
        
        // Casos de prueba representativos
        String[][] casosPrueba = {
            {"EDDI", "Berlín, Alemania", "Europa"},
            {"SKBO", "Bogotá, Colombia", "América del Sur"},
            {"VIDP", "Delhi, India", "Asia"},
            {"SBBR", "Brasilia, Brasil", "América del Sur"},
            {"EHAM", "Amsterdam, Holanda", "Europa"},
            {"OERK", "Riad, Arabia Saudita", "Asia"}
        };
        
        System.out.println("   Evaluando destinos representativos:");
        System.out.println("   ┌─────────────────────────────────────────────────┐");
        System.out.println("   │ Destino          │ Sede Óptima │ Razón           │");
        System.out.println("   ├─────────────────────────────────────────────────┤");
        
        for (String[] caso : casosPrueba) {
            String destino = caso[0];
            String ciudad = caso[1];
            String continente = caso[2];
            
            String sedeOptima = selector.seleccionarMejorSede(destino, ahora, 100);
            String nombreSede = obtenerNombreSede(sedeOptima);
            String razon = explicarSeleccion(sedeOptima, continente);
            
            System.out.printf("   │ %-16s │ %-11s │ %-15s │%n", 
                            destino + " (" + ciudad + ")", nombreSede, razon);
        }
        
        System.out.println("   └─────────────────────────────────────────────────┘");
        
        // Mostrar estadísticas
        Map<String, Integer> estadisticas = selector.obtenerEstadisticasUso();
        System.out.println("\n   📈 Distribución de selecciones:");
        estadisticas.forEach((sede, cantidad) -> 
            System.out.printf("      - %s: %d selecciones%n", obtenerNombreSede(sede), cantidad));
    }
    
    private static void demostrarOptimizacionALNS(ProcesadorPedidosCSV.ResultadoProcesamiento resultado,
                                                 DatosReales datos) {
        
        if (resultado.paquetes.isEmpty()) {
            System.out.println("   ⚠️  No hay paquetes para optimizar");
            return;
        }
        
        // Tomar una muestra pequeña para la demostración
        List<Paquete> muestra = resultado.paquetes.subList(0, Math.min(50, resultado.paquetes.size()));
        
        ContextoProblema contextoConPaquetes = new ContextoProblema(
            muestra, datos.aeropuertos, datos.vuelos, datos.continentes
        );
        
        ALNSSolver solver = new ALNSSolver(1, 500.0, 0.995);
        
        // Configurar el problema con los datos del contexto
        solver.configurarProblema(
            muestra,
            datos.aeropuertos, 
            datos.vuelos, 
            datos.continentes
        );
        
        System.out.printf("   🎯 Optimizando %d paquetes con ALNS (10 iteraciones)...%n", muestra.size());
        
        long inicio = System.currentTimeMillis();
        Solucion solucion = solver.resolver();
        long fin = System.currentTimeMillis();
        
        // DIAGNÓSTICO: Ver historial de fitness
        System.out.println("   📈 Evolución del fitness durante ejecución:");
        List<Double> historial = solver.getHistorialFitness();
        for (int i = 0; i < Math.min(historial.size(), 5); i++) {
            System.out.printf("      Iteración %d: %.2f%n", i, historial.get(i));
        }
        if (historial.size() > 5) {
            System.out.printf("      ... (total %d iteraciones)%n", historial.size());
        }
        
        System.out.printf("   ⏱️  Tiempo de optimización: %d ms%n", fin - inicio);
        System.out.printf("   📊 Paquetes ruteados: %d/%d%n", 
                        solucion.getRutasPaquetes().size(), muestra.size());
        System.out.printf("   ✅ Solución factible: %s%n", solucion.isEsFactible() ? "Sí" : "No");
        System.out.printf("   📈 Función objetivo: %.2f%n", solucion.getFitness());
        
        // Mostrar algunas rutas de ejemplo
        System.out.println("\n   🛣️  Ejemplos de rutas generadas:");
        solucion.getRutasPaquetes().entrySet().stream()
                .limit(3)
                .forEach(entry -> {
                    String paqueteId = entry.getKey();
                    Ruta ruta = entry.getValue();
                    System.out.printf("      • %s: %s%n", paqueteId, formatearRuta(ruta));
                });
    }
    
    private static String obtenerNombreSede(String codigoSede) {
        return switch(codigoSede) {
            case "SPIM" -> "Lima";
            case "EBCI" -> "Bruselas";
            case "UBBB" -> "Baku";
            default -> codigoSede;
        };
    }
    
    private static String explicarSeleccion(String sede, String continenteDestino) {
        String continenteSede = switch(sede) {
            case "SPIM" -> "América del Sur";
            case "EBCI" -> "Europa";
            case "UBBB" -> "Asia";
            default -> "Desconocido";
        };
        
        if (continenteSede.equals(continenteDestino)) {
            return "Mismo continente";
        } else {
            return "Mejor conexión";
        }
    }
    
    private static String formatearRuta(Ruta ruta) {
        if (ruta.getSegmentos().isEmpty()) {
            return "Sin segmentos";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ruta.getSegmentos().size(); i++) {
            SegmentoRuta seg = ruta.getSegmentos().get(i);
            if (i == 0) {
                sb.append(seg.getAeropuertoOrigen());
            }
            sb.append(" → ").append(seg.getAeropuertoDestino());
        }
        return sb.toString();
    }
    
    /**
     * Clase para contener todos los datos cargados
     */
    private static class DatosReales {
        final List<Aeropuerto> aeropuertos;
        final List<Vuelo> vuelos;
        final Set<Continente> continentes;
        
        DatosReales(List<Aeropuerto> aeropuertos, List<Vuelo> vuelos, Set<Continente> continentes) {
            this.aeropuertos = aeropuertos;
            this.vuelos = vuelos;
            this.continentes = continentes;
        }
    }
}
