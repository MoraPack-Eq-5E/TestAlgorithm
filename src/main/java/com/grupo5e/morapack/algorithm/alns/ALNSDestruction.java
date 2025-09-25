package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.model.*;
import java.util.*;
import java.time.temporal.ChronoUnit;

/**
 * Clase que implementa operadores de destrucción para el algoritmo ALNS
 * (Adaptive Large Neighborhood Search) específicamente diseñados para el problema
 * de logística MoraPack.
 * 
 * Los operadores están diseñados para preservar la prioridad de entregas a tiempo.
 */
public class ALNSDestruction {
    
    private Random generadorAleatorio;
    
    public ALNSDestruction() {
        this.generadorAleatorio = new Random(System.currentTimeMillis());
    }
    
    /**
     * Constructor con semilla específica para pruebas deterministas
     */
    public ALNSDestruction(long semilla) {
        this.generadorAleatorio = new Random(semilla);
    }
    
    /**
     * CORRECCIÓN: Helpers para cálculos precisos de slack y productos
     */
    private static double horasRuta(Ruta ruta) {
        if (ruta == null || ruta.getVuelos().isEmpty()) return Double.POSITIVE_INFINITY;
        double h = 0;
        for (Vuelo vuelo : ruta.getVuelos()) {
            h += vuelo.getTiempoTransporte();
        }
        if (ruta.getVuelos().size() > 1) h += (ruta.getVuelos().size() - 1) * 2.0; // conexiones de 2h
        return h;
    }
    
    private static int productosDe(Paquete pkg) {
        // En el modelo actual, cada paquete representa 1 producto
        return 1;
    }
    
    /**
     * CORRECCIÓN: Slack real - horas disponibles desde fechaCreacion menos horas de la ruta actual
     * REFINAMIENTO: Clampar slack negativo por deadlines raros (deadline < fechaCreacion)
     */
    private static double slackHoras(Paquete pkg, Ruta ruta) {
        long presupuesto = ChronoUnit.HOURS.between(pkg.getFechaPedido(), pkg.getFechaLimiteEntrega());
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
    private static boolean yaEnDestino(Ruta ruta) {
        return ruta == null || ruta.getVuelos().isEmpty();
    }
    
    /**
     * CORRECCIÓN: Destrucción aleatoria mejorada - sesgo por mayor slack y más productos
     */
    public DestructionResult randomDestroy(
            HashMap<Paquete, Ruta> solucionActual,
            double tasaDestruccion,
            int minDestruir,
            int maxDestruir) {
        
        HashMap<Paquete, Ruta> solucionParcial = new HashMap<>(solucionActual);
        
        if (solucionActual.isEmpty()) {
            return new DestructionResult(solucionParcial, new ArrayList<>());
        }
        
        // CORRECCIÓN: Construir lista con score = w1*slack + w2*productos
        class Candidato { 
            Paquete paquete; 
            double puntaje; 
        }
        
        ArrayList<Candidato> candidatos = new ArrayList<>();
        for (Map.Entry<Paquete, Ruta> entrada : solucionActual.entrySet()) {
            Paquete p = entrada.getKey();
            Ruta r = entrada.getValue();
            double slack = slackHoras(p, r);
            int productos = productosDe(p);
            
            // REFINAMIENTO: Penalizar fuertemente paquetes ya en destino (no liberan capacidad de vuelo)
            if (yaEnDestino(r)) {
                slack = slack * 0.1; // Reducir significativamente su prioridad
            }
            
            double puntaje = 1.0 * slack + 0.2 * productos; // pesos: slack y productos
            
            Candidato c = new Candidato();
            c.paquete = p; 
            c.puntaje = puntaje;
            candidatos.add(c);
        }
        
        // CORRECCIÓN: Ordenar por score desc y destruir los top-k con diversidad
        candidatos.sort((a, b) -> Double.compare(b.puntaje, a.puntaje));
        
        int numDestruir = Math.min(
            Math.max((int)(solucionActual.size() * tasaDestruccion), minDestruir),
            Math.min(maxDestruir, solucionActual.size())
        );
        
        ArrayList<Map.Entry<Paquete, Ruta>> destruidos = new ArrayList<>();
        int tomados = 0, i = 0;
        
        while (tomados < numDestruir && i < candidatos.size()) {
            // CORRECCIÓN: 10% probabilidad de saltar para diversidad
            if (generadorAleatorio.nextDouble() < 0.10 && i + 1 < candidatos.size()) {
                i++;
            }
            
            Paquete seleccionado = candidatos.get(i).paquete;
            Ruta ruta = solucionActual.get(seleccionado);
            if (ruta == null) ruta = new Ruta(); // Protección contra nulos
            
            destruidos.add(new java.util.AbstractMap.SimpleEntry<>(seleccionado, ruta));
            solucionParcial.remove(seleccionado);
            tomados++; 
            i++;
        }
        
        return new DestructionResult(solucionParcial, destruidos);
    }
    
    /**
     * Destrucción por zona geográfica: elimina paquetes de un continente específico.
     * Útil para liberar capacidad en rutas intercontinentales.
     */
    public DestructionResult geographicDestroy(
            HashMap<Paquete, Ruta> solucionActual,
            double tasaDestruccion,
            int minDestruir,
            int maxDestruir) {
        
        HashMap<Paquete, Ruta> solucionParcial = new HashMap<>(solucionActual);
        
        if (solucionActual.isEmpty()) {
            return new DestructionResult(solucionParcial, new ArrayList<>());
        }
        
        // Contar paquetes por continente (origen y destino)
        Map<String, ArrayList<Paquete>> paquetesPorContinenteOrigen = new HashMap<>();
        Map<String, ArrayList<Paquete>> paquetesPorContinenteDestino = new HashMap<>();
        
        for (Paquete pkg : solucionActual.keySet()) {
            String continenteOrigen = obtenerContinenteDeAeropuerto(pkg.getAeropuertoOrigen());
            String continenteDestino = obtenerContinenteDeAeropuerto(pkg.getAeropuertoDestino());
            
            paquetesPorContinenteOrigen.computeIfAbsent(continenteOrigen, k -> new ArrayList<>()).add(pkg);
            paquetesPorContinenteDestino.computeIfAbsent(continenteDestino, k -> new ArrayList<>()).add(pkg);
        }
        
        // Seleccionar continente con más paquetes intercontinentales
        String continenteSeleccionado = null;
        int maxPaquetesIntercontinentales = 0;
        
        for (String continente : paquetesPorContinenteOrigen.keySet()) {
            ArrayList<Paquete> paquetesOrigen = paquetesPorContinenteOrigen.get(continente);
            int conteoIntercontinental = 0;
            
            for (Paquete pkg : paquetesOrigen) {
                String continenteOrigen = obtenerContinenteDeAeropuerto(pkg.getAeropuertoOrigen());
                String continenteDestino = obtenerContinenteDeAeropuerto(pkg.getAeropuertoDestino());
                if (!continenteOrigen.equals(continenteDestino)) {
                    conteoIntercontinental++;
                }
            }
            
            if (conteoIntercontinental > maxPaquetesIntercontinentales) {
                maxPaquetesIntercontinentales = conteoIntercontinental;
                continenteSeleccionado = continente;
            }
        }
        
        if (continenteSeleccionado == null) {
            return randomDestroy(solucionActual, tasaDestruccion, minDestruir, maxDestruir);
        }
        
        // Encontrar paquetes del continente seleccionado
        ArrayList<Paquete> paquetesCandidatos = new ArrayList<>();
        for (Paquete pkg : solucionActual.keySet()) {
            String continenteOrigen = obtenerContinenteDeAeropuerto(pkg.getAeropuertoOrigen());
            String continenteDestino = obtenerContinenteDeAeropuerto(pkg.getAeropuertoDestino());
            if (continenteOrigen.equals(continenteSeleccionado) || continenteDestino.equals(continenteSeleccionado)) {
                paquetesCandidatos.add(pkg);
            }
        }
        
        if (paquetesCandidatos.size() < minDestruir) {
            return randomDestroy(solucionActual, tasaDestruccion, minDestruir, maxDestruir);
        }
        
        int numDestruir = Math.min(
            Math.max((int)(paquetesCandidatos.size() * tasaDestruccion), minDestruir),
            Math.min(maxDestruir, paquetesCandidatos.size())
        );
        
        ArrayList<Map.Entry<Paquete, Ruta>> paquetesDestruidos = new ArrayList<>();
        
        // REFINAMIENTO: Precomputar slack y productos para evitar recalcular en comparator
        class InfoCandidato {
            Paquete paquete;
            boolean intercontinental;
            double slack;
            int productos;
            boolean enDestino;
        }
        
        ArrayList<InfoCandidato> candidatos = new ArrayList<>();
        for (Paquete pkg : paquetesCandidatos) {
            InfoCandidato info = new InfoCandidato();
            info.paquete = pkg;
            String continenteOrigen = obtenerContinenteDeAeropuerto(pkg.getAeropuertoOrigen());
            String continenteDestino = obtenerContinenteDeAeropuerto(pkg.getAeropuertoDestino());
            info.intercontinental = !continenteOrigen.equals(continenteDestino);
            
            Ruta ruta = solucionActual.get(pkg);
            info.slack = slackHoras(pkg, ruta);
            info.productos = productosDe(pkg);
            info.enDestino = yaEnDestino(ruta);
            
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
        for (InfoCandidato info : candidatos) {
            paquetesCandidatos.add(info.paquete);
        }
        
        // Seleccionar paquetes con sesgo hacia los intercontinentales
        for (int i = 0; i < numDestruir; i++) {
            Paquete paqueteSeleccionado = paquetesCandidatos.get(i);
            // REFINAMIENTO: Consistencia - usar siempre solucionActual como fuente
            Ruta ruta = solucionActual.get(paqueteSeleccionado);
            if (ruta == null) ruta = new Ruta(); // Protección contra nulos
            
            paquetesDestruidos.add(new java.util.AbstractMap.SimpleEntry<>(
                paqueteSeleccionado, 
                ruta
            ));
            
            solucionParcial.remove(paqueteSeleccionado);
        }
        
        System.out.println("Destrucción geográfica: " + numDestruir + 
                          " paquetes eliminados del continente " + continenteSeleccionado);
        
        return new DestructionResult(solucionParcial, paquetesDestruidos);
    }
    
    /**
     * Destrucción por tiempo: elimina paquetes con deadlines en un rango específico.
     * Útil para rebalancear la carga temporal.
     */
    public DestructionResult timeBasedDestroy(
            HashMap<Paquete, Ruta> solucionActual,
            double tasaDestruccion,
            int minDestruir,
            int maxDestruir) {
        
        HashMap<Paquete, Ruta> solucionParcial = new HashMap<>(solucionActual);
        
        if (solucionActual.isEmpty()) {
            return new DestructionResult(solucionParcial, new ArrayList<>());
        }
        
        // CORRECCIÓN: Agrupar por slack real, no por "horas a deadline"
        ArrayList<Paquete> slackBajo = new ArrayList<>();    // slack ≤ 8 h (no tocar si es posible)
        ArrayList<Paquete> slackMedio = new ArrayList<>();    // 8–32 h
        ArrayList<Paquete> slackAlto = new ArrayList<>();   // > 32 h
        ArrayList<Paquete> enDestino = new ArrayList<>(); // REFINAMIENTO: Separar paquetes ya en destino
        
        for (Map.Entry<Paquete, Ruta> entrada : solucionActual.entrySet()) {
            Paquete pkg = entrada.getKey();
            Ruta ruta = entrada.getValue();
            
            // REFINAMIENTO: Separar paquetes ya en destino (fallback only)
            if (yaEnDestino(ruta)) {
                enDestino.add(pkg);
                continue;
            }
            
            double s = slackHoras(pkg, ruta);
            if (s <= 8) {
                slackBajo.add(pkg);
            } else if (s <= 32) {
                slackMedio.add(pkg);
            } else {
                slackAlto.add(pkg);
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
            return randomDestroy(solucionActual, tasaDestruccion, minDestruir, maxDestruir);
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
            Collections.shuffle(grupoSeleccionado.subList(5, grupoSeleccionado.size()), generadorAleatorio);
        } else {
            Collections.shuffle(grupoSeleccionado, generadorAleatorio);
        }
        
        int numDestruir = Math.min(
            Math.max((int)(grupoSeleccionado.size() * tasaDestruccion), minDestruir),
            Math.min(maxDestruir, grupoSeleccionado.size())
        );
        
        ArrayList<Map.Entry<Paquete, Ruta>> destruidos = new ArrayList<>();
        for (int i = 0; i < numDestruir; i++) {
            Paquete seleccionado = grupoSeleccionado.get(i);
            Ruta ruta = solucionActual.get(seleccionado);
            if (ruta == null) ruta = new Ruta(); // Protección contra nulos
            
            destruidos.add(new java.util.AbstractMap.SimpleEntry<>(seleccionado, ruta));
            solucionParcial.remove(seleccionado);
        }
        
        System.out.println("Destrucción temporal por slack: " + numDestruir + " paquetes del grupo " + nombreGrupo);
        
        return new DestructionResult(solucionParcial, destruidos);
    }
    
    /**
     * CORRECCIÓN COMPLETA: Destrucción de rutas congestionadas con scoring por
     * vuelo crítico + productos - urgencia
     */
    public DestructionResult congestedRouteDestroy(
            HashMap<Paquete, Ruta> solucionActual,
            double tasaDestruccion,
            int minDestruir,
            int maxDestruir) {
        
        HashMap<Paquete, Ruta> solucionParcial = new HashMap<>(solucionActual);
        if (solucionActual.isEmpty()) {
            return new DestructionResult(solucionParcial, new ArrayList<>());
        }
        
        // CORRECCIÓN: Parámetros de scoring mejorados
        final double UMBRAL_UTILIZACION = 0.85;   // umbral de "crítico"
        final double PESO_UTILIZACION = 1.0;            // peso de congestión
        final double PESO_PRODUCTOS = 0.25;          // peso por productos
        final double PENALIZACION_SLACK = 0.5;   // penaliza baja holgura
        
        // CORRECCIÓN: Score por paquete basado en congestión crítica + productos - urgencia
        class Candidato { 
            Paquete paquete; 
            double puntaje; 
        }
        
        ArrayList<Candidato> candidatos = new ArrayList<>();
        
        for (Map.Entry<Paquete, Ruta> entrada : solucionActual.entrySet()) {
            Paquete p = entrada.getKey();
            Ruta r = entrada.getValue();
            if (r == null || r.getVuelos().isEmpty()) continue;
            
            int productos = productosDe(p);
            
            // CORRECCIÓN: Congestión acumulada en vuelos por encima del umbral
            double congestion = 0.0;
            for (int i = 0; i < r.getVuelos().size(); i++) {
                // Simular utilización del vuelo (en un sistema real, esto vendría de los datos)
                double utilizacion = 0.7 + generadorAleatorio.nextDouble() * 0.3; // Simulación
                if (utilizacion > UMBRAL_UTILIZACION) {
                    congestion += (utilizacion - UMBRAL_UTILIZACION);
                }
            }
            
            // CORRECCIÓN: Penalizar quitar paquetes con poca holgura
            double slack = slackHoras(p, r);
            double penalizacionSlack = (slack <= 8) ? (8 - Math.max(slack, 0)) : 0.0;
            
            double puntaje = PESO_UTILIZACION * congestion + PESO_PRODUCTOS * productos - PENALIZACION_SLACK * penalizacionSlack;
            if (puntaje > 0) {
                Candidato c = new Candidato();
                c.paquete = p;
                c.puntaje = puntaje;
                candidatos.add(c);
            }
        }
        
        if (candidatos.size() < minDestruir) {
            return randomDestroy(solucionActual, tasaDestruccion, minDestruir, maxDestruir);
        }
        
        // CORRECCIÓN: Ordenar por score desc (más alivio esperado primero)
        candidatos.sort((a, b) -> Double.compare(b.puntaje, a.puntaje));
        
        int numDestruir = Math.min(
            Math.max((int)(candidatos.size() * tasaDestruccion), minDestruir),
            Math.min(maxDestruir, candidatos.size())
        );
        
        ArrayList<Map.Entry<Paquete, Ruta>> destruidos = new ArrayList<>();
        for (int i = 0; i < numDestruir; i++) {
            Paquete seleccionado = candidatos.get(i).paquete;
            // REFINAMIENTO: Consistencia - usar solucionActual como fuente
            Ruta ruta = solucionActual.get(seleccionado);
            if (ruta == null) ruta = new Ruta(); // Protección contra nulos
            
            destruidos.add(new java.util.AbstractMap.SimpleEntry<>(seleccionado, ruta));
            solucionParcial.remove(seleccionado);
        }
        
        System.out.println("Destrucción por congestión (mejorada): " + numDestruir + " paquetes");
        return new DestructionResult(solucionParcial, destruidos);
    }
    
    /**
     * Obtiene el continente de un aeropuerto por su código IATA
     */
    private String obtenerContinenteDeAeropuerto(String codigoIATA) {
        // Mapeo simplificado de códigos IATA a continentes
        if (codigoIATA.startsWith("SP") || codigoIATA.startsWith("SC")) {
            return "America del Sur";
        } else if (codigoIATA.startsWith("EB") || codigoIATA.startsWith("LF")) {
            return "Europa";
        } else if (codigoIATA.startsWith("UB") || codigoIATA.startsWith("UZ")) {
            return "Asia";
        }
        return "Desconocido";
    }
    
    /**
     * Clase para encapsular el resultado de una operación de destrucción
     */
    public static class DestructionResult {
        private HashMap<Paquete, Ruta> solucionParcial;
        private ArrayList<Map.Entry<Paquete, Ruta>> paquetesDestruidos;
        
        public DestructionResult(
                HashMap<Paquete, Ruta> solucionParcial,
                ArrayList<Map.Entry<Paquete, Ruta>> paquetesDestruidos) {
            this.solucionParcial = solucionParcial;
            this.paquetesDestruidos = paquetesDestruidos;
        }
        
        public HashMap<Paquete, Ruta> getPartialSolution() {
            return solucionParcial;
        }
        
        public ArrayList<Map.Entry<Paquete, Ruta>> getDestroyedPackages() {
            return paquetesDestruidos;
        }
        
        public int getNumDestroyedPackages() {
            return paquetesDestruidos.size();
        }
    }
}