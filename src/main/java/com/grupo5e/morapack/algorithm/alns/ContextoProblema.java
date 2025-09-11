package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.core.constants.ConstantesMoraPack;
import java.util.*;

/**
 * Contexto del problema que contiene toda la información necesaria
 * para que los operadores ALNS puedan funcionar correctamente.
 */
public class ContextoProblema {
    private final Map<String, Paquete> paquetes;
    private final Map<String, Aeropuerto> aeropuertos;
    private final Map<String, Vuelo> vuelos;
    private final Set<Continente> continentes;
    private final Map<String, List<Vuelo>> grafoVuelos;
    private final Map<String, String> aeropuertoAContinente;
    private final List<String> sedesMoraPack;
    
    public ContextoProblema(List<Paquete> listaPaquetes, List<Aeropuerto> listaAeropuertos, 
                           List<Vuelo> listaVuelos, Set<Continente> continentes) {
        
        // Indexar paquetes por ID
        this.paquetes = new HashMap<>();
        for (Paquete paquete : listaPaquetes) {
            this.paquetes.put(paquete.getId(), paquete);
        }
        
        // Indexar aeropuertos por código IATA
        this.aeropuertos = new HashMap<>();
        for (Aeropuerto aeropuerto : listaAeropuertos) {
            this.aeropuertos.put(aeropuerto.getCodigoIATA(), aeropuerto);
        }
        
        // Indexar vuelos por número de vuelo
        this.vuelos = new HashMap<>();
        for (Vuelo vuelo : listaVuelos) {
            this.vuelos.put(vuelo.getNumeroVuelo(), vuelo);
        }
        
        this.continentes = new HashSet<>(continentes);
        
        // Crear grafo de vuelos (aeropuerto origen -> lista de vuelos)
        this.grafoVuelos = new HashMap<>();
        for (Vuelo vuelo : listaVuelos) {
            this.grafoVuelos.computeIfAbsent(vuelo.getAeropuertoOrigen(), k -> new ArrayList<>())
                           .add(vuelo);
        }
        
        // Crear mapeo aeropuerto -> continente
        this.aeropuertoAContinente = new HashMap<>();
        for (Continente continente : continentes) {
            for (String codigoIATA : continente.getCodigosIATAAeropuertos()) {
                this.aeropuertoAContinente.put(codigoIATA, continente.getCodigo());
            }
        }
        
        // Identificar sedes de MoraPack
        this.sedesMoraPack = listaAeropuertos.stream()
                .filter(Aeropuerto::isEsSedeMoraPack)
                .map(Aeropuerto::getCodigoIATA)
                .toList();
    }
    
    // Getters para acceso a los datos
    public Paquete getPaquete(String id) {
        return paquetes.get(id);
    }
    
    public Aeropuerto getAeropuerto(String codigoIATA) {
        return aeropuertos.get(codigoIATA);
    }
    
    public Vuelo getVuelo(String numeroVuelo) {
        return vuelos.get(numeroVuelo);
    }
    
    public List<Vuelo> getVuelosDesde(String aeropuertoOrigen) {
        return grafoVuelos.getOrDefault(aeropuertoOrigen, new ArrayList<>());
    }
    
    public List<Vuelo> getVuelosHacia(String aeropuertoDestino) {
        return vuelos.values().stream()
                .filter(v -> v.getAeropuertoDestino().equals(aeropuertoDestino))
                .toList();
    }
    
    public String getContinentePorAeropuerto(String codigoIATA) {
        return aeropuertoAContinente.get(codigoIATA);
    }
    
    public boolean sonMismoContinente(String aeropuerto1, String aeropuerto2) {
        String continente1 = getContinentePorAeropuerto(aeropuerto1);
        String continente2 = getContinentePorAeropuerto(aeropuerto2);
        return continente1 != null && continente1.equals(continente2);
    }
    
    public int calcularDiasPlazo(String aeropuertoOrigen, String aeropuertoDestino) {
        return sonMismoContinente(aeropuertoOrigen, aeropuertoDestino) ? 2 : 3;
    }
    
    public List<String> getSedesMoraPack() {
        return new ArrayList<>(sedesMoraPack);
    }
    
    public Collection<Paquete> getTodosPaquetes() {
        return paquetes.values();
    }
    
    public Collection<Aeropuerto> getTodosAeropuertos() {
        return aeropuertos.values();
    }
    
    public Collection<Vuelo> getTodosVuelos() {
        return vuelos.values();
    }
    
    public Set<Continente> getContinentes() {
        return new HashSet<>(continentes);
    }
    
    public Map<String, List<Vuelo>> getGrafoVuelos() {
        return new HashMap<>(grafoVuelos);
    }
    
    /**
     * Encuentra la ruta más corta entre dos aeropuertos usando BFS
     */
    public List<String> encontrarRutaMasCorta(String origen, String destino) {
        if (origen.equals(destino)) {
            return Collections.singletonList(origen);
        }
        
        Queue<List<String>> cola = new LinkedList<>();
        Set<String> visitados = new HashSet<>();
        
        cola.offer(Collections.singletonList(origen));
        visitados.add(origen);
        
        while (!cola.isEmpty()) {
            List<String> rutaActual = cola.poll();
            String ultimoAeropuerto = rutaActual.get(rutaActual.size() - 1);
            
            for (Vuelo vuelo : getVuelosDesde(ultimoAeropuerto)) {
                String siguienteAeropuerto = vuelo.getAeropuertoDestino();
                
                if (siguienteAeropuerto.equals(destino)) {
                    List<String> rutaCompleta = new ArrayList<>(rutaActual);
                    rutaCompleta.add(siguienteAeropuerto);
                    return rutaCompleta;
                }
                
                if (!visitados.contains(siguienteAeropuerto)) {
                    visitados.add(siguienteAeropuerto);
                    List<String> nuevaRuta = new ArrayList<>(rutaActual);
                    nuevaRuta.add(siguienteAeropuerto);
                    cola.offer(nuevaRuta);
                }
            }
        }
        
        // No se encontró ruta
        return Collections.emptyList();
    }
    
    /**
     * Encuentra todos los vuelos disponibles entre dos aeropuertos (directos)
     */
    public List<Vuelo> getVuelosDirectos(String origen, String destino) {
        return getVuelosDesde(origen).stream()
                .filter(vuelo -> vuelo.getAeropuertoDestino().equals(destino))
                .toList();
    }
    
    /**
     * Calcula el costo estimado de una ruta basado en distancia y tipo de vuelo
     */
    public double calcularCostoEstimado(List<String> rutaAeropuertos) {
        if (rutaAeropuertos.size() < 2) {
            return 0.0;
        }
        
        double costoTotal = 0.0;
        for (int i = 0; i < rutaAeropuertos.size() - 1; i++) {
            String origen = rutaAeropuertos.get(i);
            String destino = rutaAeropuertos.get(i + 1);
            
            boolean mismoContinente = sonMismoContinente(origen, destino);
            costoTotal += mismoContinente ? 
                ConstantesMoraPack.COSTO_BASE_VUELO_MISMO_CONTINENTE : 
                ConstantesMoraPack.COSTO_BASE_VUELO_DISTINTO_CONTINENTE;
        }
        
        return costoTotal;
    }
    
    /**
     * Calcula el tiempo estimado de una ruta
     */
    public double calcularTiempoEstimado(List<String> rutaAeropuertos) {
        if (rutaAeropuertos.size() < 2) {
            return 0.0;
        }
        
        double tiempoTotal = 0.0;
        for (int i = 0; i < rutaAeropuertos.size() - 1; i++) {
            String origen = rutaAeropuertos.get(i);
            String destino = rutaAeropuertos.get(i + 1);
            
            boolean mismoContinente = sonMismoContinente(origen, destino);
            tiempoTotal += mismoContinente ? 
                ConstantesMoraPack.TIEMPO_VUELO_MISMO_CONTINENTE_HORAS : 
                ConstantesMoraPack.TIEMPO_VUELO_DISTINTO_CONTINENTE_HORAS;
        }
        
        return tiempoTotal;
    }
}
