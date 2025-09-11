package com.grupo5e.morapack.algorithm.tabu;

public interface Move <S>{

    // aplicar el movimiento sobre la solución
    void apply(S solution);

    // revertir nel movimiento aplicado
    void undo(S solution);

    // clave para la lista tabu
    Object attributeKey();

    // delta del costo por si se lleva ocntabilidad incremental
    double deltaCost();

}
