package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.service.GestorCancelaciones;
import com.grupo5e.morapack.service.GestorDemoras;
import com.grupo5e.morapack.service.ParserCancelaciones;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo para mostrar el funcionamiento del sistema de cancelaciones y demoras
 */
public class DemoCancelacionesDemoras {
    
    public static void main(String[] args) {
        System.out.println("🚀 DEMO: Sistema de Cancelaciones y Demoras");
        System.out.println("=" .repeat(60));
        
        // Crear vuelos de ejemplo
        Map<String, Vuelo> vuelos = crearVuelosEjemplo();
        
        // Crear gestores
        GestorCancelaciones gestorCancelaciones = new GestorCancelaciones(vuelos);
        GestorDemoras gestorDemoras = new GestorDemoras(vuelos);
        
        System.out.println("✈️ Vuelos creados: " + vuelos.size());
        System.out.println();
        
        // Demo 1: Cancelaciones manuales
        System.out.println("🔴 DEMO 1: Cancelaciones Manuales");
        System.out.println("-" .repeat(40));
        
        // Mostrar vuelos cancelables
        List<Vuelo> vuelosCancelables = gestorCancelaciones.getVuelosCancelables();
        System.out.println("Vuelos disponibles para cancelación: " + vuelosCancelables.size());
        
        if (!vuelosCancelables.isEmpty()) {
            Vuelo vueloACancelar = vuelosCancelables.get(0);
            System.out.println("Cancelando vuelo: " + vueloACancelar.getNumeroVuelo());
            
            GestorCancelaciones.ResultadoCancelacion resultado = 
                gestorCancelaciones.cancelarManual(vueloACancelar.getNumeroVuelo(), "Demanda baja");
            
            System.out.println("Resultado: " + resultado.toString());
        }
        
        System.out.println();
        
        // Demo 2: Demoras
        System.out.println("⏰ DEMO 2: Demoras de Vuelos");
        System.out.println("-" .repeat(40));
        
        // Mostrar vuelos demorables
        List<Vuelo> vuelosDemorables = gestorDemoras.getVuelosDemorables();
        System.out.println("Vuelos disponibles para demora: " + vuelosDemorables.size());
        
        if (!vuelosDemorables.isEmpty()) {
            Vuelo vueloADemorar = vuelosDemorables.get(0);
            System.out.println("Demorando vuelo: " + vueloADemorar.getNumeroVuelo());
            
            GestorDemoras.ResultadoDemora resultado = 
                gestorDemoras.aplicarDemora(vueloADemorar.getNumeroVuelo(), "Condiciones meteorológicas");
            
            System.out.println("Resultado: " + resultado.toString());
        }
        
        System.out.println();
        
        // Demo 3: Demoras masivas por aeropuerto
        System.out.println("🌧️ DEMO 3: Demoras Masivas por Aeropuerto");
        System.out.println("-" .repeat(40));
        
        List<GestorDemoras.ResultadoDemora> resultadosDemora = 
            gestorDemoras.simularDemorasMeteorologicas("SKBO", "Tormenta eléctrica");
        
        System.out.println("Demoras aplicadas: " + resultadosDemora.size());
        int exitosas = (int) resultadosDemora.stream().filter(GestorDemoras.ResultadoDemora::isExitoso).count();
        System.out.println("Demoras exitosas: " + exitosas);
        
        System.out.println();
        
        // Demo 4: Parser de cancelaciones
        System.out.println("📁 DEMO 4: Parser de Cancelaciones");
        System.out.println("-" .repeat(40));
        
        // Validar archivo de cancelaciones
        ParserCancelaciones.ResultadoValidacion validacion = 
            ParserCancelaciones.validarArchivo("data/cancelaciones_ejemplo.txt");
        
        System.out.println("Validación: " + validacion.toString());
        
        if (validacion.isValido()) {
            // Parsear cancelaciones
            List<ParserCancelaciones.CancelacionProgramada> cancelaciones = 
                ParserCancelaciones.parsearArchivo("data/cancelaciones_ejemplo.txt");
            
            System.out.println("Cancelaciones parseadas: " + cancelaciones.size());
            
            // Aplicar algunas cancelaciones programadas
            if (!cancelaciones.isEmpty()) {
                System.out.println("Aplicando primeras 3 cancelaciones programadas...");
                for (int i = 0; i < Math.min(3, cancelaciones.size()); i++) {
                    ParserCancelaciones.CancelacionProgramada cancelacion = cancelaciones.get(i);
                    GestorCancelaciones.ResultadoCancelacion resultado = 
                        gestorCancelaciones.cancelarProgramado(
                            cancelacion.getNumeroVuelo(), 
                            cancelacion.getMotivo()
                        );
                    System.out.println("   " + resultado.toString());
                }
            }
        }
        
        System.out.println();
        
        // Demo 5: Reportes
        System.out.println("📊 DEMO 5: Reportes");
        System.out.println("-" .repeat(40));
        
        gestorCancelaciones.generarReporte();
        gestorDemoras.generarReporte();
        
        System.out.println("\n🎯 Demo completado exitosamente!");
    }
    
    /**
     * Crea vuelos de ejemplo para el demo
     */
    private static Map<String, Vuelo> crearVuelosEjemplo() {
        Map<String, Vuelo> vuelos = new HashMap<>();
        
        // Vuelos domésticos (mismo continente)
        vuelos.put("LA1234", new Vuelo("LA1234", "SKBO", "SVMI", true, 250));
        vuelos.put("LA5678", new Vuelo("LA5678", "SVMI", "SBBR", true, 200));
        vuelos.put("LA9012", new Vuelo("LA9012", "SBBR", "SPIM", true, 300));
        
        // Vuelos internacionales (distinto continente)
        vuelos.put("LA3456", new Vuelo("LA3456", "SKBO", "LATI", false, 350));
        vuelos.put("LA7890", new Vuelo("LA7890", "SVMI", "EDDI", false, 400));
        vuelos.put("LA1111", new Vuelo("LA1111", "SBBR", "LOWW", false, 300));
        
        // Vuelos con paquetes reservados
        Vuelo vueloConPaquetes = vuelos.get("LA1234");
        vueloConPaquetes.reservarPaquetes(50);
        
        Vuelo vueloConMasPaquetes = vuelos.get("LA3456");
        vueloConMasPaquetes.reservarPaquetes(100);
        
        return vuelos;
    }
}
