// src/main/java/com/grupo5e/morapack/algorithm/tabu/MoraPackNeighborhood.java
package com.grupo5e.morapack.algorithm.tabu.adapters;

import com.grupo5e.morapack.algorithm.tabu.Move;
import com.grupo5e.morapack.algorithm.tabu.Neighborhood;
import com.grupo5e.morapack.algorithm.tabu.adapters.RoutingAdapters;
import com.grupo5e.morapack.algorithm.tabu.operators.RelocateShipmentMove;
import com.grupo5e.morapack.algorithm.tabu.operators.SwapShipmentsMove;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.algorithm.validation.ValidadorRestricciones;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MoraPackNeighborhood implements Neighborhood<Solucion> {

    private final ContextoProblema ctx;
    private final ValidadorRestricciones validador;
    private final RoutingAdapters<Solucion, String, Ruta> adapters;
    private final int sampleShipments;
    private final int routesPerShipment;

    public MoraPackNeighborhood(ContextoProblema ctx,
                                ValidadorRestricciones validador,
                                RoutingAdapters<Solucion, String, Ruta> adapters,
                                int sampleShipments,
                                int routesPerShipment) {
        this.ctx = ctx;
        this.validador = validador;
        this.adapters = adapters;
        this.sampleShipments = sampleShipments;
        this.routesPerShipment = routesPerShipment;
    }

    @Override
    public List<Move<Solucion>> generate(Solucion sol) {
        List<Move<Solucion>> moves = new ArrayList<>();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // 1) Muestrea paquetes presentes en la solución
        List<String> paquetes = new ArrayList<>(sol.getPaquetesIds()); // set -> lista
        if (paquetes.isEmpty()) return moves;
        Collections.shuffle(paquetes);
        if (paquetes.size() > sampleShipments) paquetes = paquetes.subList(0, sampleShipments);

        // 2) Para cada paquete, proponer rutas alternativas factibles
        for (String pkgId : paquetes) {
            Ruta rutaActual = adapters.getRutaActualDeEnvio(sol, pkgId);
            Paquete paquete = ctx.getPaquete(pkgId);
            if (paquete == null) continue;

            List<Ruta> alternativas = generarRutasCandidatas(paquete, sol);

            int count = 0;
            for (Ruta alt : alternativas) {
                // Evitar proponer "lo mismo"
                if (rutaActual != null
                        && Objects.equals(rutaActual.getAeropuertoOrigen(), alt.getAeropuertoOrigen())
                        && Objects.equals(rutaActual.getAeropuertoDestino(), alt.getAeropuertoDestino())) {
                    continue;
                }
                moves.add(new RelocateShipmentMove<Solucion, String, Ruta>(pkgId, alt, adapters));
                if (++count >= routesPerShipment) break;
            }
        }

        // 3) Algunos swaps aleatorios (diversificación)
        for (int i = 0; i < Math.min(16, paquetes.size() / 2); i++) {
            String a = paquetes.get(rnd.nextInt(paquetes.size()));
            String b = paquetes.get(rnd.nextInt(paquetes.size()));
            if (!a.equals(b)) {
                moves.add(new SwapShipmentsMove<Solucion, String, Ruta>(a, b, adapters));
            }
        }

        return moves;
    }

    // ================== helpers ==================

    /** Genera rutas como en tus constructores: directas y con una conexión, luego filtra por factibilidad. */
    private List<Ruta> generarRutasCandidatas(Paquete paquete, Solucion solucionActual) {
        List<Ruta> out = new ArrayList<>();
        String origen = paquete.getAeropuertoOrigen();
        String destino = paquete.getAeropuertoDestino();

        // (a) DIRECTAS
        List<Vuelo> directos = ctx.getVuelosDirectos(origen, destino);
        for (Vuelo v : directos) {
            Ruta r = crearRutaDirecta(v, paquete);
            if (esFactibleRuta(paquete.getId(), r, solucionActual)) {
                out.add(r);
                if (out.size() >= routesPerShipment) return out;
            }
        }

        // (b) CONEXIÓN (BFS de aeropuertos)
        List<String> rutaBFS = ctx.encontrarRutaMasCorta(origen, destino);
        if (rutaBFS != null && rutaBFS.size() >= 3) {
            Ruta r = crearRutaConConexiones(rutaBFS, paquete);
            if (r != null && esFactibleRuta(paquete.getId(), r, solucionActual)) out.add(r);
        }

        return out;
    }

    private boolean esFactibleRuta(String paqueteId, Ruta ruta, Solucion sol) {
        return validador.esRutaFactible(paqueteId, ruta, sol);
    }

    // ===== construcción de rutas con tus constructores reales =====

    private Ruta crearRutaDirecta(Vuelo vuelo, Paquete paquete) {
        // Igual que en ConstruccionEstrategia.crearRutaDirecta(...)
        Ruta ruta = new Ruta("ts_directa_" + System.currentTimeMillis(), paquete.getId());
        SegmentoRuta seg = new SegmentoRuta(
                "ts_seg_" + System.currentTimeMillis(),
                paquete.getAeropuertoOrigen(),
                paquete.getAeropuertoDestino(),
                vuelo.getNumeroVuelo(),
                vuelo.isMismoContinente()
        );
        ruta.agregarSegmento(seg);
        return ruta;
    }

    private Ruta crearRutaConConexiones(List<String> aeropuertos, Paquete paquete) {
        // Igual que en ConstruccionEstrategia.crearRutaConConexiones(...)
        Ruta ruta = new Ruta("ts_conex_" + System.currentTimeMillis(), paquete.getId());
        for (int i = 0; i < aeropuertos.size() - 1; i++) {
            String o = aeropuertos.get(i);
            String d = aeropuertos.get(i + 1);
            List<Vuelo> vuelosSegmento = ctx.getVuelosDirectos(o, d);
            if (vuelosSegmento.isEmpty()) return null; // no hay vuelo para ese tramo
            Vuelo v = vuelosSegmento.get(0);
            SegmentoRuta seg = new SegmentoRuta(
                    "ts_seg_" + i + "_" + System.currentTimeMillis(),
                    o, d, v.getNumeroVuelo(), v.isMismoContinente()
            );
            ruta.agregarSegmento(seg);
        }
        return ruta;
    }
}
