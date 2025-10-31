package com.grupo5e.morapack.simulation.controller;

import com.grupo5e.morapack.simulation.dto.OperationalOrderDTO;
import com.grupo5e.morapack.simulation.service.SimulationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operacional")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OperationalSimulationController {

    private final SimulationEngine simulationEngine;

    // 1. Crear simulación operacional (1 día, sin ALNS)
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startOperational(
            @RequestParam(name = "timeScale", defaultValue = "120") int timeScale
    ) {
        Long simulationId = simulationEngine.startOperationalSimulation(timeScale);
        Map<String, Object> res = new HashMap<>();
        res.put("simulationId", simulationId);
        res.put("message", "Simulación operacional creada");
        return ResponseEntity.ok(res);
    }

    // 2. Registrar UN pedido desde el front (los 3-4 estudiantes lo llaman al mismo tiempo)
    @PostMapping("/{simulationId}/orders")
    public ResponseEntity<Void> addOrder(
            @PathVariable Long simulationId,
            @RequestBody OperationalOrderDTO order
    ) {
        simulationEngine.ingestOrder(simulationId, order);
        return ResponseEntity.ok().build();
    }

    // 3. Cargar lote (2da parte de la prueba)
    @PostMapping("/{simulationId}/orders/batch")
    public ResponseEntity<Map<String, Object>> addOrdersBatch(
            @PathVariable Long simulationId,
            @RequestBody List<OperationalOrderDTO> orders
    ) {
        int added = simulationEngine.ingestOrders(simulationId, orders);
        Map<String, Object> res = new HashMap<>();
        res.put("inserted", added);
        return ResponseEntity.ok(res);
    }
}