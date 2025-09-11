package com.grupo5e.morapack.algorithm.validation;

import com.grupo5e.morapack.core.enums.TipoViolacion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Representa una violación específica de restricción encontrada durante la validación.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ViolacionRestriccion {
    
    private String descripcion;
    private TipoViolacion tipo;
    private LocalDateTime momentoDeteccion;
    private String contexto; // Información adicional sobre dónde ocurrió
    private double impacto; // Cuantificación del impacto (costo, tiempo, etc.)
    
    public ViolacionRestriccion(String descripcion, TipoViolacion tipo) {
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.momentoDeteccion = LocalDateTime.now();
        this.contexto = "";
        this.impacto = tipo.getPesoPenalizacion();
    }
    
    public ViolacionRestriccion(String descripcion, TipoViolacion tipo, String contexto) {
        this(descripcion, tipo);
        this.contexto = contexto;
    }
    
    public ViolacionRestriccion(String descripcion, TipoViolacion tipo, String contexto, double impacto) {
        this(descripcion, tipo, contexto);
        this.impacto = impacto;
    }
    
    /**
     * @return true si esta violación es crítica para la factibilidad
     */
    public boolean esCritica() {
        return tipo.esCritica();
    }
    
    /**
     * @return el nivel de severidad de la violación
     */
    public int getNivelSeveridad() {
        return tipo.getNivelSeveridad();
    }
    
    /**
     * @return una representación detallada de la violación
     */
    public String getDescripcionCompleta() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] %s", tipo.name(), descripcion));
        
        if (!contexto.isEmpty()) {
            sb.append(String.format(" (Contexto: %s)", contexto));
        }
        
        if (impacto > 0) {
            sb.append(String.format(" [Impacto: %.2f]", impacto));
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getDescripcionCompleta();
    }
}
