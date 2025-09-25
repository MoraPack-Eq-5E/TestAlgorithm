package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorConstruccion;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorDestruccion;
import com.grupo5e.morapack.algorithm.alns.operators.construction.ConstruccionInteligente;
import com.grupo5e.morapack.algorithm.alns.operators.construction.ConstruccionEstrategia;
import com.grupo5e.morapack.algorithm.alns.operators.construction.ConstruccionMultiDepot;
import com.grupo5e.morapack.algorithm.alns.operators.construction.RegretKInsertion;
import com.grupo5e.morapack.algorithm.alns.operators.construction.CapacityRebalancingOperator;
import com.grupo5e.morapack.algorithm.alns.operators.construction.IntelligentRepairOperator;
import com.grupo5e.morapack.algorithm.alns.operators.destruction.DestruccionAleatoria;
import com.grupo5e.morapack.algorithm.alns.operators.destruction.DestruccionSimilitud;
import com.grupo5e.morapack.algorithm.alns.operators.destruction.DestruccionPeorCosto;
import com.grupo5e.morapack.algorithm.alns.operators.destruction.DestruccionRutaCompleta;
import com.grupo5e.morapack.algorithm.alns.operators.destruction.ShawRemoval;
import com.grupo5e.morapack.algorithm.alns.operators.destruction.TimeOrientedRemoval;
import com.grupo5e.morapack.algorithm.validation.ValidadorRestricciones;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class ALNSSolver {
    
    // Parámetros del algoritmo
    private int iteracionesMaximas;
    private double temperaturaInicial;
    private double factorEnfriamiento;
    private int tamanoPoblacion;
    private double porcentajeDestruccion; // Porcentaje de paquetes a destruir (10-40%)
    
    // Contexto del problema
    private ContextoProblema contextoProblema;
    
    // Operadores ALNS
    private List<OperadorDestruccion> operadoresDestruccion;
    private List<OperadorConstruccion> operadoresConstruccion;
    private Map<String, Double> pesosOperadores; // Pesos adaptativos de operadores
    
    // Estado de la búsqueda
    private Solucion mejorSolucion;
    private Solucion solucionActual;
    private double temperaturaActual;
    private int iteracionActual;
    private List<Double> historialFitness;
    private List<Double> historialMejorFitness; // Historial del mejor fitness para convergencia
    private Random random;
    
    // Restart mechanism (literatura estándar ALNS)
    private int iteracionesSinMejora;
    private final int MAX_ITERACIONES_SIN_MEJORA = 50; // Aumentado para problemas complejos
    
    // Gestión de memoria (literatura estándar)
    private final int MAX_SOLUCIONES_MEMORIA = 1000; // Limitar memoria visitada
    private final int LIMPIAR_MEMORIA_CADA = 50; // Limpiar cada N iteraciones
    
    // Sistema de tracking de soluciones visitadas
    private Map<String, Solucion> solucionesVisitadas;
    
    // Validador de restricciones
    private ValidadorRestricciones validador;
    
    // Detector de paquetes problemáticos
    private ProblematicPackageDetector detectorProblematicos;
    
    public ALNSSolver(int iteracionesMaximas, double temperaturaInicial, double factorEnfriamiento) {
        this.iteracionesMaximas = iteracionesMaximas;
        this.temperaturaInicial = temperaturaInicial;
        this.temperaturaActual = temperaturaInicial;
        this.factorEnfriamiento = factorEnfriamiento;
        this.porcentajeDestruccion = 0.25; // 25% por defecto
        
        this.operadoresDestruccion = new ArrayList<>();
        this.operadoresConstruccion = new ArrayList<>();
        this.pesosOperadores = new HashMap<>();
        this.historialFitness = new ArrayList<>();
        this.historialMejorFitness = new ArrayList<>();
        this.solucionesVisitadas = new HashMap<>();
        this.random = new Random();
        this.detectorProblematicos = new ProblematicPackageDetector();
        this.iteracionesSinMejora = 0;
        
        inicializarOperadores();
    }
    
    private void inicializarOperadores() {
        ALNSConfig config = ALNSConfig.getInstance();
        
        // Operadores de destrucción avanzados
        operadoresDestruccion.add(new DestruccionAleatoria());
        operadoresDestruccion.add(new DestruccionSimilitud());
        operadoresDestruccion.add(new DestruccionPeorCosto());
        operadoresDestruccion.add(new DestruccionRutaCompleta());
        
        // Nuevos operadores avanzados basados en el ejemplo VRPTWFL
        if (config.isUseShawSimplifiedRemovalDeterministic()) {
            operadoresDestruccion.add(new ShawRemoval(false));
        }
        if (config.isUseShawSimplifiedRemovalRandom()) {
            operadoresDestruccion.add(new ShawRemoval(true));
        }
        if (config.isUseTimeOrientedRemovalDeterministic()) {
            operadoresDestruccion.add(new TimeOrientedRemoval(false, 
                config.getTimeOrientedJungwirthWeightStartTimeIinSolution()));
        }
        if (config.isUseTimeOrientedRemovalRandom()) {
            operadoresDestruccion.add(new TimeOrientedRemoval(true, 
                config.getTimeOrientedJungwirthWeightStartTimeIinSolution()));
        }
        
        // LITERATURA ALNS ESTÁNDAR: Solo operadores básicos y probados
        operadoresConstruccion.add(new ConstruccionEstrategia(ConstruccionEstrategia.TipoEstrategia.VORAZ));
        operadoresConstruccion.add(new ConstruccionEstrategia(ConstruccionEstrategia.TipoEstrategia.MENOR_COSTO));
        operadoresConstruccion.add(new ConstruccionEstrategia(ConstruccionEstrategia.TipoEstrategia.BALANCEADA));
        
        // REMOVER operadores custom que pueden causar problemas
        // operadoresConstruccion.add(new ConstruccionMultiDepot());  // CUSTOM - puede causar overfitting
        // operadoresConstruccion.add(new ConstruccionInteligente());  // CUSTOM - demasiado complejo
        
        // Nuevos operadores Regret-k
        if (config.isUseNRegret2()) {
            operadoresConstruccion.add(new RegretKInsertion(2));
        }
        if (config.isUseNRegret3()) {
            operadoresConstruccion.add(new RegretKInsertion(3));
        }
        if (config.isUseNRegret4()) {
            operadoresConstruccion.add(new RegretKInsertion(4));
        }
        if (config.isUseNRegret5()) {
            operadoresConstruccion.add(new RegretKInsertion(5));
        }
        if (config.isUseNRegret6()) {
            operadoresConstruccion.add(new RegretKInsertion(6));
        }
        
        // Nuevos operadores avanzados
        operadoresConstruccion.add(new CapacityRebalancingOperator());
        operadoresConstruccion.add(new IntelligentRepairOperator());
        
        // Inicializar pesos uniformes
        for (OperadorDestruccion op : operadoresDestruccion) {
            pesosOperadores.put(op.getNombre(), 1.0);
        }
        for (OperadorConstruccion op : operadoresConstruccion) {
            pesosOperadores.put(op.getNombre(), 1.0);
        }
    }
    
    public void configurarProblema(List<Paquete> paquetes, List<Aeropuerto> aeropuertos, 
                                   List<Vuelo> vuelos, Set<Continente> continentes) {
        this.contextoProblema = new ContextoProblema(paquetes, aeropuertos, vuelos, continentes);
        this.validador = new ValidadorRestricciones(aeropuertos, vuelos, continentes);
    }
    
    /**
     * Filtra operadores de construcción compatibles con el tipo de problema
     */
    private List<OperadorConstruccion> obtenerOperadoresCompatibles() {
        // CORRECCIÓN: Mejorar selección compatible para evitar estancamiento
        List<OperadorConstruccion> compatibles = new ArrayList<>();
        
        // Verificar si hay paquetes sin origen definido (problema multi-depot)
        boolean hayPaquetesSinOrigen = contextoProblema.getTodosPaquetes().stream()
                                                      .anyMatch(p -> p.getAeropuertoOrigen() == null);
        
        if (hayPaquetesSinOrigen) {
            // Incluir operadores que pueden manejar paquetes sin origen
            for (OperadorConstruccion op : operadoresConstruccion) {
                if (op instanceof ConstruccionMultiDepot || 
                    op instanceof RegretKInsertion || 
                    op instanceof ConstruccionInteligente) {
                    compatibles.add(op);
                }
            }
            
            // Si no hay operadores compatibles, usar al menos uno básico
            if (compatibles.isEmpty()) {
                compatibles.addAll(operadoresConstruccion);
            }
        } else {
            // Usar todos los operadores normalmente
            compatibles.addAll(operadoresConstruccion);
        }
        
        return compatibles;
    }
    
    public Solucion resolver() {
        // Generar solución inicial
        solucionActual = generarSolucionInicial();
        
        // CORRECCIÓN ALNS: NO asumir que la solución inicial es la mejor
        // La mejor solución se actualizará solo cuando encontremos una realmente mejor
        mejorSolucion = null; // Inicializar como null
        double mejorFitness = Double.MAX_VALUE; // Track del mejor fitness
        
        // Ciclo principal ALNS
        for (iteracionActual = 1; iteracionActual <= iteracionesMaximas; iteracionActual++) {
            // CORRECCIÓN: Crear copia para no modificar la solución actual directamente
            Solucion solucionTemporal = solucionActual.copiar();
            
            // Fase de destrucción (opera sobre la copia)
            OperadorDestruccion opDestruccion = seleccionarOperadorDestruccion();
            
            // CORRECCIÓN: Calcular tamaño de destrucción más conservador
            int paquetesRuteados = solucionTemporal.getCantidadPaquetes();
            int qMin = 1; // Mínimo 1 paquete
            int qMax = Math.max(1, (int)(paquetesRuteados * 0.15)); // Máximo 15% de paquetes ruteados (más conservador)
            
            // LITERATURA ALNS: Jitter más conservador para evitar destrucción excesiva
            double jitterMin = 0.02; // 2% - mucho más conservador
            double jitterMax = 0.10; // 10% - menos agresivo
            double porcentajeConJitter = jitterMin + random.nextDouble() * (jitterMax - jitterMin);
            
            int q = Math.max(qMin, Math.min(qMax, (int)(paquetesRuteados * porcentajeConJitter)));
            
            // LOG: Estado antes de destruir
            System.out.printf(
                "Iter %d | opD=%s | T=%.3f | q=%d | actualF=%.3f | mejorF=%s | ruteados=%d/%d%n",
                iteracionActual,
                opDestruccion.getNombre(),
                temperaturaActual,
                q,
                solucionActual.getFitness(),
                mejorSolucion != null ? String.format("%.3f", mejorSolucion.getFitness()) : "N/A",
                solucionActual.getCantidadPaquetes(),
                contextoProblema.getTodosPaquetes().size()
            );
            
            List<String> paquetesRemovidos = opDestruccion.destruir(solucionTemporal, q);
            
            // LOG: Estado tras destruir
            System.out.printf("   -> tras destruir: paquetes=%d, vuelos=%d, hash=%d, removidos=%d%n",
                solucionTemporal.getCantidadPaquetes(),
                solucionTemporal.getOcupacionVuelos().size(),
                solucionTemporal.hashCode(),
                paquetesRemovidos.size()
            );
            
            // Fase de construcción (reconstruye sobre la solución ya destruida)
            OperadorConstruccion opConstruccion = seleccionarOperadorConstruccion();
            Solucion nuevaSolucion = opConstruccion.construir(solucionTemporal, paquetesRemovidos, 
                                                             contextoProblema, validador);
            
            // LOG: Estado tras construir (sin fitness - se calcula después)
            System.out.printf("   -> tras reparar: paquetes=%d, vuelos=%d, hash=%d, viol=%d, factible=%s%n",
                nuevaSolucion.getCantidadPaquetes(),
                nuevaSolucion.getOcupacionVuelos().size(),
                nuevaSolucion.hashCode(),
                nuevaSolucion.getViolacionesRestricciones(),
                nuevaSolucion.isEsFactible()
            );
            
            // Validar la nueva solución (incluyendo validación temporal)
            validador.validarSolucionCompleta(nuevaSolucion);
            
            // LITERATURA ALNS: Calcular fitness UNA SOLA VEZ después de reparación
            int N = contextoProblema.getTodosPaquetes().size();
            nuevaSolucion.calcularFitness(N);
            
            double actualF = solucionActual.getFitness();
            double newF = nuevaSolucion.getFitness();
            double delta = newF - actualF;
            
            // Determinar score basado en el resultado
            double sigmaScore = determinarScoreOperadores(nuevaSolucion);
            
            // Criterio de aceptación (Simulated Annealing)
            boolean aceptada = aceptarSolucion(nuevaSolucion);
            double probabilidad = Math.exp(-Math.min(delta, temperaturaActual * 10) / temperaturaActual);
            System.out.printf("   -> actualF=%.3f | newF=%.3f | delta=%.3f | prob=%.6f | aceptada=%s%n",
                actualF, newF, delta, probabilidad, 
                aceptada ? "SI" : "NO"
            );
            
            // DEBUG: Mostrar información adicional cada 10 iteraciones
            if (iteracionActual % 10 == 0) {
                System.out.printf("   DEBUG: T=%.3f, delta=%.3f, prob=%.6f, mejor=%s%n",
                    temperaturaActual, delta, probabilidad, 
                    nuevaSolucion.esMejorQue(solucionActual) ? "SI" : "NO");
            }
            
            // DEBUG: Verificar si las soluciones son realmente diferentes
            if (Math.abs(delta) < 0.001) {
                System.out.printf("   WARNING: Delta muy pequeño (%.6f) - posible problema de operadores%n", delta);
            }
            
            if (aceptada) {
                solucionActual = nuevaSolucion;
                
                // CORRECCIÓN ALNS: Actualizar mejor solución solo si es realmente mejor
                double nuevaFitness = nuevaSolucion.getFitness();
                if (mejorSolucion == null || nuevaFitness < mejorFitness) {
                    mejorSolucion = nuevaSolucion.copiar();
                    mejorFitness = nuevaFitness;
                    iteracionesSinMejora = 0; // Reset contador (literatura estándar)
                    System.out.printf("   *** NUEVA MEJOR SOLUCIÓN: fitness=%.3f ***%n", mejorFitness);
                } else {
                    iteracionesSinMejora++;
                }
            } else {
                // Registrar fallos para paquetes no ruteados
                registrarFallosPaquetesNoRuteados(nuevaSolucion, opConstruccion.getNombre());
                iteracionesSinMejora++; // Incrementar también cuando se rechaza
            }
            
            // Actualizar scores de operadores
            actualizarScoresOperadores(opDestruccion, opConstruccion, sigmaScore);
            
            // Actualizar pesos de operadores
            actualizarPesosOperadores();
            
            // Enfriar temperatura
            enfriarTemperatura();
            
            // Restart mechanism (Ropke & Pisinger 2006)
            if (iteracionesSinMejora >= MAX_ITERACIONES_SIN_MEJORA) {
                ejecutarRestart();
            }
            
            // Gestión de memoria periódica (literatura estándar)
            if (iteracionActual % LIMPIAR_MEMORIA_CADA == 0) {
                limpiarMemoriaVisitada();
            }
            
            // Registrar progreso
            historialFitness.add(solucionActual.getFitness());
            historialMejorFitness.add(mejorSolucion != null ? mejorSolucion.getFitness() : solucionActual.getFitness());
            
            // Mostrar progreso cada 100 iteraciones (solo si está habilitado el logging verbose)
            ALNSConfig config = ALNSConfig.getInstance();
            if (iteracionActual % 100 == 0 && config.isEnableVerboseLogging()) {
                System.out.println("Iteración " + iteracionActual + 
                                 ", Fitness actual: " + solucionActual.getFitness() + 
                                 ", Mejor: " + (mejorSolucion != null ? mejorSolucion.getFitness() : "N/A") + 
                                 ", Temperatura: " + temperaturaActual);
                
                // Mostrar estadísticas de paquetes problemáticos
                detectorProblematicos.imprimirEstadisticasProblematicos();
            }
        }
        
        // CORRECCIÓN ALNS: Retornar la mejor solución encontrada, o la inicial si no se encontró ninguna mejor
        if (mejorSolucion != null) {
            return mejorSolucion;
        } else {
            System.out.println("WARNING: No se encontró ninguna solución mejor que la inicial");
            return solucionActual;
        }
    }
    
    /**
     * Ejecuta restart desde la mejor solución conocida (Ropke & Pisinger 2006)
     */
    private void ejecutarRestart() {
        // Restart desde mejor solución conocida
        solucionActual = mejorSolucion.copiar();
        
        // Reinicializar temperatura (literatura estándar)
        temperaturaActual = temperaturaInicial;
        
        // Limpiar memoria para permitir re-exploración
        solucionesVisitadas.clear();
        
        // CORRECCIÓN: Resetear estadísticas ALNS por segmento
        resetearEstadisticasOperadores();
        
        // Reset contador
        iteracionesSinMejora = 0;
        
        System.out.printf("Restart ejecutado en iteración %d (fitness: %.2f)%n", 
                         iteracionActual, mejorSolucion.getFitness());
    }
    
    /**
     * Resetea estadísticas de operadores (pesos, draws, etc.) para reiniciar aprendizaje
     */
    private void resetearEstadisticasOperadores() {
        // CORRECCIÓN: Resetear estadísticas internas (PI/draws) de operadores
        for (OperadorDestruccion op : operadoresDestruccion) {
            if (op instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) {
                com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator abstractOp = 
                    (com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) op;
                abstractOp.resetStats();
                pesosOperadores.put(op.getNombre(), 1.0); // Peso inicial
            }
        }
        
        for (OperadorConstruccion op : operadoresConstruccion) {
            if (op instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) {
                com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator abstractOp = 
                    (com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) op;
                abstractOp.resetStats();
                pesosOperadores.put(op.getNombre(), 1.0); // Peso inicial
            }
        }
    }
    
    /**
     * Limpia memoria de soluciones visitadas para evitar memory leaks (literatura estándar)
     */
    private void limpiarMemoriaVisitada() {
        if (solucionesVisitadas.size() > MAX_SOLUCIONES_MEMORIA) {
            // Mantener solo las mejores soluciones (50% de la memoria)
            List<Map.Entry<String, Solucion>> solucionesOrdenadas = 
                solucionesVisitadas.entrySet().stream()
                    .sorted(Map.Entry.<String, Solucion>comparingByValue(
                        (s1, s2) -> Double.compare(s1.getFitness(), s2.getFitness())))
                    .limit(MAX_SOLUCIONES_MEMORIA / 2)
                    .collect(java.util.stream.Collectors.toList());
            
            solucionesVisitadas.clear();
            solucionesOrdenadas.forEach(entry -> 
                solucionesVisitadas.put(entry.getKey(), entry.getValue()));
        }
    }
    
    private Solucion generarSolucionInicial() {
        List<String> todosPaquetes = contextoProblema.getTodosPaquetes().stream()
                                                   .map(Paquete::getId)
                                                   .toList();
        
        // LITERATURA ALNS: Solución inicial SIMPLE, no greedy extremo
        // Usar constructor básico que permita exploración
        OperadorConstruccion constructor = new ConstruccionEstrategia(ConstruccionEstrategia.TipoEstrategia.VORAZ);
        
        Solucion solucionInicial = constructor.construir(new Solucion(), todosPaquetes, contextoProblema, validador);
        
        // CORRECCIÓN: Usar función única para calcular fitness
        int N = contextoProblema.getTodosPaquetes().size();
        solucionInicial.calcularFitness(N);
        
        // DEBUG: Mostrar información de la solución inicial
        System.out.printf("SOLUCION INICIAL: paquetes=%d/%d, fitness=%.3f, factible=%s%n",
            solucionInicial.getCantidadPaquetes(), todosPaquetes.size(),
            solucionInicial.getFitness(), solucionInicial.isEsFactible());
        
        // LITERATURA ALNS: Asegurar que la solución inicial no sea demasiado optimizada
        // Si rutea menos del 50% de paquetes, es aceptable para ALNS
        double porcentajeRuteado = (double) solucionInicial.getCantidadPaquetes() / todosPaquetes.size();
        if (porcentajeRuteado < 0.3) {
            System.out.println("Solución inicial con baja cobertura (" + String.format("%.1f", porcentajeRuteado * 100) + 
                             "%) - ALNS puede mejorar significativamente");
        }
        
        return solucionInicial;
    }
    
    private OperadorDestruccion seleccionarOperadorDestruccion() {
        return seleccionarPorProbabilidad(operadoresDestruccion);
    }
    
    private OperadorConstruccion seleccionarOperadorConstruccion() {
        List<OperadorConstruccion> ops = obtenerOperadoresCompatibles();
        return seleccionarPorProbabilidad(ops);
    }
    
    private <T> T seleccionarPorProbabilidad(List<T> ops) {
        double sum = 0;
        for (T op : ops) {
            String nombre = (op instanceof OperadorDestruccion d) ? d.getNombre() : 
                          (op instanceof OperadorConstruccion c) ? c.getNombre() : "unknown";
            sum += pesosOperadores.getOrDefault(nombre, 1.0);
        }
        
        double r = random.nextDouble() * sum;
        double acc = 0;
        
        for (T op : ops) {
            String nombre = (op instanceof OperadorDestruccion d) ? d.getNombre() : 
                          (op instanceof OperadorConstruccion c) ? c.getNombre() : "unknown";
            double p = pesosOperadores.getOrDefault(nombre, 1.0);
            acc += p;
            if (acc >= r) return op;
        }
        
        return ops.get(ops.size() - 1);
    }
    
    private String firma(Solucion s) {
        StringBuilder sb = new StringBuilder(4096);
        s.getRutasPaquetes().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            sb.append(e.getKey()).append(':');
            for (SegmentoRuta seg : e.getValue().getSegmentos()) {
                sb.append(seg.getNumeroVuelo()).append('>').append(seg.getAeropuertoDestino()).append('|');
            }
            sb.append('#');
        });
        return sb.toString();
    }
    
    private boolean aceptarSolucion(Solucion nuevaSolucion) {
        ALNSConfig config = ALNSConfig.getInstance();
        
        // CORRECCIÓN: Usar firma determinista en lugar de hashCode
        String key = firma(nuevaSolucion);
        
        if (nuevaSolucion.esMejorQue(solucionActual)) {
            solucionesVisitadas.put(key, nuevaSolucion.copiar());
            return true;
        }
        
        if (solucionesVisitadas.containsKey(key)) {
            return false;
        }
        
        if (temperaturaActual <= config.getEpsilon()) {
            return false; // Temperatura muy baja, solo aceptar mejores
        }
        
        // Simulated Annealing: aceptar peores soluciones con probabilidad
        double delta = nuevaSolucion.getFitness() - solucionActual.getFitness();
        // Normalizar delta para evitar valores extremos
        double deltaNormalizado = Math.min(delta, temperaturaActual * 10);
        double probabilidad = Math.exp(-deltaNormalizado / temperaturaActual);
        
        if (random.nextDouble() < probabilidad) {
            solucionesVisitadas.put(key, nuevaSolucion.copiar());
            return true;
        }
        
        return false;
    }
    
    private void enfriarTemperatura() {
        // CORRECCIÓN: Usar el parámetro del constructor, no el hardcoded
        temperaturaActual *= factorEnfriamiento;
    }
    
    private void actualizarPesosOperadores() {
        ALNSConfig config = ALNSConfig.getInstance();
        
        if (iteracionActual % config.getUpdateInterval() == 0) {
            // Actualizar pesos de operadores de destrucción
            for (OperadorDestruccion op : operadoresDestruccion) {
                if (op instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) {
                    com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator abstractOp = 
                        (com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) op;
                    abstractOp.updateWeight(config.getReactionFactor());
                    
                    // CORRECCIÓN: Sincronizar mapa con pesos internos
                    pesosOperadores.put(op.getNombre(), abstractOp.getWeight());
                }
            }
            
            // Actualizar pesos de operadores de construcción
            for (OperadorConstruccion op : operadoresConstruccion) {
                if (op instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) {
                    com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator abstractOp = 
                        (com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) op;
                    abstractOp.updateWeight(config.getReactionFactor());
                    
                    // CORRECCIÓN: Sincronizar mapa con pesos internos
                    pesosOperadores.put(op.getNombre(), abstractOp.getWeight());
                }
            }
            
            // Normalizar probabilidades
            normalizarProbabilidades();
            
            // CORRECCIÓN: Resetear stats tras cada actualización de pesos por segmento
            for (OperadorDestruccion op : operadoresDestruccion) {
                if (op instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator a) {
                    a.resetStats();
                }
            }
            for (OperadorConstruccion op : operadoresConstruccion) {
                if (op instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator a) {
                    a.resetStats();
                }
            }
            
            if (config.isEnableVerboseLogging()) {
                System.out.println("Pesos actualizados en iteración " + iteracionActual);
                imprimirEstadisticasOperadores();
            }
        }
    }
    
    /**
     * Normaliza las probabilidades de selección de operadores
     */
    private void normalizarProbabilidades() {
        ALNSConfig config = ALNSConfig.getInstance();
        
        // Calcular suma total de pesos para destrucción
        double sumaPesosDestruccion = operadoresDestruccion.stream()
            .filter(op -> op instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator)
            .mapToDouble(op -> ((com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) op).getWeight())
            .sum();
        
        // Calcular suma total de pesos para construcción
        double sumaPesosConstruccion = operadoresConstruccion.stream()
            .filter(op -> op instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator)
            .mapToDouble(op -> ((com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) op).getWeight())
            .sum();
        
        // Actualizar probabilidades de operadores de destrucción
        for (OperadorDestruccion op : operadoresDestruccion) {
            if (op instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) {
                com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator abstractOp = 
                    (com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) op;
                abstractOp.updateProbability(sumaPesosDestruccion, config.getMinOpProb());
            }
        }
        
        // Actualizar probabilidades de operadores de construcción
        for (OperadorConstruccion op : operadoresConstruccion) {
            if (op instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) {
                com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator abstractOp = 
                    (com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) op;
                abstractOp.updateProbability(sumaPesosConstruccion, config.getMinOpProb());
            }
        }
    }
    
    /**
     * Imprime estadísticas de los operadores (solo si está habilitado el logging verbose)
     */
    private void imprimirEstadisticasOperadores() {
        ALNSConfig config = ALNSConfig.getInstance();
        if (!config.isEnableVerboseLogging()) {
            return;
        }
        
        System.out.println("=== ESTADÍSTICAS DE OPERADORES ===");
        
        System.out.println("Operadores de Destrucción:");
        for (OperadorDestruccion op : operadoresDestruccion) {
            if (op instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) {
                com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator abstractOp = 
                    (com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) op;
                System.out.println("  " + abstractOp.getStatsInfo());
            }
        }
        
        System.out.println("Operadores de Construcción:");
        for (OperadorConstruccion op : operadoresConstruccion) {
            if (op instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) {
                com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator abstractOp = 
                    (com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) op;
                System.out.println("  " + abstractOp.getStatsInfo());
            }
        }
        System.out.println("==================================");
    }
    
    // Métodos para obtener estadísticas
    public double getMejorFitness() {
        return mejorSolucion != null ? mejorSolucion.getFitness() : Double.MAX_VALUE;
    }
    
    public List<Double> getHistorialFitness() {
        return new ArrayList<>(historialFitness);
    }
    
    /**
     * Obtiene el historial del mejor fitness para análisis de convergencia
     */
    public List<Double> getHistorialMejorFitness() {
        return new ArrayList<>(historialMejorFitness);
    }
    
    public void imprimirEstadisticas() {
        ALNSConfig config = ALNSConfig.getInstance();
        if (!config.isEnableVerboseLogging()) {
            return;
        }
        
        System.out.println("=== ESTADÍSTICAS FINALES ALNS ===");
        System.out.println("Iteraciones ejecutadas: " + iteracionActual);
        System.out.println("Mejor fitness: " + getMejorFitness());
        System.out.println("Paquetes resueltos: " + mejorSolucion.getCantidadPaquetes());
        System.out.println("Costo total: " + mejorSolucion.getCostoTotal());
        System.out.println("Tiempo máximo: " + mejorSolucion.getTiempoTotalHoras() + " horas");
        System.out.println("Solución factible: " + mejorSolucion.isEsFactible());
        System.out.println("Temperatura final: " + temperaturaActual);
    }
    
    /**
     * Determina el score (sigma) para los operadores basado en el resultado
     */
    private double determinarScoreOperadores(Solucion nuevaSolucion) {
        ALNSConfig config = ALNSConfig.getInstance();
        
        // Mejor solución global
        if (nuevaSolucion.esMejorQue(mejorSolucion)) {
            return config.getSigma1();
        }
        
        // Mejor solución actual
        if (nuevaSolucion.esMejorQue(solucionActual)) {
            return config.getSigma2();
        }
        
        // Solución aceptada por Simulated Annealing
        return config.getSigma3();
    }
    
    
    /**
     * Registra fallos para paquetes no ruteados
     */
    private void registrarFallosPaquetesNoRuteados(Solucion solucion, String operadorUsado) {
        int totalPaquetes = contextoProblema.getTodosPaquetes().size();
        int paquetesRuteados = solucion.getCantidadPaquetes();
        int paquetesNoRuteados = totalPaquetes - paquetesRuteados;
        
        if (paquetesNoRuteados > 0) {
            // Identificar paquetes no ruteados
            Set<String> paquetesRuteadosIds = solucion.getPaquetesIds();
            List<String> paquetesNoRuteadosIds = contextoProblema.getTodosPaquetes().stream()
                .map(Paquete::getId)
                .filter(id -> !paquetesRuteadosIds.contains(id))
                .toList();
            
            // Registrar fallo para cada paquete no ruteado
            for (String paqueteId : paquetesNoRuteadosIds) {
                String motivo = determinarMotivoFallo(paqueteId, solucion);
                detectorProblematicos.registrarFallo(paqueteId, motivo, operadorUsado, solucion.getFitness());
            }
        }
    }
    
    /**
     * Determina el motivo del fallo para un paquete específico
     */
    private String determinarMotivoFallo(String paqueteId, Solucion solucion) {
        Paquete paquete = contextoProblema.getTodosPaquetes().stream()
            .filter(p -> p.getId().equals(paqueteId))
            .findFirst()
            .orElse(null);
        
        if (paquete == null) {
            return "paquete_no_encontrado";
        }
        
        // Verificar disponibilidad de vuelos
        List<Vuelo> vuelosDisponibles = contextoProblema.getVuelosDesde(paquete.getAeropuertoOrigen())
            .stream()
            .filter(v -> v.estaOperativo())
            .filter(v -> v.getPaquetesReservados() < v.getCapacidadMaxima())
            .toList();
        
        if (vuelosDisponibles.isEmpty()) {
            return "sin_vuelos_disponibles";
        }
        
        // Verificar si hay vuelos directos
        List<Vuelo> vuelosDirectos = vuelosDisponibles.stream()
            .filter(v -> v.getAeropuertoDestino().equals(paquete.getAeropuertoDestino()))
            .toList();
        
        if (vuelosDirectos.isEmpty()) {
            return "sin_vuelos_directos";
        }
        
        // Verificar capacidad
        List<Vuelo> vuelosConCapacidad = vuelosDirectos.stream()
            .filter(v -> v.getPaquetesReservados() < v.getCapacidadMaxima())
            .toList();
        
        if (vuelosConCapacidad.isEmpty()) {
            return "capacidad_saturada";
        }
        
        return "motivo_desconocido";
    }
    
    /**
     * Actualiza los scores de los operadores usados
     */
    private void actualizarScoresOperadores(OperadorDestruccion opDestruccion, 
                                           OperadorConstruccion opConstruccion, 
                                           double sigmaScore) {
        // Actualizar score del operador de destrucción
        if (opDestruccion instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) {
            com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator abstractOp = 
                (com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) opDestruccion;
            abstractOp.addToPI(sigmaScore);
            abstractOp.incrementDraws();
        }
        
        // Actualizar score del operador de construcción
        if (opConstruccion instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) {
            com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator abstractOp = 
                (com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) opConstruccion;
            abstractOp.addToPI(sigmaScore);
            abstractOp.incrementDraws();
        }
    }
}
