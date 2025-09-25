package com.grupo5e.morapack.utils;

import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.constants.Constants;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LectorProductos {

    private ArrayList<Paquete> productos;
    private final String filePath;
    private ArrayList<Aeropuerto> aeropuertos;

    public LectorProductos() {
        this.filePath = Constants.PRODUCTS_FILE_PATH;
        this.productos = new ArrayList<>();
    }

    public LectorProductos(String filePath, ArrayList<Aeropuerto> aeropuertos) {
        this.filePath = filePath;
        this.productos = new ArrayList<>();
        this.aeropuertos = aeropuertos;
    }

    public ArrayList<Paquete> leerProductos() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int idProducto = 1;
            Map<String, Aeropuerto> mapaAeropuertos = crearMapaAeropuertos();

            // Saltar la primera línea (header)
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                // Saltar líneas vacías
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Parsear datos del producto
                // Formato: dd\thh\tmm\tdest\t###\tIdClien (separado por tabulaciones)
                String[] partes = line.split("\\t");
                if (partes.length == 6) {
                    int dia = Integer.parseInt(partes[0]);
                    int hora = Integer.parseInt(partes[1]);
                    int minuto = Integer.parseInt(partes[2]);
                    String codigoDestino = partes[3];
                    int cantidad = Integer.parseInt(partes[4]);
                    String idCliente = partes[5];

                    // Buscar aeropuerto de destino por código IATA
                    Aeropuerto aeropuertoDestino = mapaAeropuertos.get(codigoDestino);

                    if (aeropuertoDestino != null) {
                        // Crear objeto Paquete usando el constructor correcto
                        Paquete paquete = new Paquete();
                        paquete.setId(String.valueOf(idProducto++));
                        paquete.setAeropuertoOrigen("LIM"); // Aeropuerto de origen por defecto (Lima)
                        paquete.setAeropuertoDestino(codigoDestino);
                        paquete.setClienteId(idCliente);
                        paquete.setFechaLimiteEntrega(calcularFechaEntrega(dia, hora, minuto));
                        paquete.setEstado(com.grupo5e.morapack.core.enums.EstadoGeneral.CREADO);

                        productos.add(paquete);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error leyendo datos de productos: " + e.getMessage());
            e.printStackTrace();
        }

        return productos;
    }

    private Map<String, Aeropuerto> crearMapaAeropuertos() {
        Map<String, Aeropuerto> mapa = new HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            mapa.put(aeropuerto.getCodigoIATA(), aeropuerto);
        }
        return mapa;
    }

    private LocalDateTime calcularFechaEntrega(int dia, int hora, int minuto) {
        // Calcular fecha de entrega basada en día, hora y minuto
        LocalDateTime fechaBase = LocalDateTime.now();
        return fechaBase.plusDays(dia).withHour(hora).withMinute(minuto).withSecond(0).withNano(0);
    }
}