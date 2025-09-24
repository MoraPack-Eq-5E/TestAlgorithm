package com.grupo5e.morapack.algorithm.alns.operators.construction;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorConstruccion;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;
import com.grupo5e.morapack.algorithm.validation.ValidadorRestricciones;
import com.grupo5e.morapack.core.constants.ConstantesMoraPack;
import java.util.*;

/**
 * Constructor ALNS puro que decide dinámicamente desde qué sede MoraPack enviar cada paquete.
 * Evalúa las 3 sedes (Lima, Bruselas, Baku) y elige la mejor para cada paquete.
 */
public class ConstruccionMultiDepot implements OperadorConstruccion {
    
    @Override
    public Solucion construir(Solucion solucionParcial, List<String> paquetesRemovidos,
                             ContextoProblema contexto, ValidadorRestricciones validador) {
        
        
        Solucion nuevaSolucion = solucionParcial.copiar();
        
        // Agregar aleatoriedad para exploración ALNS
        List<String> paquetesOrdenados = new ArrayList<>(paquetesRemovidos);
        
        // 70% de las veces: ordenar por prioridad (explotación)
        // 30% de las veces: orden aleatorio (exploración)
        if (Math.random() < 0.7) {
            paquetesOrdenados.sort((p1, p2) -> {
                Paquete paq1 = contexto.getPaquete(p1);
                Paquete paq2 = contexto.getPaquete(p2);
                if (paq1 != null && paq2 != null) {
                    return Integer.compare(paq1.getPrioridad(), paq2.getPrioridad()); // Ascendente (1=alta)
                }
                return 0;
            });
        } else {
            Collections.shuffle(paquetesOrdenados); // Orden aleatorio para exploración
        }
        
        int procesados = 0;
        for (String paqueteId : paquetesOrdenados) {
            Paquete paquete = contexto.getPaquete(paqueteId);
            if (paquete == null) continue;


            // ALNS PURO: Evaluar todas las sedes MoraPack dinámicamente
            Ruta mejorRuta = encontrarMejorRutaMultiDepot(paqueteId, paquete, contexto, validador, nuevaSolucion);

            if (mejorRuta != null) {
                reservarCapacidadEnVuelos(mejorRuta, contexto);
                nuevaSolucion.agregarRuta(paqueteId, mejorRuta);
            }
            procesados++;
        }
        
        return nuevaSolucion;
    }
    
    /**
     * Encuentra la mejor ruta evaluando todas las sedes MoraPack dinámicamente
     */
    private Ruta encontrarMejorRutaMultiDepot(String paqueteId, Paquete paquete, ContextoProblema contexto, 
                                            ValidadorRestricciones validador, Solucion solucionActual) {
        
        String destino = paquete.getAeropuertoDestino();
        List<EvaluacionRuta> evaluaciones = new ArrayList<>();
        
        
        // Evaluar cada sede de MoraPack
        for (String sede : ConstantesMoraPack.SEDES_MORAPACK) {
            List<Ruta> rutasCandidatas = generarRutasCandidatas(sede, destino, contexto);
            
            for (Ruta ruta : rutasCandidatas) {
                if (validador.esRutaFactible(paqueteId, ruta, solucionActual)) {
                    double puntuacion = evaluarRuta(ruta, sede, destino, contexto);
                    evaluaciones.add(new EvaluacionRuta(ruta, puntuacion, sede));
                }
            }
        }
        
        if (evaluaciones.isEmpty()) {
            return null; // No hay ruta factible desde ninguna sede
        }
        
        // Ordenar por puntuación (menor es mejor para fitness)
        evaluaciones.sort(Comparator.comparing(e -> e.puntuacion));
        
        // Agregar aleatoriedad en la selección de ruta:
        // 60% de las veces: seleccionar la mejor (explotación)
        // 40% de las veces: seleccionar entre las top 3 (exploración)
        if (Math.random() < 0.6 || evaluaciones.size() == 1) {
            return evaluaciones.get(0).ruta; // Mejor ruta
        } else {
            // Seleccionar aleatoriamente entre las mejores opciones
            int topCandidatos = Math.min(3, evaluaciones.size());
            int indiceAleatorio = (int) (Math.random() * topCandidatos);
            return evaluaciones.get(indiceAleatorio).ruta;
        }
    }
    
    /**
     * Genera rutas candidatas desde una sede específica al destino
     * ENFOQUE NATURAL: Genera todas las opciones válidas y deja que las restricciones decidan
     */
    private List<Ruta> generarRutasCandidatas(String sede, String destino, ContextoProblema contexto) {
        List<Ruta> rutas = new ArrayList<>();
        
        // 1. Evaluar rutas directas (si existen)
        List<Vuelo> vuelosDirectos = contexto.getVuelosDirectos(sede, destino);
        for (Vuelo vuelo : vuelosDirectos) {
            Ruta rutaDirecta = crearRutaDirecta(vuelo, sede, destino);
            rutas.add(rutaDirecta);
        }
        
        // 2. SIEMPRE evaluar rutas con conexión como alternativa
        // Las restricciones naturales (capacidad, horarios, costos) decidirán cuál usar
        List<String> rutaBFS = contexto.encontrarRutaMasCorta(sede, destino);
        if (rutaBFS != null && rutaBFS.size() >= 3) {
            Ruta rutaConConexion = crearRutaConConexiones(rutaBFS, contexto);
            if (rutaConConexion != null) {
                rutas.add(rutaConConexion);
            }
        }
        
        return rutas;
    }
    
    /**
     * Evalúa la calidad de una ruta (menor puntuación = mejor)
     */
    private double evaluarRuta(Ruta ruta, String sede, String destino, ContextoProblema contexto) {
        double puntuacion = 0.0;
        
        // 1. Tiempo total (40% del peso)
        puntuacion += ruta.getTiempoTotalHoras() * 0.4;
        
        // 2. Número de conexiones (30% del peso) - menos conexiones es mejor
        puntuacion += (ruta.getSegmentos().size() - 1) * 10 * 0.3;
        
        // 3. Factor geográfico (20% del peso) - mismo continente es mejor
        String continenteSede = contexto.obtenerContinente(sede);
        String continenteDestino = contexto.obtenerContinente(destino);
        if (continenteSede != null && !continenteSede.equals(continenteDestino)) {
            puntuacion += 5 * 0.2; // Penalizar distinto continente
        }
        
        // 4. Costo estimado (10% del peso)
        puntuacion += ruta.getCostoTotal() * 0.1;
        
        return puntuacion;
    }
    
    /**
     * Crea una ruta directa
     */
    private Ruta crearRutaDirecta(Vuelo vuelo, String origen, String destino) {
        Ruta ruta = new Ruta("multidepot_directa_" + System.currentTimeMillis(), "");
        
        SegmentoRuta segmento = new SegmentoRuta(
            "seg_" + System.currentTimeMillis(),
            origen,
            destino,
            vuelo.getNumeroVuelo(),
            vuelo.isMismoContinente()
        );
        
        ruta.agregarSegmento(segmento);
        return ruta;
    }
    
    /**
     * Crea una ruta con conexiones
     */
    private Ruta crearRutaConConexiones(List<String> aeropuertos, ContextoProblema contexto) {
        if (aeropuertos.size() < 2) return null;
        
        Ruta ruta = new Ruta("multidepot_conexiones_" + System.currentTimeMillis(), "");
        
        for (int i = 0; i < aeropuertos.size() - 1; i++) {
            String origenSegmento = aeropuertos.get(i);
            String destinoSegmento = aeropuertos.get(i + 1);
            
            // Buscar vuelo para este segmento
            List<Vuelo> vuelosSegmento = contexto.getVuelosDirectos(origenSegmento, destinoSegmento);
            if (vuelosSegmento.isEmpty()) {
                return null; // No hay vuelo para este segmento
            }
            
            Vuelo vueloSeleccionado = vuelosSegmento.get(0);
            SegmentoRuta segmento = new SegmentoRuta(
                "seg_" + System.currentTimeMillis() + "_" + i,
                origenSegmento,
                destinoSegmento,
                vueloSeleccionado.getNumeroVuelo(),
                vueloSeleccionado.isMismoContinente()
            );
            
            ruta.agregarSegmento(segmento);
        }
        
        return ruta;
    }
    
    @Override
    public String getNombre() {
        return "ConstruccionMultiDepot";
    }
    
    @Override
    public String getDescripcion() {
        return "Constructor ALNS puro que decide dinámicamente desde qué sede MoraPack enviar cada paquete";
    }
    
    /**
     * CRÍTICO: Reserva capacidad en todos los vuelos de una ruta
     * Sin esto, las capacidades nunca se actualizan y siempre se permiten rutas directas
     */
    private void reservarCapacidadEnVuelos(Ruta ruta, ContextoProblema contexto) {
        for (SegmentoRuta segmento : ruta.getSegmentos()) {
            String numeroVuelo = segmento.getNumeroVuelo();
            
            // Buscar el vuelo en el contexto
            for (Vuelo vuelo : contexto.getTodosVuelos()) {
                if (vuelo.getNumeroVuelo().equals(numeroVuelo)) {
                    try {
                        vuelo.reservarPaquetes(1); // Reservar 1 paquete
                        System.out.println("   [DEBUG] Reservado en " + numeroVuelo + 
                                         " (" + vuelo.getPaquetesReservados() + "/" + vuelo.getCapacidadMaxima() + ")");
                    } catch (IllegalStateException e) {
                        System.out.println("   [ERROR] Vuelo " + numeroVuelo + " LLENO: " + e.getMessage());
                    }
                    break;
                }
            }
        }
        
        // ¡NUEVO! Actualizar ocupación de almacenes en aeropuertos destino
        actualizarOcupacionAlmacenes(ruta, contexto);
    }
    
    /**
     * NUEVO: Actualiza la ocupación de almacenes en aeropuertos destino
     */
    private void actualizarOcupacionAlmacenes(Ruta ruta, ContextoProblema contexto) {
        for (SegmentoRuta segmento : ruta.getSegmentos()) {
            String aeropuertoDestino = segmento.getAeropuertoDestino();
            Aeropuerto aeropuerto = contexto.getAeropuerto(aeropuertoDestino);
            
            if (aeropuerto != null) {
                try {
                    aeropuerto.agregarPaquetes(1); // Incrementar ocupación del almacén
                    System.out.println("   [ALMACEN] " + aeropuertoDestino + 
                                     " (" + aeropuerto.getPaquetesEnAlmacen() + "/" + aeropuerto.getCapacidadAlmacen() + ")");
                } catch (IllegalStateException e) {
                    System.out.println("   [ERROR] Almacén " + aeropuertoDestino + " LLENO: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Clase para evaluación de rutas
     */
    private static class EvaluacionRuta {
        final Ruta ruta;
        final double puntuacion;
        final String sede;
        
        EvaluacionRuta(Ruta ruta, double puntuacion, String sede) {
            this.ruta = ruta;
            this.puntuacion = puntuacion;
            this.sede = sede;
        }
    }
}
