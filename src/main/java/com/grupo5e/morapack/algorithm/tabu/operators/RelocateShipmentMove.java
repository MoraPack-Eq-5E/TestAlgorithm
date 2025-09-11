package com.grupo5e.morapack.algorithm.tabu.operators;

import com.grupo5e.morapack.algorithm.tabu.Move;

import java.util.function.BiFunction;

public class RelocateShipmentMove<S> implements Move<S> {
    private final Object envioId;
    private final Object rutaDestino;
    private final BiFunction<S, Object, Object> getRutaActual;
    private final Tri<S, Object, Object> asignar;    // (sol, envio, ruta)
    private final Tri<S, Object, Object> desasignar; // (sol, envio, ruta)
    private Object rutaOrigen;

    public RelocateShipmentMove(Object envioId,
                                Object rutaDestino,
                                BiFunction<S, Object, Object> getRutaActual,
                                Tri<S, Object, Object> asignar,
                                Tri<S, Object, Object> desasignar) {
        this.envioId = envioId;
        this.rutaDestino = rutaDestino;
        this.getRutaActual = getRutaActual;
        this.asignar = asignar;
        this.desasignar = desasignar;
    }

    @Override public void apply(S solution) {
        rutaOrigen = getRutaActual.apply(solution, envioId);
        if (rutaOrigen != null) desasignar.accept(solution, envioId, rutaOrigen);
        asignar.accept(solution, envioId, rutaDestino);
    }

    @Override public void undo(S solution) {
        desasignar.accept(solution, envioId, rutaDestino);
        if (rutaOrigen != null) asignar.accept(solution, envioId, rutaOrigen);
    }

    @Override public Object attributeKey() {
        // Prohibimos "volver a traer" el mismo envío desde la misma ruta inmediatamente.
        return "relocate:" + envioId + ":" + (rutaOrigen == null ? "none" : rutaOrigen.toString());
    }

    @Override public double deltaCost() { return Double.NaN; }

    @FunctionalInterface public interface Tri<A,B,C> { void accept(A a, B b, C c); }
}
