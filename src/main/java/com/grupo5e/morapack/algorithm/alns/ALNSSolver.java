package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorConstruccion;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorDestruccion;
import com.grupo5e.morapack.algorithm.alns.operators.construction.ConstruccionInteligente;
import com.grupo5e.morapack.algorithm.alns.operators.construction.ConstruccionEstrategia;
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
    private Random random;
    
    // Sistema de tracking de soluciones visitadas
    private Map<Integer, Solucion> solucionesVisitadas;
    
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
        this.solucionesVisitadas = new HashMap<>();
        this.random = new Random();
        this.detectorProblematicos = new ProblematicPackageDetector();
        
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
        
        // Operadores de construcción avanzados
        operadoresConstruccion.add(new ConstruccionInteligente());  // Constructor principal
        operadoresConstruccion.add(new ConstruccionEstrategia(ConstruccionEstrategia.TipoEstrategia.VORAZ));
        operadoresConstruccion.add(new ConstruccionEstrategia(ConstruccionEstrategia.TipoEstrategia.MENOR_COSTO));
        
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
    
    public Solucion resolver() {
        // Generar solución inicial
        solucionActual = generarSolucionInicial();
        mejorSolucion = solucionActual.copiar();
        
        // Ciclo principal ALNS
        for (iteracionActual = 1; iteracionActual <= iteracionesMaximas; iteracionActual++) {
            // CORRECCIÓN: Crear copia para no modificar la solución actual directamente
            Solucion solucionTemporal = solucionActual.copiar();
            
            // Fase de destrucción (opera sobre la copia)
            OperadorDestruccion opDestruccion = seleccionarOperadorDestruccion();
            List<String> paquetesRemovidos = opDestruccion.destruir(solucionTemporal, 
                                                (int)(contextoProblema.getTodosPaquetes().size() * porcentajeDestruccion));
            
            // Fase de construcción (reconstruye la solución completa)
            OperadorConstruccion opConstruccion = seleccionarOperadorConstruccion();
            Solucion nuevaSolucion = opConstruccion.construir(solucionTemporal, paquetesRemovidos, 
                                                             contextoProblema, validador);
            
            // Validar la nueva solución
            validador.validarSolucion(nuevaSolucion);
            
            // Recalcular fitness con penalización por paquetes no ruteados
            recalcularFitnessConContexto(nuevaSolucion);
            
            // Determinar score basado en el resultado
            double sigmaScore = determinarScoreOperadores(nuevaSolucion);
            
            // Criterio de aceptación (Simulated Annealing)
            if (aceptarSolucion(nuevaSolucion)) {
                solucionActual = nuevaSolucion;
                
                // Actualizar mejor solución
                if (nuevaSolucion.esMejorQue(mejorSolucion)) {
                    mejorSolucion = nuevaSolucion.copiar();
                }
            } else {
                // Registrar fallos para paquetes no ruteados
                registrarFallosPaquetesNoRuteados(nuevaSolucion, opConstruccion.getNombre());
            }
            
            // Actualizar scores de operadores
            actualizarScoresOperadores(opDestruccion, opConstruccion, sigmaScore);
            
            // Actualizar pesos de operadores
            actualizarPesosOperadores();
            
            // Enfriar temperatura
            enfriarTemperatura();
            
            // Registrar progreso
            historialFitness.add(solucionActual.getFitness());
            
            // Mostrar progreso cada 100 iteraciones (solo si está habilitado el logging verbose)
            ALNSConfig config = ALNSConfig.getInstance();
            if (iteracionActual % 100 == 0 && config.isEnableVerboseLogging()) {
                System.out.println("Iteración " + iteracionActual + 
                                 ", Fitness actual: " + solucionActual.getFitness() + 
                                 ", Mejor: " + mejorSolucion.getFitness() + 
                                 ", Temperatura: " + temperaturaActual);
                
                // Mostrar estadísticas de paquetes problemáticos
                detectorProblematicos.imprimirEstadisticasProblematicos();
            }
        }
        
        return mejorSolucion;
    }
    
    private Solucion generarSolucionInicial() {
        // Generación voraz inicial: asignar ruta más corta a cada paquete
        ConstruccionEstrategia constructor = new ConstruccionEstrategia(ConstruccionEstrategia.TipoEstrategia.VORAZ);
        
        List<String> todosPaquetes = contextoProblema.getTodosPaquetes().stream()
                                                   .map(Paquete::getId)
                                                   .toList();
        
        return constructor.construir(new Solucion(), todosPaquetes, contextoProblema, validador);
    }
    
    private OperadorDestruccion seleccionarOperadorDestruccion() {
        return seleccionarOperadorPorPeso(operadoresDestruccion.stream()
                                                .map(op -> (Object) op)
                                                .toList());
    }
    
    private OperadorConstruccion seleccionarOperadorConstruccion() {
        return seleccionarOperadorPorPeso(operadoresConstruccion.stream()
                                                .map(op -> (Object) op)
                                                .toList());
    }
    
    @SuppressWarnings("unchecked")
    private <T> T seleccionarOperadorPorPeso(List<Object> operadores) {
        double pesoTotal = operadores.stream()
                .mapToDouble(op -> {
                    if (op instanceof OperadorDestruccion) {
                        return pesosOperadores.get(((OperadorDestruccion) op).getNombre());
                    } else if (op instanceof OperadorConstruccion) {
                        return pesosOperadores.get(((OperadorConstruccion) op).getNombre());
                    }
                    return 0.0;
                }).sum();
        
        double valorAleatorio = random.nextDouble() * pesoTotal;
        double acumulado = 0.0;
        
        for (Object op : operadores) {
            String nombre = "";
            if (op instanceof OperadorDestruccion) {
                nombre = ((OperadorDestruccion) op).getNombre();
            } else if (op instanceof OperadorConstruccion) {
                nombre = ((OperadorConstruccion) op).getNombre();
            }
            
            acumulado += pesosOperadores.get(nombre);
            if (acumulado >= valorAleatorio) {
                return (T) op;
            }
        }
        
        // Fallback: retornar último operador
        return (T) operadores.get(operadores.size() - 1);
    }
    
    private boolean aceptarSolucion(Solucion nuevaSolucion) {
        ALNSConfig config = ALNSConfig.getInstance();
        
        // Verificar si la solución ya fue visitada (solo para soluciones peores)
        int hashSolucion = nuevaSolucion.hashCode();
        
        if (nuevaSolucion.esMejorQue(solucionActual)) {
            // Siempre aceptar mejores soluciones, incluso si ya fueron visitadas
            solucionesVisitadas.put(hashSolucion, nuevaSolucion.copiar());
            return true;
        }
        
        // Para soluciones peores, verificar si ya fueron visitadas
        if (solucionesVisitadas.containsKey(hashSolucion)) {
            return false; // No aceptar soluciones peores ya visitadas
        }
        
        if (temperaturaActual <= config.getEpsilon()) {
            return false; // Temperatura muy baja, solo aceptar mejores
        }
        
        // Simulated Annealing: aceptar peores soluciones con probabilidad
        double delta = nuevaSolucion.getFitness() - solucionActual.getFitness();
        double probabilidad = Math.exp(-delta / temperaturaActual);
        
        if (random.nextDouble() < probabilidad) {
            // Agregar a soluciones visitadas
            solucionesVisitadas.put(hashSolucion, nuevaSolucion.copiar());
            return true;
        }
        
        return false;
    }
    
    private void enfriarTemperatura() {
        ALNSConfig config = ALNSConfig.getInstance();
        temperaturaActual *= config.getCoolingRate();
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
                }
            }
            
            // Actualizar pesos de operadores de construcción
            for (OperadorConstruccion op : operadoresConstruccion) {
                if (op instanceof com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) {
                    com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator abstractOp = 
                        (com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator) op;
                    abstractOp.updateWeight(config.getReactionFactor());
                }
            }
            
            // Normalizar probabilidades
            normalizarProbabilidades();
            
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
     * Recalcula el fitness de una solución considerando el contexto del problema
     */
    private void recalcularFitnessConContexto(Solucion solucion) {
        int totalPaquetes = contextoProblema.getTodosPaquetes().size();
        double penalizacionPaquetesNoRuteados = solucion.calcularPenalizacionPaquetesNoRuteados(totalPaquetes);
        
        // Recalcular fitness con la penalización correcta
        double fitnessBase = solucion.getCostoTotal() + solucion.getTiempoTotalHoras();
        double penalizacionViolaciones = solucion.getViolacionesRestricciones() * 50.0 + 100.0;
        
        solucion.setFitness(fitnessBase + penalizacionViolaciones + penalizacionPaquetesNoRuteados);
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
