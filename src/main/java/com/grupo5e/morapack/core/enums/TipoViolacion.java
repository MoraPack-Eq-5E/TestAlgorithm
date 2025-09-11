package com.grupo5e.morapack.core.enums;

/**
 * Tipos de violaciones de restricciones en el problema MoraPack.
 * Categoriza las diferentes violaciones que pueden ocurrir.
 */
public enum TipoViolacion {
    
    // Violaciones críticas (hacen la solución inviable)
    RUTA_INVALIDA("Ruta inválida o desconectada", true),
    AEROPUERTO_INEXISTENTE("Aeropuerto no existe", true),
    VUELO_INEXISTENTE("Vuelo no existe", true),
    
    // Violaciones de capacidad (críticas operacionalmente)
    CAPACIDAD_VUELO_EXCEDIDA("Capacidad de vuelo excedida", true),
    CAPACIDAD_ALMACEN_EXCEDIDA("Capacidad de almacén excedida", true),
    
    // Violaciones de tiempo (críticas para el negocio)
    PLAZO_EXCEDIDO("Plazo de entrega excedido", true),
    DURACION_VUELO_INCORRECTA("Duración de vuelo incorrecta", false),
    
    // Violaciones de consistencia (pueden ser críticas)
    INCONSISTENCIA_VUELO("Inconsistencia entre vuelo y ruta", true),
    CONTINENTE_INDETERMINADO("No se puede determinar continente", false),
    
    // Violaciones de negocio (importantes pero no críticas)
    RUTA_SUBOPTIMA("Ruta subóptima detectada", false),
    UTILIZACION_INEFICIENTE("Utilización ineficiente de recursos", false),
    
    // Violaciones de datos
    DATOS_FALTANTES("Datos requeridos faltantes", true),
    DATOS_INCONSISTENTES("Datos inconsistentes", false);
    
    private final String descripcion;
    private final boolean critica;
    
    TipoViolacion(String descripcion, boolean critica) {
        this.descripcion = descripcion;
        this.critica = critica;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public boolean esCritica() {
        return critica;
    }
    
    /**
     * @return true si esta violación afecta la factibilidad de la solución
     */
    public boolean afectaFactibilidad() {
        return critica;
    }
    
    /**
     * @return el nivel de severidad (1-3, donde 3 es más severo)
     */
    public int getNivelSeveridad() {
        if (critica) {
            return 3; // Crítica
        } else {
            return 2; // Moderada
        }
    }
    
    /**
     * @return el peso de penalización para la función objetivo
     */
    public double getPesoPenalizacion() {
        return critica ? 1000.0 : 100.0;
    }
}
