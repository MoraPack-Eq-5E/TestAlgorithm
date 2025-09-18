package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.algorithm.alns.ALNSSolver;
import com.grupo5e.morapack.algorithm.alns.ALNSConfig;
import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.core.enums.*;
import com.grupo5e.morapack.utils.MoraPackDataLoader;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Demo REALISTA del ALNS mejorado con escenarios que realmente tienen sentido
 * para el contexto de MoraPack. Este demo simula situaciones reales donde
 * el ALNS puede optimizar y mejorar soluciones.
 * 
 * Para deshabilitar el debug verbose, cambiar ENABLE_VERBOSE_DEBUG a false.
 */
public class DemoALNSRealista {
    
    private static final boolean ENABLE_VERBOSE_DEBUG = false; // Cambiar a true para más detalles
    
    public static void main(String[] args) {
        System.out.println("🚀 DEMO REALISTA: ALNS Mejorado para MoraPack");
        System.out.println("=" .repeat(80));
        
        DemoALNSRealista demo = new DemoALNSRealista();
        demo.ejecutarDemoCompleto();
    }
    
    public void ejecutarDemoCompleto() {
        try {
            // 1. Cargar datos reales del sistema
            System.out.println("📁 Cargando datos reales de MoraPack...");
            DatosReales datos = cargarDatosReales();
            
            // 2. Configurar ALNS con parámetros optimizados
            System.out.println("\n⚙️ Configurando ALNS mejorado...");
            configurarALNS();
            
            // 3. Escenario 1: Optimización inicial de rutas
            System.out.println("\n🎯 ESCENARIO 1: Optimización inicial de rutas");
            ejecutarEscenarioOptimizacionInicial(datos);
            
            // 4. Escenario 2: Reasignación por cancelaciones
            System.out.println("\n🎯 ESCENARIO 2: Reasignación por cancelaciones de vuelos");
            ejecutarEscenarioCancelaciones(datos);
            
            // 5. Escenario 3: Reasignación por demoras
            System.out.println("\n🎯 ESCENARIO 3: Reasignación por demoras de vuelos");
            ejecutarEscenarioDemoras(datos);
            
            // 6. Escenario 4: Optimización con alta demanda
            System.out.println("\n🎯 ESCENARIO 4: Optimización con alta demanda");
            ejecutarEscenarioAltaDemanda(datos);
            
            System.out.println("\n✅ DEMO COMPLETADO - ALNS funcionando correctamente!");
            
        } catch (Exception e) {
            System.err.println("❌ Error en demo: " + e.getMessage());
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
    
    private void ejecutarEscenarioOptimizacionInicial(DatosReales datos) {
        // Crear paquetes con diferentes prioridades y fechas límite
        List<Paquete> paquetes = crearPaquetesEscenario1(datos);
        
        System.out.println("   📦 Paquetes creados: " + paquetes.size());
        System.out.println("   🎯 Objetivo: Optimizar rutas iniciales");
        
        // Crear ALNS y resolver con parámetros más conservadores
        ALNSSolver alns = new ALNSSolver(150, 50.0, 0.998);
        alns.configurarProblema(paquetes, datos.aeropuertos, datos.vuelos, datos.continentes);
        
        long inicio = System.currentTimeMillis();
        Solucion solucion = alns.resolver();
        long tiempo = System.currentTimeMillis() - inicio;
        
        mostrarResultados("Optimización Inicial", solucion, tiempo, paquetes.size());
    }
    
    private void ejecutarEscenarioCancelaciones(DatosReales datos) {
        // Crear paquetes y simular cancelaciones
        List<Paquete> paquetes = crearPaquetesEscenario2(datos);
        List<Vuelo> vuelosConCancelaciones = simularCancelaciones(datos.vuelos);
        
        System.out.println("   📦 Paquetes: " + paquetes.size());
        System.out.println("   ❌ Vuelos cancelados: " + (datos.vuelos.size() - vuelosConCancelaciones.size()));
        System.out.println("   🎯 Objetivo: Reasignar paquetes afectados");
        
        ALNSSolver alns = new ALNSSolver(200, 75.0, 0.995);
        alns.configurarProblema(paquetes, datos.aeropuertos, vuelosConCancelaciones, datos.continentes);
        
        long inicio = System.currentTimeMillis();
        Solucion solucion = alns.resolver();
        long tiempo = System.currentTimeMillis() - inicio;
        
        mostrarResultados("Reasignación por Cancelaciones", solucion, tiempo, paquetes.size());
    }
    
    private void ejecutarEscenarioDemoras(DatosReales datos) {
        // Crear paquetes urgentes y simular demoras
        List<Paquete> paquetes = crearPaquetesEscenario3(datos);
        List<Vuelo> vuelosConDemoras = simularDemoras(datos.vuelos);
        
        System.out.println("   📦 Paquetes urgentes: " + paquetes.size());
        System.out.println("   ⏰ Vuelos con demora: " + vuelosConDemoras.stream()
            .mapToInt(v -> v.getHorasDemora() > 0 ? 1 : 0).sum());
        System.out.println("   🎯 Objetivo: Reoptimizar por demoras");
        
        ALNSSolver alns = new ALNSSolver(180, 60.0, 0.996);
        alns.configurarProblema(paquetes, datos.aeropuertos, vuelosConDemoras, datos.continentes);
        
        long inicio = System.currentTimeMillis();
        Solucion solucion = alns.resolver();
        long tiempo = System.currentTimeMillis() - inicio;
        
        mostrarResultados("Reasignación por Demoras", solucion, tiempo, paquetes.size());
    }
    
    private void ejecutarEscenarioAltaDemanda(DatosReales datos) {
        // Crear muchos paquetes para simular alta demanda
        List<Paquete> paquetes = crearPaquetesEscenario4(datos);
        
        System.out.println("   📦 Paquetes (alta demanda): " + paquetes.size());
        System.out.println("   🎯 Objetivo: Optimizar con alta carga");
        
        ALNSSolver alns = new ALNSSolver(300, 100.0, 0.997);
        alns.configurarProblema(paquetes, datos.aeropuertos, datos.vuelos, datos.continentes);
        
        long inicio = System.currentTimeMillis();
        Solucion solucion = alns.resolver();
        long tiempo = System.currentTimeMillis() - inicio;
        
        mostrarResultados("Optimización Alta Demanda", solucion, tiempo, paquetes.size());
    }
    
    private List<Paquete> crearPaquetesEscenario1(DatosReales datos) {
        List<Paquete> paquetes = new ArrayList<>();
        Random random = new Random(42); // Seed fijo para reproducibilidad
        
        // Crear paquetes con diferentes características
        String[] origenes = {"SKBO", "SEQM", "SVMI", "SBBR", "SPIM"};
        String[] destinos = {"SCEL", "SABE", "SGAS", "SUAA", "LATI", "EDDI", "LOWW", "EBCI"};
        
        for (int i = 0; i < 15; i++) {
            String origen = origenes[random.nextInt(origenes.length)];
            String destino = destinos[random.nextInt(destinos.length)];
            
            Paquete paquete = new Paquete("PKG_" + String.format("%03d", i), origen, destino, "CLI" + String.format("%03d", i % 5));
            
            // Asignar prioridades variadas
            paquete.setPrioridad(random.nextInt(3) + 1);
            
            // Asignar fechas límite variadas (1-5 días)
            int diasLimite = random.nextInt(5) + 1;
            paquete.setFechaLimiteEntrega(LocalDateTime.now().plusDays(diasLimite));
            
            paquetes.add(paquete);
        }
        
        return paquetes;
    }
    
    private List<Paquete> crearPaquetesEscenario2(DatosReales datos) {
        List<Paquete> paquetes = new ArrayList<>();
        
        // Paquetes que podrían verse afectados por cancelaciones
        String[] rutasCriticas = {"SKBO-SVMI", "SEQM-SBBR", "SVMI-SCEL", "SBBR-SABE"};
        
        for (int i = 0; i < 12; i++) {
            String[] ruta = rutasCriticas[i % rutasCriticas.length].split("-");
            String origen = ruta[0];
            String destino = ruta[1];
            
            Paquete paquete = new Paquete("PKG_CANC_" + String.format("%03d", i), origen, destino, "CLI" + String.format("%03d", i % 3));
            paquete.setPrioridad(1); // Alta prioridad
            paquete.setFechaLimiteEntrega(LocalDateTime.now().plusDays(2)); // Urgente
            
            paquetes.add(paquete);
        }
        
        return paquetes;
    }
    
    private List<Paquete> crearPaquetesEscenario3(DatosReales datos) {
        List<Paquete> paquetes = new ArrayList<>();
        Random random = new Random(456);
        
        // Paquetes urgentes que necesitan reoptimización
        for (int i = 0; i < 10; i++) {
            String origen = "SKBO";
            String destino = datos.aeropuertos.get(random.nextInt(datos.aeropuertos.size())).getCodigoIATA();
            
            Paquete paquete = new Paquete("PKG_URG_" + String.format("%03d", i), origen, destino, "CLI" + String.format("%03d", i % 4));
            paquete.setPrioridad(1); // Alta prioridad
            paquete.setFechaLimiteEntrega(LocalDateTime.now().plusHours(12)); // Muy urgente
            
            paquetes.add(paquete);
        }
        
        return paquetes;
    }
    
    private List<Paquete> crearPaquetesEscenario4(DatosReales datos) {
        List<Paquete> paquetes = new ArrayList<>();
        Random random = new Random(789);
        
        // Muchos paquetes para simular alta demanda
        for (int i = 0; i < 25; i++) {
            String origen = datos.aeropuertos.get(random.nextInt(datos.aeropuertos.size())).getCodigoIATA();
            String destino = datos.aeropuertos.get(random.nextInt(datos.aeropuertos.size())).getCodigoIATA();
            
            // Evitar mismo origen y destino
            while (origen.equals(destino)) {
                destino = datos.aeropuertos.get(random.nextInt(datos.aeropuertos.size())).getCodigoIATA();
            }
            
            Paquete paquete = new Paquete("PKG_ALTA_" + String.format("%03d", i), origen, destino, "CLI" + String.format("%03d", i % 8));
            paquete.setPrioridad(random.nextInt(3) + 1);
            paquete.setFechaLimiteEntrega(LocalDateTime.now().plusDays(random.nextInt(4) + 1));
            
            paquetes.add(paquete);
        }
        
        return paquetes;
    }
    
    private List<Vuelo> simularCancelaciones(List<Vuelo> vuelos) {
        List<Vuelo> vuelosDisponibles = new ArrayList<>(vuelos);
        Random random = new Random(999);
        
        // Cancelar 20% de los vuelos aleatoriamente
        int vuelosACancelar = (int) (vuelos.size() * 0.2);
        for (int i = 0; i < vuelosACancelar; i++) {
            int indice = random.nextInt(vuelosDisponibles.size());
            vuelosDisponibles.remove(indice);
        }
        
        return vuelosDisponibles;
    }
    
    private List<Vuelo> simularDemoras(List<Vuelo> vuelos) {
        List<Vuelo> vuelosConDemoras = new ArrayList<>();
        Random random = new Random(888);
        
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
            
            // Simular demora en 30% de los vuelos
            if (random.nextDouble() < 0.3) {
                vueloCopia.setHorasDemora(3); // Demora de 3 horas
                vueloCopia.setEstadoVuelo(EstadoGeneral.DEMORADO);
            } else {
                vueloCopia.setHorasDemora(0);
            }
            
            vuelosConDemoras.add(vueloCopia);
        }
        
        return vuelosConDemoras;
    }
    
    private void mostrarResultados(String escenario, Solucion solucion, long tiempoMs, int totalPaquetes) {
        System.out.println("\n   📊 RESULTADOS - " + escenario + ":");
        System.out.println("   ⏱️  Tiempo: " + (tiempoMs / 1000.0) + " segundos");
        System.out.println("   📦 Paquetes procesados: " + totalPaquetes);
        System.out.println("   🛣️  Rutas generadas: " + solucion.getRutasPaquetes().size());
        System.out.println("   💰 Costo total: " + String.format("%.2f", solucion.getCostoTotal()));
        System.out.println("   ⏰ Tiempo total: " + String.format("%.2f", solucion.getTiempoTotalHoras()) + " horas");
        System.out.println("   ✅ Solución factible: " + (solucion.isEsFactible() ? "SÍ" : "NO"));
        
        // Mostrar algunas rutas como ejemplo solo si está habilitado el debug verbose
        if (ENABLE_VERBOSE_DEBUG) {
            System.out.println("   📋 Ejemplo de rutas:");
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
        System.out.println("   📈 Eficiencia: " + String.format("%.1f", eficiencia) + "% de paquetes ruteados");
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
     * 4. Los escenarios se ejecutan secuencialmente:
     *    - Escenario 1: Optimización inicial (15 paquetes)
     *    - Escenario 2: Cancelaciones (12 paquetes, 20% vuelos cancelados)
     *    - Escenario 3: Demoras (10 paquetes urgentes, 30% vuelos con demora)
     *    - Escenario 4: Alta demanda (25 paquetes)
     */
}
