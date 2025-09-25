package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.model.*;
import java.util.*;
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
    
    private List<Aeropuerto> aeropuertos;
    private List<Vuelo> vuelos;
    private Map<String, Integer> ocupacionAlmacenes;
    private Random generadorAleatorio;
    
    public ALNSRepair(List<Aeropuerto> aeropuertos, List<Vuelo> vuelos, 
                      Map<String, Integer> ocupacionAlmacenes) {
        this.aeropuertos = aeropuertos;
        this.vuelos = vuelos;
        this.ocupacionAlmacenes = ocupacionAlmacenes;
        this.generadorAleatorio = new Random(System.currentTimeMillis());
    }
    
    /**
     * Constructor con semilla específica
     */
    public ALNSRepair(List<Aeropuerto> aeropuertos, List<Vuelo> vuelos, 
                      Map<String, Integer> ocupacionAlmacenes, long semilla) {
        this.aeropuertos = aeropuertos;
        this.vuelos = vuelos;
        this.ocupacionAlmacenes = ocupacionAlmacenes;
        this.generadorAleatorio = new Random(semilla);
    }
    
    /**
     * Reparación Greedy: Inserta paquetes usando el enfoque greedy optimizado.
     * Prioriza paquetes por deadline y busca la mejor ruta disponible.
     */
    public RepairResult greedyRepair(
            Map<Paquete, Ruta> solucionParcial,
            List<Map.Entry<Paquete, Ruta>> paquetesDestruidos) {
        
        Map<Paquete, Ruta> solucionReparada = new HashMap<>(solucionParcial);
        List<Paquete> paquetesNoAsignados = new ArrayList<>();
        
        // Ordenar paquetes destruidos por deadline (más urgente primero)
        List<Paquete> paquetesParaReparar = new ArrayList<>();
        for (Map.Entry<Paquete, Ruta> entrada : paquetesDestruidos) {
            paquetesParaReparar.add(entrada.getKey());
        }
        
        // Ordenamiento null-safe
        paquetesParaReparar.sort((p1, p2) -> {
            if (p1.getFechaLimiteEntrega() == null && p2.getFechaLimiteEntrega() == null) return 0;
            if (p1.getFechaLimiteEntrega() == null) return 1; // nulls al final
            if (p2.getFechaLimiteEntrega() == null) return -1; // nulls al final
            return p1.getFechaLimiteEntrega().compareTo(p2.getFechaLimiteEntrega());
        });
        
        int paquetesReinsertados = 0;
        
        // Intentar reinsertar cada paquete
        for (Paquete paquete : paquetesParaReparar) {
            Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCodigo(paquete.getAeropuertoDestino());
            
            // Obtener cantidad de productos para este paquete
            int cantidadProductos = 1; // En nuestro modelo, cada paquete = 1 producto
            
            // Verificar capacidad del almacén
            if (aeropuertoDestino == null || !tieneCapacidadAlmacen(aeropuertoDestino, cantidadProductos)) {
                paquetesNoAsignados.add(paquete);
                continue;
            }
            
            // Buscar la mejor ruta
            Ruta mejorRuta = encontrarMejorRuta(paquete);
            if (mejorRuta != null && esRutaValida(paquete, mejorRuta, cantidadProductos)) {
                solucionReparada.put(paquete, mejorRuta);
                actualizarCapacidadesVuelos(mejorRuta, cantidadProductos);
                incrementarOcupacionAlmacen(aeropuertoDestino, cantidadProductos);
                paquetesReinsertados++;
            } else {
                paquetesNoAsignados.add(paquete);
            }
        }
        
        System.out.println("Reparación Greedy: " + paquetesReinsertados + "/" + paquetesParaReparar.size() + 
                          " paquetes reinsertados");
        
        return new RepairResult(solucionReparada, paquetesNoAsignados);
    }
    
    /**
     * Reparación por Regret: Calcula el "arrepentimiento" de no insertar cada paquete
     * y prioriza aquellos con mayor diferencia entre mejor y segunda mejor opción.
     */
    public RepairResult regretRepair(
            Map<Paquete, Ruta> solucionParcial,
            List<Map.Entry<Paquete, Ruta>> paquetesDestruidos,
            int nivelRegret) {
        
        Map<Paquete, Ruta> solucionReparada = new HashMap<>(solucionParcial);
        List<Paquete> paquetesNoAsignados = new ArrayList<>();
        
        List<Paquete> paquetesRestantes = new ArrayList<>();
        for (Map.Entry<Paquete, Ruta> entrada : paquetesDestruidos) {
            paquetesRestantes.add(entrada.getKey());
        }
        
        int paquetesReinsertados = 0;
        
        // Mientras haya paquetes por insertar
        while (!paquetesRestantes.isEmpty()) {
            Paquete mejorPaquete = null;
            Ruta mejorRuta = null;
            double maxRegret = Double.NEGATIVE_INFINITY;
            
            // Calcular regret para cada paquete restante
            for (Paquete paquete : paquetesRestantes) {
                Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCodigo(paquete.getAeropuertoDestino());
                int cantidadProductos = 1;
                
                if (aeropuertoDestino == null || !tieneCapacidadAlmacen(aeropuertoDestino, cantidadProductos)) {
                    continue;
                }
                
                // Encontrar las mejores rutas para este paquete
                List<OpcionRuta> opcionesRuta = encontrarTodasOpcionesRuta(paquete);
                
                if (opcionesRuta.isEmpty()) {
                    continue;
                }
                
                // Ordenar por margen de tiempo (mejor primero)
                opcionesRuta.sort((r1, r2) -> Double.compare(r2.margenTiempo, r1.margenTiempo));
                
                // Calcular regret
                double regret = 0;
                if (opcionesRuta.size() >= 2) {
                    // Regret = diferencia entre mejor y segunda mejor opción
                    regret = opcionesRuta.get(0).margenTiempo - opcionesRuta.get(1).margenTiempo;
                } else if (opcionesRuta.size() == 1) {
                    // Solo una opción: regret basado en urgencia
                    LocalDateTime ahora = LocalDateTime.now();
                    long horasHastaDeadline = ChronoUnit.HOURS.between(ahora, paquete.getFechaLimiteEntrega());
                    regret = Math.max(0, 168 - horasHastaDeadline); // Más regret para deadlines más cercanos
                }
                
                // Añadir factor de urgencia al regret
                LocalDateTime ahora = LocalDateTime.now();
                long horasHastaDeadline = ChronoUnit.HOURS.between(ahora, paquete.getFechaLimiteEntrega());
                double factorUrgencia = Math.max(1, 72.0 / Math.max(1, horasHastaDeadline));
                regret *= factorUrgencia;
                
                if (regret > maxRegret) {
                    maxRegret = regret;
                    mejorPaquete = paquete;
                    mejorRuta = opcionesRuta.get(0).ruta;
                }
            }
            
            // Insertar el paquete con mayor regret
            if (mejorPaquete != null && mejorRuta != null) {
                solucionReparada.put(mejorPaquete, mejorRuta);
                int cantidadProductos = 1;
                actualizarCapacidadesVuelos(mejorRuta, cantidadProductos);
                incrementarOcupacionAlmacen(obtenerAeropuertoPorCodigo(mejorPaquete.getAeropuertoDestino()), cantidadProductos);
                paquetesRestantes.remove(mejorPaquete);
                paquetesReinsertados++;
            } else {
                // No se pudo insertar ningún paquete, agregar todos los restantes como no asignados
                paquetesNoAsignados.addAll(paquetesRestantes);
                break;
            }
        }
        
        System.out.println("Reparación por Regret: " + paquetesReinsertados + "/" + paquetesDestruidos.size() + 
                          " paquetes reinsertados");
        
        return new RepairResult(solucionReparada, paquetesNoAsignados);
    }
    
    /**
     * Reparación por tiempo: Prioriza paquetes con deadlines más cercanos.
     */
    public RepairResult timeBasedRepair(
            Map<Paquete, Ruta> solucionParcial,
            List<Map.Entry<Paquete, Ruta>> paquetesDestruidos) {
        
        Map<Paquete, Ruta> solucionReparada = new HashMap<>(solucionParcial);
        List<Paquete> paquetesNoAsignados = new ArrayList<>();
        
        // Extraer paquetes y ordenar por urgencia (deadline más cercano primero)
        List<Paquete> paquetesParaReparar = new ArrayList<>();
        for (Map.Entry<Paquete, Ruta> entrada : paquetesDestruidos) {
            paquetesParaReparar.add(entrada.getKey());
        }
        
        paquetesParaReparar.sort((p1, p2) -> {
            LocalDateTime ahora = LocalDateTime.now();
            long p1Horas = ChronoUnit.HOURS.between(ahora, p1.getFechaLimiteEntrega());
            long p2Horas = ChronoUnit.HOURS.between(ahora, p2.getFechaLimiteEntrega());
            return Long.compare(p1Horas, p2Horas);
        });
        
        int paquetesReinsertados = 0;
        
        // Insertar paquetes en orden de urgencia
        for (Paquete paquete : paquetesParaReparar) {
            Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCodigo(paquete.getAeropuertoDestino());
            int cantidadProductos = 1;
            
            if (aeropuertoDestino == null || !tieneCapacidadAlmacen(aeropuertoDestino, cantidadProductos)) {
                paquetesNoAsignados.add(paquete);
                continue;
            }
            
            // Buscar ruta con mayor margen de tiempo
            Ruta mejorRuta = encontrarRutaConMaximoMargen(paquete);
            if (mejorRuta != null && esRutaValida(paquete, mejorRuta, cantidadProductos)) {
                solucionReparada.put(paquete, mejorRuta);
                actualizarCapacidadesVuelos(mejorRuta, cantidadProductos);
                incrementarOcupacionAlmacen(aeropuertoDestino, cantidadProductos);
                paquetesReinsertados++;
            } else {
                paquetesNoAsignados.add(paquete);
            }
        }
        
        System.out.println("Reparación por tiempo: " + paquetesReinsertados + "/" + paquetesParaReparar.size() + 
                          " paquetes reinsertados");
        
        return new RepairResult(solucionReparada, paquetesNoAsignados);
    }
    
    /**
     * Reparación por capacidad: Prioriza rutas con mayor capacidad disponible.
     */
    public RepairResult capacityBasedRepair(
            Map<Paquete, Ruta> solucionParcial,
            List<Map.Entry<Paquete, Ruta>> paquetesDestruidos) {
        
        Map<Paquete, Ruta> solucionReparada = new HashMap<>(solucionParcial);
        List<Paquete> paquetesNoAsignados = new ArrayList<>();
        
        List<Paquete> paquetesParaReparar = new ArrayList<>();
        for (Map.Entry<Paquete, Ruta> entrada : paquetesDestruidos) {
            paquetesParaReparar.add(entrada.getKey());
        }
        
        // Ordenar por deadline como criterio secundario
        // Ordenamiento null-safe
        paquetesParaReparar.sort((p1, p2) -> {
            if (p1.getFechaLimiteEntrega() == null && p2.getFechaLimiteEntrega() == null) return 0;
            if (p1.getFechaLimiteEntrega() == null) return 1; // nulls al final
            if (p2.getFechaLimiteEntrega() == null) return -1; // nulls al final
            return p1.getFechaLimiteEntrega().compareTo(p2.getFechaLimiteEntrega());
        });
        
        int paquetesReinsertados = 0;
        
        for (Paquete paquete : paquetesParaReparar) {
            Aeropuerto aeropuertoDestino = obtenerAeropuertoPorCodigo(paquete.getAeropuertoDestino());
            int cantidadProductos = 1;
            
            if (aeropuertoDestino == null || !tieneCapacidadAlmacen(aeropuertoDestino, cantidadProductos)) {
                paquetesNoAsignados.add(paquete);
                continue;
            }
            
            // Buscar ruta con mayor capacidad disponible
            Ruta mejorRuta = encontrarRutaConMaximaCapacidad(paquete);
            if (mejorRuta != null && esRutaValida(paquete, mejorRuta, cantidadProductos)) {
                solucionReparada.put(paquete, mejorRuta);
                actualizarCapacidadesVuelos(mejorRuta, cantidadProductos);
                incrementarOcupacionAlmacen(aeropuertoDestino, cantidadProductos);
                paquetesReinsertados++;
            } else {
                paquetesNoAsignados.add(paquete);
            }
        }
        
        System.out.println("Reparación por capacidad: " + paquetesReinsertados + "/" + paquetesParaReparar.size() + 
                          " paquetes reinsertados");
        
        return new RepairResult(solucionReparada, paquetesNoAsignados);
    }
    
    // ================= MÉTODOS AUXILIARES =================
    
    private List<OpcionRuta> encontrarTodasOpcionesRuta(Paquete paquete) {
        List<OpcionRuta> opciones = new ArrayList<>();
        String origen = paquete.getAeropuertoActual();
        String destino = paquete.getAeropuertoDestino();
        
        if (origen.equals(destino)) {
            Ruta rutaDirecta = new Ruta();
            rutaDirecta.setId(1); // ID temporal
            rutaDirecta.setVuelos(new ArrayList<>());
            opciones.add(new OpcionRuta(rutaDirecta, Double.MAX_VALUE));
            return opciones;
        }
        
        // Buscar ruta directa
        Ruta rutaDirecta = buscarRutaDirecta(origen, destino);
        if (rutaDirecta != null && esRutaValida(paquete, rutaDirecta)) {
            double margen = calcularMargenTiempoRuta(paquete, rutaDirecta);
            opciones.add(new OpcionRuta(rutaDirecta, margen));
        }
        
        // Buscar rutas con una escala
        Ruta rutaConEscala = buscarRutaConEscala(origen, destino);
        if (rutaConEscala != null && esRutaValida(paquete, rutaConEscala)) {
            double margen = calcularMargenTiempoRuta(paquete, rutaConEscala);
            opciones.add(new OpcionRuta(rutaConEscala, margen));
        }
        
        // Buscar rutas con dos escalas
        Ruta rutaConDosEscalas = buscarRutaConDosEscalas(origen, destino);
        if (rutaConDosEscalas != null && esRutaValida(paquete, rutaConDosEscalas)) {
            double margen = calcularMargenTiempoRuta(paquete, rutaConDosEscalas);
            opciones.add(new OpcionRuta(rutaConDosEscalas, margen));
        }
        
        return opciones;
    }
    
    private Ruta encontrarMejorRuta(Paquete paquete) {
        List<OpcionRuta> opciones = encontrarTodasOpcionesRuta(paquete);
        if (opciones.isEmpty()) return null;
        
        // Seleccionar la ruta con mayor margen de tiempo
        opciones.sort((r1, r2) -> Double.compare(r2.margenTiempo, r1.margenTiempo));
        return opciones.get(0).ruta;
    }
    
    private Ruta encontrarRutaConMaximoMargen(Paquete paquete) {
        return encontrarMejorRuta(paquete); // Ya implementado arriba
    }
    
    private Ruta encontrarRutaConMaximaCapacidad(Paquete paquete) {
        List<OpcionRuta> opciones = encontrarTodasOpcionesRuta(paquete);
        if (opciones.isEmpty()) return null;
        
        // Calcular capacidad disponible para cada ruta
        List<OpcionCapacidadRuta> opcionesCapacidad = new ArrayList<>();
        for (OpcionRuta opcion : opciones) {
            double capacidadTotal = 0;
            double capacidadUsada = 0;
            
            for (Vuelo vuelo : opcion.ruta.getVuelos()) {
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
     * Calcula margen sin doble conteo y usando fechaCreacion
     */
    private double calcularMargenTiempoRuta(Paquete paquete, Ruta ruta) {
        if (ruta == null || ruta.getVuelos().isEmpty()) {
            return 0.0;
        }
        
        // Sumar solo tiempo de vuelos + conexiones (sin extras por continente)
        double tiempoTotal = 0.0;
        for (Vuelo vuelo : ruta.getVuelos()) {
            tiempoTotal += vuelo.getTiempoTransporte();
        }
        
        // Agregar tiempo de conexión (2 horas por conexión)
        tiempoTotal += (ruta.getVuelos().size() - 1) * 2.0;
        
        // Usar fechaCreacion vs deadline, null-safe
        if (paquete.getFechaCreacion() == null || paquete.getFechaLimiteEntrega() == null) {
            return 1.0;
        }
        
        // Calcular presupuesto de horas desde fechaCreacion
        long presupuesto = ChronoUnit.HOURS.between(paquete.getFechaCreacion(), paquete.getFechaLimiteEntrega());
        if (presupuesto < 0) presupuesto = 0; // Clamp negativo
        
        double margen = presupuesto - tiempoTotal;
        return Math.max(margen, 0.0) + 1.0;
    }
    
    /**
     * Helper para validar capacidad de ruta con cantidad específica
     */
    private boolean cabeEnCapacidadRuta(Ruta ruta, int cantidad) {
        if (ruta == null) return false;
        for (Vuelo vuelo : ruta.getVuelos()) {
            if (vuelo.getCapacidadUsada() + cantidad > vuelo.getCapacidadMaxima()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Versión con cantidad específica de productos
     */
    private boolean esRutaValida(Paquete paquete, Ruta ruta, int cantidad) {
        if (ruta == null || ruta.getVuelos().isEmpty()) {
            return paquete.getAeropuertoActual().equals(paquete.getAeropuertoDestino());
        }
        
        // Verificar capacidad de vuelos con cantidad específica
        if (!cabeEnCapacidadRuta(ruta, cantidad)) {
            return false;
        }
        
        // Verificar continuidad de ruta
        String ubicacionActual = paquete.getAeropuertoActual();
        for (Vuelo vuelo : ruta.getVuelos()) {
            if (!vuelo.getAeropuertoOrigen().getCodigoIATA().equals(ubicacionActual)) {
                return false;
            }
            ubicacionActual = vuelo.getAeropuertoDestino().getCodigoIATA();
        }
        
        if (!ubicacionActual.equals(paquete.getAeropuertoDestino())) {
            return false;
        }
        
        // Verificar deadline
        return respetaDeadline(paquete, ruta);
    }
    
    /**
     * Versión original que delega calculando cantidad
     */
    private boolean esRutaValida(Paquete paquete, Ruta ruta) {
        int cantidad = 1; // En nuestro modelo, cada paquete = 1 producto
        return esRutaValida(paquete, ruta, cantidad);
    }
    
    /**
     * Verifica deadline sin doble conteo y null-safe
     */
    private boolean respetaDeadline(Paquete paquete, Ruta ruta) {
        if (ruta == null || ruta.getVuelos().isEmpty()) {
            return false;
        }
        
        // Null-safe
        if (paquete.getFechaCreacion() == null || paquete.getFechaLimiteEntrega() == null) {
            return false;
        }
        
        // Sumar solo tiempo de vuelos + conexiones
        double tiempoTotal = 0.0;
        for (Vuelo vuelo : ruta.getVuelos()) {
            tiempoTotal += vuelo.getTiempoTransporte();
        }
        
        // Agregar tiempo de conexión
        tiempoTotal += (ruta.getVuelos().size() - 1) * 2.0;
        
        // Calcular tiempo disponible desde fechaCreacion
        long horasHastaDeadline = ChronoUnit.HOURS.between(paquete.getFechaCreacion(), paquete.getFechaLimiteEntrega());
        
        return horasHastaDeadline >= tiempoTotal;
    }
    
    // Métodos de búsqueda de rutas
    private Ruta buscarRutaDirecta(String origen, String destino) {
        if (origen.equals(destino)) {
            Ruta ruta = new Ruta();
            ruta.setId(1); // ID temporal
            ruta.setVuelos(new ArrayList<>());
            return ruta;
        }
        
        // Buscar vuelo directo
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen().getCodigoIATA().equals(origen) && 
                vuelo.getAeropuertoDestino().getCodigoIATA().equals(destino) &&
                vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                
                Ruta ruta = new Ruta();
                ruta.setId(1); // ID temporal
                ruta.setVuelos(new ArrayList<>());
                ruta.getVuelos().add(vuelo);
                ruta.setTiempoTotal(vuelo.getTiempoTransporte());
                ruta.setCostoTotal(vuelo.getCosto());
                return ruta;
            }
        }
        return null;
    }
    
    private Ruta buscarRutaConEscala(String origen, String destino) {
        // Buscar escala intermedia
        for (Aeropuerto aeropuertoIntermedio : aeropuertos) {
            String intermedio = aeropuertoIntermedio.getCodigoIATA();
            if (intermedio.equals(origen) || intermedio.equals(destino)) continue;
            
            // Buscar primer vuelo: origen -> intermedio
            Vuelo primerVuelo = null;
            for (Vuelo vuelo : vuelos) {
                if (vuelo.getAeropuertoOrigen().getCodigoIATA().equals(origen) && 
                    vuelo.getAeropuertoDestino().getCodigoIATA().equals(intermedio) &&
                    vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                    primerVuelo = vuelo;
                    break;
                }
            }
            
            if (primerVuelo == null) continue;
            
            // Buscar segundo vuelo: intermedio -> destino
            Vuelo segundoVuelo = null;
            for (Vuelo vuelo : vuelos) {
                if (vuelo.getAeropuertoOrigen().getCodigoIATA().equals(intermedio) && 
                    vuelo.getAeropuertoDestino().getCodigoIATA().equals(destino) &&
                    vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                    segundoVuelo = vuelo;
                    break;
                }
            }
            
            if (segundoVuelo != null) {
                Ruta ruta = new Ruta();
                ruta.setId(2); // ID temporal
                ruta.setVuelos(new ArrayList<>());
                ruta.getVuelos().add(primerVuelo);
                ruta.getVuelos().add(segundoVuelo);
                ruta.setTiempoTotal(primerVuelo.getTiempoTransporte() + segundoVuelo.getTiempoTransporte());
                ruta.setCostoTotal(primerVuelo.getCosto() + segundoVuelo.getCosto());
                return ruta;
            }
        }
        
        return null;
    }
    
    private Ruta buscarRutaConDosEscalas(String origen, String destino) {
        // Simplificado: buscar solo algunas combinaciones aleatorias para eficiencia
        List<Aeropuerto> candidatos = new ArrayList<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (!aeropuerto.getCodigoIATA().equals(origen) && !aeropuerto.getCodigoIATA().equals(destino)) {
                candidatos.add(aeropuerto);
            }
        }
        
        if (candidatos.size() < 2) return null;
        
        Collections.shuffle(candidatos, generadorAleatorio);
        int maxIntentos = Math.min(10, candidatos.size() - 1);
        
        for (int i = 0; i < maxIntentos; i++) {
            Aeropuerto primero = candidatos.get(i);
            for (int j = i + 1; j < Math.min(i + 5, candidatos.size()); j++) {
                Aeropuerto segundo = candidatos.get(j);
                
                Ruta ruta = intentarRutaConDosEscalas(origen, primero.getCodigoIATA(), segundo.getCodigoIATA(), destino);
                if (ruta != null) return ruta;
                
                // También probar en orden inverso
                ruta = intentarRutaConDosEscalas(origen, segundo.getCodigoIATA(), primero.getCodigoIATA(), destino);
                if (ruta != null) return ruta;
            }
        }
        
        return null;
    }
    
    private Ruta intentarRutaConDosEscalas(String origen, String primero, String segundo, String destino) {
        Vuelo vuelo1 = null, vuelo2 = null, vuelo3 = null;
        
        // Buscar vuelo 1: origen -> primero
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen().getCodigoIATA().equals(origen) && 
                vuelo.getAeropuertoDestino().getCodigoIATA().equals(primero) &&
                vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                vuelo1 = vuelo;
                break;
            }
        }
        
        if (vuelo1 == null) return null;
        
        // Buscar vuelo 2: primero -> segundo
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen().getCodigoIATA().equals(primero) && 
                vuelo.getAeropuertoDestino().getCodigoIATA().equals(segundo) &&
                vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                vuelo2 = vuelo;
                break;
            }
        }
        
        if (vuelo2 == null) return null;
        
        // Buscar vuelo 3: segundo -> destino
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen().getCodigoIATA().equals(segundo) && 
                vuelo.getAeropuertoDestino().getCodigoIATA().equals(destino) &&
                vuelo.getCapacidadUsada() < vuelo.getCapacidadMaxima()) {
                vuelo3 = vuelo;
                break;
            }
        }
        
        if (vuelo3 != null) {
            Ruta ruta = new Ruta();
            ruta.setId(3); // ID temporal
            ruta.setVuelos(new ArrayList<>());
            ruta.getVuelos().add(vuelo1);
            ruta.getVuelos().add(vuelo2);
            ruta.getVuelos().add(vuelo3);
            ruta.setTiempoTotal(vuelo1.getTiempoTransporte() + vuelo2.getTiempoTransporte() + vuelo3.getTiempoTransporte());
            ruta.setCostoTotal(vuelo1.getCosto() + vuelo2.getCosto() + vuelo3.getCosto());
            return ruta;
        }
        
        return null;
    }
    
    /**
     * Obtiene un aeropuerto por su código IATA
     */
    private Aeropuerto obtenerAeropuertoPorCodigo(String codigoIATA) {
        return aeropuertos.stream()
            .filter(a -> a.getCodigoIATA().equals(codigoIATA))
            .findFirst()
            .orElse(null);
    }
    
    private boolean tieneCapacidadAlmacen(Aeropuerto aeropuertoDestino, int cantidadProductos) {
        if (aeropuertoDestino == null) {
            return false;
        }
        
        int ocupacionActual = ocupacionAlmacenes.getOrDefault(aeropuertoDestino.getCodigoIATA(), 0);
        return (ocupacionActual + cantidadProductos) <= aeropuertoDestino.getCapacidadAlmacen();
    }
    
    private void actualizarCapacidadesVuelos(Ruta ruta, int cantidadProductos) {
        for (Vuelo vuelo : ruta.getVuelos()) {
            vuelo.setCapacidadUsada(vuelo.getCapacidadUsada() + cantidadProductos);
        }
    }
    
    private void incrementarOcupacionAlmacen(Aeropuerto aeropuerto, int cantidadProductos) {
        if (aeropuerto != null) {
            int actual = ocupacionAlmacenes.getOrDefault(aeropuerto.getCodigoIATA(), 0);
            ocupacionAlmacenes.put(aeropuerto.getCodigoIATA(), actual + cantidadProductos);
        }
    }
    
    // ================= CLASES AUXILIARES =================
    
    private static class OpcionRuta {
        Ruta ruta;
        double margenTiempo;
        
        OpcionRuta(Ruta ruta, double margenTiempo) {
            this.ruta = ruta;
            this.margenTiempo = margenTiempo;
        }
    }
    
    private static class OpcionCapacidadRuta {
        Ruta ruta;
        double capacidadDisponible;
        double margenTiempo;
        
        OpcionCapacidadRuta(Ruta ruta, double capacidadDisponible, double margenTiempo) {
            this.ruta = ruta;
            this.capacidadDisponible = capacidadDisponible;
            this.margenTiempo = margenTiempo;
        }
    }
    
    /**
     * Clase para encapsular el resultado de una operación de reparación
     */
    public static class RepairResult {
        private Map<Paquete, Ruta> solucionReparada;
        private List<Paquete> paquetesNoAsignados;
        
        public RepairResult(Map<Paquete, Ruta> solucionReparada,
                           List<Paquete> paquetesNoAsignados) {
            this.solucionReparada = solucionReparada;
            this.paquetesNoAsignados = paquetesNoAsignados;
        }
        
        public Map<Paquete, Ruta> getRepairedSolution() {
            return solucionReparada;
        }
        
        public List<Paquete> getUnassignedPackages() {
            return paquetesNoAsignados;
        }
        
        public int getNumRepairedPackages() {
            return solucionReparada.size();
        }
        
        public boolean isSuccess() {
            return !solucionReparada.isEmpty() || paquetesNoAsignados.isEmpty();
        }
        
        public int getNumUnassignedPackages() {
            return paquetesNoAsignados.size();
        }
    }
}