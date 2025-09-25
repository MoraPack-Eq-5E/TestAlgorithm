package com.grupo5e.morapack.algorithm.ts;

import com.grupo5e.morapack.core.model.Paquete;
import com.grupo5e.morapack.core.model.Vuelo;

import java.util.ArrayList;

public class TSMove {
    public String tipo; // "RELOCATE", "SWAP", "INSERT", "REMOVE"
    public Paquete paquete1;
    public Paquete paquete2; // Solo para SWAP
    public ArrayList<Vuelo> rutaAnterior1;
    public ArrayList<Vuelo> rutaNueva1;
    public ArrayList<Vuelo> rutaAnterior2; // Solo para SWAP
    public ArrayList<Vuelo> rutaNueva2; // Solo para SWAP
    public int iteracionCreacion;

    // Constructor para movimientos RELOCATE, INSERT, REMOVE
    public TSMove(String tipo, Paquete paquete1, ArrayList<Vuelo> rutaAnterior, ArrayList<Vuelo> rutaNueva) {
        this.tipo = tipo;
        this.paquete1 = paquete1;
        this.paquete2 = null;
        this.rutaAnterior1 = rutaAnterior != null ? new ArrayList<>(rutaAnterior) : null;
        this.rutaNueva1 = rutaNueva != null ? new ArrayList<>(rutaNueva) : null;
        this.rutaAnterior2 = null;
        this.rutaNueva2 = null;
        this.iteracionCreacion = 0;
    }

    // Constructor para movimientos SWAP
    public TSMove(String tipo, Paquete paquete1, Paquete paquete2,
                    ArrayList<Vuelo> rutaAnterior1, ArrayList<Vuelo> rutaNueva1,
                    ArrayList<Vuelo> rutaAnterior2, ArrayList<Vuelo> rutaNueva2) {
        this.tipo = tipo;
        this.paquete1 = paquete1;
        this.paquete2 = paquete2;
        this.rutaAnterior1 = rutaAnterior1 != null ? new ArrayList<>(rutaAnterior1) : null;
        this.rutaNueva1 = rutaNueva1 != null ? new ArrayList<>(rutaNueva1) : null;
        this.rutaAnterior2 = rutaAnterior2 != null ? new ArrayList<>(rutaAnterior2) : null;
        this.rutaNueva2 = rutaNueva2 != null ? new ArrayList<>(rutaNueva2) : null;
        this.iteracionCreacion = 0;
    }

    /**
     * Verifica si este movimiento es equivalente a otro (para lista tabú)
     */
    public boolean esEquivalente(TSMove otro) {
        if (!this.tipo.equals(otro.tipo)) {
            return false;
        }

        switch (this.tipo) {
            case "RELOCATE":
            case "INSERT":
            case "REMOVE":
                return this.paquete1.getId() == otro.paquete1.getId() &&
                        rutasEquivalentes(this.rutaNueva1, otro.rutaNueva1);

            case "SWAP":
                return (this.paquete1.getId() == otro.paquete1.getId() &&
                        this.paquete2.getId() == otro.paquete2.getId()) ||
                        (this.paquete1.getId() == otro.paquete2.getId() &&
                                this.paquete2.getId() == otro.paquete1.getId());

            default:
                return false;
        }
    }

    /**
     * Verifica si dos rutas son equivalentes
     */
    private boolean rutasEquivalentes(ArrayList<Vuelo> ruta1, ArrayList<Vuelo> ruta2) {
        if (ruta1 == null && ruta2 == null) return true;
        if (ruta1 == null || ruta2 == null) return false;
        if (ruta1.size() != ruta2.size()) return false;

        for (int i = 0; i < ruta1.size(); i++) {
            if (ruta1.get(i).getId() != ruta2.get(i).getId()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Crea el movimiento inverso para revertir este movimiento
     */
    public TSMove crearMovimientoInverso() {
        switch (this.tipo) {
            case "RELOCATE":
                return new TSMove("RELOCATE", this.paquete1, this.rutaNueva1, this.rutaAnterior1);

            case "INSERT":
                return new TSMove("REMOVE", this.paquete1, this.rutaNueva1, null);

            case "REMOVE":
                return new TSMove("INSERT", this.paquete1, null, this.rutaAnterior1);

            case "SWAP":
                return new TSMove("SWAP", this.paquete1, this.paquete2,
                        this.rutaNueva1, this.rutaAnterior1,
                        this.rutaNueva2, this.rutaAnterior2);

            default:
                return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TSMove{tipo=").append(tipo);
        sb.append(", paquete1=").append(paquete1.getId());
        if (paquete2 != null) {
            sb.append(", paquete2=").append(paquete2.getId());
        }
        sb.append(", iteracion=").append(iteracionCreacion);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TSMove TSMove = (TSMove) obj;
        return esEquivalente(TSMove);
    }

    @Override
    public int hashCode() {
        int result = tipo.hashCode();
        result = 31 * result + paquete1.getId();
        if (paquete2 != null) {
            result = 31 * result + paquete2.getId();
        }
        return result;
    }
}
