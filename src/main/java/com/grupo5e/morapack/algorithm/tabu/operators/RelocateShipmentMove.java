package com.grupo5e.morapack.algorithm.tabu.operators;

import com.grupo5e.morapack.algorithm.tabu.Move;
import com.grupo5e.morapack.algorithm.tabu.adapters.RoutingAdapters;

public class RelocateShipmentMove<S, K, R> implements Move<S> {
    private final K envioId;
    private final R rutaDestino;
    private final RoutingAdapters<S, K, R> adapters;
    private R rutaOrigen;

    public RelocateShipmentMove(K envioId, R rutaDestino, RoutingAdapters<S, K, R> adapters) {
        this.envioId = envioId;
        this.rutaDestino = rutaDestino;
        this.adapters = adapters;
    }

    @Override public void apply(S solution) {
        rutaOrigen = adapters.getRutaActualDeEnvio(solution, envioId);
        if (rutaOrigen != null) adapters.quitarEnvioDeRuta(solution, envioId, rutaOrigen);
        adapters.asignarEnvioARuta(solution, envioId, rutaDestino);
    }

    @Override public void undo(S solution) {
        adapters.quitarEnvioDeRuta(solution, envioId, rutaDestino);
        if (rutaOrigen != null) adapters.asignarEnvioARuta(solution, envioId, rutaOrigen);
    }

    @Override public Object attributeKey() {
        return "relocate:"+envioId+":"+(rutaOrigen==null?"none":rutaOrigen.toString());
    }
    @Override public double deltaCost() { return Double.NaN; }
}
