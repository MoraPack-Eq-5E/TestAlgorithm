package com.grupo5e.morapack.utils;

import com.grupo5e.morapack.core.enums.EstadoProducto;
import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.core.enums.Continente;
import com.grupo5e.morapack.core.model.Cliente;
import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.core.enums.EstadoPaquete;
import com.grupo5e.morapack.core.model.Producto;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LectorProductos {
    private ArrayList<Paquete> paquetes;
    private final String rutaArchivo;
    private ArrayList<Aeropuerto> aeropuertos;
    private Map<String, Aeropuerto> mapaAeropuertos;
    private Random aleatorio;
    private int idProducto = 1;

    public LectorProductos(String rutaArchivo, ArrayList<Aeropuerto> aeropuertos) {
        this.rutaArchivo = rutaArchivo;
        this.paquetes = new ArrayList<>();
        this.aeropuertos = aeropuertos;
        this.aleatorio = new Random();
        crearMapaAeropuertos();
    }

    private void crearMapaAeropuertos() {
        this.mapaAeropuertos = new HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            mapaAeropuertos.put(aeropuerto.getCodigo(), aeropuerto);
        }
    }

    public ArrayList<Paquete> leerProductos() {
        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            Long idPaquete = 1L;
            
            while ((linea = reader.readLine()) != null) {
                // Saltar líneas vacías
                if (linea.trim().isEmpty()) {
                    continue;
                }
                
                // Parsear datos del producto
                String[] partes = linea.trim().split("\\s+");
                if (partes.length >= 6) {
                    int diasPrioridad = Integer.parseInt(partes[0]);
                    int hora = Integer.parseInt(partes[1]);
                    int minuto = Integer.parseInt(partes[2]);
                    String codigoAeropuertoDestino = partes[3];
                    int cantidadProductos = Integer.parseInt(partes[4]); // Cantidad de productos en el paquete
                    Long idCliente = (long) Integer.parseInt(partes[5]); // ID del cliente
                    
                    // Buscar aeropuerto de destino
                    Aeropuerto aeropuertoDestino = mapaAeropuertos.get(codigoAeropuertoDestino);
                    
                    if (aeropuertoDestino != null) {
                        // Crear cliente
                        Cliente cliente = new Cliente();
                        cliente.setId(idCliente);
                        cliente.setNombres("Cliente " + idCliente);
                        cliente.setCorreo("cliente" + idCliente + "@ejemplo.com");
                        cliente.setCiudadRecojo(aeropuertoDestino.getCiudad());
                        
                        // Calcular fecha de pedido y plazo de entrega
                        LocalDateTime ahora = LocalDateTime.now();
                        LocalDateTime fechaPedido = ahora.withHour(hora).withMinute(minuto).withSecond(0).withNano(0);
                        
                        // Si la fechaPedido está en el pasado, establecerla para mañana
                        if (fechaPedido.isBefore(ahora)) {
                            fechaPedido = fechaPedido.plusDays(1);
                        }
                        
                        // Establecer plazo de entrega basado en días de prioridad
                        LocalDateTime plazoEntrega;
                        switch (diasPrioridad) {
                            case 1:  // Prioridad más alta - 1 día
                                plazoEntrega = fechaPedido.plus(1, ChronoUnit.DAYS);
                                break;
                            case 4:  // Prioridad media - 4 días
                                plazoEntrega = fechaPedido.plus(4, ChronoUnit.DAYS);
                                break;
                            case 12: // Prioridad baja - 12 días
                                plazoEntrega = fechaPedido.plus(12, ChronoUnit.DAYS);
                                break;
                            case 24: // Prioridad más baja - 24 días
                                plazoEntrega = fechaPedido.plus(24, ChronoUnit.DAYS);
                                break;
                            default: // Por defecto 7 días
                                plazoEntrega = fechaPedido.plus(7, ChronoUnit.DAYS);
                                break;
                        }
                        
                        // Crear objeto Paquete
                        Paquete paquete = new Paquete();
                        paquete.setId(idPaquete+1);
                        paquete.setCliente(cliente);
                        paquete.setCiudadDestino(aeropuertoDestino.getCiudad());
                        paquete.setFechaPedido(fechaPedido);
                        paquete.setFechaLimiteEntrega(plazoEntrega);
                        paquete.setEstado(EstadoPaquete.PENDIENTE);
                        
                        // Crear productos para este paquete
                        ArrayList<Producto> productos = new ArrayList<>();
                        for (int i = 0; i < cantidadProductos; i++) {
                            Producto producto = new Producto();
                            producto.setId(idProducto+1L);
                            producto.setEstado(EstadoProducto.EN_ALMACEN); // Producto no asignado inicialmente
                            productos.add(producto);
                        }
                        paquete.setProductos(productos);
                        
                        // Asumir que el paquete comienza en un almacén aleatorio en un continente diferente
                        Ciudad ubicacionActual = obtenerUbicacionAlmacenAleatoria(aeropuertoDestino.getCiudad().getContinente());
                        paquete.setUbicacionActual(ubicacionActual);
                        
                        // Establecer prioridad basada en ventana de tiempo de entrega
                        double valorPrioridad = calcularPrioridad(fechaPedido, plazoEntrega);
                        paquete.setPrioridad(valorPrioridad);
                        
                        paquetes.add(paquete);
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error leyendo datos de productos: " + e.getMessage());
            e.printStackTrace();
        }
        
        return paquetes;
    }
    
    private Ciudad obtenerUbicacionAlmacenAleatoria(Continente continenteDestino) {
        // MoraPack tiene sedes en Lima (Perú), Bruselas (Bélgica) y Bakú (Azerbaiyán)
        // Los paquetes deben comenzar desde una de estas tres ubicaciones con stock ilimitado
        
        ArrayList<Ciudad> almacenesMoraPack = new ArrayList<>();
        
        // Buscar ciudades de almacenes MoraPack
        for (Aeropuerto aeropuerto : aeropuertos) {
            Ciudad ciudad = aeropuerto.getCiudad();
            String nombreCiudad = ciudad.getNombre();
            
            if (nombreCiudad.equals("Lima") || nombreCiudad.equals("Bruselas") || nombreCiudad.equals("Baku") ||
                nombreCiudad.contains("Lima") || nombreCiudad.contains("Bruselas") || nombreCiudad.contains("Baku")) {
                // Preferir almacenes en continente diferente al destino para maximizar cobertura
                if (ciudad.getContinente() != continenteDestino) {
                    almacenesMoraPack.add(ciudad);
                }
            }
        }
        
        // Si no hay almacenes en continente diferente, permitir cualquier almacén MoraPack
        if (almacenesMoraPack.isEmpty()) {
            for (Aeropuerto aeropuerto : aeropuertos) {
                Ciudad ciudad = aeropuerto.getCiudad();
                String nombreCiudad = ciudad.getNombre();
                
                if (nombreCiudad.equals("Lima") || nombreCiudad.equals("Bruselas") || nombreCiudad.equals("Baku") ||
                    nombreCiudad.contains("Lima") || nombreCiudad.contains("Bruselas") || nombreCiudad.contains("Baku")) {
                    almacenesMoraPack.add(ciudad);
                }
            }
        }
        
        // Si de alguna manera no se encuentran almacenes MoraPack (no debería pasar), usar Lima como respaldo
        if (almacenesMoraPack.isEmpty()) {
            System.err.println("Advertencia: No se encontraron almacenes MoraPack, usando respaldo");
            for (Aeropuerto aeropuerto : aeropuertos) {
                if (aeropuerto.getCiudad().getNombre().contains("Lima")) {
                    return aeropuerto.getCiudad();
                }
            }
        }
        
        // Retornar almacén MoraPack aleatorio
        return almacenesMoraPack.get(aleatorio.nextInt(almacenesMoraPack.size()));
    }
    
    private double calcularPrioridad(LocalDateTime fechaPedido, LocalDateTime plazoEntrega) {
        // Calcular prioridad basada en ventana de tiempo
        long horas = ChronoUnit.HOURS.between(fechaPedido, plazoEntrega);
        
        // Normalizar prioridad: ventanas de entrega más cortas obtienen mayor prioridad (1.0 es la más alta)
        if (horas <= 24) {
            return 1.0; // Prioridad más alta para entrega de 1 día
        } else if (horas <= 96) {
            return 0.75; // Alta prioridad para entrega de 4 días
        } else if (horas <= 288) {
            return 0.5; // Prioridad media para entrega de 12 días
        } else {
            return 0.25; // Prioridad baja para entrega de 24 días
        }
    }
}
