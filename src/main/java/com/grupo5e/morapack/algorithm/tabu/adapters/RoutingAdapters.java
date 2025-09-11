package com.grupo5e.morapack.algorithm.tabu.adapters;

/**
 * Adaptador mínimo para que los operadores Tabu (Relocate / Swap) puedan:
 *  1) saber en qué ruta está hoy un envío,
 *  2) quitar ese envío de su ruta actual,
 *  3) asignarlo a una nueva ruta.
 *
 * S  = tipo de tu solución (p.ej., Solucion)
 * K  = tipo del identificador de envío/paquete (p.ej., String)
 * R  = tipo de la ruta (p.ej., Ruta)
 */
public interface RoutingAdapters<S, K, R> {
    /** Devuelve la ruta actual en la que está el envío (o null si no tiene). */
    R getRutaActualDeEnvio(S solution, K envioId);

    /** Asigna el envío a la ruta indicada (debe actualizar métricas si corresponde). */
    void asignarEnvioARuta(S solution, K envioId, R ruta);

    /** Quita el envío de la ruta indicada (debe actualizar métricas si corresponde). */
    void quitarEnvioDeRuta(S solution, K envioId, R ruta);
}
