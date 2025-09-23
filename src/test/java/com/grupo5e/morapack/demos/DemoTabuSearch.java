package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.algorithm.tabu.adapters.MoraPackNeighborhood;
import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;
import com.grupo5e.morapack.algorithm.validation.ValidadorRestricciones;

import com.grupo5e.morapack.algorithm.tabu.*;
import com.grupo5e.morapack.algorithm.tabu.adapters.MoraPackRoutingAdapters;

import java.util.*;
import java.time.LocalTime;

/**
 * Demo de Tabu Search para MoraPack.
 * - Modo BASICO (datos sintéticos) similar a DemoSimpleFuncional.
 * - Modo REALES (si tienes loader) similar a DemoConDatosReales.
 *
 * Ejecuta:  mvn -q -Dtest=com.grupo5e.morapack.demos.DemoTabuSearch test  (o desde IDE run)
 */
public class DemoTabuSearch {

    public enum TipoDemo { BASICO, REALES }

    public static void main(String[] args) {
        TipoDemo tipo = (args != null && args.length > 0)
                ? parse(args[0]) : TipoDemo.BASICO;

        System.out.println("=== DEMO TABU SEARCH (" + tipo + ") ===\n");
        try {
            if (tipo == TipoDemo.BASICO) {
                ejecutarBasico();
            } else {
                ejecutarReales();
            }
        } catch (Exception e) {
            System.err.println("Error en DemoTabuSearch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static TipoDemo parse(String s) {
        try { return TipoDemo.valueOf(s.toUpperCase()); } catch (Exception e) { return TipoDemo.BASICO; }
    }

    // ============================================================
    // MODO 1: BASICO (datos sintéticos)
    // ============================================================

    private static void ejecutarBasico() {
        // Construir problema básico
        var aeropuertos = Arrays.asList(
                new Aeropuerto("LIM", "Lima", "Perú", "América", -12.0, -77.0, 100, true),
                new Aeropuerto("BRU", "Bruselas", "Bélgica", "Europa", 50.0, 4.0, 100, true),
                new Aeropuerto("BOG", "Bogotá", "Colombia", "América", 4.0, -74.0, 80, false),
                new Aeropuerto("MAD", "Madrid", "España", "Europa", 40.0, -3.0, 80, false)
        );

        Set<Continente> continentes = new HashSet<>();
        Continente america = new Continente("América", "AM", "LIM");
        america.agregarAeropuerto("LIM");
        america.agregarAeropuerto("BOG");
        Continente europa = new Continente("Europa", "EU", "BRU");
        europa.agregarAeropuerto("BRU");
        europa.agregarAeropuerto("MAD");
        continentes.add(america);
        continentes.add(europa);

        var vuelos = Arrays.asList(
                // numeroVuelo, origen, destino, mismoContinente, capacidad, tiempoHoras
                new Vuelo("V1", "LIM", "BOG", true, 30),
                new Vuelo("V2", "LIM", "BRU", false, 30),
                new Vuelo("V3", "BRU", "MAD", true, 25),
                new Vuelo("V4", "BOG", "MAD", false, 20),
                new Vuelo("V5", "BOG", "BRU", false, 20)
        );

        var paquetes = Arrays.asList(
                new Paquete("PKG_001", "LIM", "MAD", "CLI_001"),
                new Paquete("PKG_002", "LIM", "BOG", "CLI_002"),
                new Paquete("PKG_003", "BRU", "MAD", "CLI_003"),
                new Paquete("PKG_004", "BOG", "BRU", "CLI_004")
        );

        // 2) Contexto + Validador (idéntico a tu ALNS)
        ContextoProblema ctx = new ContextoProblema(paquetes, aeropuertos, vuelos, continentes);
        ValidadorRestricciones validador = new ValidadorRestricciones(aeropuertos, vuelos, continentes);

        // 3) Solución inicial "simple": asigna la primera ruta factible a cada paquete (estilo voraz)
        Solucion initial = construirSolucionInicialVoraz(paquetes, ctx, validador);

        // 4) Armar Neighborhood + Solver
        Neighborhood<Solucion> nb = new MoraPackNeighborhood(
                ctx, validador,
                new MoraPackRoutingAdapters(),
                64, // sampleShipments
                3   // routesPerShipment
        );

        var params = new TabuSearchSolver.Params();
        params.maxIterations = 1500;
        params.maxNoImprove  = 300;
        params.tabuTenure    = 15;
        params.neighborhoodSampleSize = 64;
        params.acceptInfeasible = false;

        CostFunction<Solucion> costFn = Solucion::getFitness; // tu fitness interno
        Feasibility<Solucion> feas   = s -> true;             // ya filtramos candidatos factibles
        Aspiration<Solucion> asp     = (cur, cand, cc, best) -> cc < best;

        var solver = new TabuSearchSolver<>(
                nb, costFn, feas, asp, Solucion::copiar, params // copiar() ya está implementado
        );

        // 5) Ejecutar
        long t0 = System.currentTimeMillis();
        var res = solver.run(initial);
        long t1 = System.currentTimeMillis();

        System.out.println("\n==== RESULTADOS TABU (BÁSICO) ====");
        imprimirResumen(res.bestSolution, (t1 - t0) / 1000.0);
    }

    /**
     * Construye una solución inicial rápida: para cada paquete,
     * intenta una ruta directa; si no hay, una con una conexión (BFS),
     * y solo asigna si pasa esRutaFactible.
     */
    private static Solucion construirSolucionInicialVoraz(List<Paquete> paquetes,
                                                          ContextoProblema ctx,
                                                          ValidadorRestricciones validador) {
        Solucion sol = new Solucion();
        for (Paquete p : paquetes) {
            String pid = p.getId();
            String o = p.getAeropuertoOrigen();
            String d = p.getAeropuertoDestino();

            // Intento directo
            List<Vuelo> directos = ctx.getVuelosDirectos(o, d);
            boolean asignado = false;
            for (Vuelo v : directos) {
                Ruta r = rutaDirecta(v, p);
                if (validador.esRutaFactible(pid, r, sol)) { // factibilidad real
                    sol.agregarRuta(pid, r);
                    sol.recalcularMetricas();
                    asignado = true;
                    break;
                }
            }
            if (asignado) continue;

            // Intento con conexión (camino más corto en aeropuertos)
            List<String> rutaBFS = ctx.encontrarRutaMasCorta(o, d);
            if (rutaBFS != null && rutaBFS.size() >= 3) {
                Ruta r = rutaConConexiones(rutaBFS, ctx, p);
                if (r != null && validador.esRutaFactible(pid, r, sol)) {
                    sol.agregarRuta(pid, r);
                    sol.recalcularMetricas();
                }
            }
        }
        return sol;
    }

    // Helpers para crear rutas como en tus constructores ALNS
    private static Ruta rutaDirecta(Vuelo v, Paquete p) {
        Ruta r = new Ruta("init_dir_" + System.nanoTime(), p.getId());
        SegmentoRuta s = new SegmentoRuta("init_seg_" + System.nanoTime(),
                p.getAeropuertoOrigen(), p.getAeropuertoDestino(),
                v.getNumeroVuelo(), v.isMismoContinente());
        r.agregarSegmento(s);
        return r;
    }

    private static Ruta rutaConConexiones(List<String> aeropuertos, ContextoProblema ctx, Paquete p) {
        Ruta r = new Ruta("init_cnx_" + System.nanoTime(), p.getId());
        for (int i = 0; i < aeropuertos.size() - 1; i++) {
            String o = aeropuertos.get(i);
            String d = aeropuertos.get(i + 1);
            List<Vuelo> segs = ctx.getVuelosDirectos(o, d);
            if (segs == null || segs.isEmpty()) return null;
            Vuelo v = segs.get(0);
            SegmentoRuta s = new SegmentoRuta("init_seg_" + i + "_" + System.nanoTime(),
                    o, d, v.getNumeroVuelo(), v.isMismoContinente());
            r.agregarSegmento(s);
        }
        return r;
    }

    // ============================================================
    // MODO 2: REALES
    // ============================================================

    private static void ejecutarReales() throws Exception {
        // Reutilizar pipeline de carga
        // Por simplicidad, arma listas mínimas; puedes sustituir por tu loader real.
        throw new UnsupportedOperationException("Implementa aquí la carga real si ya tienes MoraPackDataLoader en tu proyecto.");
    }

    // ============================================================
    // Utilitarios de impresión
    // ============================================================

    private static void imprimirResumen(Solucion s, double elapsedSec) {
        if (s == null) {
            System.out.println("No se encontró solución.");
            return;
        }
        System.out.println("Tiempo: " + String.format(Locale.US, "%.2f s", elapsedSec));
        System.out.println("Paquetes ruteados: " + s.getCantidadPaquetes());
        System.out.println("Costo total: " + String.format(Locale.US, "%.2f", s.getCostoTotal()));
        System.out.println("Tiempo total (h): " + String.format(Locale.US, "%.2f", s.getTiempoTotalHoras()));
        System.out.println("Fitness: " + String.format(Locale.US, "%.2f", s.getFitness()));
        System.out.println("Factible: " + s.isEsFactible());
        // Puedes imprimir más KPIs si tienes getters extra
    }
}
