package com.grupo5e.morapack.experimentos;

import java.util.HashMap;
import java.util.Map;

/**
 * Almacena los resultados de una ejecución de algoritmo
 */
public class ResultadoAlgoritmo {
    private String nombreAlgoritmo;
    private long tiempoInicio;
    private long tiempoFin;

    // Métricas principales (usando el mismo fitness que tu ALNSSolver)
    private int fitnessFinal;
    private int paquetesAsignados;
    private int totalPaquetes;
    private double tasaEntregasATiempo;
    private double tiempoPromedioEntrega;

    // Métricas de desempeño
    private int iteracionesTotales;
    private int mejorasEncontradas;

    public ResultadoAlgoritmo(String nombreAlgoritmo) {
        this.nombreAlgoritmo = nombreAlgoritmo;
        this.tiempoInicio = System.currentTimeMillis();
    }

    // Getters y Setters
    public String getNombreAlgoritmo() { return nombreAlgoritmo; }

    public int getFitnessFinal() { return fitnessFinal; }
    public void setFitnessFinal(int fitnessFinal) { this.fitnessFinal = fitnessFinal; }

    public int getPaquetesAsignados() { return paquetesAsignados; }
    public void setPaquetesAsignados(int paquetesAsignados) {
        this.paquetesAsignados = paquetesAsignados;
    }

    public int getTotalPaquetes() { return totalPaquetes; }
    public void setTotalPaquetes(int totalPaquetes) {
        this.totalPaquetes = totalPaquetes;
    }

    public double getTasaEntregasATiempo() { return tasaEntregasATiempo; }
    public void setTasaEntregasATiempo(double tasaEntregasATiempo) {
        this.tasaEntregasATiempo = tasaEntregasATiempo;
    }

    public double getTiempoPromedioEntrega() { return tiempoPromedioEntrega; }
    public void setTiempoPromedioEntrega(double tiempoPromedioEntrega) {
        this.tiempoPromedioEntrega = tiempoPromedioEntrega;
    }

    public int getIteracionesTotales() { return iteracionesTotales; }
    public void setIteracionesTotales(int iteracionesTotales) {
        this.iteracionesTotales = iteracionesTotales;
    }

    public int getMejorasEncontradas() { return mejorasEncontradas; }
    public void setMejorasEncontradas(int mejorasEncontradas) {
        this.mejorasEncontradas = mejorasEncontradas;
    }

    public long getTiempoEjecucionMs() {
        return tiempoFin - tiempoInicio;
    }

    public void finalizarEjecucion() {
        this.tiempoFin = System.currentTimeMillis();
    }

    public double getPorcentajePaquetesAsignados() {
        return totalPaquetes > 0 ? (paquetesAsignados * 100.0) / totalPaquetes : 0.0;
    }

    @Override
    public String toString() {
        return String.format(
                "%s | Fitness: %,d | Paquetes: %d/%d (%.1f%%) | Tiempo: %.1fs",
                nombreAlgoritmo, fitnessFinal, paquetesAsignados, totalPaquetes,
                getPorcentajePaquetesAsignados(), getTiempoEjecucionMs() / 1000.0
        );
    }
}