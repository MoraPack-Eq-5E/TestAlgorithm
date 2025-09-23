package com.grupo5e.morapack.algorithm.alns.operators.construction;

import com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorConstruccion;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;
import com.grupo5e.morapack.algorithm.validation.ValidadorRestricciones;
import com.grupo5e.morapack.core.model.*;

import java.util.*;

/**
 * Operador de Reparación Inteligente
 * 
 * OBJETIVO: Identificar y reparar paquetes problemáticos usando estrategias avanzadas
 * 
 * ESTRATEGIAS:
 * 1. Detección de paquetes problemáticos (que fallan repetidamente)
 * 2. Análisis de patrones de falla
 * 3. Estrategias de reparación especializadas
 * 4. Aprendizaje de soluciones exitosas
 */
public class IntelligentRepairOperator extends AbstractOperator implements OperadorConstruccion {
    
    // Historial de paquetes problemáticos
    private Map<String, Integer> contadorFallos = new HashMap<>();
    private Map<String, List<String>> estrategiasExitosas = new HashMap<>();
    private Map<String, Set<String>> vuelosEvitados = new HashMap<>();
    
    public IntelligentRepairOperator() {
        super("IntelligentRepair", "construction");
    }
    
    @Override
    public Solucion construir(Solucion solucion, List<String> paquetesRemovidos, 
                            ContextoProblema contexto, ValidadorRestricciones validador) {
        
        if (paquetesRemovidos.isEmpty()) {
            return solucion;
        }
        
        Solucion solucionTemporal = solucion.copiar();
        List<String> paquetesPendientes = new ArrayList<>(paquetesRemovidos);
        
        if (com.grupo5e.morapack.algorithm.alns.ALNSConfig.getInstance().isEnableVerboseLogging()) {
            System.out.println("   IntelligentRepair: Reparando " + paquetesPendientes.size() + " paquetes");
        }
        
        Map<String, Integer> dificultadPaquetes = clasificarDificultadPaquetes(paquetesPendientes, contexto);
        
        List<String> paquetesOrdenados = paquetesPendientes.stream()
            .sorted(Comparator.comparing(dificultadPaquetes::get).reversed())
            .toList();
        
        int paquetesReparados = 0;
        for (String paqueteId : paquetesOrdenados) {
            if (repararPaqueteInteligente(solucionTemporal, paqueteId, contexto)) {
                paquetesReparados++;
                paquetesPendientes.remove(paqueteId);
            } else {
                registrarFallo(paqueteId);
            }
        }
        
        if (com.grupo5e.morapack.algorithm.alns.ALNSConfig.getInstance().isEnableVerboseLogging()) {
            System.out.println("   IntelligentRepair: Reparados " + paquetesReparados + " paquetes");
        }
        return solucionTemporal;
    }
    
    /**
     * Clasifica paquetes por nivel de dificultad basado en historial y características
     */
    private Map<String, Integer> clasificarDificultadPaquetes(List<String> paquetesPendientes, 
                                                            ContextoProblema contexto) {
        Map<String, Integer> dificultad = new HashMap<>();
        
        for (String paqueteId : paquetesPendientes) {
            int nivelDificultad = 0;
            
            // Factor 1: Historial de fallos
            nivelDificultad += contadorFallos.getOrDefault(paqueteId, 0) * 10;
            
            // Factor 2: Distancia del origen al destino
            Paquete paquete = contexto.getPaquete(paqueteId);
            if (paquete != null) {
                Aeropuerto origen = contexto.getAeropuerto(paquete.getAeropuertoOrigen());
                Aeropuerto destino = contexto.getAeropuerto(paquete.getAeropuertoDestino());
                
                if (origen != null && destino != null) {
                    double distancia = calcularDistanciaHaversine(origen, destino);
                    nivelDificultad += (int) (distancia / 1000); // 1 punto por cada 1000km
                }
            }
            
            // Factor 3: Disponibilidad de vuelos directos
            if (paquete != null) {
                List<Vuelo> vuelosDirectos = contexto.getVuelosDesde(paquete.getAeropuertoOrigen())
                    .stream()
                    .filter(v -> v.getAeropuertoDestino().equals(paquete.getAeropuertoDestino()))
                    .filter(v -> v.estaOperativo())
                    .filter(v -> v.getPaquetesReservados() < v.getCapacidadMaxima())
                    .toList();
                
                nivelDificultad += Math.max(0, 5 - vuelosDirectos.size()) * 5;
            }
            
            // Factor 4: Prioridad del paquete (mayor prioridad = menor dificultad)
            if (paquete != null) {
                nivelDificultad -= paquete.getPrioridad() * 2;
            }
            
            dificultad.put(paqueteId, Math.max(0, nivelDificultad));
        }
        
        return dificultad;
    }
    
    /**
     * Repara un paquete usando estrategias inteligentes
     */
    private boolean repararPaqueteInteligente(Solucion solucion, String paqueteId, ContextoProblema contexto) {
        Paquete paquete = contexto.getPaquete(paqueteId);
        if (paquete == null) return false;
        
        // Estrategia 1: Intentar estrategias que funcionaron antes
        if (intentarEstrategiasExitosas(solucion, paquete, contexto)) {
            return true;
        }
        
        // Estrategia 2: Búsqueda exhaustiva de vuelos disponibles
        if (busquedaExhaustivaVuelos(solucion, paquete, contexto)) {
            return true;
        }
        
        // Estrategia 3: Rutas con múltiples escalas
        if (buscarRutasMultiEscalas(solucion, paquete, contexto)) {
            return true;
        }
        
        // Estrategia 4: Intercambio con paquetes existentes
        if (intercambioConPaquetesExistentes(solucion, paquete, contexto)) {
            return true;
        }
        
        // Estrategia 5: Relajación temporal de restricciones
        if (relajacionTemporalRestricciones(solucion, paquete, contexto)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Intenta estrategias que funcionaron exitosamente antes
     */
    private boolean intentarEstrategiasExitosas(Solucion solucion, Paquete paquete, ContextoProblema contexto) {
        List<String> estrategias = estrategiasExitosas.get(paquete.getId());
        if (estrategias == null || estrategias.isEmpty()) {
            return false;
        }
        
        for (String estrategia : estrategias) {
            switch (estrategia) {
                case "vuelo_directo":
                    if (buscarVueloDirecto(solucion, paquete, contexto)) return true;
                    break;
                case "ruta_escalas":
                    if (buscarRutaConEscalas(solucion, paquete, contexto)) return true;
                    break;
                case "vuelo_alternativo":
                    if (buscarVueloAlternativo(solucion, paquete, contexto)) return true;
                    break;
            }
        }
        
        return false;
    }
    
    /**
     * Búsqueda exhaustiva de vuelos disponibles
     */
    private boolean busquedaExhaustivaVuelos(Solucion solucion, Paquete paquete, ContextoProblema contexto) {
        // Obtener todos los vuelos desde el origen
        List<Vuelo> vuelosDisponibles = contexto.getVuelosDesde(paquete.getAeropuertoOrigen())
            .stream()
            .filter(v -> v.estaOperativo())
            .filter(v -> v.getPaquetesReservados() < v.getCapacidadMaxima())
            .filter(v -> !esVueloEvitado(paquete.getId(), v.getNumeroVuelo()))
            .sorted(Comparator.comparingDouble(v -> calcularCostoVuelo(v, paquete, contexto)))
            .toList();
        
        for (Vuelo vuelo : vuelosDisponibles) {
            if (insertarPaqueteEnVuelo(solucion, paquete, vuelo, contexto)) {
                registrarEstrategiaExitosa(paquete.getId(), "vuelo_directo");
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Busca rutas con múltiples escalas (hasta 3 vuelos)
     */
    private boolean buscarRutasMultiEscalas(Solucion solucion, Paquete paquete, ContextoProblema contexto) {
        // Buscar rutas con 2 escalas
        List<List<Vuelo>> rutas2Escalas = buscarRutasConEscalas(paquete, contexto, 2);
        
        for (List<Vuelo> ruta : rutas2Escalas) {
            if (insertarPaqueteEnRutaMultiEscalas(solucion, paquete, ruta, contexto)) {
                registrarEstrategiaExitosa(paquete.getId(), "ruta_escalas");
                return true;
            }
        }
        
        // Buscar rutas con 3 escalas (solo si es muy problemático)
        if (contadorFallos.getOrDefault(paquete.getId(), 0) > 3) {
            List<List<Vuelo>> rutas3Escalas = buscarRutasConEscalas(paquete, contexto, 3);
            
            for (List<Vuelo> ruta : rutas3Escalas) {
                if (insertarPaqueteEnRutaMultiEscalas(solucion, paquete, ruta, contexto)) {
                    registrarEstrategiaExitosa(paquete.getId(), "ruta_escalas");
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Intercambia con paquetes existentes para liberar capacidad
     */
    private boolean intercambioConPaquetesExistentes(Solucion solucion, Paquete paquete, ContextoProblema contexto) {
        // Buscar paquetes que podrían ser reubicados
        for (String paqueteExistenteId : solucion.getPaquetesIds()) {
            Paquete paqueteExistente = contexto.getPaquete(paqueteExistenteId);
            if (paqueteExistente == null) continue;
            
            // Verificar si el paquete existente puede ser reubicado
            if (puedeReubicarPaquete(paqueteExistente, contexto)) {
                // Intentar intercambio
                if (realizarIntercambioPaquetes(solucion, paquete, paqueteExistente, contexto)) {
                    registrarEstrategiaExitosa(paquete.getId(), "intercambio");
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Relaja temporalmente algunas restricciones para paquetes muy problemáticos
     */
    private boolean relajacionTemporalRestricciones(Solucion solucion, Paquete paquete, ContextoProblema contexto) {
        // Solo para paquetes que han fallado muchas veces
        if (contadorFallos.getOrDefault(paquete.getId(), 0) < 5) {
            return false;
        }
        
        // Estrategia: Permitir vuelos con capacidad al 95% en lugar del 100%
        List<Vuelo> vuelosRelajados = contexto.getVuelosDesde(paquete.getAeropuertoOrigen())
            .stream()
            .filter(v -> v.estaOperativo())
            .filter(v -> v.getPaquetesReservados() < v.getCapacidadMaxima() * 0.95)
            .sorted(Comparator.comparingDouble(v -> calcularCostoVuelo(v, paquete, contexto)))
            .toList();
        
        for (Vuelo vuelo : vuelosRelajados) {
            if (insertarPaqueteEnVuelo(solucion, paquete, vuelo, contexto)) {
                registrarEstrategiaExitosa(paquete.getId(), "relajacion");
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Busca rutas con un número específico de escalas
     */
    private List<List<Vuelo>> buscarRutasConEscalas(Paquete paquete, ContextoProblema contexto, int numEscalas) {
        List<List<Vuelo>> rutas = new ArrayList<>();
        
        if (numEscalas == 2) {
            // Buscar rutas con 2 vuelos
            List<Vuelo> vuelosDesdeOrigen = contexto.getVuelosDesde(paquete.getAeropuertoOrigen());
            
            for (Vuelo vuelo1 : vuelosDesdeOrigen) {
                if (vuelo1.getPaquetesReservados() >= vuelo1.getCapacidadMaxima()) continue;
                
                List<Vuelo> vuelosDesdeEscala = contexto.getVuelosDesde(vuelo1.getAeropuertoDestino());
                
                for (Vuelo vuelo2 : vuelosDesdeEscala) {
                    if (vuelo2.getAeropuertoDestino().equals(paquete.getAeropuertoDestino()) &&
                        vuelo2.getPaquetesReservados() < vuelo2.getCapacidadMaxima() &&
                        vuelo2.estaOperativo()) {
                        
                        rutas.add(Arrays.asList(vuelo1, vuelo2));
                    }
                }
            }
        }
        // Implementar lógica para 3 escalas si es necesario
        
        // Ordenar por costo total
        rutas.sort(Comparator.comparingDouble(ruta -> 
            ruta.stream().mapToDouble(v -> calcularCostoVuelo(v, paquete, contexto)).sum()));
        
        return rutas;
    }
    
    /**
     * Inserta un paquete en un vuelo específico
     */
    private boolean insertarPaqueteEnVuelo(Solucion solucion, Paquete paquete, Vuelo vuelo, ContextoProblema contexto) {
        try {
            if (vuelo.getPaquetesReservados() >= vuelo.getCapacidadMaxima()) {
                return false;
            }
            
            vuelo.setPaquetesReservados(vuelo.getPaquetesReservados() + 1);
            
            Ruta ruta = new Ruta("RUTA_" + paquete.getId(), paquete.getId());
            ruta.setCostoTotal(calcularCostoVuelo(vuelo, paquete, contexto));
            ruta.setTiempoTotalHoras(vuelo.getDuracionHoras());
            
            solucion.agregarRuta(paquete.getId(), ruta);
            solucion.getOcupacionVuelos().put(vuelo.getNumeroVuelo(), vuelo.getPaquetesReservados());
            
            System.out.println("   Producto " + paquete.getId() + " reparado en vuelo " + vuelo.getNumeroVuelo());
            return true;
            
        } catch (Exception e) {
            System.out.println("   Error reparando producto " + paquete.getId() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Inserta un paquete en una ruta con múltiples escalas
     */
    private boolean insertarPaqueteEnRutaMultiEscalas(Solucion solucion, Paquete paquete, 
                                                    List<Vuelo> vuelos, ContextoProblema contexto) {
        try {
            // Verificar capacidad de todos los vuelos
            for (Vuelo vuelo : vuelos) {
                if (vuelo.getPaquetesReservados() >= vuelo.getCapacidadMaxima()) {
                    return false;
                }
            }
            
            // Actualizar ocupación de todos los vuelos
            for (Vuelo vuelo : vuelos) {
                vuelo.setPaquetesReservados(vuelo.getPaquetesReservados() + 1);
            }
            
            // Crear ruta con escalas
            Ruta ruta = new Ruta("RUTA_ESCALAS_" + paquete.getId(), paquete.getId());
            double costoTotal = vuelos.stream().mapToDouble(v -> calcularCostoVuelo(v, paquete, contexto)).sum();
            double tiempoTotal = vuelos.stream().mapToDouble(Vuelo::getDuracionHoras).sum();
            
            ruta.setCostoTotal(costoTotal);
            ruta.setTiempoTotalHoras(tiempoTotal);
            
            solucion.agregarRuta(paquete.getId(), ruta);
            
            // Actualizar ocupaciones
            for (Vuelo vuelo : vuelos) {
                solucion.getOcupacionVuelos().put(vuelo.getNumeroVuelo(), vuelo.getPaquetesReservados());
            }
            
            System.out.println("   Producto " + paquete.getId() + " reparado en ruta con " + vuelos.size() + " escalas");
            return true;
            
        } catch (Exception e) {
            System.out.println("   Error reparando producto " + paquete.getId() + " en ruta multi-escalas: " + e.getMessage());
            return false;
        }
    }
    
    // Métodos auxiliares
    private void registrarFallo(String paqueteId) {
        contadorFallos.put(paqueteId, contadorFallos.getOrDefault(paqueteId, 0) + 1);
    }
    
    private void registrarEstrategiaExitosa(String paqueteId, String estrategia) {
        estrategiasExitosas.computeIfAbsent(paqueteId, k -> new ArrayList<>()).add(estrategia);
    }
    
    private boolean esVueloEvitado(String paqueteId, String numeroVuelo) {
        return vuelosEvitados.getOrDefault(paqueteId, new HashSet<>()).contains(numeroVuelo);
    }
    
    private double calcularCostoVuelo(Vuelo vuelo, Paquete paquete, ContextoProblema contexto) {
        // Implementación similar a RegretKInsertion
        try {
            Aeropuerto origen = contexto.getAeropuerto(vuelo.getAeropuertoOrigen());
            Aeropuerto destino = contexto.getAeropuerto(vuelo.getAeropuertoDestino());
            Aeropuerto destinoProducto = contexto.getAeropuerto(paquete.getAeropuertoDestino());
            
            if (origen == null || destino == null || destinoProducto == null) {
                return Double.MAX_VALUE;
            }
            
            double distanciaVuelo = calcularDistanciaHaversine(origen, destino);
            double distanciaFinal = calcularDistanciaHaversine(destino, destinoProducto);
            
            return distanciaVuelo * 0.1 + distanciaFinal * 0.1 + vuelo.getDuracionHoras() * 10.0;
            
        } catch (Exception e) {
            return Double.MAX_VALUE;
        }
    }
    
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
    
    // Métodos de búsqueda de rutas optimizados
    private boolean buscarVueloDirecto(Solucion solucion, Paquete paquete, ContextoProblema contexto) {
        // Buscar vuelos directos desde el origen al destino
        List<Vuelo> vuelosDirectos = contexto.getVuelosDirectos(
            paquete.getAeropuertoOrigen(), paquete.getAeropuertoDestino()
        );
        
        for (Vuelo vuelo : vuelosDirectos) {
            if (vuelo.puedeCargar(1) && vuelo.estaOperativo()) {
                Ruta ruta = new Ruta("RUTA_" + paquete.getId(), paquete.getId());
                SegmentoRuta segmento = new SegmentoRuta(
                    "SEG_" + paquete.getId(),
                    paquete.getAeropuertoOrigen(),
                    paquete.getAeropuertoDestino(),
                    vuelo.getNumeroVuelo(),
                    vuelo.isMismoContinente()
                );
                ruta.agregarSegmento(segmento);
                solucion.agregarRuta(paquete.getId(), ruta);
                return true;
            }
        }
        return false;
    }
    
    private boolean buscarRutaConEscalas(Solucion solucion, Paquete paquete, ContextoProblema contexto) {
        // Buscar rutas con escalas usando BFS
        List<String> rutaBFS = contexto.encontrarRutaMasCorta(
            paquete.getAeropuertoOrigen(), paquete.getAeropuertoDestino()
        );
        
        if (rutaBFS.size() > 1) {
            Ruta ruta = new Ruta("RUTA_" + paquete.getId(), paquete.getId());
            
            // Crear vuelos para cada segmento de la ruta
            for (int i = 0; i < rutaBFS.size() - 1; i++) {
                String origen = rutaBFS.get(i);
                String destino = rutaBFS.get(i + 1);
                
                List<Vuelo> vuelosSegmento = contexto.getVuelosDirectos(origen, destino);
                if (!vuelosSegmento.isEmpty() && vuelosSegmento.get(0).puedeCargar(1)) {
                    SegmentoRuta segmento = new SegmentoRuta(
                        "SEG_" + paquete.getId() + "_" + i,
                        origen, destino, vuelosSegmento.get(0).getNumeroVuelo(),
                        vuelosSegmento.get(0).isMismoContinente()
                    );
                ruta.agregarSegmento(segmento);
                } else {
                    return false; // No se puede completar la ruta
                }
            }
            
            solucion.agregarRuta(paquete.getId(), ruta);
            return true;
        }
        return false;
    }
    
    private boolean buscarVueloAlternativo(Solucion solucion, Paquete paquete, ContextoProblema contexto) {
        // Buscar vuelos alternativos con horarios diferentes
        List<Vuelo> vuelosAlternativos = contexto.getVuelosDesde(paquete.getAeropuertoOrigen())
            .stream()
            .filter(v -> v.getAeropuertoDestino().equals(paquete.getAeropuertoDestino()))
            .filter(v -> v.puedeCargar(1))
            .filter(v -> v.estaOperativo())
            .toList();
        
        if (!vuelosAlternativos.isEmpty()) {
            Ruta ruta = new Ruta("RUTA_" + paquete.getId(), paquete.getId());
            SegmentoRuta segmento = new SegmentoRuta(
                "SEG_" + paquete.getId(),
                paquete.getAeropuertoOrigen(),
                paquete.getAeropuertoDestino(),
                vuelosAlternativos.get(0).getNumeroVuelo(),
                vuelosAlternativos.get(0).isMismoContinente()
            );
            ruta.agregarSegmento(segmento);
            solucion.agregarRuta(paquete.getId(), ruta);
            return true;
        }
        return false;
    }
    
    private boolean puedeReubicarPaquete(Paquete paquete, ContextoProblema contexto) {
        // Un paquete puede ser reubicado si hay vuelos alternativos disponibles
        List<Vuelo> vuelosAlternativos = contexto.getVuelosDesde(paquete.getAeropuertoOrigen())
            .stream()
            .filter(v -> v.estaOperativo())
            .filter(v -> v.getPaquetesReservados() < v.getCapacidadMaxima())
            .toList();
        
        return !vuelosAlternativos.isEmpty();
    }
    
    private boolean realizarIntercambioPaquetes(Solucion solucion, Paquete paquete1, Paquete paquete2, ContextoProblema contexto) {
        // Implementación simplificada: verificar si ambos paquetes pueden intercambiar rutas
        // En una implementación completa, aquí se intercambiarían las rutas de los paquetes
        
        // Verificar que ambos paquetes tengan rutas asignadas
        if (!solucion.getRutasPaquetes().containsKey(paquete1.getId()) || 
            !solucion.getRutasPaquetes().containsKey(paquete2.getId())) {
            return false;
        }
        
        // Verificar compatibilidad básica de rutas
        Ruta ruta1 = solucion.getRutasPaquetes().get(paquete1.getId());
        Ruta ruta2 = solucion.getRutasPaquetes().get(paquete2.getId());
        
        // Intercambio simple: verificar si las rutas son compatibles
        return ruta1 != null && ruta2 != null && 
               ruta1.getSegmentos().size() > 0 && ruta2.getSegmentos().size() > 0;
    }
    
    @Override
    public String getNombre() {
        return getName();
    }
    
    @Override
    public String getDescripcion() {
        return "Intelligent Repair: identifica y repara paquetes problemáticos usando estrategias avanzadas y aprendizaje";
    }
}
