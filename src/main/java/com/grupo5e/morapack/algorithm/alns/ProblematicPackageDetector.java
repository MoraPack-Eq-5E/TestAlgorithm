package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.model.*;

import java.util.*;

/**
 * Detecta y analiza paquetes problemáticos para optimizar estrategias de ruteo.
 */
public class ProblematicPackageDetector {
    
    private Map<String, List<FalloInfo>> historialFallos = new HashMap<>();
    private Map<String, EstadisticasPaquete> estadisticasPaquetes = new HashMap<>();
    private static final int UMBRAL_FALLOS_PROBLEMATICO = 3;
    private static final int UMBRAL_FALLOS_CRITICO = 5;
    private static final double FACTOR_DIFICULTAD_DISTANCIA = 0.001;
    private static final double FACTOR_DIFICULTAD_CAPACIDAD = 0.5;
    
    public static class FalloInfo {
        private final long timestamp;
        private final String motivo;
        private final String operadorUsado;
        private final double fitnessSolucion;
        
        public FalloInfo(String motivo, String operadorUsado, double fitnessSolucion) {
            this.timestamp = System.currentTimeMillis();
            this.motivo = motivo;
            this.operadorUsado = operadorUsado;
            this.fitnessSolucion = fitnessSolucion;
        }
        
        public long getTimestamp() { return timestamp; }
        public String getMotivo() { return motivo; }
        public String getOperadorUsado() { return operadorUsado; }
        public double getFitnessSolucion() { return fitnessSolucion; }
    }
    
    public static class EstadisticasPaquete {
        private int totalFallos = 0;
        private int fallosRecientes = 0;
        private double dificultadCalculada = 0.0;
        private List<String> operadoresFallidos = new ArrayList<>();
        private List<String> motivosFallos = new ArrayList<>();
        private long ultimoFallo = 0;
        private boolean esProblematico = false;
        private boolean esCritico = false;
        
        public int getTotalFallos() { return totalFallos; }
        public void setTotalFallos(int totalFallos) { this.totalFallos = totalFallos; }
        
        public int getFallosRecientes() { return fallosRecientes; }
        public void setFallosRecientes(int fallosRecientes) { this.fallosRecientes = fallosRecientes; }
        
        public double getDificultadCalculada() { return dificultadCalculada; }
        public void setDificultadCalculada(double dificultadCalculada) { this.dificultadCalculada = dificultadCalculada; }
        
        public List<String> getOperadoresFallidos() { return operadoresFallidos; }
        public void setOperadoresFallidos(List<String> operadoresFallidos) { this.operadoresFallidos = operadoresFallidos; }
        
        public List<String> getMotivosFallos() { return motivosFallos; }
        public void setMotivosFallos(List<String> motivosFallos) { this.motivosFallos = motivosFallos; }
        
        public long getUltimoFallo() { return ultimoFallo; }
        public void setUltimoFallo(long ultimoFallo) { this.ultimoFallo = ultimoFallo; }
        
        public boolean isEsProblematico() { return esProblematico; }
        public void setEsProblematico(boolean esProblematico) { this.esProblematico = esProblematico; }
        
        public boolean isEsCritico() { return esCritico; }
        public void setEsCritico(boolean esCritico) { this.esCritico = esCritico; }
    }
    
    public void registrarFallo(String paqueteId, String motivo, String operadorUsado, double fitnessSolucion) {
        FalloInfo fallo = new FalloInfo(motivo, operadorUsado, fitnessSolucion);
        
        historialFallos.computeIfAbsent(paqueteId, k -> new ArrayList<>()).add(fallo);
        actualizarEstadisticas(paqueteId, fallo);
        
        System.out.println("Fallo registrado para " + paqueteId + ": " + motivo + " (Operador: " + operadorUsado + ")");
    }
    
    private void actualizarEstadisticas(String paqueteId, FalloInfo fallo) {
        EstadisticasPaquete stats = estadisticasPaquetes.computeIfAbsent(paqueteId, k -> new EstadisticasPaquete());
        
        stats.setTotalFallos(stats.getTotalFallos() + 1);
        stats.setUltimoFallo(fallo.getTimestamp());
        long tiempoLimite = System.currentTimeMillis() - (10 * 60 * 1000); // 10 minutos
        int fallosRecientes = (int) historialFallos.get(paqueteId).stream()
            .filter(f -> f.getTimestamp() > tiempoLimite)
            .count();
        stats.setFallosRecientes(fallosRecientes);
        
        if (!stats.getOperadoresFallidos().contains(fallo.getOperadorUsado())) {
            stats.getOperadoresFallidos().add(fallo.getOperadorUsado());
        }
        if (!stats.getMotivosFallos().contains(fallo.getMotivo())) {
            stats.getMotivosFallos().add(fallo.getMotivo());
        }
        
        stats.setEsProblematico(stats.getTotalFallos() >= UMBRAL_FALLOS_PROBLEMATICO);
        stats.setEsCritico(stats.getTotalFallos() >= UMBRAL_FALLOS_CRITICO);
    }
    
    public double calcularDificultadPaquete(String paqueteId, Paquete paquete, ContextoProblema contexto) {
        EstadisticasPaquete stats = estadisticasPaquetes.get(paqueteId);
        if (stats == null) {
            stats = new EstadisticasPaquete();
            estadisticasPaquetes.put(paqueteId, stats);
        }
        
        double dificultad = 0.0;
        
        dificultad += stats.getTotalFallos() * 10.0;
        dificultad += stats.getFallosRecientes() * 5.0;
        
        if (paquete != null) {
            Aeropuerto origen = contexto.getAeropuerto(paquete.getAeropuertoOrigen());
            Aeropuerto destino = contexto.getAeropuerto(paquete.getAeropuertoDestino());
            
            if (origen != null && destino != null) {
                double distancia = calcularDistanciaHaversine(origen, destino);
                dificultad += distancia * FACTOR_DIFICULTAD_DISTANCIA;
            }
        }
        
        if (paquete != null) {
            List<Vuelo> vuelosDirectos = contexto.getVuelosDesde(paquete.getAeropuertoOrigen())
                .stream()
                .filter(v -> v.getAeropuertoDestino().equals(paquete.getAeropuertoDestino()))
                .filter(v -> v.estaOperativo())
                .filter(v -> v.getPaquetesReservados() < v.getCapacidadMaxima())
                .toList();
            
            dificultad += Math.max(0, 5 - vuelosDirectos.size()) * FACTOR_DIFICULTAD_CAPACIDAD;
        }
        
        if (paquete != null) {
            dificultad -= paquete.getPrioridad() * 2.0;
        }
        
        dificultad += stats.getOperadoresFallidos().size() * 3.0;
        
        stats.setDificultadCalculada(Math.max(0, dificultad));
        return stats.getDificultadCalculada();
    }
    
    public List<String> getPaquetesProblematicos() {
        return estadisticasPaquetes.entrySet().stream()
            .filter(entry -> entry.getValue().isEsProblematico())
            .sorted(Comparator.<Map.Entry<String, EstadisticasPaquete>>comparingDouble(entry -> entry.getValue().getDificultadCalculada()).reversed())
            .map(Map.Entry::getKey)
            .toList();
    }
    
    public List<String> getPaquetesCriticos() {
        return estadisticasPaquetes.entrySet().stream()
            .filter(entry -> entry.getValue().isEsCritico())
            .sorted(Comparator.<Map.Entry<String, EstadisticasPaquete>>comparingDouble(entry -> entry.getValue().getDificultadCalculada()).reversed())
            .map(Map.Entry::getKey)
            .toList();
    }
    
    public List<String> getRecomendacionesEstrategias(String paqueteId) {
        EstadisticasPaquete stats = estadisticasPaquetes.get(paqueteId);
        if (stats == null) {
            return Arrays.asList("estrategia_estandar");
        }
        
        List<String> recomendaciones = new ArrayList<>();
        
        if (stats.getMotivosFallos().contains("capacidad_saturada")) {
            recomendaciones.add("capacity_rebalancing");
            recomendaciones.add("intelligent_repair");
        }
        
        if (stats.getMotivosFallos().contains("sin_vuelos_disponibles")) {
            recomendaciones.add("multi_scale_routes");
            recomendaciones.add("relaxed_constraints");
        }
        
        if (stats.getTotalFallos() > 5) {
            recomendaciones.add("emergency_strategy");
            recomendaciones.add("manual_intervention");
        }
        
        if (recomendaciones.isEmpty()) {
            recomendaciones.add("regret_insertion");
        }
        
        return recomendaciones;
    }
    
    public EstadisticasPaquete getEstadisticasPaquete(String paqueteId) {
        return estadisticasPaquetes.get(paqueteId);
    }
    
    public Map<String, Object> getResumenProblematicos() {
        Map<String, Object> resumen = new HashMap<>();
        
        int totalPaquetes = estadisticasPaquetes.size();
        int paquetesProblematicos = (int) estadisticasPaquetes.values().stream()
            .filter(EstadisticasPaquete::isEsProblematico)
            .count();
        int paquetesCriticos = (int) estadisticasPaquetes.values().stream()
            .filter(EstadisticasPaquete::isEsCritico)
            .count();
        
        resumen.put("total_paquetes", totalPaquetes);
        resumen.put("paquetes_problematicos", paquetesProblematicos);
        resumen.put("paquetes_criticos", paquetesCriticos);
        resumen.put("porcentaje_problematicos", totalPaquetes > 0 ? (double) paquetesProblematicos / totalPaquetes * 100 : 0.0);
        resumen.put("porcentaje_criticos", totalPaquetes > 0 ? (double) paquetesCriticos / totalPaquetes * 100 : 0.0);
        
        return resumen;
    }
    
    public void limpiarHistorialAntiguo() {
        long tiempoLimite = System.currentTimeMillis() - (60 * 60 * 1000); // 1 hora
        
        for (Map.Entry<String, List<FalloInfo>> entry : historialFallos.entrySet()) {
            List<FalloInfo> fallosRecientes = entry.getValue().stream()
                .filter(f -> f.getTimestamp() > tiempoLimite)
                .toList();
            
            if (fallosRecientes.isEmpty()) {
                historialFallos.remove(entry.getKey());
                estadisticasPaquetes.remove(entry.getKey());
            } else {
                historialFallos.put(entry.getKey(), fallosRecientes);
            }
        }
    }
    
    private double calcularDistanciaHaversine(Aeropuerto origen, Aeropuerto destino) {
        final double R = 6371;
        
        double lat1 = Math.toRadians(origen.getLatitud());
        double lat2 = Math.toRadians(destino.getLatitud());
        double deltaLat = Math.toRadians(destino.getLatitud() - origen.getLatitud());
        double deltaLon = Math.toRadians(destino.getLongitud() - origen.getLongitud());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    public void imprimirEstadisticasProblematicos() {
        System.out.println("=== ESTADÍSTICAS DE PAQUETES PROBLEMÁTICOS ===");
        
        Map<String, Object> resumen = getResumenProblematicos();
        System.out.println("Total paquetes: " + resumen.get("total_paquetes"));
        System.out.println("Paquetes problemáticos: " + resumen.get("paquetes_problematicos") + 
                          " (" + String.format("%.1f", (Double) resumen.get("porcentaje_problematicos")) + "%)");
        System.out.println("Paquetes críticos: " + resumen.get("paquetes_criticos") + 
                          " (" + String.format("%.1f", (Double) resumen.get("porcentaje_criticos")) + "%)");
        
        // Mostrar top 5 paquetes más problemáticos
        List<String> topProblematicos = getPaquetesProblematicos().stream()
            .limit(5)
            .toList();
        
        if (!topProblematicos.isEmpty()) {
            System.out.println("\nTop 5 paquetes más problemáticos:");
            for (String paqueteId : topProblematicos) {
                EstadisticasPaquete stats = getEstadisticasPaquete(paqueteId);
                System.out.println("  • " + paqueteId + ": " + stats.getTotalFallos() + " fallos, " +
                                 "dificultad: " + String.format("%.2f", stats.getDificultadCalculada()));
            }
        }
        
        System.out.println("=============================================");
    }
}
