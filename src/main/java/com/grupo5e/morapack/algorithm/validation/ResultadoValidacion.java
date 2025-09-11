package com.grupo5e.morapack.algorithm.validation;

import com.grupo5e.morapack.core.enums.TipoViolacion;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de la validación de restricciones.
 * Contiene información detallada sobre violaciones encontradas.
 */
@Getter
@Setter
public class ResultadoValidacion {
    
    private List<ViolacionRestriccion> violaciones;
    private boolean factible;
    
    public ResultadoValidacion() {
        this.violaciones = new ArrayList<>();
        this.factible = true;
    }
    
    /**
     * Agrega una nueva violación al resultado.
     */
    public void agregarViolacion(String descripcion, TipoViolacion tipo) {
        violaciones.add(new ViolacionRestriccion(descripcion, tipo));
        this.factible = false;
    }
    
    /**
     * Combina este resultado con otro resultado de validación.
     */
    public void combinar(ResultadoValidacion otro) {
        this.violaciones.addAll(otro.getViolaciones());
        if (!otro.esFactible()) {
            this.factible = false;
        }
    }
    
    /**
     * @return true si no hay violaciones, false en caso contrario
     */
    public boolean esFactible() {
        return factible;
    }
    
    /**
     * @return el número total de violaciones
     */
    public int getTotalViolaciones() {
        return violaciones.size();
    }
    
    /**
     * @return el número de violaciones de un tipo específico
     */
    public long getViolacionesPorTipo(TipoViolacion tipo) {
        return violaciones.stream()
                .filter(v -> v.getTipo() == tipo)
                .count();
    }
    
    /**
     * @return una lista de descripciones de todas las violaciones
     */
    public List<String> getDescripcionesViolaciones() {
        return violaciones.stream()
                .map(ViolacionRestriccion::getDescripcion)
                .toList();
    }
    
    /**
     * @return true si tiene violaciones críticas (que hacen la solución inviable)
     */
    public boolean tieneViolacionesCriticas() {
        return violaciones.stream()
                .anyMatch(v -> v.getTipo().esCritica());
    }
    
    /**
     * Limpia todas las violaciones y marca como factible.
     */
    public void limpiar() {
        violaciones.clear();
        factible = true;
    }
    
    @Override
    public String toString() {
        if (esFactible()) {
            return "Validación exitosa - Sin violaciones";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Validación fallida - %d violaciones:\n", getTotalViolaciones()));
        
        for (ViolacionRestriccion violacion : violaciones) {
            sb.append(String.format("  [%s] %s\n", 
                     violacion.getTipo().name(), violacion.getDescripcion()));
        }
        
        return sb.toString();
    }
    
    /**
     * Genera un resumen estadístico de las violaciones.
     */
    public String generarResumen() {
        if (esFactible()) {
            return "✓ Solución factible";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("✗ Solución NO factible:\n");
        
        // Contar violaciones por tipo
        for (TipoViolacion tipo : TipoViolacion.values()) {
            long count = getViolacionesPorTipo(tipo);
            if (count > 0) {
                sb.append(String.format("  - %s: %d violaciones\n", 
                         tipo.getDescripcion(), count));
            }
        }
        
        return sb.toString();
    }
}
