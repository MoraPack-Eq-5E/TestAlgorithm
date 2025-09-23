package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.algorithm.alns.ALNSSolver;
import com.grupo5e.morapack.algorithm.alns.ALNSConfig;
import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.utils.MoraPackDataLoader;

import java.util.*;

/**
 * Demo REALISTA del ALNS mejorado con escenarios que realmente tienen sentido
 * para el contexto de MoraPack. Este demo simula situaciones reales donde
 * el ALNS puede optimizar y mejorar soluciones.
 * 
 * Para deshabilitar el debug verbose, cambiar ENABLE_VERBOSE_DEBUG a false.
 */
public class DemoALNSRealista {
    
    private static final boolean ENABLE_VERBOSE_DEBUG = false; // Production setting: verbose logging disabled
    
    public static void main(String[] args) {
        System.out.println("DEMO REALISTA: ALNS Mejorado para MoraPack");
        System.out.println("=" .repeat(80));
        
        DemoALNSRealista demo = new DemoALNSRealista();
        demo.ejecutarDemoCompleto();
    }
    
    public void ejecutarDemoCompleto() {
        try {
            // 1. Cargar datos reales del sistema
            System.out.println("Cargando datos reales de MoraPack...");
            DatosReales datos = cargarDatosReales();
            
            // 2. Cargar datos de prueba desde archivos
            System.out.println("\nCargando datos de prueba desde archivos...");
            MoraPackDataLoader.DatosPrueba datosPrueba = cargarDatosPrueba();
            
            // 3. Configurar ALNS con parámetros optimizados
            System.out.println("\nConfigurando ALNS mejorado...");
            configurarALNS();
            
            // 4. Escenario 1: Optimización inicial de rutas
            System.out.println("\nESCENARIO 1: Optimización inicial de rutas");
            ejecutarEscenarioOptimizacionInicial(datos, datosPrueba);
            
            // 5. Escenario 2: Reasignación por cancelaciones
            System.out.println("\nESCENARIO 2: Reasignación por cancelaciones de vuelos");
            ejecutarEscenarioCancelaciones(datos, datosPrueba);
            
            // 6. Escenario 3: Reasignación por demoras
            System.out.println("\nESCENARIO 3: Reasignación por demoras de vuelos");
            ejecutarEscenarioDemoras(datos, datosPrueba);
            
            // 7. Escenario 4: Optimización con alta demanda
            System.out.println("\nESCENARIO 4: Optimización con alta demanda");
            ejecutarEscenarioAltaDemanda(datos, datosPrueba);
            
            System.out.println("\nDEMO COMPLETADO - ALNS funcionando correctamente!");
            
        } catch (Exception e) {
            System.err.println("Error en demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private DatosReales cargarDatosReales() {
        List<Aeropuerto> aeropuertos = MoraPackDataLoader.cargarAeropuertos();
        List<Vuelo> vuelos = MoraPackDataLoader.cargarVuelos(aeropuertos);
        Set<Continente> continentes = MoraPackDataLoader.crearContinentes(aeropuertos);
        
        System.out.println("   ✓ Aeropuertos: " + aeropuertos.size());
        System.out.println("   ✓ Vuelos: " + vuelos.size());
        System.out.println("   ✓ Continentes: " + continentes.size());
        
        return new DatosReales(aeropuertos, vuelos, continentes);
    }
    
    private MoraPackDataLoader.DatosPrueba cargarDatosPrueba() {
        MoraPackDataLoader.DatosPrueba datosPrueba = MoraPackDataLoader.cargarDatosPrueba();
        
        System.out.println("   ✓ Paquetes de prueba: " + datosPrueba.paquetes.size());
        System.out.println("   ✓ Clientes de prueba: " + datosPrueba.clientes.size());
        System.out.println("   ✓ Pedidos de prueba: " + datosPrueba.pedidos.size());
        System.out.println("   ✓ Cancelaciones: " + datosPrueba.cancelaciones.size());
        System.out.println("   ✓ Demoras: " + datosPrueba.demoras.size());
        
        return datosPrueba;
    }
    
    private void configurarALNS() {
        ALNSConfig config = ALNSConfig.getInstance();
        
        // Configurar debug verbose basado en la constante
        config.setEnableVerboseLogging(ENABLE_VERBOSE_DEBUG);
        
        // Mostrar configuración actual (ALNSConfig usa valores por defecto)
        System.out.println("   ✓ Factor de reacción: " + config.getReactionFactor());
        System.out.println("   ✓ Intervalo de actualización: " + config.getUpdateInterval());
        System.out.println("   ✓ Tasa de enfriamiento: " + config.getCoolingRate());
        System.out.println("   ✓ Debug verbose: " + (ENABLE_VERBOSE_DEBUG ? "HABILITADO" : "DESHABILITADO"));
        
        if (ENABLE_VERBOSE_DEBUG) {
            System.out.println("   ✓ Regret-2 habilitado: " + config.isUseNRegret2());
            System.out.println("   ✓ Regret-3 habilitado: " + config.isUseNRegret3());
            System.out.println("   ✓ Shaw Removal habilitado: " + config.isUseShawSimplifiedRemovalDeterministic());
            System.out.println("   ✓ Time-Oriented habilitado: " + config.isUseTimeOrientedRemovalDeterministic());
        }
    }
    
    private void ejecutarEscenarioOptimizacionInicial(DatosReales datos, MoraPackDataLoader.DatosPrueba datosPrueba) {
        // Usar paquetes de prueba con diferentes prioridades
        List<Paquete> paquetes = new ArrayList<>(datosPrueba.paquetes);
        
        System.out.println("   Paquetes cargados: " + paquetes.size());
        System.out.println("   Objetivo: Optimizar rutas iniciales");
        
        // Crear ALNS y resolver con parámetros más conservadores
        ALNSSolver alns = new ALNSSolver(150, 50.0, 0.998);
        alns.configurarProblema(paquetes, datos.aeropuertos, datos.vuelos, datos.continentes);
        
        long inicio = System.currentTimeMillis();
        Solucion solucion = alns.resolver();
        long tiempo = System.currentTimeMillis() - inicio;
        
        mostrarResultados("Optimización Inicial", solucion, tiempo, paquetes.size());
    }
    
    private void ejecutarEscenarioCancelaciones(DatosReales datos, MoraPackDataLoader.DatosPrueba datosPrueba) {
        // Usar paquetes de prueba y aplicar cancelaciones reales
        List<Paquete> paquetes = new ArrayList<>(datosPrueba.paquetes);
        List<Vuelo> vuelosConCancelaciones = simularCancelacionesReales(datos.vuelos, datosPrueba.cancelaciones);
        
        System.out.println("   Paquetes: " + paquetes.size());
        System.out.println("   Vuelos cancelados: " + datosPrueba.cancelaciones.size());
        System.out.println("   Objetivo: Reasignar paquetes afectados");
        
        ALNSSolver alns = new ALNSSolver(200, 75.0, 0.995);
        alns.configurarProblema(paquetes, datos.aeropuertos, vuelosConCancelaciones, datos.continentes);
        
        long inicio = System.currentTimeMillis();
        Solucion solucion = alns.resolver();
        long tiempo = System.currentTimeMillis() - inicio;
        
        mostrarResultados("Reasignación por Cancelaciones", solucion, tiempo, paquetes.size());
    }
    
    private void ejecutarEscenarioDemoras(DatosReales datos, MoraPackDataLoader.DatosPrueba datosPrueba) {
        // Usar paquetes de prueba y aplicar demoras reales
        List<Paquete> paquetes = new ArrayList<>(datosPrueba.paquetes);
        List<Vuelo> vuelosConDemoras = simularDemorasReales(datos.vuelos, datosPrueba.demoras);
        
        System.out.println("   Paquetes: " + paquetes.size());
        System.out.println("   Vuelos con demora: " + datosPrueba.demoras.size());
        System.out.println("   Objetivo: Reoptimizar por demoras");
        
        ALNSSolver alns = new ALNSSolver(180, 60.0, 0.996);
        alns.configurarProblema(paquetes, datos.aeropuertos, vuelosConDemoras, datos.continentes);
        
        long inicio = System.currentTimeMillis();
        Solucion solucion = alns.resolver();
        long tiempo = System.currentTimeMillis() - inicio;
        
        mostrarResultados("Reasignación por Demoras", solucion, tiempo, paquetes.size());
    }
    
    private void ejecutarEscenarioAltaDemanda(DatosReales datos, MoraPackDataLoader.DatosPrueba datosPrueba) {
        // Usar todos los paquetes de prueba para simular alta demanda
        List<Paquete> paquetes = new ArrayList<>(datosPrueba.paquetes);
        
        System.out.println("   Paquetes (alta demanda): " + paquetes.size());
        System.out.println("   Objetivo: Optimizar con alta carga");
        
        ALNSSolver alns = new ALNSSolver(300, 100.0, 0.997);
        alns.configurarProblema(paquetes, datos.aeropuertos, datos.vuelos, datos.continentes);
        
        long inicio = System.currentTimeMillis();
        Solucion solucion = alns.resolver();
        long tiempo = System.currentTimeMillis() - inicio;
        
        mostrarResultados("Optimización Alta Demanda", solucion, tiempo, paquetes.size());
    }
    
    
    private void mostrarResultados(String escenario, Solucion solucion, long tiempoMs, int totalPaquetes) {
        System.out.println("\n   RESULTADOS - " + escenario + ":");
        System.out.println("   Tiempo: " + (tiempoMs / 1000.0) + " segundos");
        System.out.println("   Paquetes procesados: " + totalPaquetes);
        System.out.println("   Rutas generadas: " + solucion.getRutasPaquetes().size());
        System.out.println("   Costo total: " + String.format("%.2f", solucion.getCostoTotal()));
        System.out.println("   Tiempo total: " + String.format("%.2f", solucion.getTiempoTotalHoras()) + " horas");
        System.out.println("   Solución factible: " + (solucion.isEsFactible() ? "SÍ" : "NO"));
        
        // Mostrar algunas rutas como ejemplo solo si está habilitado el debug verbose
        if (ENABLE_VERBOSE_DEBUG) {
            System.out.println("   Ejemplo de rutas:");
            int contador = 0;
            for (Map.Entry<String, Ruta> entry : solucion.getRutasPaquetes().entrySet()) {
                if (contador < 3) { // Mostrar solo las primeras 3
                    Ruta ruta = entry.getValue();
                    System.out.println("      • " + entry.getKey() + ": " + 
                        String.format("%.2f", ruta.getCostoTotal()) + " costo, " + 
                        String.format("%.2f", ruta.getTiempoTotalHoras()) + "h");
                    contador++;
                }
            }
        }
        
        // Mostrar resumen de eficiencia
        double eficiencia = (double) solucion.getRutasPaquetes().size() / totalPaquetes * 100;
        System.out.println("   Eficiencia: " + String.format("%.1f", eficiencia) + "% de paquetes ruteados");
    }
    
    private List<Vuelo> simularCancelacionesReales(List<Vuelo> vuelos, List<String> vuelosACancelar) {
        List<Vuelo> vuelosDisponibles = new ArrayList<>();
        
        for (Vuelo vuelo : vuelos) {
            // Verificar si este vuelo debe ser cancelado
            boolean debeCancelar = vuelosACancelar.stream()
                .anyMatch(numeroVuelo -> vuelo.getNumeroVuelo().contains(numeroVuelo) || 
                                        numeroVuelo.contains(vuelo.getNumeroVuelo()));
            
            if (!debeCancelar) {
                vuelosDisponibles.add(vuelo);
            }
        }
        
        return vuelosDisponibles;
    }
    
    private List<Vuelo> simularDemorasReales(List<Vuelo> vuelos, List<String> vuelosConDemora) {
        List<Vuelo> vuelosConDemoras = new ArrayList<>();
        
        for (Vuelo vuelo : vuelos) {
            Vuelo vueloCopia = new Vuelo();
            vueloCopia.setNumeroVuelo(vuelo.getNumeroVuelo());
            vueloCopia.setAeropuertoOrigen(vuelo.getAeropuertoOrigen());
            vueloCopia.setAeropuertoDestino(vuelo.getAeropuertoDestino());
            vueloCopia.setHoraSalida(vuelo.getHoraSalida());
            vueloCopia.setHoraLlegada(vuelo.getHoraLlegada());
            vueloCopia.setCapacidadMaxima(vuelo.getCapacidadMaxima());
            vueloCopia.setPaquetesReservados(vuelo.getPaquetesReservados());
            vueloCopia.setMismoContinente(vuelo.isMismoContinente());
            vueloCopia.setDuracionHoras(vuelo.getDuracionHoras());
            vueloCopia.setTipoVuelo(vuelo.getTipoVuelo());
            vueloCopia.setFrecuenciaDiaria(vuelo.getFrecuenciaDiaria());
            vueloCopia.setEstadoVuelo(vuelo.getEstadoVuelo());
            vueloCopia.setPuedeCancelar(vuelo.isPuedeCancelar());
            
            // Verificar si este vuelo debe tener demora
            boolean tieneDemora = vuelosConDemora.stream()
                .anyMatch(numeroVuelo -> vuelo.getNumeroVuelo().contains(numeroVuelo) || 
                                        numeroVuelo.contains(vuelo.getNumeroVuelo()));
            
            if (tieneDemora) {
                vueloCopia.setHorasDemora(3); // Demora de 3 horas
                vueloCopia.setEstadoVuelo(com.grupo5e.morapack.core.enums.EstadoGeneral.DEMORADO);
            } else {
                vueloCopia.setHorasDemora(0);
            }
            
            vuelosConDemoras.add(vueloCopia);
        }
        
        return vuelosConDemoras;
    }
    
    // Clase auxiliar para agrupar datos
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
    
    /*
     * INSTRUCCIONES DE USO:
     * 
     * 1. Para ejecutar con debug mínimo (recomendado para testing):
     *    - ENABLE_VERBOSE_DEBUG = false (ya está configurado así)
     * 
     * 2. Para ejecutar con debug detallado (para debugging):
     *    - Cambiar ENABLE_VERBOSE_DEBUG = true
     *    - Esto mostrará detalles de operadores, rutas individuales, etc.
     * 
     * 3. Para deshabilitar completamente el debug del ALNS:
     *    - En ALNSConfig, usar setEnableVerboseLogging(false)
     * 
     * 4. Los escenarios se ejecutan secuencialmente usando datos de archivos:
     *    - Escenario 1: Optimización inicial (paquetes desde datos_prueba_completos.txt)
     *    - Escenario 2: Cancelaciones (paquetes + cancelaciones desde archivo)
     *    - Escenario 3: Demoras (paquetes + demoras desde archivo)
     *    - Escenario 4: Alta demanda (todos los paquetes del archivo)
     * 
     * 5. Los datos se cargan desde:
     *    - data/datos_prueba_completos.txt (paquetes, clientes, pedidos, cancelaciones, demoras)
     *    - data/aeropuertosinfo.txt (aeropuertos)
     *    - data/vuelos.txt (vuelos)
     */
}
