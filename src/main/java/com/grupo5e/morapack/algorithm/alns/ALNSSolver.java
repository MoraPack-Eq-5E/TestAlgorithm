package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.core.model.Producto;
import com.grupo5e.morapack.core.constants.Constantes;
import com.grupo5e.morapack.utils.LectorAeropuerto;
import com.grupo5e.morapack.utils.LectorVuelos;
import com.grupo5e.morapack.utils.LectorProductos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * ALNSSolver adaptado desde el Solution que proporcionaste.
 * Mantiene la lógica lo más fiel posible, cambiando únicamente nombres y constantes
 * para integrarse en tu paquete/comunidad de modelos.
 */
public class ALNSSolver {
    private HashMap<HashMap<Paquete, ArrayList<Vuelo>>, Integer> solucion;
    private LectorAeropuerto lectorAeropuertos;
    private LectorVuelos lectorVuelos;
    private LectorProductos lectorProductos;

    // Cache robusta Ciudad→Aeropuerto por nombre
    private Map<String, Aeropuerto> cacheNombreCiudadAeropuerto;
    private ArrayList<Aeropuerto> aeropuertos;
    private ArrayList<Vuelo> vuelos;
    private ArrayList<Paquete> paquetes;

    // Unitización
    private static final boolean HABILITAR_UNITIZACION_PRODUCTO = true;
    private ArrayList<Paquete> paquetesOriginales;

    // Ancla temporal T0
    private LocalDateTime T0;
    // Ocupación de almacenes
    private HashMap<Aeropuerto, Integer> ocupacionAlmacenes;
    private HashMap<Aeropuerto, int[]> ocupacionTemporalAlmacenes;
    // Mejor solución y random
    private HashMap<HashMap<Paquete, ArrayList<Vuelo>>, Integer> mejorSolucion;
    private Random aleatorio;

    // ALNS operators
    private ALNSDestruction operadoresDestruccion;
    private ALNSRepair operadoresReparacion;
    private double[][] pesosOperadores;
    private double[][] puntajesOperadores;
    private int[][] usoOperadores;
    private double temperatura;
    private double tasaEnfriamiento;
    private int maxIteraciones;
    private int tamanoSegmento;

    // Diversificación / intensificación
    private int contadorEstancamiento;
    private int umbralDiversificacion;
    private boolean modoDiversificacion;
    private int ultimaIteracionMejora;
    private double factorDiversificacion;

    // Pool no asignados
    private ArrayList<Paquete> poolNoAsignados;

    // Diversificación extrema / restart
    private int iteracionesDesdeMejoraSignificativa;
    private int contadorRestarts;
    private double ultimoPesoSignificativo;

    // Horizon days
    private static final int HORIZON_DAYS = 4;
    private static final boolean DEBUG_MODE = false;

    public ALNSSolver() {
        this.lectorAeropuertos = new LectorAeropuerto(Constantes.RUTA_ARCHIVO_INFO_AEROPUERTOS);
        this.solucion = new HashMap<>();
        this.aeropuertos = lectorAeropuertos.leerAeropuertos();
        this.lectorVuelos = new LectorVuelos(Constantes.RUTA_ARCHIVO_VUELOS, this.aeropuertos);
        this.vuelos = lectorVuelos.leerVuelos();
        this.lectorProductos = new LectorProductos(Constantes.RUTA_ARCHIVO_PRODUCTOS, this.aeropuertos);
        this.paquetesOriginales = lectorProductos.leerProductos();

        if (HABILITAR_UNITIZACION_PRODUCTO) {
            this.paquetes = expandirPaquetesAUnidadesProducto(this.paquetesOriginales);
            System.out.println("UNITIZACIÓN APLICADA: " + this.paquetesOriginales.size() +
                               " paquetes originales → " + this.paquetes.size() + " unidades de producto");
        } else {
            this.paquetes = new ArrayList<>(this.paquetesOriginales);
            System.out.println("UNITIZACIÓN DESHABILITADA: Usando paquetes originales");
        }

        this.ocupacionAlmacenes = new HashMap<>();
        this.ocupacionTemporalAlmacenes = new HashMap<>();

        inicializarCacheCiudadAeropuerto();
        inicializarT0();

        this.aleatorio = new Random(System.currentTimeMillis());

        this.operadoresDestruccion = new ALNSDestruction();
        this.operadoresReparacion = new ALNSRepair(aeropuertos, vuelos, ocupacionAlmacenes);

        inicializarParametrosALNS();

        inicializarOcupacionAlmacenes();
        inicializarOcupacionTemporalAlmacenes();
    }

    private void inicializarParametrosALNS() {
        int numOperadoresDestruccion = 4;
        int numOperadoresReparacion = 4;

        this.pesosOperadores = new double[numOperadoresDestruccion][numOperadoresReparacion];
        this.puntajesOperadores = new double[numOperadoresDestruccion][numOperadoresReparacion];
        this.usoOperadores = new int[numOperadoresDestruccion][numOperadoresReparacion];

        for (int i = 0; i < numOperadoresDestruccion; i++) {
            for (int j = 0; j < numOperadoresReparacion; j++) {
                this.pesosOperadores[i][j] = 1.0;
                this.puntajesOperadores[i][j] = 0.0;
                this.usoOperadores[i][j] = 0;
            }
        }

        this.temperatura = 100.0;
        this.tasaEnfriamiento = 0.98;
        //OJO CON ESTE
        this.maxIteraciones = 500;
        this.tamanoSegmento = 25;

        this.contadorEstancamiento = 0;
        this.umbralDiversificacion = 100;
        this.modoDiversificacion = false;
        this.ultimaIteracionMejora = 0;
        this.factorDiversificacion = 1.0;

        this.poolNoAsignados = new ArrayList<>();

        this.iteracionesDesdeMejoraSignificativa = 0;
        this.contadorRestarts = 0;
        this.ultimoPesoSignificativo = 0.0;
    }

    public void resolver() {
        System.out.println("Iniciando solución ALNS");
        System.out.println("Lectura de aeropuertos");
        System.out.println("Aeropuertos leídos: " + this.aeropuertos.size());
        System.out.println("Lectura de vuelos");
        System.out.println("Vuelos leídos: " + this.vuelos.size());
        System.out.println("Lectura de productos");
        System.out.println("Productos leídos: " + this.paquetes.size());

        //SE GENERA UNA SOLUCION QUE SE MODIFICARA HASTA HALLAR LA CORRECTA O MEJOR
        System.out.println("\n=== GENERANDO SOLUCIÓN INICIAL ===");
        this.generarSolucionInicial();

        System.out.println("Validando solución...");
        boolean esValida = this.esSolucionValida();
        System.out.println("Solución válida: " + (esValida ? "SÍ" : "NO"));

        this.imprimirDescripcionSolucion(1);

        mejorSolucion = new HashMap<>(solucion);

        inicializarPoolNoAsignados();

        System.out.println("\n=== INICIANDO ALGORITMO ALNS ===");
        ejecutarAlgoritmoALNS();

        System.out.println("\n=== RESULTADO FINAL ALNS ===");
        this.imprimirDescripcionSolucion(2);
    }

    private void inicializarPoolNoAsignados() {
        poolNoAsignados.clear();
        if (solucion.isEmpty()) return;
        HashMap<Paquete, ArrayList<Vuelo>> solucionActual = solucion.keySet().iterator().next();
        for (Paquete paquete : paquetes) {
            if (!solucionActual.containsKey(paquete)) {
                poolNoAsignados.add(paquete);
            }
        }
        if (Constantes.LOGGING_VERBOSO) {
            System.out.println("Pool de no asignados inicializado: " + poolNoAsignados.size() + " paquetes disponibles para expansión ALNS");
        }
    }

    private ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> expandirConPaquetesNoAsignados(
            ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> paquetesDestruidos, int maxAgregar) {

        if (poolNoAsignados.isEmpty() || maxAgregar <= 0) {
            return paquetesDestruidos;
        }

        ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> listaExpandida = new ArrayList<>(paquetesDestruidos);

        double ratioPool = (double) poolNoAsignados.size() / paquetes.size();
        double probabilidadExpansion;

        if (ratioPool > 0.5) {
            probabilidadExpansion = modoDiversificacion ? 0.9 : 0.7;
        } else if (ratioPool > 0.3) {
            probabilidadExpansion = modoDiversificacion ? 0.7 : 0.5;
        } else if (ratioPool > 0.1) {
            probabilidadExpansion = modoDiversificacion ? 0.5 : 0.3;
        } else {
            probabilidadExpansion = modoDiversificacion ? 0.3 : 0.1;
        }

        if (aleatorio.nextDouble() < probabilidadExpansion) {
            ArrayList<Paquete> noAsignadosOrdenados = new ArrayList<>(poolNoAsignados);
            noAsignadosOrdenados.sort((p1, p2) -> {
                LocalDateTime d1 = p1.getFechaLimiteEntrega();
                LocalDateTime d2 = p2.getFechaLimiteEntrega();
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d1.compareTo(d2);
            });

            int maxDinamico;
            if (ratioPool > 0.5) {
                maxDinamico = Math.min(200, poolNoAsignados.size());
            } else if (ratioPool > 0.3) {
                maxDinamico = Math.min(100, poolNoAsignados.size());
            } else {
                maxDinamico = Math.min(50, poolNoAsignados.size());
            }

            int agregar = Math.min(maxDinamico, noAsignadosOrdenados.size());

            for (int i = 0; i < agregar; i++) {
                Paquete paquete = noAsignadosOrdenados.get(i);
                listaExpandida.add(new java.util.AbstractMap.SimpleEntry<>(paquete, new ArrayList<>()));
            }

            if (Constantes.LOGGING_VERBOSO) {
                System.out.println("Expansión ALNS: Agregando " + agregar + " paquetes no asignados para exploración" +
                                 " (Pool: " + poolNoAsignados.size() + "/" + paquetes.size() +
                                 " = " + String.format("%.1f%%", ratioPool * 100) +
                                 ", Prob: " + String.format("%.0f%%", probabilidadExpansion * 100) + ")");
            }
        }

        return listaExpandida;
    }

    private void actualizarPoolNoAsignados(HashMap<Paquete, ArrayList<Vuelo>> solucionActual) {
        poolNoAsignados.clear();
        for (Paquete paquete : paquetes) {
            if (!solucionActual.containsKey(paquete)) {
                poolNoAsignados.add(paquete);
            }
        }
    }

    private void ejecutarAlgoritmoALNS() {
        HashMap<Paquete, ArrayList<Vuelo>> solucionActual = null;
        int pesoActual = Integer.MAX_VALUE;

        for (Map.Entry<HashMap<Paquete, ArrayList<Vuelo>>, Integer> entrada : solucion.entrySet()) {
            solucionActual = new HashMap<>(entrada.getKey());
            pesoActual = entrada.getValue();
            break;
        }

        if (solucionActual == null) {
            System.out.println("Error: No se pudo obtener la solución inicial");
            return;
        }

        System.out.println("Peso de solución inicial: " + pesoActual);

        ultimoPesoSignificativo = pesoActual;
        iteracionesDesdeMejoraSignificativa = 0;

        int mejorPeso = pesoActual;
        int mejoras = 0;
        int conteoSinMejoras = 0;

        for (int iteracion = 0; iteracion < maxIteraciones; iteracion++) {
            if (Constantes.LOGGING_VERBOSO || iteracion % Constantes.INTERVALO_LOG_ITERACION == 0) {
                System.out.println("ALNS Iteración " + iteracion + "/" + maxIteraciones);
            }

            int[] operadoresSeleccionados = seleccionarOperadores();
            int operadorDestruccion = operadoresSeleccionados[0];
            int operadorReparacion = operadoresSeleccionados[1];

            if (Constantes.LOGGING_VERBOSO) {
                System.out.println("  Operadores seleccionados: Destrucción=" + operadorDestruccion + ", Reparación=" + operadorReparacion);
            }

            HashMap<Paquete, ArrayList<Vuelo>> solucionTemporal = new HashMap<>(solucionActual);

            Map<Vuelo, Integer> snapshotCapacidades = crearSnapshotCapacidades();
            Map<Aeropuerto, Integer> snapshotAlmacenes = crearSnapshotAlmacenes();

            if (Constantes.LOGGING_VERBOSO) {
                System.out.println("  Aplicando operador de destrucción...");
            }
            long tiempoInicio = System.currentTimeMillis();
            ALNSDestruction.ResultadoDestruccion resultadoDestruccion = aplicarOperadorDestruccion(
                solucionTemporal, operadorDestruccion);
            long tiempoFin = System.currentTimeMillis();

            if (Constantes.LOGGING_VERBOSO) {
                System.out.println("  Operador de destrucción completado en " + (tiempoFin - tiempoInicio) + "ms");
            }

            if (resultadoDestruccion == null || resultadoDestruccion.getPaquetesDestruidos().isEmpty()) {
                if (Constantes.LOGGING_VERBOSO) {
                    System.out.println("  No se pudo destruir nada, continuando...");
                }
                continue;
            }

            if (Constantes.LOGGING_VERBOSO) {
                System.out.println("  Paquetes destruidos: " + resultadoDestruccion.getPaquetesDestruidos().size());
            }

            solucionTemporal = new HashMap<>(resultadoDestruccion.getSolucionParcial());
            reconstruirCapacidadesDesdeSolucion(solucionTemporal);
            reconstruirAlmacenesDesdeSolucion(solucionTemporal);

            ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> paquetesExpandidos =
                expandirConPaquetesNoAsignados(resultadoDestruccion.getPaquetesDestruidos(), 100);

            ALNSRepair.ResultadoReparacion resultadoReparacion = aplicarOperadorReparacion(
                solucionTemporal, operadorReparacion, paquetesExpandidos);

            if (resultadoReparacion == null || !resultadoReparacion.esExitoso()) {
                restaurarCapacidades(snapshotCapacidades);
                restaurarAlmacenes(snapshotAlmacenes);
                continue;
            }

            solucionTemporal = new HashMap<>(resultadoReparacion.getSolucionReparada());
            reconstruirCapacidadesDesdeSolucion(solucionTemporal);
            reconstruirAlmacenesDesdeSolucion(solucionTemporal);

            int pesoTemporal = calcularPesoSolucion(solucionTemporal);

            usoOperadores[operadorDestruccion][operadorReparacion]++;

            boolean aceptada = false;
            double ratioMejora = 0.0;

            if (pesoTemporal > pesoActual) {
                ratioMejora = (double)(pesoTemporal - pesoActual) / Math.max(pesoActual, 1);
                solucionActual = solucionTemporal;
                pesoActual = pesoTemporal;
                aceptada = true;

                if (pesoTemporal > mejorPeso) {
                    mejorPeso = pesoTemporal;
                    mejorSolucion.clear();
                    mejorSolucion.put(new HashMap<>(solucionActual), pesoActual);
                    puntajesOperadores[operadorDestruccion][operadorReparacion] += 300;
                    mejoras++;
                    conteoSinMejoras = 0;
                    ultimaIteracionMejora = iteracion;
                    contadorEstancamiento = 0;
                    modoDiversificacion = false;
                    actualizarPoolNoAsignados(solucionActual);

                    if (ratioMejora >= (Constantes.UMBRAL_MEJORA_SIGNIFICATIVA / 100.0)) {
                        iteracionesDesdeMejoraSignificativa = 0;
                        ultimoPesoSignificativo = mejorPeso;
                    } else {
                        iteracionesDesdeMejoraSignificativa++;
                    }

                    System.out.println("Iteración " + iteracion + ": ¡Nueva mejor solución! Peso: " + mejorPeso +
                                     " (mejora: " + String.format("%.2f%%", ratioMejora * 100) + ")" +
                                     " | No asignados: " + poolNoAsignados.size());
                } else if (ratioMejora > 0.05) {
                    puntajesOperadores[operadorDestruccion][operadorReparacion] += 100;
                    conteoSinMejoras = Math.max(0, conteoSinMejoras - 5);
                    actualizarPoolNoAsignados(solucionActual);
                } else if (ratioMejora > 0.01) {
                    puntajesOperadores[operadorDestruccion][operadorReparacion] += 50;
                    conteoSinMejoras = Math.max(0, conteoSinMejoras - 2);
                    actualizarPoolNoAsignados(solucionActual);
                } else {
                    puntajesOperadores[operadorDestruccion][operadorReparacion] += 25;
                    actualizarPoolNoAsignados(solucionActual);
                }
            } else {
                double delta = pesoTemporal - pesoActual;
                double temperaturaAjustada = temperatura * (1.0 + 0.1 * Math.random());
                double probabilidad = Math.exp(delta / temperaturaAjustada);

                if (aleatorio.nextDouble() < probabilidad) {
                    solucionActual = solucionTemporal;
                    pesoActual = pesoTemporal;
                    aceptada = true;
                    actualizarPoolNoAsignados(solucionActual);
                    puntajesOperadores[operadorDestruccion][operadorReparacion] += 15;
                    conteoSinMejoras++;
                } else {
                    puntajesOperadores[operadorDestruccion][operadorReparacion] += 5;
                    conteoSinMejoras++;
                }

                if (!aceptada) {
                    iteracionesDesdeMejoraSignificativa++;
                }
            }

            if (!aceptada) {
                restaurarCapacidades(snapshotCapacidades);
                restaurarAlmacenes(snapshotAlmacenes);
                conteoSinMejoras++;
            }

            contadorEstancamiento = iteracion - ultimaIteracionMejora;
            if (contadorEstancamiento > umbralDiversificacion && !modoDiversificacion) {
                modoDiversificacion = true;
                factorDiversificacion = 1.5;
                temperatura *= 2.0;
                if (Constantes.LOGGING_VERBOSO) {
                    System.out.println("Iteración " + iteracion + ": Activando modo DIVERSIFICACIÓN");
                }
            } else if (modoDiversificacion && contadorEstancamiento <= umbralDiversificacion / 2) {
                modoDiversificacion = false;
                factorDiversificacion = 1.0;
                if (Constantes.LOGGING_VERBOSO) {
                    System.out.println("Iteración " + iteracion + ": Volviendo a modo INTENSIFICACIÓN");
                }
            }

            // Diversificación extrema / Restart
            if (iteracionesDesdeMejoraSignificativa >= Constantes.UMBRAL_ESTANCAMIENTO_PARA_RESTART &&
                contadorRestarts < Constantes.MAX_RESTARTS) {

                solucionActual = aplicarDiversificacionExtrema(solucionActual, iteracion);
                pesoActual = calcularPesoSolucion(solucionActual);

                if (pesoActual > mejorPeso) {
                    mejorPeso = pesoActual;
                    mejorSolucion.clear();
                    mejorSolucion.put(new HashMap<>(solucionActual), pesoActual);
                    mejoras++;
                    System.out.println("🎉 ¡Diversificación extrema encontró mejor solución! Peso: " + mejorPeso);
                }

                reconstruirCapacidadesDesdeSolucion(solucionActual);
                reconstruirAlmacenesDesdeSolucion(solucionActual);
            }

            if ((iteracion + 1) % tamanoSegmento == 0) {
                actualizarPesosOperadores();
                temperatura *= tasaEnfriamiento;

                if (iteracion % 100 == 0) {
                    System.out.println("Iteración " + iteracion +
                                     " | Mejor peso: " + mejorPeso +
                                     " | Temperatura: " + String.format("%.2f", temperatura) +
                                     " | Modo: " + (modoDiversificacion ? "DIVERSIFICACIÓN" : "INTENSIFICACIÓN"));
                }
            }

            if (contadorEstancamiento > 300) {
                if (Constantes.LOGGING_VERBOSO) {
                    System.out.println("Parada temprana en iteración " + iteracion +
                                     " (sin mejoras por " + contadorEstancamiento + " iteraciones)");
                }
                break;
            }
        }

        solucion.clear();
        solucion.putAll(mejorSolucion);

        System.out.println("ALNS completado:");
        System.out.println("  Mejoras encontradas: " + mejoras);
        System.out.println("  Peso final: " + (mejorSolucion.isEmpty() ? 0 : mejorSolucion.values().iterator().next()));
        if (Constantes.LOGGING_VERBOSO) {
            System.out.println("  Temperatura final: " + temperatura);
        }
    }

    private HashMap<Paquete, ArrayList<Vuelo>> aplicarDiversificacionExtrema(
            HashMap<Paquete, ArrayList<Vuelo>> solucionActual, int iteracion) {

        System.out.println("\n🚀 ACTIVANDO DIVERSIFICACIÓN EXTREMA 🚀");
        System.out.println("Iteración " + iteracion + ": " + iteracionesDesdeMejoraSignificativa +
                         " iteraciones sin mejora significativa");
        System.out.println("Restart #" + (contadorRestarts + 1) + "/" + Constantes.MAX_RESTARTS);

        HashMap<Paquete, ArrayList<Vuelo>> nuevaSolucion;

        switch (contadorRestarts % 3) {
            case 0:
                System.out.println("Estrategia: DESTRUCCIÓN EXTREMA (" + (int)(Constantes.RATIO_DESTRUCCION_EXTREMA*100) + "%)");
                nuevaSolucion = destruccionExtrema(solucionActual);
                break;
            case 1:
                System.out.println("Estrategia: RESTART GREEDY COMPLETO");
                nuevaSolucion = restartGreedy();
                break;
            case 2:
                System.out.println("Estrategia: RESTART HÍBRIDO");
                nuevaSolucion = restartHibrido(solucionActual);
                break;
            default:
                System.out.println("Estrategia: DESTRUCCIÓN EXTREMA (fallback)");
                nuevaSolucion = destruccionExtrema(solucionActual);
                break;
        }

        contadorRestarts++;
        iteracionesDesdeMejoraSignificativa = 0;

        temperatura = 100.0;

        actualizarPoolNoAsignados(nuevaSolucion);

        int nuevoPeso = calcularPesoSolucion(nuevaSolucion);
        System.out.println("Peso después de diversificación extrema: " + nuevoPeso);
        System.out.println("Paquetes asignados: " + nuevaSolucion.size() + "/" + paquetes.size());
        System.out.println("=== FIN DIVERSIFICACIÓN EXTREMA ===\n");

        return nuevaSolucion;
    }

    private HashMap<Paquete, ArrayList<Vuelo>> destruccionExtrema(HashMap<Paquete, ArrayList<Vuelo>> solucionActual) {
        HashMap<Paquete, ArrayList<Vuelo>> nuevaSolucion = new HashMap<>(solucionActual);

        ArrayList<Paquete> asignados = new ArrayList<>(nuevaSolucion.keySet());
        Collections.shuffle(asignados, aleatorio);

        int paquetesAEliminar = (int)(asignados.size() * Constantes.RATIO_DESTRUCCION_EXTREMA);

        for (int i = 0; i < paquetesAEliminar && i < asignados.size(); i++) {
            nuevaSolucion.remove(asignados.get(i));
        }

        System.out.println("Destruidos " + paquetesAEliminar + "/" + asignados.size() + " paquetes");

        reconstruirCapacidadesDesdeSolucion(nuevaSolucion);
        reconstruirAlmacenesDesdeSolucion(nuevaSolucion);

        return nuevaSolucion;
    }

    private HashMap<Paquete, ArrayList<Vuelo>> restartGreedy() {
        for (Vuelo f : vuelos) {
            f.setCapacidadUsada(0);
        }
        inicializarOcupacionAlmacenes();

        HashMap<Paquete, ArrayList<Vuelo>> nuevaSolucion = new HashMap<>();

        ArrayList<Paquete> ordenados = new ArrayList<>(paquetes);

        switch (contadorRestarts % 4) {
            case 0:
                ordenados.sort((p1, p2) -> Double.compare(p1.getPrioridad(), p2.getPrioridad()));
                System.out.println("Ordenamiento: Prioridad inversa");
                break;
            case 1:
                ordenados.sort((p1, p2) -> {
                    int a = p1.getProductos() != null ? p1.getProductos().size() : 1;
                    int b = p2.getProductos() != null ? p2.getProductos().size() : 1;
                    return Integer.compare(b, a);
                });
                System.out.println("Ordenamiento: Más productos primero");
                break;
            case 2:
                ordenados.sort((p1, p2) -> {
                    boolean p1Cont = p1.getUbicacionActual().getContinente() == p1.getCiudadDestino().getContinente();
                    boolean p2Cont = p2.getUbicacionActual().getContinente() == p2.getCiudadDestino().getContinente();
                    return Boolean.compare(p1Cont, p2Cont);
                });
                System.out.println("Ordenamiento: Intercontinentales primero");
                break;
            case 3:
                Collections.shuffle(ordenados, aleatorio);
                System.out.println("Ordenamiento: Aleatorio");
                break;
        }

        int asignados = 0;
        for (Paquete p : ordenados) {
            ArrayList<Vuelo> mejorRuta = encontrarMejorRutaConVentanasTiempo(p, nuevaSolucion);
            if (mejorRuta != null) {
                int cnt = p.getProductos() != null ? p.getProductos().size() : 1;
                if (puedeAsignarConOptimizacionEspacio(p, mejorRuta, nuevaSolucion)) {
                    nuevaSolucion.put(p, mejorRuta);
                    actualizarCapacidadesVuelos(mejorRuta, cnt);
                    Aeropuerto destino = obtenerAeropuertoPorCiudad(p.getCiudadDestino());
                    if (destino != null) incrementarOcupacionAlmacen(destino, cnt);
                    asignados++;
                }
            }
        }

        System.out.println("Restart greedy: " + asignados + "/" + paquetes.size() + " paquetes asignados");
        return nuevaSolucion;
    }

    private HashMap<Paquete, ArrayList<Vuelo>> restartHibrido(HashMap<Paquete, ArrayList<Vuelo>> solucionActual) {
        HashMap<Paquete, ArrayList<Vuelo>> nuevaSolucion = new HashMap<>();

        ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> entradas = new ArrayList<>(solucionActual.entrySet());
        entradas.sort((e1, e2) -> {
            try {
                int s1 = calcularCalidadRuta(e1.getKey(), e1.getValue());
                int s2 = calcularCalidadRuta(e2.getKey(), e2.getValue());
                int cmp = Integer.compare(s2, s1);
                if (cmp != 0) return cmp;
                int cmpVuelos = Integer.compare(e1.getValue().size(), e2.getValue().size());
                if (cmpVuelos != 0) return cmpVuelos;
                int cmpPrior = Double.compare(e2.getKey().getPrioridad(), e1.getKey().getPrioridad());
                if (cmpPrior != 0) return cmpPrior;
                return Integer.compare(e1.getKey().hashCode(), e2.getKey().hashCode());
            } catch (Exception ex) {
                System.out.println("Warning: Error en comparación de calidad, usando fallback");
                return Integer.compare(e1.getValue().size(), e2.getValue().size());
            }
        });

        int mantener = (int)(entradas.size() * 0.3);
        for (int i = 0; i < mantener && i < entradas.size(); i++) {
            nuevaSolucion.put(entradas.get(i).getKey(), entradas.get(i).getValue());
        }

        System.out.println("Híbrido: Manteniendo " + mantener + " mejores paquetes, regenerando " + (solucionActual.size() - mantener));

        reconstruirCapacidadesDesdeSolucion(nuevaSolucion);
        reconstruirAlmacenesDesdeSolucion(nuevaSolucion);

        return nuevaSolucion;
    }

    private int calcularCalidadRuta(Paquete p, ArrayList<Vuelo> ruta) {
        if (ruta == null || ruta.isEmpty()) return 0;

        int score = 0;
        if (ruta.size() == 1) score += 1000;
        else if (ruta.size() == 2) score += 500;
        else score += 100;

        double total = 0;
        for (Vuelo v : ruta) total += v.getTiempoTransporte();
        if (ruta.size() > 1) total += (ruta.size() - 1) * 2.0;

        score += Math.max(0, 2000 - (int)(total * 10));

        int products = p.getProductos() != null ? p.getProductos().size() : 1;
        score += products * 10;
        score += (int)(p.getPrioridad() * 50);

        boolean mismoContinente = p.getUbicacionActual().getContinente() == p.getCiudadDestino().getContinente();
        if (mismoContinente) score += 200;
        else score += 100;

        return Math.max(1, score);
    }

    private int[] seleccionarOperadores() {
        try {
            if (Constantes.LOGGING_VERBOSO) {
                System.out.println("    Seleccionando operadores...");
            }

            double pesoTotal = 0.0;
            for (int i = 0; i < pesosOperadores.length; i++) {
                for (int j = 0; j < pesosOperadores[i].length; j++) {
                    pesoTotal += pesosOperadores[i][j];
                }
            }

            if (Constantes.LOGGING_VERBOSO) {
                System.out.println("    Peso total: " + pesoTotal);
            }
            double valorAleatorio = aleatorio.nextDouble() * pesoTotal;
            double pesoAcumulado = 0.0;

            for (int i = 0; i < pesosOperadores.length; i++) {
                for (int j = 0; j < pesosOperadores[i].length; j++) {
                    pesoAcumulado += pesosOperadores[i][j];
                    if (valorAleatorio <= pesoAcumulado) {
                        if (Constantes.LOGGING_VERBOSO) {
                            System.out.println("    Operadores seleccionados: " + i + ", " + j);
                        }
                        return new int[]{i, j};
                    }
                }
            }

            if (Constantes.LOGGING_VERBOSO) {
                System.out.println("    Usando fallback: 0, 0");
            }
            return new int[]{0, 0};
        } catch (Exception e) {
            System.out.println("    Error en selección de operadores: " + e.getMessage());
            e.printStackTrace();
            return new int[]{0, 0};
        }
    }

    private ALNSDestruction.ResultadoDestruccion aplicarOperadorDestruccion(
            HashMap<Paquete, ArrayList<Vuelo>> solucion, int indiceOperador) {
        try {
            double ratioAjustado = Constantes.RATIO_DESTRUCCION * factorDiversificacion;
            int minAjustado = (int)(Constantes.DESTRUCCION_MIN_PAQUETES * factorDiversificacion);
            int maxAjustado = (int)(Constantes.DESTRUCCION_MAX_PAQUETES * factorDiversificacion);

            switch (indiceOperador) {
                case 0:
                    if (Constantes.LOGGING_VERBOSO) {
                        System.out.println("    Ejecutando destruccionAleatoria... (ratio: " + String.format("%.2f", ratioAjustado) + ")");
                    }
                    return operadoresDestruccion.destruccionAleatoria(solucion, ratioAjustado, minAjustado, maxAjustado);
                case 1:
                    if (Constantes.LOGGING_VERBOSO) {
                        System.out.println("    Ejecutando destruccionGeografica... (ratio: " + String.format("%.2f", ratioAjustado) + ")");
                    }
                    return operadoresDestruccion.destruccionGeografica(solucion, ratioAjustado, minAjustado, maxAjustado);
                case 2:
                    if (Constantes.LOGGING_VERBOSO) {
                        System.out.println("    Ejecutando destruccionBasadaEnTiempo... (ratio: " + String.format("%.2f", ratioAjustado) + ")");
                    }
                    return operadoresDestruccion.destruccionBasadaEnTiempo(solucion, ratioAjustado, minAjustado, maxAjustado);
                case 3:
                    if (Constantes.LOGGING_VERBOSO) {
                        System.out.println("    Ejecutando destruccionRutaCongestionada... (ratio: " + String.format("%.2f", ratioAjustado) + ")");
                    }
                    return operadoresDestruccion.destruccionRutaCongestionada(solucion, ratioAjustado, minAjustado, maxAjustado);
                default:
                    if (Constantes.LOGGING_VERBOSO) {
                        System.out.println("    Ejecutando destruccionAleatoria (default)... (ratio: " + String.format("%.2f", ratioAjustado) + ")");
                    }
                    return operadoresDestruccion.destruccionAleatoria(solucion, ratioAjustado, minAjustado, maxAjustado);
            }
        } catch (Exception e) {
            System.out.println("    Error en operador de destrucción: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private ALNSRepair.ResultadoReparacion aplicarOperadorReparacion(
            HashMap<Paquete, ArrayList<Vuelo>> solucion, int indiceOperador,
            ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> paquetesDestruidos) {

        switch (indiceOperador) {
            case 0:
                return operadoresReparacion.reparacionCodiciosa(solucion, paquetesDestruidos);
            case 1:
                return operadoresReparacion.reparacionArrepentimiento(solucion, paquetesDestruidos, 2);
            case 2:
                return operadoresReparacion.reparacionPorTiempo(solucion, paquetesDestruidos);
            case 3:
                return operadoresReparacion.reparacionPorCapacidad(solucion, paquetesDestruidos);
            default:
                return operadoresReparacion.reparacionCodiciosa(solucion, paquetesDestruidos);
        }
    }

    private void actualizarPesosOperadores() {
        double lambda = 0.1;

        for (int i = 0; i < puntajesOperadores.length; i++) {
            for (int j = 0; j < puntajesOperadores[i].length; j++) {
                if (usoOperadores[i][j] > 0) {
                    double puntajePromedio = puntajesOperadores[i][j] / usoOperadores[i][j];
                    pesosOperadores[i][j] = (1 - lambda) * pesosOperadores[i][j] +
                                           lambda * puntajePromedio;

                    puntajesOperadores[i][j] = 0.0;
                    usoOperadores[i][j] = 0;
                }
            }
        }
    }

    private Map<Vuelo, Integer> crearSnapshotCapacidades() {
        Map<Vuelo, Integer> snapshot = new HashMap<>();
        for (Vuelo f : vuelos) {
            snapshot.put(f, f.getCapacidadUsada());
        }
        return snapshot;
    }

    private void restaurarCapacidades(Map<Vuelo, Integer> snapshot) {
        for (Vuelo f : vuelos) {
            f.setCapacidadUsada(snapshot.getOrDefault(f, 0));
        }
    }

    private void reconstruirCapacidadesDesdeSolucion(HashMap<Paquete, ArrayList<Vuelo>> solucion) {
        for (Vuelo f : vuelos) {
            f.setCapacidadUsada(0);
        }

        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : solucion.entrySet()) {
            Paquete paquete = entrada.getKey();
            ArrayList<Vuelo> ruta = entrada.getValue();
            int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;

            for (Vuelo f : ruta) {
                f.setCapacidadUsada(f.getCapacidadUsada() + conteoProductos);
            }
        }
    }

    private Map<Aeropuerto, Integer> crearSnapshotAlmacenes() {
        Map<Aeropuerto, Integer> snapshot = new HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            snapshot.put(aeropuerto, ocupacionAlmacenes.getOrDefault(aeropuerto, 0));
        }
        return snapshot;
    }

    private void restaurarAlmacenes(Map<Aeropuerto, Integer> snapshot) {
        ocupacionAlmacenes.clear();
        ocupacionAlmacenes.putAll(snapshot);
    }

    private void reconstruirAlmacenesDesdeSolucion(HashMap<Paquete, ArrayList<Vuelo>> solucion) {
        inicializarOcupacionAlmacenes();

        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : solucion.entrySet()) {
            Paquete paquete = entrada.getKey();
            ArrayList<Vuelo> ruta = entrada.getValue();
            int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;

            if (ruta == null || ruta.isEmpty()) {
                Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
                if (aeropuertoDestino != null) {
                    incrementarOcupacionAlmacen(aeropuertoDestino, conteoProductos);
                }
            } else {
                Vuelo ultimoVuelo = ruta.get(ruta.size() - 1);
                incrementarOcupacionAlmacen(ultimoVuelo.getAeropuertoDestino(), conteoProductos);
            }
        }
    }

    private boolean cabeEnCapacidad(ArrayList<Vuelo> ruta, int cantidad) {
        if (ruta == null || ruta.isEmpty()) return true;

        for (Vuelo vuelo : ruta) {
            if (vuelo.getCapacidadUsada() + cantidad > vuelo.getCapacidadMaxima()) {
                return false;
            }
        }
        return true;
    }

    private void inicializarCacheCiudadAeropuerto() {
        cacheNombreCiudadAeropuerto = new HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (aeropuerto.getCiudad() != null && aeropuerto.getCiudad().getNombre() != null) {
                String claveCiudad = aeropuerto.getCiudad().getNombre().toLowerCase().trim();
                cacheNombreCiudadAeropuerto.put(claveCiudad, aeropuerto);
            }
        }
        System.out.println("Cache inicializada: " + cacheNombreCiudadAeropuerto.size() + " ciudades");
    }

    private void inicializarT0() {
        T0 = LocalDateTime.now();

        if (paquetes != null && !paquetes.isEmpty()) {
            LocalDateTime minFechaPedido = paquetes.stream()
                .filter(p -> p.getFechaPedido() != null)
                .map(Paquete::getFechaPedido)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            T0 = minFechaPedido;
        }

        System.out.println("T0 inicializado: " + T0);
    }

    private ArrayList<Paquete> expandirPaquetesAUnidadesProducto(ArrayList<Paquete> paquetesOriginales) {
        ArrayList<Paquete> unidadesProducto = new ArrayList<>();

        for (Paquete paqueteOriginal : paquetesOriginales) {
            int conteoProductos = (paqueteOriginal.getProductos() != null && !paqueteOriginal.getProductos().isEmpty())
                             ? paqueteOriginal.getProductos().size() : 1;

            for (int i = 0; i < conteoProductos; i++) {
                Paquete unidad = crearUnidadPaquete(paqueteOriginal, i);
                unidadesProducto.add(unidad);
            }
        }

        return unidadesProducto;
    }

    private Paquete crearUnidadPaquete(Paquete paqueteOriginal, int indiceUnidad) {
        Paquete unidad = new Paquete();

        String idUnidadString = paqueteOriginal.getId() + "#" + indiceUnidad;
        unidad.setId((long) idUnidadString.hashCode());

        unidad.setCliente(paqueteOriginal.getCliente());
        unidad.setCiudadDestino(paqueteOriginal.getCiudadDestino());
        unidad.setFechaPedido(paqueteOriginal.getFechaPedido());
        unidad.setFechaLimiteEntrega(paqueteOriginal.getFechaLimiteEntrega());
        unidad.setEstado(paqueteOriginal.getEstado());
        unidad.setUbicacionActual(paqueteOriginal.getUbicacionActual());
        unidad.setPrioridad(paqueteOriginal.getPrioridad());
        unidad.setRutaAsignada(paqueteOriginal.getRutaAsignada());

        ArrayList<Producto> productoUnico = new ArrayList<>();
        if (paqueteOriginal.getProductos() != null && indiceUnidad < paqueteOriginal.getProductos().size()) {
            Producto productoOriginal = paqueteOriginal.getProductos().get(indiceUnidad);
            Producto copiaProducto = new Producto();
            copiaProducto.setId(productoOriginal.getId());
            copiaProducto.setVueloAsignado(productoOriginal.getVueloAsignado());
            copiaProducto.setEstado(productoOriginal.getEstado());
            productoUnico.add(copiaProducto);
        } else {
            Producto productoGenerico = new Producto();
            String idProductoString = paqueteOriginal.getId() + "_P" + indiceUnidad;
            productoGenerico.setId((long) idProductoString.hashCode());
            productoUnico.add(productoGenerico);
        }

        unidad.setProductos(productoUnico);

        return unidad;
    }

    private Aeropuerto obtenerAeropuertoPorCiudad(Ciudad ciudad) {
        if (ciudad == null || ciudad.getNombre() == null) return null;
        String claveCiudad = ciudad.getNombre().toLowerCase().trim();
        return cacheNombreCiudadAeropuerto.get(claveCiudad);
    }

    private ArrayList<Vuelo> encontrarRutaDirecta(Ciudad origen, Ciudad destino) {
        if (origen == null || destino == null) return null;

        Aeropuerto aeropuertoOrigen = obtenerAeropuertoPorCiudad(origen);
        Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(destino);

        if (aeropuertoOrigen == null || aeropuertoDestino == null) return null;

        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen().equals(aeropuertoOrigen) &&
                vuelo.getAeropuertoDestino().equals(aeropuertoDestino)) {
                ArrayList<Vuelo> ruta = new ArrayList<>();
                ruta.add(vuelo);
                return ruta;
            }
        }

        return null;
    }

    private boolean esRutaValida(Paquete paquete, ArrayList<Vuelo> ruta) {
        if (paquete == null || ruta == null || ruta.isEmpty()) return false;

        int qty = paquete.getProductos() != null && !paquete.getProductos().isEmpty() ? paquete.getProductos().size() : 1;

        if (!cabeEnCapacidad(ruta, qty)) return false;

        Aeropuerto expectedOrigin = obtenerAeropuertoPorCiudad(paquete.getUbicacionActual());
        if (expectedOrigin == null || !ruta.get(0).getAeropuertoOrigen().equals(expectedOrigin)) return false;

        for (int i = 0; i < ruta.size() - 1; i++) {
            if (!ruta.get(i).getAeropuertoDestino().equals(ruta.get(i + 1).getAeropuertoOrigen())) return false;
        }

        Aeropuerto expectedDestination = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
        if (expectedDestination == null || !ruta.get(ruta.size() - 1).getAeropuertoDestino().equals(expectedDestination)) return false;

        return seRespetaDeadline(paquete, ruta);
    }

    private boolean puedeAsignarConOptimizacionEspacio(Paquete paquete, ArrayList<Vuelo> ruta,
                                                       HashMap<Paquete, ArrayList<Vuelo>> solucionActual) {
        Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
        if (aeropuertoDestino == null) return false;

        int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
        int ocupacionActual = ocupacionAlmacenes.getOrDefault(aeropuertoDestino, 0);
        int capacidadMaxima = aeropuertoDestino.getCapacidadMaxima();

        return (ocupacionActual + conteoProductos) <= capacidadMaxima;
    }

    private void actualizarCapacidadesVuelos(ArrayList<Vuelo> ruta, int conteoProductos) {
        for (Vuelo vuelo : ruta) {
            vuelo.setCapacidadUsada(vuelo.getCapacidadUsada() + conteoProductos);
        }
    }

    private void incrementarOcupacionAlmacen(Aeropuerto aeropuerto, int conteoProductos) {
        int ocupacionActual = ocupacionAlmacenes.getOrDefault(aeropuerto, 0);
        ocupacionAlmacenes.put(aeropuerto, ocupacionActual + conteoProductos);
    }

    private int obtenerTiempoInicioPaquete(Paquete paquete) {
        if (paquete == null || paquete.getFechaPedido() == null || T0 == null) {
            return 0;
        }
        long minutosDesdeT0 = ChronoUnit.MINUTES.between(T0, paquete.getFechaPedido());
        int offset = Math.floorMod(paquete.getId(), 60);
        int minutoInicio = (int) (minutosDesdeT0 + offset);
        final int TOTAL_MINUTOS = HORIZON_DAYS * 24 * 60;
        return Math.max(0, Math.min(minutoInicio, TOTAL_MINUTOS - 1));
    }

    private double calcularMargenTiempoRuta(Paquete paquete, ArrayList<Vuelo> ruta) {
        if (paquete == null || ruta == null) return 1.0;
        if (paquete.getFechaPedido() == null || paquete.getFechaLimiteEntrega() == null) return 1.0;

        double tiempoTotal = 0.0;
        for (Vuelo vuelo : ruta) tiempoTotal += vuelo.getTiempoTransporte();
        if (ruta.size() > 1) tiempoTotal += (ruta.size() - 1) * 2.0;

        long horasDisponibles = ChronoUnit.HOURS.between(paquete.getFechaPedido(), paquete.getFechaLimiteEntrega());
        double margen = horasDisponibles - tiempoTotal;
        return Math.max(margen, 0.0) + 1.0;
    }

    public void generarSolucionInicial() {
        if (Constantes.USAR_SOLUCION_INICIAL_CODICIOSA) {
            generarSolucionInicialGreedy();
        } else {
            generarSolucionInicialAleatoria();
        }
    }

    private void generarSolucionInicialGreedy() {
        System.out.println("=== GENERANDO SOLUCIÓN INICIAL GREEDY ===");
        
        // Crear estructura de solución temporal
        HashMap<Paquete, ArrayList<Vuelo>> solActual = new HashMap<>();
        
        // Ordenar paquetes con un componente aleatorio
        ArrayList<Paquete> paquetesOrdenados = new ArrayList<>(paquetes);
        
        // Decidir aleatoriamente entre diferentes estrategias de ordenamiento
        int estrategiaOrdenamiento = 0; // Puedes cambiar o parametrizar esta elección
        
        switch (estrategiaOrdenamiento) {
            case 0:
                // Ordenamiento por deadline (original)
                System.out.println("Estrategia de ordenamiento: Por deadline optimizado");
                paquetesOrdenados.sort((p1, p2) -> p1.getFechaLimiteEntrega().compareTo(p2.getFechaLimiteEntrega()));
                break;
            case 1:
                // Ordenamiento por prioridad
                System.out.println("Estrategia de ordenamiento: Por prioridad");
                paquetesOrdenados.sort((p1, p2) -> Double.compare(p2.getPrioridad(), p1.getPrioridad()));
                break;
            case 2:
                // Ordenamiento por distancia entre continentes
                System.out.println("Estrategia de ordenamiento: Por distancia entre continentes");
                paquetesOrdenados.sort((p1, p2) -> {
                    boolean p1DiffCont = p1.getUbicacionActual().getContinente() != p1.getCiudadDestino().getContinente();
                    boolean p2DiffCont = p2.getUbicacionActual().getContinente() != p2.getCiudadDestino().getContinente();
                    return Boolean.compare(p1DiffCont, p2DiffCont);
                });
                break;
            case 3:
                // Ordenamiento por margen de tiempo (más urgentes primero)
                System.out.println("Estrategia de ordenamiento: Por margen de tiempo");
                paquetesOrdenados.sort((p1, p2) -> {
                    LocalDateTime ahora = LocalDateTime.now();
                    long margen1 = ChronoUnit.HOURS.between(ahora, p1.getFechaLimiteEntrega());
                    long margen2 = ChronoUnit.HOURS.between(ahora, p2.getFechaLimiteEntrega());
                    return Long.compare(margen1, margen2);
                });
                break;
            case 4:
                // Ordenamiento aleatorio
                System.out.println("Estrategia de ordenamiento: Aleatorio");
                Collections.shuffle(paquetesOrdenados, aleatorio);
                break;
            default:
                // Fallback: aleatorio
                Collections.shuffle(paquetesOrdenados, aleatorio);
                break;
        }
        
        // Usar algoritmo optimizado con ventanas de tiempo y reasignación dinámica
        int paquetesAsignados = generarSolucionOptima(solActual, paquetesOrdenados);
        
        // Calcular el peso/costo de esta solución
        int pesoSolucion = calcularPesoSolucion(solActual);
        
        // Almacenar la solución con su peso
        solucion.put(solActual, pesoSolucion);
        
        System.out.println("Solución inicial generada: " + paquetesAsignados + "/" + paquetes.size() + " paquetes asignados");
        System.out.println("Peso de la solución: " + pesoSolucion);
    }
    

    private void generarSolucionInicialAleatoria() {
        System.out.println("=== GENERANDO SOLUCIÓN INICIAL ALEATORIA ===");
        System.out.println("Probabilidad de asignación: " + (Constantes.PROBABILIDAD_ASIGNACION_ALEATORIA * 100) + "%");

        HashMap<Paquete, ArrayList<Vuelo>> solucionActual = new HashMap<>();
        int paquetesAsignados = 0;

        ArrayList<Paquete> paquetesBarajados = new ArrayList<>(paquetes);
        Collections.shuffle(paquetesBarajados, aleatorio);

        for (Paquete paquete : paquetesBarajados) {
            if (aleatorio.nextDouble() < Constantes.PROBABILIDAD_ASIGNACION_ALEATORIA) {
                ArrayList<Vuelo> rutaAleatoria = generarRutaAleatoria(paquete);

                if (rutaAleatoria != null && !rutaAleatoria.isEmpty()) {
                    int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;

                    if (cabeEnCapacidad(rutaAleatoria, conteoProductos)) {
                        Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
                        if (aeropuertoDestino != null &&
                            puedeAsignarConOptimizacionEspacio(paquete, rutaAleatoria, solucionActual)) {

                            solucionActual.put(paquete, rutaAleatoria);
                            actualizarCapacidadesVuelos(rutaAleatoria, conteoProductos);
                            incrementarOcupacionAlmacen(aeropuertoDestino, conteoProductos);
                            paquetesAsignados++;
                        }
                    }
                }
            }
        }

        int pesoSolucion = calcularPesoSolucion(solucionActual);
        solucion.put(solucionActual, pesoSolucion);

        System.out.println("Solución inicial aleatoria generada: " + paquetesAsignados + "/" + paquetes.size() + " paquetes asignados");
        System.out.println("Peso de la solución: " + pesoSolucion);
    }

    private int generarSolucionOptima(HashMap<Paquete, ArrayList<Vuelo>> solucionActual,
                                  ArrayList<Paquete> paquetesOrdenados) {
    int paquetesAsignados = 0;
    int maxIteraciones = 3; // Máximo número de iteraciones para reasignación

    System.out.println("Iniciando algoritmo optimizado con " + maxIteraciones + " iteraciones...");

    for (int iteracion = 0; iteracion < maxIteraciones; iteracion++) {
        if (iteracion > 0) {
            System.out.println("Iteración " + iteracion + " - Reasignación dinámica...");
            // En iteraciones posteriores, intentar reasignar paquetes no asignados
            ArrayList<Paquete> paquetesNoAsignados = new ArrayList<>();
            for (Paquete pkg : paquetesOrdenados) {
                if (!solucionActual.containsKey(pkg)) {
                    paquetesNoAsignados.add(pkg);
                }
            }
            paquetesOrdenados = paquetesNoAsignados;
        }

        int asignadosEnIteracion = 0;

        for (Paquete pkg : paquetesOrdenados) {
            Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(pkg.getCiudadDestino());
            if (aeropuertoDestino == null) continue;

            int cantidadProductos = pkg.getProductos() != null ? pkg.getProductos().size() : 1;

            // Intentar asignar el paquete usando diferentes estrategias
            ArrayList<Vuelo> mejorRuta = encontrarMejorRutaConVentanasDeTiempo(pkg, solucionActual);

            if (mejorRuta != null && esRutaValida(pkg, mejorRuta)) {
                // Primero validar temporalmente sin actualizar capacidades
                if (puedeAsignarConOptimizacionEspacio(pkg, mejorRuta, solucionActual)) {
                    // Si la validación temporal pasa, entonces actualizar capacidades
                    solucionActual.put(pkg, mejorRuta);
                    paquetesAsignados++;
                    asignadosEnIteracion++;

                    // Actualizar capacidades DESPUÉS de la validación
                    actualizarCapacidadesVuelos(mejorRuta, cantidadProductos);
                    incrementarOcupacionAlmacen(aeropuertoDestino, cantidadProductos);

                    if (iteracion > 0) {
                        System.out.println("  Reasignado paquete " + pkg.getId() + " en iteración " + iteracion);
                    }
                }
            }
        }

        System.out.println("  Iteración " + iteracion + " completada: " + asignadosEnIteracion + " paquetes asignados");

        // Si no se asignaron paquetes en esta iteración, no hay punto en continuar
        if (asignadosEnIteracion == 0) {
            break;
        }
    }

        return paquetesAsignados;
    }

    private ArrayList<Vuelo> encontrarMejorRutaConVentanasDeTiempo(Paquete paquete, HashMap<Paquete, ArrayList<Vuelo>> solucionActual) {
         // Primero intentar con el método original (ruta estándar)
        ArrayList<Vuelo> rutaOriginal = encontrarMejorRuta(paquete);

        // Si no existe ruta original o no se puede asignar con optimización de espacio,
        // intentar con diferentes horarios de salida (salida retrasada)
        if (rutaOriginal == null || !puedeAsignarConOptimizacionDeEspacio(paquete, rutaOriginal, solucionActual)) {
            return encontrarRutaConSalidaRetrasada(paquete, solucionActual);
        }

        // Si la ruta original es válida y cabe, devolverla
        return rutaOriginal;
    }

    private boolean puedeAsignarConOptimizacionDeEspacio(Paquete paquete,
                                                     ArrayList<Vuelo> ruta,
                                                     HashMap<Paquete, ArrayList<Vuelo>> solucionActual) {
    // Validación simplificada de la capacidad del almacén final
    Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
    if (aeropuertoDestino == null) return false;
    
    int cantidadProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
    int ocupacionActual = ocupacionAlmacenes.getOrDefault(aeropuertoDestino, 0);
    int capacidadMaxima = aeropuertoDestino.getCapacidadMaxima();
    
    return (ocupacionActual + cantidadProductos) <= capacidadMaxima;
}


    private ArrayList<Vuelo> generarRutaAleatoria(Paquete paquete) {
        Ciudad origen = paquete.getUbicacionActual();
        Ciudad destino = paquete.getCiudadDestino();

        if (origen == null || destino == null) return null;

        Aeropuerto aeropuertoOrigen = obtenerAeropuertoPorCiudad(origen);
        Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(destino);

        if (aeropuertoOrigen == null || aeropuertoDestino == null) return null;

        ArrayList<Vuelo> rutaDirecta = encontrarRutaDirecta(origen, destino);
        if (rutaDirecta != null && !rutaDirecta.isEmpty()) {
            return rutaDirecta;
        }

        ArrayList<Aeropuerto> aeropuertosBarajados = new ArrayList<>(aeropuertos);
        Collections.shuffle(aeropuertosBarajados, aleatorio);

        for (int i = 0; i < Math.min(5, aeropuertosBarajados.size()); i++) {
            Aeropuerto intermedio = aeropuertosBarajados.get(i);
            if (intermedio.equals(aeropuertoOrigen) || intermedio.equals(aeropuertoDestino)) continue;

            ArrayList<Vuelo> tramo1 = encontrarRutaDirecta(origen, intermedio.getCiudad());
            ArrayList<Vuelo> tramo2 = encontrarRutaDirecta(intermedio.getCiudad(), destino);

            if (tramo1 != null && tramo2 != null && !tramo1.isEmpty() && !tramo2.isEmpty()) {
                ArrayList<Vuelo> ruta = new ArrayList<>();
                ruta.addAll(tramo1);
                ruta.addAll(tramo2);
                return ruta;
            }
        }

        return null;
    }

    private ArrayList<Vuelo> encontrarMejorRutaConVentanasTiempo(Paquete paquete, HashMap<Paquete, ArrayList<Vuelo>> solucionActual) {
        ArrayList<Vuelo> rutaOriginal = encontrarMejorRuta(paquete);
        if (rutaOriginal == null || !puedeAsignarConOptimizacionEspacio(paquete, rutaOriginal, solucionActual)) {
            return encontrarRutaConSalidaRetrasada(paquete, solucionActual);
        }
        return rutaOriginal;
    }

    private ArrayList<Vuelo> encontrarRutaConSalidaRetrasada(Paquete paquete,
                                                             HashMap<Paquete, ArrayList<Vuelo>> solucionActual) {
        for (int delayHours = 2; delayHours <= 12; delayHours += 2) {
            Paquete paqueteRetrasado = crearPaqueteRetrasado(paquete, delayHours);
            if (paqueteRetrasado == null) continue;

            ArrayList<Vuelo> ruta = encontrarMejorRuta(paqueteRetrasado);
            if (ruta != null && puedeAsignarConOptimizacionEspacio(paqueteRetrasado, ruta, solucionActual)) {
                return ruta;
            }
        }
        return null;
    }

    private Paquete crearPaqueteRetrasado(Paquete original, int horasRetraso) {
        LocalDateTime fechaPedidoRetrasada = original.getFechaPedido().plusHours(horasRetraso);
        if (fechaPedidoRetrasada.isAfter(original.getFechaLimiteEntrega())) {
            return null;
        }

        Paquete retrasado = new Paquete();
        retrasado.setId(original.getId());
        retrasado.setCliente(original.getCliente());
        retrasado.setCiudadDestino(original.getCiudadDestino());
        retrasado.setFechaPedido(fechaPedidoRetrasada);
        retrasado.setFechaLimiteEntrega(original.getFechaLimiteEntrega());
        retrasado.setUbicacionActual(original.getUbicacionActual());
        retrasado.setProductos(original.getProductos());
        retrasado.setPrioridad(original.getPrioridad());
        return retrasado;
    }

    private ArrayList<Vuelo> encontrarMejorRuta(Paquete paquete) {
        Ciudad origen = paquete.getUbicacionActual();
        Ciudad destino = paquete.getCiudadDestino();

        if (origen.equals(destino)) {
            return new ArrayList<>();
        }

        ArrayList<ArrayList<Vuelo>> rutasValidas = new ArrayList<>();
        ArrayList<Double> puntajesRuta = new ArrayList<>();

        ArrayList<Vuelo> directa = encontrarRutaDirecta(origen, destino);
        if (directa != null && esRutaValida(paquete, directa)) {
            rutasValidas.add(directa);
            puntajesRuta.add(calcularMargenTiempoRuta(paquete, directa));
        }

        ArrayList<Vuelo> unaEscala = encontrarRutaUnaEscala(origen, destino);
        if (unaEscala != null && esRutaValida(paquete, unaEscala)) {
            rutasValidas.add(unaEscala);
            puntajesRuta.add(calcularMargenTiempoRuta(paquete, unaEscala));
        }

        ArrayList<Vuelo> dosEscalas = encontrarRutaDosEscalas(origen, destino);
        if (dosEscalas != null && esRutaValida(paquete, dosEscalas)) {
            rutasValidas.add(dosEscalas);
            puntajesRuta.add(calcularMargenTiempoRuta(paquete, dosEscalas));
        }

        if (rutasValidas.isEmpty()) return null;

        int total = rutasValidas.size();
        int indiceSeleccionado;
        if (total > 1) {
            double suma = 0;
            for (double p : puntajesRuta) suma += p;
            if (suma > 0) {
                double rand = aleatorio.nextDouble() * suma;
                double acum = 0;
                indiceSeleccionado = 0;
                for (int i = 0; i < puntajesRuta.size(); i++) {
                    acum += puntajesRuta.get(i);
                    if (rand <= acum) {
                        indiceSeleccionado = i;
                        break;
                    }
                }
            } else {
                indiceSeleccionado = aleatorio.nextInt(total);
            }
        } else {
            indiceSeleccionado = 0;
        }

        return rutasValidas.get(indiceSeleccionado);
    }

    private ArrayList<Vuelo> encontrarRutaUnaEscala(Ciudad origen, Ciudad destino) {
        Aeropuerto aOrigen = obtenerAeropuertoPorCiudad(origen);
        Aeropuerto aDestino = obtenerAeropuertoPorCiudad(destino);
        if (aOrigen == null || aDestino == null) return null;

        ArrayList<Aeropuerto> posibles = new ArrayList<>();
        for (Aeropuerto a : aeropuertos) {
            if (!a.equals(aOrigen) && !a.equals(aDestino)) posibles.add(a);
        }
        Collections.shuffle(posibles, aleatorio);

        for (Aeropuerto escala : posibles) {
            Vuelo primero = null;
            for (Vuelo v : vuelos) {
                if (v.getAeropuertoOrigen().equals(aOrigen) &&
                    v.getAeropuertoDestino().equals(escala) &&
                    v.getCapacidadUsada() < v.getCapacidadMaxima()) {
                    primero = v; break;
                }
            }
            if (primero == null) continue;
            Vuelo segundo = null;
            for (Vuelo v : vuelos) {
                if (v.getAeropuertoOrigen().equals(escala) &&
                    v.getAeropuertoDestino().equals(aDestino) &&
                    v.getCapacidadUsada() < v.getCapacidadMaxima()) {
                    segundo = v; break;
                }
            }
            if (segundo != null) {
                ArrayList<Vuelo> ruta = new ArrayList<>();
                ruta.add(primero); ruta.add(segundo);
                return ruta;
            }
        }
        return null;
    }

    private ArrayList<Vuelo> encontrarRutaDosEscalas(Ciudad origen, Ciudad destino) {
        Aeropuerto aOrigen = obtenerAeropuertoPorCiudad(origen);
        Aeropuerto aDestino = obtenerAeropuertoPorCiudad(destino);
        if (aOrigen == null || aDestino == null) return null;

        ArrayList<Aeropuerto> primeras = new ArrayList<>();
        for (Aeropuerto a : aeropuertos) {
            if (!a.equals(aOrigen) && !a.equals(aDestino)) primeras.add(a);
        }
        Collections.shuffle(primeras, aleatorio);
        int maxPrimeras = Math.min(10, primeras.size());

        for (int i = 0; i < maxPrimeras; i++) {
            Aeropuerto p1 = primeras.get(i);
            ArrayList<Aeropuerto> segundas = new ArrayList<>();
            for (Aeropuerto a : aeropuertos) {
                if (!a.equals(aOrigen) && !a.equals(aDestino) && !a.equals(p1)) segundas.add(a);
            }
            Collections.shuffle(segundas, aleatorio);
            int maxSeg = Math.min(10, segundas.size());
            for (int j = 0; j < maxSeg; j++) {
                Aeropuerto p2 = segundas.get(j);
                Vuelo f1 = null;
                for (Vuelo v : vuelos) {
                    if (v.getAeropuertoOrigen().equals(aOrigen) && v.getAeropuertoDestino().equals(p1) && v.getCapacidadUsada() < v.getCapacidadMaxima()) { f1 = v; break; }
                }
                if (f1 == null) continue;
                Vuelo f2 = null;
                for (Vuelo v : vuelos) {
                    if (v.getAeropuertoOrigen().equals(p1) && v.getAeropuertoDestino().equals(p2) && v.getCapacidadUsada() < v.getCapacidadMaxima()) { f2 = v; break; }
                }
                if (f2 == null) continue;
                Vuelo f3 = null;
                for (Vuelo v : vuelos) {
                    if (v.getAeropuertoOrigen().equals(p2) && v.getAeropuertoDestino().equals(aDestino) && v.getCapacidadUsada() < v.getCapacidadMaxima()) { f3 = v; break; }
                }
                if (f3 != null) {
                    ArrayList<Vuelo> ruta = new ArrayList<>();
                    ruta.add(f1); ruta.add(f2); ruta.add(f3);
                    double tiempoTotal = f1.getTiempoTransporte() + f2.getTiempoTransporte() + f3.getTiempoTransporte();
                    tiempoTotal += 2.0;
                    if (tiempoTotal > Constantes.TIEMPO_MAX_ENTREGA_DIFERENTE_CONTINENTE * 24) continue;
                    return ruta;
                }
            }
        }
        return null;
    }

    private boolean seRespetaDeadline(Paquete paquete, ArrayList<Vuelo> ruta) {
        double tiempoTotal = 0;
        for (Vuelo v : ruta) tiempoTotal += v.getTiempoTransporte();
        if (ruta.size() > 1) tiempoTotal += (ruta.size() - 1) * 2.0;

        if (!validarPromesaEntregaMoraPack(paquete, tiempoTotal)) return false;

        double margenSeguridad = 0.0;
        if (aleatorio != null) {
            Ciudad origen = paquete.getUbicacionActual();
            Ciudad destino = paquete.getCiudadDestino();
            boolean misma = (origen != null && destino != null) && origen.getContinente() == destino.getContinente();
            int factor = ruta.size() + (misma ? 0 : 2);
            margenSeguridad = 0.01 * (1 + aleatorio.nextInt(factor * 3));
            tiempoTotal = tiempoTotal * (1.0 + margenSeguridad);
        }

        long horasHastaDeadline = ChronoUnit.HOURS.between(paquete.getFechaPedido(), paquete.getFechaLimiteEntrega());
        return tiempoTotal <= horasHastaDeadline;
    }

    private boolean validarPromesaEntregaMoraPack(Paquete paquete, double tiempoTotalHoras) {
        Ciudad origen = paquete.getUbicacionActual();
        Ciudad destino = paquete.getCiudadDestino();

        if (origen == null || destino == null) {
            System.err.println("Error: origen o destino nulo para paquete " + paquete.getId());
            return false;
        }

        boolean mismoContinente = origen.getContinente() == destino.getContinente();
        long horasPromesa = mismoContinente ? 48 : 72;

        if (tiempoTotalHoras > horasPromesa) {
            if (DEBUG_MODE) {
                System.out.println("VIOLACIÓN PROMESA MORAPACK - Paquete " + paquete.getId() +
                    ": " + tiempoTotalHoras + "h > " + horasPromesa + "h");
            }
            return false;
        }

        long horasHastaDeadline = ChronoUnit.HOURS.between(paquete.getFechaPedido(), paquete.getFechaLimiteEntrega());
        if (tiempoTotalHoras > horasHastaDeadline) {
            if (DEBUG_MODE) {
                System.out.println("VIOLACIÓN DEADLINE CLIENTE - Paquete " + paquete.getId() +
                    ": " + tiempoTotalHoras + "h > " + horasHastaDeadline + "h disponibles");
            }
            return false;
        }

        if (!esSedeMoraPack(origen)) {
            if (DEBUG_MODE) {
                System.out.println("ADVERTENCIA - Paquete " + paquete.getId() + " no origina desde sede MoraPack: " + origen.getNombre());
            }
        }

        return true;
    }

    private boolean esSedeMoraPack(Ciudad ciudad) {
        if (ciudad == null || ciudad.getNombre() == null) return false;
        String nombre = ciudad.getNombre().toLowerCase();
        return nombre.contains("lima") || nombre.contains("bruselas") || nombre.contains("brussels") || nombre.contains("baku");
    }

    private int calcularPesoSolucion(HashMap<Paquete, ArrayList<Vuelo>> mapaSolucion) {
        int totalPaquetes = mapaSolucion.size();
        int totalProductos = 0;
        double tiempoTotalEntrega = 0;
        int entregasATiempo = 0;
        double utilizacionCapacidadTotal = 0;
        int totalVuelosUsados = 0;
        double margenEntregaTotal = 0;

        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : mapaSolucion.entrySet()) {
            Paquete paquete = entrada.getKey();
            ArrayList<Vuelo> ruta = entrada.getValue();

            int productosEnPaquete = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
            totalProductos += productosEnPaquete;

            double tiempoRuta = 0;
            for (Vuelo vuelo : ruta) {
                tiempoRuta += vuelo.getTiempoTransporte();
                utilizacionCapacidadTotal += (double) vuelo.getCapacidadUsada() / vuelo.getCapacidadMaxima();
                totalVuelosUsados++;
            }

            if (ruta.size() > 1) tiempoRuta += (ruta.size() - 1) * 2.0;

            tiempoTotalEntrega += tiempoRuta;

            if (seRespetaDeadline(paquete, ruta)) {
                entregasATiempo++;
                LocalDateTime entregaEstimada = paquete.getFechaPedido().plusHours((long)tiempoRuta);
                double horasMargen = ChronoUnit.HOURS.between(entregaEstimada, paquete.getFechaLimiteEntrega());
                margenEntregaTotal += horasMargen;
            }
        }

        double tiempoPromedioEntrega = totalPaquetes > 0 ? tiempoTotalEntrega / totalPaquetes : 0;
        double utilizacionCapacidadPromedio = totalVuelosUsados > 0 ? utilizacionCapacidadTotal / totalVuelosUsados : 0;
        double tasaATiempo = totalPaquetes > 0 ? (double) entregasATiempo / totalPaquetes : 0;
        double margenPromedioEntrega = entregasATiempo > 0 ? margenEntregaTotal / entregasATiempo : 0;

        double eficienciaContinental = calcularEficienciaContinental(mapaSolucion);
        double utilizacionAlmacenes = calcularUtilizacionAlmacenes();

        int peso = (int) (
            totalPaquetes * 100000 +
            totalProductos * 10000 +
            tasaATiempo * 5000 +
            Math.min(margenPromedioEntrega * 50, 1000) +
            eficienciaContinental * 500 +
            utilizacionCapacidadPromedio * 200 +
            utilizacionAlmacenes * 100 -
            tiempoPromedioEntrega * 20 -
            calcularComplejidadRuteo(mapaSolucion) * 50
        );

        if (tasaATiempo < 0.8) {
            peso = (int)(peso * 0.5);
        }

        if (tasaATiempo >= 0.95 && totalPaquetes > 10) {
            peso = (int)(peso * 1.1);
        }

        if (totalPaquetes > 1000) {
            peso = (int)(peso * 1.15);
        }

        return peso;
    }

    private double calcularEficienciaContinental(HashMap<Paquete, ArrayList<Vuelo>> mapaSolucion) {
        if (mapaSolucion.isEmpty()) return 0.0;

        int sameDirect = 0, sameOneStop = 0, diffDirect = 0, diffOneStop = 0, inefficient = 0;

        for (Map.Entry<Paquete, ArrayList<Vuelo>> e : mapaSolucion.entrySet()) {
            Paquete p = e.getKey();
            ArrayList<Vuelo> ruta = e.getValue();
            boolean mismo = p.getUbicacionActual().getContinente() == p.getCiudadDestino().getContinente();
            if (ruta.isEmpty()) continue;
            if (mismo) {
                if (ruta.size() == 1) sameDirect++;
                else if (ruta.size() == 2) sameOneStop++;
                else inefficient++;
            } else {
                if (ruta.size() == 1) diffDirect++;
                else if (ruta.size() <= 2) diffOneStop++;
                else inefficient++;
            }
        }

        double ef = sameDirect * 1.0 + sameOneStop * 0.8 + diffDirect * 1.2 + diffOneStop * 1.0 + inefficient * (-0.5);
        return ef;
    }

    private double calcularUtilizacionAlmacenes() {
        if (ocupacionAlmacenes.isEmpty()) return 0.0;
        double total = 0.0;
        int valid = 0;
        for (Map.Entry<Aeropuerto, Integer> e : ocupacionAlmacenes.entrySet()) {
            Aeropuerto a = e.getKey();
            int occ = e.getValue();
            if (a.getCapacidadMaxima() > 0) {
                total += (double) occ / a.getCapacidadMaxima();
                valid++;
            }
        }
        return valid > 0 ? total / valid : 0.0;
    }

    private double calcularComplejidadRuteo(HashMap<Paquete, ArrayList<Vuelo>> mapaSolucion) {
        if (mapaSolucion.isEmpty()) return 0.0;
        double total = 0.0;
        for (Map.Entry<Paquete, ArrayList<Vuelo>> e : mapaSolucion.entrySet()) {
            Paquete p = e.getKey();
            ArrayList<Vuelo> ruta = e.getValue();
            if (ruta.isEmpty()) continue;
            boolean mismo = p.getUbicacionActual().getContinente() == p.getCiudadDestino().getContinente();
            int esperado = mismo ? 1 : 2;
            if (ruta.size() > esperado) total += (ruta.size() - esperado) * 2.0;
            if (ruta.size() > 1) {
                for (Vuelo f : ruta) {
                    double util = (double) f.getCapacidadUsada() / f.getCapacidadMaxima();
                    if (util < 0.3) total += 1.0;
                }
            }
        }
        return total;
    }

    public boolean esSolucionValida() {
        if (solucion.isEmpty()) return false;
        HashMap<Paquete, ArrayList<Vuelo>> solucionActual = solucion.keySet().iterator().next();

        for (Map.Entry<Paquete, ArrayList<Vuelo>> e : solucionActual.entrySet()) {
            Paquete p = e.getKey();
            ArrayList<Vuelo> ruta = e.getValue();
            if (!esRutaValida(p, ruta)) return false;
        }

        if (!esSolucionTemporalValida(solucionActual)) {
            System.out.println("La solución viola las restricciones de capacidad temporal de almacenes");
            return false;
        }

        return true;
    }

    public boolean esSolucionCapacidadValida() {
        if (solucion.isEmpty()) return false;
        HashMap<Paquete, ArrayList<Vuelo>> solucionActual = solucion.keySet().iterator().next();
        Map<Vuelo, Integer> uso = new HashMap<>();
        for (Map.Entry<Paquete, ArrayList<Vuelo>> e : solucionActual.entrySet()) {
            Paquete p = e.getKey();
            ArrayList<Vuelo> ruta = e.getValue();
            int productos = p.getProductos() != null && !p.getProductos().isEmpty() ? p.getProductos().size() : 1;
            for (Vuelo f : ruta) uso.merge(f, productos, Integer::sum);
        }
        for (Map.Entry<Vuelo, Integer> e : uso.entrySet()) {
            if (e.getValue() > e.getKey().getCapacidadMaxima()) return false;
        }
        return true;
    }

    public void imprimirDescripcionSolucion(int nivelDetalle) {
        if (solucion.isEmpty()) {
            System.out.println("No hay solución disponible para mostrar.");
            return;
        }

        HashMap<Paquete, ArrayList<Vuelo>> solucionActual = solucion.keySet().iterator().next();
        int pesoSolucion = solucion.get(solucionActual);

        int totalProductosAsignados = 0;
        int totalProductosEnSistema = 0;
        for (Paquete paquete : paquetes) {
            int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
            totalProductosEnSistema += conteoProductos;
            if (solucionActual.containsKey(paquete)) totalProductosAsignados += conteoProductos;
        }

        System.out.println("\n========== DESCRIPCIÓN DE LA SOLUCIÓN ==========");
        System.out.println("Peso de la solución: " + pesoSolucion);
        System.out.println("Paquetes asignados: " + solucionActual.size() + "/" + paquetes.size());
        System.out.println("Productos transportados: " + totalProductosAsignados + "/" + totalProductosEnSistema);

        int rutasDirectas = 0, rutasUnaEscala = 0, rutasDosEscalas = 0, rutasMismoContinente = 0, rutasDiferentesContinentes = 0, entregasATiempo = 0;
        for (Map.Entry<Paquete, ArrayList<Vuelo>> e : solucionActual.entrySet()) {
            Paquete p = e.getKey();
            ArrayList<Vuelo> ruta = e.getValue();
            if (ruta.size() == 1) rutasDirectas++;
            else if (ruta.size() == 2) rutasUnaEscala++;
            else if (ruta.size() == 3) rutasDosEscalas++;
            if (p.getUbicacionActual().getContinente() == p.getCiudadDestino().getContinente()) rutasMismoContinente++;
            else rutasDiferentesContinentes++;
            if (seRespetaDeadline(p, ruta)) entregasATiempo++;
        }

        System.out.println("\n----- Estadísticas de Rutas -----");
        System.out.println("Rutas directas: " + rutasDirectas + " (" + formatearPorcentaje(rutasDirectas, solucionActual.size()) + "%)");
        System.out.println("Rutas con 1 escala: " + rutasUnaEscala + " (" + formatearPorcentaje(rutasUnaEscala, solucionActual.size()) + "%)");
        System.out.println("Rutas con 2 escalas: " + rutasDosEscalas + " (" + formatearPorcentaje(rutasDosEscalas, solucionActual.size()) + "%)");
        System.out.println("Rutas en mismo continente: " + rutasMismoContinente + " (" + formatearPorcentaje(rutasMismoContinente, solucionActual.size()) + "%)");
        System.out.println("Rutas entre continentes: " + rutasDiferentesContinentes + " (" + formatearPorcentaje(rutasDiferentesContinentes, solucionActual.size()) + "%)");
        System.out.println("Entregas a tiempo: " + entregasATiempo + " (" + formatearPorcentaje(entregasATiempo, solucionActual.size()) + "% de asignados)");
        System.out.println("Entregas a tiempo del total: " + entregasATiempo + "/" + paquetes.size() + " (" + formatearPorcentaje(entregasATiempo, paquetes.size()) + "%)");

        int paquetesNoAsignados = paquetes.size() - solucionActual.size();
        if (paquetesNoAsignados > 0) {
            System.out.println("Paquetes no asignados: " + paquetesNoAsignados + "/" + paquetes.size() + " (" + formatearPorcentaje(paquetesNoAsignados, paquetes.size()) + "%)");
            System.out.println("Razón principal: Capacidad de almacenes insuficiente");
        }

        System.out.println("\n----- Ocupación de Almacenes -----");
        int totalCapacidad = 0, totalOcupacion = 0, almacenesAlMax = 0;
        for (Map.Entry<Aeropuerto, Integer> e : ocupacionAlmacenes.entrySet()) {
            Aeropuerto a = e.getKey();
            int occ = e.getValue();
            if (a.getCapacidadMaxima() > 0) {
                int max = a.getCapacidadMaxima();
                totalCapacidad += max;
                totalOcupacion += occ;
                if (occ >= max) almacenesAlMax++;
                double porcentaje = (occ * 100.0) / max;
                if (porcentaje > 80.0) {
                    System.out.println("  " + a.getCiudad().getNombre() + ": " + occ + "/" + max + " (" + String.format("%.1f", porcentaje) + "%)");
                }
            }
        }

        double avgPorcentaje = totalCapacidad > 0 ? (totalOcupacion * 100.0) / totalCapacidad : 0.0;
        System.out.println("Ocupación promedio de almacenes: " + String.format("%.1f", avgPorcentaje) + "%");
        System.out.println("Almacenes llenos: " + almacenesAlMax + "/" + aeropuertos.size());

        if (ocupacionTemporalAlmacenes != null && !ocupacionTemporalAlmacenes.isEmpty()) {
            System.out.println("\n----- Picos de Ocupación Temporal -----");
            for (Aeropuerto aeropuerto : aeropuertos) {
                if (aeropuerto.getCapacidadMaxima() > 0) {
                    int[] pico = findPeakOccupancy(aeropuerto);
                    int minutoPico = pico[0];
                    int maxOcc = pico[1];
                    if (maxOcc > 0) {
                        int hora = minutoPico / 60;
                        int min = minutoPico % 60;
                        double pct = (maxOcc * 100.0) / aeropuerto.getCapacidadMaxima();
                        if (pct > 50.0) {
                            System.out.println("  " + aeropuerto.getCiudad().getNombre() +
                                              " - Pico: " + maxOcc + "/" + aeropuerto.getCapacidadMaxima() +
                                              " (" + String.format("%.1f", pct) + "%) a las " +
                                              String.format("%02d:%02d", hora, min));
                        }
                    }
                }
            }
        }

        if (nivelDetalle < 2) return;

        System.out.println("\n----- Rutas por Prioridad -----");
        List<Paquete> ordenados = new ArrayList<>(solucionActual.keySet());
        ordenados.sort((p1, p2) -> {
            int cmp = Double.compare(p2.getPrioridad(), p1.getPrioridad());
            if (cmp != 0) return cmp;
            return p1.getFechaLimiteEntrega().compareTo(p2.getFechaLimiteEntrega());
        });

        int mostrar = nivelDetalle == 2 ? Math.min(10, ordenados.size()) : ordenados.size();

        for (int i = 0; i < mostrar; i++) {
            Paquete p = ordenados.get(i);
            ArrayList<Vuelo> ruta = solucionActual.get(p);

            System.out.println("\nPaquete #" + p.getId() +
                              " (Prioridad: " + String.format("%.2f", p.getPrioridad()) +
                              ", Deadline: " + p.getFechaLimiteEntrega() + ")");

            System.out.println("  Origen: " + p.getUbicacionActual().getNombre() +
                              " (" + p.getUbicacionActual().getContinente() + ")");
            System.out.println("  Destino: " + p.getCiudadDestino().getNombre() +
                              " (" + p.getCiudadDestino().getContinente() + ")");

            if (ruta.isEmpty()) {
                System.out.println("  Ruta: Ya está en el destino");
                continue;
            }

            System.out.println("  Ruta (" + ruta.size() + " vuelos):");
            double tiempoTotal = 0;
            for (int j = 0; j < ruta.size(); j++) {
                Vuelo v = ruta.get(j);
                tiempoTotal += v.getTiempoTransporte();
                System.out.println("    " + (j+1) + ". " +
                                  v.getAeropuertoOrigen().getCiudad().getNombre() + " → " +
                                  v.getAeropuertoDestino().getCiudad().getNombre() +
                                  " (" + String.format("%.1f", v.getTiempoTransporte()) + "h, " +
                                  v.getCapacidadUsada() + "/" + v.getCapacidadMaxima() + " paquetes)");
            }

            if (ruta.size() > 1) tiempoTotal += (ruta.size() - 1) * 2.0;

            System.out.println("  Tiempo total estimado: " + String.format("%.1f", tiempoTotal) + "h");

            boolean at = seRespetaDeadline(p, ruta);
            System.out.println("  Entrega a tiempo: " + (at ? "SÍ" : "NO"));
        }

        if (mostrar < ordenados.size()) {
            System.out.println("\n... y " + (ordenados.size() - mostrar) + " paquetes más (use nivel de detalle 3 para ver todos)");
        }

        System.out.println("\n=================================================");
    }

    private String formatearPorcentaje(int valor, int total) {
        if (total == 0) return "0.0";
        return String.format("%.1f", (valor * 100.0) / total);
    }

    private void inicializarOcupacionAlmacenes() {
        for (Aeropuerto aeropuerto : aeropuertos) {
            ocupacionAlmacenes.put(aeropuerto, 0);
        }
    }

    private void inicializarOcupacionTemporalAlmacenes() {
        final int TOTAL_MINUTOS = HORIZON_DAYS * 24 * 60;
        for (Aeropuerto aeropuerto : aeropuertos) {
            ocupacionTemporalAlmacenes.put(aeropuerto, new int[TOTAL_MINUTOS]);
        }
    }

    public boolean esSolucionTemporalValida(HashMap<Paquete, ArrayList<Vuelo>> mapaSolucion) {
        inicializarOcupacionTemporalAlmacenes();
        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : mapaSolucion.entrySet()) {
            Paquete paquete = entrada.getKey();
            ArrayList<Vuelo> ruta = entrada.getValue();
            if (!simularFlujoPaquete(paquete, ruta)) {
                return false;
            }
        }
        return true;
    }

    private boolean simularFlujoPaquete(Paquete paquete, ArrayList<Vuelo> ruta) {
        if (ruta == null || ruta.isEmpty()) {
            Aeropuerto destino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
            int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
            int inicio = obtenerTiempoInicioPaquete(paquete);
            return agregarOcupacionTemporal(destino, inicio, Constantes.HORAS_MAX_RECOGIDA_CLIENTE * 60, conteoProductos);
        }

        int minutoActual = obtenerTiempoInicioPaquete(paquete);
        int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;

        for (int i = 0; i < ruta.size(); i++) {
            Vuelo vuelo = ruta.get(i);
            Aeropuerto salida = vuelo.getAeropuertoOrigen();
            Aeropuerto llegada = vuelo.getAeropuertoDestino();

            int tiempoEspera = 120;
            if (!agregarOcupacionTemporal(salida, minutoActual, tiempoEspera, conteoProductos)) {
                System.out.println("Violación de capacidad en " + salida.getCiudad().getNombre() +
                                  " en minuto " + minutoActual + " (fase de espera) para paquete " + paquete.getId());
                return false;
            }

            int inicioVuelo = minutoActual + tiempoEspera;
            int duracionVuelo = (int)(vuelo.getTiempoTransporte() * 60);
            int minutoLlegada = inicioVuelo + duracionVuelo;

            int duracionEstancia;
            if (i < ruta.size() - 1) duracionEstancia = 120;
            else duracionEstancia = Constantes.HORAS_MAX_RECOGIDA_CLIENTE * 60;

            if (duracionEstancia > 0 && !agregarOcupacionTemporal(llegada, minutoLlegada, duracionEstancia, conteoProductos)) {
                System.out.println("Violación de capacidad en " + llegada.getCiudad().getNombre() +
                                  " en minuto " + minutoLlegada + " (fase de llegada) para paquete " + paquete.getId());
                return false;
            }

            minutoActual = minutoLlegada;
            if (i < ruta.size() - 1) minutoActual += 120;
        }

        return true;
    }

    private boolean agregarOcupacionTemporal(Aeropuerto aeropuerto, int minutoInicio, int duracionMinutos, int conteoProductos) {
        if (aeropuerto == null) return false;
        int[] array = ocupacionTemporalAlmacenes.get(aeropuerto);
        int capacidadMaxima = aeropuerto.getCapacidadMaxima();
        final int TOTAL_MINUTOS = HORIZON_DAYS * 24 * 60;
        int inicioClamp = Math.max(0, Math.min(minutoInicio, TOTAL_MINUTOS - 1));
        int finClamp = Math.max(0, Math.min(minutoInicio + duracionMinutos, TOTAL_MINUTOS));
        for (int m = inicioClamp; m < finClamp; m++) {
            array[m] += conteoProductos;
            if (array[m] > capacidadMaxima) return false;
        }
        return true;
    }

    private int[] findPeakOccupancy(Aeropuerto aeropuerto) {
        int[] array = ocupacionTemporalAlmacenes.get(aeropuerto);
        int max = 0; int minuto = 0;
        final int TOTAL_MINUTOS = HORIZON_DAYS * 24 * 60;
        for (int m = 0; m < TOTAL_MINUTOS; m++) {
            if (array[m] > max) { max = array[m]; minuto = m; }
        }
        return new int[]{minuto, max};
    }
}
