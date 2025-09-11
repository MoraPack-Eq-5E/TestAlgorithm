package com.grupo5e.morapack.algorithm.tabu.operators;

import com.grupo5e.morapack.algorithm.tabu.Move;
import com.grupo5e.morapack.algorithm.tabu.adapters.RoutingAdapters;

public class SwapShipmentsMove<S, K, R> implements Move<S> {
    private final K envioA, envioB;
    private final RoutingAdapters<S, K, R> adapters;
    private R rutaA, rutaB;

    public SwapShipmentsMove(K envioA, K envioB, RoutingAdapters<S, K, R> adapters) {
        this.envioA = envioA; this.envioB = envioB; this.adapters = adapters;
    }

    @Override public void apply(S solution) {
        rutaA = adapters.getRutaActualDeEnvio(solution, envioA);
        rutaB = adapters.getRutaActualDeEnvio(solution, envioB);
        if (rutaA != null) adapters.quitarEnvioDeRuta(solution, envioA, rutaA);
        if (rutaB != null) adapters.quitarEnvioDeRuta(solution, envioB, rutaB);
        if (rutaB != null) adapters.asignarEnvioARuta(solution, envioA, rutaB);
        if (rutaA != null) adapters.asignarEnvioARuta(solution, envioB, rutaA);
    }

    @Override public void undo(S solution) {
        if (rutaB != null) adapters.quitarEnvioDeRuta(solution, envioA, rutaB);
        if (rutaA != null) adapters.quitarEnvioDeRuta(solution, envioB, rutaA);
        if (rutaA != null) adapters.asignarEnvioARuta(solution, envioA, rutaA);
        if (rutaB != null) adapters.asignarEnvioARuta(solution, envioB, rutaB);
    }

    @Override public Object attributeKey() { return "swap:"+envioA+"<->"+envioB; }
    @Override public double deltaCost() { return Double.NaN; }
}
