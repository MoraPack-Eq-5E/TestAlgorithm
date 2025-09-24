package com.grupo5e.morapack.service;

import com.grupo5e.morapack.algorithm.alns.ContextoProblema;
import com.grupo5e.morapack.algorithm.alns.SedeSelector;
import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.core.model.Cliente;
import com.grupo5e.morapack.core.constants.ConstantesMoraPack;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Procesador de pedidos desde pedidos_generados.csv
 * Utiliza SedeSelector para determinar automáticamente desde qué sede MoraPack 
 * enviar cada pedido, optimizando la logística global.
 */
public class ProcesadorPedidosCSV {
    
    private static final String PEDIDOS_CSV_FILE = "data/pedidos_generados.csv";
    
    private final ContextoProblema contexto;
    private final SedeSelector sedeSelector;
    
    // Estadísticas de procesamiento
    private int pedidosProcesados = 0;
    private int paquetesGenerados = 0;
    private Map<String, Integer> distribucionSedes = new HashMap<>();
    private Map<String, Integer> distribucionDestinos = new HashMap<>();
    
    public ProcesadorPedidosCSV(ContextoProblema contexto) {
        this.contexto = contexto;
        this.sedeSelector = new SedeSelector(contexto);
        
        // Inicializar estadísticas
        for (String sede : ConstantesMoraPack.SEDES_MORAPACK) {
            distribucionSedes.put(sede, 0);
        }
    }
    
    /**
     * Procesa el archivo pedidos_generados.csv y genera paquetes optimizados.
     * 
     * @return Lista de paquetes con sede de origen optimizada
     */
    public ResultadoProcesamiento procesarPedidos() {
        return procesarPedidos(LocalDateTime.now()); // Fecha base actual
    }
    
    /**
     * Procesa pedidos con una fecha base específica.
     * 
     * @param fechaBase Fecha base para calcular fechas de envío
     * @return Resultado del procesamiento
     */
    public ResultadoProcesamiento procesarPedidos(LocalDateTime fechaBase) {
        
        List<Paquete> paquetes = new ArrayList<>();
        List<Cliente> clientes = new ArrayList<>();
        List<PedidoProcesado> pedidosProcesados = new ArrayList<>();
        
        try {
            List<String> lineas = Files.readAllLines(Paths.get(PEDIDOS_CSV_FILE));
            
            // Saltar header
            for (int i = 1; i < lineas.size(); i++) {
                String linea = lineas.get(i).trim();
                if (linea.isEmpty()) continue;
                
                try {
                    PedidoCSV pedidoCSV = parsearPedidoCSV(linea);
                    if (pedidoCSV != null) {
                        // Procesar el pedido
                        ResultadoPedidoIndividual resultado = procesarPedidoIndividual(pedidoCSV, fechaBase);
                        
                        paquetes.addAll(resultado.paquetes);
                        if (resultado.cliente != null) {
                            clientes.add(resultado.cliente);
                        }
                        pedidosProcesados.add(resultado.pedidoProcesado);
                        
                        this.pedidosProcesados++;
                        this.paquetesGenerados += resultado.paquetes.size();
                    }
                } catch (Exception e) {
                    System.err.println("Error procesando línea " + i + ": " + linea + " - " + e.getMessage());
                }
            }
            
            if (com.grupo5e.morapack.algorithm.alns.ALNSConfig.getInstance().isEnableVerboseLogging()) {
                imprimirEstadisticas();
            }
            
        } catch (IOException e) {
            System.err.println("Error leyendo archivo de pedidos: " + e.getMessage());
            return new ResultadoProcesamiento(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 
                                            "Error de E/S: " + e.getMessage());
        }
        
        return new ResultadoProcesamiento(paquetes, clientes, pedidosProcesados, 
                                        "Procesados " + this.pedidosProcesados + " pedidos, generados " + 
                                        this.paquetesGenerados + " paquetes");
    }
    
    /**
     * Parsea una línea del CSV a estructura PedidoCSV
     */
    private PedidoCSV parsearPedidoCSV(String linea) {
        try {
            String[] partes = linea.split(",");
            if (partes.length != 6) {
                System.err.println("Línea con formato incorrecto: " + linea);
                return null;
            }
            
            int dia = Integer.parseInt(partes[0].trim());
            int hora = Integer.parseInt(partes[1].trim());
            int minuto = Integer.parseInt(partes[2].trim());
            String destino = partes[3].trim();
            int cantidad = Integer.parseInt(partes[4].trim());
            String clienteId = partes[5].trim();
            
            return new PedidoCSV(dia, hora, minuto, destino, cantidad, clienteId);
            
        } catch (NumberFormatException e) {
            System.err.println("Error parseando números en: " + linea + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Procesa un pedido individual y genera paquetes optimizados
     */
    private ResultadoPedidoIndividual procesarPedidoIndividual(PedidoCSV pedidoCSV, LocalDateTime fechaBase) {
        
        // Calcular fecha de envío
        LocalDateTime fechaEnvio = fechaBase.withDayOfMonth(pedidoCSV.dia)
                                          .withHour(pedidoCSV.hora)
                                          .withMinute(pedidoCSV.minuto)
                                          .withSecond(0);
        
        // Seleccionar sede óptima usando SedeSelector
        String sedeOptima = sedeSelector.seleccionarMejorSede(
            pedidoCSV.destino, fechaEnvio, pedidoCSV.cantidad
        );
        
        // Actualizar estadísticas
        distribucionSedes.put(sedeOptima, distribucionSedes.get(sedeOptima) + 1);
        distribucionDestinos.put(pedidoCSV.destino, 
                               distribucionDestinos.getOrDefault(pedidoCSV.destino, 0) + 1);
        
        // Crear cliente si no existe
        Cliente cliente = crearOActualizarCliente(pedidoCSV.clienteId, pedidoCSV.destino);
        
        // Generar paquetes individuales
        List<Paquete> paquetes = new ArrayList<>();
        for (int i = 0; i < pedidoCSV.cantidad; i++) {
            String paqueteId = String.format("PKG_%s_%03d_%02d", 
                                           pedidoCSV.clienteId, pedidoCSV.cantidad, i + 1);
            
            Paquete paquete = new Paquete(paqueteId, sedeOptima, pedidoCSV.destino, pedidoCSV.clienteId);
            
            // Configurar prioridad basado en tiempo de envío
            int prioridad = calcularPrioridad(fechaEnvio);
            paquete.setPrioridad(prioridad);
            
            // Establecer fecha límite basado en continentes
            configurarFechaLimite(paquete, sedeOptima, pedidoCSV.destino, fechaEnvio);
            
            paquetes.add(paquete);
        }
        
        // Crear pedido procesado para seguimiento
        PedidoProcesado pedidoProcesado = new PedidoProcesado(
            generarIdPedido(pedidoCSV), pedidoCSV.clienteId, sedeOptima, pedidoCSV.destino,
            pedidoCSV.cantidad, fechaEnvio, paquetes.stream().map(Paquete::getId).toList()
        );
        
        return new ResultadoPedidoIndividual(paquetes, cliente, pedidoProcesado);
    }
    
    /**
     * Crea o actualiza información del cliente
     */
    private Cliente crearOActualizarCliente(String clienteId, String destino) {
        // Generar información básica del cliente
        String nombre = "Cliente " + clienteId;
        String email = "cliente" + clienteId + "@morapack.com";
        
        Cliente cliente = new Cliente(clienteId, nombre, email, destino);
        
        // Los clientes con IDs bajos son VIP (simulación)
        try {
            long idNumerico = Long.parseLong(clienteId);
            cliente.setClienteVIP(idNumerico < 1000000); // Primeros millón son VIP
        } catch (NumberFormatException e) {
            cliente.setClienteVIP(false);
        }
        
        return cliente;
    }
    
    /**
     * Calcula prioridad basado en tiempo de envío
     */
    private int calcularPrioridad(LocalDateTime fechaEnvio) {
        int hora = fechaEnvio.getHour();
        
        if (hora >= 0 && hora < 8) {
            return 1; // Envío nocturno/madrugada = urgente
        } else if (hora >= 8 && hora < 18) {
            return 2; // Horario comercial = normal
        } else {
            return 3; // Horario vespertino = baja prioridad
        }
    }
    
    /**
     * Configura fecha límite basado en reglas de MoraPack
     */
    private void configurarFechaLimite(Paquete paquete, String origen, String destino, LocalDateTime fechaEnvio) {
        String continenteOrigen = contexto.obtenerContinente(origen);
        String continenteDestino = contexto.obtenerContinente(destino);
        
        boolean mismoContinente = continenteOrigen != null && continenteOrigen.equals(continenteDestino);
        int diasPlazo = ConstantesMoraPack.obtenerPlazoDias(mismoContinente);
        
        LocalDateTime fechaLimite = fechaEnvio.plusDays(diasPlazo);
        paquete.setFechaLimiteEntrega(fechaLimite);
    }
    
    /**
     * Genera ID único para el pedido
     */
    private String generarIdPedido(PedidoCSV pedido) {
        return String.format("PED_%02d%02d%02d_%s_%03d_%s", 
                           pedido.dia, pedido.hora, pedido.minuto,
                           pedido.destino, pedido.cantidad, pedido.clienteId);
    }
    
    /**
     * Imprime estadísticas del procesamiento
     */
    private void imprimirEstadisticas() {
        System.out.println("\n=== ESTADÍSTICAS DE PROCESAMIENTO DE PEDIDOS ===");
        System.out.println("Pedidos procesados: " + pedidosProcesados);
        System.out.println("Paquetes generados: " + paquetesGenerados);
        
        System.out.println("\n📍 Distribución por sedes de origen:");
        for (String sede : ConstantesMoraPack.SEDES_MORAPACK) {
            int cantidad = distribucionSedes.get(sede);
            double porcentaje = (cantidad * 100.0) / pedidosProcesados;
            String nombreSede = switch(sede) {
                case "SPIM" -> "Lima";
                case "EBCI" -> "Bruselas"; 
                case "UBBB" -> "Baku";
                default -> sede;
            };
            System.out.printf("  - %s (%s): %d pedidos (%.1f%%)%n", nombreSede, sede, cantidad, porcentaje);
        }
        
        System.out.println("\n🎯 Top 10 destinos más solicitados:");
        distribucionDestinos.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> System.out.printf("  - %s: %d pedidos%n", entry.getKey(), entry.getValue()));
        
        // Estadísticas del SedeSelector
        System.out.println("\n🧠 Eficiencia del SedeSelector:");
        Map<String, Integer> usoSedes = sedeSelector.obtenerEstadisticasUso();
        usoSedes.forEach((sede, uso) -> 
            System.out.printf("  - %s: %d selecciones%n", sede, uso)
        );
    }
    
    // ========================== CLASES INTERNAS ==========================
    
    /**
     * Representa un pedido parseado del CSV
     */
    private static class PedidoCSV {
        final int dia, hora, minuto;
        final String destino;
        final int cantidad;
        final String clienteId;
        
        PedidoCSV(int dia, int hora, int minuto, String destino, int cantidad, String clienteId) {
            this.dia = dia;
            this.hora = hora;
            this.minuto = minuto;
            this.destino = destino;
            this.cantidad = cantidad;
            this.clienteId = clienteId;
        }
    }
    
    /**
     * Resultado del procesamiento de un pedido individual
     */
    private static class ResultadoPedidoIndividual {
        final List<Paquete> paquetes;
        final Cliente cliente;
        final PedidoProcesado pedidoProcesado;
        
        ResultadoPedidoIndividual(List<Paquete> paquetes, Cliente cliente, PedidoProcesado pedidoProcesado) {
            this.paquetes = paquetes;
            this.cliente = cliente;
            this.pedidoProcesado = pedidoProcesado;
        }
    }
    
    /**
     * Información de un pedido ya procesado
     */
    public static class PedidoProcesado {
        public final String id;
        public final String clienteId;
        public final String sedeOrigen;
        public final String destino;
        public final int cantidad;
        public final LocalDateTime fechaEnvio;
        public final List<String> paqueteIds;
        
        public PedidoProcesado(String id, String clienteId, String sedeOrigen, String destino,
                             int cantidad, LocalDateTime fechaEnvio, List<String> paqueteIds) {
            this.id = id;
            this.clienteId = clienteId;
            this.sedeOrigen = sedeOrigen;
            this.destino = destino;
            this.cantidad = cantidad;
            this.fechaEnvio = fechaEnvio;
            this.paqueteIds = paqueteIds;
        }
        
        @Override
        public String toString() {
            return String.format("%s: %s→%s (%d paquetes) para cliente %s", 
                               id, sedeOrigen, destino, cantidad, clienteId);
        }
    }
    
    /**
     * Resultado completo del procesamiento
     */
    public static class ResultadoProcesamiento {
        public final List<Paquete> paquetes;
        public final List<Cliente> clientes;
        public final List<PedidoProcesado> pedidos;
        public final String resumen;
        
        public ResultadoProcesamiento(List<Paquete> paquetes, List<Cliente> clientes, 
                                    List<PedidoProcesado> pedidos, String resumen) {
            this.paquetes = paquetes;
            this.clientes = clientes;
            this.pedidos = pedidos;
            this.resumen = resumen;
        }
    }
}
