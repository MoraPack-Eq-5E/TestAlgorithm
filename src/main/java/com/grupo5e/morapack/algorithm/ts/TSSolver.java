package com.grupo5e.morapack.algorithm.ts;


import com.grupo5e.morapack.core.constants.Constantes;
import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.utils.LectorAeropuerto;
import com.grupo5e.morapack.utils.LectorProductos;
import com.grupo5e.morapack.utils.LectorVuelos;

import java.util.*;

public class TSSolver {

    private ArrayList<Paquete> paquetes;
    private ArrayList<Vuelo> vuelos;
    private ArrayList<Aeropuerto> aeropuertos;

    private HashMap<Vuelo, Integer> vueloOcupacion = new HashMap<>();
    private HashMap<Almacen, Integer> almacenOcupacion = new HashMap<>();

    private Map<HashMap<Paquete, ArrayList<Vuelo>>, Integer> solucion = new HashMap<>();
    private HashMap<Paquete, ArrayList<Vuelo>> mejorSolucion = new HashMap<>();
    private int mejorPeso = Integer.MAX_VALUE;

    private HashMap<Paquete, ArrayList<Vuelo>> solucionActualTS = new HashMap<>();
    private int pesoActual = Integer.MAX_VALUE;

    private LinkedList<TSMove> listaTabu = new LinkedList<>();
    private final int tamanioListaTabu = 50;
    private final int maxIteraciones = 1000;

    private Random aleatorio = new Random();

    public void resolver() {
        System.out.println("========== INICIANDO TABU SEARCH ==========");
        leerDatos();
        generarSolucionInicialAleatoria();

        for (int iter = 1; iter <= maxIteraciones; iter++) {
            SolucionVecino mejorVecino = encontrarMejorVecino();

            if (mejorVecino == null) {
                System.out.println("No se encontraron vecinos viables en la iteración " + iter);
                continue;
            }

            // Aplicar movimiento
            solucionActualTS = mejorVecino.solucion;
            pesoActual = mejorVecino.peso;

            // Actualizar lista Tabú
            listaTabu.add(mejorVecino.movimiento);
            if (listaTabu.size() > tamanioListaTabu) {
                listaTabu.removeFirst();
            }

            // Mejor solución global
            if (pesoActual < mejorPeso) {
                mejorSolucion.clear();
                mejorSolucion.putAll(solucionActualTS);
                mejorPeso = pesoActual;
                System.out.println(">> Iteración " + iter + ": Nueva mejor solución con peso " + mejorPeso);
            }
        }

        System.out.println("========== BÚSQUEDA FINALIZADA ==========");
        System.out.println("Peso final de la mejor solución: " + mejorPeso);
        System.out.println("Cantidad de paquetes asignados: " + mejorSolucion.size());
    }

    private void leerDatos() {
        this.aeropuertos = new LectorAeropuerto(Constantes.RUTA_ARCHIVO_INFO_AEROPUERTOS).leerAeropuertos();
        this.paquetes = new LectorProductos(Constantes.RUTA_ARCHIVO_PRODUCTOS, this.aeropuertos).leerProductos();
        this.vuelos = new LectorVuelos(Constantes.RUTA_ARCHIVO_VUELOS, this.aeropuertos).leerVuelos();
    }

    private void generarSolucionInicialAleatoria() {
        System.out.println("=== GENERANDO SOLUCIÓN INICIAL ALEATORIA ===");
        System.out.println("Probabilidad de asignación: " + (Constantes.PROBABILIDAD_ASIGNACION_ALEATORIA * 100) + "%");

        HashMap<Paquete, ArrayList<Vuelo>> solucionInicial = new HashMap<>();
        int paquetesAsignados = 0;

        ArrayList<Paquete> paquetesBarajados = new ArrayList<>(paquetes);
        Collections.shuffle(paquetesBarajados, aleatorio);

        for (Paquete paquete : paquetesBarajados) {
            if (aleatorio.nextDouble() < Constantes.PROBABILIDAD_ASIGNACION_ALEATORIA) {
                ArrayList<Vuelo> rutaAleatoria = generarRutaAleatoria(paquete);

                if (rutaAleatoria != null && !rutaAleatoria.isEmpty()) {
                    int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;

                    if (cabeEnCapacidad(rutaAleatoria, conteoProductos)) {
                        Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino().getNombre());
                        if (aeropuertoDestino != null &&
                                puedeAsignarConOptimizacionEspacio(paquete, rutaAleatoria, solucionInicial)) {

                            solucionInicial.put(paquete, rutaAleatoria);
                            actualizarCapacidadesVuelos(rutaAleatoria, conteoProductos);
                            incrementarOcupacionAlmacen(aeropuertoDestino, conteoProductos);
                            paquetesAsignados++;
                        }
                    }
                }
            }
        }

        int pesoSolucion = calcularPesoSolucion(solucionInicial);
        solucion.put(solucionInicial, pesoSolucion);

        System.out.println("Solución inicial aleatoria generada: " + paquetesAsignados + " paquetes asignados.");
        System.out.println("Peso de la solución inicial: " + pesoSolucion);
        System.out.println("==============================================");

        mejorSolucion.putAll(solucionInicial);
        mejorPeso = pesoSolucion;
        solucionActualTS.clear();
        solucionActualTS.putAll(solucionInicial);
        pesoActual = pesoSolucion;
    }

    private ArrayList<Vuelo> generarRutaAleatoria(Paquete paquete) {
        ArrayList<Vuelo> candidatos = new ArrayList<>();

        for (Vuelo vuelo : vuelos) {
            String origenVuelo = vuelo.getAeropuertoOrigen().getCiudad().getNombre();
            String destinoVuelo = vuelo.getAeropuertoDestino().getCiudad().getNombre();
            String origenPaquete = paquete.getUbicacionActual().getNombre();
            String destinoPaquete = paquete.getCiudadDestino().getNombre();

            if (origenVuelo.equals(origenPaquete) &&
                    destinoVuelo.equals(destinoPaquete)) {

                candidatos.add(vuelo);
            }
        }

        if (candidatos.isEmpty()) return null;

        Vuelo seleccionado = candidatos.get(aleatorio.nextInt(candidatos.size()));
        ArrayList<Vuelo> ruta = new ArrayList<>();
        ruta.add(seleccionado);
        return ruta;
    }


    private boolean cabeEnCapacidad(List<Vuelo> ruta, int cantidad) {
        for (Vuelo vuelo : ruta) {
            int ocupacionActual = vueloOcupacion.getOrDefault(vuelo, 0);
            if (ocupacionActual + cantidad > vuelo.getCapacidadMaxima()) {
                return false;
            }
        }
        return true;
    }

    private Aeropuerto obtenerAeropuertoPorCiudad(String ciudad) {
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (aeropuerto.getCiudad().getNombre().equalsIgnoreCase(ciudad)) {
                return aeropuerto;
            }
        }
        return null;
    }

    private boolean puedeAsignarConOptimizacionEspacio(Paquete paquete, List<Vuelo> ruta, HashMap<Paquete, ArrayList<Vuelo>> solucionParcial) {
        if (ruta == null || ruta.isEmpty()) return false;

        Vuelo ultimo = ruta.get(ruta.size() - 1);
        if (ultimo == null || ultimo.getAeropuertoDestino() == null || ultimo.getAeropuertoDestino().getAlmacen() == null) return false;

        Almacen almacen = ultimo.getAeropuertoDestino().getAlmacen();
        int ocupacion = almacenOcupacion.getOrDefault(almacen, 0);
        int productos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;

        return ocupacion + productos <= almacen.getCapacidadMaxima();
    }

    private void actualizarCapacidadesVuelos(List<Vuelo> ruta, int cantidad) {
        for (Vuelo vuelo : ruta) {
            int actual = vueloOcupacion.getOrDefault(vuelo, 0);
            vueloOcupacion.put(vuelo, actual + cantidad);
        }
    }

    private void incrementarOcupacionAlmacen(Aeropuerto aeropuertoDestino, int cantidad) {
        if (aeropuertoDestino != null && aeropuertoDestino.getAlmacen() != null) {
            Almacen almacen = aeropuertoDestino.getAlmacen();
            int actual = almacenOcupacion.getOrDefault(almacen, 0);
            almacenOcupacion.put(almacen, actual + cantidad);
        }
    }

    private int calcularPesoSolucion(HashMap<Paquete, ArrayList<Vuelo>> solucion) {
        int pesoTotal = 0;
        for (Map.Entry<Paquete, ArrayList<Vuelo>> entry : solucion.entrySet()) {
            ArrayList<Vuelo> ruta = entry.getValue();
            if (ruta != null && !ruta.isEmpty()) {
                for (Vuelo vuelo : ruta) {
                    pesoTotal += vuelo.getTiempoTransporte();
                }
                if (ruta.size() > 1) {
                    pesoTotal += (ruta.size() - 1) * 2; // penalización por escalas
                }
            }
        }
        return pesoTotal;
    }

    private SolucionVecino encontrarMejorVecino() {
        List<SolucionVecino> vecinos = new ArrayList<>();

        // Convertir el HashMap<Almacen, Integer> → HashMap<Aeropuerto, Integer>
        HashMap<Aeropuerto, Integer> almacenOcupacionPorAeropuerto = new HashMap<>();
        for (Map.Entry<Almacen, Integer> entry : almacenOcupacion.entrySet()) {
            Almacen almacen = entry.getKey();
            Integer ocupacion = entry.getValue();

            // Buscar el aeropuerto correspondiente al almacén
            for (Aeropuerto aeropuerto : aeropuertos) {
                if (aeropuerto.getAlmacen() != null && aeropuerto.getAlmacen().equals(almacen)) {
                    almacenOcupacionPorAeropuerto.put(aeropuerto, ocupacion);
                    break;
                }
            }
        }


        // Constructores correctos basados en tus clases
        TSRelocate operadorRelocate = new TSRelocate(aeropuertos, vuelos, almacenOcupacionPorAeropuerto);
        TSSwap operadorSwap = new TSSwap(aeropuertos, vuelos, almacenOcupacionPorAeropuerto);

        // Llamadas a los métodos correctos de generación
        vecinos.addAll(operadorRelocate.generarMovimientosRelocate(
                solucionActualTS,
                Constantes.MAX_VECINOS_TS // usa una constante configurable si la tienes
        ));

        vecinos.addAll(operadorSwap.generarMovimientosSwap(
                solucionActualTS,
                Constantes.MAX_VECINOS_TS
        ));

        // Buscar el mejor vecino (fuera de la lista Tabú o que cumple criterio de aspiración)
        SolucionVecino mejor = null;

        for (SolucionVecino vecino : vecinos) {
            if (vecino == null) continue;

            if (!listaTabu.contains(vecino.movimiento)) {
                if (mejor == null || vecino.peso < mejor.peso) {
                    mejor = vecino;
                }
            } else if (aspiracion(vecino)) {
                // Permitir Tabú si cumple criterio de aspiración
                if (mejor == null || vecino.peso < mejor.peso) {
                    mejor = vecino;
                }
            }
        }

        return mejor;
    }

    private boolean aspiracion(SolucionVecino vecino) {
        return vecino.peso < mejorPeso;
    }


}
