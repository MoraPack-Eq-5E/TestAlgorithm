package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.core.enums.EstadoGeneral;
import com.grupo5e.morapack.service.ParserCancelaciones.CancelacionProgramada;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestor de cancelaciones de vuelos
 * Maneja cancelaciones manuales y programadas según las especificaciones
 */
public class GestorCancelaciones {
    
    private final Map<String, Vuelo> vuelos;
    private final List<CancelacionEvento> historialCancelaciones;
    
    public GestorCancelaciones(Map<String, Vuelo> vuelos) {
        this.vuelos = vuelos;
        this.historialCancelaciones = new ArrayList<>();
    }
    
    /**
     * Cancela un vuelo manualmente (desde interfaz de usuario)
     * Solo se puede cancelar si no ha despegado
     */
    public ResultadoCancelacion cancelarManual(String numeroVuelo, String motivo) {
        Vuelo vuelo = vuelos.get(numeroVuelo);
        
        if (vuelo == null) {
            return new ResultadoCancelacion(false, "Vuelo no encontrado: " + numeroVuelo);
        }
        
        if (!vuelo.isPuedeCancelar()) {
            return new ResultadoCancelacion(false, 
                "No se puede cancelar el vuelo " + numeroVuelo + " porque ya ha despegado");
        }
        
        // Intentar cancelar
        boolean cancelado = vuelo.cancelarManual(motivo);
        
        if (cancelado) {
            // Registrar en historial
            CancelacionEvento evento = new CancelacionEvento(
                numeroVuelo, 
                "MANUAL", 
                motivo, 
                LocalDateTime.now(),
                vuelo.getPaquetesReservados()
            );
            historialCancelaciones.add(evento);
            
            System.out.println("✅ Vuelo " + numeroVuelo + " cancelado manualmente");
            System.out.println("   Motivo: " + motivo);
            System.out.println("   Paquetes liberados: " + vuelo.getPaquetesReservados());
            
            return new ResultadoCancelacion(true, "Vuelo cancelado exitosamente");
        } else {
            return new ResultadoCancelacion(false, "No se pudo cancelar el vuelo");
        }
    }
    
    /**
     * Cancela un vuelo programadamente (desde archivo)
     * Se puede cancelar independientemente del estado
     */
    public ResultadoCancelacion cancelarProgramado(String numeroVuelo, String motivo) {
        Vuelo vuelo = vuelos.get(numeroVuelo);
        
        if (vuelo == null) {
            return new ResultadoCancelacion(false, "Vuelo no encontrado: " + numeroVuelo);
        }
        
        // Cancelar programadamente (siempre es posible)
        vuelo.cancelarProgramado(motivo);
        
        // Registrar en historial
        CancelacionEvento evento = new CancelacionEvento(
            numeroVuelo, 
            "PROGRAMADA", 
            motivo, 
            LocalDateTime.now(),
            vuelo.getPaquetesReservados()
        );
        historialCancelaciones.add(evento);
        
        System.out.println("✅ Vuelo " + numeroVuelo + " cancelado programadamente");
        System.out.println("   Motivo: " + motivo);
        System.out.println("   Paquetes liberados: " + vuelo.getPaquetesReservados());
        
        return new ResultadoCancelacion(true, "Vuelo cancelado programadamente");
    }
    
    /**
     * Cancela múltiples vuelos desde un archivo
     */
    public List<ResultadoCancelacion> cancelarDesdeArchivo(String rutaArchivo) {
        List<ResultadoCancelacion> resultados = new ArrayList<>();
        
        try {
            List<CancelacionProgramada> cancelaciones = ParserCancelaciones.parsearArchivo(rutaArchivo);
            
            System.out.println("📁 Procesando " + cancelaciones.size() + " cancelaciones programadas...");
            
            for (CancelacionProgramada cancelacion : cancelaciones) {
                ResultadoCancelacion resultado = cancelarProgramado(
                    cancelacion.getNumeroVuelo(), 
                    cancelacion.getMotivo()
                );
                resultados.add(resultado);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error al procesar archivo de cancelaciones: " + e.getMessage());
            resultados.add(new ResultadoCancelacion(false, "Error: " + e.getMessage()));
        }
        
        return resultados;
    }
    
    /**
     * Obtiene vuelos que pueden ser cancelados manualmente
     */
    public List<Vuelo> getVuelosCancelables() {
        return vuelos.values().stream()
            .filter(Vuelo::isPuedeCancelar)
            .filter(vuelo -> vuelo.getEstadoVuelo() != EstadoGeneral.CANCELADO_MANUAL)
            .filter(vuelo -> vuelo.getEstadoVuelo() != EstadoGeneral.CANCELADO_PROGRAMADO)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene vuelos cancelados
     */
    public List<Vuelo> getVuelosCancelados() {
        return vuelos.values().stream()
            .filter(vuelo -> vuelo.getEstadoVuelo().esVueloCancelado())
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene estadísticas de cancelaciones
     */
    public EstadisticasCancelaciones getEstadisticas() {
        int totalCancelaciones = historialCancelaciones.size();
        int cancelacionesManuales = (int) historialCancelaciones.stream()
            .filter(c -> "MANUAL".equals(c.getTipo()))
            .count();
        int cancelacionesProgramadas = (int) historialCancelaciones.stream()
            .filter(c -> "PROGRAMADA".equals(c.getTipo()))
            .count();
        
        int totalPaquetesAfectados = historialCancelaciones.stream()
            .mapToInt(CancelacionEvento::getPaquetesAfectados)
            .sum();
        
        return new EstadisticasCancelaciones(
            totalCancelaciones,
            cancelacionesManuales,
            cancelacionesProgramadas,
            totalPaquetesAfectados
        );
    }
    
    /**
     * Genera reporte de cancelaciones
     */
    public void generarReporte() {
        System.out.println("\n📊 REPORTE DE CANCELACIONES");
        System.out.println("=" .repeat(60));
        
        EstadisticasCancelaciones stats = getEstadisticas();
        
        System.out.println("📈 Estadísticas Generales:");
        System.out.println("   Total de cancelaciones: " + stats.getTotalCancelaciones());
        System.out.println("   Cancelaciones manuales: " + stats.getCancelacionesManuales());
        System.out.println("   Cancelaciones programadas: " + stats.getCancelacionesProgramadas());
        System.out.println("   Total paquetes afectados: " + stats.getTotalPaquetesAfectados());
        
        System.out.println("\n🛫 Vuelos Cancelados Actualmente:");
        List<Vuelo> vuelosCancelados = getVuelosCancelados();
        if (vuelosCancelados.isEmpty()) {
            System.out.println("   No hay vuelos cancelados");
        } else {
            for (Vuelo vuelo : vuelosCancelados) {
                System.out.println("   " + vuelo.getNumeroVuelo() + " - " + 
                                 vuelo.getEstadoVuelo() + " - " + 
                                 vuelo.getMotivoCancelacion());
            }
        }
        
        System.out.println("\n✈️ Vuelos Disponibles para Cancelación Manual:");
        List<Vuelo> vuelosCancelables = getVuelosCancelables();
        if (vuelosCancelables.isEmpty()) {
            System.out.println("   No hay vuelos disponibles para cancelación");
        } else {
            for (Vuelo vuelo : vuelosCancelables) {
                System.out.println("   " + vuelo.getNumeroVuelo() + " - " + 
                                 vuelo.getAeropuertoOrigen() + " -> " + 
                                 vuelo.getAeropuertoDestino() + " - " +
                                 vuelo.getHoraSalida());
            }
        }
    }
    
    /**
     * Clase para eventos de cancelación
     */
    public static class CancelacionEvento {
        private final String numeroVuelo;
        private final String tipo; // "MANUAL" o "PROGRAMADA"
        private final String motivo;
        private final LocalDateTime fechaHora;
        private final int paquetesAfectados;
        
        public CancelacionEvento(String numeroVuelo, String tipo, String motivo, 
                               LocalDateTime fechaHora, int paquetesAfectados) {
            this.numeroVuelo = numeroVuelo;
            this.tipo = tipo;
            this.motivo = motivo;
            this.fechaHora = fechaHora;
            this.paquetesAfectados = paquetesAfectados;
        }
        
        // Getters
        public String getNumeroVuelo() { return numeroVuelo; }
        public String getTipo() { return tipo; }
        public String getMotivo() { return motivo; }
        public LocalDateTime getFechaHora() { return fechaHora; }
        public int getPaquetesAfectados() { return paquetesAfectados; }
        
        @Override
        public String toString() {
            return String.format("Cancelación[%s: %s - %s - %d paquetes]", 
                               numeroVuelo, tipo, motivo, paquetesAfectados);
        }
    }
    
    /**
     * Clase para resultados de cancelación
     */
    public static class ResultadoCancelacion {
        private final boolean exitoso;
        private final String mensaje;
        
        public ResultadoCancelacion(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }
        
        public boolean isExitoso() { return exitoso; }
        public String getMensaje() { return mensaje; }
        
        @Override
        public String toString() {
            return (exitoso ? "✅" : "❌") + " " + mensaje;
        }
    }
    
    /**
     * Clase para estadísticas de cancelaciones
     */
    public static class EstadisticasCancelaciones {
        private final int totalCancelaciones;
        private final int cancelacionesManuales;
        private final int cancelacionesProgramadas;
        private final int totalPaquetesAfectados;
        
        public EstadisticasCancelaciones(int totalCancelaciones, int cancelacionesManuales, 
                                       int cancelacionesProgramadas, int totalPaquetesAfectados) {
            this.totalCancelaciones = totalCancelaciones;
            this.cancelacionesManuales = cancelacionesManuales;
            this.cancelacionesProgramadas = cancelacionesProgramadas;
            this.totalPaquetesAfectados = totalPaquetesAfectados;
        }
        
        // Getters
        public int getTotalCancelaciones() { return totalCancelaciones; }
        public int getCancelacionesManuales() { return cancelacionesManuales; }
        public int getCancelacionesProgramadas() { return cancelacionesProgramadas; }
        public int getTotalPaquetesAfectados() { return totalPaquetesAfectados; }
    }
}
