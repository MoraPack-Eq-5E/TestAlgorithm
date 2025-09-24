package com.grupo5e.morapack.algorithm.validation;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.core.enums.TipoViolacion;
import java.util.*;

/**
 * Validador de restricciones para el problema MoraPack.
 * Verifica que las soluciones cumplan con todas las restricciones del negocio.
 */
public class ValidadorRestricciones {
    
    private final Map<String, Aeropuerto> aeropuertos;
    private final Map<String, Vuelo> vuelos;
    private final Map<String, String> aeropuertoAContinente;
    
    public ValidadorRestricciones(List<Aeropuerto> listaAeropuertos, List<Vuelo> listaVuelos, 
                                  Set<Continente> continentes) {
        this.aeropuertos = new HashMap<>();
        this.vuelos = new HashMap<>();
        this.aeropuertoAContinente = new HashMap<>();
        
        // Indexar aeropuertos
        for (Aeropuerto aeropuerto : listaAeropuertos) {
            aeropuertos.put(aeropuerto.getCodigoIATA(), aeropuerto);
        }
        
        // Indexar vuelos
        for (Vuelo vuelo : listaVuelos) {
            vuelos.put(vuelo.getNumeroVuelo(), vuelo);
        }
        
        // Crear mapeo aeropuerto -> continente
        for (Continente continente : continentes) {
            for (String codigoIATA : continente.getCodigosIATAAeropuertos()) {
                aeropuertoAContinente.put(codigoIATA, continente.getCodigo());
            }
        }
    }
    
    /**
     * Valida una solución completa y actualiza sus métricas de violaciones.
     */
    public ResultadoValidacion validarSolucion(Solucion solucion) {
        ResultadoValidacion resultado = new ResultadoValidacion();
        
        for (Map.Entry<String, Ruta> entry : solucion.getRutasPaquetes().entrySet()) {
            String paqueteId = entry.getKey();
            Ruta ruta = entry.getValue();
            
            // Validar ruta individual
            ResultadoValidacion validacionRuta = validarRuta(paqueteId, ruta);
            resultado.combinar(validacionRuta);
        }
        
        // Validar restricciones globales
        validarCapacidadesGlobales(solucion, resultado);
        
        // Actualizar métricas de la solución
        solucion.setViolacionesRestricciones(resultado.getTotalViolaciones());
        solucion.setEsFactible(resultado.esFactible());
        
        return resultado;
    }
    
    /**
     * Valida una ruta individual.
     */
    public ResultadoValidacion validarRuta(String paqueteId, Ruta ruta) {
        ResultadoValidacion resultado = new ResultadoValidacion();
        
        if (ruta.getSegmentos().isEmpty()) {
            resultado.agregarViolacion("Ruta vacía para paquete " + paqueteId, TipoViolacion.RUTA_INVALIDA);
            return resultado;
        }
        
        try {
            ruta.validarRuta(); // Validación básica de conectividad
        } catch (IllegalStateException e) {
            resultado.agregarViolacion("Ruta desconectada: " + e.getMessage(), TipoViolacion.RUTA_INVALIDA);
        }
        
        // Validar cada segmento
        for (int i = 0; i < ruta.getSegmentos().size(); i++) {
            SegmentoRuta segmento = ruta.getSegmentos().get(i);
            validarSegmento(segmento, resultado, paqueteId, i);
        }
        
        // Validar plazos de entrega
        validarPlazosEntrega(paqueteId, ruta, resultado);
        
        return resultado;
    }
    
    private void validarSegmento(SegmentoRuta segmento, ResultadoValidacion resultado, 
                                String paqueteId, int numeroSegmento) {
        
        // Verificar que los aeropuertos existan
        if (!aeropuertos.containsKey(segmento.getAeropuertoOrigen())) {
            resultado.agregarViolacion(
                String.format("Aeropuerto origen %s no existe en segmento %d de paquete %s", 
                            segmento.getAeropuertoOrigen(), numeroSegmento, paqueteId),
                TipoViolacion.AEROPUERTO_INEXISTENTE
            );
        }
        
        if (!aeropuertos.containsKey(segmento.getAeropuertoDestino())) {
            resultado.agregarViolacion(
                String.format("Aeropuerto destino %s no existe en segmento %d de paquete %s", 
                            segmento.getAeropuertoDestino(), numeroSegmento, paqueteId),
                TipoViolacion.AEROPUERTO_INEXISTENTE
            );
        }
        
        // Verificar que el vuelo exista
        Vuelo vuelo = vuelos.get(segmento.getNumeroVuelo());
        if (vuelo == null) {
            resultado.agregarViolacion(
                String.format("Vuelo %s no existe en segmento %d de paquete %s", 
                            segmento.getNumeroVuelo(), numeroSegmento, paqueteId),
                TipoViolacion.VUELO_INEXISTENTE
            );
            return; // No continuar validando este segmento
        }
        
        // Verificar consistencia de aeropuertos con vuelo
        if (!vuelo.getAeropuertoOrigen().equals(segmento.getAeropuertoOrigen()) ||
            !vuelo.getAeropuertoDestino().equals(segmento.getAeropuertoDestino())) {
            resultado.agregarViolacion(
                String.format("Inconsistencia aeropuerto-vuelo en segmento %d de paquete %s", 
                            numeroSegmento, paqueteId),
                TipoViolacion.INCONSISTENCIA_VUELO
            );
        }
        
        // Verificar capacidad del vuelo (simplificado - en implementación real se haría global)
        if (!vuelo.puedeCargar(1)) {
            resultado.agregarViolacion(
                String.format("Vuelo %s sin capacidad para paquete %s", 
                            vuelo.getNumeroVuelo(), paqueteId),
                TipoViolacion.CAPACIDAD_VUELO_EXCEDIDA
            );
        }
        
        // Verificar capacidad de almacén en aeropuerto destino
        Aeropuerto aeropuertoDestino = aeropuertos.get(segmento.getAeropuertoDestino());
        if (aeropuertoDestino != null && !aeropuertoDestino.puedeAlmacenar(1)) {
            resultado.agregarViolacion(
                String.format("Aeropuerto %s sin capacidad de almacén para paquete %s", 
                            aeropuertoDestino.getCodigoIATA(), paqueteId),
                TipoViolacion.CAPACIDAD_ALMACEN_EXCEDIDA
            );
        }
    }
    
    private void validarPlazosEntrega(String paqueteId, Ruta ruta, ResultadoValidacion resultado) {
        // Obtener información del continente para calcular plazo
        String aeropuertoOrigen = ruta.getAeropuertoOrigen();
        String aeropuertoDestino = ruta.getAeropuertoDestino();
        
        if (aeropuertoOrigen == null || aeropuertoDestino == null) {
            return;
        }
        
        String continenteOrigen = aeropuertoAContinente.get(aeropuertoOrigen);
        String continenteDestino = aeropuertoAContinente.get(aeropuertoDestino);
        
        if (continenteOrigen == null || continenteDestino == null) {
            resultado.agregarViolacion(
                String.format("No se puede determinar continente para paquete %s", paqueteId),
                TipoViolacion.CONTINENTE_INDETERMINADO
            );
            return;
        }
        
        boolean mismoContinente = continenteOrigen.equals(continenteDestino);
        int diasPlazoMaximo = mismoContinente ? 2 : 3;
        
        // Verificar si el tiempo total de la ruta excede el plazo
        double horasMaximas = diasPlazoMaximo * 24.0;
        if (ruta.getTiempoTotalHoras() > horasMaximas) {
            resultado.agregarViolacion(
                String.format("Ruta de paquete %s excede plazo de entrega (%.1f > %.1f horas)", 
                            paqueteId, ruta.getTiempoTotalHoras(), horasMaximas),
                TipoViolacion.PLAZO_EXCEDIDO
            );
        }
        
        // Verificar tiempos de vuelo según restricciones MoraPack
        for (SegmentoRuta segmento : ruta.getSegmentos()) {
            double horasEsperadas = segmento.isMismoContinente() ? 12.0 : 24.0;
            // Allow some tolerance for real-world flight duration variations
            double tolerancia = 1.0; // 1 hour tolerance
            if (Math.abs(segmento.getDuracionHoras() - horasEsperadas) > tolerancia) {
                resultado.agregarViolacion(
                    String.format("Duración incorrecta en segmento de paquete %s (%.1f != %.1f±%.1f horas)", 
                                paqueteId, segmento.getDuracionHoras(), horasEsperadas, tolerancia),
                    TipoViolacion.DURACION_VUELO_INCORRECTA
                );
            }
        }
    }
    
    private void validarCapacidadesGlobales(Solucion solucion, ResultadoValidacion resultado) {
        // Validar capacidades globales de vuelos
        Map<String, Integer> usosVuelo = new HashMap<>();
        Map<String, Integer> usosAlmacen = new HashMap<>();
        
        for (Ruta ruta : solucion.getRutasPaquetes().values()) {
            for (SegmentoRuta segmento : ruta.getSegmentos()) {
                // Contar uso de vuelos
                usosVuelo.put(segmento.getNumeroVuelo(), 
                           usosVuelo.getOrDefault(segmento.getNumeroVuelo(), 0) + 1);
                
                // Contar uso de almacenes
                usosAlmacen.put(segmento.getAeropuertoDestino(),
                             usosAlmacen.getOrDefault(segmento.getAeropuertoDestino(), 0) + 1);
            }
        }
        
        // Verificar capacidades de vuelos
        for (Map.Entry<String, Integer> entry : usosVuelo.entrySet()) {
            Vuelo vuelo = vuelos.get(entry.getKey());
            if (vuelo != null && entry.getValue() > vuelo.getCapacidadMaxima()) {
                resultado.agregarViolacion(
                    String.format("Capacidad de vuelo %s excedida (%d > %d)", 
                                entry.getKey(), entry.getValue(), vuelo.getCapacidadMaxima()),
                    TipoViolacion.CAPACIDAD_VUELO_EXCEDIDA
                );
            }
        }
        
        // Verificar capacidades de almacenes
        for (Map.Entry<String, Integer> entry : usosAlmacen.entrySet()) {
            Aeropuerto aeropuerto = aeropuertos.get(entry.getKey());
            if (aeropuerto != null && entry.getValue() > aeropuerto.getCapacidadAlmacen()) {
                resultado.agregarViolacion(
                    String.format("Capacidad de almacén %s excedida (%d > %d)", 
                                entry.getKey(), entry.getValue(), aeropuerto.getCapacidadAlmacen()),
                    TipoViolacion.CAPACIDAD_ALMACEN_EXCEDIDA
                );
            }
        }
    }
    
    /**
     * Verifica si una ruta es factible sin agregarla a la solución.
     */
    public boolean esRutaFactible(String paqueteId, Ruta ruta, Solucion solucionActual) {
        ResultadoValidacion resultado = validarRuta(paqueteId, ruta);
        
        // Verificar impacto en capacidades globales
        if (resultado.esFactible()) {
            // Simular adición temporal de la ruta
            Solucion solucionTemporal = solucionActual.copiar();
            solucionTemporal.agregarRuta(paqueteId, ruta);
            
            ResultadoValidacion validacionGlobal = new ResultadoValidacion();
            validarCapacidadesGlobales(solucionTemporal, validacionGlobal);
            
            return validacionGlobal.esFactible();
        }
        
        return false;
    }
    
    /**
     * Valida capacidades de almacenes considerando el flujo temporal de paquetes.
     * Simula minuto a minuto el flujo de paquetes para detectar picos de ocupación.
     * 
     * @param solucion solución a validar
     * @return true si no hay violaciones temporales, false si las hay
     */
    public boolean validarCapacidadesTemporales(Solucion solucion) {
        // DEBUG: Verificar que se está ejecutando
        System.out.println("   [VALIDACIÓN TEMPORAL] Iniciando validación temporal para " + solucion.getRutasPaquetes().size() + " paquetes");
        
        // Matriz temporal: [aeropuerto][minuto_del_dia] = ocupación_actual
        Map<String, int[]> ocupacionTemporal = new HashMap<>();
        final int MINUTES_PER_DAY = 24 * 60; // 1440 minutos
        
        // Inicializar matriz temporal para todos los aeropuertos
        for (String codigoIATA : aeropuertos.keySet()) {
            ocupacionTemporal.put(codigoIATA, new int[MINUTES_PER_DAY]);
        }
        
        // Simular flujo temporal de cada paquete
        int violacionesTemporales = 0;
        for (Map.Entry<String, Ruta> entry : solucion.getRutasPaquetes().entrySet()) {
            String paqueteId = entry.getKey();
            Ruta ruta = entry.getValue();
            
            if (!simularFlujoTemporalPaquete(paqueteId, ruta, ocupacionTemporal)) {
                violacionesTemporales++;
                System.out.println("   [VIOLACIÓN TEMPORAL] Paquete " + paqueteId + " viola capacidad temporal");
            }
        }
        
        if (violacionesTemporales > 0) {
            System.out.println("   [VALIDACIÓN TEMPORAL] " + violacionesTemporales + " violaciones temporales detectadas");
            return false; // Se encontró una violación de capacidad temporal
        }
        
        // DEBUG: Mostrar picos de ocupación
        System.out.println("   [VALIDACIÓN TEMPORAL] ✅ Sin violaciones temporales");
        mostrarPicosOcupacion(ocupacionTemporal);
        return true; // No hay violaciones de capacidad temporal
    }
    
    /**
     * Simula el flujo temporal de un paquete a través de su ruta asignada.
     * 
     * @param paqueteId ID del paquete
     * @param ruta ruta asignada al paquete
     * @param ocupacionTemporal matriz temporal de ocupación
     * @return true si no viola capacidades, false si las viola
     */
    private boolean simularFlujoTemporalPaquete(String paqueteId, Ruta ruta, 
                                               Map<String, int[]> ocupacionTemporal) {
        if (ruta.getSegmentos().isEmpty()) {
            // Paquete ya está en destino, solo verificar capacidad final
            String aeropuertoDestino = ruta.getAeropuertoDestino();
            if (aeropuertoDestino != null) {
                return agregarOcupacionTemporal(aeropuertoDestino, 0, 1440, ocupacionTemporal);
            }
            return true;
        }
        
        // Simular flujo temporal del paquete
        int minutoActual = 0; // Inicio del día (se puede ajustar según hora de pedido)
        String aeropuertoActual = ruta.getAeropuertoOrigen();
        
        for (int i = 0; i < ruta.getSegmentos().size(); i++) {
            SegmentoRuta segmento = ruta.getSegmentos().get(i);
            String aeropuertoOrigen = segmento.getAeropuertoOrigen();
            String aeropuertoDestino = segmento.getAeropuertoDestino();
            
            // 1. Paquete llega al aeropuerto de origen (si no es el inicial)
            if (i == 0 && !aeropuertoOrigen.equals(aeropuertoActual)) {
                // El paquete inicia su viaje desde el aeropuerto de origen
                if (!agregarOcupacionTemporal(aeropuertoOrigen, minutoActual, 30, ocupacionTemporal)) {
                    return false; // Violación de capacidad en aeropuerto origen
                }
            }
            
            // 2. Tiempo de espera en aeropuerto origen (antes del vuelo)
            int tiempoEspera = 30; // 30 minutos de espera antes del vuelo
            if (!agregarOcupacionTemporal(aeropuertoOrigen, minutoActual, tiempoEspera, ocupacionTemporal)) {
                return false; // Violación de capacidad durante espera
            }
            minutoActual += tiempoEspera;
            
            // 3. Tiempo de vuelo (paquete no está en almacén)
            int tiempoVuelo = (int)(segmento.getDuracionHoras() * 60); // Convertir a minutos
            minutoActual += tiempoVuelo;
            
            // 4. Paquete llega al aeropuerto destino
            int tiempoEnDestino = 60; // 1 hora en almacén de destino
            if (!agregarOcupacionTemporal(aeropuertoDestino, minutoActual, tiempoEnDestino, ocupacionTemporal)) {
                return false; // Violación de capacidad en aeropuerto destino
            }
            minutoActual += tiempoEnDestino;
            
            // 5. Tiempo de conexión (si no es el último segmento)
            if (i < ruta.getSegmentos().size() - 1) {
                int tiempoConexion = 120; // 2 horas de conexión
                if (!agregarOcupacionTemporal(aeropuertoDestino, minutoActual, tiempoConexion, ocupacionTemporal)) {
                    return false; // Violación de capacidad durante conexión
                }
                minutoActual += tiempoConexion;
            }
        }
        
        return true; // No hay violaciones de capacidad temporal
    }
    
    /**
     * Agrega ocupación temporal a un aeropuerto durante un período de tiempo.
     * 
     * @param codigoIATA código IATA del aeropuerto
     * @param minutoInicio minuto de inicio (0-1439)
     * @param duracionMinutos duración en minutos
     * @param ocupacionTemporal matriz temporal de ocupación
     * @return true si no excede capacidad, false si la excede
     */
    private boolean agregarOcupacionTemporal(String codigoIATA, int minutoInicio, int duracionMinutos,
                                           Map<String, int[]> ocupacionTemporal) {
        Aeropuerto aeropuerto = aeropuertos.get(codigoIATA);
        if (aeropuerto == null) {
            return false; // Aeropuerto no existe
        }
        
        int[] ocupacionArray = ocupacionTemporal.get(codigoIATA);
        // DEBUG: Reducir capacidad temporalmente para forzar validación
        int capacidadMaxima = Math.min(aeropuerto.getCapacidadAlmacen(), 50); // Máximo 50 paquetes por almacén
        
        // Verificar y agregar ocupación para cada minuto del período
        for (int minuto = minutoInicio; minuto < Math.min(minutoInicio + duracionMinutos, 1440); minuto++) {
            ocupacionArray[minuto]++;
            if (ocupacionArray[minuto] > capacidadMaxima) {
                return false; // Violación de capacidad temporal
            }
        }
        
        return true; // No hay violación de capacidad
    }
    
    /**
     * Encuentra el minuto del día con mayor ocupación en un aeropuerto específico.
     * 
     * @param codigoIATA código IATA del aeropuerto
     * @param ocupacionTemporal matriz temporal de ocupación
     * @return array [minuto, ocupación_máxima]
     */
    public int[] encontrarPicoOcupacion(String codigoIATA, Map<String, int[]> ocupacionTemporal) {
        int[] ocupacionArray = ocupacionTemporal.get(codigoIATA);
        if (ocupacionArray == null) {
            return new int[]{0, 0};
        }
        
        int maxOcupacion = 0;
        int minutoPico = 0;
        
        for (int minuto = 0; minuto < 1440; minuto++) {
            if (ocupacionArray[minuto] > maxOcupacion) {
                maxOcupacion = ocupacionArray[minuto];
                minutoPico = minuto;
            }
        }
        
        return new int[]{minutoPico, maxOcupacion};
    }
    
    /**
     * Valida una solución completa incluyendo validación temporal de almacenes.
     * 
     * @param solucion solución a validar
     * @return resultado de validación con información temporal
     */
    public ResultadoValidacion validarSolucionCompleta(Solucion solucion) {
        ResultadoValidacion resultado = validarSolucion(solucion);
        
        // Agregar validación temporal
        if (resultado.esFactible()) {
            boolean validacionTemporal = validarCapacidadesTemporales(solucion);
            if (!validacionTemporal) {
                resultado.agregarViolacion(
                    "Violación de capacidad temporal en almacenes", 
                    TipoViolacion.CAPACIDAD_ALMACEN_EXCEDIDA
                );
            }
        }
        
        return resultado;
    }
    
    /**
     * Muestra los picos de ocupación temporal para debugging.
     */
    private void mostrarPicosOcupacion(Map<String, int[]> ocupacionTemporal) {
        System.out.println("   [PICOS OCUPACIÓN] Analizando picos de ocupación...");
        
        for (Map.Entry<String, int[]> entry : ocupacionTemporal.entrySet()) {
            String codigoIATA = entry.getKey();
            int[] ocupacionArray = entry.getValue();
            Aeropuerto aeropuerto = aeropuertos.get(codigoIATA);
            
            if (aeropuerto == null) continue;
            
            int maxOcupacion = 0;
            int minutoPico = 0;
            
            for (int minuto = 0; minuto < 1440; minuto++) {
                if (ocupacionArray[minuto] > maxOcupacion) {
                    maxOcupacion = ocupacionArray[minuto];
                    minutoPico = minuto;
                }
            }
            
            if (maxOcupacion > 0) {
                int horaPico = minutoPico / 60;
                int minutoPicoHora = minutoPico % 60;
                double porcentajeOcupacion = (maxOcupacion * 100.0) / aeropuerto.getCapacidadAlmacen();
                
                System.out.println("     " + codigoIATA + ": " + maxOcupacion + "/" + aeropuerto.getCapacidadAlmacen() + 
                                 " (" + String.format("%.1f", porcentajeOcupacion) + "%) a las " + 
                                 String.format("%02d:%02d", horaPico, minutoPicoHora));
            }
        }
    }
}
