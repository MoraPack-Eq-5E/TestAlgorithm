package com.grupo5e.morapack.utils;

import com.grupo5e.morapack.core.enums.Continente;
import com.grupo5e.morapack.core.enums.EstadoProducto;
import com.grupo5e.morapack.core.enums.EstadoPedido;
import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.service.PedidoService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LectorPedidos {
    private final String rutaArchivo;
    private final ArrayList<Aeropuerto> aeropuertos;
    private final Map<String, Aeropuerto> mapaAeropuertos;
    private final Random aleatorio;

    // Usamos el PedidoService en lugar de repositorios individuales
    private final PedidoService pedidoService;

    public LectorPedidos(String rutaArchivo,
                         ArrayList<Aeropuerto> aeropuertos,
                         PedidoService pedidoService) {
        this.rutaArchivo = rutaArchivo;
        this.aeropuertos = aeropuertos;
        this.pedidoService = pedidoService;
        this.aleatorio = new Random();
        this.mapaAeropuertos = crearMapaAeropuertos();
    }

    private Map<String, Aeropuerto> crearMapaAeropuertos() {
        Map<String, Aeropuerto> mapa = new HashMap<>();
        for (Aeropuerto a : aeropuertos) {
            if (a.getCodigoIATA() != null) {
                mapa.put(a.getCodigoIATA().trim().toUpperCase(), a);
            }
        }
        return mapa;
    }

    public void leerYGuardarProductos() {
        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;

            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty()) {
                    continue;
                }

                String[] partes = linea.trim().split("\\s+");
                if (partes.length >= 6) {
                    procesarLineaProducto(partes);
                }
            }

            System.out.println("Proceso de carga de paquetes completado exitosamente.");

        } catch (IOException e) {
            System.err.println("Error leyendo datos de productos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void procesarLineaProducto(String[] partes) {
        int diasPrioridad = Integer.parseInt(partes[0]);
        int hora = Integer.parseInt(partes[1]);
        int minuto = Integer.parseInt(partes[2]);
        String codigoAeropuertoDestino = partes[3].trim().toUpperCase();
        int cantidadProductos = Integer.parseInt(partes[4]);
        Long idCliente = (long) Integer.parseInt(partes[5]);

        Aeropuerto aeropuertoDestino = mapaAeropuertos.get(codigoAeropuertoDestino);

        if (aeropuertoDestino != null) {
            // Crear cliente (asumiendo que el ClienteService manejará la persistencia)
            Cliente cliente = crearCliente(idCliente, aeropuertoDestino.getCiudad());

            // Calcular fechas
            LocalDateTime fechaPedido = calcularFechaPedido(hora, minuto);
            LocalDateTime plazoEntrega = calcularPlazoEntrega(diasPrioridad, fechaPedido);

            // Crear pedido
            Pedido pedido = crearPedido(cliente, aeropuertoDestino, fechaPedido, plazoEntrega);

            // Crear productos
            ArrayList<Producto> productos = crearProductos(cantidadProductos, pedido);
            pedido.setProductos(productos);

            // Guardar paquete usando el servicio
            try {
                Long idPaqueteGuardado = pedidoService.insertar(pedido);
                System.out.println("Paquete guardado con ID: " + idPaqueteGuardado);
            } catch (Exception e) {
                System.err.println("Error al guardar paquete: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Aeropuerto no encontrado: " + codigoAeropuertoDestino);
        }
    }

    private Cliente crearCliente(Long idCliente, Ciudad ciudadRecojo) {
        Cliente cliente = new Cliente();
        cliente.setId(idCliente);
        cliente.setNombres("Cliente " + idCliente);
        cliente.setCorreo("cliente" + idCliente + "@ejemplo.com");
        cliente.setCiudadRecojo(ciudadRecojo);
        return cliente;
    }

    private LocalDateTime calcularFechaPedido(int hora, int minuto) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaPedido = ahora.withHour(hora).withMinute(minuto).withSecond(0).withNano(0);

        if (fechaPedido.isBefore(ahora)) {
            fechaPedido = fechaPedido.plusDays(1);
        }

        return fechaPedido;
    }

    private LocalDateTime calcularPlazoEntrega(int diasPrioridad, LocalDateTime fechaPedido) {
        switch (diasPrioridad) {
            case 1:  return fechaPedido.plus(1, ChronoUnit.DAYS);
            case 4:  return fechaPedido.plus(4, ChronoUnit.DAYS);
            case 12: return fechaPedido.plus(12, ChronoUnit.DAYS);
            case 24: return fechaPedido.plus(24, ChronoUnit.DAYS);
            default: return fechaPedido.plus(7, ChronoUnit.DAYS);
        }
    }

    private Pedido crearPedido(Cliente cliente, Aeropuerto aeropuertoDestino,
                                 LocalDateTime fechaPedido, LocalDateTime plazoEntrega) {
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setAeropuertoDestinoCodigo(aeropuertoDestino.getCodigoIATA());
        pedido.setFechaPedido(fechaPedido);
        pedido.setFechaLimiteEntrega(plazoEntrega);
        pedido.setEstado(EstadoPedido.PENDIENTE);

        // Establecer aeropuerto actual (almacén inicial)
        Aeropuerto aeropuertoActual = obtenerAeropuertoAlmacenAleatorio(aeropuertoDestino.getCiudad().getContinente());
        pedido.setAeropuertoOrigenCodigo(aeropuertoActual.getCodigoIATA());

        // Calcular y establecer prioridad
        double prioridad = calcularPrioridad(fechaPedido, plazoEntrega);
        pedido.setPrioridad(prioridad);

        return pedido;
    }

    private ArrayList<Producto> crearProductos(int cantidad, Pedido pedido) {
        ArrayList<Producto> productos = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            Producto producto = new Producto();
            producto.setEstado(EstadoProducto.EN_ALMACEN);
            producto.setPedido(pedido);
            productos.add(producto);
        }
        return productos;
    }

    private Aeropuerto obtenerAeropuertoAlmacenAleatorio(Continente continenteDestino) {
        ArrayList<Aeropuerto> almacenesMoraPack = new ArrayList<>();

        for (Aeropuerto aeropuerto : aeropuertos) {
            Ciudad ciudad = aeropuerto.getCiudad();
            String nombreCiudad = ciudad.getNombre().toLowerCase();

            if (nombreCiudad.contains("lima") || nombreCiudad.contains("bruselas") || nombreCiudad.contains("baku")) {
                if (ciudad.getContinente() != continenteDestino) {
                    almacenesMoraPack.add(aeropuerto);
                }
            }
        }

        if (almacenesMoraPack.isEmpty()) {
            for (Aeropuerto aeropuerto : aeropuertos) {
                Ciudad ciudad = aeropuerto.getCiudad();
                String nombreCiudad = ciudad.getNombre().toLowerCase();

                if (nombreCiudad.contains("lima") || nombreCiudad.contains("bruselas") || nombreCiudad.contains("baku")) {
                    almacenesMoraPack.add(aeropuerto);
                }
            }
        }

        if (almacenesMoraPack.isEmpty()) {
            System.err.println("Advertencia: No se encontraron almacenes MoraPack, usando respaldo");
            for (Aeropuerto aeropuerto : aeropuertos) {
                if (aeropuerto.getCiudad().getNombre().toLowerCase().contains("lima")) {
                    return aeropuerto;
                }
            }
            return aeropuertos.get(0); // Último respaldo
        }

        return almacenesMoraPack.get(aleatorio.nextInt(almacenesMoraPack.size()));
    }

    private double calcularPrioridad(LocalDateTime fechaPedido, LocalDateTime plazoEntrega) {
        long horas = ChronoUnit.HOURS.between(fechaPedido, plazoEntrega);

        if (horas <= 24) {
            return 1.0;
        } else if (horas <= 96) {
            return 0.75;
        } else if (horas <= 288) {
            return 0.5;
        } else {
            return 0.25;
        }
    }

    // Método adicional para verificar los paquetes guardados
    public void listarPedidosGuardados() {
        try {
            var pedidos = pedidoService.listar();
            System.out.println("Total de pedidos en sistema: " + pedidos.size());
            for (Pedido pedido : pedidos) {
                System.out.println("Paquete ID: " + pedido.getId() +
                        ", Cliente: " + pedido.getCliente().getNombres() +
                        ", Estado: " + pedido.getEstado());
            }
        } catch (Exception e) {
            System.err.println("Error al listar paquetes: " + e.getMessage());
        }
    }
}