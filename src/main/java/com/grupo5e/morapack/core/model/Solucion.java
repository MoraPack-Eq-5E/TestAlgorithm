package com.grupo5e.morapack.core.model;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.Objects;

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
    
    private void recomputarOcupacionesDesdeRutas() {
        ocupacionVuelos.clear();
        ocupacionAlmacenes.clear();
        for (Ruta r : rutasPaquetes.values()) {
            for (SegmentoRuta s : r.getSegmentos()) {
                ocupacionVuelos.merge(s.getNumeroVuelo(), 1, Integer::sum);
                ocupacionAlmacenes.merge(s.getAeropuertoDestino(), 1, Integer::sum);
            }
        }
    }
    
    public void recalcularMetricas() {
        // Recalcular ocupaciones primero para no arrastrar drift
        recomputarOcupacionesDesdeRutas();

        costoTotal = rutasPaquetes.values().stream()
                .mapToDouble(Ruta::getCostoTotal)
                .sum();

        // makespan (tiempo máximo de ruta); si prefieres suma, cambia aquí
        tiempoTotalHoras = rutasPaquetes.values().stream()
                .mapToDouble(Ruta::getTiempoTotalHoras)
                .max()
                .orElse(0.0);

        // LITERATURA ALNS: NO calcular fitness aquí - solo métricas básicas
        // El fitness se calcula UNA SOLA VEZ después de la reparación completa
    }
    
    /**
     * FUNCIÓN ÚNICA PARA CALCULAR FITNESS
     * Esta es la única función que debe usarse para calcular el fitness
     * Garantiza consistencia en todo el algoritmo
     */
    public void calcularFitness(int totalPaquetes) {
        recalcularMetricas(); // Asegurar que las métricas estén actualizadas
        double penalViol = calcularPenalizacionGradual();
        double penalNoRuteados = calcularPenalizacionPaquetesNoRuteados(totalPaquetes);
        
        // FÓRMULA ÚNICA Y CONSISTENTE
        this.funcionObjetivo = costoTotal + tiempoTotalHoras + penalViol + penalNoRuteados;
    }
    
    /**
     * Versión sin contexto (para compatibilidad)
     * @deprecated Usar calcularFitness(int totalPaquetes) en su lugar
     */
    @Deprecated
    public void calcularFuncionObjetivo() {
        calcularFitness(0); // Sin penalización por no ruteados
    }
    
    /**
     * Versión con contexto (para compatibilidad)
     * @deprecated Usar calcularFitness(int totalPaquetes) en su lugar
     */
    @Deprecated
    public void calcularFuncionObjetivo(int totalPaquetes) {
        calcularFitness(totalPaquetes);
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
        if (cantidad <= 0 || rutasPaquetes.isEmpty()) return Collections.emptyList();
        List<String> pkgs = new ArrayList<>(rutasPaquetes.keySet());
        Collections.shuffle(pkgs);
        return pkgs.subList(0, Math.min(cantidad, pkgs.size()));
    }
    
    public Solucion copiar() {
        Solucion c = new Solucion();
        for (Map.Entry<String, Ruta> e : this.rutasPaquetes.entrySet()) {
            c.rutasPaquetes.put(e.getKey(), e.getValue().copiar());
        }
        // Recalcula todo desde rutas (evita copiar mapas desfasados)
        c.recalcularMetricas();
        
        // CORRECCIÓN: Copiar también el fitness y estados de validación
        c.funcionObjetivo = this.funcionObjetivo;
        c.esFactible = this.esFactible;
        c.violacionesRestricciones = this.violacionesRestricciones;
        
        return c;
    }
    
    /**
     * API para recibir resultado de validación
     * Para alinear esFactible y violacionesRestricciones con FO
     */
    public void aplicarResultadoValidacion(boolean esFactible, int violaciones, int totalPaquetes) {
        this.esFactible = esFactible;
        this.violacionesRestricciones = violaciones;
        // CORRECCIÓN: Usar función única para calcular fitness
        calcularFitness(totalPaquetes);
    }
    
    /**
     * Actualiza el fitness con contexto completo (costo + makespan + penalizaciones)
     * @deprecated Usar calcularFitness(int totalPaquetes) en su lugar
     */
    @Deprecated
    public void actualizarFitnessConContexto(int totalPaquetes) {
        // CORRECCIÓN: Usar función única para garantizar consistencia
        calcularFitness(totalPaquetes);
    }
    
    public boolean esMejorQue(Solucion otra) {
        if (this.esFactible && !otra.esFactible) return true;
        if (!this.esFactible && otra.esFactible) return false;
        return this.funcionObjetivo < otra.funcionObjetivo;
    }
    
    public double calcularPromedioPaquetesPorVuelo() {
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Solucion solucion = (Solucion) o;
        
        // Comparar por contenido de rutas (no por identidad de objeto)
        if (rutasPaquetes.size() != solucion.rutasPaquetes.size()) return false;
        
        // Verificar que todas las rutas sean iguales
        for (Map.Entry<String, Ruta> entry : rutasPaquetes.entrySet()) {
            String paqueteId = entry.getKey();
            Ruta ruta = entry.getValue();
            Ruta otraRuta = solucion.rutasPaquetes.get(paqueteId);
            
            if (otraRuta == null || !rutasEquivalentes(ruta, otraRuta)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public int hashCode() {
        // Hash basado en contenido de rutas, no en identidad de objeto
        int hash = 0;
        for (Map.Entry<String, Ruta> entry : rutasPaquetes.entrySet()) {
            hash += entry.getKey().hashCode();
            hash += entry.getValue().getSegmentos().hashCode();
        }
        return hash;
    }
    
    /**
     * Compara si dos rutas son equivalentes (mismo contenido)
     */
    private boolean rutasEquivalentes(Ruta ruta1, Ruta ruta2) {
        if (ruta1.getSegmentos().size() != ruta2.getSegmentos().size()) {
            return false;
        }
        
        for (int i = 0; i < ruta1.getSegmentos().size(); i++) {
            SegmentoRuta seg1 = ruta1.getSegmentos().get(i);
            SegmentoRuta seg2 = ruta2.getSegmentos().get(i);
            
            if (!segmentosEquivalentes(seg1, seg2)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Compara si dos segmentos son equivalentes
     */
    private boolean segmentosEquivalentes(SegmentoRuta seg1, SegmentoRuta seg2) {
        return Objects.equals(seg1.getAeropuertoOrigen(), seg2.getAeropuertoOrigen()) &&
               Objects.equals(seg1.getAeropuertoDestino(), seg2.getAeropuertoDestino()) &&
               Objects.equals(seg1.getNumeroVuelo(), seg2.getNumeroVuelo());
    }
    
    @Override
    public String toString() {
        return String.format("Solucion[Paquetes: %d, Costo: %.2f, Tiempo: %.2f hrs, Factible: %s, FObjetivo: %.2f]", 
                           rutasPaquetes.size(), costoTotal, tiempoTotalHoras, esFactible, funcionObjetivo);
    }
}
