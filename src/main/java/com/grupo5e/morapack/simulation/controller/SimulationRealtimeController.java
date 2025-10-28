package com.grupo5e.morapack.simulation.controller;

import com.grupo5e.morapack.simulation.dto.*;
import com.grupo5e.morapack.simulation.model.*;
import com.grupo5e.morapack.simulation.service.SimulationEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller para visualización en tiempo real de simulaciones.
 * 
 * Flujo recomendado:
 * 1. Frontend inicia simulación ALNS con POST /api/simulacion/semanal/iniciar
 * 2. Backend ejecuta algoritmo ALNS (puede tardar minutos)
 * 3. Cuando ALNS termina, frontend llama POST /api/simulations/{id}/visualization/start
 * 4. Esto carga la solución en memoria y prepara la visualización
 * 5. Frontend hace polling a GET /api/simulations/{id}/status cada 2 segundos
 * 6. Frontend puede controlar con PATCH /api/simulations/{id}/control
 */
@RestController
@RequestMapping("/api/simulations")
@Tag(name = "Simulación en Tiempo Real", description = "Endpoints para visualización y control de simulaciones")
@Slf4j
@CrossOrigin(origins = "*")
public class SimulationRealtimeController {
    
    private final SimulationEngine simulationEngine;
    
    public SimulationRealtimeController(SimulationEngine simulationEngine) {
        this.simulationEngine = simulationEngine;
    }
    
    // ==================== INICIO DE VISUALIZACIÓN ====================
    
    @Operation(
            summary = "Iniciar visualización de una simulación completada",
            description = "Carga la solución del ALNS en memoria y prepara la simulación en tiempo real. " +
                          "Solo funciona si la simulación ya completó el proceso de optimización."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Visualización iniciada exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Simulación no encontrada"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Simulación aún no completada o sin solución"
            )
    })
    @PostMapping("/{simulacionId}/visualization/start")
    public ResponseEntity<SimulationStatusResponse> startVisualization(
            @Parameter(description = "ID de la simulación", required = true)
            @PathVariable Long simulacionId,
            @Valid @RequestBody(required = false) StartVisualizationRequest request) {
        
        log.info("🎬 Iniciando visualización para simulación {}", simulacionId);
        
        // Configuración por defecto
        Integer timeScale = (request != null && request.getTimeScale() != null) ? 
                request.getTimeScale() : 112;
        Boolean autoStart = (request != null && request.getAutoStart() != null) ? 
                request.getAutoStart() : true;
        
        // Iniciar simulación en memoria
        SimulationState state = simulationEngine.startSimulation(simulacionId, timeScale);
        
        // Si no es autoStart, pausar inmediatamente
        if (!autoStart) {
            simulationEngine.pauseSimulation(simulacionId);
        }
        
        // Devolver estado inicial
        SimulationStatusResponse response = buildStatusResponse(state);
        
        log.info("✅ Visualización iniciada: {} vuelos, factor {}x, autoStart={}", 
                state.getFlights().size(), timeScale, autoStart);
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== POLLING (ENDPOINT PRINCIPAL) ====================
    
    @Operation(
            summary = "Obtener estado actual de la simulación (POLLING)",
            description = "Endpoint principal para actualización en tiempo real. " +
                          "El frontend debe llamar a este endpoint cada 2-3 segundos. " +
                          "El backend calcula automáticamente el tiempo transcurrido y las posiciones actuales de los vuelos."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado actualizado obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = SimulationStatusResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Simulación no activa en memoria. Debe iniciar visualización primero."
            )
    })
    @GetMapping("/{simulacionId}/status")
    public ResponseEntity<?> getSimulationStatus(
            @Parameter(description = "ID de la simulación", required = true)
            @PathVariable Long simulacionId) {
        
        try {
            // Actualizar simulación (calcula posiciones actuales)
            SimulationState state = simulationEngine.updateSimulation(simulacionId);
            
            // Construir respuesta
            SimulationStatusResponse response = buildStatusResponse(state);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Simulación no está en memoria - devolver 503 con mensaje claro
            log.warn("Simulación {} no está en memoria. Mensaje: {}", simulacionId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("simulationId", simulacionId);
            errorResponse.put("error", "SIMULATION_NOT_LOADED");
            errorResponse.put("message", "La simulación no está cargada en memoria. Debe iniciar la visualización primero.");
            errorResponse.put("action", "POST /api/simulations/" + simulacionId + "/visualization/start");
            
            return ResponseEntity.status(503).body(errorResponse);
        }
    }
    
    /**
     * Construye la respuesta de status a partir del estado en memoria
     */
    private SimulationStatusResponse buildStatusResponse(SimulationState state) {
        LocalDateTime currentSimulatedTime = state.getCurrentSimulatedTime();
        long elapsedSeconds = ChronoUnit.SECONDS.between(state.getSimulatedStartTime(), currentSimulatedTime);
        
        // Extraer día, hora, minuto
        long totalMinutes = elapsedSeconds / 60;
        int currentDay = (int) (totalMinutes / (24 * 60)) + 1;  // Día 1, 2, 3...
        int currentHour = (int) ((totalMinutes % (24 * 60)) / 60);
        int currentMinute = (int) (totalMinutes % 60);
        
        // Convertir vuelos activos (solo IN_FLIGHT)
        List<ActiveFlightDTO> activeFlights = state.getFlights().stream()
                .filter(f -> f.getStatus() == FlightStatus.IN_FLIGHT)
                .map(this::convertToActiveFlightDTO)
                .collect(Collectors.toList());
        
        // Convertir almacenes
        List<WarehouseStateDTO> warehouses = state.getWarehouses().stream()
                .map(this::convertToWarehouseDTO)
                .collect(Collectors.toList());
        
        // Convertir métricas
        MetricsDTO metrics = convertToMetricsDTO(state.getMetrics());
        
        // Eventos recientes (últimos 20)
        List<SimulationEvent> recentEvents = new ArrayList<>(state.getRecentEvents());
        if (recentEvents.size() > 20) {
            recentEvents = recentEvents.subList(recentEvents.size() - 20, recentEvents.size());
        }
        
        return SimulationStatusResponse.builder()
                .simulationId(state.getSimulationId())
                .status(state.getStatus().name())
                .currentSimulatedTime(currentSimulatedTime)
                .elapsedSimulatedSeconds(elapsedSeconds)
                .progressPercentage(Math.round(state.calculateProgress() * 10000.0) / 100.0)
                .currentDay(currentDay)
                .currentHour(currentHour)
                .currentMinute(currentMinute)
                .activeFlights(activeFlights)
                .warehouses(warehouses)
                .metrics(metrics)
                .recentEvents(recentEvents)
                .timeScale(state.getTimeScale())
                .build();
    }
    
    private ActiveFlightDTO convertToActiveFlightDTO(FlightSnapshot flight) {
        return ActiveFlightDTO.builder()
                .flightId(flight.getFlightId())
                .flightCode(flight.getFlightCode())
                .currentLat(flight.getCurrentLat())
                .currentLng(flight.getCurrentLng())
                .originLat(flight.getOriginLat())
                .originLng(flight.getOriginLng())
                .destinationLat(flight.getDestinationLat())
                .destinationLng(flight.getDestinationLng())
                .originCode(flight.getOriginCode())
                .destinationCode(flight.getDestinationCode())
                .originCity(flight.getOriginCity())
                .destinationCity(flight.getDestinationCity())
                .status(flight.getStatus().name())
                .progressPercentage(flight.getProgressPercentage())
                .packagesOnBoard(flight.getPackagesOnBoard())
                .capacityUsed(flight.getCapacityUsed())
                .capacityMax(flight.getCapacityMax())
                .occupancyPercentage(flight.getOccupancyPercentage())
                .build();
    }
    
    private WarehouseStateDTO convertToWarehouseDTO(WarehouseSnapshot warehouse) {
        return WarehouseStateDTO.builder()
                .warehouseId(warehouse.getWarehouseId())
                .code(warehouse.getCode())
                .cityName(warehouse.getCityName())
                .latitude(warehouse.getLatitude())
                .longitude(warehouse.getLongitude())
                .capacity(warehouse.getCapacity())
                .current(warehouse.getCurrentOccupancy())
                .available(warehouse.getAvailable())
                .occupancyPercentage(warehouse.getOccupancyPercentage())
                .status(warehouse.getStatus().name())
                .isPrincipal(warehouse.isPrincipal())
                .build();
    }
    
    private MetricsDTO convertToMetricsDTO(SimulationMetrics metrics) {
        return MetricsDTO.builder()
                .totalFlights(metrics.getTotalFlights())
                .flightsScheduled(metrics.getFlightsScheduled())
                .flightsInAir(metrics.getFlightsInAir())
                .flightsCompleted(metrics.getFlightsCompleted())
                .totalOrders(metrics.getTotalOrders())
                .ordersDelivered(metrics.getOrdersDelivered())
                .ordersInTransit(metrics.getOrdersInTransit())
                .ordersWaiting(metrics.getOrdersWaiting())
                .slaCompliancePercentage(metrics.getSlaCompliancePercentage())
                .averageWarehouseOccupancy(metrics.getAverageWarehouseOccupancy())
                .build();
    }
    
    // ==================== CONTROL DE SIMULACIÓN ====================
    
    @Operation(
            summary = "Controlar simulación (pause/resume/stop/setSpeed)",
            description = "Permite controlar la ejecución de la simulación en tiempo real"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Comando ejecutado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Comando inválido"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Simulación no activa"
            )
    })
    @PatchMapping("/{simulacionId}/control")
    public ResponseEntity<SimulationStatusResponse> controlSimulation(
            @Parameter(description = "ID de la simulación", required = true)
            @PathVariable Long simulacionId,
            @Valid @RequestBody SimulationControlRequest request) {
        
        log.info("🎮 Control de simulación {}: action={}", simulacionId, request.getAction());
        
        switch (request.getAction().toLowerCase()) {
            case "pause":
                simulationEngine.pauseSimulation(simulacionId);
                break;
                
            case "resume":
                simulationEngine.resumeSimulation(simulacionId);
                break;
                
            case "stop":
                simulationEngine.stopSimulation(simulacionId);
                break;
                
            case "setspeed":
                if (request.getNewSpeed() == null) {
                    throw new IllegalArgumentException("newSpeed es requerido para action=setSpeed");
                }
                simulationEngine.setSimulationSpeed(simulacionId, request.getNewSpeed());
                break;
                
            default:
                throw new IllegalArgumentException("Acción inválida: " + request.getAction());
        }
        
        // Devolver estado actualizado
        SimulationState state = simulationEngine.getSimulation(simulacionId);
        SimulationStatusResponse response = buildStatusResponse(state);
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== INFORMACIÓN Y GESTIÓN ====================
    
    @Operation(
            summary = "Verificar si una simulación está activa en memoria",
            description = "Retorna información del estado si está activa, null si no"
    )
    @GetMapping("/{simulacionId}/info")
    public ResponseEntity<Map<String, Object>> getSimulationInfo(
            @Parameter(description = "ID de la simulación", required = true)
            @PathVariable Long simulacionId) {
        
        SimulationState state = simulationEngine.getSimulation(simulacionId);
        
        if (state == null) {
            return ResponseEntity.status(404).build();
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put("simulationId", state.getSimulationId());
        info.put("status", state.getStatus().name());
        info.put("isActive", true);
        info.put("timeScale", state.getTimeScale());
        info.put("totalFlights", state.getFlights().size());
        info.put("startTime", state.getSimulatedStartTime());
        
        return ResponseEntity.ok(info);
    }
}

