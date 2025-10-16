package com.grupo5e.morapack.algorithm.alns.modular;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.Producto;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.service.AeropuertoService;
import com.grupo5e.morapack.service.PedidoService;
import com.grupo5e.morapack.service.VueloService;

import java.util.ArrayList;
import java.util.List;

//CARGA Y PREPARACION DE DATOS
public class DataLoader {
    private final AeropuertoService aeropuertoService;
    private final PedidoService pedidoService;
    private final VueloService vueloService;

    //Constructor de los servicios
    public DataLoader(AeropuertoService aeropuertoService, PedidoService pedidoService, VueloService vueloService) {
        this.aeropuertoService = aeropuertoService;
        this.pedidoService = pedidoService;
        this.vueloService = vueloService;
    }

    //Clase estatica para cargar los datos de las listas que se usaran para el algoritmo
    public DataLoadResult cargarDatos(boolean habilitarUnitizacion){
        List<Aeropuerto> aeropuertos = new ArrayList<>(aeropuertoService.listar());
        List<Pedido> pedidosOriginales = new ArrayList<>(pedidoService.listar());
        List<Vuelo> vuelos = new ArrayList<>(vueloService.listar());
        List<Pedido> pedidos;

        if(habilitarUnitizacion){
            pedidos = expandirPaquetesAUnidadesProducto(pedidosOriginales);
            System.out.println("UNITIZACIÓN APLICADA: " + pedidosOriginales.size() +
                    " → " + pedidos.size() + " unidades");
        }else {
            pedidos = new ArrayList<>(pedidosOriginales);
            System.out.println("UNITIZACIÓN DESHABILITADA");
        }

        return new DataLoadResult(aeropuertos, vuelos, pedidos, pedidosOriginales);
    }

    private List<Pedido> expandirPaquetesAUnidadesProducto(List<Pedido> paquetesOriginales) {
        List<Pedido> unidadesProducto = new ArrayList<>();
        for (Pedido pedidoOriginal : paquetesOriginales) {
            int conteoProductos = (pedidoOriginal.getProductos() != null &&
                    !pedidoOriginal.getProductos().isEmpty())
                    ? pedidoOriginal.getProductos().size() : 1;
            //por cada pedido hay que crear todos sus pedidos correspondientes
            for (int i = 0; i < conteoProductos; i++) {
                unidadesProducto.add(crearUnidadPaquete(pedidoOriginal, i));
            }
        }
        return unidadesProducto;
    }

    private Pedido crearUnidadPaquete(Pedido pedidoOriginal, int indiceUnidad) {
        // Implementación simplificada de creación de unidad
        Pedido unidad = new Pedido();
        String idUnidadString = pedidoOriginal.getId() + "#" + indiceUnidad;
        unidad.setId((long) idUnidadString.hashCode());
        unidad.setCliente(pedidoOriginal.getCliente());
        unidad.setAeropuertoDestinoCodigo(pedidoOriginal.getAeropuertoDestinoCodigo());
        unidad.setFechaPedido(pedidoOriginal.getFechaPedido());
        unidad.setFechaLimiteEntrega(pedidoOriginal.getFechaLimiteEntrega());
        unidad.setEstado(pedidoOriginal.getEstado());
        unidad.setPrioridad(pedidoOriginal.getPrioridad());

        ArrayList<Producto> productoUnico = new ArrayList<>();
        if (pedidoOriginal.getProductos() != null) {
            if(indiceUnidad < pedidoOriginal.getProductos().size())
                productoUnico.add(pedidoOriginal.getProductos().get(indiceUnidad));
        }
        unidad.setProductos(productoUnico);

        return unidad;
    }

    public static class DataLoadResult {
        public final List<Aeropuerto> aeropuertos;
        public final List<Vuelo> vuelos;
        public final List<Pedido> pedidos;
        public final List<Pedido> pedidosOriginales;

        public DataLoadResult(List<Aeropuerto> aeropuertos, List<Vuelo> vuelos,
                              List<Pedido> pedidos, List<Pedido> pedidosOriginales) {
            this.aeropuertos = aeropuertos;
            this.vuelos = vuelos;
            this.pedidos = pedidos;
            this.pedidosOriginales = pedidosOriginales;
        }
    }
}
