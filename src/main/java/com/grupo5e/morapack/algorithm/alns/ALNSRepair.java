package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.core.model.Aeropuerto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Collections;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Clase que implementa operadores de reparación para el algoritmo ALNS
 * (Adaptive Large Neighborhood Search) específicamente diseñados para el problema
 * de logística MoraPack.
 * 
 * Los operadores priorizan las entregas a tiempo y la eficiencia de rutas.
 */
public class ALNSRepair {
    
    private ArrayList<Aeropuerto> aeropuertos;
    private ArrayList<Vuelo> vuelos;
    private HashMap<Aeropuerto, Integer> ocupacionAlmacenes;
    private Random aleatorio;
    
    public ALNSRepair(ArrayList<Aeropuerto> aeropuertos, ArrayList<Vuelo> vuelos, 
                      HashMap<Aeropuerto, Integer> ocupacionAlmacenes) {
        this.aeropuertos = aeropuertos;
        this.vuelos = vuelos;
        this.ocupacionAlmacenes = ocupacionAlmacenes;
        this.aleatorio = new Random(System.currentTimeMillis());
    }
    
    /**
     * Constructor con semilla específica
     */
    public ALNSRepair(ArrayList<Aeropuerto> aeropuertos, ArrayList<Vuelo> vuelos, 
                      HashMap<Aeropuerto, Integer> ocupacionAlmacenes, long semilla) {
        this.aeropuertos = aeropuertos;
        this.vuelos = vuelos;
        this.ocupacionAlmacenes = ocupacionAlmacenes;
        this.aleatorio = new Random(semilla);
    }
    
    /**
     * Reparación Greedy Mejorada: Inserta paquetes usando enfoque optimizado para MoraPack.
     * Prioriza paquetes por deadline y eficiencia de ruta específica para el negocio.
     */
    public ResultadoReparacion reparacionCodiciosa(
            HashMap<Paquete, ArrayList<Vuelo>> solucionParcial,
            ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> paquetesDestruidos) {
        
        HashMap<Paquete, ArrayList<Vuelo>> solucionReparada = new HashMap<>(solucionParcial);
        ArrayList<Paquete> paquetesNoAsignados = new ArrayList<>();
        
        // Ordenamiento inteligente específico para MoraPack
        ArrayList<Paquete> paquetesParaReparar = new ArrayList<>();
        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : paquetesDestruidos) {
            paquetesParaReparar.add(entrada.getKey());
        }
        
        paquetesParaReparar.sort((p1, p2) -> {
            // 1. Priorizar por urgencia (tiempo restante vs promesa MoraPack)
            double urgencia1 = calcularUrgenciaPaquete(p1);
            double urgencia2 = calcularUrgenciaPaquete(p2);
            int comparacionUrgencia = Double.compare(urgencia2, urgencia1); // Mayor urgencia primero
            if (comparacionUrgencia != 0) return comparacionUrgencia;
            
            // 2. Priorizar paquetes con más productos (mayor valor de negocio)
            int productos1 = p1.getProductos() != null ? p1.getProductos().size() : 1;
            int productos2 = p2.getProductos() != null ? p2.getProductos().size() : 1;
            int comparacionProductos = Integer.compare(productos2, productos1);
            if (comparacionProductos != 0) return comparacionProductos;
            
            // 3. Tie-break por deadline absoluto
            LocalDateTime d1 = p1.getFechaLimiteEntrega();
            LocalDateTime d2 = p2.getFechaLimiteEntrega();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1; // nulls last
            if (d2 == null) return -1; // nulls last
            return d1.compareTo(d2);
        });
        
        int conteoReinsertados = 0;
        
        // Intentar reinsertar cada paquete
        for (Paquete paquete : paquetesParaReparar) {
            Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
            
            // Obtener conteo de productos para este paquete
            int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
            
            // Verificar capacidad del almacén
            if (aeropuertoDestino == null || !tieneCapacidadAlmacen(aeropuertoDestino, conteoProductos)) {
                paquetesNoAsignados.add(paquete);
                continue;
            }
            
            // Buscar la mejor ruta
            ArrayList<Vuelo> mejorRuta = encontrarMejorRuta(paquete);
            if (mejorRuta != null && esRutaValida(paquete, mejorRuta, conteoProductos)) {
                solucionReparada.put(paquete, mejorRuta);
                actualizarCapacidadesVuelos(mejorRuta, conteoProductos);
                incrementarOcupacionAlmacen(aeropuertoDestino, conteoProductos);
                conteoReinsertados++;
            } else {
                paquetesNoAsignados.add(paquete);
            }
        }
        
        System.out.println("Reparación Greedy: " + conteoReinsertados + "/" + paquetesParaReparar.size() + 
                          " paquetes reinsertados");
        
        return new ResultadoReparacion(solucionReparada, paquetesNoAsignados);
    }
    
    /**
     * Reparación por Regret: Calcula el "arrepentimiento" de no insertar cada paquete
     * y prioriza aquellos con mayor diferencia entre mejor y segunda mejor opción.
     */
    public ResultadoReparacion reparacionArrepentimiento(
            HashMap<Paquete, ArrayList<Vuelo>> solucionParcial,
            ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> paquetesDestruidos,
            int nivelArrepentimiento) {
        
        HashMap<Paquete, ArrayList<Vuelo>> solucionReparada = new HashMap<>(solucionParcial);
        ArrayList<Paquete> paquetesNoAsignados = new ArrayList<>();
        
        ArrayList<Paquete> paquetesRestantes = new ArrayList<>();
        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : paquetesDestruidos) {
            paquetesRestantes.add(entrada.getKey());
        }
        
        int conteoReinsertados = 0;
        
        // Mientras haya paquetes por insertar
        while (!paquetesRestantes.isEmpty()) {
            Paquete mejorPaquete = null;
            ArrayList<Vuelo> mejorRuta = null;
            double maxArrepentimiento = Double.NEGATIVE_INFINITY;
            
            // Calcular arrepentimiento para cada paquete restante
            for (Paquete paquete : paquetesRestantes) {
                Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
                int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
                
                if (aeropuertoDestino == null || !tieneCapacidadAlmacen(aeropuertoDestino, conteoProductos)) {
                    continue;
                }
                
                // Encontrar las mejores rutas para este paquete
                ArrayList<OpcionRuta> opcionesRuta = encontrarTodasLasOpcionesRuta(paquete);
                
                if (opcionesRuta.isEmpty()) {
                    continue;
                }
                
                // Ordenar por margen de tiempo (mejor primero)
                opcionesRuta.sort((r1, r2) -> Double.compare(r2.margenTiempo, r1.margenTiempo));
                
                // Calcular regret-k real
                double arrepentimiento = 0;
                int k = Math.max(2, nivelArrepentimiento);
                int limite = Math.min(k, opcionesRuta.size());
                if (limite >= 2) {
                    double mejorMargen = opcionesRuta.get(0).margenTiempo;
                    for (int i = 1; i < limite; i++) {
                        arrepentimiento += (mejorMargen - opcionesRuta.get(i).margenTiempo);
                    }
                } else if (opcionesRuta.size() == 1) {
                    // Solo una opción: usar urgencia basada en orderDate→deadline
                    if (paquete.getFechaPedido() != null && paquete.getFechaLimiteEntrega() != null) {
                        long horasHastaDeadline = ChronoUnit.HOURS.between(paquete.getFechaPedido(), paquete.getFechaLimiteEntrega());
                        arrepentimiento = Math.max(0, 72 - Math.min(72, horasHastaDeadline));
                    } else {
                        arrepentimiento = 0;
                    }
                }
                
                // Añadir factor de urgencia al arrepentimiento
                LocalDateTime ahora = LocalDateTime.now();
                long horasHastaDeadline = ChronoUnit.HOURS.between(ahora, paquete.getFechaLimiteEntrega());
                double factorUrgencia = Math.max(1, 72.0 / Math.max(1, horasHastaDeadline));
                arrepentimiento *= factorUrgencia;
                
                if (arrepentimiento > maxArrepentimiento) {
                    maxArrepentimiento = arrepentimiento;
                    mejorPaquete = paquete;
                    mejorRuta = opcionesRuta.get(0).ruta;
                }
            }
            
            // Insertar el paquete con mayor arrepentimiento
            if (mejorPaquete != null && mejorRuta != null && esRutaValida(mejorPaquete, mejorRuta, Math.max(1, mejorPaquete.getProductos() != null ? mejorPaquete.getProductos().size() : 1))) {
                solucionReparada.put(mejorPaquete, mejorRuta);
                int conteoProductos = mejorPaquete.getProductos() != null ? mejorPaquete.getProductos().size() : 1;
                actualizarCapacidadesVuelos(mejorRuta, conteoProductos);
                incrementarOcupacionAlmacen(obtenerAeropuertoPorCiudad(mejorPaquete.getCiudadDestino()), conteoProductos);
                paquetesRestantes.remove(mejorPaquete);
                conteoReinsertados++;
            } else {
                // No se pudo insertar ningún paquete, agregar todos los restantes como no asignados
                paquetesNoAsignados.addAll(paquetesRestantes);
                break;
            }
        }
        
        System.out.println("Reparación por Arrepentimiento: " + conteoReinsertados + "/" + paquetesDestruidos.size() + 
                          " paquetes reinsertados");
        
        return new ResultadoReparacion(solucionReparada, paquetesNoAsignados);
    }
    
    /**
     * Reparación por tiempo: Prioriza paquetes con deadlines más cercanos.
     */
    public ResultadoReparacion reparacionPorTiempo(
            HashMap<Paquete, ArrayList<Vuelo>> solucionParcial,
            ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> paquetesDestruidos) {
        
        HashMap<Paquete, ArrayList<Vuelo>> solucionReparada = new HashMap<>(solucionParcial);
        ArrayList<Paquete> paquetesNoAsignados = new ArrayList<>();
        
        // Extraer paquetes y ordenar por urgencia (deadline más cercano primero)
        ArrayList<Paquete> paquetesParaReparar = new ArrayList<>();
        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : paquetesDestruidos) {
            paquetesParaReparar.add(entrada.getKey());
        }
        
        paquetesParaReparar.sort((p1, p2) -> {
            // Ordenar por presupuesto real desde orderDate (nulls last)
            if (p1.getFechaPedido() == null || p1.getFechaLimiteEntrega() == null) return 1;
            if (p2.getFechaPedido() == null || p2.getFechaLimiteEntrega() == null) return -1;
            long horasP1 = ChronoUnit.HOURS.between(p1.getFechaPedido(), p1.getFechaLimiteEntrega());
            long horasP2 = ChronoUnit.HOURS.between(p2.getFechaPedido(), p2.getFechaLimiteEntrega());
            return Long.compare(horasP1, horasP2);
        });
        
        int conteoReinsertados = 0;
        
        // Insertar paquetes en orden de urgencia
        for (Paquete paquete : paquetesParaReparar) {
            Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
            int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
            
            if (aeropuertoDestino == null || !tieneCapacidadAlmacen(aeropuertoDestino, conteoProductos)) {
                paquetesNoAsignados.add(paquete);
                continue;
            }
            
            // Buscar ruta con mayor margen de tiempo
            ArrayList<Vuelo> mejorRuta = encontrarRutaConMaximoMargen(paquete);
            if (mejorRuta != null && esRutaValida(paquete, mejorRuta, conteoProductos)) {
                solucionReparada.put(paquete, mejorRuta);
                actualizarCapacidadesVuelos(mejorRuta, conteoProductos);
                incrementarOcupacionAlmacen(aeropuertoDestino, conteoProductos);
                conteoReinsertados++;
            } else {
                paquetesNoAsignados.add(paquete);
            }
        }
        
        System.out.println("Reparación por tiempo: " + conteoReinsertados + "/" + paquetesParaReparar.size() + 
                          " paquetes reinsertados");
        
        return new ResultadoReparacion(solucionReparada, paquetesNoAsignados);
    }
    
    /**
     * Reparación por capacidad: Prioriza rutas con mayor capacidad disponible.
     */
    public ResultadoReparacion reparacionPorCapacidad(
            HashMap<Paquete, ArrayList<Vuelo>> solucionParcial,
            ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> paquetesDestruidos) {
        
        HashMap<Paquete, ArrayList<Vuelo>> solucionReparada = new HashMap<>(solucionParcial);
        ArrayList<Paquete> paquetesNoAsignados = new ArrayList<>();
        
        ArrayList<Paquete> paquetesParaReparar = new ArrayList<>();
        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : paquetesDestruidos) {
            paquetesParaReparar.add(entrada.getKey());
        }
        
        // Ordenar por deadline como criterio secundario
        // PATCH: Null-safe sorting
        paquetesParaReparar.sort((p1, p2) -> {
            if (p1.getFechaLimiteEntrega() == null && p2.getFechaLimiteEntrega() == null) return 0;
            if (p1.getFechaLimiteEntrega() == null) return 1; // nulls last
            if (p2.getFechaLimiteEntrega() == null) return -1; // nulls last
            return p1.getFechaLimiteEntrega().compareTo(p2.getFechaLimiteEntrega());
        });
        
        int conteoReinsertados = 0;
        
        for (Paquete paquete : paquetesParaReparar) {
            Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(paquete.getCiudadDestino());
            int conteoProductos = paquete.getProductos() != null ? paquete.getProductos().size() : 1;
            
            if (aeropuertoDestino == null || !tieneCapacidadAlmacen(aeropuertoDestino, conteoProductos)) {
                paquetesNoAsignados.add(paquete);
                continue;
            }
            
            // Buscar ruta con mayor capacidad disponible
            ArrayList<Vuelo> mejorRuta = encontrarRutaConMaximaCapacidad(paquete);
            if (mejorRuta != null && esRutaValida(paquete, mejorRuta, conteoProductos)) {
                solucionReparada.put(paquete, mejorRuta);
                actualizarCapacidadesVuelos(mejorRuta, conteoProductos);
                incrementarOcupacionAlmacen(aeropuertoDestino, conteoProductos);
                conteoReinsertados++;
            } else {
                paquetesNoAsignados.add(paquete);
            }
        }
        
        System.out.println("Reparación por capacidad: " + conteoReinsertados + "/" + paquetesParaReparar.size() + 
                          " paquetes reinsertados");
        
        return new ResultadoReparacion(solucionReparada, paquetesNoAsignados);
    }
    
    // ================= MÉTODOS AUXILIARES =================
    
    /**
     * Calcula la urgencia de un paquete específico para MoraPack
     * Considera promesas de entrega según continentes y tiempo restante
     */
    private double calcularUrgenciaPaquete(Paquete paquete) {
        if (paquete.getFechaPedido() == null || paquete.getFechaLimiteEntrega() == null) {
            return 0.0; // Sin información de tiempo, baja prioridad
        }
        
        // Calcular tiempo disponible desde orden hasta deadline
        long totalHorasDisponibles = ChronoUnit.HOURS.between(paquete.getFechaPedido(), paquete.getFechaLimiteEntrega());
        
        // Determinar promesa MoraPack según continentes
        boolean rutaMismoContinente = paquete.getUbicacionActual().getContinente() == 
                                    paquete.getCiudadDestino().getContinente();
        long promesaHorasMoraPack = rutaMismoContinente ? 48 : 72; // 2 días intra / 3 días inter
        
        // Calcular factor de urgencia
        double factorUrgencia;
        if (totalHorasDisponibles <= promesaHorasMoraPack * 0.5) {
            // Muy urgente: menos de la mitad del tiempo de promesa disponible
            factorUrgencia = 10.0;
        } else if (totalHorasDisponibles <= promesaHorasMoraPack * 0.75) {
            // Urgente: menos del 75% del tiempo de promesa disponible
            factorUrgencia = 5.0;
        } else if (totalHorasDisponibles <= promesaHorasMoraPack) {
            // Moderadamente urgente: dentro del tiempo de promesa
            factorUrgencia = 3.0;
        } else if (totalHorasDisponibles <= promesaHorasMoraPack * 1.5) {
            // Tiempo holgado: 50% más tiempo que la promesa
            factorUrgencia = 1.0;
        } else {
            // Mucho tiempo disponible
            factorUrgencia = 0.5;
        }
        
        // Ajustar por prioridad del paquete (si está disponible)
        if (paquete.getPrioridad() > 0) {
            factorUrgencia *= (1.0 + paquete.getPrioridad() / 10.0); // Boost por prioridad
        }
        
        // Penalizar si excede promesa MoraPack
        if (totalHorasDisponibles > promesaHorasMoraPack * 1.2) {
            factorUrgencia *= 0.8; // Reducir prioridad para paquetes con demasiado tiempo
        }
        
        return factorUrgencia;
    }
    
    private ArrayList<OpcionRuta> encontrarTodasLasOpcionesRuta(Paquete paquete) {
        ArrayList<OpcionRuta> opciones = new ArrayList<>();
        Ciudad origen = paquete.getUbicacionActual();
        Ciudad destino = paquete.getCiudadDestino();
        
        if (origen.equals(destino)) {
            opciones.add(new OpcionRuta(new ArrayList<>(), Double.MAX_VALUE));
            return opciones;
        }
        
        // Buscar ruta directa
        ArrayList<Vuelo> rutaDirecta = encontrarRutaDirecta(origen, destino);
        if (rutaDirecta != null && esRutaValida(paquete, rutaDirecta)) {
            double margen = calcularMargenTiempoRuta(paquete, rutaDirecta);
            opciones.add(new OpcionRuta(rutaDirecta, margen));
        }
        
        // Buscar rutas con una escala
        ArrayList<Vuelo> rutaUnaEscala = encontrarRutaUnaEscala(origen, destino);
        if (rutaUnaEscala != null && esRutaValida(paquete, rutaUnaEscala)) {
            double margen = calcularMargenTiempoRuta(paquete, rutaUnaEscala);
            opciones.add(new OpcionRuta(rutaUnaEscala, margen));
        }
        
        // Buscar rutas con dos escalas
        ArrayList<Vuelo> rutaDosEscalas = encontrarRutaDosEscalas(origen, destino);
        if (rutaDosEscalas != null && esRutaValida(paquete, rutaDosEscalas)) {
            double margen = calcularMargenTiempoRuta(paquete, rutaDosEscalas);
            opciones.add(new OpcionRuta(rutaDosEscalas, margen));
        }
        
        return opciones;
    }
    
    private ArrayList<Vuelo> encontrarMejorRuta(Paquete paquete) {
        ArrayList<OpcionRuta> opciones = encontrarTodasLasOpcionesRuta(paquete);
        if (opciones.isEmpty()) return null;
        
        // Seleccionar la ruta con mayor margen de tiempo
        opciones.sort((r1, r2) -> Double.compare(r2.margenTiempo, r1.margenTiempo));
        return opciones.get(0).ruta;
    }
    
    private ArrayList<Vuelo> encontrarRutaConMaximoMargen(Paquete paquete) {
        return encontrarMejorRuta(paquete); // Ya implementado arriba
    }
    
    private ArrayList<Vuelo> encontrarRutaConMaximaCapacidad(Paquete paquete) {
        ArrayList<OpcionRuta> opciones = encontrarTodasLasOpcionesRuta(paquete);
        if (opciones.isEmpty()) return null;
        
        // Calcular capacidad disponible para cada ruta
        ArrayList<OpcionCapacidadRuta> opcionesCapacidad = new ArrayList<>();
        for (OpcionRuta opcion : opciones) {
            double capacidadTotal = 0;
            double capacidadUsada = 0;
            
            for (Vuelo vuelo : opcion.ruta) {
                capacidadTotal += vuelo.getCapacidadMaxima();
                capacidadUsada += vuelo.getCapacidadUsada();
            }
            
            double ratioCapacidadDisponible = (capacidadTotal - capacidadUsada) / Math.max(1, capacidadTotal);
            opcionesCapacidad.add(new OpcionCapacidadRuta(opcion.ruta, ratioCapacidadDisponible, opcion.margenTiempo));
        }
        
        // Ordenar por capacidad disponible, pero considerando también el margen de tiempo
        opcionesCapacidad.sort((r1, r2) -> {
            // Priorizar rutas con capacidad disponible, pero no sacrificar entregas a tiempo
            if (r1.margenTiempo <= 0 && r2.margenTiempo > 0) return 1;
            if (r2.margenTiempo <= 0 && r1.margenTiempo > 0) return -1;
            
            return Double.compare(r2.capacidadDisponible, r1.capacidadDisponible);
        });
        
        return opcionesCapacidad.get(0).ruta;
    }
    
    /**
     * PATCH: Calcula margen sin doble conteo y usando fechaPedido
     */
    private double calcularMargenTiempoRuta(Paquete paquete, ArrayList<Vuelo> ruta) {
        if (ruta == null || ruta.isEmpty()) {
            return 0.0;
        }
        
        // Sumar solo tiempo de vuelos + conexiones (sin extras por continente)
        double tiempoTotal = 0.0;
        for (Vuelo vuelo : ruta) {
            tiempoTotal += vuelo.getTiempoTransporte();
        }
        
        // Agregar tiempo de conexión (2 horas por conexión)
        tiempoTotal += (ruta.size() - 1) * 2.0;
        
        // PATCH: Usar fechaPedido vs deadline, null-safe
        if (paquete.getFechaPedido() == null || paquete.getFechaLimiteEntrega() == null) {
            return 1.0;
        }
        
        // Calcular presupuesto de horas desde fechaPedido
        long presupuesto = ChronoUnit.HOURS.between(paquete.getFechaPedido(), paquete.getFechaLimiteEntrega());
        if (presupuesto < 0) presupuesto = 0; // Clamp negativo
        
        double margen = presupuesto - tiempoTotal;
        return Math.max(margen, 0.0) + 1.0;
    }
    
    /**
     * PATCH: Helper para validar capacidad de ruta con cantidad específica
     */
    private boolean cabeEnCapacidadRuta(ArrayList<Vuelo> ruta, int cantidad) {
        if (ruta == null) return false;
        for (Vuelo f : ruta) {
            if (f.getCapacidadUsada() + cantidad > f.getCapacidadMaxima()) return false;
        }
        return true;
    }
    
    /**
     * PATCH: Versión con cantidad específica de productos
     */
    private boolean esRutaValida(Paquete paquete, ArrayList<Vuelo> ruta, int cantidad) {
        if (ruta == null || ruta.isEmpty()) {
            return paquete.getUbicacionActual().equals(paquete.getCiudadDestino());
        }
        
        // Verificar capacidad de vuelos con cantidad específica
        if (!cabeEnCapacidadRuta(ruta, cantidad)) {
            return false;
        }
        
        // Verificar continuidad de ruta
        Ciudad ubicacionActual = paquete.getUbicacionActual();
        for (Vuelo vuelo : ruta) {
            Aeropuerto aeropuertoActual = obtenerAeropuertoPorCiudad(ubicacionActual);
            if (!vuelo.getAeropuertoOrigen().equals(aeropuertoActual)) {
                return false;
            }
            ubicacionActual = obtenerCiudadPorAeropuerto(vuelo.getAeropuertoDestino());
        }
        
        if (!ubicacionActual.equals(paquete.getCiudadDestino())) {
            return false;
        }
        
        // Verificar deadline
        return seRespetaDeadline(paquete, ruta);
    }
    
    /**
     * PATCH: Versión original que delega calculando cantidad
     */
    private boolean esRutaValida(Paquete paquete, ArrayList<Vuelo> ruta) {
        int cantidad = (paquete.getProductos() != null && !paquete.getProductos().isEmpty()) ? paquete.getProductos().size() : 1;
        return esRutaValida(paquete, ruta, cantidad);
    }
    
    /**
     * CORRECCIÓN: Aplicar las mismas correcciones que Solution.java
     */
    private boolean seRespetaDeadline(Paquete paquete, ArrayList<Vuelo> ruta) {
        double tiempoTotal = 0;
        
        // CORRECCIÓN: Solo usar transportTime de vuelos (sin doble conteo)
        for (Vuelo vuelo : ruta) {
            tiempoTotal += vuelo.getTiempoTransporte();
        }
        
        // Añadir penalización por conexiones
        if (ruta.size() > 1) {
            tiempoTotal += (ruta.size() - 1) * 2.0;
        }
        
        // CORRECCIÓN: Validar promesas MoraPack explícitamente
        Ciudad origen = paquete.getUbicacionActual();
        Ciudad destino = paquete.getCiudadDestino();
        boolean rutaMismoContinente = origen.getContinente() == destino.getContinente();
        long promesaHorasMoraPack = rutaMismoContinente ? 48 : 72; // 2 días intra / 3 días inter
        
        if (tiempoTotal > promesaHorasMoraPack) {
            return false; // Excede promesa MoraPack
        }
        
        // Factor de seguridad
        if (aleatorio != null) {
            int factorComplejidad = ruta.size() + (rutaMismoContinente ? 0 : 2);
            double margenSeguridad = 0.01 * (1 + aleatorio.nextInt(factorComplejidad * 3));
            tiempoTotal = tiempoTotal * (1.0 + margenSeguridad);
        }
        
        // CORRECCIÓN: Usar orderDate en lugar de "now"
        long horasHastaDeadline = ChronoUnit.HOURS.between(paquete.getFechaPedido(), paquete.getFechaLimiteEntrega());
        
        return tiempoTotal <= horasHastaDeadline;
    }
    
    // Métodos de búsqueda de rutas (simplificados, podrían referenciar a Solution.java)
    private ArrayList<Vuelo> encontrarRutaDirecta(Ciudad origen, Ciudad destino) {
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
    
    private ArrayList<Vuelo> encontrarRutaUnaEscala(Ciudad origen, Ciudad destino) {
        Aeropuerto aeropuertoOrigen = obtenerAeropuertoPorCiudad(origen);
        Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(destino);
        
        if (aeropuertoOrigen == null || aeropuertoDestino == null) return null;
        
        // Crear lista de aeropuertos intermedios y barajarla
        ArrayList<Aeropuerto> intermedios = new ArrayList<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (!aeropuerto.equals(aeropuertoOrigen) && !aeropuerto.equals(aeropuertoDestino)) {
                intermedios.add(aeropuerto);
            }
        }
        Collections.shuffle(intermedios, aleatorio);
        
        for (Aeropuerto intermedio : intermedios) {
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
                return ruta;
            }
        }
        
        return null;
    }
    
    private ArrayList<Vuelo> encontrarRutaDosEscalas(Ciudad origen, Ciudad destino) {
        Aeropuerto aeropuertoOrigen = obtenerAeropuertoPorCiudad(origen);
        Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCiudad(destino);
        
        if (aeropuertoOrigen == null || aeropuertoDestino == null) return null;
        
        // Simplificado: buscar solo algunas combinaciones aleatorias para eficiencia
        ArrayList<Aeropuerto> candidatos = new ArrayList<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (!aeropuerto.equals(aeropuertoOrigen) && !aeropuerto.equals(aeropuertoDestino)) {
                candidatos.add(aeropuerto);
            }
        }
        
        if (candidatos.size() < 2) return null;
        
        Collections.shuffle(candidatos, aleatorio);
        int maxIntentos = Math.min(10, candidatos.size() - 1);
        
        for (int i = 0; i < maxIntentos; i++) {
            Aeropuerto primero = candidatos.get(i);
            for (int j = i + 1; j < Math.min(i + 5, candidatos.size()); j++) {
                Aeropuerto segundo = candidatos.get(j);
                
                ArrayList<Vuelo> ruta = intentarRutaDosEscalas(aeropuertoOrigen, primero, segundo, aeropuertoDestino);
                if (ruta != null) return ruta;
                
                // También probar en orden inverso
                ruta = intentarRutaDosEscalas(aeropuertoOrigen, segundo, primero, aeropuertoDestino);
                if (ruta != null) return ruta;
            }
        }
        
        return null;
    }
    
    private ArrayList<Vuelo> intentarRutaDosEscalas(Aeropuerto origen, Aeropuerto primero, Aeropuerto segundo, Aeropuerto destino) {
        Vuelo vuelo1 = null, vuelo2 = null, vuelo3 = null;
        
        // Buscar vuelo 1: origen -> primero
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen().equals(origen) && 
                vuelo.getAeropuertoDestino().equals(primero) &&
                vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                vuelo1 = vuelo;
                break;
            }
        }
        
        if (vuelo1 == null) return null;
        
        // Buscar vuelo 2: primero -> segundo
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen().equals(primero) && 
                vuelo.getAeropuertoDestino().equals(segundo) &&
                vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                vuelo2 = vuelo;
                break;
            }
        }
        
        if (vuelo2 == null) return null;
        
        // Buscar vuelo 3: segundo -> destino
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen().equals(segundo) && 
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
    
    /**
     * PATCH: Ciudad→Aeropuerto robusto por nombre (evita equals frágil)
     */
    private Aeropuerto obtenerAeropuertoPorCiudad(Ciudad ciudad) {
        if (ciudad == null || ciudad.getNombre() == null) return null;
        
        String nombreCiudad = ciudad.getNombre().trim().toLowerCase();
        
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (aeropuerto.getCiudad() != null && aeropuerto.getCiudad().getNombre() != null &&
                aeropuerto.getCiudad().getNombre().trim().toLowerCase().equals(nombreCiudad)) {
                return aeropuerto;
            }
        }
        return null;
    }
    
    private Ciudad obtenerCiudadPorAeropuerto(Aeropuerto aeropuerto) {
        return aeropuerto.getCiudad();
    }
    
    private boolean tieneCapacidadAlmacen(Aeropuerto aeropuertoDestino, int conteoProductos) {
        if (aeropuertoDestino.getAlmacen() == null) {
            return false;
        }
        
        int ocupacionActual = ocupacionAlmacenes.getOrDefault(aeropuertoDestino, 0);
        return (ocupacionActual + conteoProductos) <= aeropuertoDestino.getAlmacen().getCapacidadMaxima();
    }
    
    private void actualizarCapacidadesVuelos(ArrayList<Vuelo> ruta, int conteoProductos) {
        for (Vuelo vuelo : ruta) {
            vuelo.setCapacidadUsada(vuelo.getCapacidadUsada() + conteoProductos);
        }
    }
    
    private void incrementarOcupacionAlmacen(Aeropuerto aeropuerto, int conteoProductos) {
        int actual = ocupacionAlmacenes.getOrDefault(aeropuerto, 0);
        ocupacionAlmacenes.put(aeropuerto, actual + conteoProductos);
    }
    
    // ================= CLASES AUXILIARES =================
    
    private static class OpcionRuta {
        ArrayList<Vuelo> ruta;
        double margenTiempo;
        
        OpcionRuta(ArrayList<Vuelo> ruta, double margenTiempo) {
            this.ruta = ruta;
            this.margenTiempo = margenTiempo;
        }
    }
    
    private static class OpcionCapacidadRuta {
        ArrayList<Vuelo> ruta;
        double capacidadDisponible;
        double margenTiempo;
        
        OpcionCapacidadRuta(ArrayList<Vuelo> ruta, double capacidadDisponible, double margenTiempo) {
            this.ruta = ruta;
            this.capacidadDisponible = capacidadDisponible;
            this.margenTiempo = margenTiempo;
        }
    }
    
    /**
     * Clase para encapsular el resultado de una operación de reparación
     */
    public static class ResultadoReparacion {
        private HashMap<Paquete, ArrayList<Vuelo>> solucionReparada;
        private ArrayList<Paquete> paquetesNoAsignados;
        
        public ResultadoReparacion(HashMap<Paquete, ArrayList<Vuelo>> solucionReparada,
                           ArrayList<Paquete> paquetesNoAsignados) {
            this.solucionReparada = solucionReparada;
            this.paquetesNoAsignados = paquetesNoAsignados;
        }
        
        public HashMap<Paquete, ArrayList<Vuelo>> getSolucionReparada() {
            return solucionReparada;
        }
        
        public ArrayList<Paquete> getPaquetesNoAsignados() {
            return paquetesNoAsignados;
        }
        
        public int getNumPaquetesReparados() {
            return solucionReparada.size();
        }
        
        public boolean esExitoso() {
            return !solucionReparada.isEmpty() || paquetesNoAsignados.isEmpty();
        }
        
        public int getNumPaquetesNoAsignados() {
            return paquetesNoAsignados.size();
        }
    }
}