package com.grupo5e.morapack;

import com.grupo5e.morapack.utils.CoordenadasUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitario para verificar el parseo de coordenadas DMS a decimal
 */
public class CoordenadasUtilsTest {

    @Test
    public void testParsearCoordenadasBogota() {
        // Bogotá: 04°42'05" N, 74°08'49" W
        String latitud = "04°42'05\" N";
        String longitud = "74°08'49\" W";
        
        double lat = CoordenadasUtils.parsearCoordenada(latitud);
        double lon = CoordenadasUtils.parsearCoordenada(longitud);
        
        System.out.println("🧪 TEST BOGOTÁ:");
        System.out.println("   Input lat: " + latitud);
        System.out.println("   Output lat: " + lat);
        System.out.println("   Esperado: ~4.7014");
        System.out.println("   Input lon: " + longitud);
        System.out.println("   Output lon: " + lon);
        System.out.println("   Esperado: ~-74.1469");
        
        // Verificar que están en el rango válido
        assertTrue(lat >= -90 && lat <= 90, "Latitud fuera de rango: " + lat);
        assertTrue(lon >= -180 && lon <= 180, "Longitud fuera de rango: " + lon);
        
        // Verificar valores aproximados (con margen de error de 0.01)
        assertEquals(4.7014, lat, 0.01, "Latitud de Bogotá incorrecta");
        assertEquals(-74.1469, lon, 0.01, "Longitud de Bogotá incorrecta");
    }

    @Test
    public void testParsearCoordenadasQuito() {
        // Quito: 00°06'48" N, 78°21'31" W
        String latitud = "00°06'48\" N";
        String longitud = "78°21'31\" W";
        
        double lat = CoordenadasUtils.parsearCoordenada(latitud);
        double lon = CoordenadasUtils.parsearCoordenada(longitud);
        
        System.out.println("🧪 TEST QUITO:");
        System.out.println("   Input lat: " + latitud);
        System.out.println("   Output lat: " + lat);
        System.out.println("   Esperado: ~0.1133");
        System.out.println("   Input lon: " + longitud);
        System.out.println("   Output lon: " + lon);
        System.out.println("   Esperado: ~-78.3586");
        
        assertTrue(lat >= -90 && lat <= 90, "Latitud fuera de rango: " + lat);
        assertTrue(lon >= -180 && lon <= 180, "Longitud fuera de rango: " + lon);
        
        assertEquals(0.1133, lat, 0.01, "Latitud de Quito incorrecta");
        assertEquals(-78.3586, lon, 0.01, "Longitud de Quito incorrecta");
    }

    @Test
    public void testParsearCoordenadasBaku() {
        // Baku: 40°28'02" N, 50°02'48" E
        String latitud = "40°28'02\" N";
        String longitud = "50°02'48\" E";
        
        double lat = CoordenadasUtils.parsearCoordenada(latitud);
        double lon = CoordenadasUtils.parsearCoordenada(longitud);
        
        System.out.println("🧪 TEST BAKU:");
        System.out.println("   Input lat: " + latitud);
        System.out.println("   Output lat: " + lat);
        System.out.println("   Esperado: ~40.4672");
        System.out.println("   Input lon: " + longitud);
        System.out.println("   Output lon: " + lon);
        System.out.println("   Esperado: ~50.0467");
        
        assertTrue(lat >= -90 && lat <= 90, "Latitud fuera de rango: " + lat);
        assertTrue(lon >= -180 && lon <= 180, "Longitud fuera de rango: " + lon);
        
        assertEquals(40.4672, lat, 0.01, "Latitud de Baku incorrecta");
        assertEquals(50.0467, lon, 0.01, "Longitud de Baku incorrecta");
    }

    @Test
    public void testParsearCoordenadasSur() {
        // Santiago: 33°23'47" S, 70°47'41" W (hemisferio sur)
        String latitud = "33°23'47\" S";
        String longitud = "70°47'41\" W";
        
        double lat = CoordenadasUtils.parsearCoordenada(latitud);
        double lon = CoordenadasUtils.parsearCoordenada(longitud);
        
        System.out.println("🧪 TEST SANTIAGO (SUR):");
        System.out.println("   Input lat: " + latitud);
        System.out.println("   Output lat: " + lat);
        System.out.println("   Esperado: ~-33.3964");
        System.out.println("   Input lon: " + longitud);
        System.out.println("   Output lon: " + lon);
        System.out.println("   Esperado: ~-70.7947");
        
        assertTrue(lat >= -90 && lat <= 90, "Latitud fuera de rango: " + lat);
        assertTrue(lon >= -180 && lon <= 180, "Longitud fuera de rango: " + lon);
        
        // Verificar que es negativo (sur)
        assertTrue(lat < 0, "Latitud sur debe ser negativa");
        assertTrue(lon < 0, "Longitud oeste debe ser negativa");
        
        assertEquals(-33.3964, lat, 0.01, "Latitud de Santiago incorrecta");
        assertEquals(-70.7947, lon, 0.01, "Longitud de Santiago incorrecta");
    }

    @Test
    public void testParsearCoordenadasConPrefijo() {
        // Con prefijo "Latitude:" y "Longitude:"
        String latitud = "Latitude: 04°42'05\" N";
        String longitud = "Longitude: 74°08'49\" W";
        
        double lat = CoordenadasUtils.parsearCoordenada(latitud);
        double lon = CoordenadasUtils.parsearCoordenada(longitud);
        
        System.out.println("🧪 TEST CON PREFIJOS:");
        System.out.println("   Input lat: " + latitud);
        System.out.println("   Output lat: " + lat);
        System.out.println("   Input lon: " + longitud);
        System.out.println("   Output lon: " + lon);
        
        assertTrue(lat >= -90 && lat <= 90, "Latitud fuera de rango: " + lat);
        assertTrue(lon >= -180 && lon <= 180, "Longitud fuera de rango: " + lon);
        
        assertEquals(4.7014, lat, 0.01, "Latitud con prefijo incorrecta");
        assertEquals(-74.1469, lon, 0.01, "Longitud con prefijo incorrecta");
    }

    @Test
    public void testParsearCoordenadasInvalidas() {
        String invalida1 = "";
        String invalida2 = null;
        String invalida3 = "invalid";
        
        double result1 = CoordenadasUtils.parsearCoordenada(invalida1);
        double result2 = CoordenadasUtils.parsearCoordenada(invalida2);
        double result3 = CoordenadasUtils.parsearCoordenada(invalida3);
        
        System.out.println("🧪 TEST COORDENADAS INVÁLIDAS:");
        System.out.println("   Empty string: " + result1);
        System.out.println("   Null: " + result2);
        System.out.println("   Invalid: " + result3);
        
        assertEquals(0.0, result1, "String vacío debe retornar 0.0");
        assertEquals(0.0, result2, "Null debe retornar 0.0");
        assertEquals(0.0, result3, "String inválido debe retornar 0.0");
    }

    @Test
    public void testInterpolacion() {
        // Test de interpolación
        double lat1 = 4.7014;  // Bogotá
        double lat2 = -33.3964; // Santiago
        
        double medio = CoordenadasUtils.interpolar(lat1, lat2, 0.5);
        
        System.out.println("🧪 TEST INTERPOLACIÓN:");
        System.out.println("   Inicio: " + lat1);
        System.out.println("   Fin: " + lat2);
        System.out.println("   Medio (50%): " + medio);
        System.out.println("   Esperado: ~-14.3475");
        
        assertEquals(-14.3475, medio, 0.01, "Interpolación incorrecta");
    }
}

