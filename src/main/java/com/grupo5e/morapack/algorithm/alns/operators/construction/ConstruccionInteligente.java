package com.grupo5e.morapack.algorithm.alns.operators.construction;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorConstruccion;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;
import com.grupo5e.morapack.algorithm.alns.SedeSelector;
import com.grupo5e.morapack.algorithm.validation.ValidadorRestricciones;
import java.util.*;

/**
 * Constructor inteligente que RESPETA las restricciones de capacidad durante la construcción.
 * Si una ruta directa viola capacidades, busca rutas con conexiones.
 */
public class ConstruccionInteligente implements OperadorConstruccion {
    
    @Override
    public Solucion construir(Solucion solucionParcial, List<String> paquetesRemovidos,
                             ContextoProblema contexto, ValidadorRestricciones validador) {
        
        Solucion nuevaSolucion = solucionParcial.copiar();
        int paquetesNoRuteados = 0;
        
        // Ordenar paquetes por prioridad (alta prioridad primero)
        List<String> paquetesOrdenados = new ArrayList<>(paquetesRemovidos);
        paquetesOrdenados.sort((p1, p2) -> {
            Paquete paq1 = contexto.getPaquete(p1);
            Paquete paq2 = contexto.getPaquete(p2);
            if (paq1 != null && paq2 != null) {
                return Integer.compare(paq2.getPrioridad(), paq1.getPrioridad()); // Descendente
            }
            return 0;
        });
        
        for (String paqueteId : paquetesOrdenados) {
            Paquete paquete = contexto.getPaquete(paqueteId);
            if (paquete == null) continue;
            
            Ruta rutaValida = encontrarRutaQueRespeteCapacidades(paqueteId, paquete, contexto, validador, nuevaSolucion);
            
            if (rutaValida != null) {
                nuevaSolucion.agregarRuta(paqueteId, rutaValida);
                nuevaSolucion.recalcularMetricas();
            } else {
                paquetesNoRuteados++;
                // Log only if verbose logging is enabled
                if (com.grupo5e.morapack.algorithm.alns.ALNSConfig.getInstance().isEnableVerboseLogging()) {
                    System.out.println("No se pudo rutear " + paqueteId + " respetando capacidades");
                }
            }
        }
        
        if (paquetesNoRuteados > 0 && com.grupo5e.morapack.algorithm.alns.ALNSConfig.getInstance().isEnableVerboseLogging()) {
            System.out.println("Paquetes no ruteados por saturación: " + paquetesNoRuteados + "/" + paquetesRemovidos.size());
        }
        
        return nuevaSolucion;
    }
    
    private Ruta encontrarRutaQueRespeteCapacidades(String paqueteId, Paquete paquete, 
                                                   ContextoProblema contexto, ValidadorRestricciones validador,
                                                   Solucion solucionActual) {
        
        // CORRECCIÓN: En MoraPack, los paquetes no tienen origen predefinido
        // El origen se determina dinámicamente desde las sedes
        String destino = paquete.getAeropuertoDestino();
        
        if (destino == null) {
            if (com.grupo5e.morapack.algorithm.alns.ALNSConfig.getInstance().isEnableVerboseLogging()) {
                System.out.println("⚠️ Paquete " + paqueteId + " sin destino definido");
            }
            return null;
        }
        
        // CORRECCIÓN: Asignar origen dinámicamente desde las sedes si es null
        String origen = paquete.getAeropuertoOrigen();
        if (origen == null) {
            SedeSelector sedeSelector = new SedeSelector(contexto);
            origen = sedeSelector.seleccionarMejorSede(destino, java.time.LocalDateTime.now(), 1);
            if (origen == null) {
                return null; // No se encontró sede válida
            }
        }
        
        // 1. Intentar ruta directa
        List<Vuelo> vuelosDirectos = contexto.getVuelosDirectos(origen, destino);
        for (Vuelo vuelo : vuelosDirectos) {
            Ruta rutaDirecta = crearRutaDirecta(vuelo, origen, destino);
            
            if (validador.esRutaFactible(paqueteId, rutaDirecta, solucionActual)) {
                return rutaDirecta;
            }
        }
        
        // 2. Si directa no funciona, buscar con 1 conexión
        List<String> rutaBFS = contexto.encontrarRutaMasCorta(origen, destino);
        if (rutaBFS.size() >= 3) { // Origen -> Conexión -> Destino
            Ruta rutaConConexion = crearRutaConConexiones(rutaBFS, contexto);
            
            if (rutaConConexion != null && validador.esRutaFactible(paqueteId, rutaConConexion, solucionActual)) {
                if (com.grupo5e.morapack.algorithm.alns.ALNSConfig.getInstance().isEnableVerboseLogging()) {
                    System.out.println("🔀 " + paqueteId + ": Ruta con conexión " + origen + " → " + rutaBFS.get(1) + " → " + destino);
                }
                return rutaConConexion;
            }
        }
        
        // 3. Si aún no funciona, buscar rutas alternativas usando diferentes aeropuertos intermedios
        List<String> aeropuertosIntermedios = encontrarAeropuertosViables(origen, destino, contexto);
        for (String intermedio : aeropuertosIntermedios) {
            Ruta rutaAlternativa = crearRutaViaTramite(origen, intermedio, destino, contexto);
            
            if (rutaAlternativa != null && validador.esRutaFactible(paqueteId, rutaAlternativa, solucionActual)) {
                if (com.grupo5e.morapack.algorithm.alns.ALNSConfig.getInstance().isEnableVerboseLogging()) {
                    System.out.println("🛤️  " + paqueteId + ": Ruta alternativa " + origen + " → " + intermedio + " → " + destino);
                }
                return rutaAlternativa;
            }
        }
        
        // 4. No se pudo rutear respetando capacidades
        return null;
    }
    
    private Ruta crearRutaDirecta(Vuelo vuelo, String origen, String destino) {
        Ruta ruta = new Ruta("ruta_directa_" + System.currentTimeMillis(), "temp_paquete");
        
        SegmentoRuta segmento = new SegmentoRuta("seg_" + System.currentTimeMillis(), 
                                               origen, destino, vuelo.getNumeroVuelo(), 
                                               vuelo.isMismoContinente());
        ruta.agregarSegmento(segmento);
        
        return ruta;
    }
    
    private Ruta crearRutaConConexiones(List<String> aeropuertos, ContextoProblema contexto) {
        Ruta ruta = new Ruta("ruta_conexiones_" + System.currentTimeMillis(), "temp_paquete");
        
        for (int i = 0; i < aeropuertos.size() - 1; i++) {
            String origenSegmento = aeropuertos.get(i);
            String destinoSegmento = aeropuertos.get(i + 1);
            
            List<Vuelo> vuelosSegmento = contexto.getVuelosDirectos(origenSegmento, destinoSegmento);
            if (vuelosSegmento.isEmpty()) {
                return null; // No hay vuelo para este segmento
            }
            
            // Tomar el primer vuelo disponible (se podría optimizar)
            Vuelo vuelo = vuelosSegmento.get(0);
            SegmentoRuta segmento = new SegmentoRuta("seg_" + i + "_" + System.currentTimeMillis(),
                                                   origenSegmento, destinoSegmento, vuelo.getNumeroVuelo(),
                                                   vuelo.isMismoContinente());
            ruta.agregarSegmento(segmento);
        }
        
        return ruta;
    }
    
    private Ruta crearRutaViaTramite(String origen, String tramite, String destino, ContextoProblema contexto) {
        List<Vuelo> vuelos1 = contexto.getVuelosDirectos(origen, tramite);
        List<Vuelo> vuelos2 = contexto.getVuelosDirectos(tramite, destino);
        
        if (vuelos1.isEmpty() || vuelos2.isEmpty()) {
            return null;
        }
        
        
        Ruta ruta = new Ruta("ruta_tramite_" + System.currentTimeMillis(), "temp_paquete");
        
        // Primer segmento: origen → trámite
        Vuelo vuelo1 = vuelos1.get(0);
        SegmentoRuta segmento1 = new SegmentoRuta("seg1_" + System.currentTimeMillis(),
                                                 origen, tramite, vuelo1.getNumeroVuelo(),
                                                 vuelo1.isMismoContinente());
        ruta.agregarSegmento(segmento1);
        
        // Segundo segmento: trámite → destino
        Vuelo vuelo2 = vuelos2.get(0);
        SegmentoRuta segmento2 = new SegmentoRuta("seg2_" + System.currentTimeMillis(),
                                                 tramite, destino, vuelo2.getNumeroVuelo(),
                                                 vuelo2.isMismoContinente());
        ruta.agregarSegmento(segmento2);
        
        return ruta;
    }
    
    private List<String> encontrarAeropuertosViables(String origen, String destino, ContextoProblema contexto) {
        List<String> viables = new ArrayList<>();
        
        // Buscar aeropuertos que tengan conexiones tanto desde origen como hacia destino
        Set<String> todosAeropuertos = new HashSet<>();
        for (Vuelo vuelo : contexto.getTodosVuelos()) {
            todosAeropuertos.add(vuelo.getAeropuertoOrigen());
            todosAeropuertos.add(vuelo.getAeropuertoDestino());
        }
        
        for (String aeropuerto : todosAeropuertos) {
            if (!aeropuerto.equals(origen) && !aeropuerto.equals(destino)) {
                boolean tieneDesdeOrigen = !contexto.getVuelosDirectos(origen, aeropuerto).isEmpty();
                boolean tieneHaciaDestino = !contexto.getVuelosDirectos(aeropuerto, destino).isEmpty();
                
                if (tieneDesdeOrigen && tieneHaciaDestino) {
                    viables.add(aeropuerto);
                }
            }
        }
        
        return viables;
    }
    
    
    @Override
    public String getNombre() {
        return "ConstruccionInteligente";
    }
    
    @Override
    public String getDescripcion() {
        return "Constructor que respeta restricciones de capacidad y busca rutas alternativas";
    }
}
