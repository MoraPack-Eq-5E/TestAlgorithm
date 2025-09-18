package com.grupo5e.morapack.algorithm.alns.operators.construction;

import com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorConstruccion;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;
import com.grupo5e.morapack.algorithm.validation.ValidadorRestricciones;
import com.grupo5e.morapack.core.model.*;

import java.util.*;

/**
 * Clase auxiliar para representar una inserción posible
 */
class InserciónPosible {
    String paqueteId;
    String rutaId;
    int posición;
    double costo;
    
    InserciónPosible(String paqueteId, String rutaId, int posición, double costo) {
        this.paqueteId = paqueteId;
        this.rutaId = rutaId;
        this.posición = posición;
        this.costo = costo;
    }
}

/**
 * Operador de construcción Regret-k basado en el ejemplo VRPTWFL
 * Selecciona el paquete con mayor regret (diferencia entre mejor y k-ésima mejor inserción)
 */
public class RegretKInsertion extends AbstractOperator implements OperadorConstruccion {
    
    private final int k;
    
    public RegretKInsertion(int k) {
        super("Regret" + k + "Insertion", "construction");
        this.k = k;
    }
    
    @Override
    public Solucion construir(Solucion solucion, List<String> paquetesRemovidos, 
                            ContextoProblema contexto, ValidadorRestricciones validador) {
        
        if (paquetesRemovidos.isEmpty()) {
            return solucion;
        }
        
        Solucion solucionTemporal = solucion.copiar();
        List<String> paquetesPendientes = new ArrayList<>(paquetesRemovidos);
        
        // Debug: mostrar paquetes a insertar
        System.out.println("   🔧 Regret" + k + "Insertion: Insertando " + paquetesPendientes.size() + " paquetes");
        
        // Algoritmo Regret-k: insertar paquetes uno por uno usando regret
        int paquetesInsertados = 0;
        while (!paquetesPendientes.isEmpty()) {
            // Encontrar el paquete con mayor regret
            String mejorPaquete = seleccionarPaqueteConMayorRegret(solucionTemporal, paquetesPendientes, contexto);
            
            if (mejorPaquete != null) {
                // Insertar el producto en la mejor posición usando vuelos reales
                insertarProductoEnMejorPosicion(solucionTemporal, mejorPaquete, contexto);
                paquetesPendientes.remove(mejorPaquete);
                paquetesInsertados++;
            } else {
                // Si no se puede insertar ningún paquete, terminar
                System.out.println("   ⚠️  Regret" + k + "Insertion: No se pudo insertar más paquetes");
                break;
            }
        }
        
        System.out.println("   ✅ Regret" + k + "Insertion: Insertados " + paquetesInsertados + " paquetes");
        return solucionTemporal;
    }
    
    /**
     * Selecciona el paquete con mayor regret basado en el algoritmo VRPTWFL
     */
    private String seleccionarPaqueteConMayorRegret(Solucion solucion, List<String> paquetesPendientes, 
                                                   ContextoProblema contexto) {
        
        double maxRegret = -1;
        String mejorPaquete = null;
        double mejorCostoInserción = Double.MAX_VALUE;
        
        // Iterar sobre todos los paquetes pendientes
        for (String paqueteId : paquetesPendientes) {
            // Obtener todas las inserciones posibles para este paquete
            List<InserciónPosible> insercionesPosibles = calcularInsercionesPosibles(solucion, paqueteId, contexto);
            
            if (insercionesPosibles.isEmpty()) {
                continue; // No se puede insertar este paquete
            }
            
            // Calcular regret para este paquete
            double regret = calcularRegret(insercionesPosibles);
            
            // Considerar paquetes con al menos 1 opción de inserción
            if (insercionesPosibles.size() >= 1) {
                // Seleccionar el paquete con mayor regret
                // En caso de empate, usar el de menor costo de inserción
                if (regret > maxRegret + 1e-6 ||
                    (Math.abs(regret - maxRegret) < 1e-6 && insercionesPosibles.get(0).costo < mejorCostoInserción)) {
                    maxRegret = regret;
                    mejorPaquete = paqueteId;
                    mejorCostoInserción = insercionesPosibles.get(0).costo;
                }
            }
        }
        
        return mejorPaquete;
    }
    
    /**
     * Calcula todas las inserciones posibles para un producto usando vuelos reales
     */
    private List<InserciónPosible> calcularInsercionesPosibles(Solucion solucion, String productoId, ContextoProblema contexto) {
        List<InserciónPosible> inserciones = new ArrayList<>();

        // Obtener el producto
        Paquete producto = contexto.getTodosPaquetes().stream()
            .filter(p -> p.getId().equals(productoId))
            .findFirst()
            .orElse(null);

        if (producto == null) return inserciones;

        // Buscar vuelos disponibles desde el origen del producto
        List<Vuelo> vuelosDisponibles = contexto.getVuelosDesde(producto.getAeropuertoOrigen());
        
        for (Vuelo vuelo : vuelosDisponibles) {
            // Verificar capacidad del vuelo
            if (vuelo.getPaquetesReservados() >= vuelo.getCapacidadMaxima()) {
                continue;
            }
            
            // Verificar si el vuelo está operativo
            if (!vuelo.estaOperativo()) {
                continue;
            }
            
            // Calcular costo de usar este vuelo
            double costo = calcularCostoVuelo(vuelo, producto, contexto);
            if (costo < Double.MAX_VALUE) {
                inserciones.add(new InserciónPosible(productoId, vuelo.getNumeroVuelo(), 0, costo));
            }
        }

        // Ordenar por costo (mejor inserción primero)
        inserciones.sort(Comparator.comparingDouble(ins -> ins.costo));

        return inserciones;
    }
    
    /**
     * Calcula el regret basado en el algoritmo VRPTWFL
     */
    private double calcularRegret(List<InserciónPosible> insercionesPosibles) {
        if (insercionesPosibles.isEmpty()) {
            return 0;
        }
        
        double regret = 0;
        double bigM = 10000.0; // Valor grande para penalizar inserciones faltantes
        
        // Calcular regret desde k hasta 2
        for (int i = k; i >= 2; i--) {
            if (insercionesPosibles.size() >= i) {
                // Si hay al menos i inserciones, calcular diferencia
                regret += insercionesPosibles.get(i - 1).costo - insercionesPosibles.get(0).costo;
            } else {
                // Si hay menos de i inserciones, penalizar con bigM
                regret += (i - insercionesPosibles.size()) * bigM - insercionesPosibles.get(0).costo;
            }
        }
        
        return regret;
    }
    
    
    /**
     * Inserta un producto en la mejor posición posible usando vuelos reales
     * CORRECCIÓN: Ahora trabaja con vuelos reales y capacidad
     */
    private void insertarProductoEnMejorPosicion(Solucion solucion, String productoId, ContextoProblema contexto) {
        // Obtener todas las inserciones posibles
        List<InserciónPosible> inserciones = calcularInsercionesPosibles(solucion, productoId, contexto);
        
        if (inserciones.isEmpty()) {
            System.out.println("   ⚠️  No hay inserciones posibles para " + productoId);
            return;
        }
        
        // Tomar la mejor inserción (primera en la lista ordenada)
        InserciónPosible mejorInsercion = inserciones.get(0);
        
        // Obtener el producto
        Paquete producto = contexto.getTodosPaquetes().stream()
            .filter(p -> p.getId().equals(productoId))
            .findFirst()
            .orElse(null);
        
        if (producto == null) {
            System.out.println("   ❌ Producto " + productoId + " no encontrado");
            return;
        }
        
        // Buscar el vuelo correspondiente
        Vuelo vuelo = contexto.getTodosVuelos().stream()
            .filter(v -> v.getNumeroVuelo().equals(mejorInsercion.rutaId))
            .findFirst()
            .orElse(null);
        
        if (vuelo != null) {
            // Insertar el producto en el vuelo
            insertarProductoEnVuelo(vuelo, producto, contexto);
            
            // Actualizar la solución
            actualizarSolucionConProducto(solucion, producto, vuelo);
        } else {
            System.out.println("   ❌ Vuelo " + mejorInsercion.rutaId + " no encontrado");
        }
    }
    
    /**
     * Inserta un producto (paquete) en un vuelo existente
     * CORRECCIÓN: Ahora inserta productos en vuelos reales con capacidad
     */
    private void insertarProductoEnVuelo(Vuelo vuelo, Paquete producto, ContextoProblema contexto) {
        try {
            // Verificar capacidad del vuelo
            if (vuelo.getPaquetesReservados() >= vuelo.getCapacidadMaxima()) {
                System.out.println("   ⚠️  Vuelo " + vuelo.getNumeroVuelo() + " sin capacidad para " + producto.getId());
                return;
            }
            
            // Agregar el producto al vuelo
            vuelo.setPaquetesReservados(vuelo.getPaquetesReservados() + 1);
            
            // Actualizar ubicación del producto
            producto.actualizarUbicacion(vuelo.getAeropuertoDestino());
            producto.setEstado(com.grupo5e.morapack.core.enums.EstadoGeneral.EN_TRANSITO);
            
            System.out.println("   ✅ Producto " + producto.getId() + " insertado en vuelo " + vuelo.getNumeroVuelo());
            
        } catch (Exception e) {
            System.out.println("   ❌ Error insertando producto " + producto.getId() + " en vuelo: " + e.getMessage());
        }
    }
    
    
    /**
     * Calcula el costo de usar un vuelo para un producto
     */
    private double calcularCostoVuelo(Vuelo vuelo, Paquete producto, ContextoProblema contexto) {
        try {
            // Obtener aeropuertos
            Aeropuerto origen = contexto.getAeropuerto(vuelo.getAeropuertoOrigen());
            Aeropuerto destino = contexto.getAeropuerto(vuelo.getAeropuertoDestino());
            Aeropuerto destinoProducto = contexto.getAeropuerto(producto.getAeropuertoDestino());
            
            if (origen == null || destino == null || destinoProducto == null) {
                return Double.MAX_VALUE;
            }
            
            // Calcular distancia del vuelo
            double distanciaVuelo = calcularDistanciaHaversine(origen, destino);
            
            // Calcular distancia desde destino del vuelo al destino del producto
            double distanciaFinal = calcularDistanciaHaversine(destino, destinoProducto);
            
            // Costo total = distancia del vuelo + distancia final + tiempo de vuelo
            double costo = distanciaVuelo * 0.1 + distanciaFinal * 0.1 + vuelo.getDuracionHoras() * 10.0;
            
            return costo;
            
        } catch (Exception e) {
            return Double.MAX_VALUE;
        }
    }
    
    /**
     * Calcula distancia entre dos aeropuertos usando fórmula de Haversine
     */
    private double calcularDistanciaHaversine(Aeropuerto origen, Aeropuerto destino) {
        final double R = 6371; // Radio de la Tierra en km
        
        double lat1 = Math.toRadians(origen.getLatitud());
        double lat2 = Math.toRadians(destino.getLatitud());
        double deltaLat = Math.toRadians(destino.getLatitud() - origen.getLatitud());
        double deltaLon = Math.toRadians(destino.getLongitud() - origen.getLongitud());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * Actualiza la solución con el producto insertado en el vuelo
     */
    private void actualizarSolucionConProducto(Solucion solucion, Paquete producto, Vuelo vuelo) {
        // Crear una ruta simple para el producto
        Ruta ruta = new Ruta("RUTA_" + producto.getId(), producto.getId());
        
        // Calcular costo basado en distancia y tiempo del vuelo
        double costo = vuelo.getDuracionHoras() * 10.0; // Costo simplificado
        ruta.setCostoTotal(costo);
        ruta.setTiempoTotalHoras(vuelo.getDuracionHoras());
        
        // Agregar la ruta a la solución
        solucion.agregarRuta(producto.getId(), ruta);
        
        // Actualizar ocupación del vuelo en la solución
        solucion.getOcupacionVuelos().put(vuelo.getNumeroVuelo(), vuelo.getPaquetesReservados());
    }
    
    
    @Override
    public String getNombre() {
        return getName();
    }
    
    @Override
    public String getDescripcion() {
        return "Regret-" + k + " insertion: selecciona el paquete con mayor regret (diferencia entre mejor y k-ésima mejor inserción)";
    }
}
