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
     * Reparación Greedy: Inserta paquetes usando el enfoque greedy optimizado.
     * Prioriza paquetes por deadline y busca la mejor ruta disponible.
     */
    public ResultadoReparacion reparacionCodiciosa(
            HashMap<Paquete, ArrayList<Vuelo>> solucionParcial,
            ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> paquetesDestruidos) {
        
        HashMap<Paquete, ArrayList<Vuelo>> solucionReparada = new HashMap<>(solucionParcial);
        ArrayList<Paquete> paquetesNoAsignados = new ArrayList<>();
        
        // Ordenar paquetes destruidos por deadline (más urgente primero)
        ArrayList<Paquete> paquetesParaReparar = new ArrayList<>();
        for (Map.Entry<Paquete, ArrayList<Vuelo>> entrada : paquetesDestruidos) {
            paquetesParaReparar.add(entrada.getKey());
        }
        
        // PATCH: Null-safe sorting
        paquetesParaReparar.sort((p1, p2) -> {
            if (p1.getFechaLimiteEntrega() == null && p2.getFechaLimiteEntrega() == null) return 0;
            if (p1.getFechaLimiteEntrega() == null) return 1; // nulls last
            if (p2.getFechaLimiteEntrega() == null) return -1; // nulls last
            return p1.getFechaLimiteEntrega().compareTo(p2.getFechaLimiteEntrega());
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
    public ResultadoReparacion reparacionPorArrepentimiento(
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
                
                // Calcular arrepentimiento
                double arrepentimiento = 0;
                if (opcionesRuta.size() >= 2) {
                    // Arrepentimiento = diferencia entre mejor y segunda mejor opción
                    arrepentimiento = opcionesRuta.get(0).margenTiempo - opcionesRuta.get(1).margenTiempo;
                } else if (opcionesRuta.size() == 1) {
                    // Solo una opción: arrepentimiento basado en urgencia
                    LocalDateTime ahora = LocalDateTime.now();
                    long horasHastaDeadline = ChronoUnit.HOURS.between(ahora, paquete.getFechaLimiteEntrega());
                    arrepentimiento = Math.max(0, 168 - horasHastaDeadline); // Más arrepentimiento para deadlines más cercanos
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
            if (mejorPaquete != null && mejorRuta != null) {
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
    public ResultadoReparacion reparacionBasadaEnTiempo(
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
            LocalDateTime ahora = LocalDateTime.now();
            long horasP1 = ChronoUnit.HOURS.between(ahora, p1.getFechaLimiteEntrega());
            long horasP2 = ChronoUnit.HOURS.between(ahora, p2.getFechaLimiteEntrega());
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
    public ResultadoReparacion reparacionBasadaEnCapacidad(
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
    
    private ArrayList<OpcionRuta> encontrarTodasLasOpcionesRuta(Paquete paquete) {
        ArrayList<OpcionRuta> opciones = new ArrayList<>();
        Ciudad origen = paquete.getUbicacionActual();
        Ciudad destino = paquete.getCiudadDestino();
        
        if (origen.equals(destino)) {
            opciones.add(new OpcionRuta(new ArrayList<>(), Double.MAX_VALUE));
            return opciones;
        }
        
        // Buscar ruta directa
        ArrayList<Vuelo> rutaDirecta = buscarRutaDirecta(origen, destino);
        if (rutaDirecta != null && esRutaValida(paquete, rutaDirecta)) {
            double margen = calcularMargenTiempoRuta(paquete, rutaDirecta);
            opciones.add(new OpcionRuta(rutaDirecta, margen));
        }
        
        // Buscar rutas con una escala
        ArrayList<Vuelo> rutaConEscala = buscarRutaConEscala(origen, destino);
        if (rutaConEscala != null && esRutaValida(paquete, rutaConEscala)) {
            double margen = calcularMargenTiempoRuta(paquete, rutaConEscala);
            opciones.add(new OpcionRuta(rutaConEscala, margen));
        }
        
        // Buscar rutas con dos escalas
        ArrayList<Vuelo> rutaConDosEscalas = buscarRutaConDosEscalas(origen, destino);
        if (rutaConDosEscalas != null && esRutaValida(paquete, rutaConDosEscalas)) {
            double margen = calcularMargenTiempoRuta(paquete, rutaConDosEscalas);
            opciones.add(new OpcionRuta(rutaConDosEscalas, margen));
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
     * PATCH: Verifica deadline sin doble conteo y null-safe
     */
    private boolean seRespetaDeadline(Paquete paquete, ArrayList<Vuelo> ruta) {
        if (ruta == null || ruta.isEmpty()) {
            return false;
        }
        
        // PATCH: Null-safe
        if (paquete.getFechaPedido() == null || paquete.getFechaLimiteEntrega() == null) {
            return false;
        }
        
        // Sumar solo tiempo de vuelos + conexiones
        double tiempoTotal = 0.0;
        for (Vuelo vuelo : ruta) {
            tiempoTotal += vuelo.getTiempoTransporte();
        }
        
        // Agregar tiempo de conexión
        tiempoTotal += (ruta.size() - 1) * 2.0;
        
        // Calcular tiempo disponible desde fechaPedido
        long horasHastaDeadline = ChronoUnit.HOURS.between(paquete.getFechaPedido(), paquete.getFechaLimiteEntrega());
        
        return horasHastaDeadline >= tiempoTotal;
    }
    
    // Métodos de búsqueda de rutas (simplificados, podrían referenciar a Solution.java)
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
    
    private ArrayList<Vuelo> buscarRutaConEscala(Ciudad origen, Ciudad destino) {
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
    
    private ArrayList<Vuelo> buscarRutaConDosEscalas(Ciudad origen, Ciudad destino) {
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
                
                ArrayList<Vuelo> ruta = intentarRutaConDosEscalas(aeropuertoOrigen, primero, segundo, aeropuertoDestino);
                if (ruta != null) return ruta;
                
                // También probar en orden inverso
                ruta = intentarRutaConDosEscalas(aeropuertoOrigen, segundo, primero, aeropuertoDestino);
                if (ruta != null) return ruta;
            }
        }
        
        return null;
    }
    
    private ArrayList<Vuelo> intentarRutaConDosEscalas(Aeropuerto origen, Aeropuerto primero, Aeropuerto segundo, Aeropuerto destino) {
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
