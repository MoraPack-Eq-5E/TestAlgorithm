package com.grupo5e.morapack.algorithm.tabu.adapters;

import com.grupo5e.morapack.core.model.Ruta;
import com.grupo5e.morapack.core.model.Solucion;

public class MoraPackRoutingAdapters implements RoutingAdapters<Solucion, String, Ruta> {

    @Override
    public Ruta getRutaActualDeEnvio(Solucion solution, String envioId) {
        return solution.getRutasPaquetes().get(envioId); // lee del mapa paquete->ruta
    }

    @Override
    public void asignarEnvioARuta(Solucion solution, String envioId, Ruta ruta) {
        if (solution.getRutasPaquetes().containsKey(envioId)) {
            solution.reemplazarRuta(envioId, ruta);       // reemplaza y recalcula métricas
        } else {
            solution.agregarRuta(envioId, ruta);          // agrega y recalcula métricas
        }
    }

    @Override
    public void quitarEnvioDeRuta(Solucion solution, String envioId, Ruta ruta) {
        solution.removerRuta(envioId);                    // quita y recalcula métricas
    }
}
