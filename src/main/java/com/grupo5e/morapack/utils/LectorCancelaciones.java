package com.grupo5e.morapack.utils;

import com.grupo5e.morapack.core.model.Cancelacion;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lector de cancelaciones de vuelos desde archivo.
 *
 * Formato esperado del archivo:
 * dd.ORIGEN-DESTINO-HH:MM
 *
 * Ejemplo: 01.SKBO-SEQM-03:34
 */
public class LectorCancelaciones {

    private final String rutaArchivo;

    public LectorCancelaciones(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    /**
     * Lee el archivo de cancelaciones y retorna una lista de objetos Cancelacion.
     *
     * @return Lista de cancelaciones cargadas desde el archivo.
     */
    public List<Cancelacion> leerCancelaciones() {
        List<Cancelacion> cancelaciones = new ArrayList<>();
        int lineasLeidas = 0;
        int lineasValidas = 0;
        int lineasInvalidas = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;

            while ((linea = reader.readLine()) != null) {
                lineasLeidas++;

                // Saltar líneas vacías o comentarios
                if (linea.trim().isEmpty() || linea.trim().startsWith("#")) {
                    continue;
                }

                try {
                    // Parsear línea: dd.ORIGEN-DESTINO-HH:MM
                    String[] partes = linea.trim().split("\\.", 2);
                    if (partes.length != 2) {
                        System.err.println("Formato inválido en línea " + lineasLeidas + ": " + linea);
                        lineasInvalidas++;
                        continue;
                    }

                    int dia = Integer.parseInt(partes[0].trim());
                    if (dia < 1 || dia > 365) {
                        System.err.println("Día fuera de rango en línea " + lineasLeidas + ": " + dia);
                        lineasInvalidas++;
                        continue;
                    }

                    String identificador = partes[1].trim(); // ORIGEN-DESTINO-HH:MM
                    if (!validarFormatoIdentificador(identificador)) {
                        System.err.println("Identificador de vuelo inválido en línea " + lineasLeidas + ": " + identificador);
                        lineasInvalidas++;
                        continue;
                    }

                    // Extraer partes
                    String[] vueloPartes = identificador.split("-");
                    String codigoIATAOrigen = vueloPartes[0];
                    String codigoIATADestino = vueloPartes[1];

                    // Hora está al final: HH:MM
                    String horaCompleta = vueloPartes[2];
                    String[] horaPartes = horaCompleta.split(":");
                    int hora = Integer.parseInt(horaPartes[0]);
                    int minuto = Integer.parseInt(horaPartes[1]);

                    Cancelacion cancelacion = new Cancelacion(
                            dia,
                            codigoIATAOrigen,
                            codigoIATADestino,
                            hora,
                            minuto
                    );

                    cancelaciones.add(cancelacion);
                    lineasValidas++;

                } catch (NumberFormatException e) {
                    System.err.println("Error numérico en línea " + lineasLeidas + ": " + linea);
                    lineasInvalidas++;
                } catch (Exception e) {
                    System.err.println("Error procesando línea " + lineasLeidas + ": " + linea);
                    e.printStackTrace();
                    lineasInvalidas++;
                }
            }

        } catch (IOException e) {
            System.err.println("Error leyendo archivo de cancelaciones: " + e.getMessage());
        }

        System.out.println("=== Lectura de Cancelaciones ===");
        System.out.println("Archivo: " + rutaArchivo);
        System.out.println("Líneas procesadas: " + lineasLeidas);
        System.out.println("Líneas válidas: " + lineasValidas);
        System.out.println("Líneas inválidas: " + lineasInvalidas);
        System.out.println("Cancelaciones totales: " + cancelaciones.size());

        return cancelaciones;
    }

    /**
     * Valida el formato del identificador: CODIGO-CODIGO-HH:MM
     */
    private boolean validarFormatoIdentificador(String identificador) {
        if (identificador == null || identificador.isEmpty()) {
            return false;
        }
        String[] partes = identificador.split("-");
        if (partes.length != 3) {
            return false;
        }
        String ultimaParte = partes[2];
        return ultimaParte.matches("\\d{2}:\\d{2}");
    }
}
