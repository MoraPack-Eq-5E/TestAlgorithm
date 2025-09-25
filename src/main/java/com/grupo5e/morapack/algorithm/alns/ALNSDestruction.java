package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.core.enums.Continente;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Collections;
import java.time.temporal.ChronoUnit;

/**
 * Clase que implementa operadores de destrucción para el algoritmo ALNS
 * (Adaptive Large Neighborhood Search) específicamente diseñados para el problema
 * de logística MoraPack.
 * 
 * Los operadores están diseñados para preservar la prioridad de entregas a tiempo.
 */
public class ALNSDestruction {
    
    private Random aleatorio;
    
    public ALNSDestruction() {
        this.aleatorio = new Random(System.currentTimeMillis());
    }
    
    /**
     * Constructor con semilla específica para pruebas deterministas
     */
    public ALNSDestruction(long semilla) {
        this.aleatorio = new Random(semilla);
    }
    
    /**
     * CORRECCIÓN: Helpers para cálculos precisos de slack y productos
     */
    private static double horasRuta(ArrayList<Vuelo> ruta) {
        if (ruta == null || ruta.isEmpty()) return Double.POSITIVE_INFINITY;
        double h = 0;
        for (Vuelo f : ruta) h += f.getTiempoTransporte();
        if (ruta.size() > 1) h += (ruta.size() - 1) * 2.0; // conexiones de 2h
        return h;
    }
    
    private static int productosDe(Paquete paquete) {
        return (paquete.getProductos() != null && !paquete.getProductos().isEmpty()) ? paquete.getProductos().size() : 1;
    }
    
    /**
     * CORRECCIÓN: Slack real - horas disponibles desde fechaPedido menos horas de la ruta actual
     * REFINAMIENTO: Clampar slack negativo por deadlines raros (deadline < fechaPedido)
     */
    private static double slackHoras(Paquete paquete, ArrayList<Vuelo> ruta) {
        long presupuesto = ChronoUnit.HOURS.between(paquete.getFechaPedido(), paquete.getFechaLimiteEntrega());
        // REFINAMIENTO: Clampar budget negativo a 0 para evitar data mala
        if (presupuesto < 0) {
            presupuesto = 0; // Deadline en el pasado o mal configurado
        }
        double slack = presupuesto - horasRuta(ruta);
        return Math.max(slack, 0.0); // REFINAMIENTO: Clampar slack final a >= 0
    }
    
    /**
     * REFINAMIENTO: Verificar si un paquete ya está en destino (ruta null/empty)
     * Estos paquetes no liberan capacidad de vuelo
     */
    private static boolean yaEstaEnDestino(ArrayList<Vuelo> ruta) {
        return ruta == null || ruta.isEmpty();
    }
    
    /**
     * CORRECCIÓN: Destrucción aleatoria mejorada - sesgo por mayor slack y más productos
     */
    public ResultadoDestruccion destruccionAleatoria(
            HashMap<Paquete, ArrayList<Vuelo>> solucionActual,
            double ratioDestruccion,
            int minDestruir,
            int maxDestruir) {
        
        HashMap<Paquete, ArrayList<Vuelo>> solucionParcial = new HashMap<>(solucionActual);
        
        if (solucionActual.isEmpty()) {
            return new ResultadoDestruccion(solucionParcial, new ArrayList<>());
        }
        
        // CORRECCIÓN: Construir lista con score = w1*slack + w2*productos
        class Candidato { 
            Paquete paquete; 
            double puntuacion; 
        }
        
        ArrayList<Candidato> candidatos = new ArrayList<>();
        for (Map.Entry<Paquete, ArrayList<Vuelo>> e : solucionActual.entrySet()) {
            Paquete p = e.getKey();
            ArrayList<Vuelo> r = e.getValue();
            double slack = slackHoras(p, r);
            int productos = productosDe(p);
            
            // REFINAMIENTO: Penalizar fuertemente paquetes ya en destino (no liberan capacidad de vuelo)
            if (yaEstaEnDestino(r)) {
                slack = slack * 0.1; // Reducir significativamente su prioridad
            }
            
            double puntuacion = 1.0 * slack + 0.2 * productos; // pesos: slack y productos
            
            Candidato c = new Candidato();
            c.paquete = p; 
            c.puntuacion = puntuacion;
            candidatos.add(c);
        }
        
        // CORRECCIÓN: Ordenar por puntuacion desc y destruir los top-k con diversidad
        candidatos.sort((a, b) -> Double.compare(b.puntuacion, a.puntuacion));
        
        int numADestruir = Math.min(
            Math.max((int)(solucionActual.size() * ratioDestruccion), minDestruir),
            Math.min(maxDestruir, solucionActual.size())
        );
        
        ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> destruidos = new ArrayList<>();
        int tomados = 0, i = 0;
        
        while (tomados < numADestruir && i < candidatos.size()) {
            // CORRECCIÓN: 10% probabilidad de saltar para diversidad
            if (aleatorio.nextDouble() < 0.10 && i + 1 < candidatos.size()) {
                i++;
            }
            
            Paquete seleccionado = candidatos.get(i).paquete;
            ArrayList<Vuelo> ruta = solucionActual.get(seleccionado);
            if (ruta == null) ruta = new ArrayList<>(); // Protección contra nulos
            
            destruidos.add(new java.util.AbstractMap.SimpleEntry<>(seleccionado, new ArrayList<>(ruta)));
            solucionParcial.remove(seleccionado);
            tomados++; 
            i++;
        }
        
        return new ResultadoDestruccion(solucionParcial, destruidos);
    }
    
    /**
     * Destrucción por zona geográfica: elimina paquetes de un continente específico.
     * Útil para liberar capacidad en rutas intercontinentales.
     */
    public ResultadoDestruccion destruccionGeografica(
            HashMap<Paquete, ArrayList<Vuelo>> solucionActual,
            double ratioDestruccion,
            int minDestruir,
            int maxDestruir) {
        
        HashMap<Paquete, ArrayList<Vuelo>> solucionParcial = new HashMap<>(solucionActual);
        
        if (solucionActual.isEmpty()) {
            return new ResultadoDestruccion(solucionParcial, new ArrayList<>());
        }
        
        // Contar paquetes por continente (origen y destino)
        Map<Continente, ArrayList<Paquete>> paquetesPorContinenteOrigen = new HashMap<>();
        Map<Continente, ArrayList<Paquete>> paquetesPorContinenteDestino = new HashMap<>();
        
        for (Paquete paquete : solucionActual.keySet()) {
            Continente continenteOrigen = paquete.getUbicacionActual().getContinente();
            Continente continenteDestino = paquete.getCiudadDestino().getContinente();
            
            paquetesPorContinenteOrigen.computeIfAbsent(continenteOrigen, k -> new ArrayList<>()).add(paquete);
            paquetesPorContinenteDestino.computeIfAbsent(continenteDestino, k -> new ArrayList<>()).add(paquete);
        }
        
        // Seleccionar continente con más paquetes intercontinentales
        Continente continenteSeleccionado = null;
        int maxPaquetesIntercontinentales = 0;
        
        for (Continente continente : Continente.values()) {
            ArrayList<Paquete> paquetesOrigen = paquetesPorContinenteOrigen.getOrDefault(continente, new ArrayList<>());
            int conteoIntercontinental = 0;
            
            for (Paquete paquete : paquetesOrigen) {
                if (paquete.getUbicacionActual().getContinente() != paquete.getCiudadDestino().getContinente()) {
                    conteoIntercontinental++;
                }
            }
            
            if (conteoIntercontinental > maxPaquetesIntercontinentales) {
                maxPaquetesIntercontinentales = conteoIntercontinental;
                continenteSeleccionado = continente;
            }
        }
        
        if (continenteSeleccionado == null) {
            return destruccionAleatoria(solucionActual, ratioDestruccion, minDestruir, maxDestruir);
        }
        
        // Encontrar paquetes del continente seleccionado
        ArrayList<Paquete> paquetesCandidatos = new ArrayList<>();
        for (Paquete paquete : solucionActual.keySet()) {
            if (paquete.getUbicacionActual().getContinente() == continenteSeleccionado ||
                paquete.getCiudadDestino().getContinente() == continenteSeleccionado) {
                paquetesCandidatos.add(paquete);
            }
        }
        
        if (paquetesCandidatos.size() < minDestruir) {
            return destruccionAleatoria(solucionActual, ratioDestruccion, minDestruir, maxDestruir);
        }
        
        int numADestruir = Math.min(
            Math.max((int)(paquetesCandidatos.size() * ratioDestruccion), minDestruir),
            Math.min(maxDestruir, paquetesCandidatos.size())
        );
        
        ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> paquetesDestruidos = new ArrayList<>();
        
        // REFINAMIENTO: Precomputar slack y productos para evitar recalcular en comparator
        class InformacionCandidato {
            Paquete paquete;
            boolean intercontinental;
            double slack;
            int productos;
            boolean enDestino;
        }
        
        ArrayList<InformacionCandidato> candidatos = new ArrayList<>();
        for (Paquete paquete : paquetesCandidatos) {
            InformacionCandidato info = new InformacionCandidato();
            info.paquete = paquete;
            info.intercontinental = paquete.getUbicacionActual().getContinente() != paquete.getCiudadDestino().getContinente();
            
            ArrayList<Vuelo> ruta = solucionActual.get(paquete);
            info.slack = slackHoras(paquete, ruta);
            info.productos = productosDe(paquete);
            info.enDestino = yaEstaEnDestino(ruta);
            
            candidatos.add(info);
        }
        
        // Priorizar: 1) intercontinental, 2) NO en destino, 3) mayor slack, 4) más productos
        candidatos.sort((c1, c2) -> {
            // Primero: intercontinental vs continental
            if (c1.intercontinental != c2.intercontinental) {
                return Boolean.compare(c2.intercontinental, c1.intercontinental);
            }
            
            // REFINAMIENTO: Segundo: evitar paquetes ya en destino
            if (c1.enDestino != c2.enDestino) {
                return Boolean.compare(c1.enDestino, c2.enDestino);
            }
            
            // Tercero: mayor slack
            int comparacionSlack = Double.compare(c2.slack, c1.slack);
            if (comparacionSlack != 0) {
                return comparacionSlack;
            }
            
            // REFINAMIENTO: Cuarto: tie-break por más productos (liberar más capacidad)
            return Integer.compare(c2.productos, c1.productos);
        });
        
        // Extraer los packages ordenados
        paquetesCandidatos.clear();
        for (InformacionCandidato info : candidatos) {
            paquetesCandidatos.add(info.paquete);
        }
        
        // Seleccionar paquetes con sesgo hacia los intercontinentales
        for (int i = 0; i < numADestruir; i++) {
            Paquete paqueteSeleccionado = paquetesCandidatos.get(i);
            // REFINAMIENTO: Consistencia - usar siempre solucionActual como fuente
            ArrayList<Vuelo> ruta = solucionActual.get(paqueteSeleccionado);
            if (ruta == null) ruta = new ArrayList<>(); // Protección contra nulos
            
            paquetesDestruidos.add(new java.util.AbstractMap.SimpleEntry<>(
                paqueteSeleccionado, 
                new ArrayList<>(ruta)
            ));
            
            solucionParcial.remove(paqueteSeleccionado);
        }
        
        System.out.println("Destrucción geográfica: " + numADestruir + 
                          " paquetes eliminados del continente " + continenteSeleccionado);
        
        return new ResultadoDestruccion(solucionParcial, paquetesDestruidos);
    }
    
    /**
     * Destrucción por tiempo: elimina paquetes con deadlines en un rango específico.
     * Útil para rebalancear la carga temporal.
     */
    public ResultadoDestruccion destruccionBasadaEnTiempo(
            HashMap<Paquete, ArrayList<Vuelo>> solucionActual,
            double ratioDestruccion,
            int minDestruir,
            int maxDestruir) {
        
        HashMap<Paquete, ArrayList<Vuelo>> solucionParcial = new HashMap<>(solucionActual);
        
        if (solucionActual.isEmpty()) {
            return new ResultadoDestruccion(solucionParcial, new ArrayList<>());
        }
        
        // CORRECCIÓN: Agrupar por slack real, no por "horas a deadline"
        ArrayList<Paquete> slackBajo = new ArrayList<>();    // slack ≤ 8 h (no tocar si es posible)
        ArrayList<Paquete> slackMedio = new ArrayList<>();   // 8–32 h
        ArrayList<Paquete> slackAlto = new ArrayList<>();    // > 32 h
        ArrayList<Paquete> enDestino = new ArrayList<>();   // REFINAMIENTO: Separar paquetes ya en destino
        
        for (Map.Entry<Paquete, ArrayList<Vuelo>> e : solucionActual.entrySet()) {
            Paquete paquete = e.getKey();
            ArrayList<Vuelo> ruta = e.getValue();
            
            // REFINAMIENTO: Separar paquetes ya en destino (fallback only)
            if (yaEstaEnDestino(ruta)) {
                enDestino.add(paquete);
                continue;
            }
            
            double s = slackHoras(paquete, ruta);
            if (s <= 8) {
                slackBajo.add(paquete);
            } else if (s <= 32) {
                slackMedio.add(paquete);
            } else {
                slackAlto.add(paquete);
            }
        }
        
        // REFINAMIENTO: Elige grupo prioritariamente, usando enDestino como último recurso
        ArrayList<Paquete> grupoSeleccionado;
        String nombreGrupo;
        
        if (!slackAlto.isEmpty()) {
            grupoSeleccionado = slackAlto;
            nombreGrupo = "alto slack";
        } else if (!slackMedio.isEmpty()) {
            grupoSeleccionado = slackMedio;
            nombreGrupo = "slack medio";
        } else if (!slackBajo.isEmpty()) {
            grupoSeleccionado = slackBajo;
            nombreGrupo = "bajo slack";
        } else {
            grupoSeleccionado = enDestino;
            nombreGrupo = "ya en destino (fallback)";
        }
        
        if (grupoSeleccionado.size() < minDestruir) {
            return destruccionAleatoria(solucionActual, ratioDestruccion, minDestruir, maxDestruir);
        }
        
        // REFINAMIENTO: Ordenar por productos desc para tie-break (más productos = más capacidad liberada)
        if (grupoSeleccionado != enDestino) { // Solo si no son paquetes en destino
            grupoSeleccionado.sort((p1, p2) -> {
                int productosP1 = productosDe(p1);
                int productosP2 = productosDe(p2);
                return Integer.compare(productosP2, productosP1); // Más productos primero
            });
        }
        
        // Barajar parcialmente para diversidad (mantener bias hacia más productos en el top)
        if (grupoSeleccionado.size() > 10) {
            // Solo barajar los últimos elementos, mantener los primeros (más productos) intactos
            Collections.shuffle(grupoSeleccionado.subList(5, grupoSeleccionado.size()), aleatorio);
        } else {
            Collections.shuffle(grupoSeleccionado, aleatorio);
        }
        
        int numADestruir = Math.min(
            Math.max((int)(grupoSeleccionado.size() * ratioDestruccion), minDestruir),
            Math.min(maxDestruir, grupoSeleccionado.size())
        );
        
        ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> destruidos = new ArrayList<>();
        for (int i = 0; i < numADestruir; i++) {
            Paquete seleccionado = grupoSeleccionado.get(i);
            ArrayList<Vuelo> ruta = solucionActual.get(seleccionado);
            if (ruta == null) ruta = new ArrayList<>(); // Protección contra nulos
            
            destruidos.add(new java.util.AbstractMap.SimpleEntry<>(seleccionado, new ArrayList<>(ruta)));
            solucionParcial.remove(seleccionado);
        }
        
        System.out.println("Destrucción temporal por slack: " + numADestruir + " paquetes del grupo " + nombreGrupo);
        
        return new ResultadoDestruccion(solucionParcial, destruidos);
    }
    
    /**
     * CORRECCIÓN COMPLETA: Destrucción de rutas congestionadas con scoring por
     * vuelo crítico + productos - urgencia
     */
    public ResultadoDestruccion destruccionRutaCongestionada(
            HashMap<Paquete, ArrayList<Vuelo>> solucionActual,
            double ratioDestruccion,
            int minDestruir,
            int maxDestruir) {
        
        HashMap<Paquete, ArrayList<Vuelo>> parcial = new HashMap<>(solucionActual);
        if (solucionActual.isEmpty()) {
            return new ResultadoDestruccion(parcial, new ArrayList<>());
        }
        
        // CORRECCIÓN: Parámetros de scoring mejorados
        final double UMBRAL_UTILIZACION = 0.85;   // umbral de "crítico"
        final double PESO_UTILIZACION = 1.0;      // peso de congestión
        final double PESO_PRODUCTOS = 0.25;       // peso por productos
        final double PENALIZACION_SLACK = 0.5;    // penaliza baja holgura
        
        // CORRECCIÓN: Score por paquete basado en congestión crítica + productos - urgencia
        class Candidato { 
            Paquete paquete; 
            double puntuacion; 
        }
        
        ArrayList<Candidato> candidatos = new ArrayList<>();
        
        for (Map.Entry<Paquete, ArrayList<Vuelo>> e : solucionActual.entrySet()) {
            Paquete p = e.getKey();
            ArrayList<Vuelo> r = e.getValue();
            if (r == null || r.isEmpty()) continue;
            
            int productos = productosDe(p);
            
            // CORRECCIÓN: Congestión acumulada en vuelos por encima del umbral
            double congestion = 0.0;
            for (Vuelo f : r) {
                double utilizacion = (f.getCapacidadMaxima() > 0) ? 
                    ((double) f.getCapacidadUsada() / f.getCapacidadMaxima()) : 0.0;
                if (utilizacion > UMBRAL_UTILIZACION) {
                    congestion += (utilizacion - UMBRAL_UTILIZACION);
                }
            }
            
            // CORRECCIÓN: Penalizar quitar paquetes con poca holgura
            double slack = slackHoras(p, r);
            double penalizacionSlack = (slack <= 8) ? (8 - Math.max(slack, 0)) : 0.0;
            
            double puntuacion = PESO_UTILIZACION * congestion + PESO_PRODUCTOS * productos - PENALIZACION_SLACK * penalizacionSlack;
            if (puntuacion > 0) {
                Candidato c = new Candidato();
                c.paquete = p;
                c.puntuacion = puntuacion;
                candidatos.add(c);
            }
        }
        
        if (candidatos.size() < minDestruir) {
            return destruccionAleatoria(solucionActual, ratioDestruccion, minDestruir, maxDestruir);
        }
        
        // CORRECCIÓN: Ordenar por puntuacion desc (más alivio esperado primero)
        candidatos.sort((a, b) -> Double.compare(b.puntuacion, a.puntuacion));
        
        int numADestruir = Math.min(
            Math.max((int)(candidatos.size() * ratioDestruccion), minDestruir),
            Math.min(maxDestruir, candidatos.size())
        );
        
        ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> destruidos = new ArrayList<>();
        for (int i = 0; i < numADestruir; i++) {
            Paquete seleccionado = candidatos.get(i).paquete;
            // REFINAMIENTO: Consistencia - usar solucionActual como fuente
            ArrayList<Vuelo> ruta = solucionActual.get(seleccionado);
            if (ruta == null) ruta = new ArrayList<>(); // Protección contra nulos
            
            destruidos.add(new java.util.AbstractMap.SimpleEntry<>(seleccionado, new ArrayList<>(ruta)));
            parcial.remove(seleccionado);
        }
        
        System.out.println("Destrucción por congestión (mejorada): " + numADestruir + " paquetes");
        return new ResultadoDestruccion(parcial, destruidos);
    }
    
    /**
     * Clase para encapsular el resultado de una operación de destrucción
     */
    public static class ResultadoDestruccion {
        private HashMap<Paquete, ArrayList<Vuelo>> solucionParcial;
        private ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> paquetesDestruidos;
        
        public ResultadoDestruccion(
                HashMap<Paquete, ArrayList<Vuelo>> solucionParcial,
                ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> paquetesDestruidos) {
            this.solucionParcial = solucionParcial;
            this.paquetesDestruidos = paquetesDestruidos;
        }
        
        public HashMap<Paquete, ArrayList<Vuelo>> getSolucionParcial() {
            return solucionParcial;
        }
        
        public ArrayList<Map.Entry<Paquete, ArrayList<Vuelo>>> getPaquetesDestruidos() {
            return paquetesDestruidos;
        }
        
        public int getNumPaquetesDestruidos() {
            return paquetesDestruidos.size();
        }
    }
}
