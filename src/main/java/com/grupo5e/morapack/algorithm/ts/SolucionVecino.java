package com.grupo5e.morapack.algorithm.ts;

import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.core.model.Vuelo;

import java.util.ArrayList;
import java.util.HashMap;

public class SolucionVecino {
    public HashMap<Paquete, ArrayList<Vuelo>> solucion;
    public int peso;
    public TSMove movimiento;
    public double deltaEvaluacion; // Cambio en la evaluación respecto a la solución actual
    public double calidad; // Métrica adicional de calidad

    /**
     * Constructor básico.
     */
    public SolucionVecino(HashMap<Paquete, ArrayList<Vuelo>> solucion, int peso, TSMove movimiento) {
        this.solucion = new HashMap<>(solucion);
        this.peso = peso;
        this.movimiento = movimiento;
        this.deltaEvaluacion = 0.0;
        this.calidad = 0.0;
    }

    /**
     * Constructor con evaluación delta.
     */
    public SolucionVecino(HashMap<Paquete, ArrayList<Vuelo>> solucion, int peso, TSMove movimiento, double deltaEvaluacion) {
        this.solucion = new HashMap<>(solucion);
        this.peso = peso;
        this.movimiento = movimiento;
        this.deltaEvaluacion = deltaEvaluacion;
        this.calidad = 0.0;
    }

    /**
     * Verifica si esta solución es mejor que otra basándose en el peso.
     */
    public boolean esMejorQue(SolucionVecino otra) {
        return this.peso > otra.peso;
    }

    /**
     * Verifica si esta solución mejora respecto a un peso de referencia.
     */
    public boolean mejora(int pesoReferencia) {
        return this.peso > pesoReferencia;
    }

    /**
     * Calcula la calidad relativa de la solución basada en múltiples criterios.
     */
    public void calcularCalidad(HashMap<Paquete, ArrayList<Vuelo>> solucionReferencia) {
        double calidad = 0.0;

        // Factor 1: Mejora en peso (30%)
        if (solucionReferencia != null) {
            double denom = Math.max(1.0, Math.abs((double) this.peso));
            double mejoraPeso = this.deltaEvaluacion / denom; // relativo al peso actual
            // Normalizar con tanh para mantenerlo en [-1,1] y suavizar outliers
            calidad += 0.30 * Math.tanh(mejoraPeso * 2.0);
        }

        // Factor 2: Eficiencia de rutas (25%)
        double eficienciaRutas = calcularEficienciaRutas();
        calidad += 0.25 * eficienciaRutas;

        // Factor 3: Utilización de capacidad (20%)
        double utilizacionCapacidad = calcularUtilizacionCapacidad();
        calidad += 0.2 * utilizacionCapacidad;

        // Factor 4: Cumplimiento de deadlines (15%)
        double cumplimientoDeadlines = calcularCumplimientoDeadlines(); // [0,1]
        calidad += 0.15 * cumplimientoDeadlines;

        // Normalizar y acotar a [0,1]
        // (Puede ser ligeramente negativo si la mejora de peso es muy mala; clamp a 0)
        this.calidad = Math.max(0.0, Math.min(1.0, calidad));

    }

    /**
     * Eficiencia de rutas en [0,1]: penaliza rutas con muchas escalas.
     * Aproximación simple: eficiencia = 1 / max(1, promedioTramosPorPaquete).
     */
    private double calcularEficienciaRutas() {
        if (this.solucion == null || this.solucion.isEmpty()) return 0.0;

        long paquetesConRuta = 0L;
        long totalTramos = 0L;

        for (java.util.Map.Entry<Paquete, ArrayList<Vuelo>> e : this.solucion.entrySet()) {
            ArrayList<Vuelo> ruta = e.getValue();
            if (ruta != null && !ruta.isEmpty()) {
                paquetesConRuta++;
                totalTramos += ruta.size();
            }
        }

        if (paquetesConRuta == 0L) return 0.0;

        double promedioTramos = (double) totalTramos / (double) paquetesConRuta; // >= 1
        double eficiencia = 1.0 / Math.max(1.0, promedioTramos);
        // clamp por seguridad
        return Math.max(0.0, Math.min(1.0, eficiencia));
    }

    /**
     * Utilización de capacidad promedio en [0,1].
     * Se agrega el uso por vuelo contando cuántos paquetes lo incluyen como tramo,
     * y se divide entre su capacidad declarada (si > 0).
     */
    private double calcularUtilizacionCapacidad() {
        if (this.solucion == null || this.solucion.isEmpty()) return 0.0;

        java.util.HashMap<Vuelo, Integer> usoPorVuelo = new java.util.HashMap<>();

        for (java.util.Map.Entry<Paquete, ArrayList<Vuelo>> e : this.solucion.entrySet()) {
            ArrayList<Vuelo> ruta = e.getValue();
            if (ruta == null) continue;
            int productos = (e.getKey() != null && e.getKey().getProductos() != null)
                    ? e.getKey().getProductos().size() : 1;
            for (Vuelo v : ruta) {
                if (v == null) continue;
                usoPorVuelo.put(v, usoPorVuelo.getOrDefault(v, 0) + productos);
            }
        }

        if (usoPorVuelo.isEmpty()) return 0.0;

        double sumaRatios = 0.0;
        int cont = 0;

        for (java.util.Map.Entry<Vuelo, Integer> e : usoPorVuelo.entrySet()) {
            Vuelo vuelo = e.getKey();
            int usados = e.getValue();
            try {
                int capacidadMax = (vuelo != null) ? vuelo.getCapacidadMaxima() : 0;
                if (capacidadMax > 0) {
                    double ratio = (double) usados / (double) capacidadMax;
                    ratio = Math.max(0.0, Math.min(1.0, ratio));
                    sumaRatios += ratio;
                    cont++;
                }
            } catch (Exception ex) {
                // Ignorar vuelos con datos inconsistentes
            }
        }

        if (cont == 0) return 0.0;
        double promedio = sumaRatios / (double) cont;
        return Math.max(0.0, Math.min(1.0, promedio));
    }

    /**
     * Cumplimiento de deadlines en [0,1].
     * Cuenta el porcentaje de paquetes cuya última llegada no excede su deadline.
     * Si no se puede evaluar (fechas nulas o tipos distintos), se ignora el paquete.
     */
    private double calcularCumplimientoDeadlines() {
        if (this.solucion == null || this.solucion.isEmpty()) return 0.0;

        int evaluables = 0;
        int enTiempo = 0;

        for (java.util.Map.Entry<Paquete, ArrayList<Vuelo>> e : this.solucion.entrySet()) {
            Paquete p = e.getKey();
            ArrayList<Vuelo> ruta = e.getValue();

            if (p == null || p.getFechaPedido() == null || p.getFechaLimiteEntrega() == null) {
                continue;
            }

            double tiempoRuta = 0.0;
            if (ruta != null && !ruta.isEmpty()) {
                for (Vuelo v : ruta) {
                    if (v != null) tiempoRuta += v.getTiempoTransporte();
                }
                // Conexiones de 2 horas por tramo adicional
                if (ruta.size() > 1) {
                    tiempoRuta += (ruta.size() - 1) * 2.0;
                }
            }

            long horasDisponibles = java.time.temporal.ChronoUnit.HOURS.between(
                    p.getFechaPedido(), p.getFechaLimiteEntrega());

            if (horasDisponibles <= 0) continue;

            evaluables++;
            if (tiempoRuta <= horasDisponibles) {
                enTiempo++;
            }
        }

        if (evaluables == 0) return 0.0;
        double ratio = (double) enTiempo / (double) evaluables;
        return Math.max(0.0, Math.min(1.0, ratio));
    }

    @Override
    public String toString() {
        String tipo = (movimiento != null ? movimiento.tipo : "N/A");
        String p1 = (movimiento != null && movimiento.paquete1 != null) ? String.valueOf(movimiento.paquete1.getId()) : "-";
        String p2 = (movimiento != null && movimiento.paquete2 != null) ? String.valueOf(movimiento.paquete2.getId()) : "-";
        return "Vecino{peso=" + peso +
                ", delta=" + deltaEvaluacion +
                ", calidad=" + calidad +
                ", mov=" + tipo +
                ", p1=" + p1 +
                ", p2=" + p2 +
                ", rutas=" + (solucion != null ? solucion.size() : 0) +
                '}';
    }
}
