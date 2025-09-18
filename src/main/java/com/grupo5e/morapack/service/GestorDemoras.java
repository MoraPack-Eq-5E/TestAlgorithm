package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.core.enums.EstadoGeneral;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestor de demoras de vuelos
 * Maneja demoras de 3 horas según las especificaciones
 */
public class GestorDemoras {
    
    private final Map<String, Vuelo> vuelos;
    private final List<DemoraEvento> historialDemoras;
    
    public GestorDemoras(Map<String, Vuelo> vuelos) {
        this.vuelos = vuelos;
        this.historialDemoras = new ArrayList<>();
    }
    
    /**
     * Aplica demora de 3 horas a un vuelo
     */
    public ResultadoDemora aplicarDemora(String numeroVuelo, String motivo) {
        Vuelo vuelo = vuelos.get(numeroVuelo);
        
        if (vuelo == null) {
            return new ResultadoDemora(false, "Vuelo no encontrado: " + numeroVuelo);
        }
        
        // Verificar si el vuelo puede ser demorado
        if (!puedeSerDemorado(vuelo)) {
            return new ResultadoDemora(false, 
                "El vuelo " + numeroVuelo + " no puede ser demorado en su estado actual: " + 
                vuelo.getEstadoVuelo());
        }
        
        // Aplicar demora
        vuelo.aplicarDemora();
        
        // Registrar en historial
        DemoraEvento evento = new DemoraEvento(
            numeroVuelo,
            motivo,
            LocalDateTime.now(),
            vuelo.getHorasDemora(),
            vuelo.getHoraSalida().toString(),
            vuelo.getHoraLlegada().toString()
        );
        historialDemoras.add(evento);
        
        System.out.println("⏰ Vuelo " + numeroVuelo + " demorado por 3 horas");
        System.out.println("   Motivo: " + motivo);
        System.out.println("   Nueva hora de salida: " + vuelo.getHoraSalida());
        System.out.println("   Nueva hora de llegada: " + vuelo.getHoraLlegada());
        
        return new ResultadoDemora(true, "Demora aplicada exitosamente");
    }
    
    /**
     * Aplica demora a múltiples vuelos
     */
    public List<ResultadoDemora> aplicarDemoraMasiva(List<String> numerosVuelos, String motivo) {
        List<ResultadoDemora> resultados = new ArrayList<>();
        
        System.out.println("⏰ Aplicando demora masiva a " + numerosVuelos.size() + " vuelos...");
        
        for (String numeroVuelo : numerosVuelos) {
            ResultadoDemora resultado = aplicarDemora(numeroVuelo, motivo);
            resultados.add(resultado);
        }
        
        return resultados;
    }
    
    /**
     * Verifica si un vuelo puede ser demorado
     */
    private boolean puedeSerDemorado(Vuelo vuelo) {
        // Solo se pueden demorar vuelos que estén planificados o en proceso
        // No se pueden demorar vuelos cancelados o ya completados
        return vuelo.getEstadoVuelo() == EstadoGeneral.PLANIFICADO || 
               vuelo.getEstadoVuelo() == EstadoGeneral.EN_PROCESO;
    }
    
    /**
     * Obtiene vuelos que pueden ser demorados
     */
    public List<Vuelo> getVuelosDemorables() {
        return vuelos.values().stream()
            .filter(this::puedeSerDemorado)
            .filter(vuelo -> vuelo.getHorasDemora() == 0) // Que no tengan demora previa
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene vuelos con demora
     */
    public List<Vuelo> getVuelosConDemora() {
        return vuelos.values().stream()
            .filter(vuelo -> vuelo.getEstadoVuelo() == EstadoGeneral.DEMORADO)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene vuelos que pueden ser demorados por aeropuerto
     */
    public Map<String, List<Vuelo>> getVuelosDemorablesPorAeropuerto() {
        return getVuelosDemorables().stream()
            .collect(Collectors.groupingBy(Vuelo::getAeropuertoOrigen));
    }
    
    /**
     * Simula demoras por condiciones meteorológicas en un aeropuerto
     */
    public List<ResultadoDemora> simularDemorasMeteorologicas(String codigoAeropuerto, String motivo) {
        List<Vuelo> vuelosAfectados = getVuelosDemorablesPorAeropuerto().get(codigoAeropuerto);
        
        if (vuelosAfectados == null || vuelosAfectados.isEmpty()) {
            return Arrays.asList(new ResultadoDemora(false, 
                "No hay vuelos demorables en el aeropuerto " + codigoAeropuerto));
        }
        
        System.out.println("🌧️ Simulando demoras meteorológicas en " + codigoAeropuerto);
        System.out.println("   Vuelos afectados: " + vuelosAfectados.size());
        
        List<ResultadoDemora> resultados = new ArrayList<>();
        for (Vuelo vuelo : vuelosAfectados) {
            ResultadoDemora resultado = aplicarDemora(vuelo.getNumeroVuelo(), motivo);
            resultados.add(resultado);
        }
        
        return resultados;
    }
    
    /**
     * Obtiene estadísticas de demoras
     */
    public EstadisticasDemoras getEstadisticas() {
        int totalDemoras = historialDemoras.size();
        int vuelosConDemora = getVuelosConDemora().size();
        int vuelosDemorables = getVuelosDemorables().size();
        
        double promedioHorasDemora = historialDemoras.stream()
            .mapToInt(DemoraEvento::getHorasDemora)
            .average()
            .orElse(0.0);
        
        return new EstadisticasDemoras(
            totalDemoras,
            vuelosConDemora,
            vuelosDemorables,
            promedioHorasDemora
        );
    }
    
    /**
     * Genera reporte de demoras
     */
    public void generarReporte() {
        System.out.println("\n⏰ REPORTE DE DEMORAS");
        System.out.println("=" .repeat(60));
        
        EstadisticasDemoras stats = getEstadisticas();
        
        System.out.println("📈 Estadísticas Generales:");
        System.out.println("   Total de demoras aplicadas: " + stats.getTotalDemoras());
        System.out.println("   Vuelos con demora actual: " + stats.getVuelosConDemora());
        System.out.println("   Vuelos demorables: " + stats.getVuelosDemorables());
        System.out.println("   Promedio de horas de demora: " + String.format("%.1f", stats.getPromedioHorasDemora()));
        
        System.out.println("\n🛫 Vuelos con Demora Actual:");
        List<Vuelo> vuelosConDemora = getVuelosConDemora();
        if (vuelosConDemora.isEmpty()) {
            System.out.println("   No hay vuelos con demora");
        } else {
            for (Vuelo vuelo : vuelosConDemora) {
                System.out.println("   " + vuelo.getNumeroVuelo() + " - " + 
                                 vuelo.getAeropuertoOrigen() + " -> " + 
                                 vuelo.getAeropuertoDestino() + " - " +
                                 "Demora: " + vuelo.getHorasDemora() + "h - " +
                                 "Nueva salida: " + vuelo.getHoraSalida());
            }
        }
        
        System.out.println("\n✈️ Vuelos Disponibles para Demora:");
        List<Vuelo> vuelosDemorables = getVuelosDemorables();
        if (vuelosDemorables.isEmpty()) {
            System.out.println("   No hay vuelos disponibles para demora");
        } else {
            for (Vuelo vuelo : vuelosDemorables) {
                System.out.println("   " + vuelo.getNumeroVuelo() + " - " + 
                                 vuelo.getAeropuertoOrigen() + " -> " + 
                                 vuelo.getAeropuertoDestino() + " - " +
                                 vuelo.getHoraSalida());
            }
        }
        
        // Mostrar demoras por aeropuerto
        System.out.println("\n🌍 Vuelos Demorables por Aeropuerto:");
        Map<String, List<Vuelo>> vuelosPorAeropuerto = getVuelosDemorablesPorAeropuerto();
        if (vuelosPorAeropuerto.isEmpty()) {
            System.out.println("   No hay vuelos demorables por aeropuerto");
        } else {
            vuelosPorAeropuerto.forEach((aeropuerto, vuelos) -> {
                System.out.println("   " + aeropuerto + ": " + vuelos.size() + " vuelos");
            });
        }
    }
    
    /**
     * Clase para eventos de demora
     */
    public static class DemoraEvento {
        private final String numeroVuelo;
        private final String motivo;
        private final LocalDateTime fechaHora;
        private final int horasDemora;
        private final String horaSalidaOriginal;
        private final String horaSalidaNueva;
        
        public DemoraEvento(String numeroVuelo, String motivo, LocalDateTime fechaHora, 
                           int horasDemora, String horaSalidaOriginal, String horaSalidaNueva) {
            this.numeroVuelo = numeroVuelo;
            this.motivo = motivo;
            this.fechaHora = fechaHora;
            this.horasDemora = horasDemora;
            this.horaSalidaOriginal = horaSalidaOriginal;
            this.horaSalidaNueva = horaSalidaNueva;
        }
        
        // Getters
        public String getNumeroVuelo() { return numeroVuelo; }
        public String getMotivo() { return motivo; }
        public LocalDateTime getFechaHora() { return fechaHora; }
        public int getHorasDemora() { return horasDemora; }
        public String getHoraSalidaOriginal() { return horaSalidaOriginal; }
        public String getHoraSalidaNueva() { return horaSalidaNueva; }
        
        @Override
        public String toString() {
            return String.format("Demora[%s: %dh - %s -> %s]", 
                               numeroVuelo, horasDemora, horaSalidaOriginal, horaSalidaNueva);
        }
    }
    
    /**
     * Clase para resultados de demora
     */
    public static class ResultadoDemora {
        private final boolean exitoso;
        private final String mensaje;
        
        public ResultadoDemora(boolean exitoso, String mensaje) {
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
     * Clase para estadísticas de demoras
     */
    public static class EstadisticasDemoras {
        private final int totalDemoras;
        private final int vuelosConDemora;
        private final int vuelosDemorables;
        private final double promedioHorasDemora;
        
        public EstadisticasDemoras(int totalDemoras, int vuelosConDemora, 
                                 int vuelosDemorables, double promedioHorasDemora) {
            this.totalDemoras = totalDemoras;
            this.vuelosConDemora = vuelosConDemora;
            this.vuelosDemorables = vuelosDemorables;
            this.promedioHorasDemora = promedioHorasDemora;
        }
        
        // Getters
        public int getTotalDemoras() { return totalDemoras; }
        public int getVuelosConDemora() { return vuelosConDemora; }
        public int getVuelosDemorables() { return vuelosDemorables; }
        public double getPromedioHorasDemora() { return promedioHorasDemora; }
    }
}
