package com.grupo5e.morapack.simulation.model;

import lombok.*;

/**
 * Métricas en tiempo real de la simulación
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationMetrics {
    
    /**
     * Total de vuelos en la simulación
     */
    private int totalFlights;
    
    /**
     * Vuelos que aún no han despegado
     */
    private int flightsScheduled;
    
    /**
     * Vuelos actualmente en el aire
     */
    private int flightsInAir;
    
    /**
     * Vuelos que ya aterrizaron
     */
    private int flightsCompleted;
    
    /**
     * Total de pedidos/paquetes
     */
    private int totalOrders;
    
    /**
     * Pedidos ya entregados
     */
    private int ordersDelivered;
    
    /**
     * Pedidos actualmente en tránsito
     */
    private int ordersInTransit;
    
    /**
     * Pedidos aún en bodega esperando
     */
    private int ordersWaiting;
    
    /**
     * Porcentaje de cumplimiento de SLA (0-100)
     */
    private double slaCompliancePercentage;
    
    /**
     * Ocupación promedio de almacenes (0-100)
     */
    private double averageWarehouseOccupancy;
}

