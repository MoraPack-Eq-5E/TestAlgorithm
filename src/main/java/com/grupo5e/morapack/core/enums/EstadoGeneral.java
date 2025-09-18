package com.grupo5e.morapack.core.enums;

/**
 * Enum unificado que reemplaza EstadoPaquete, EstadoPedido, EstadoRuta y EstadoSegmento.
 * Centralizamos todos los estados posibles en un solo lugar para mejor mantenibilidad.
 */
public enum EstadoGeneral {
    
    // Estados generales aplicables a paquetes, pedidos, rutas y segmentos
    CREADO("Recién creado, pendiente de procesamiento"),
    PLANIFICADO("Planificado pero no iniciado"),
    EN_PROCESO("En proceso de ejecución"),
    EN_TRANSITO("En movimiento hacia destino"),
    EN_ALMACEN("Almacenado temporalmente"),
    COMPLETADO("Procesamiento completado exitosamente"),
    ENTREGADO("Entregado al destinatario final"),
    RETRASADO("Con retraso respecto al plazo planificado"),
    CANCELADO("Cancelado por el usuario o sistema"),
    FALLIDO("Falló durante el procesamiento"),
    
    // Estados específicos para elementos de routing
    PAUSADO("Temporalmente pausado"),
    EN_ESPERA("En espera de recursos o condiciones"),
    VALIDANDO("En proceso de validación"),
    RECHAZADO("Rechazado por no cumplir restricciones"),
    
    // Estados específicos para vuelos (nuevas especificaciones)
    DEMORADO("Vuelo con demora de 3 horas"),
    CANCELADO_PROGRAMADO("Vuelo cancelado programadamente"),
    CANCELADO_MANUAL("Vuelo cancelado manualmente"),
    
    // Estados específicos para productos/paquetes (nuevas especificaciones)
    REASIGNABLE("Producto puede ser reasignado"),
    NO_REASIGNABLE("Producto no puede ser reasignado"),
    EN_ALMACEN_PASO("En almacén de paso (reasignable)"),
    EN_ALMACEN_ENTREGA("En almacén de entrega (no reasignable)");
    
    private final String descripcion;
    
    EstadoGeneral(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    /**
     * Verifica si el estado representa un proceso en curso
     */
    public boolean esActivo() {
        return this == EN_PROCESO || this == EN_TRANSITO || this == EN_ALMACEN || 
               this == PLANIFICADO || this == EN_ESPERA || this == VALIDANDO;
    }
    
    /**
     * Verifica si el estado representa un proceso terminado exitosamente
     */
    public boolean esExitoso() {
        return this == COMPLETADO || this == ENTREGADO;
    }
    
    /**
     * Verifica si el estado representa un problema o fallo
     */
    public boolean esProblematico() {
        return this == RETRASADO || this == CANCELADO || this == FALLIDO || this == RECHAZADO ||
               this == DEMORADO || this == CANCELADO_PROGRAMADO || this == CANCELADO_MANUAL;
    }
    
    /**
     * Verifica si el estado permite reasignación de productos
     */
    public boolean permiteReasignacion() {
        return this == REASIGNABLE || this == EN_ALMACEN_PASO || this == EN_TRANSITO;
    }
    
    /**
     * Verifica si el estado es de vuelo cancelado
     */
    public boolean esVueloCancelado() {
        return this == CANCELADO_PROGRAMADO || this == CANCELADO_MANUAL;
    }
    
    /**
     * Verifica si el estado es de vuelo demorado
     */
    public boolean esVueloDemorado() {
        return this == DEMORADO;
    }
    
    /**
     * Obtiene el estado inicial por defecto para nuevos elementos
     */
    public static EstadoGeneral inicial() {
        return CREADO;
    }
    
    /**
     * Mapeo específico para compatibilidad con los enums antiguos
     */
    public static class Mapeo {
        // Estados típicos para Paquetes
        public static EstadoGeneral paqueteInicial() { return CREADO; }
        public static EstadoGeneral paqueteEnRuta() { return EN_TRANSITO; }
        public static EstadoGeneral paqueteEntregado() { return ENTREGADO; }
        
        // Estados típicos para Rutas
        public static EstadoGeneral rutaPlanificada() { return PLANIFICADO; }
        public static EstadoGeneral rutaEjecutandose() { return EN_PROCESO; }
        public static EstadoGeneral rutaCompletada() { return COMPLETADO; }
        
        // Estados típicos para Pedidos
        public static EstadoGeneral pedidoPendiente() { return CREADO; }
        public static EstadoGeneral pedidoProcesando() { return EN_PROCESO; }
        public static EstadoGeneral pedidoFinalizado() { return COMPLETADO; }
        
        // Estados típicos para Segmentos
        public static EstadoGeneral segmentoPendiente() { return PLANIFICADO; }
        public static EstadoGeneral segmentoEjecutando() { return EN_PROCESO; }
        public static EstadoGeneral segmentoCompletado() { return COMPLETADO; }
    }
}
