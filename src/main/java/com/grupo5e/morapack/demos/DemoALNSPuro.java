package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.algorithm.alns.ALNSSolver;

public class DemoALNSPuro {
    public static void main(String[] args) {
        System.out.println("Iniciando Solución MoraPack con ALNS Puro");

        // Crear una nueva instancia del solver ALNS
        ALNSSolver solver = new ALNSSolver();

        // Ejecutar el método resolver
        solver.resolver();

        System.out.println("Solución MoraPack ALNS Puro completada");
    }
}