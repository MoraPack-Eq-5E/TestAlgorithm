package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.algorithm.ts.TSSolver;

public class DemoTS {
    public static void main(String[] args) {
        System.out.println("Iniciando Solución MoraPack con Tabu Search");

        // Crear una nueva instancia del solver TS
        TSSolver solver = new TSSolver();

        // Ejecutar el método resolver
        solver.resolver();

        System.out.println("Solución MoraPack Tabu Search completada");
    }
}
