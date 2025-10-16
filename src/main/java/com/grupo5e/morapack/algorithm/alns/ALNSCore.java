package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.index.IndiceVuelos;
import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.core.service.ServicioDisponibilidadVuelos;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ALNSCore {

    private List<Aeropuerto> aeropuertos;
    private List<Vuelo> vuelos;
    private List<Pedido> pedidos;
    private Map<Aeropuerto, Integer> ocupacionAlmacenes;
    private Random aleatorio;
    private ServicioDisponibilidadVuelos servicioDisponibilidadVuelos;
    private IndiceVuelos indiceVuelos;


}
