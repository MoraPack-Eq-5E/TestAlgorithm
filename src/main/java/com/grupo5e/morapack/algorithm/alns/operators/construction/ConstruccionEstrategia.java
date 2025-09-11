package com.grupo5e.morapack.algorithm.alns.operators.construction;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorConstruccion;
import com.grupo5e.morapack.algorithm.validation.ValidadorRestricciones;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;

import java.util.*;

/**
 * Constructor unificado que reemplaza los 4 constructores duplicados (Voraz, MenorCosto, MenorTiempo, Balanceado).
 * Usa patrón Strategy para diferentes criterios de selección de ruta.
 */
public class ConstruccionEstrategia implements OperadorConstruccion {
    
    private final EstrategiaSeleccion estrategia;
    private final String nombre;
    private final String descripcion;
    
    // Estrategias disponibles
    public enum TipoEstrategia {
        VORAZ("Selecciona la primera ruta factible encontrada"),
        MENOR_COSTO("Prioriza rutas con menor costo total"),
        MENOR_TIEMPO("Prioriza rutas con menor tiempo de viaje"),
        BALANCEADA("Balance entre costo (60%) y tiempo (40%)"),
        INTELIGENTE("Respeta restricciones de capacidad y busca rutas alternativas");
        
        private final String descripcion;
        
        TipoEstrategia(String descripcion) {
            this.descripcion = descripcion;
        }
        
        public String getDescripcion() { return descripcion; }
    }
    
    public ConstruccionEstrategia(TipoEstrategia tipo) {
        this.estrategia = crearEstrategia(tipo);
        this.nombre = "ConstruccionEstrategia_" + tipo.name();
        this.descripcion = tipo.getDescripcion();
    }
    
    @Override
    public Solucion construir(Solucion solucionParcial, List<String> paquetesRemovidos,
                             ContextoProblema contexto, ValidadorRestricciones validador) {
        
        Solucion nuevaSolucion = solucionParcial.copiar();
        int paquetesNoRuteados = 0;
        
        // Ordenar paquetes según estrategia (si aplica)
        List<String> paquetesOrdenados = estrategia.ordenarPaquetes(paquetesRemovidos, contexto);
        
        for (String paqueteId : paquetesOrdenados) {
            Paquete paquete = contexto.getPaquete(paqueteId);
            if (paquete == null) {
                System.err.println("⚠️ Paquete no encontrado: " + paqueteId);
                continue;
            }
            
            Ruta mejorRuta = estrategia.seleccionarMejorRuta(paqueteId, paquete, contexto, validador, nuevaSolucion);
            
            if (mejorRuta != null) {
                nuevaSolucion.agregarRuta(paqueteId, mejorRuta);
                nuevaSolucion.recalcularMetricas();
            } else {
                paquetesNoRuteados++;
                estrategia.manejarPaqueteNoRuteado(paqueteId, paquete, nuevaSolucion);
            }
        }
        
        if (paquetesNoRuteados > 0) {
            System.out.println("📊 " + nombre + " - Paquetes no ruteados: " + paquetesNoRuteados + "/" + paquetesRemovidos.size());
        }
        
        return nuevaSolucion;
    }
    
    @Override
    public String getNombre() {
        return nombre;
    }
    
    @Override
    public String getDescripcion() {
        return descripcion;
    }
    
    // ================================================================================
    // ESTRATEGIAS ESPECÍFICAS
    // ================================================================================
    
    private EstrategiaSeleccion crearEstrategia(TipoEstrategia tipo) {
        return switch (tipo) {
            case VORAZ -> new EstrategiaVoraz();
            case MENOR_COSTO -> new EstrategiaMenorCosto();
            case MENOR_TIEMPO -> new EstrategiaMenorTiempo();
            case BALANCEADA -> new EstrategiaBalanceada();
            case INTELIGENTE -> new EstrategiaInteligente();
        };
    }
    
    // ================================================================================
    // INTERFACE STRATEGY
    // ================================================================================
    
    private interface EstrategiaSeleccion {
        List<String> ordenarPaquetes(List<String> paquetes, ContextoProblema contexto);
        Ruta seleccionarMejorRuta(String paqueteId, Paquete paquete, ContextoProblema contexto, 
                                 ValidadorRestricciones validador, Solucion solucionActual);
        void manejarPaqueteNoRuteado(String paqueteId, Paquete paquete, Solucion solucion);
    }
    
    // ================================================================================
    // ESTRATEGIA VORAZ (Primera ruta factible)
    // ================================================================================
    
    private static class EstrategiaVoraz implements EstrategiaSeleccion {
        
        @Override
        public List<String> ordenarPaquetes(List<String> paquetes, ContextoProblema contexto) {
            return new ArrayList<>(paquetes); // Sin ordenamiento especial
        }
        
        @Override
        public Ruta seleccionarMejorRuta(String paqueteId, Paquete paquete, ContextoProblema contexto, 
                                        ValidadorRestricciones validador, Solucion solucionActual) {
            
            // Buscar primera ruta directa factible
            List<Vuelo> vuelosDirectos = contexto.getVuelosDirectos(
                paquete.getAeropuertoOrigen(), paquete.getAeropuertoDestino()
            );
            
            for (Vuelo vuelo : vuelosDirectos) {
                Ruta rutaDirecta = ConstruccionEstrategia.crearRutaDirecta(vuelo, paquete);
                if (ConstruccionEstrategia.esRutaBasicamenteFactible(rutaDirecta)) {
                    return rutaDirecta;
                }
            }
            
            // Si no hay directa, buscar con una conexión usando BFS
            List<String> rutaBFS = contexto.encontrarRutaMasCorta(
                paquete.getAeropuertoOrigen(), paquete.getAeropuertoDestino()
            );
            
            if (rutaBFS.size() >= 3) {
                return ConstruccionEstrategia.crearRutaConConexiones(rutaBFS, contexto, paquete);
            }
            
            return null;
        }
        
        @Override
        public void manejarPaqueteNoRuteado(String paqueteId, Paquete paquete, Solucion solucion) {
            // Estrategia voraz: crear ruta básica aunque viole restricciones
            Ruta rutaBasica = ConstruccionEstrategia.crearRutaBasicaFallback(paquete);
            solucion.agregarRuta(paqueteId, rutaBasica);
            solucion.setViolacionesRestricciones(solucion.getViolacionesRestricciones() + 1);
            solucion.setEsFactible(false);
        }
    }
    
    // ================================================================================
    // ESTRATEGIA MENOR COSTO
    // ================================================================================
    
    private static class EstrategiaMenorCosto implements EstrategiaSeleccion {
        
        @Override
        public List<String> ordenarPaquetes(List<String> paquetes, ContextoProblema contexto) {
            // Ordenar por distancia (mayor distancia primero, más opciones)
            return paquetes.stream()
                .sorted((p1, p2) -> {
                    Paquete paq1 = contexto.getPaquete(p1);
                    Paquete paq2 = contexto.getPaquete(p2);
                    if (paq1 != null && paq2 != null) {
                        boolean intercontinental1 = !contexto.sonMismoContinente(paq1.getAeropuertoOrigen(), paq1.getAeropuertoDestino());
                        boolean intercontinental2 = !contexto.sonMismoContinente(paq2.getAeropuertoOrigen(), paq2.getAeropuertoDestino());
                        return Boolean.compare(intercontinental2, intercontinental1); // Intercontinentales primero
                    }
                    return 0;
                })
                .toList();
        }
        
        @Override
        public Ruta seleccionarMejorRuta(String paqueteId, Paquete paquete, ContextoProblema contexto, 
                                        ValidadorRestricciones validador, Solucion solucionActual) {
            
            List<Ruta> rutasFactibles = ConstruccionEstrategia.generarRutasFactibles(paquete, contexto);
            
            return rutasFactibles.stream()
                .min(Comparator.comparing(Ruta::getCostoTotal))
                .orElse(null);
        }
        
        @Override
        public void manejarPaqueteNoRuteado(String paqueteId, Paquete paquete, Solucion solucion) {
            // No agregar si no hay ruta de costo aceptable
            System.out.println("💰 No se encontró ruta de costo aceptable para " + paqueteId);
        }
    }
    
    // ================================================================================
    // ESTRATEGIA MENOR TIEMPO  
    // ================================================================================
    
    private static class EstrategiaMenorTiempo implements EstrategiaSeleccion {
        
        @Override
        public List<String> ordenarPaquetes(List<String> paquetes, ContextoProblema contexto) {
            // Ordenar por prioridad (alta prioridad primero)
            return paquetes.stream()
                .sorted((p1, p2) -> {
                    Paquete paq1 = contexto.getPaquete(p1);
                    Paquete paq2 = contexto.getPaquete(p2);
                    if (paq1 != null && paq2 != null) {
                        return Integer.compare(paq2.getPrioridad(), paq1.getPrioridad()); // Descendente
                    }
                    return 0;
                })
                .toList();
        }
        
        @Override
        public Ruta seleccionarMejorRuta(String paqueteId, Paquete paquete, ContextoProblema contexto, 
                                        ValidadorRestricciones validador, Solucion solucionActual) {
            
            List<Ruta> rutasFactibles = ConstruccionEstrategia.generarRutasFactibles(paquete, contexto);
            
            return rutasFactibles.stream()
                .min(Comparator.comparing(Ruta::getTiempoTotalHoras))
                .orElse(null);
        }
        
        @Override
        public void manejarPaqueteNoRuteado(String paqueteId, Paquete paquete, Solucion solucion) {
            System.out.println("⏰ No se encontró ruta de tiempo aceptable para " + paqueteId);
        }
    }
    
    // ================================================================================
    // ESTRATEGIA BALANCEADA
    // ================================================================================
    
    private static class EstrategiaBalanceada implements EstrategiaSeleccion {
        private static final double PESO_COSTO = 0.6;
        private static final double PESO_TIEMPO = 0.4;
        
        @Override
        public List<String> ordenarPaquetes(List<String> paquetes, ContextoProblema contexto) {
            return new ArrayList<>(paquetes); // Sin ordenamiento especial
        }
        
        @Override
        public Ruta seleccionarMejorRuta(String paqueteId, Paquete paquete, ContextoProblema contexto, 
                                        ValidadorRestricciones validador, Solucion solucionActual) {
            
            List<Ruta> rutasFactibles = ConstruccionEstrategia.generarRutasFactibles(paquete, contexto);
            
            return rutasFactibles.stream()
                .min((r1, r2) -> {
                    double score1 = PESO_COSTO * r1.getCostoTotal() + PESO_TIEMPO * r1.getTiempoTotalHoras();
                    double score2 = PESO_COSTO * r2.getCostoTotal() + PESO_TIEMPO * r2.getTiempoTotalHoras();
                    return Double.compare(score1, score2);
                })
                .orElse(null);
        }
        
        @Override
        public void manejarPaqueteNoRuteado(String paqueteId, Paquete paquete, Solucion solucion) {
            System.out.println("⚖️ No se encontró ruta balanceada para " + paqueteId);
        }
    }
    
    // ================================================================================
    // ESTRATEGIA INTELIGENTE (Respeta restricciones)
    // ================================================================================
    
    private static class EstrategiaInteligente implements EstrategiaSeleccion {
        
        @Override
        public List<String> ordenarPaquetes(List<String> paquetes, ContextoProblema contexto) {
            // Ordenar por prioridad (alta prioridad primero)
            return paquetes.stream()
                .sorted((p1, p2) -> {
                    Paquete paq1 = contexto.getPaquete(p1);
                    Paquete paq2 = contexto.getPaquete(p2);
                    if (paq1 != null && paq2 != null) {
                        return Integer.compare(paq2.getPrioridad(), paq1.getPrioridad());
                    }
                    return 0;
                })
                .toList();
        }
        
        @Override
        public Ruta seleccionarMejorRuta(String paqueteId, Paquete paquete, ContextoProblema contexto, 
                                        ValidadorRestricciones validador, Solucion solucionActual) {
            
            String origen = paquete.getAeropuertoOrigen();
            String destino = paquete.getAeropuertoDestino();
            
            // 1. Intentar ruta directa validando capacidades
            List<Vuelo> vuelosDirectos = contexto.getVuelosDirectos(origen, destino);
            for (Vuelo vuelo : vuelosDirectos) {
                Ruta rutaDirecta = ConstruccionEstrategia.crearRutaDirecta(vuelo, paquete);
                if (validador.esRutaFactible(paqueteId, rutaDirecta, solucionActual)) {
                    return rutaDirecta;
                }
            }
            
            // 2. Intentar ruta con conexión validando capacidades
            List<String> rutaBFS = contexto.encontrarRutaMasCorta(origen, destino);
            if (rutaBFS.size() >= 3) {
                Ruta rutaConConexion = ConstruccionEstrategia.crearRutaConConexiones(rutaBFS, contexto, paquete);
                if (rutaConConexion != null && validador.esRutaFactible(paqueteId, rutaConConexion, solucionActual)) {
                    return rutaConConexion;
                }
            }
            
            return null; // No hay ruta factible que respete restricciones
        }
        
        @Override
        public void manejarPaqueteNoRuteado(String paqueteId, Paquete paquete, Solucion solucion) {
            System.out.println("⚠️ No se pudo rutear " + paqueteId + " respetando capacidades");
        }
    }
    
    // ================================================================================
    // MÉTODOS UTILITARIOS COMUNES
    // ================================================================================
    
    private static List<Ruta> generarRutasFactibles(Paquete paquete, ContextoProblema contexto) {
        List<Ruta> rutas = new ArrayList<>();
        
        // Rutas directas
        List<Vuelo> vuelosDirectos = contexto.getVuelosDirectos(
            paquete.getAeropuertoOrigen(), paquete.getAeropuertoDestino()
        );
        
        for (Vuelo vuelo : vuelosDirectos) {
            Ruta rutaDirecta = ConstruccionEstrategia.crearRutaDirecta(vuelo, paquete);
            if (ConstruccionEstrategia.esRutaBasicamenteFactible(rutaDirecta)) {
                rutas.add(rutaDirecta);
            }
        }
        
        // Ruta con conexión usando BFS
        List<String> rutaBFS = contexto.encontrarRutaMasCorta(
            paquete.getAeropuertoOrigen(), paquete.getAeropuertoDestino()
        );
        
        if (rutaBFS.size() >= 3) {
            Ruta rutaConexion = ConstruccionEstrategia.crearRutaConConexiones(rutaBFS, contexto, paquete);
            if (rutaConexion != null && ConstruccionEstrategia.esRutaBasicamenteFactible(rutaConexion)) {
                rutas.add(rutaConexion);
            }
        }
        
        return rutas;
    }
    
    private static Ruta crearRutaDirecta(Vuelo vuelo, Paquete paquete) {
        Ruta ruta = new Ruta("ruta_directa_" + System.currentTimeMillis(), paquete.getId());
        
        SegmentoRuta segmento = new SegmentoRuta(
            "seg_" + System.currentTimeMillis(),
            paquete.getAeropuertoOrigen(),
            paquete.getAeropuertoDestino(),
            vuelo.getNumeroVuelo(),
            vuelo.isMismoContinente()
        );
        
        ruta.agregarSegmento(segmento);
        return ruta;
    }
    
    private static Ruta crearRutaConConexiones(List<String> aeropuertos, ContextoProblema contexto, Paquete paquete) {
        Ruta ruta = new Ruta("ruta_conexiones_" + System.currentTimeMillis(), paquete.getId());
        
        for (int i = 0; i < aeropuertos.size() - 1; i++) {
            String origenSegmento = aeropuertos.get(i);
            String destinoSegmento = aeropuertos.get(i + 1);
            
            List<Vuelo> vuelosSegmento = contexto.getVuelosDirectos(origenSegmento, destinoSegmento);
            if (vuelosSegmento.isEmpty()) {
                return null; // No hay vuelo para este segmento
            }
            
            Vuelo vuelo = vuelosSegmento.get(0); // Tomar primer vuelo disponible
            SegmentoRuta segmento = new SegmentoRuta(
                "seg_" + i + "_" + System.currentTimeMillis(),
                origenSegmento,
                destinoSegmento,
                vuelo.getNumeroVuelo(),
                vuelo.isMismoContinente()
            );
            
            ruta.agregarSegmento(segmento);
        }
        
        return ruta;
    }
    
    private static Ruta crearRutaBasicaFallback(Paquete paquete) {
        Ruta ruta = new Ruta("ruta_fallback_" + System.currentTimeMillis(), paquete.getId());
        
        SegmentoRuta segmento = new SegmentoRuta(
            "seg_fallback_" + System.currentTimeMillis(),
            paquete.getAeropuertoOrigen(),
            paquete.getAeropuertoDestino(),
            "VUELO_FALLBACK",
            true // Asumir mismo continente para menor penalización
        );
        
        ruta.agregarSegmento(segmento);
        return ruta;
    }
    
    private static boolean esRutaBasicamenteFactible(Ruta ruta) {
        return ruta != null && !ruta.getSegmentos().isEmpty() && 
               ruta.getCostoTotal() > 0 && ruta.getTiempoTotalHoras() > 0;
    }
}
