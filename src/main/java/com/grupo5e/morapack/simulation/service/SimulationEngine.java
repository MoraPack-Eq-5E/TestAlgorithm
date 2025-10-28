package com.grupo5e.morapack.simulation.service;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.repository.SimulacionAsignacionRepository;
import com.grupo5e.morapack.repository.SimulacionSemanalRepository;
import com.grupo5e.morapack.service.AeropuertoService;
import com.grupo5e.morapack.simulation.model.*;
import com.grupo5e.morapack.utils.CoordenadasUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Motor de simulación en tiempo real.
 * 
 * Mantiene el estado de simulaciones activas en memoria y calcula
 * posiciones de vuelos mediante interpolación en cada request del frontend.
 * 
 * Este servicio es thread-safe y soporta múltiples simulaciones simultáneas.
 */
@Service
@Slf4j
public class SimulationEngine {
    
    /**
     * Simulaciones activas en memoria
     * Key: simulationId, Value: SimulationState
     */
    private final ConcurrentHashMap<Long, SimulationState> activeSimulations = new ConcurrentHashMap<>();
    
    /**
     * Cache de coordenadas para acceso rápido
     * Key: códigoIATA, Value: [longitud, latitud]
     */
    private final Map<String, double[]> coordinatesCache = new HashMap<>();
    
    private final SimulacionSemanalRepository simulacionRepository;
    private final SimulacionAsignacionRepository asignacionRepository;
    private final AeropuertoService aeropuertoService;
    
    /**
     * Factor de aceleración por defecto: 112x
     * Simula 1 semana (604,800 seg) en 90 minutos (5,400 seg)
     */
    private static final int DEFAULT_TIME_SCALE = 112;
    
    /**
     * Aeropuertos principales de MoraPack
     */
    private static final Set<String> MAIN_AIRPORTS = Set.of("SPIM", "UBBB", "EBCI");
    
    public SimulationEngine(
            SimulacionSemanalRepository simulacionRepository,
            SimulacionAsignacionRepository asignacionRepository,
            AeropuertoService aeropuertoService) {
        this.simulacionRepository = simulacionRepository;
        this.asignacionRepository = asignacionRepository;
        this.aeropuertoService = aeropuertoService;
        
        // Inicializar cache de coordenadas al arrancar
        initializeCoordinatesCache();
    }
    
    /**
     * Carga todas las coordenadas de aeropuertos en memoria
     * para acceso rápido durante interpolación
     */
    private void initializeCoordinatesCache() {
        log.info("🗺️ Inicializando cache de coordenadas de aeropuertos...");
        
        List<Aeropuerto> aeropuertos = aeropuertoService.listar();
        
        for (Aeropuerto aeropuerto : aeropuertos) {
            double lat = CoordenadasUtils.parsearCoordenada(aeropuerto.getLatitud());
            double lon = CoordenadasUtils.parsearCoordenada(aeropuerto.getLongitud());
            coordinatesCache.put(aeropuerto.getCodigoIATA(), new double[]{lon, lat});
        }
        
        log.info("✅ Cache inicializado con {} aeropuertos", coordinatesCache.size());
    }
    
    /**
     * Inicia una nueva simulación cargando datos desde BD a memoria
     * 
     * @param simulacionId ID de la simulación (debe existir en BD con asignaciones)
     * @param timeScale Factor de aceleración (default: 112)
     * @return SimulationState creado
     */
    public SimulationState startSimulation(Long simulacionId, Integer timeScale) {
        log.info("🚀 Iniciando simulación en tiempo real para simulationId={}", simulacionId);
        
        // Verificar que la simulación existe
        SimulacionSemanal simulacion = simulacionRepository.findById(simulacionId)
                .orElseThrow(() -> new RuntimeException("Simulación no encontrada: " + simulacionId));
        
        // Cargar asignaciones desde BD
        List<SimulacionAsignacion> asignaciones = asignacionRepository.findBySimulacion(simulacion);
        
        if (asignaciones.isEmpty()) {
            throw new RuntimeException("La simulación no tiene asignaciones (solución vacía)");
        }
        
        // Crear estado de simulación
        SimulationState state = SimulationState.builder()
                .simulationId(simulacionId)
                .realStartTimeMillis(System.currentTimeMillis())
                .simulatedStartTime(simulacion.getTiempoInicialReferencia())
                .timeScale(timeScale != null ? timeScale : DEFAULT_TIME_SCALE)
                .simulationDurationDays(simulacion.getTiempoSimuladoDias() != null ? simulacion.getTiempoSimuladoDias() : 7)
                .status(SimulationStatus.RUNNING)
                .simulacionEntity(simulacion)
                .accumulatedSimulatedMillis(0)
                .build();
        
        // Cargar vuelos en memoria
        state.setFlights(buildFlightSnapshots(asignaciones));
        
        // Cargar almacenes en memoria
        state.setWarehouses(buildWarehouseSnapshots());
        
        // Calcular métricas iniciales
        updateMetrics(state);
        
        // Guardar en memoria
        activeSimulations.put(simulacionId, state);
        
        // Evento de inicio
        state.addEvent(SimulationEvent.builder()
                .id(UUID.randomUUID().toString())
                .type(EventType.INFO)
                .message("Simulación iniciada")
                .simulatedTime(state.getSimulatedStartTime())
                .realTime(LocalDateTime.now())
                .build());
        
        log.info("✅ Simulación {} cargada en memoria con {} vuelos y {} almacenes", 
                simulacionId, state.getFlights().size(), state.getWarehouses().size());
        
        return state;
    }
    
    /**
     * Construye snapshots de vuelos desde las asignaciones de la BD
     */
    private List<FlightSnapshot> buildFlightSnapshots(List<SimulacionAsignacion> asignaciones) {
        // Agrupar asignaciones por vuelo
        Map<Integer, List<SimulacionAsignacion>> byFlight = asignaciones.stream()
                .collect(Collectors.groupingBy(a -> a.getVuelo().getId()));
        
        List<FlightSnapshot> snapshots = new ArrayList<>();
        
        for (Map.Entry<Integer, List<SimulacionAsignacion>> entry : byFlight.entrySet()) {
            Integer vueloId = entry.getKey();
            List<SimulacionAsignacion> vueloAsignaciones = entry.getValue();
            
            // Tomar la primera asignación para datos del vuelo
            SimulacionAsignacion first = vueloAsignaciones.get(0);
            Vuelo vuelo = first.getVuelo();
            Aeropuerto origen = vuelo.getAeropuertoOrigen();
            Aeropuerto destino = vuelo.getAeropuertoDestino();
            
            // Extraer pedidos en este vuelo (distinct por ID)
            List<Pedido> pedidosEnVuelo = vueloAsignaciones.stream()
                    .map(SimulacionAsignacion::getPedido)
                    .distinct()
                    .collect(Collectors.toList());
            
            // Extraer IDs de pedidos para el DTO
            List<Long> packagesOnBoard = pedidosEnVuelo.stream()
                    .map(Pedido::getId)
                    .collect(Collectors.toList());
            
            // ✅ CALCULAR CAPACIDAD DINÁMICA (suma de productos de todos los pedidos a bordo)
            int capacidadUsadaDinamica = pedidosEnVuelo.stream()
                    .mapToInt(p -> p.getProductos() != null ? p.getProductos().size() : 1)
                    .sum();
            
            int capacidadMaxima = vuelo.getCapacidadMaxima();
            
            // Coordenadas
            double originLat = first.getLatitudInicio();
            double originLng = first.getLongitudInicio();
            double destLat = first.getLatitudFin();
            double destLng = first.getLongitudFin();
            
            // Tiempos (usar el rango completo del vuelo)
            Integer minutoInicio = vueloAsignaciones.stream()
                    .map(SimulacionAsignacion::getMinutoInicio)
                    .min(Integer::compare)
                    .orElse(0);
            
            Integer minutoFin = vueloAsignaciones.stream()
                    .map(SimulacionAsignacion::getMinutoFin)
                    .max(Integer::compare)
                    .orElse(0);
            
            // Obtener T0 de la primera simulación
            LocalDateTime t0 = first.getSimulacion().getTiempoInicialReferencia();
            
            LocalDateTime departureTime = t0.plusMinutes(minutoInicio);
            LocalDateTime arrivalTime = t0.plusMinutes(minutoFin);
            
            FlightSnapshot snapshot = FlightSnapshot.builder()
                    .flightId(vueloId)
                    .flightCode("MP-" + vueloId)
                    .route(new double[][]{{originLng, originLat}, {destLng, destLat}})
                    .originLat(originLat)
                    .originLng(originLng)
                    .destinationLat(destLat)
                    .destinationLng(destLng)
                    .currentLat(originLat)  // Inicialmente en origen
                    .currentLng(originLng)
                    .departureTime(departureTime)
                    .arrivalTime(arrivalTime)
                    .status(FlightStatus.SCHEDULED)
                    .progress(0.0)
                    .progressPercentage(0.0)
                    .packagesOnBoard(packagesOnBoard)
                    .capacityUsed(capacidadUsadaDinamica)  // ✅ Calculada dinámicamente
                    .capacityMax(capacidadMaxima)
                    .occupancyPercentage(capacidadMaxima > 0 ? 
                            (capacidadUsadaDinamica * 100.0) / capacidadMaxima : 0)
                    .originCode(origen.getCodigoIATA())
                    .destinationCode(destino.getCodigoIATA())
                    .originCity(origen.getCiudad() != null ? origen.getCiudad().getNombre() : "")
                    .destinationCity(destino.getCiudad() != null ? destino.getCiudad().getNombre() : "")
                    .durationMinutes(minutoFin - minutoInicio)
                    .build();
            
            snapshots.add(snapshot);
        }
        
        return snapshots;
    }
    
    /**
     * Construye snapshots de almacenes/aeropuertos
     */
    private List<WarehouseSnapshot> buildWarehouseSnapshots() {
        List<Aeropuerto> aeropuertos = aeropuertoService.listar();
        
        return aeropuertos.stream()
                .map(this::buildWarehouseSnapshot)
                .collect(Collectors.toList());
    }
    
    private WarehouseSnapshot buildWarehouseSnapshot(Aeropuerto aeropuerto) {
        int capacity = aeropuerto.getCapacidadMaxima();
        int current = aeropuerto.getCapacidadActual();
        int available = capacity - current;
        double occupancyPct = capacity > 0 ? (current * 100.0) / capacity : 0;
        
        WarehouseStatus status = calculateWarehouseStatus(occupancyPct);
        
        return WarehouseSnapshot.builder()
                .warehouseId(aeropuerto.getId())
                .code(aeropuerto.getCodigoIATA())
                .cityName(aeropuerto.getCiudad() != null ? aeropuerto.getCiudad().getNombre() : "")
                .latitude(CoordenadasUtils.parsearCoordenada(aeropuerto.getLatitud()))
                .longitude(CoordenadasUtils.parsearCoordenada(aeropuerto.getLongitud()))
                .capacity(capacity)
                .currentOccupancy(current)
                .available(available)
                .occupancyPercentage(Math.round(occupancyPct * 100.0) / 100.0)
                .status(status)
                .packagesInWarehouse(current)
                .packagesInTransit(0)  // Se calculará dinámicamente
                .packagesAtDestination(0)  // Se calculará dinámicamente
                .isPrincipal(MAIN_AIRPORTS.contains(aeropuerto.getCodigoIATA()))
                .build();
    }
    
    private WarehouseStatus calculateWarehouseStatus(double occupancyPct) {
        if (occupancyPct >= 100) return WarehouseStatus.FULL;
        if (occupancyPct >= 90) return WarehouseStatus.CRITICAL;
        if (occupancyPct >= 70) return WarehouseStatus.WARNING;
        return WarehouseStatus.NORMAL;
    }
    
    /**
     * Actualiza el estado de una simulación activa
     * Calcula posiciones actuales de todos los vuelos mediante interpolación
     * 
     * @param simulationId ID de la simulación
     * @return SimulationState actualizado
     */
    public SimulationState updateSimulation(Long simulationId) {
        SimulationState state = activeSimulations.get(simulationId);
        
        if (state == null) {
            throw new RuntimeException("Simulación no activa en memoria: " + simulationId);
        }
        
        // Verificar si completó
        if (state.isCompleted() && state.getStatus() != SimulationStatus.COMPLETED) {
            state.setStatus(SimulationStatus.COMPLETED);
            state.addEvent(SimulationEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .type(EventType.INFO)
                    .message("Simulación completada")
                    .simulatedTime(state.getCurrentSimulatedTime())
                    .realTime(LocalDateTime.now())
                    .build());
            log.info("✅ Simulación {} completada", simulationId);
        }
        
        // No actualizar si está pausada o completada
        if (state.getStatus() == SimulationStatus.PAUSED || 
            state.getStatus() == SimulationStatus.STOPPED ||
            state.getStatus() == SimulationStatus.COMPLETED) {
            return state;
        }
        
        LocalDateTime currentSimulatedTime = state.getCurrentSimulatedTime();
        
        // Actualizar cada vuelo
        List<SimulationEvent> newEvents = new ArrayList<>();
        
        for (FlightSnapshot flight : state.getFlights()) {
            FlightStatus oldStatus = flight.getStatus();
            updateFlightPosition(flight, currentSimulatedTime);
            FlightStatus newStatus = flight.getStatus();
            
            // Generar eventos de cambio de estado
            if (oldStatus != newStatus) {
                if (newStatus == FlightStatus.IN_FLIGHT) {
                    newEvents.add(SimulationEvent.builder()
                            .id(UUID.randomUUID().toString())
                            .type(EventType.FLIGHT_DEPARTURE)
                            .message(String.format("Vuelo %s despegó de %s", flight.getFlightCode(), flight.getOriginCity()))
                            .simulatedTime(currentSimulatedTime)
                            .realTime(LocalDateTime.now())
                            .relatedFlightId(flight.getFlightId())
                            .relatedAirportCode(flight.getOriginCode())
                            .build());
                } else if (newStatus == FlightStatus.LANDED) {
                    newEvents.add(SimulationEvent.builder()
                            .id(UUID.randomUUID().toString())
                            .type(EventType.FLIGHT_ARRIVAL)
                            .message(String.format("Vuelo %s aterrizó en %s", flight.getFlightCode(), flight.getDestinationCity()))
                            .simulatedTime(currentSimulatedTime)
                            .realTime(LocalDateTime.now())
                            .relatedFlightId(flight.getFlightId())
                            .relatedAirportCode(flight.getDestinationCode())
                            .build());
                }
            }
        }
        
        // Agregar eventos generados
        newEvents.forEach(state::addEvent);
        
        // Actualizar métricas
        updateMetrics(state);
        
        return state;
    }
    
    /**
     * Calcula la posición actual de un vuelo mediante interpolación lineal
     */
    private void updateFlightPosition(FlightSnapshot flight, LocalDateTime currentTime) {
        LocalDateTime departure = flight.getDepartureTime();
        LocalDateTime arrival = flight.getArrivalTime();
        
        // Determinar estado
        if (currentTime.isBefore(departure)) {
            // Aún no despega
            flight.setStatus(FlightStatus.SCHEDULED);
            flight.setProgress(0.0);
            flight.setProgressPercentage(0.0);
            flight.setCurrentLat(flight.getOriginLat());
            flight.setCurrentLng(flight.getOriginLng());
            
        } else if (currentTime.isAfter(arrival)) {
            // Ya aterrizó
            flight.setStatus(FlightStatus.LANDED);
            flight.setProgress(1.0);
            flight.setProgressPercentage(100.0);
            flight.setCurrentLat(flight.getDestinationLat());
            flight.setCurrentLng(flight.getDestinationLng());
            
        } else {
            // En vuelo - calcular progreso
            flight.setStatus(FlightStatus.IN_FLIGHT);
            
            long totalDurationMillis = ChronoUnit.MILLIS.between(departure, arrival);
            long elapsedMillis = ChronoUnit.MILLIS.between(departure, currentTime);
            
            double progress = totalDurationMillis > 0 ? 
                    (double) elapsedMillis / totalDurationMillis : 0.0;
            
            progress = Math.max(0.0, Math.min(1.0, progress));  // Clamp 0-1
            
            flight.setProgress(progress);
            flight.setProgressPercentage(Math.round(progress * 10000.0) / 100.0);  // 2 decimales
            
            // Interpolación lineal de posición
            double currentLat = CoordenadasUtils.interpolar(
                    flight.getOriginLat(), 
                    flight.getDestinationLat(), 
                    progress
            );
            
            double currentLng = CoordenadasUtils.interpolar(
                    flight.getOriginLng(), 
                    flight.getDestinationLng(), 
                    progress
            );
            
            flight.setCurrentLat(currentLat);
            flight.setCurrentLng(currentLng);
        }
    }
    
    /**
     * Actualiza métricas de la simulación
     */
    private void updateMetrics(SimulationState state) {
        SimulationMetrics metrics = state.getMetrics();
        
        List<FlightSnapshot> flights = state.getFlights();
        
        metrics.setTotalFlights(flights.size());
        metrics.setFlightsScheduled((int) flights.stream()
                .filter(f -> f.getStatus() == FlightStatus.SCHEDULED).count());
        metrics.setFlightsInAir((int) flights.stream()
                .filter(f -> f.getStatus() == FlightStatus.IN_FLIGHT).count());
        metrics.setFlightsCompleted((int) flights.stream()
                .filter(f -> f.getStatus() == FlightStatus.LANDED).count());
        
        // TODO: Calcular métricas de pedidos cuando sea necesario
        metrics.setTotalOrders(0);
        metrics.setOrdersDelivered(0);
        metrics.setOrdersInTransit(0);
        metrics.setOrdersWaiting(0);
        
        // Ocupación promedio de almacenes
        double avgOccupancy = state.getWarehouses().stream()
                .mapToDouble(WarehouseSnapshot::getOccupancyPercentage)
                .average()
                .orElse(0.0);
        
        metrics.setAverageWarehouseOccupancy(Math.round(avgOccupancy * 100.0) / 100.0);
    }
    
    /**
     * Obtiene el estado actual de una simulación
     */
    public SimulationState getSimulation(Long simulationId) {
        return activeSimulations.get(simulationId);
    }
    
    /**
     * Pausa una simulación
     */
    public void pauseSimulation(Long simulationId) {
        SimulationState state = activeSimulations.get(simulationId);
        if (state != null && state.getStatus() == SimulationStatus.RUNNING) {
            state.setPausedAtMillis(System.currentTimeMillis());
            state.setAccumulatedSimulatedMillis(state.calculateElapsedSimulatedMillis());
            state.setStatus(SimulationStatus.PAUSED);
            
            state.addEvent(SimulationEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .type(EventType.INFO)
                    .message("Simulación pausada")
                    .simulatedTime(state.getCurrentSimulatedTime())
                    .realTime(LocalDateTime.now())
                    .build());
            
            log.info("⏸️ Simulación {} pausada", simulationId);
        }
    }
    
    /**
     * Reanuda una simulación pausada
     */
    public void resumeSimulation(Long simulationId) {
        SimulationState state = activeSimulations.get(simulationId);
        if (state != null && state.getStatus() == SimulationStatus.PAUSED) {
            // Ajustar tiempo de inicio para mantener el tiempo simulado acumulado
            state.setRealStartTimeMillis(System.currentTimeMillis());
            state.setPausedAtMillis(null);
            state.setStatus(SimulationStatus.RUNNING);
            
            state.addEvent(SimulationEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .type(EventType.INFO)
                    .message("Simulación reanudada")
                    .simulatedTime(state.getCurrentSimulatedTime())
                    .realTime(LocalDateTime.now())
                    .build());
            
            log.info("▶️ Simulación {} reanudada", simulationId);
        }
    }
    
    /**
     * Detiene una simulación (no se puede reanudar)
     */
    public void stopSimulation(Long simulationId) {
        SimulationState state = activeSimulations.get(simulationId);
        if (state != null) {
            state.setStatus(SimulationStatus.STOPPED);
            
            state.addEvent(SimulationEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .type(EventType.INFO)
                    .message("Simulación detenida")
                    .simulatedTime(state.getCurrentSimulatedTime())
                    .realTime(LocalDateTime.now())
                    .build());
            
            log.info("⏹️ Simulación {} detenida", simulationId);
        }
    }
    
    /**
     * Cambia la velocidad de una simulación en ejecución
     */
    public void setSimulationSpeed(Long simulationId, int newTimeScale) {
        SimulationState state = activeSimulations.get(simulationId);
        if (state != null) {
            // Guardar tiempo simulado acumulado hasta ahora
            long elapsed = state.calculateElapsedSimulatedMillis();
            state.setAccumulatedSimulatedMillis(elapsed);
            
            // Resetear punto de inicio y cambiar escala
            state.setRealStartTimeMillis(System.currentTimeMillis());
            state.setTimeScale(newTimeScale);
            
            log.info("⚡ Velocidad de simulación {} cambiada a {}x", simulationId, newTimeScale);
        }
    }
    
    /**
     * Elimina una simulación de memoria
     */
    public void removeSimulation(Long simulationId) {
        SimulationState removed = activeSimulations.remove(simulationId);
        if (removed != null) {
            log.info("🗑️ Simulación {} eliminada de memoria", simulationId);
        }
    }
    
    /**
     * Obtiene todas las simulaciones activas
     */
    public Map<Long, SimulationState> getAllActiveSimulations() {
        return new HashMap<>(activeSimulations);
    }
    
    /**
     * Limpia simulaciones completadas o detenidas hace más de X tiempo
     */
    public void cleanupOldSimulations(long maxAgeMillis) {
        long now = System.currentTimeMillis();
        List<Long> toRemove = new ArrayList<>();
        
        activeSimulations.forEach((id, state) -> {
            if (state.getStatus() == SimulationStatus.COMPLETED || 
                state.getStatus() == SimulationStatus.STOPPED) {
                
                long age = now - state.getRealStartTimeMillis();
                if (age > maxAgeMillis) {
                    toRemove.add(id);
                }
            }
        });
        
        toRemove.forEach(this::removeSimulation);
        
        if (!toRemove.isEmpty()) {
            log.info("🧹 Limpiadas {} simulaciones antiguas", toRemove.size());
        }
    }
}

