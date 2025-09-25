package com.grupo5e.morapack.utils;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.core.model.Almacen;
import com.grupo5e.morapack.core.enums.Continente;
import com.grupo5e.morapack.core.enums.EstadoAeropuerto;
import com.grupo5e.morapack.core.constants.Constants;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LectorAeropuerto {

    private ArrayList<Aeropuerto> aeropuertos;
    private final String filePath;

    public LectorAeropuerto() {
        this.filePath = Constants.AIRPORT_INFO_FILE_PATH;
        this.aeropuertos = new ArrayList<>();
    }

    public LectorAeropuerto(String filePath) {
        this.filePath = filePath;
        this.aeropuertos = new ArrayList<>();
    }

    public ArrayList<Aeropuerto> leerAeropuertos() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            Continente continenteActual = null;
            Map<String, Ciudad> mapaCiudades = new HashMap<>();

            // Saltar las primeras dos líneas (header)
            reader.readLine();
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                
                // Saltar líneas vacías
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Verificar si es una línea de header de continente
                if (line.contains("America") || line.contains("Europa") || line.contains("Asia")) {
                    if (line.contains("America")) {
                        continenteActual = Continente.AMERICA;
                        System.out.println("Continente: " + continenteActual);
                    } else if (line.contains("Europa")) {
                        continenteActual = Continente.EUROPA;
                        System.out.println("Continente: " + continenteActual);
                    } else if (line.contains("Asia")) {
                        continenteActual = Continente.ASIA;
                        System.out.println("Continente: " + continenteActual);
                    }
                    continue;
                }
                
                // Parsear datos del aeropuerto
                String[] partes = line.trim().split("\\s+");
                
                if (partes.length >= 7) {
                    int id = Integer.parseInt(partes[0]);
                    String codigoIATA = partes[1];
                    
                    // Extraer nombre de la ciudad (puede contener múltiples palabras)
                    int finNombreCiudad = 3;
                    while (!partes[finNombreCiudad].contains("GMT") && 
                           !Character.isDigit(partes[finNombreCiudad].charAt(0))) {
                        finNombreCiudad++;
                    }
                    
                    StringBuilder constructorNombreCiudad = new StringBuilder(partes[2]);
                    for (int i = 3; i < finNombreCiudad; i++) {
                        constructorNombreCiudad.append(" ").append(partes[i]);
                    }
                    String nombreCiudad = constructorNombreCiudad.toString();
                    
                    // Extraer nombre del país
                    String nombrePais = partes[finNombreCiudad];
                    
                    // Extraer alias
                    String alias = partes[finNombreCiudad + 1];
                    
                    // Extraer zona horaria
                    int zonaHoraria;
                    try {
                        zonaHoraria = Integer.parseInt(partes[5]);
                    } catch (NumberFormatException e) {
                        // Manejar problemas de formato de zona horaria
                        String tzStr = partes[5];
                        if (tzStr.startsWith("+")) {
                            zonaHoraria = Integer.parseInt(tzStr.substring(1));
                        } else if (tzStr.startsWith("-")) {
                            zonaHoraria = Integer.parseInt(tzStr);
                        } else {
                            zonaHoraria = 0; // Valor por defecto si falla el parsing
                            System.out.println("Advertencia: No se pudo parsear zona horaria para " + codigoIATA + ", usando valor por defecto 0");
                        }
                    }
                    
                    // Extraer capacidad
                    double capacidadMaxima;
                    try {
                        capacidadMaxima = Double.parseDouble(partes[6]);
                    } catch (NumberFormatException e) {
                        capacidadMaxima = 400.0; // Capacidad por defecto
                        System.out.println("Advertencia: No se pudo parsear capacidad para " + codigoIATA + ", usando valor por defecto 400.0");
                    }
                    
                    // Extraer latitud y longitud
                    String latitudStr = "";
                    String longitudStr = "";
                    
                    // Buscar latitud y longitud en la línea
                    int indiceLat = line.indexOf("Latitude:");
                    int indiceLong = line.indexOf("Longitude:");
                    
                    if (indiceLat != -1 && indiceLong != -1) {
                        latitudStr = line.substring(indiceLat + 10, indiceLong).trim();
                        longitudStr = line.substring(indiceLong + 11).trim();
                        
                        // Limpiar caracteres especiales de latitud y longitud
                        latitudStr = latitudStr.replaceAll("[°'\"NSEW]", "").trim();
                        longitudStr = longitudStr.replaceAll("[°'\"NSEW]", "").trim();
                    }
                    
                    // Crear objeto Ciudad si no existe
                    String claveCiudad = nombreCiudad + "-" + nombrePais;
                    Ciudad ciudad = mapaCiudades.get(claveCiudad);
                    if (ciudad == null) {
                        ciudad = new Ciudad();
                        ciudad.setId(mapaCiudades.size() + 1);
                        ciudad.setNombre(nombreCiudad);
                        ciudad.setContinente(continenteActual);
                        mapaCiudades.put(claveCiudad, ciudad);
                    }
                    
                    // Crear Almacén para el aeropuerto
                    Almacen almacen = new Almacen();
                    almacen.setId(id);
                    almacen.setCapacidadMaxima((int)capacidadMaxima);
                    almacen.setCapacidadUsada(0);
                    almacen.setNombre(nombreCiudad + " Almacén");
                    almacen.setEsPrincipal(false);
                    
                    // Crear objeto Aeropuerto
                    Aeropuerto aeropuerto = new Aeropuerto();
                    aeropuerto.setId(id);
                    aeropuerto.setCodigoIATA(codigoIATA);
                    aeropuerto.setAlias(alias);
                    aeropuerto.setZonaHorariaUTC(zonaHoraria);
                    aeropuerto.setLatitud(latitudStr);
                    aeropuerto.setLongitud(longitudStr);
                    aeropuerto.setCiudad(ciudad);
                    aeropuerto.setEstado(EstadoAeropuerto.DISPONIBLE);
                    aeropuerto.setAlmacen(almacen);
                    
                    // Establecer referencia circular
                    almacen.setAeropuerto(aeropuerto);
                    
                    aeropuertos.add(aeropuerto);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error leyendo datos de aeropuertos: " + e.getMessage());
            e.printStackTrace();
        }
        
        return aeropuertos;
    }
}
