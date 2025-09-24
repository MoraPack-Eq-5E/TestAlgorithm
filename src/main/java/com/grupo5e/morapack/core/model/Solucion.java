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
    private double funcionObjetivo; // Función objetivo del negocio
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
        this.funcionObjetivo = Double.MAX_VALUE;
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
        
        calcularFuncionObjetivo();
    }
    
    private void calcularFuncionObjetivo() {
        // Función objetivo del negocio: minimizar tiempo total + costos + penalizaciones
        double penalizacionViolaciones = calcularPenalizacionGradual();
        double penalizacionPaquetesNoRuteados = calcularPenalizacionPaquetesNoRuteados();
        
        this.funcionObjetivo = costoTotal + tiempoTotalHoras + penalizacionViolaciones + penalizacionPaquetesNoRuteados;
    }
    
    /**
     * Calcula penalización gradual basada en el tipo y severidad de violaciones
     */
    private double calcularPenalizacionGradual() {
        if (violacionesRestricciones == 0) {
            return 0.0;
        }
        
        // Penalización base más suave
        double penalizacionBase = 100.0;
        
        // Penalización incremental (no exponencial)
        double penalizacionIncremental = violacionesRestricciones * 50.0;
        
        return penalizacionBase + penalizacionIncremental;
    }
    
    /**
     * Calcula penalización por paquetes no ruteados de manera gradual
     */
    private double calcularPenalizacionPaquetesNoRuteados() {
        // Este método será llamado desde el contexto del problema
        // para obtener el número total de paquetes
        return 0.0; // Se calculará en el contexto
    }
    
    /**
     * Calcula penalización por paquetes no ruteados con información del contexto
     */
    public double calcularPenalizacionPaquetesNoRuteados(int totalPaquetes) {
        int paquetesRuteados = getCantidadPaquetes();
        int paquetesNoRuteados = totalPaquetes - paquetesRuteados;
        
        if (paquetesNoRuteados == 0) {
            return 0.0;
        }
        
        // Penalización gradual: 50 por el primer paquete, 100 por el segundo, etc.
        double penalizacion = 0.0;
        for (int i = 1; i <= paquetesNoRuteados; i++) {
            penalizacion += 50.0 * i; // 50, 100, 150, 200, ...
        }
        
        return penalizacion;
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
        return this.funcionObjetivo < otra.funcionObjetivo;
    }
    
    public double calcularPorcentajeUtilizacionVuelos() {
        if (ocupacionVuelos.isEmpty()) return 0.0;
        return ocupacionVuelos.values().stream()
                .mapToDouble(Integer::doubleValue)
                .average()
                .orElse(0.0);
    }
    
    // Métodos de acceso
    public double getFuncionObjetivo() {
        return funcionObjetivo;
    }
    
    // Método legacy para compatibilidad
    public double getFitness() {
        return funcionObjetivo;
    }
    
    @Override
    public String toString() {
        return String.format("Solucion[Paquetes: %d, Costo: %.2f, Tiempo: %.2f hrs, Factible: %s, FObjetivo: %.2f]", 
                           rutasPaquetes.size(), costoTotal, tiempoTotalHoras, esFactible, funcionObjetivo);
    }
}
