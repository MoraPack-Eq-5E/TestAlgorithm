package com.grupo5e.morapack.algorithm.ts;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.core.model.Vuelo;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class TSSwap {
    private ArrayList<Aeropuerto> aeropuertos;
    private ArrayList<Vuelo> vuelos;
    private HashMap<Aeropuerto, Integer> ocupacionAlmacenes;
    private Random aleatorio;

    public TSSwap(ArrayList<Aeropuerto> aeropuertos, ArrayList<Vuelo> vuelos,
                            HashMap<Aeropuerto, Integer> ocupacionAlmacenes) {
        this.aeropuertos = aeropuertos;
        this.vuelos = vuelos;
        this.ocupacionAlmacenes = ocupacionAlmacenes;
        this.aleatorio = new Random(System.currentTimeMillis());
    }

    /**
     * Genera movimientos de intercambio (swap) entre pares de paquetes.
     */
    public ArrayList<SolucionVecino> generarMovimientosSwap(
            HashMap<Paquete, ArrayList<Vuelo>> solucionActual,
            int maxMovimientos) {

        ArrayList<SolucionVecino> movimientos = new ArrayList<>();

        if (solucionActual.size() < 2) {
            return movimientos; // Necesitamos al menos 2 paquetes para hacer swap
        }

        ArrayList<Paquete> paquetesAsignados = new ArrayList<>(solucionActual.keySet());
        Collections.shuffle(paquetesAsignados, aleatorio);

        int movimientosGenerados = 0;

        for (int i = 0; i < paquetesAsignados.size() && movimientosGenerados < maxMovimientos; i++) {
            for (int j = i + 1; j < paquetesAsignados.size() && movimientosGenerados < maxMovimientos; j++) {

                Paquete paquete1 = paquetesAsignados.get(i);
                Paquete paquete2 = paquetesAsignados.get(j);

                ArrayList<Vuelo> ruta1 = solucionActual.get(paquete1);
                ArrayList<Vuelo> ruta2 = solucionActual.get(paquete2);

                // Verificar si el intercambio es válido y beneficioso
                if (esIntercambioValido(paquete1, ruta2, paquete2, ruta1) &&
                        esIntercambioBeneficioso(paquete1, ruta1, ruta2, paquete2, ruta2, ruta1)) {

                    // Crear nueva solución con intercambio
                    HashMap<Paquete, ArrayList<Vuelo>> nuevaSolucion = new HashMap<>(solucionActual);
                    nuevaSolucion.put(paquete1, new ArrayList<>(ruta2));
                    nuevaSolucion.put(paquete2, new ArrayList<>(ruta1));

                    // Verificar factibilidad completa
                    if (esSolucionFactible(nuevaSolucion, paquete1, paquete2, ruta1, ruta2)) {
                        int peso = calcularPesoSolucion(nuevaSolucion);
                        TSMove movimiento = new TSMove("SWAP", paquete1, paquete2, ruta1, ruta2, ruta2, ruta1);

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
     * Genera movimientos de intercambio inteligente basados en similitud de destinos.
     */
    public ArrayList<SolucionVecino> generarSwapInteligente(
            HashMap<Paquete, ArrayList<Vuelo>> solucionActual,
            int maxMovimientos) {

        ArrayList<SolucionVecino> movimientos = new ArrayList<>();
        ArrayList<Paquete> paquetesAsignados = new ArrayList<>(solucionActual.keySet());

        // Agrupar paquetes por continente de destino para intercambios más inteligentes
        HashMap<String, ArrayList<Paquete>> paquetesPorContinente = new HashMap<>();

        for (Paquete paquete : paquetesAsignados) {
            String continente = paquete.getCiudadDestino().getContinente().toString();
            paquetesPorContinente.computeIfAbsent(continente, k -> new ArrayList<>()).add(paquete);
        }

        int movimientosGenerados = 0;

        // Intercambios dentro del mismo continente (más probable que sean beneficiosos)
        for (ArrayList<Paquete> paquetesContinente : paquetesPorContinente.values()) {
            if (paquetesContinente.size() < 2) continue;

            Collections.shuffle(paquetesContinente, aleatorio);

            for (int i = 0; i < paquetesContinente.size() && movimientosGenerados < maxMovimientos; i++) {
                for (int j = i + 1; j < paquetesContinente.size() && movimientosGenerados < maxMovimientos; j++) {

                    Paquete paquete1 = paquetesContinente.get(i);
                    Paquete paquete2 = paquetesContinente.get(j);

                    SolucionVecino vecino = intentarIntercambio(solucionActual, paquete1, paquete2, "SWAP_INTELIGENTE");
                    if (vecino != null) {
                        movimientos.add(vecino);
                        movimientosGenerados++;
                    }
                }
            }
        }

        // Si no hemos generado suficientes movimientos, intentar intercambios entre continentes
        if (movimientosGenerados < maxMovimientos) {
            ArrayList<String> continentes = new ArrayList<>(paquetesPorContinente.keySet());

            for (int i = 0; i < continentes.size() && movimientosGenerados < maxMovimientos; i++) {
                for (int j = i + 1; j < continentes.size() && movimientosGenerados < maxMovimientos; j++) {

                    ArrayList<Paquete> paquetes1 = paquetesPorContinente.get(continentes.get(i));
                    ArrayList<Paquete> paquetes2 = paquetesPorContinente.get(continentes.get(j));

                    Collections.shuffle(paquetes1, aleatorio);
                    Collections.shuffle(paquetes2, aleatorio);

                    int maxPares = Math.min(3, Math.min(paquetes1.size(), paquetes2.size()));

                    for (int p1 = 0; p1 < maxPares && movimientosGenerados < maxMovimientos; p1++) {
                        for (int p2 = 0; p2 < maxPares && movimientosGenerados < maxMovimientos; p2++) {

                            Paquete paquete1 = paquetes1.get(p1);
                            Paquete paquete2 = paquetes2.get(p2);

                            SolucionVecino vecino = intentarIntercambio(solucionActual, paquete1, paquete2, "SWAP_INTERCONTINENTAL");
                            if (vecino != null) {
                                movimientos.add(vecino);
                                movimientosGenerados++;
                            }
                        }
                    }
                }
            }
        }

        return movimientos;
    }

    /**
     * Genera intercambios basados en optimización de capacidad de vuelos.
     */
    public ArrayList<SolucionVecino> generarSwapOptimizacionCapacidad(
            HashMap<Paquete, ArrayList<Vuelo>> solucionActual,
            int maxMovimientos) {

        ArrayList<SolucionVecino> movimientos = new ArrayList<>();

        // Identificar paquetes en rutas con alta utilización
        ArrayList<Paquete> paquetesAltaUtilizacion = new ArrayList<>();
        ArrayList<Paquete> paquetesBajaUtilizacion = new ArrayList<>();

        for (Paquete paquete : solucionActual.keySet()) {
            ArrayList<Vuelo> ruta = solucionActual.get(paquete);
            double utilizacionPromedio = calcularUtilizacionPromedioRuta(ruta);

            if (utilizacionPromedio > 0.8) {
                paquetesAltaUtilizacion.add(paquete);
            } else if (utilizacionPromedio < 0.5) {
                paquetesBajaUtilizacion.add(paquete);
            }
        }

        Collections.shuffle(paquetesAltaUtilizacion, aleatorio);
        Collections.shuffle(paquetesBajaUtilizacion, aleatorio);

        int movimientosGenerados = 0;

        // Intercambiar paquetes de alta utilización con los de baja utilización
        for (int i = 0; i < paquetesAltaUtilizacion.size() && movimientosGenerados < maxMovimientos; i++) {
            for (int j = 0; j < paquetesBajaUtilizacion.size() && movimientosGenerados < maxMovimientos; j++) {

                Paquete paqueteAlta = paquetesAltaUtilizacion.get(i);
                Paquete paqueteBaja = paquetesBajaUtilizacion.get(j);

                SolucionVecino vecino = intentarIntercambio(solucionActual, paqueteAlta, paqueteBaja, "SWAP_CAPACIDAD");
                if (vecino != null) {
                    // Verificar que realmente mejore la utilización
                    if (mejorUtilizacionCapacidad(solucionActual, vecino.solucion)) {
                        movimientos.add(vecino);
                        movimientosGenerados++;
                    }
                }
            }
        }

        return movimientos;
    }

    private SolucionVecino intentarIntercambio(HashMap<Paquete, ArrayList<Vuelo>> solucionActual,
                                               Paquete paquete1, Paquete paquete2, String tipoSwap) {

        ArrayList<Vuelo> ruta1 = solucionActual.get(paquete1);
        ArrayList<Vuelo> ruta2 = solucionActual.get(paquete2);

        if (!esIntercambioValido(paquete1, ruta2, paquete2, ruta1)) {
            return null;
        }

        HashMap<Paquete, ArrayList<Vuelo>> nuevaSolucion = new HashMap<>(solucionActual);
        nuevaSolucion.put(paquete1, new ArrayList<>(ruta2));
        nuevaSolucion.put(paquete2, new ArrayList<>(ruta1));

        if (!esSolucionFactible(nuevaSolucion, paquete1, paquete2, ruta1, ruta2)) {
            return null;
        }

        int peso = calcularPesoSolucion(nuevaSolucion);
        TSMove movimiento = new TSMove(tipoSwap, paquete1, paquete2, ruta1, ruta2, ruta2, ruta1);

        SolucionVecino vecino = new SolucionVecino(nuevaSolucion, peso, movimiento);
        vecino.deltaEvaluacion = peso - calcularPesoSolucion(solucionActual);

        return vecino;
    }

    /**
     * Verifica si el intercambio entre dos paquetes es válido.
     */
    private boolean esIntercambioValido(Paquete paquete1, ArrayList<Vuelo> rutaPara1,
                                        Paquete paquete2, ArrayList<Vuelo> rutaPara2) {

        // Verificar que las rutas sean geográficamente compatibles
        if (!esRutaCompatible(paquete1, rutaPara1) || !esRutaCompatible(paquete2, rutaPara2)) {
            return false;
        }

        // Verificar restricciones de capacidad
        int productos1 = paquete1.getProductos() != null ? paquete1.getProductos().size() : 1;
        int productos2 = paquete2.getProductos() != null ? paquete2.getProductos().size() : 1;

        if (!cabeEnCapacidadRuta(rutaPara1, productos1) || !cabeEnCapacidadRuta(rutaPara2, productos2)) {
            return false;
        }

        // Verificar deadlines
        if (!cumpleDeadline(paquete1, rutaPara1) || !cumpleDeadline(paquete2, rutaPara2)) {
            return false;
        }

        return true;
    }

    /**
     * Evalúa si el intercambio es potencialmente beneficioso.
     */
    private boolean esIntercambioBeneficioso(Paquete paquete1, ArrayList<Vuelo> rutaActual1, ArrayList<Vuelo> rutaNueva1,
                                             Paquete paquete2, ArrayList<Vuelo> rutaActual2, ArrayList<Vuelo> rutaNueva2) {

        // Calcular mejora en tiempo de entrega
        double tiempoActual1 = calcularTiempoRuta(rutaActual1);
        double tiempoNuevo1 = calcularTiempoRuta(rutaNueva1);
        double tiempoActual2 = calcularTiempoRuta(rutaActual2);
        double tiempoNuevo2 = calcularTiempoRuta(rutaNueva2);

        double mejoraTotal = (tiempoActual1 - tiempoNuevo1) + (tiempoActual2 - tiempoNuevo2);

        // El intercambio es beneficioso si reduce el tiempo total o mejora otros criterios
        if (mejoraTotal > 0) {
            return true;
        }

        // También considerar mejora en utilización de vuelos
        double utilizacionActual = calcularUtilizacionPromedioRuta(rutaActual1) + calcularUtilizacionPromedioRuta(rutaActual2);
        double utilizacionNueva = calcularUtilizacionPromedioRuta(rutaNueva1) + calcularUtilizacionPromedioRuta(rutaNueva2);

        // Preferir intercambios que equilibren mejor la utilización
        double diferenciaUtilizacion = Math.abs(calcularUtilizacionPromedioRuta(rutaActual1) - calcularUtilizacionPromedioRuta(rutaActual2));
        double nuevaDiferenciaUtilizacion = Math.abs(calcularUtilizacionPromedioRuta(rutaNueva1) - calcularUtilizacionPromedioRuta(rutaNueva2));

        return nuevaDiferenciaUtilizacion < diferenciaUtilizacion;
    }

    private boolean esRutaCompatible(Paquete paquete, ArrayList<Vuelo> ruta) {
        if (ruta == null || ruta.isEmpty()) {
            // Ruta vacía significa que ya está en destino
            return paquete.getUbicacionActual().getNombre().equals(paquete.getCiudadDestino().getNombre());
        }

        // Verificar que la ruta conecte origen con destino del paquete
        String origenEsperado = paquete.getUbicacionActual().getNombre();
        String destinoEsperado = paquete.getCiudadDestino().getNombre();

        String origenRuta = ruta.get(0).getAeropuertoOrigen().getCiudad().getNombre();
        String destinoRuta = ruta.get(ruta.size() - 1).getAeropuertoDestino().getCiudad().getNombre();

        return origenEsperado.equals(origenRuta) && destinoEsperado.equals(destinoRuta);
    }

    private boolean cabeEnCapacidadRuta(ArrayList<Vuelo> ruta, int cantidadProductos) {
        if (ruta == null || ruta.isEmpty()) return true;

        for (Vuelo vuelo : ruta) {
            if (vuelo.getCapacidadUsada() + cantidadProductos > vuelo.getCapacidadMaxima()) {
                return false;
            }
        }
        return true;
    }

    private boolean cumpleDeadline(Paquete paquete, ArrayList<Vuelo> ruta) {
        if (paquete.getFechaPedido() == null || paquete.getFechaLimiteEntrega() == null) {
            return true; // Sin información de tiempo, asumir válido
        }

        double tiempoRuta = calcularTiempoRuta(ruta);
        long horasDisponibles = ChronoUnit.HOURS.between(paquete.getFechaPedido(), paquete.getFechaLimiteEntrega());

        return tiempoRuta <= horasDisponibles;
    }

    private double calcularTiempoRuta(ArrayList<Vuelo> ruta) {
        if (ruta == null || ruta.isEmpty()) return 0.0;

        double tiempo = 0.0;
        for (Vuelo vuelo : ruta) {
            tiempo += vuelo.getTiempoTransporte();
        }

        // Agregar tiempo de conexiones
        if (ruta.size() > 1) {
            tiempo += (ruta.size() - 1) * 2.0;
        }

        return tiempo;
    }

    private double calcularUtilizacionPromedioRuta(ArrayList<Vuelo> ruta) {
        if (ruta == null || ruta.isEmpty()) return 0.0;

        double utilizacionTotal = 0.0;
        for (Vuelo vuelo : ruta) {
            utilizacionTotal += (double) vuelo.getCapacidadUsada() / vuelo.getCapacidadMaxima();
        }

        return utilizacionTotal / ruta.size();
    }

    private boolean mejorUtilizacionCapacidad(HashMap<Paquete, ArrayList<Vuelo>> solucionOriginal,
                                              HashMap<Paquete, ArrayList<Vuelo>> nuevaSolucion) {

        double utilizacionOriginal = calcularUtilizacionTotalSolucion(solucionOriginal);
        double utilizacionNueva = calcularUtilizacionTotalSolucion(nuevaSolucion);

        // Preferir mejor equilibrio en utilización (no demasiado alta ni demasiado baja)
        double optimo = 0.7; // Utilización objetivo

        double distanciaOriginal = Math.abs(utilizacionOriginal - optimo);
        double distanciaNueva = Math.abs(utilizacionNueva - optimo);

        return distanciaNueva < distanciaOriginal;
    }

    private double calcularUtilizacionTotalSolucion(HashMap<Paquete, ArrayList<Vuelo>> solucion) {
        HashMap<Integer, Double> utilizacionVuelos = new HashMap<>();

        for (ArrayList<Vuelo> ruta : solucion.values()) {
            for (Vuelo vuelo : ruta) {
                double utilizacion = (double) vuelo.getCapacidadUsada() / vuelo.getCapacidadMaxima();
                utilizacionVuelos.put(vuelo.getId(), utilizacion);
            }
        }

        if (utilizacionVuelos.isEmpty()) return 0.0;

        double sumaUtilizacion = 0.0;
        for (Double utilizacion : utilizacionVuelos.values()) {
            sumaUtilizacion += utilizacion;
        }

        return sumaUtilizacion / utilizacionVuelos.size();
    }

    private boolean esSolucionFactible(HashMap<Paquete, ArrayList<Vuelo>> solucion,
                                       Paquete paquete1, Paquete paquete2,
                                       ArrayList<Vuelo> rutaOriginal1, ArrayList<Vuelo> rutaOriginal2) {

        // Verificar restricciones de capacidad global
        HashMap<Integer, Integer> usoVuelos = new HashMap<>();

        for (Paquete paquete : solucion.keySet()) {
            ArrayList<Vuelo> ruta = solucion.get(paquete);
            int productos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;

            for (Vuelo vuelo : ruta) {
                usoVuelos.merge(vuelo.getId(), productos, Integer::sum);
            }
        }

        // Verificar que ningún vuelo exceda su capacidad
        for (Vuelo vuelo : vuelos) {
            int uso = usoVuelos.getOrDefault(vuelo.getId(), 0);
            if (uso > vuelo.getCapacidadMaxima()) {
                return false;
            }
        }

        // Verificar restricciones de almacenes en destinos
        HashMap<Integer, Integer> usoAlmacenes = new HashMap<>();

        for (Paquete paquete : solucion.keySet()) {
            ArrayList<Vuelo> ruta = solucion.get(paquete);
            int productos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;

            // Determinar aeropuerto de destino final
            Aeropuerto aeropuertoDestino;
            if (ruta.isEmpty()) {
                // Ya está en destino
                aeropuertoDestino = obtenerAeropuertoPorNombre(paquete.getCiudadDestino().getNombre());
            } else {
                aeropuertoDestino = ruta.get(ruta.size() - 1).getAeropuertoDestino();
            }

            if (aeropuertoDestino != null) {
                usoAlmacenes.merge(aeropuertoDestino.getId(), productos, Integer::sum);
            }
        }

        // Verificar capacidad de almacenes
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (aeropuerto.getAlmacen() != null) {
                int uso = usoAlmacenes.getOrDefault(aeropuerto.getId(), 0);
                if (uso > aeropuerto.getAlmacen().getCapacidadMaxima()) {
                    return false;
                }
            }
        }

        return true;
    }

    private Aeropuerto obtenerAeropuertoPorNombre(String nombreCiudad) {
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (aeropuerto.getCiudad() != null &&
                    aeropuerto.getCiudad().getNombre().equalsIgnoreCase(nombreCiudad)) {
                return aeropuerto;
            }
        }
        return null;
    }

    private int calcularPesoSolucion(HashMap<Paquete, ArrayList<Vuelo>> solucion) {
        // Placeholder para cálculo de peso - implementar lógica completa similar a ALNSSolver
        return solucion.size() * 100;
    }
}
