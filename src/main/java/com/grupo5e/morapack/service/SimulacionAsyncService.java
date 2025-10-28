package com.grupo5e.morapack.service;

import com.grupo5e.morapack.algorithm.alns.ALNSSolver;
import com.grupo5e.morapack.api.dto.SimulacionSemanalRequestDTO;
import com.grupo5e.morapack.core.enums.EstadoSimulacion;
import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.repository.SimulacionAsignacionRepository;
import com.grupo5e.morapack.repository.SimulacionSemanalRepository;
import com.grupo5e.morapack.simulation.service.SimulationEngine;
import com.grupo5e.morapack.utils.CoordenadasUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio separado para ejecución asíncrona del algoritmo ALNS.
 * Se requiere una clase separada para que @Async funcione correctamente.
 */
@Service
@Slf4j
public class SimulacionAsyncService {

    private final SimulacionSemanalRepository simulacionRepository;
    private final SimulacionAsignacionRepository asignacionRepository;
    private final AeropuertoService aeropuertoService;
    private final PedidoService pedidoService;
    private final VueloService vueloService;
    private final SimulationEngine simulationEngine;

    public SimulacionAsyncService(
            SimulacionSemanalRepository simulacionRepository,
            SimulacionAsignacionRepository asignacionRepository,
            AeropuertoService aeropuertoService,
            PedidoService pedidoService,
            VueloService vueloService,
            @Lazy SimulationEngine simulationEngine) {
        this.simulacionRepository = simulacionRepository;
        this.asignacionRepository = asignacionRepository;
        this.aeropuertoService = aeropuertoService;
        this.pedidoService = pedidoService;
        this.vueloService = vueloService;
        this.simulationEngine = simulationEngine;
    }

    /**
     * Ejecuta el algoritmo ALNS de forma asíncrona en un thread separado.
     * Este método NO bloquea la respuesta HTTP.
     */
    @Async("simulacionExecutor")
    public void ejecutarAlgoritmoAsync(Long simulacionId, SimulacionSemanalRequestDTO request) {
        log.info("⚙️ [Thread: {}] Ejecutando ALNS para simulación {}", 
                Thread.currentThread().getName(), simulacionId);

        try {
            // Actualizar estado EN_PROGRESO en transacción separada
            actualizarEstadoSimulacion(simulacionId, EstadoSimulacion.EN_PROGRESO, 10);
            
            SimulacionSemanal simulacion = simulacionRepository.findById(simulacionId)
                    .orElseThrow(() -> new RuntimeException("Simulación no encontrada"));

            // Crear solver con configuración personalizada
            Integer iteraciones = request.getIteracionesAlns() != null ? request.getIteracionesAlns() : 500;
            Integer timeout = request.getTiempoLimiteSegundos() != null ? request.getTiempoLimiteSegundos() : 0;
            
            log.info("📊 Inicializando ALNSSolver con {} iteraciones, timeout: {} seg", iteraciones, timeout);
            ALNSSolver solver = new ALNSSolver(aeropuertoService, pedidoService, vueloService, iteraciones, timeout);

            // Ejecutar algoritmo con timeout
            log.info("🔄 Ejecutando algoritmo ALNS (timeout: {} segundos)...", 
                    request.getTiempoLimiteSegundos());
            long startTime = System.currentTimeMillis();
            
            // Ejecutar en thread con timeout
            solver.resolver();
            
            long endTime = System.currentTimeMillis();
            long duracionMs = endTime - startTime;
            
            log.info("⏱️ ALNS ejecutado en {} segundos", duracionMs / 1000);

            log.info("✅ ALNS completado en {} ms", duracionMs);

            // Obtener solución
            HashMap<Pedido, ArrayList<Vuelo>> solucionOptima = solver.getMejorSolucion();
            Integer pesoSolucion = solver.getPesoMejorSolucion();
            LocalDateTime T0 = solver.getT0();
            List<Pedido> pedidosNoAsignados = solver.getPedidosNoAsignados();

            // Validar solución
            boolean esValida = solver.esSolucionValida();
            boolean capacidadValida = solver.esSolucionCapacidadValida();

            // Recargar simulación para actualizar
            simulacion = simulacionRepository.findById(simulacionId)
                    .orElseThrow(() -> new RuntimeException("Simulación no encontrada"));

            // Actualizar registro de simulación
            simulacion.setFechaFin(LocalDateTime.now());
            simulacion.setDuracionMs(duracionMs);
            simulacion.setEstado(EstadoSimulacion.COMPLETADA);
            simulacion.setProgreso(100);
            simulacion.setTiempoInicialReferencia(T0);
            simulacion.setPesoSolucion(pesoSolucion);
            simulacion.setSolucionValida(esValida && capacidadValida);
            
            // Calcular estadísticas
            int totalPedidos = solver.getPedidos().size();
            int pedidosAsignados = solucionOptima.size();
            int pedidosNoAsig = pedidosNoAsignados.size();

            simulacion.setTotalPedidos(totalPedidos);
            simulacion.setPedidosAsignados(pedidosAsignados);
            simulacion.setPedidosNoAsignados(pedidosNoAsig);

            // Calcular costo y tiempo promedio
            double costoTotal = calcularCostoTotal(solucionOptima);
            double tiempoPromedio = calcularTiempoPromedioEntrega(solucionOptima);

            simulacion.setCostoTotal(costoTotal);
            simulacion.setTiempoPromedioEntrega(tiempoPromedio);

            simulacionRepository.save(simulacion);

            // Guardar asignaciones
            log.info("💾 Guardando asignaciones en BD...");
            guardarAsignaciones(simulacion, solucionOptima, T0);

            log.info("🎉 Simulación {} completada exitosamente", simulacionId);
            
            // Si se solicitó, cargar visualización en memoria automáticamente
            if (Boolean.TRUE.equals(request.getAutoStartVisualization())) {
                try {
                    log.info("🎬 Cargando visualización en memoria automáticamente...");
                    Integer timeScale = request.getFactorAceleracion() != null ? 
                            request.getFactorAceleracion() : 112;
                    simulationEngine.startSimulation(simulacionId, timeScale);
                    log.info("✅ Visualización cargada y lista para polling");
                } catch (Exception e) {
                    log.warn("⚠️ No se pudo cargar visualización automáticamente: {}", e.getMessage());
                    // No fallar la simulación por esto
                }
            }

        } catch (Exception e) {
            log.error("❌ Error en simulación {}: {}", simulacionId, e.getMessage(), e);
            
            // Actualizar estado de error en transacción separada
            actualizarEstadoError(simulacionId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void actualizarEstadoError(Long simulacionId, String mensajeError) {
        SimulacionSemanal sim = simulacionRepository.findById(simulacionId).orElse(null);
        if (sim != null) {
            sim.setEstado(EstadoSimulacion.ERROR);
            sim.setMensajeError(mensajeError);
            sim.setFechaFin(LocalDateTime.now());
            simulacionRepository.saveAndFlush(sim);
        }
    }

    /**
     * Guarda las asignaciones (solución) en la base de datos
     */
    @Transactional
    protected void guardarAsignaciones(SimulacionSemanal simulacion, 
                                       HashMap<Pedido, ArrayList<Vuelo>> solucion,
                                       LocalDateTime T0) {
        int contador = 0;
        
        for (Map.Entry<Pedido, ArrayList<Vuelo>> entry : solucion.entrySet()) {
            Pedido pedido = entry.getKey();
            ArrayList<Vuelo> ruta = entry.getValue();

            int minutoActual = calcularMinutoInicio(pedido, T0);

            for (int secuencia = 0; secuencia < ruta.size(); secuencia++) {
                Vuelo vuelo = ruta.get(secuencia);

                // Calcular minutos de inicio y fin
                int minutoInicio = minutoActual;
                int duracionMinutos = (int) (vuelo.getTiempoTransporte() * 60);
                int minutoFin = minutoInicio + duracionMinutos;

                // Obtener coordenadas
                Aeropuerto origen = vuelo.getAeropuertoOrigen();
                Aeropuerto destino = vuelo.getAeropuertoDestino();

                // Log para debuggear (solo primera vez)
                if (contador == 0) {
                    log.info("🔍 DEBUG - Parseando coordenadas:");
                    log.info("   Origen ({}): lat='{}', lon='{}'", origen.getCodigoIATA(), origen.getLatitud(), origen.getLongitud());
                }

                double latOrigen = CoordenadasUtils.parsearCoordenada(origen.getLatitud());
                double lonOrigen = CoordenadasUtils.parsearCoordenada(origen.getLongitud());
                double latDestino = CoordenadasUtils.parsearCoordenada(destino.getLatitud());
                double lonDestino = CoordenadasUtils.parsearCoordenada(destino.getLongitud());

                // Log de resultado
                if (contador == 0) {
                    log.info("   Resultado: lat={}, lon={}", latOrigen, lonOrigen);
                }

                // Crear asignación
                SimulacionAsignacion asignacion = new SimulacionAsignacion();
                asignacion.setSimulacion(simulacion);
                asignacion.setPedido(pedido);
                asignacion.setSecuencia(secuencia + 1);
                asignacion.setVuelo(vuelo);
                asignacion.setMinutoInicio(minutoInicio);
                asignacion.setMinutoFin(minutoFin);
                asignacion.setLatitudInicio(latOrigen);
                asignacion.setLongitudInicio(lonOrigen);
                asignacion.setLatitudFin(latDestino);
                asignacion.setLongitudFin(lonDestino);

                asignacionRepository.save(asignacion);

                // Actualizar minuto para siguiente vuelo (incluye tiempo de conexión)
                minutoActual = minutoFin + 120; // 2 horas de conexión

                contador++;
            }
        }

        log.info("💾 {} asignaciones guardadas", contador);
    }

    /**
     * Calcula el minuto de inicio de un pedido desde T0
     */
    private int calcularMinutoInicio(Pedido pedido, LocalDateTime T0) {
        if (pedido.getFechaPedido() == null || T0 == null) {
            return 0;
        }
        
        Duration duracion = Duration.between(T0, pedido.getFechaPedido());
        long minutos = duracion.toMinutes();
        
        // Agregar offset aleatorio basado en el ID
        int offset = (int) (pedido.getId() % 60);
        
        return (int) Math.max(0, minutos + offset);
    }

    /**
     * Calcula el costo total de la solución
     */
    private double calcularCostoTotal(HashMap<Pedido, ArrayList<Vuelo>> solucion) {
        double total = 0.0;
        java.util.Set<Vuelo> vuelosUsados = new java.util.HashSet<>();

        for (ArrayList<Vuelo> ruta : solucion.values()) {
            for (Vuelo vuelo : ruta) {
                vuelosUsados.add(vuelo);
            }
        }

        for (Vuelo vuelo : vuelosUsados) {
            total += vuelo.getCosto();
        }

        return total;
    }

    /**
     * Calcula el tiempo promedio de entrega
     */
    private double calcularTiempoPromedioEntrega(HashMap<Pedido, ArrayList<Vuelo>> solucion) {
        if (solucion.isEmpty()) {
            return 0.0;
        }

        double sumaHoras = 0.0;

        for (ArrayList<Vuelo> ruta : solucion.values()) {
            double horasRuta = 0.0;
            for (Vuelo vuelo : ruta) {
                horasRuta += vuelo.getTiempoTransporte();
            }
            // Agregar tiempo de conexiones (2 horas por escala)
            if (ruta.size() > 1) {
                horasRuta += (ruta.size() - 1) * 2.0;
            }
            sumaHoras += horasRuta;
        }

        return sumaHoras / solucion.size();
    }

    /**
     * Actualiza el estado de la simulación en una transacción separada
     * para que el cambio se persista inmediatamente
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void actualizarEstadoSimulacion(Long simulacionId, EstadoSimulacion estado, Integer progreso) {
        SimulacionSemanal sim = simulacionRepository.findById(simulacionId).orElse(null);
        if (sim != null) {
            sim.setEstado(estado);
            if (progreso != null) {
                sim.setProgreso(progreso);
            }
            simulacionRepository.saveAndFlush(sim);
            log.debug("📊 Estado actualizado: {} - {}%", estado, progreso);
        }
    }
}

