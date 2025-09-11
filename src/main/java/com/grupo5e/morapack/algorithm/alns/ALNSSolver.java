package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorConstruccion;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorDestruccion;
import com.grupo5e.morapack.algorithm.alns.operators.construction.ConstruccionInteligente;
import com.grupo5e.morapack.algorithm.alns.operators.construction.ConstruccionEstrategia;
import com.grupo5e.morapack.algorithm.alns.operators.destruction.DestruccionAleatoria;
import com.grupo5e.morapack.algorithm.alns.operators.destruction.DestruccionSimilitud;
import com.grupo5e.morapack.algorithm.alns.operators.destruction.DestruccionPeorCosto;
import com.grupo5e.morapack.algorithm.alns.operators.destruction.DestruccionRutaCompleta;
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
    
    // Validador de restricciones
    private ValidadorRestricciones validador;
    
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
        this.random = new Random();
        
        inicializarOperadores();
    }
    
    private void inicializarOperadores() {
        // Operadores de destrucción
        operadoresDestruccion.add(new DestruccionAleatoria());
        operadoresDestruccion.add(new DestruccionSimilitud());
        operadoresDestruccion.add(new DestruccionPeorCosto());
        operadoresDestruccion.add(new DestruccionRutaCompleta());
        
        // Operadores de construcción INTELIGENTES que respetan restricciones
        operadoresConstruccion.add(new ConstruccionInteligente());  // Constructor principal
        operadoresConstruccion.add(new ConstruccionInteligente());  // Duplicado para mayor probabilidad
        operadoresConstruccion.add(new ConstruccionEstrategia(ConstruccionEstrategia.TipoEstrategia.VORAZ));        // Fallback para casos simples
        operadoresConstruccion.add(new ConstruccionEstrategia(ConstruccionEstrategia.TipoEstrategia.MENOR_COSTO));   // Fallback alternativo
        
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
        System.out.println("Iniciando ALNS con " + contextoProblema.getTodosPaquetes().size() + " paquetes...");
        
        // Generar solución inicial
        solucionActual = generarSolucionInicial();
        mejorSolucion = solucionActual.copiar();
        
        System.out.println("Solución inicial: " + solucionActual);
        
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
            
            // Criterio de aceptación (Simulated Annealing)
            if (aceptarSolucion(nuevaSolucion)) {
                solucionActual = nuevaSolucion;
                
                // Actualizar mejor solución
                if (nuevaSolucion.esMejorQue(mejorSolucion)) {
                    mejorSolucion = nuevaSolucion.copiar();
                    System.out.println("Nueva mejor solución en iteración " + iteracionActual + 
                                     ": " + mejorSolucion.getFitness());
                }
            }
            
            // Actualizar pesos de operadores (cada 50 iteraciones)
            if (iteracionActual % 50 == 0) {
                actualizarPesosOperadores();
            }
            
            // Enfriar temperatura
            enfriarTemperatura();
            
            // Registrar progreso
            historialFitness.add(solucionActual.getFitness());
            
            // Mostrar progreso cada 100 iteraciones
            if (iteracionActual % 100 == 0) {
                System.out.println("Iteración " + iteracionActual + 
                                 ", Fitness actual: " + solucionActual.getFitness() + 
                                 ", Mejor: " + mejorSolucion.getFitness() + 
                                 ", Temperatura: " + temperaturaActual);
            }
        }
        
        System.out.println("ALNS completado. Mejor solución: " + mejorSolucion);
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
        if (nuevaSolucion.esMejorQue(solucionActual)) {
            return true; // Siempre aceptar mejores soluciones
        }
        
        if (temperaturaActual <= 0.001) {
            return false; // Temperatura muy baja, solo aceptar mejores
        }
        
        // Simulated Annealing: aceptar peores soluciones con probabilidad
        double delta = nuevaSolucion.getFitness() - solucionActual.getFitness();
        double probabilidad = Math.exp(-delta / temperaturaActual);
        return random.nextDouble() < probabilidad;
    }
    
    private void enfriarTemperatura() {
        temperaturaActual *= factorEnfriamiento;
    }
    
    private void actualizarPesosOperadores() {
        // Lógica simplificada: mantener pesos estables por ahora
        // En una implementación completa, se actualizarían según el rendimiento
        System.out.println("Actualizando pesos de operadores...");
    }
    
    // Métodos para obtener estadísticas
    public double getMejorFitness() {
        return mejorSolucion != null ? mejorSolucion.getFitness() : Double.MAX_VALUE;
    }
    
    public List<Double> getHistorialFitness() {
        return new ArrayList<>(historialFitness);
    }
    
    public void imprimirEstadisticas() {
        System.out.println("=== ESTADÍSTICAS FINALES ALNS ===");
        System.out.println("Iteraciones ejecutadas: " + iteracionActual);
        System.out.println("Mejor fitness: " + getMejorFitness());
        System.out.println("Paquetes resueltos: " + mejorSolucion.getCantidadPaquetes());
        System.out.println("Costo total: " + mejorSolucion.getCostoTotal());
        System.out.println("Tiempo máximo: " + mejorSolucion.getTiempoTotalHoras() + " horas");
        System.out.println("Solución factible: " + mejorSolucion.isEsFactible());
        System.out.println("Temperatura final: " + temperaturaActual);
    }
}
