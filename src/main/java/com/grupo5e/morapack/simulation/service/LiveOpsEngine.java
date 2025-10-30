//// src/main/java/.../simulation/service/LiveOpsEngine.java
//package com.grupo5e.morapack.simulation.service;
//
//import com.grupo5e.morapack.simulation.service.SimulationEngine;
//import com.grupo5e.morapack.simulation.model.*;
//import com.grupo5e.morapack.simulation.dto.SimulationControlRequest;
//import com.grupo5e.morapack.core.model.*; // Vuelo, Pedido, Aeropuerto, etc.
//import com.grupo5e.morapack.repository.*; // VueloRepository, PedidoRepository, AeropuertoRepository
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.time.*;
//import java.util.*;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class LiveOpsEngine {
//
//    // ====== INYECCIONES ======
//    private final VueloRepository vueloRepo;
//    private final PedidoRepository pedidoRepo;
//    private final AeropuertoRepository aeropuertoRepo;
//    private final SimulationEngine simulationEngine; // reusamos su lógica de update/metrics
//    private final Clock clock = Clock.systemDefaultZone(); // o inyecta por constructor si prefieres
//
//    // ====== ESTADO EN MEMORIA (Live) ======
//    private final AtomicReference<SimulationState> stateRef = new AtomicReference<>();
//
//    // ====== API PÚBLICA ======
//
//    /**
//     * Carga/recarga el estado "en vivo" para HOY (o una ventana rolling) y
//     * deja la simulación corriendo en memoria. Devuelve el estado actual.
//     */
//    public SimulationState startOrReloadToday(Integer timeScale, boolean autoStart) {
//        int scale = (timeScale != null && timeScale > 0) ? timeScale : 1; // en vivo tipicamente 1x
//        LocalDate today = LocalDate.now(clock);
//
//        // 1) Construir snapshots desde datos del día
//        SimulationState fresh = buildLiveState(today, scale);
//
//        // 2) Poner en marcha (RUNNING o PAUSED)
//        fresh.setStatus(autoStart ? SimulationStatus.RUNNING : SimulationStatus.PAUSED);
//
//        stateRef.set(fresh);
//        log.info("[LIVE] Estado live inicializado para {}, flights={}, warehouses={}",
//                today, fresh.getFlights().size(), fresh.getWarehouses().size());
//
//        return fresh;
//    }
//
//    /**
//     * Poll de estado: recalcula posiciones/eventos vs tiempo real (o timescale), actualiza métricas y retorna.
//     */
//    public SimulationState update() {
//        SimulationState st = ensureState();
//        // Reusar el mismo 'update' del SimulationEngine si opera sobre SimulationState genérico.
//        simulationEngine.update(st, now());
//        return st;
//    }
//
//    public void control(SimulationControlRequest req) {
//        SimulationState st = ensureState();
//        switch (req.getAction()) {
//            case PAUSE -> simulationEngine.pause(st);
//            case RESUME -> simulationEngine.resume(st);
//            case STOP -> simulationEngine.stop(st);
//            case SET_SPEED -> {
//                Integer sp = req.getNewSpeed();
//                if (sp == null || sp <= 0) throw new IllegalArgumentException("newSpeed requerido > 0");
//                simulationEngine.setTimeScale(st, sp);
//            }
//            default -> throw new IllegalArgumentException("Acción no soportada para Live: " + req.getAction());
//        }
//    }
//
//    public SimulationState getState() {
//        return ensureState();
//    }
//
//    // ====== CONSTRUCCIÓN DEL ESTADO LIVE ======
//
//    /**
//     * Crea SimulationState para HOY (o una ventana alrededor de now), armando:
//     * - FlightSnapshots desde vuelos del día
//     * - WarehouseSnapshots desde aeropuertos
//     * - Empaque/assignments greedy básico (opcional)
//     */
//    private SimulationState buildLiveState(LocalDate baseDate, int timeScale) {
//        // Ventana "hoy" (puedes ampliar ±12h si lo prefieres)
//        ZonedDateTime startZdt = baseDate.atStartOfDay(ZoneId.systemDefault());
//        ZonedDateTime endZdt = startZdt.plusDays(1);
//
//        // 1) Traer vuelos del día
//        List<Vuelo> vuelos = loadFlightsForWindow(startZdt.toInstant(), endZdt.toInstant());
//
//        // 2) Traer pedidos "pendientes" del día (ajusta criterio de negocio)
//        List<Pedido> pedidos = loadPendingOrdersUntil(endZdt.toInstant());
//
//        // 3) (Opcional) Packing/assignments simple (directos). Devuelve map vueloId -> pedidos asignados
//        Map<Long, List<Pedido>> asignaciones = packGreedy(vuelos, pedidos);
//
//        // 4) Snapshots de vuelos (incluye asignaciones en 'packagesOnBoard' / 'capacityUsed')
//        List<FlightSnapshot> flightSnapshots = toFlightSnapshots(vuelos, asignaciones);
//
//        // 5) Snapshots de almacenes (desde aeropuertos)
//        List<WarehouseSnapshot> whSnapshots = toWarehouseSnapshots();
//
//        // 6) Armar SimulationState live
//        SimulationState st = new SimulationState();
//        st.setSimulationId(0L); // fijo para live
//        st.setMode(SimulationMode.LIVE); // si existe, útil para distinguir en UI/logs
//        st.setStatus(SimulationStatus.RUNNING);
//        st.setCreatedAt(now());
//        st.setSimulatedStartTime(now()); // en vivo: baseline = ahora
//        st.setCurrentSimulatedTime(now());
//        st.setTimeScale(timeScale);
//        st.setFlights(flightSnapshots);
//        st.setWarehouses(whSnapshots);
//        st.setEvents(new ArrayList<>());
//        st.setMetrics(initialMetrics(flightSnapshots, whSnapshots));
//
//        // 7) Ajustes/métricas iniciales si tu engine los requiere
//        simulationEngine.recomputeAggregates(st);
//
//        return st;
//    }
//
//    // ====== HELPERS (BD, PACKER, SNAPSHOTS, METRICS) ======
//
//    private List<Vuelo> loadFlightsForWindow(Instant start, Instant end) {
//        // TODO: reemplazar por tu query real (JPA/JPQL/SQL)
//        // Ejemplo indicativo:
//        return vueloRepo.findByDepartureBetween(Date.from(start), Date.from(end));
//    }
//
//    private List<Pedido> loadPendingOrdersUntil(Instant until) {
//        // TODO: criterio real (estado PENDIENTE/CONFIRMADO y no entregado, fecha <= until)
//        return pedidoRepo.findPendingUntil(Date.from(until));
//    }
//
//    /**
//     * Packer greedy ultra simple: llena vuelos por orden de salida con pedidos compatibles por origen y destino DIRECTO.
//     * Mejora: prioridad por SLA/urgencia, soportar escalas, etc.
//     */
//    private Map<Long, List<Pedido>> packGreedy(List<Vuelo> vuelos, List<Pedido> pedidos) {
//        // Index pedidos por (origen, destino) para rápido acceso
//        Map<String, Deque<Pedido>> pool = pedidos.stream()
//                .sorted(Comparator
//                        .comparing(Pedido::getDeadline) // primero los más urgentes
//                        .thenComparing(Pedido::getPrioridad, Comparator.reverseOrder()))
//                .collect(Collectors.groupingBy(
//                        p -> key(p.getOrigen().getCodigoIata(), p.getDestino().getCodigoIata()),
//                        Collectors.toCollection(ArrayDeque::new)));
//
//        Map<Long, List<Pedido>> asignaciones = new HashMap<>();
//
//        // Vuelos ordenados por hora de salida
//        List<Vuelo> ordenados = new ArrayList<>(vuelos);
//        ordenados.sort(Comparator.comparing(Vuelo::getFechaSalida));
//
//        for (Vuelo v : ordenados) {
//            String k = key(v.getOrigen().getCodigoIata(), v.getDestino().getCodigoIata());
//            Deque<Pedido> cand = pool.getOrDefault(k, new ArrayDeque<>());
//
//            double capacidad = Optional.ofNullable(v.getCapacidadMaxima()).orElse(0.0d);
//            double used = 0.0d;
//
//            List<Pedido> asignados = new ArrayList<>();
//            while (!cand.isEmpty() && used < capacidad) {
//                Pedido p = cand.peekFirst();
//                double size = pedidoSize(p); // volumen/peso/UDS – define tu métrica
//                if (used + size <= capacidad) {
//                    asignados.add(cand.pollFirst());
//                    used += size;
//                } else {
//                    // si no entra el primero, intenta con alguno que sí (opcional: búsqueda lineal limitada)
//                    Optional<Pedido> fit = cand.stream().filter(px -> pedidoSize(px) <= (capacidad - used)).findFirst();
//                    if (fit.isPresent()) {
//                        cand.remove(fit.get());
//                        asignados.add(fit.get());
//                        used += pedidoSize(fit.get());
//                    } else {
//                        break; // no cabe nadie más
//                    }
//                }
//            }
//            if (!asignados.isEmpty()) {
//                asignaciones.put(v.getId(), asignados);
//            }
//        }
//        return asignaciones;
//    }
//
//    private String key(String origenIata, String destinoIata) {
//        return origenIata + "→" + destinoIata;
//    }
//
//    private double pedidoSize(Pedido p) {
//        // TODO: define tu unidad: peso, volumen, unidades, o combinación
//        return Optional.ofNullable(p.getPeso()).orElse(1.0d);
//    }
//
//    private List<FlightSnapshot> toFlightSnapshots(List<Vuelo> vuelos, Map<Long, List<Pedido>> asignaciones) {
//        Instant now = now();
//
//        return vuelos.stream().map(v -> {
//            FlightSnapshot fs = new FlightSnapshot();
//            fs.setFlightId(v.getId());
//            fs.setOriginIata(v.getOrigen().getCodigoIata());
//            fs.setDestinationIata(v.getDestino().getCodigoIata());
//            fs.setDepartureTime(v.getFechaSalida().toInstant());
//            fs.setArrivalTime(v.getFechaLlegada().toInstant());
//
//            // Estado inicial según reloj
//            if (now.isBefore(fs.getDepartureTime())) {
//                fs.setStatus(FlightStatus.SCHEDULED);
//            } else if (now.isAfter(fs.getArrivalTime())) {
//                fs.setStatus(FlightStatus.LANDED);
//            } else {
//                fs.setStatus(FlightStatus.IN_FLIGHT);
//            }
//
//            // Capacidad usada/paquetes a bordo (estimado)
//            List<Pedido> onBoard = asignaciones.getOrDefault(v.getId(), Collections.emptyList());
//            fs.setPackagesOnBoard(
//                    onBoard.stream().map(this::toPackageRef).toList()
//            );
//            double used = onBoard.stream().mapToDouble(this::pedidoSize).sum();
//            fs.setCapacityUsed(used);
//            fs.setCapacityMax(Optional.ofNullable(v.getCapacidadMaxima()).orElse(0.0d));
//
//            // Coordenadas iniciales (si tu engine las interpola, puedes dejarlas vacías y que las calcule)
//            // simulationEngine.initPosition(fs); // si tienes un helper para setear pos inicial
//
//            return fs;
//        }).toList();
//    }
//
//    private PackageRef toPackageRef(Pedido p) {
//        PackageRef r = new PackageRef();
//        r.setId(p.getId());
//        r.setOrderCode(p.getCodigo()); // si tienes
//        r.setWeight(Optional.ofNullable(p.getPeso()).orElse(0.0d));
//        r.setPriority(p.getPrioridad() != null ? p.getPrioridad().name() : "NORMAL");
//        return r;
//    }
//
//    private List<WarehouseSnapshot> toWarehouseSnapshots() {
//        List<Aeropuerto> aeropuertos = aeropuertoRepo.findAll();
//
//        return aeropuertos.stream().map(a -> {
//            WarehouseSnapshot w = new WarehouseSnapshot();
//            w.setAirportIata(a.getCodigoIata());
//            w.setName(a.getNombre());
//            w.setLatitude(a.getLat());
//            w.setLongitude(a.getLng());
//            // Valores iniciales: si no llevas inventario real, arranca en 0
//            w.setCapacityMax(Optional.ofNullable(a.getCapacidadBodega()).orElse(0.0d));
//            w.setCurrentOccupancy(0.0d);
//            w.setStatus(WarehouseStatus.OK);
//            return w;
//        }).toList();
//    }
//
//    private SimulationMetrics initialMetrics(List<FlightSnapshot> flights, List<WarehouseSnapshot> whs) {
//        SimulationMetrics m = new SimulationMetrics();
//        m.setTotalFlights(flights.size());
//        m.setActiveFlights((int) flights.stream().filter(f -> f.getStatus() == FlightStatus.IN_FLIGHT).count());
//        m.setWarehouses(whs.size());
//        // agrega KPIs que ya muestres en UI
//        return m;
//    }
//
//    // ====== UTIL ======
//
//    private Instant now() {
//        return Instant.now(clock);
//    }
//
//    private SimulationState ensureState() {
//        SimulationState st = stateRef.get();
//        if (st == null) {
//            // Carga perezosa con defaults (1x y autoStart)
//            st = startOrReloadToday(1, true);
//        }
//        return st;
//    }
//}
