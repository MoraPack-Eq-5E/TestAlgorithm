package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.core.enums.*;
import com.grupo5e.morapack.algorithm.alns.*;
import com.grupo5e.morapack.algorithm.alns.operators.construction.*;
import com.grupo5e.morapack.algorithm.alns.operators.destruction.*;
import com.grupo5e.morapack.algorithm.validation.*;

import java.time.LocalTime;
import java.util.*;

/**
 * Demo de debugging CORREGIDO con nueva estructura.
 * Versión simplificada y funcional para identificar problemas en el ALNS.
 */
public class DebugDemo {
    
    public static void main(String[] args) {
        System.out.println("=== 🔬 DEBUG ALNS - NUEVA ESTRUCTURA ===\n");
        
        DebugDemo debug = new DebugDemo();
        debug.ejecutarDebugCompleto();
    }
    
    public void ejecutarDebugCompleto() {
        try {
            System.out.println("1. Creando datos mínimos para debugging...");
            ConfiguracionDebug config = crearDatosMinimos();
            
            System.out.println("\n2. Probando componentes individuales...");
            probarComponentes(config);
            
            System.out.println("\n3. Ejecutando ALNS con logging detallado...");
            ejecutarALNSDebug(config);
            
        } catch (Exception e) {
            System.err.println("❌ Error en debug: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private ConfiguracionDebug crearDatosMinimos() {
        // Crear 4 aeropuertos mínimos
        List<Aeropuerto> aeropuertos = Arrays.asList(
            new Aeropuerto("A", "Airport A", "Country A", "América", 0, 0, 50, true),
            new Aeropuerto("B", "Airport B", "Country B", "Europa", 0, 0, 50, true),
            new Aeropuerto("C", "Airport C", "Country C", "América", 0, 0, 30, false),
            new Aeropuerto("D", "Airport D", "Country D", "Europa", 0, 0, 30, false)
        );
        
        // Crear continentes
        Set<Continente> continentes = new HashSet<>();
        Continente america = new Continente("América", "AM", "A");
        america.agregarAeropuerto("A");
        america.agregarAeropuerto("C");
        
        Continente europa = new Continente("Europa", "EU", "B");
        europa.agregarAeropuerto("B");
        europa.agregarAeropuerto("D");
        
        continentes.add(america);
        continentes.add(europa);
        
        // Crear vuelos con capacidad GENEROSA para debugging
        List<Vuelo> vuelos = Arrays.asList(
            crearVueloSeguro("V1", "A", "C", true, 20, 6.0),
            crearVueloSeguro("V2", "A", "B", false, 25, 12.0),
            crearVueloSeguro("V3", "B", "D", true, 20, 6.0),
            crearVueloSeguro("V4", "C", "D", false, 25, 12.0),
            crearVueloSeguro("V5", "A", "D", false, 30, 18.0),
            crearVueloSeguro("V6", "B", "C", false, 30, 18.0)
        );
        
        // Crear 5 paquetes simples
        List<Paquete> paquetes = Arrays.asList(
            new Paquete("PKG_001", "A", "C", "CLI_001"),
            new Paquete("PKG_002", "A", "D", "CLI_002"),
            new Paquete("PKG_003", "B", "C", "CLI_003"),
            new Paquete("PKG_004", "B", "D", "CLI_004"),
            new Paquete("PKG_005", "A", "B", "CLI_005")
        );
        
        // Configurar paquetes con EstadoGeneral
        for (Paquete paquete : paquetes) {
            paquete.setEstado(EstadoGeneral.CREADO);
            paquete.setPrioridad(1);
        }
        
        System.out.println("  ✅ Configuración mínima:");
        System.out.println("    - Aeropuertos: " + aeropuertos.size() + " (2 sedes)");
        System.out.println("    - Vuelos: " + vuelos.size() + " (capacidad alta: 20-30)");
        System.out.println("    - Paquetes: " + paquetes.size() + " (casos simples)");
        
        return new ConfiguracionDebug(paquetes, aeropuertos, vuelos, continentes);
    }
    
    private Vuelo crearVueloSeguro(String numero, String origen, String destino, 
                                  boolean mismoContinente, int capacidad, double duracion) {
        Vuelo vuelo = new Vuelo(numero, origen, destino, mismoContinente, capacidad);
        
        // Horarios seguros (sin exceder 23 horas)
        LocalTime horaSalida = LocalTime.of(8, 0);
        int horaLlegada = (8 + (int)duracion) % 24; // Modulo para evitar overflow
        
        vuelo.setHoraSalida(horaSalida);
        vuelo.setHoraLlegada(LocalTime.of(horaLlegada, 0));
        vuelo.setDuracionHoras(duracion);
        
        return vuelo;
    }
    
    private void probarComponentes(ConfiguracionDebug config) {
        System.out.println("🔍 Probando ContextoProblema...");
        
        ContextoProblema contexto = new ContextoProblema(
            config.getPaquetes(), config.getAeropuertos(),
            config.getVuelos(), config.getContinentes()
        );
        
        // Probar conectividad de cada paquete
        for (Paquete paquete : config.getPaquetes()) {
            List<Vuelo> vuelosDirectos = contexto.getVuelosDirectos(
                paquete.getAeropuertoOrigen(), paquete.getAeropuertoDestino()
            );
            System.out.println("📦 " + paquete.getId() + " (" + 
                paquete.getAeropuertoOrigen() + "→" + paquete.getAeropuertoDestino() + "): " +
                vuelosDirectos.size() + " vuelos directos");
        }
        
        System.out.println("\n🔧 Probando ConstruccionEstrategia...");
        ConstruccionEstrategia constructor = new ConstruccionEstrategia(ConstruccionEstrategia.TipoEstrategia.VORAZ);
        ValidadorRestricciones validador = new ValidadorRestricciones(
            config.getAeropuertos(), config.getVuelos(), config.getContinentes()
        );
        
        // Probar construcción de una sola ruta
        List<String> unPaquete = Arrays.asList("PKG_001");
        Solucion solucionTest = constructor.construir(new Solucion(), unPaquete, contexto, validador);
        
        System.out.println("📊 Resultado construcción individual: " + solucionTest.getCantidadPaquetes() + " paquetes");
        System.out.println("📊 Factible: " + solucionTest.isEsFactible());
        
        if (!solucionTest.getRutasPaquetes().isEmpty()) {
            Ruta ruta = solucionTest.getRutasPaquetes().values().iterator().next();
            System.out.println("🛫 Ruta creada: " + ruta.toString());
        }
    }
    
    private void ejecutarALNSDebug(ConfiguracionDebug config) {
        System.out.println("🔬 Simulando iteración ALNS manual...");
        
        // Crear contexto y validador
        ContextoProblema contexto = new ContextoProblema(
            config.getPaquetes(), config.getAeropuertos(),
            config.getVuelos(), config.getContinentes()
        );
        
        ValidadorRestricciones validador = new ValidadorRestricciones(
            config.getAeropuertos(), config.getVuelos(), config.getContinentes()
        );
        
        // Crear solución inicial con ConstruccionEstrategia
        ConstruccionEstrategia constructor = new ConstruccionEstrategia(ConstruccionEstrategia.TipoEstrategia.VORAZ);
        List<String> todosPaquetes = config.getPaquetes().stream().map(Paquete::getId).toList();
        Solucion solucionInicial = constructor.construir(new Solucion(), todosPaquetes, contexto, validador);
        
        System.out.println("📊 Solución inicial: " + solucionInicial);
        
        // Simular una iteración de destrucción/construcción
        if (!solucionInicial.getRutasPaquetes().isEmpty()) {
            DestruccionAleatoria destructor = new DestruccionAleatoria();
            List<String> paquetesRemovidos = destructor.destruir(solucionInicial, 2);
            
            System.out.println("🗑️ Paquetes removidos: " + paquetesRemovidos);
            System.out.println("📊 Después destrucción: " + solucionInicial.getCantidadPaquetes() + " paquetes");
            
            // Reconstruir
            Solucion solucionReconstruida = constructor.construir(solucionInicial, paquetesRemovidos, contexto, validador);
            System.out.println("🔧 Después construcción: " + solucionReconstruida.getCantidadPaquetes() + " paquetes");
            System.out.println("📊 Solución reconstruida: " + solucionReconstruida);
            
            if (solucionReconstruida.getCantidadPaquetes() == config.getPaquetes().size()) {
                System.out.println("✅ SUCCESS: Construcción funcionó correctamente!");
            } else {
                System.out.println("❌ ERROR: Se perdieron paquetes durante la reconstrucción");
            }
        }
        
        // Ejecutar ALNS completo pero corto
        System.out.println("\n🚀 Ejecutando ALNS completo (versión corta)...");
        ALNSSolver solver = new ALNSSolver(50, 200.0, 0.99); // Solo 50 iteraciones para debug
        solver.configurarProblema(
            config.getPaquetes(), config.getAeropuertos(),
            config.getVuelos(), config.getContinentes()
        );
        
        Solucion mejorSolucion = solver.resolver();
        System.out.println("🎯 RESULTADO FINAL: " + mejorSolucion);
        
        // Validación final
        ResultadoValidacion validacion = validador.validarSolucion(mejorSolucion);
        if (validacion.esFactible()) {
            System.out.println("✅ Solución factible - Debug exitoso");
        } else {
            System.out.println("⚠️ Solución con " + validacion.getTotalViolaciones() + " violaciones");
        }
    }
    
    // Clase de configuración simplificada
    private static class ConfiguracionDebug {
        private final List<Paquete> paquetes;
        private final List<Aeropuerto> aeropuertos;
        private final List<Vuelo> vuelos;
        private final Set<Continente> continentes;
        
        public ConfiguracionDebug(List<Paquete> paquetes, List<Aeropuerto> aeropuertos,
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
