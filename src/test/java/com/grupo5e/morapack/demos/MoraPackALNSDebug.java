package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.core.enums.*;
import com.grupo5e.morapack.core.constants.ConstantesMoraPack;
import com.grupo5e.morapack.algorithm.alns.*;
import com.grupo5e.morapack.algorithm.alns.operators.construction.*;
import com.grupo5e.morapack.algorithm.alns.operators.destruction.*;
import com.grupo5e.morapack.algorithm.validation.*;

import java.time.LocalTime;
import java.util.*;

/**
 * Versión de debugging del ALNS para identificar exactamente dónde fallan los operadores.
 * Esta versión tiene logging detallado y manejo robusto de errores.
 */
public class MoraPackALNSDebug {
    
    public static void main(String[] args) {
        System.out.println("=== DEBUG ALNS MORAPACK ===\n");
        
        MoraPackALNSDebug debug = new MoraPackALNSDebug();
        debug.ejecutarDebug();
    }
    
    public void ejecutarDebug() {
        // 1. Crear datos mínimos y controlados
        System.out.println("1. Creando datos mínimos para debugging...");
        ConfiguracionDebug config = crearDatosMinimos();
        
        // 2. Probar cada componente individualmente
        System.out.println("\n2. Probando componentes individuales...");
        probarComponentes(config);
        
        // 3. Ejecutar ALNS con debugging
        System.out.println("\n3. Ejecutando ALNS con logging detallado...");
        ejecutarALNSDebug(config);
    }
    
    private ConfiguracionDebug crearDatosMinimos() {
        // Crear red mínima: 2 sedes + 2 destinos = 4 aeropuertos total
        List<Aeropuerto> aeropuertos = new ArrayList<>();
        aeropuertos.add(new Aeropuerto("LIM", "Lima", "Perú", "América", -12.0, -77.0, 1000, true));
        aeropuertos.add(new Aeropuerto("BRU", "Bruselas", "Bélgica", "Europa", 50.9, 4.5, 1000, true));
        aeropuertos.add(new Aeropuerto("NYC", "Nueva York", "USA", "América", 40.7, -74.0, 800, false));
        aeropuertos.add(new Aeropuerto("PAR", "París", "Francia", "Europa", 48.9, 2.4, 850, false));
        
        // Vuelos con alta capacidad para evitar problemas
        List<Vuelo> vuelos = new ArrayList<>();
        // Vuelos directos desde sedes a destinos (capacity 500 = muy alta)
        vuelos.add(crearVueloSeguro("V001", "LIM", "NYC", true, 500, 12.0));   // Mismo continente
        vuelos.add(crearVueloSeguro("V002", "LIM", "BRU", false, 500, 24.0));  // Inter-continental
        vuelos.add(crearVueloSeguro("V003", "LIM", "PAR", false, 500, 24.0));  // Inter-continental
        vuelos.add(crearVueloSeguro("V004", "BRU", "NYC", false, 500, 24.0));  // Inter-continental
        vuelos.add(crearVueloSeguro("V005", "BRU", "PAR", true, 500, 12.0));   // Mismo continente
        
        // Vuelos de retorno
        vuelos.add(crearVueloSeguro("V101", "NYC", "LIM", true, 500, 12.0));
        vuelos.add(crearVueloSeguro("V102", "BRU", "LIM", false, 500, 24.0));
        vuelos.add(crearVueloSeguro("V103", "PAR", "LIM", false, 500, 24.0));
        vuelos.add(crearVueloSeguro("V104", "NYC", "BRU", false, 500, 24.0));
        vuelos.add(crearVueloSeguro("V105", "PAR", "BRU", true, 500, 12.0));
        
        // Continentes
        Set<Continente> continentes = new HashSet<>();
        Continente america = new Continente("América", "AM", "LIM");
        america.agregarAeropuerto("LIM");
        america.agregarAeropuerto("NYC");
        
        Continente europa = new Continente("Europa", "EU", "BRU");
        europa.agregarAeropuerto("BRU");
        europa.agregarAeropuerto("PAR");
        
        continentes.add(america);
        continentes.add(europa);
        
        // Solo 5 paquetes para debugging
        List<Paquete> paquetes = new ArrayList<>();
        paquetes.add(new Paquete("PKG_001", "LIM", "NYC", "CLI_001"));
        paquetes.add(new Paquete("PKG_002", "LIM", "PAR", "CLI_002"));
        paquetes.add(new Paquete("PKG_003", "BRU", "NYC", "CLI_003"));
        paquetes.add(new Paquete("PKG_004", "BRU", "PAR", "CLI_001"));
        paquetes.add(new Paquete("PKG_005", "LIM", "BRU", "CLI_002"));
        
        // Calcular fechas límite
        for (Paquete paquete : paquetes) {
            boolean mismoContinente = sonMismoContinente(paquete.getAeropuertoOrigen(), 
                                                       paquete.getAeropuertoDestino(), aeropuertos);
            paquete.calcularFechaLimite(mismoContinente, mismoContinente ? 2 : 3);
        }
        
        // Clientes simples
        List<Cliente> clientes = Arrays.asList(
            new Cliente("CLI_001", "Juan Pérez", "juan@test.com", "NYC"),
            new Cliente("CLI_002", "María García", "maria@test.com", "PAR"),
            new Cliente("CLI_003", "Pedro López", "pedro@test.com", "BRU")
        );
        
        System.out.println("  ✅ Configuración mínima:");
        System.out.println("    - Aeropuertos: " + aeropuertos.size() + " (2 sedes)");
        System.out.println("    - Vuelos: " + vuelos.size() + " (capacidad alta: 500)");
        System.out.println("    - Paquetes: " + paquetes.size() + " (casos simples)");
        System.out.println("    - Clientes: " + clientes.size());
        
        return new ConfiguracionDebug(paquetes, aeropuertos, vuelos, continentes, clientes);
    }
    
    private Vuelo crearVueloSeguro(String numero, String origen, String destino, boolean mismoContinente, 
                                   int capacidad, double duracion) {
        Vuelo vuelo = new Vuelo(numero, origen, destino, mismoContinente, capacidad);
        vuelo.setHoraSalida(LocalTime.of(8, 0));
        
        // Manejar correctamente vuelos que cruzan medianoche
        int horaLlegada = (8 + (int)duracion) % 24;
        vuelo.setHoraLlegada(LocalTime.of(horaLlegada, 0));
        vuelo.setDuracionHoras(duracion);
        return vuelo;
    }
    
    private boolean sonMismoContinente(String aeropuerto1, String aeropuerto2, List<Aeropuerto> aeropuertos) {
        String continente1 = null, continente2 = null;
        
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (aeropuerto.getCodigoIATA().equals(aeropuerto1)) continente1 = aeropuerto.getContinente();
            if (aeropuerto.getCodigoIATA().equals(aeropuerto2)) continente2 = aeropuerto.getContinente();
        }
        
        return continente1 != null && continente1.equals(continente2);
    }
    
    private void probarComponentes(ConfiguracionDebug config) {
        // 1. Probar ContextoProblema
        System.out.println("🔍 Probando ContextoProblema...");
        ContextoProblema contexto = new ContextoProblema(
            config.getPaquetes(), config.getAeropuertos(), 
            config.getVuelos(), config.getContinentes()
        );
        
        // Verificar que encuentra vuelos
        for (Paquete paquete : config.getPaquetes()) {
            List<Vuelo> vuelosDirectos = contexto.getVuelosDirectos(
                paquete.getAeropuertoOrigen(), paquete.getAeropuertoDestino()
            );
            System.out.println("  📦 " + paquete.getId() + " (" + paquete.getAeropuertoOrigen() + 
                             "→" + paquete.getAeropuertoDestino() + "): " + 
                             vuelosDirectos.size() + " vuelos directos");
            
            if (vuelosDirectos.isEmpty()) {
                List<String> rutaBFS = contexto.encontrarRutaMasCorta(
                    paquete.getAeropuertoOrigen(), paquete.getAeropuertoDestino()
                );
                System.out.println("      🔍 BFS encontró ruta: " + rutaBFS);
            }
        }
        
        // 2. Probar construcción individual
        System.out.println("\n🔧 Probando ConstruccionEstrategia (Voraz)...");
        ConstruccionEstrategia constructor = new ConstruccionEstrategia(ConstruccionEstrategia.TipoEstrategia.VORAZ);
        ValidadorRestricciones validador = new ValidadorRestricciones(
            config.getAeropuertos(), config.getVuelos(), config.getContinentes()
        );
        
        // Probar insertar un paquete individual
        List<String> unPaquete = Arrays.asList("PKG_001");
        Solucion solucionPrueba = constructor.construir(new Solucion(), unPaquete, contexto, validador);
        
        System.out.println("  📊 Resultado construcción individual: " + solucionPrueba.getCantidadPaquetes() + " paquetes");
        System.out.println("  📊 Factible: " + solucionPrueba.isEsFactible());
        
        if (solucionPrueba.getCantidadPaquetes() > 0) {
            Ruta rutaPrueba = solucionPrueba.getRutasPaquetes().get("PKG_001");
            System.out.println("  🛫 Ruta creada: " + rutaPrueba);
        }
    }
    
    private void ejecutarALNSDebug(ConfiguracionDebug config) {
        // Configurar ALNS con parámetros muy conservadores para debugging
        ALNSSolver solver = new ALNSSolver(10, 100.0, 0.9); // Solo 10 iteraciones
        solver.configurarProblema(
            config.getPaquetes(), config.getAeropuertos(), 
            config.getVuelos(), config.getContinentes()
        );
        
        // Simular manualmente una iteración ALNS
        System.out.println("🔬 Simulando iteración ALNS manual...");
        
        // 1. Crear solución inicial manualmente
        ContextoProblema contexto = new ContextoProblema(
            config.getPaquetes(), config.getAeropuertos(),
            config.getVuelos(), config.getContinentes()
        );
        ValidadorRestricciones validador = new ValidadorRestricciones(
            config.getAeropuertos(), config.getVuelos(), config.getContinentes()
        );
        
        ConstruccionEstrategia constructor = new ConstruccionEstrategia(ConstruccionEstrategia.TipoEstrategia.VORAZ);
        List<String> todosPaquetes = config.getPaquetes().stream().map(Paquete::getId).toList();
        
        Solucion solucionInicial = constructor.construir(new Solucion(), todosPaquetes, contexto, validador);
        System.out.println("  📊 Solución inicial: " + solucionInicial);
        
        if (solucionInicial.getCantidadPaquetes() > 0) {
            // 2. Probar destrucción
            DestruccionAleatoria destructor = new DestruccionAleatoria();
            List<String> paquetesRemovidos = destructor.destruir(solucionInicial, 2); // Remover solo 2
            System.out.println("  🗑️ Paquetes removidos: " + paquetesRemovidos);
            System.out.println("  📊 Después destrucción: " + solucionInicial.getCantidadPaquetes() + " paquetes");
            
            // 3. Probar construcción
            Solucion solucionReconstruida = constructor.construir(solucionInicial, paquetesRemovidos, contexto, validador);
            System.out.println("  🔧 Después construcción: " + solucionReconstruida.getCantidadPaquetes() + " paquetes");
            System.out.println("  📊 Solución reconstruida: " + solucionReconstruida);
            
            // 4. Si la reconstrucción falló, investigar por qué
            if (solucionReconstruida.getCantidadPaquetes() != config.getPaquetes().size()) {
                System.out.println("  ❌ ERROR: Construcción falló!");
                investigarFalloReconstruccion(paquetesRemovidos, contexto, validador);
            } else {
                System.out.println("  ✅ SUCCESS: Construcción funcionó correctamente!");
                
                // Ejecutar ALNS real pero con pocas iteraciones
                ALNSSolver solverFinal = new ALNSSolver(50, 100.0, 0.95);
                solverFinal.configurarProblema(config.getPaquetes(), config.getAeropuertos(), 
                                              config.getVuelos(), config.getContinentes());
                
                Solucion mejorSolucion = solverFinal.resolver();
                System.out.println("\n🎯 RESULTADO FINAL: " + mejorSolucion);
                mostrarRutasDetalladas(mejorSolucion, config);
            }
        }
    }
    
    private void investigarFalloReconstruccion(List<String> paquetesRemovidos, ContextoProblema contexto, 
                                             ValidadorRestricciones validador) {
        System.out.println("\n🔍 INVESTIGANDO FALLO DE RECONSTRUCCIÓN...");
        
        for (String paqueteId : paquetesRemovidos) {
            Paquete paquete = contexto.getPaquete(paqueteId);
            System.out.println("  📦 Investigando " + paqueteId + ": " + 
                             paquete.getAeropuertoOrigen() + " → " + paquete.getAeropuertoDestino());
            
            // Verificar vuelos directos
            List<Vuelo> vuelosDirectos = contexto.getVuelosDirectos(
                paquete.getAeropuertoOrigen(), paquete.getAeropuertoDestino()
            );
            System.out.println("    🛫 Vuelos directos disponibles: " + vuelosDirectos.size());
            
            for (Vuelo vuelo : vuelosDirectos) {
                System.out.println("      - " + vuelo.getNumeroVuelo() + 
                                 " (capacidad: " + vuelo.getCapacidadDisponible() + "/" + 
                                 vuelo.getCapacidadMaxima() + ")");
            }
            
            // Verificar BFS
            List<String> rutaBFS = contexto.encontrarRutaMasCorta(
                paquete.getAeropuertoOrigen(), paquete.getAeropuertoDestino()
            );
            System.out.println("    🔍 BFS ruta: " + rutaBFS);
            
            // Intentar crear ruta manualmente
            if (!vuelosDirectos.isEmpty() && vuelosDirectos.get(0).puedeCargar(1)) {
                System.out.println("    ✅ Debería poder crear ruta directa");
            } else if (rutaBFS.size() > 1) {
                System.out.println("    🔄 Debería poder crear ruta con conexión");
            } else {
                System.out.println("    ❌ No hay forma de crear ruta válida");
            }
        }
    }
    
    private void mostrarRutasDetalladas(Solucion solucion, ConfiguracionDebug config) {
        System.out.println("\n=== RUTAS OPTIMIZADAS DETALLADAS ===");
        
        for (Map.Entry<String, Ruta> entry : solucion.getRutasPaquetes().entrySet()) {
            String paqueteId = entry.getKey();
            Ruta ruta = entry.getValue();
            Paquete paquete = config.getPaquetesPorId().get(paqueteId);
            
            System.out.println("📦 " + paqueteId + " (Cliente: " + paquete.getClienteId() + ")");
            System.out.println("   🚀 " + ruta.getAeropuertoOrigen() + " → " + ruta.getAeropuertoDestino());
            System.out.println("   💰 Costo: $" + String.format("%.2f", ruta.getCostoTotal()));
            System.out.println("   ⏱️ Tiempo: " + String.format("%.1f", ruta.getTiempoTotalHoras()) + " horas");
            System.out.println("   🛫 Segmentos: " + ruta.getCantidadSegmentos());
            
            for (int i = 0; i < ruta.getSegmentos().size(); i++) {
                SegmentoRuta seg = ruta.getSegmentos().get(i);
                System.out.println("      " + (i+1) + ". " + seg.getAeropuertoOrigen() + 
                                 " → " + seg.getAeropuertoDestino() + 
                                 " (Vuelo " + seg.getNumeroVuelo() + ")");
            }
            System.out.println();
        }
    }
    
    
    // Clase de configuración para debugging
    public static class ConfiguracionDebug {
        private final List<Paquete> paquetes;
        private final List<Aeropuerto> aeropuertos;
        private final List<Vuelo> vuelos;
        private final Set<Continente> continentes;
        private final List<Cliente> clientes;
        private final Map<String, Paquete> paquetesPorId;
        
        public ConfiguracionDebug(List<Paquete> paquetes, List<Aeropuerto> aeropuertos,
                                 List<Vuelo> vuelos, Set<Continente> continentes, List<Cliente> clientes) {
            this.paquetes = paquetes;
            this.aeropuertos = aeropuertos;
            this.vuelos = vuelos;
            this.continentes = continentes;
            this.clientes = clientes;
            
            this.paquetesPorId = new HashMap<>();
            for (Paquete paquete : paquetes) {
                this.paquetesPorId.put(paquete.getId(), paquete);
            }
        }
        
        public List<Paquete> getPaquetes() { return paquetes; }
        public List<Aeropuerto> getAeropuertos() { return aeropuertos; }
        public List<Vuelo> getVuelos() { return vuelos; }
        public Set<Continente> getContinentes() { return continentes; }
        public List<Cliente> getClientes() { return clientes; }
        public Map<String, Paquete> getPaquetesPorId() { return paquetesPorId; }
    }
}
