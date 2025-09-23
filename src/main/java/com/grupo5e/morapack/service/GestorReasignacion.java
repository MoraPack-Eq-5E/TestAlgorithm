package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.enums.EstadoGeneral;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestor de reasignación de productos/paquetes
 * Maneja la reasignación según ubicación del producto según las especificaciones
 */
public class GestorReasignacion {
    
    private final Map<String, Paquete> paquetes;
    private final Map<String, Pedido> pedidos;
    private final List<ReasignacionEvento> historialReasignaciones;
    
    public GestorReasignacion(Map<String, Paquete> paquetes, Map<String, Pedido> pedidos) {
        this.paquetes = paquetes;
        this.pedidos = pedidos;
        this.historialReasignaciones = new ArrayList<>();
    }
    
    /**
     * Reasigna un paquete a un nuevo cliente y destino
     */
    public ResultadoReasignacion reasignarPaquete(String paqueteId, String nuevoClienteId, 
                                                 String nuevoDestino, String nuevoPedidoId) {
        Paquete paquete = paquetes.get(paqueteId);
        
        if (paquete == null) {
            return new ResultadoReasignacion(false, "Paquete no encontrado: " + paqueteId);
        }
        
        // Verificar si el paquete puede ser reasignado
        if (!paquete.puedeSerReasignado()) {
            return new ResultadoReasignacion(false, 
                "El paquete " + paqueteId + " no puede ser reasignado en su estado actual: " + 
                paquete.getEstado() + " (tipo almacén: " + paquete.getTipoAlmacen() + ")");
        }
        
        // Guardar información original para el historial
        String clienteOriginal = paquete.getClienteId();
        String destinoOriginal = paquete.getAeropuertoDestino();
        String pedidoOriginal = paquete.getPedidoOriginalId();
        
        // Intentar reasignar
        boolean reasignado = paquete.reasignar(nuevoClienteId, nuevoDestino, nuevoPedidoId);
        
        if (reasignado) {
            // Registrar en historial
            ReasignacionEvento evento = new ReasignacionEvento(
                paqueteId,
                clienteOriginal,
                nuevoClienteId,
                destinoOriginal,
                nuevoDestino,
                pedidoOriginal,
                nuevoPedidoId,
                LocalDateTime.now(),
                paquete.getEstado().toString(),
                paquete.getTipoAlmacen()
            );
            historialReasignaciones.add(evento);
            
            // Actualizar el pedido original si existe
            if (pedidoOriginal != null) {
                Pedido pedidoOriginalObj = pedidos.get(pedidoOriginal);
                if (pedidoOriginalObj != null) {
                    pedidoOriginalObj.removerPaquete(paqueteId);
                }
            }
            
            // Agregar al nuevo pedido si existe
            if (nuevoPedidoId != null) {
                Pedido nuevoPedido = pedidos.get(nuevoPedidoId);
                if (nuevoPedido != null) {
                    nuevoPedido.agregarPaquete(paqueteId);
                }
            }
            
            System.out.println("Paquete " + paqueteId + " reasignado exitosamente");
            System.out.println("   Cliente: " + clienteOriginal + " -> " + nuevoClienteId);
            System.out.println("   Destino: " + destinoOriginal + " -> " + nuevoDestino);
            System.out.println("   Pedido: " + pedidoOriginal + " -> " + nuevoPedidoId);
            
            return new ResultadoReasignacion(true, "Paquete reasignado exitosamente");
        } else {
            return new ResultadoReasignacion(false, "No se pudo reasignar el paquete");
        }
    }
    
    /**
     * Reasigna múltiples paquetes a un nuevo destino
     */
    public List<ResultadoReasignacion> reasignarPaquetesMasiva(List<String> paqueteIds, 
                                                              String nuevoClienteId, 
                                                              String nuevoDestino, 
                                                              String nuevoPedidoId) {
        List<ResultadoReasignacion> resultados = new ArrayList<>();
        
        System.out.println("🔄 Reasignando " + paqueteIds.size() + " paquetes masivamente...");
        
        for (String paqueteId : paqueteIds) {
            ResultadoReasignacion resultado = reasignarPaquete(paqueteId, nuevoClienteId, nuevoDestino, nuevoPedidoId);
            resultados.add(resultado);
        }
        
        int exitosas = (int) resultados.stream().filter(ResultadoReasignacion::isExitoso).count();
        System.out.println("   Reasignaciones exitosas: " + exitosas + "/" + paqueteIds.size());
        
        return resultados;
    }
    
    /**
     * Obtiene paquetes que pueden ser reasignados
     */
    public List<Paquete> getPaquetesReasignables() {
        return paquetes.values().stream()
            .filter(Paquete::puedeSerReasignado)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene paquetes reasignables por ubicación
     */
    public Map<String, List<Paquete>> getPaquetesReasignablesPorUbicacion() {
        return getPaquetesReasignables().stream()
            .collect(Collectors.groupingBy(Paquete::getAeropuertoActual));
    }
    
    /**
     * Obtiene paquetes reasignables por estado
     */
    public Map<EstadoGeneral, List<Paquete>> getPaquetesReasignablesPorEstado() {
        return getPaquetesReasignables().stream()
            .collect(Collectors.groupingBy(Paquete::getEstado));
    }
    
    /**
     * Obtiene paquetes reasignables por tipo de almacén
     */
    public Map<String, List<Paquete>> getPaquetesReasignablesPorTipoAlmacen() {
        return getPaquetesReasignables().stream()
            .collect(Collectors.groupingBy(Paquete::getTipoAlmacen));
    }
    
    /**
     * Simula reasignación por cancelación de vuelo
     */
    public List<ResultadoReasignacion> simularReasignacionPorCancelacionVuelo(String numeroVuelo, 
                                                                             String nuevoClienteId, 
                                                                             String nuevoDestino, 
                                                                             String nuevoPedidoId) {
        // Buscar paquetes que estaban en ese vuelo (simulado)
        List<Paquete> paquetesEnVuelo = paquetes.values().stream()
            .filter(p -> p.getEstado() == EstadoGeneral.EN_TRANSITO)
            .filter(p -> p.getAeropuertoActual().contains(numeroVuelo)) // Simulación
            .collect(Collectors.toList());
        
        if (paquetesEnVuelo.isEmpty()) {
            return Arrays.asList(new ResultadoReasignacion(false, 
                "No se encontraron paquetes en el vuelo " + numeroVuelo));
        }
        
        System.out.println("🛫 Reasignando paquetes del vuelo cancelado " + numeroVuelo);
        System.out.println("   Paquetes afectados: " + paquetesEnVuelo.size());
        
        List<String> paqueteIds = paquetesEnVuelo.stream()
            .map(Paquete::getId)
            .collect(Collectors.toList());
        
        return reasignarPaquetesMasiva(paqueteIds, nuevoClienteId, nuevoDestino, nuevoPedidoId);
    }
    
    /**
     * Optimiza reasignaciones por proximidad geográfica
     */
    public List<ResultadoReasignacion> optimizarReasignacionesPorProximidad(String aeropuertoOrigen) {
        // Obtener paquetes reasignables en el aeropuerto
        List<Paquete> paquetesEnAeropuerto = getPaquetesReasignablesPorUbicacion().get(aeropuertoOrigen);
        
        if (paquetesEnAeropuerto == null || paquetesEnAeropuerto.isEmpty()) {
            return Arrays.asList(new ResultadoReasignacion(false, 
                "No hay paquetes reasignables en el aeropuerto " + aeropuertoOrigen));
        }
        
        System.out.println("🌍 Optimizando reasignaciones por proximidad en " + aeropuertoOrigen);
        System.out.println("   Paquetes disponibles: " + paquetesEnAeropuerto.size());
        
        List<ResultadoReasignacion> resultados = new ArrayList<>();
        
        // Agrupar por destino original para optimizar
        Map<String, List<Paquete>> paquetesPorDestino = paquetesEnAeropuerto.stream()
            .collect(Collectors.groupingBy(Paquete::getAeropuertoDestino));
        
        for (Map.Entry<String, List<Paquete>> entry : paquetesPorDestino.entrySet()) {
            String destinoOriginal = entry.getKey();
            List<Paquete> paquetesDestino = entry.getValue();
            
            // Buscar pedidos que van al mismo destino
            List<Pedido> pedidosMismoDestino = pedidos.values().stream()
                .filter(p -> destinoOriginal.equals(p.getAeropuertoDestino()))
                .filter(p -> !p.estaCompleto())
                .collect(Collectors.toList());
            
            if (!pedidosMismoDestino.isEmpty()) {
                Pedido pedidoOptimizado = pedidosMismoDestino.get(0);
                
                for (Paquete paquete : paquetesDestino) {
                    ResultadoReasignacion resultado = reasignarPaquete(
                        paquete.getId(),
                        pedidoOptimizado.getClienteId(),
                        pedidoOptimizado.getAeropuertoDestino(),
                        pedidoOptimizado.getId()
                    );
                    resultados.add(resultado);
                }
            }
        }
        
        return resultados;
    }
    
    /**
     * Obtiene estadísticas de reasignaciones
     */
    public EstadisticasReasignacion getEstadisticas() {
        int totalReasignaciones = historialReasignaciones.size();
        int paquetesReasignables = getPaquetesReasignables().size();
        int paquetesNoReasignables = paquetes.size() - paquetesReasignables;
        
        // Contar por tipo de ubicación
        Map<String, Long> reasignacionesPorUbicacion = historialReasignaciones.stream()
            .collect(Collectors.groupingBy(ReasignacionEvento::getTipoAlmacen, Collectors.counting()));
        
        return new EstadisticasReasignacion(
            totalReasignaciones,
            paquetesReasignables,
            paquetesNoReasignables,
            reasignacionesPorUbicacion
        );
    }
    
    /**
     * Genera reporte de reasignaciones
     */
    public void generarReporte() {
        System.out.println("\n🔄 REPORTE DE REASIGNACIONES");
        System.out.println("=" .repeat(60));
        
        EstadisticasReasignacion stats = getEstadisticas();
        
        System.out.println("Estadísticas Generales:");
        System.out.println("   Total de reasignaciones: " + stats.getTotalReasignaciones());
        System.out.println("   Paquetes reasignables: " + stats.getPaquetesReasignables());
        System.out.println("   Paquetes no reasignables: " + stats.getPaquetesNoReasignables());
        
        System.out.println("\n📍 Reasignaciones por Ubicación:");
        stats.getReasignacionesPorUbicacion().forEach((ubicacion, cantidad) -> {
            System.out.println("   " + ubicacion + ": " + cantidad + " reasignaciones");
        });
        
        System.out.println("\n🔄 Paquetes Reasignables por Estado:");
        Map<EstadoGeneral, List<Paquete>> paquetesPorEstado = getPaquetesReasignablesPorEstado();
        if (paquetesPorEstado.isEmpty()) {
            System.out.println("   No hay paquetes reasignables");
        } else {
            paquetesPorEstado.forEach((estado, paquetes) -> {
                System.out.println("   " + estado + ": " + paquetes.size() + " paquetes");
            });
        }
        
        System.out.println("\n🏢 Paquetes Reasignables por Tipo de Almacén:");
        Map<String, List<Paquete>> paquetesPorTipo = getPaquetesReasignablesPorTipoAlmacen();
        if (paquetesPorTipo.isEmpty()) {
            System.out.println("   No hay paquetes reasignables");
        } else {
            paquetesPorTipo.forEach((tipo, paquetes) -> {
                System.out.println("   " + tipo + ": " + paquetes.size() + " paquetes");
            });
        }
        
        System.out.println("\n🌍 Paquetes Reasignables por Ubicación:");
        Map<String, List<Paquete>> paquetesPorUbicacion = getPaquetesReasignablesPorUbicacion();
        if (paquetesPorUbicacion.isEmpty()) {
            System.out.println("   No hay paquetes reasignables");
        } else {
            paquetesPorUbicacion.forEach((ubicacion, paquetes) -> {
                System.out.println("   " + ubicacion + ": " + paquetes.size() + " paquetes");
            });
        }
    }
    
    /**
     * Clase para eventos de reasignación
     */
    public static class ReasignacionEvento {
        private final String paqueteId;
        private final String clienteOriginal;
        private final String clienteNuevo;
        private final String destinoOriginal;
        private final String destinoNuevo;
        private final String pedidoOriginal;
        private final String pedidoNuevo;
        private final LocalDateTime fechaHora;
        private final String estadoPaquete;
        private final String tipoAlmacen;
        
        public ReasignacionEvento(String paqueteId, String clienteOriginal, String clienteNuevo,
                                 String destinoOriginal, String destinoNuevo, String pedidoOriginal,
                                 String pedidoNuevo, LocalDateTime fechaHora, String estadoPaquete,
                                 String tipoAlmacen) {
            this.paqueteId = paqueteId;
            this.clienteOriginal = clienteOriginal;
            this.clienteNuevo = clienteNuevo;
            this.destinoOriginal = destinoOriginal;
            this.destinoNuevo = destinoNuevo;
            this.pedidoOriginal = pedidoOriginal;
            this.pedidoNuevo = pedidoNuevo;
            this.fechaHora = fechaHora;
            this.estadoPaquete = estadoPaquete;
            this.tipoAlmacen = tipoAlmacen;
        }
        
        // Getters
        public String getPaqueteId() { return paqueteId; }
        public String getClienteOriginal() { return clienteOriginal; }
        public String getClienteNuevo() { return clienteNuevo; }
        public String getDestinoOriginal() { return destinoOriginal; }
        public String getDestinoNuevo() { return destinoNuevo; }
        public String getPedidoOriginal() { return pedidoOriginal; }
        public String getPedidoNuevo() { return pedidoNuevo; }
        public LocalDateTime getFechaHora() { return fechaHora; }
        public String getEstadoPaquete() { return estadoPaquete; }
        public String getTipoAlmacen() { return tipoAlmacen; }
        
        @Override
        public String toString() {
            return String.format("Reasignación[%s: %s->%s, %s->%s]", 
                               paqueteId, clienteOriginal, clienteNuevo, destinoOriginal, destinoNuevo);
        }
    }
    
    /**
     * Clase para resultados de reasignación
     */
    public static class ResultadoReasignacion {
        private final boolean exitoso;
        private final String mensaje;
        
        public ResultadoReasignacion(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }
        
        public boolean isExitoso() { return exitoso; }
        public String getMensaje() { return mensaje; }
        
        @Override
        public String toString() {
            return (exitoso ? "OK" : "ERROR") + " " + mensaje;
        }
    }
    
    /**
     * Clase para estadísticas de reasignaciones
     */
    public static class EstadisticasReasignacion {
        private final int totalReasignaciones;
        private final int paquetesReasignables;
        private final int paquetesNoReasignables;
        private final Map<String, Long> reasignacionesPorUbicacion;
        
        public EstadisticasReasignacion(int totalReasignaciones, int paquetesReasignables, 
                                      int paquetesNoReasignables, Map<String, Long> reasignacionesPorUbicacion) {
            this.totalReasignaciones = totalReasignaciones;
            this.paquetesReasignables = paquetesReasignables;
            this.paquetesNoReasignables = paquetesNoReasignables;
            this.reasignacionesPorUbicacion = reasignacionesPorUbicacion;
        }
        
        // Getters
        public int getTotalReasignaciones() { return totalReasignaciones; }
        public int getPaquetesReasignables() { return paquetesReasignables; }
        public int getPaquetesNoReasignables() { return paquetesNoReasignables; }
        public Map<String, Long> getReasignacionesPorUbicacion() { return reasignacionesPorUbicacion; }
    }
}
