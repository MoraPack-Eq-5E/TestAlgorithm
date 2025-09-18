package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.EstadoGeneral;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Pedido {
    private String id;
    private String clienteId;
    private LocalDateTime fechaPedido;
    private LocalDateTime fechaLimiteEntrega;
    private String aeropuertoDestino;
    private List<String> paqueteIds; // Lista de paquetes que pertenecen a este pedido
    private EstadoGeneral estado;
    private int cantidadProductosMPE;
    private int prioridadPedido; // 1 = alta, 2 = media, 3 = baja
    private String sedeOrigenAsignada; // Lima, Bruselas o Baku
    
    // Nuevos campos para formato de archivos (especificaciones)
    private String archivoOrigen; // Archivo del cual proviene el pedido
    private int numeroLinea; // Número de línea en el archivo
    private String formatoArchivo; // Formato: dd-hh-mm-dest-###-IdClien
    
    public Pedido(String id, String clienteId, String aeropuertoDestino, int cantidadProductosMPE) {
        this.id = id;
        this.clienteId = clienteId;
        this.fechaPedido = LocalDateTime.now();
        this.aeropuertoDestino = aeropuertoDestino;
        this.cantidadProductosMPE = cantidadProductosMPE;
        this.paqueteIds = new ArrayList<>();
        this.estado = EstadoGeneral.CREADO;
        this.prioridadPedido = 2; // Prioridad media por defecto
        
        // Calcular fecha límite (se puede mejorar con lógica de continentes)
        this.fechaLimiteEntrega = fechaPedido.plusDays(3); // Default 3 días
        
        // Inicializar nuevos campos
        this.archivoOrigen = null;
        this.numeroLinea = 0;
        this.formatoArchivo = null;
    }
    
    /**
     * Constructor específico para pedidos creados desde archivos
     * Formato: dd-hh-mm-dest-###-IdClien
     */
    public Pedido(String id, String clienteId, String aeropuertoDestino, int cantidadProductosMPE,
                  LocalDateTime fechaHoraPedido, String archivoOrigen, int numeroLinea) {
        this.id = id;
        this.clienteId = clienteId;
        this.fechaPedido = fechaHoraPedido; // Fecha específica del archivo
        this.aeropuertoDestino = aeropuertoDestino;
        this.cantidadProductosMPE = cantidadProductosMPE;
        this.paqueteIds = new ArrayList<>();
        this.estado = EstadoGeneral.CREADO;
        this.prioridadPedido = 2; // Prioridad media por defecto
        
        // Campos específicos del archivo
        this.archivoOrigen = archivoOrigen;
        this.numeroLinea = numeroLinea;
        this.formatoArchivo = "dd-hh-mm-dest-###-IdClien";
        
        // Calcular fecha límite (máximo 3 días para vuelos entre continentes)
        this.fechaLimiteEntrega = fechaPedido.plusDays(3);
    }
    
    public void agregarPaquete(String paqueteId) {
        if (!paqueteIds.contains(paqueteId)) {
            paqueteIds.add(paqueteId);
        }
    }
    
    public void removerPaquete(String paqueteId) {
        paqueteIds.remove(paqueteId);
    }
    
    public int getCantidadPaquetes() {
        return paqueteIds.size();
    }
    
    public boolean estaCompleto() {
        return cantidadProductosMPE == paqueteIds.size();
    }
    
    public boolean estaEnPlazo() {
        return LocalDateTime.now().isBefore(fechaLimiteEntrega);
    }
    
    public long getDiasRestantes() {
        return java.time.Duration.between(LocalDateTime.now(), fechaLimiteEntrega).toDays();
    }
    
    public void calcularFechaLimite(boolean mismoContinente) {
        int diasPlazo = mismoContinente ? 2 : 3;
        this.fechaLimiteEntrega = fechaPedido.plusDays(diasPlazo);
    }
    
    public void asignarSedeOptima(String continenteDestino) {
        // Lógica simplificada para asignar la sede más cercana
        switch (continenteDestino.toLowerCase()) {
            case "america":
                this.sedeOrigenAsignada = "LIM"; // Lima
                break;
            case "europa":
                this.sedeOrigenAsignada = "BRU"; // Bruselas
                break;
            case "asia":
                this.sedeOrigenAsignada = "BAK"; // Baku
                break;
            default:
                this.sedeOrigenAsignada = "LIM"; // Lima por defecto
        }
    }
    
    // Métodos para validación de formato de archivos (nuevas especificaciones)
    
    /**
     * Valida si el formato del archivo es correcto
     * Formato esperado: dd-hh-mm-dest-###-IdClien
     */
    public static boolean validarFormatoArchivo(String linea) {
        if (linea == null || linea.trim().isEmpty() || linea.startsWith("#")) {
            return false; // Línea vacía o comentario
        }
        
        String[] partes = linea.trim().split("-");
        if (partes.length != 6) {
            return false; // Debe tener exactamente 6 partes
        }
        
        try {
            // Validar día (01-31)
            int dia = Integer.parseInt(partes[0]);
            if (dia < 1 || dia > 31) return false;
            
            // Validar hora (00-23)
            int hora = Integer.parseInt(partes[1]);
            if (hora < 0 || hora > 23) return false;
            
            // Validar minutos (00-59)
            int minutos = Integer.parseInt(partes[2]);
            if (minutos < 0 || minutos > 59) return false;
            
            // Validar código aeropuerto (4 caracteres)
            String codigoAeropuerto = partes[3];
            if (codigoAeropuerto.length() != 4) return false;
            
            // Validar cantidad productos (001-999)
            int cantidad = Integer.parseInt(partes[4]);
            if (cantidad < 1 || cantidad > 999) return false;
            
            // Validar ID cliente (debe empezar con CLI)
            String idCliente = partes[5];
            if (!idCliente.startsWith("CLI")) return false;
            
            return true;
            
        } catch (NumberFormatException e) {
            return false; // Error al parsear números
        }
    }
    
    /**
     * Crea un pedido desde una línea de archivo
     */
    public static Pedido crearDesdeArchivo(String linea, String archivoOrigen, int numeroLinea) {
        if (!validarFormatoArchivo(linea)) {
            throw new IllegalArgumentException("Formato de archivo inválido: " + linea);
        }
        
        String[] partes = linea.trim().split("-");
        
        // Parsear componentes
        int dia = Integer.parseInt(partes[0]);
        int hora = Integer.parseInt(partes[1]);
        int minutos = Integer.parseInt(partes[2]);
        String codigoDestino = partes[3];
        int cantidadProductos = Integer.parseInt(partes[4]);
        String idCliente = partes[5];
        
        // Crear fecha (asumiendo mes actual y año actual)
        LocalDateTime fechaHoraPedido = LocalDateTime.now()
            .withDayOfMonth(dia)
            .withHour(hora)
            .withMinute(minutos)
            .withSecond(0)
            .withNano(0);
        
        // Crear ID del pedido
        String idPedido = "PED_" + String.format("%04d", numeroLinea);
        
        return new Pedido(idPedido, idCliente, codigoDestino, cantidadProductos,
                         fechaHoraPedido, archivoOrigen, numeroLinea);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pedido pedido = (Pedido) o;
        return Objects.equals(id, pedido.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Pedido[%s: Cliente %s, %d MPE -> %s, Estado: %s]", 
                           id, clienteId, cantidadProductosMPE, aeropuertoDestino, estado);
    }
}
