package com.grupo5e.morapack.algorithm.tabu.operators;

import com.grupo5e.morapack.algorithm.tabu.Move;

import java.util.function.BiFunction;

public class SwapShipmentsMove<S> implements Move<S> {
    private final Object envioA, envioB;
    private final BiFunction<S, Object, Object> getRutaActual;
    private final RelocateShipmentMove.Tri<S, Object, Object> asignar;
    private final RelocateShipmentMove.Tri<S, Object, Object> desasignar;
    private Object rutaA, rutaB;

    public SwapShipmentsMove(Object envioA, Object envioB,
                             BiFunction<S, Object, Object> getRutaActual,
                             RelocateShipmentMove.Tri<S, Object, Object> asignar,
                             RelocateShipmentMove.Tri<S, Object, Object> desasignar) {
        this.envioA = envioA; this.envioB = envioB;
        this.getRutaActual = getRutaActual;
        this.asignar = asignar; this.desasignar = desasignar;
    }

    @Override public void apply(S solution) {
        rutaA = getRutaActual.apply(solution, envioA);
        rutaB = getRutaActual.apply(solution, envioB);

        if (rutaA != null) desasignar.accept(solution, envioA, rutaA);
        if (rutaB != null) desasignar.accept(solution, envioB, rutaB);

        if (rutaB != null) asignar.accept(solution, envioA, rutaB);
        if (rutaA != null) asignar.accept(solution, envioB, rutaA);
    }

    @Override public void undo(S solution) {
        if (rutaB != null) desasignar.accept(solution, envioA, rutaB);
        if (rutaA != null) desasignar.accept(solution, envioB, rutaA);

        if (rutaA != null) asignar.accept(solution, envioA, rutaA);
        if (rutaB != null) asignar.accept(solution, envioB, rutaB);
    }

    @Override public Object attributeKey() {
        // Prohibimos repetir el mismo par de swap enseguida.
        return "swap:" + envioA + "<->" + envioB;
    }

    @Override public double deltaCost() { return Double.NaN; }
}
