package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.core.constants.ConstantesMoraPack;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;
import com.grupo5e.morapack.algorithm.validation.ValidadorRestricciones;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static java.util.stream.Collectors.toList;

/**
 * LNS validado:
 *  - Destructor: remueve aleatorio q paquetes (liberando capacidad local).
 *  - Reparador: asigna 1 segmento DIRECTO desde una sede a destino (si hay capacidad).
 *  - Validaciones: capacidad por vuelo (local), continuidad (trivial 1-seg), deadline con holgura,
 *                  capacidad de almacén destino (estático vía mapa local).
 *  - Aceptación: mejor => siempre; peor => SA con temperatura.
 */
public class LNSBasicoValidado {

    private final int iterMax;
    private final double tempInicial;
    private final double enfriamiento;
    private final double porcentajeDestruccion;

    private final ContextoProblema ctx;
    private final ValidadorRestricciones validador;

    // Sedes de origen permitidas
    private final List<String> sedes = List.of("EBCI", "SPIM", "UBBB");

    // Holgura % sobre tiempo de ruta vs deadline (p.ej. 5%)
    private final double deadlineSlack = 0.05;

    // Capacidad de almacén por aeropuerto (estático en la Solución de esta corrida)
    private final Map<String, Integer> capacidadAlmacenPorIata = new HashMap<>();
    private final Map<String, Integer> ocupacionAlmacenPorIata = new HashMap<>();

    public LNSBasicoValidado(ContextoProblema ctx,
                             ValidadorRestricciones validador,
                             int iterMax, double tempInicial, double enfriamiento, double porcentajeDestruccion) {
        this.ctx = ctx;
        this.validador = validador;
        this.iterMax = iterMax;
        this.tempInicial = tempInicial;
        this.enfriamiento = enfriamiento;
        this.porcentajeDestruccion = porcentajeDestruccion;
        inicializarAlmacenes();
    }

    public Solucion resolver(Solucion solucionInicial) {
        Random rnd = new Random();
        Solucion actual = solucionInicial.copiar();

        if (actual.getCantidadPaquetes() == 0) {
            actual = crearSolucionInicialGreedy(rnd);
        }

        // Recontar almacenes desde las rutas de 'actual'
        recontarAlmacenDesdeSolucion(actual);

        // Validar con la MISMA función rápida de las iteraciones
        if (!validaSolucionRapida(actual)) {
            actual.setFuncionObjetivo(Double.MAX_VALUE / 4);
        }
        // Misma secuencia para fitness
        validador.validarSolucionCompleta(actual);
        actual.recalcularMetricas();

        Solucion mejor = actual.copiar();
        double T = tempInicial;

        for (int it = 1; it <= iterMax; it++) {
            Solucion copia = actual.copiar();

            // snapshot del almacén para esta copia
            Map<String, Integer> occAlmLocal = new HashMap<>(ocupacionAlmacenPorIata);

            // --- Destruir q paquetes liberando capacidad local y almacén
            int q = Math.max(1, (int) Math.round(copia.getCantidadPaquetes() * porcentajeDestruccion));
            List<String> ids = new ArrayList<>(copia.getPaquetesIds());
            Collections.shuffle(ids, rnd);
            for (String pid : ids.stream().limit(q).collect(toList())) {
                liberarCapacidades(copia, pid); // vuelos + almacén local
                copia.removerRuta(pid);
            }

            // --- Reparar (directo sede->destino) con validaciones inplace
            repararGreedyDirectoValidado(copia, rnd);

            // --- Validar y fitness
            boolean ok = validaSolucionRapida(copia);
            if (!ok) {
                copia.setFuncionObjetivo(Double.MAX_VALUE / 4);
            }

            validador.validarSolucionCompleta(copia);
            copia.recalcularMetricas();

            // --- Aceptación (SA) - Alineado con fitness
            double fCur = actual.getFitness();
            double fNew = copia.getFitness();
            boolean aceptar = (fNew < fCur) || rnd.nextDouble() < Math.exp(-(fNew - fCur) / Math.max(1e-9, T));
            
            if (aceptar) {
                actual = copia;
                // **aquí sí** promover el almacén local al global
                ocupacionAlmacenPorIata.clear();
                ocupacionAlmacenPorIata.putAll(occAlmLocal);
            }

            if (actual.getFitness() < mejor.getFitness()) mejor = actual.copiar();

            T *= enfriamiento;

            // Log SIEMPRE con fitness (alineado con aceptación y Best)
            System.out.printf("Iter %d | F=%.2f | Best=%.2f | T=%.3f | ruteados=%d%n",
                    it, actual.getFitness(), mejor.getFitness(), T, actual.getCantidadPaquetes());
        }
        return mejor;
    }

    /* ================== Reparación (directo) + validaciones ================== */

    private void repararGreedyDirectoValidado(Solucion sol, Random rnd) {
        // paquetes no ruteados
        Set<String> yaRuteados = sol.getPaquetesIds();
        List<Paquete> restantes = ctx.getTodosPaquetes().stream()
                .filter(p -> !yaRuteados.contains(p.getId()))
                .collect(toList());

        Collections.shuffle(restantes, rnd);
        List<String> sedesShuffled = new ArrayList<>(sedes);
        Collections.shuffle(sedesShuffled, rnd);

        // index vuelos por (origen->destino)
        Map<String, List<Vuelo>> vuelosOD = indexVuelosOD();

        for (Paquete p : restantes) {
            String destino = p.getAeropuertoDestino();
            if (!hayCapacidadAlmacen(destino)) continue;

            boolean asignado = false;
            for (String sede : sedesShuffled) {
                String key = sede + "->" + destino;
                List<Vuelo> cand = new ArrayList<>(vuelosOD.getOrDefault(key, List.of()));
                if (cand.isEmpty()) continue;
                
                // Filtrar candidatos válidos
                List<Vuelo> validos = new ArrayList<>();
                for (Vuelo v : cand) {
                    if (!v.estaOperativo()) continue;
                    if (!hayCapacidadVueloLocal(sol, v)) continue;
                    
                    Ruta r = construirRutaUnSegmento(p, v);
                    if (!respetaDeadline(p, r)) continue;
                    
                    validos.add(v);
                }
                
                if (validos.isEmpty()) continue;
                
                // Ordenar por duración (criterio simple)
                validos.sort(Comparator.comparingDouble(Vuelo::getDuracionHoras));
                
                // Ruleta entre top-k (3-5 mejores)
                int k = Math.min(5, validos.size());
                int idx = ruletaEntreTopK(k, rnd);
                Vuelo elegido = validos.get(idx);
                
                // Aplicar asignación
                Ruta r = construirRutaUnSegmento(p, elegido);
                aplicarAsignacion(sol, p, r, elegido);
                asignado = true;
                if (asignado) break;
            }
        }
    }

    private Ruta construirRutaUnSegmento(Paquete p, Vuelo v) {
        SegmentoRuta seg = new SegmentoRuta(
                "SEG_" + p.getId(),
                v.getAeropuertoOrigen(),
                v.getAeropuertoDestino(),
                v.getNumeroVuelo(),
                v.isMismoContinente()
        );
        seg.setDuracionHoras(v.getDuracionHoras());
        seg.setCosto(v.getDuracionHoras() * 10.0);

        Ruta r = new Ruta("R_" + p.getId(), p.getId());
        r.agregarSegmento(seg);
        return r;
    }

    private void aplicarAsignacion(Solucion sol, Paquete p, Ruta r, Vuelo v) {
        sol.agregarRuta(p.getId(), r);
        // capacidad vuelo (local a Solucion)
        String num = v.getNumeroVuelo();
        sol.getOcupacionVuelos().put(num, sol.getOcupacionVuelos().getOrDefault(num, 0) + 1);
        // almacén destino
        String dest = v.getAeropuertoDestino();
        ocupacionAlmacenPorIata.put(dest, ocupacionAlmacenPorIata.getOrDefault(dest, 0) + 1);
    }

    /* ================== Validaciones rápidas ================== */

    private boolean validaSolucionRapida(Solucion sol) {
        // 1) capacidad de vuelos (local)
        Map<String, Integer> usoLocal = new HashMap<>();
        for (Ruta r : sol.getRutasPaquetes().values()) {
            for (SegmentoRuta s : r.getSegmentos()) {
                String num = s.getNumeroVuelo();
                int u = usoLocal.getOrDefault(num, 0) + 1;
                usoLocal.put(num, u);
                Vuelo v = buscarVuelo(num);
                if (v == null || u > v.getCapacidadMaxima()) return false;
            }
        }
        // 2) deadline por paquete
        for (Map.Entry<String, Ruta> e : sol.getRutasPaquetes().entrySet()) {
            Paquete p = buscarPaquete(e.getKey());
            if (p == null) continue;
            if (!respetaDeadline(p, e.getValue())) return false;
        }
        // 3) almacén destino
        Map<String, Integer> occ = new HashMap<>();
        for (Ruta r : sol.getRutasPaquetes().values()) {
            String dest = r.getSegmentos().get(r.getSegmentos().size() - 1).getAeropuertoDestino();
            int c = occ.getOrDefault(dest, 0) + 1;
            occ.put(dest, c);
            int cap = capacidadAlmacenPorIata.getOrDefault(dest, Integer.MAX_VALUE);
            if (c > cap) return false;
        }
        return true;
    }

    private boolean respetaDeadline(Paquete p, Ruta r) {
        double t = r.getSegmentos().stream().mapToDouble(SegmentoRuta::getDuracionHoras).sum();
        int conexiones = Math.max(0, r.getSegmentos().size() - 1);
        t += conexiones * 2.0; // Tiempo de conexión entre vuelos
        
        // Agregar tiempo de procesamiento administrativo final
        t += ConstantesMoraPack.TIEMPO_PROCESAMIENTO_ADMINISTRATIVO_HORAS;
        
        long horas = ChronoUnit.HOURS.between(LocalDateTime.now(), p.getFechaLimiteEntrega());
        return t * (1.0 + deadlineSlack) <= horas;
    }

    /* ================== Estado local (capacidad/almacén) ================== */

    private void liberarCapacidades(Solucion sol, String paqueteId) {
        Ruta r = sol.getRutasPaquetes().get(paqueteId);
        if (r == null) return;

        // vuelos
        for (SegmentoRuta s : r.getSegmentos()) {
            String num = s.getNumeroVuelo();
            int usados = sol.getOcupacionVuelos().getOrDefault(num, 0);
            if (usados > 0) sol.getOcupacionVuelos().put(num, usados - 1);
        }
        // almacén
        String destino = r.getSegmentos().get(r.getSegmentos().size() - 1).getAeropuertoDestino();
        int occ = ocupacionAlmacenPorIata.getOrDefault(destino, 0);
        if (occ > 0) ocupacionAlmacenPorIata.put(destino, occ - 1);
    }

    private boolean hayCapacidadVueloLocal(Solucion sol, Vuelo v) {
        int usados = sol.getOcupacionVuelos().getOrDefault(v.getNumeroVuelo(), 0);
        return usados < v.getCapacidadMaxima();
    }

    private boolean hayCapacidadAlmacen(String iataDestino) {
        int cap = capacidadAlmacenPorIata.getOrDefault(iataDestino, Integer.MAX_VALUE);
        int occ = ocupacionAlmacenPorIata.getOrDefault(iataDestino, 0);
        return occ < cap;
    }

    private void inicializarAlmacenes() {
        // Usar capacidades reales del archivo aeropuertosinfo.txt
        for (Aeropuerto a : ctx.getTodosAeropuertos()) {
            String iata = a.getCodigoIATA();
            int cap = a.getCapacidadAlmacen(); // Usar capacidad real del aeropuerto
            capacidadAlmacenPorIata.put(iata, cap);
            ocupacionAlmacenPorIata.put(iata, 0);
        }
    }

    /* ================== Utilidades ================== */
    
    /**
     * Recontar almacenes desde las rutas de la solución
     */
    private void recontarAlmacenDesdeSolucion(Solucion s) {
        // reset ocupación
        ocupacionAlmacenPorIata.replaceAll((k, v) -> 0);
        for (Ruta r : s.getRutasPaquetes().values()) {
            String dest = r.getSegmentos().get(r.getSegmentos().size() - 1).getAeropuertoDestino();
            ocupacionAlmacenPorIata.put(dest, ocupacionAlmacenPorIata.getOrDefault(dest, 0) + 1);
        }
    }

    /**
     * Ruleta entre top-k candidatos con pesos 1/i (1, 1/2, 1/3...)
     */
    private int ruletaEntreTopK(int k, Random rnd) {
        double[] w = new double[k];
        double sum = 0;
        for (int i = 0; i < k; i++) {
            w[i] = 1.0 / (i + 1);
            sum += w[i];
        }
        double r = rnd.nextDouble() * sum;
        double acc = 0;
        for (int i = 0; i < k; i++) {
            acc += w[i];
            if (r <= acc) return i;
        }
        return 0;
    }

    private Map<String, List<Vuelo>> indexVuelosOD() {
        Map<String, List<Vuelo>> map = new HashMap<>();
        for (Vuelo v : ctx.getTodosVuelos()) {
            String key = v.getAeropuertoOrigen() + "->" + v.getAeropuertoDestino();
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(v);
        }
        return map;
    }

    private Vuelo buscarVuelo(String numero) {
        for (Vuelo v : ctx.getTodosVuelos()) if (v.getNumeroVuelo().equals(numero)) return v;
        return null;
    }

    private Paquete buscarPaquete(String id) {
        for (Paquete p : ctx.getTodosPaquetes()) if (p.getId().equals(id)) return p;
        return null;
    }

    private Solucion crearSolucionInicialGreedy(Random rnd) {
        Solucion sol = new Solucion();
        repararGreedyDirectoValidado(sol, rnd);
        return sol;
    }
}
