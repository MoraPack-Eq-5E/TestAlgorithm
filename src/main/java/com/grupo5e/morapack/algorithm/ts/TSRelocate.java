package com.grupo5e.morapack.algorithm.ts;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.core.model.Vuelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class TSRelocate {
    private ArrayList<Aeropuerto> aeropuertos;
    private ArrayList<Vuelo> vuelos;
    private HashMap<Aeropuerto, Integer> ocupacionAlmacenes;
    private Random aleatorio;

    public TSRelocate(ArrayList<Aeropuerto> aeropuertos, ArrayList<Vuelo> vuelos,
                                HashMap<Aeropuerto, Integer> ocupacionAlmacenes) {
        this.aeropuertos = aeropuertos;
        this.vuelos = vuelos;
        this.ocupacionAlmacenes = ocupacionAlmacenes;
        this.aleatorio = new Random(System.currentTimeMillis());
    }

    /**
     * Genera movimientos de relocación para un conjunto de paquetes.
     */
    public ArrayList<SolucionVecino> generarMovimientosRelocate(
            HashMap<Paquete, ArrayList<Vuelo>> solucionActual,
            int maxMovimientos) {

        ArrayList<SolucionVecino> movimientos = new ArrayList<>();

        if (solucionActual.isEmpty()) {
            return movimientos;
        }

        ArrayList<Paquete> paquetesAsignados = new ArrayList<>(solucionActual.keySet());
        Collections.shuffle(paquetesAsignados, aleatorio);

        int movimientosGenerados = 0;

        for (Paquete paquete : paquetesAsignados) {
            if (movimientosGenerados >= maxMovimientos) break;

            ArrayList<Vuelo> rutaActual = solucionActual.get(paquete);
            if (rutaActual == null) continue;

            // Generar rutas alternativas para este paquete
            ArrayList<ArrayList<Vuelo>> rutasAlternativas = encontrarRutasAlternativas(paquete, rutaActual);

            for (ArrayList<Vuelo> nuevaRuta : rutasAlternativas) {
                if (movimientosGenerados >= maxMovimientos) break;

                // Verificar si la nueva ruta es válida y diferente
                if (esRutaValida(paquete, nuevaRuta) && !sonRutasEquivalentes(rutaActual, nuevaRuta)) {

                    // Crear nueva solución con el paquete reubicado
                    HashMap<Paquete, ArrayList<Vuelo>> nuevaSolucion = new HashMap<>(solucionActual);
                    nuevaSolucion.put(paquete, new ArrayList<>(nuevaRuta));

                    // Verificar factibilidad de la nueva solución
                    if (esSolucionFactible(nuevaSolucion, paquete, rutaActual, nuevaRuta)) {
                        int peso = calcularPesoSolucion(nuevaSolucion);
                        TSMove movimiento = new TSMove("RELOCATE", paquete, rutaActual, nuevaRuta);

                        SolucionVecino vecino = new SolucionVecino(nuevaSolucion, peso, movimiento);
                        vecino.deltaEvaluacion = peso - calcularPesoSolucion(solucionActual);

                        movimientos.add(vecino);
                        movimientosGenerados++;
                    }
                }
            }
        }

        return movimientos;
    }

    /**
     * Genera movimientos de relocación intensiva para diversificación.
     */
    public ArrayList<SolucionVecino> generarRelocateIntensivo(
            HashMap<Paquete, ArrayList<Vuelo>> solucionActual,
            double factorIntensidad) {

        ArrayList<SolucionVecino> movimientos = new ArrayList<>();
        ArrayList<Paquete> paquetesAsignados = new ArrayList<>(solucionActual.keySet());

        // Ordenar paquetes por criterios específicos para intensificación
        paquetesAsignados.sort((p1, p2) -> {
            // Priorizar paquetes con rutas largas o menos eficientes
            ArrayList<Vuelo> ruta1 = solucionActual.get(p1);
            ArrayList<Vuelo> ruta2 = solucionActual.get(p2);

            int complejidad1 = calcularComplejidadRuta(ruta1);
            int complejidad2 = calcularComplejidadRuta(ruta2);

            return Integer.compare(complejidad2, complejidad1); // Más complejas primero
        });

        int maxMovimientos = (int)(paquetesAsignados.size() * factorIntensidad);

        for (int i = 0; i < maxMovimientos && i < paquetesAsignados.size(); i++) {
            Paquete paquete = paquetesAsignados.get(i);
            ArrayList<Vuelo> rutaActual = solucionActual.get(paquete);

            // Buscar específicamente rutas más eficientes
            ArrayList<ArrayList<Vuelo>> rutasOptimas = buscarRutasOptimas(paquete, rutaActual);

            for (ArrayList<Vuelo> rutaOptima : rutasOptimas) {
                if (esRutaValida(paquete, rutaOptima)) {
                    HashMap<Paquete, ArrayList<Vuelo>> nuevaSolucion = new HashMap<>(solucionActual);
                    nuevaSolucion.put(paquete, new ArrayList<>(rutaOptima));

                    if (esSolucionFactible(nuevaSolucion, paquete, rutaActual, rutaOptima)) {
                        int peso = calcularPesoSolucion(nuevaSolucion);
                        TSMove movimiento = new TSMove("RELOCATE_INTENSIVO", paquete, rutaActual, rutaOptima);

                        SolucionVecino vecino = new SolucionVecino(nuevaSolucion, peso, movimiento);
                        vecino.deltaEvaluacion = peso - calcularPesoSolucion(solucionActual);

                        movimientos.add(vecino);
                    }
                }
            }
        }

        return movimientos;
    }

    /**
     * Encuentra rutas alternativas para un paquete específico.
     */
    private ArrayList<ArrayList<Vuelo>> encontrarRutasAlternativas(Paquete paquete, ArrayList<Vuelo> rutaExcluida) {
        ArrayList<ArrayList<Vuelo>> alternativas = new ArrayList<>();

        Ciudad origen = paquete.getUbicacionActual();
        Ciudad destino = paquete.getCiudadDestino();

        if (origen.equals(destino)) {
            if (rutaExcluida == null || !rutaExcluida.isEmpty()) {
                alternativas.add(new ArrayList<>()); // Ruta vacía (ya está en destino)
            }
            return alternativas;
        }

        // Buscar ruta directa
        ArrayList<Vuelo> rutaDirecta = buscarRutaDirecta(origen, destino);
        if (rutaDirecta != null && !sonRutasEquivalentes(rutaDirecta, rutaExcluida)) {
            alternativas.add(rutaDirecta);
        }

        // Buscar rutas con una escala
        ArrayList<ArrayList<Vuelo>> rutasUnaEscala = buscarRutasUnaEscala(origen, destino, 3);
        for (ArrayList<Vuelo> ruta : rutasUnaEscala) {
            if (!sonRutasEquivalentes(ruta, rutaExcluida)) {
                alternativas.add(ruta);
            }
        }

        // Buscar rutas con dos escalas (limitado)
        if (alternativas.size() < 2) {
            ArrayList<ArrayList<Vuelo>> rutasDosEscalas = buscarRutasDosEscalas(origen, destino, 2);
            for (ArrayList<Vuelo> ruta : rutasDosEscalas) {
                if (!sonRutasEquivalentes(ruta, rutaExcluida)) {
                    alternativas.add(ruta);
                }
            }
        }

        return alternativas;
    }

    /**
     * Busca rutas óptimas específicamente (rutas directas o con menor tiempo).
     */
    private ArrayList<ArrayList<Vuelo>> buscarRutasOptimas(Paquete paquete, ArrayList<Vuelo> rutaActual) {
        ArrayList<ArrayList<Vuelo>> rutasOptimas = new ArrayList<>();

        Ciudad origen = paquete.getUbicacionActual();
        Ciudad destino = paquete.getCiudadDestino();

        if (origen.equals(destino)) {
            rutasOptimas.add(new ArrayList<>());
            return rutasOptimas;
        }

        double tiempoRutaActual = calcularTiempoTotalRuta(rutaActual);

        // Buscar ruta directa primero
        ArrayList<Vuelo> rutaDirecta = buscarRutaDirecta(origen, destino);
        if (rutaDirecta != null && calcularTiempoTotalRuta(rutaDirecta) < tiempoRutaActual) {
            rutasOptimas.add(rutaDirecta);
        }

        // Si no hay ruta directa mejor, buscar rutas con una escala más eficientes
        if (rutasOptimas.isEmpty()) {
            ArrayList<ArrayList<Vuelo>> rutasUnaEscala = buscarRutasUnaEscala(origen, destino, 5);
            for (ArrayList<Vuelo> ruta : rutasUnaEscala) {
                if (calcularTiempoTotalRuta(ruta) < tiempoRutaActual) {
                    rutasOptimas.add(ruta);
                    if (rutasOptimas.size() >= 3) break; // Limitar a 3 rutas óptimas
                }
            }
        }

        return rutasOptimas;
    }

    /**
     * Busca ruta directa entre dos ciudades.
     */
    private ArrayList<Vuelo> buscarRutaDirecta(Ciudad origen, Ciudad destino) {
        Aeropuerto aeropuertoOrigen = obtenerAeropuertoPorCiudad(origen);
        Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(destino);

        if (aeropuertoOrigen == null || aeropuertoDestino == null) return null;

        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen().equals(aeropuertoOrigen) &&
                    vuelo.getAeropuertoDestino().equals(aeropuertoDestino) &&
                    vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {

                ArrayList<Vuelo> ruta = new ArrayList<>();
                ruta.add(vuelo);
                return ruta;
            }
        }

        return null;
    }

    /**
     * Busca rutas con una escala.
     */
    private ArrayList<ArrayList<Vuelo>> buscarRutasUnaEscala(Ciudad origen, Ciudad destino, int maxRutas) {
        ArrayList<ArrayList<Vuelo>> rutas = new ArrayList<>();

        Aeropuerto aeropuertoOrigen = obtenerAeropuertoPorCiudad(origen);
        Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(destino);

        if (aeropuertoOrigen == null || aeropuertoDestino == null) return rutas;

        // Crear lista de aeropuertos intermedios y priorizarlos
        ArrayList<Aeropuerto> intermedios = new ArrayList<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (!aeropuerto.equals(aeropuertoOrigen) && !aeropuerto.equals(aeropuertoDestino)) {
                intermedios.add(aeropuerto);
            }
        }

        // Priorizar aeropuertos por continente (mismo continente que origen o destino primero)
        intermedios.sort((a1, a2) -> {
            boolean a1MismoCont = a1.getCiudad().getContinente() == origen.getContinente() ||
                    a1.getCiudad().getContinente() == destino.getContinente();
            boolean a2MismoCont = a2.getCiudad().getContinente() == origen.getContinente() ||
                    a2.getCiudad().getContinente() == destino.getContinente();

            if (a1MismoCont && !a2MismoCont) return -1;
            if (!a1MismoCont && a2MismoCont) return 1;
            return 0;
        });

        for (Aeropuerto intermedio : intermedios) {
            if (rutas.size() >= maxRutas) break;

            Vuelo primerVuelo = null;
            Vuelo segundoVuelo = null;

            // Buscar primer segmento
            for (Vuelo vuelo : vuelos) {
                if (vuelo.getAeropuertoOrigen().equals(aeropuertoOrigen) &&
                        vuelo.getAeropuertoDestino().equals(intermedio) &&
                        vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                    primerVuelo = vuelo;
                    break;
                }
            }

            if (primerVuelo == null) continue;

            // Buscar segundo segmento
            for (Vuelo vuelo : vuelos) {
                if (vuelo.getAeropuertoOrigen().equals(intermedio) &&
                        vuelo.getAeropuertoDestino().equals(aeropuertoDestino) &&
                        vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                    segundoVuelo = vuelo;
                    break;
                }
            }

            if (segundoVuelo != null) {
                ArrayList<Vuelo> ruta = new ArrayList<>();
                ruta.add(primerVuelo);
                ruta.add(segundoVuelo);
                rutas.add(ruta);
            }
        }

        return rutas;
    }

    /**
     * Busca rutas con dos escalas (limitado para eficiencia).
     */
    private ArrayList<ArrayList<Vuelo>> buscarRutasDosEscalas(Ciudad origen, Ciudad destino, int maxRutas) {
        ArrayList<ArrayList<Vuelo>> rutas = new ArrayList<>();

        Aeropuerto aeropuertoOrigen = obtenerAeropuertoPorCiudad(origen);
        Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(destino);

        if (aeropuertoOrigen == null || aeropuertoDestino == null) return rutas;

        // Buscar combinaciones limitadas de escalas
        ArrayList<Aeropuerto> candidatos = new ArrayList<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (!aeropuerto.equals(aeropuertoOrigen) && !aeropuerto.equals(aeropuertoDestino)) {
                candidatos.add(aeropuerto);
            }
        }

        Collections.shuffle(candidatos, aleatorio);
        int maxCandidatos = Math.min(6, candidatos.size());

        for (int i = 0; i < maxCandidatos && rutas.size() < maxRutas; i++) {
            for (int j = i + 1; j < maxCandidatos && rutas.size() < maxRutas; j++) {
                Aeropuerto escala1 = candidatos.get(i);
                Aeropuerto escala2 = candidatos.get(j);

                ArrayList<Vuelo> ruta = intentarRutaDosEscalas(aeropuertoOrigen, escala1, escala2, aeropuertoDestino);
                if (ruta != null) {
                    rutas.add(ruta);
                }

                // También probar en orden inverso
                if (rutas.size() < maxRutas) {
                    ruta = intentarRutaDosEscalas(aeropuertoOrigen, escala2, escala1, aeropuertoDestino);
                    if (ruta != null) {
                        rutas.add(ruta);
                    }
                }
            }
        }

        return rutas;
    }

    private ArrayList<Vuelo> intentarRutaDosEscalas(Aeropuerto origen, Aeropuerto escala1,
                                                    Aeropuerto escala2, Aeropuerto destino) {
        Vuelo vuelo1 = null, vuelo2 = null, vuelo3 = null;

        // Buscar vuelo 1
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen().equals(origen) &&
                    vuelo.getAeropuertoDestino().equals(escala1) &&
                    vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                vuelo1 = vuelo;
                break;
            }
        }

        if (vuelo1 == null) return null;

        // Buscar vuelo 2
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen().equals(escala1) &&
                    vuelo.getAeropuertoDestino().equals(escala2) &&
                    vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                vuelo2 = vuelo;
                break;
            }
        }

        if (vuelo2 == null) return null;

        // Buscar vuelo 3
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen().equals(escala2) &&
                    vuelo.getAeropuertoDestino().equals(destino) &&
                    vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                vuelo3 = vuelo;
                break;
            }
        }

        if (vuelo3 != null) {
            ArrayList<Vuelo> ruta = new ArrayList<>();
            ruta.add(vuelo1);
            ruta.add(vuelo2);
            ruta.add(vuelo3);
            return ruta;
        }

        return null;
    }

    // Métodos auxiliares

    private boolean sonRutasEquivalentes(ArrayList<Vuelo> ruta1, ArrayList<Vuelo> ruta2) {
        if (ruta1 == null && ruta2 == null) return true;
        if (ruta1 == null || ruta2 == null) return false;
        if (ruta1.size() != ruta2.size()) return false;

        for (int i = 0; i < ruta1.size(); i++) {
            if (ruta1.get(i).getId() != ruta2.get(i).getId()) {
                return false;
            }
        }
        return true;
    }

    private int calcularComplejidadRuta(ArrayList<Vuelo> ruta) {
        if (ruta == null) return 0;

        int complejidad = ruta.size() * 10; // Penalizar rutas largas

        for (Vuelo vuelo : ruta) {
            double utilizacion = (double) vuelo.getCapacidadUsada() / vuelo.getCapacidadMaxima();
            if (utilizacion > 0.8) {
                complejidad += 5; // Penalizar rutas con alta utilización
            }
        }

        return complejidad;
    }

    private double calcularTiempoTotalRuta(ArrayList<Vuelo> ruta) {
        if (ruta == null || ruta.isEmpty()) return 0.0;

        double tiempo = 0.0;
        for (Vuelo vuelo : ruta) {
            tiempo += vuelo.getTiempoTransporte();
        }

        // Agregar tiempo de conexiones
        if (ruta.size() > 1) {
            tiempo += (ruta.size() - 1) * 2.0; // 2 horas por conexión
        }

        return tiempo;
    }

    private Aeropuerto obtenerAeropuertoPorCiudad(Ciudad ciudad) {
        if (ciudad == null || ciudad.getNombre() == null) return null;

        for (Aeropuerto aeropuerto : aeropuertos) {
            if (aeropuerto.getCiudad() != null && aeropuerto.getCiudad().getNombre() != null &&
                    aeropuerto.getCiudad().getNombre().equalsIgnoreCase(ciudad.getNombre().trim())) {
                return aeropuerto;
            }
        }
        return null;
    }

    private boolean esRutaValida(Paquete paquete, ArrayList<Vuelo> ruta) {
        // Implementar validación de ruta (similar a ALNSRepair)
        // Por simplicidad, asumir que es válida si cumple restricciones básicas
        return true; // Placeholder - implementar validación completa
    }

    private boolean esSolucionFactible(HashMap<Paquete, ArrayList<Vuelo>> solucion,
                                       Paquete paqueteModificado, ArrayList<Vuelo> rutaAnterior,
                                       ArrayList<Vuelo> rutaNueva) {
        // Verificar factibilidad de capacidades y almacenes
        // Placeholder - implementar verificación completa
        return true;
    }

    private int calcularPesoSolucion(HashMap<Paquete, ArrayList<Vuelo>> solucion) {
        // Implementar cálculo de peso (similar a ALNSSolver)
        return solucion.size() * 100; // Placeholder simple
    }
}
