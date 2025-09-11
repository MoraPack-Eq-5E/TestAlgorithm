package com.grupo5e.morapack.core.model;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class Solucion {
    private Map<String, Ruta> rutasPaquetes; // paqueteId -> Ruta
    private double costoTotal;
    private double tiempoTotalHoras;
    private int violacionesRestricciones;
    private boolean esFactible;
    private double fitness; // Función objetivo
    private Map<String, Integer> ocupacionVuelos; // numeroVuelo -> cantidad paquetes
    private Map<String, Integer> ocupacionAlmacenes; // codigoIATA -> cantidad paquetes
    
    public Solucion() {
        this.rutasPaquetes = new HashMap<>();
        this.ocupacionVuelos = new HashMap<>();
        this.ocupacionAlmacenes = new HashMap<>();
        this.costoTotal = 0.0;
        this.tiempoTotalHoras = 0.0;
        this.violacionesRestricciones = 0;
        this.esFactible = true;
        this.fitness = Double.MAX_VALUE;
    }
    
    public Solucion(Map<String, Ruta> rutasPaquetes) {
        this();
        this.rutasPaquetes = new HashMap<>(rutasPaquetes);
        recalcularMetricas();
    }
    
    public void agregarRuta(String paqueteId, Ruta ruta) {
        rutasPaquetes.put(paqueteId, ruta);
        actualizarOcupaciones(ruta, 1);
        recalcularMetricas();
    }
    
    public void removerRuta(String paqueteId) {
        Ruta ruta = rutasPaquetes.remove(paqueteId);
        if (ruta != null) {
            actualizarOcupaciones(ruta, -1);
            recalcularMetricas();
        }
    }
    
    public void reemplazarRuta(String paqueteId, Ruta nuevaRuta) {
        Ruta rutaAnterior = rutasPaquetes.get(paqueteId);
        if (rutaAnterior != null) {
            actualizarOcupaciones(rutaAnterior, -1);
        }
        rutasPaquetes.put(paqueteId, nuevaRuta);
        actualizarOcupaciones(nuevaRuta, 1);
        recalcularMetricas();
    }
    
    private void actualizarOcupaciones(Ruta ruta, int incremento) {
        for (SegmentoRuta segmento : ruta.getSegmentos()) {
            // Actualizar ocupación de vuelos
            String vuelo = segmento.getNumeroVuelo();
            ocupacionVuelos.put(vuelo, ocupacionVuelos.getOrDefault(vuelo, 0) + incremento);
            
            // Actualizar ocupación de almacenes (aeropuertos intermedios)
            String aeropuertoDestino = segmento.getAeropuertoDestino();
            ocupacionAlmacenes.put(aeropuertoDestino, 
                ocupacionAlmacenes.getOrDefault(aeropuertoDestino, 0) + incremento);
        }
    }
    
    public void recalcularMetricas() {
        costoTotal = rutasPaquetes.values().stream()
                .mapToDouble(Ruta::getCostoTotal)
                .sum();
        
        tiempoTotalHoras = rutasPaquetes.values().stream()
                .mapToDouble(Ruta::getTiempoTotalHoras)
                .max()
                .orElse(0.0); // Tiempo máximo (makespan)
        
        calcularFitness();
    }
    
    private void calcularFitness() {
        // Función objetivo: minimizar costo total + penalización por violaciones
        double penalizacionViolaciones = violacionesRestricciones * 1000.0;
        this.fitness = costoTotal + tiempoTotalHoras + penalizacionViolaciones;
    }
    
    public boolean esVacia() {
        return rutasPaquetes.isEmpty();
    }
    
    public int getCantidadPaquetes() {
        return rutasPaquetes.size();
    }
    
    public Set<String> getPaquetesIds() {
        return new HashSet<>(rutasPaquetes.keySet());
    }
    
    public List<String> getPaquetesAleatorios(int cantidad) {
        List<String> paquetes = new ArrayList<>(rutasPaquetes.keySet());
        Collections.shuffle(paquetes);
        return paquetes.subList(0, Math.min(cantidad, paquetes.size()));
    }
    
    public Solucion copiar() {
        Solucion copia = new Solucion();
        for (Map.Entry<String, Ruta> entry : this.rutasPaquetes.entrySet()) {
            copia.rutasPaquetes.put(entry.getKey(), entry.getValue().copiar());
        }
        copia.ocupacionVuelos = new HashMap<>(this.ocupacionVuelos);
        copia.ocupacionAlmacenes = new HashMap<>(this.ocupacionAlmacenes);
        copia.recalcularMetricas();
        return copia;
    }
    
    public boolean esMejorQue(Solucion otra) {
        if (this.esFactible && !otra.esFactible) return true;
        if (!this.esFactible && otra.esFactible) return false;
        return this.fitness < otra.fitness;
    }
    
    public double calcularPorcentajeUtilizacionVuelos() {
        if (ocupacionVuelos.isEmpty()) return 0.0;
        return ocupacionVuelos.values().stream()
                .mapToDouble(Integer::doubleValue)
                .average()
                .orElse(0.0);
    }
    
    @Override
    public String toString() {
        return String.format("Solucion[Paquetes: %d, Costo: %.2f, Tiempo: %.2f hrs, Factible: %s, Fitness: %.2f]", 
                           rutasPaquetes.size(), costoTotal, tiempoTotalHoras, esFactible, fitness);
    }
}
