package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.algorithm.validation.ValidadorRestricciones;
import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.utils.MoraPackDataLoader;
import java.util.*;

/**
 * Demo para probar la validación temporal de almacenes.
 */
public class DemoValidacionTemporal {
    
    public static void main(String[] args) {
        System.out.println("🧪 DEMO: Validación Temporal de Almacenes");
        System.out.println("==========================================");
        
        try {
            // Cargar datos
            MoraPackDataLoader.DatosPrueba datos = MoraPackDataLoader.cargarDatos();
            System.out.println("✅ Datos cargados: " + datos.aeropuertos.size() + " aeropuertos, " + 
                             datos.vuelos.size() + " vuelos, " + datos.paquetes.size() + " paquetes");
            
            // Crear validador
            ValidadorRestricciones validador = new ValidadorRestricciones(
                datos.aeropuertos, datos.vuelos, datos.continentes
            );
            
            // Crear solución de prueba con muchos paquetes al mismo destino
            Solucion solucionPrueba = crearSolucionPrueba(datos);
            System.out.println("✅ Solución de prueba creada con " + solucionPrueba.getRutasPaquetes().size() + " paquetes");
            
            // Probar validación temporal
            System.out.println("\n🔍 PROBANDO VALIDACIÓN TEMPORAL:");
            boolean esValida = validador.validarCapacidadesTemporales(solucionPrueba);
            System.out.println("Resultado: " + (esValida ? "✅ VÁLIDA" : "❌ INVÁLIDA"));
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crea una solución de prueba con muchos paquetes dirigidos al mismo destino
     * para forzar la validación temporal.
     */
    private static Solucion crearSolucionPrueba(MoraPackDataLoader.DatosPrueba datos) {
        Solucion solucion = new Solucion();
        
        // Tomar los primeros 100 paquetes y dirigirlos todos a EDDI (Berlín)
        int contador = 0;
        for (Paquete paquete : datos.paquetes) {
            if (contador >= 100) break; // Limitar a 100 paquetes para la prueba
            
            // Crear ruta directa desde SPIM (Lima) a EDDI (Berlín)
            Ruta ruta = new Ruta();
            ruta.setAeropuertoOrigen("SPIM");
            ruta.setAeropuertoDestino("EDDI");
            
            // Agregar segmento de vuelo
            SegmentoRuta segmento = new SegmentoRuta();
            segmento.setAeropuertoOrigen("SPIM");
            segmento.setAeropuertoDestino("EDDI");
            segmento.setNumeroVuelo("TEST001"); // Vuelo ficticio
            segmento.setDuracionHoras(24.0); // 24 horas (intercontinental)
            segmento.setMismoContinente(false);
            
            ruta.agregarSegmento(segmento);
            ruta.recalcularMetricas();
            
            solucion.agregarRuta(paquete.getId(), ruta);
            contador++;
        }
        
        solucion.recalcularMetricas();
        return solucion;
    }
}
