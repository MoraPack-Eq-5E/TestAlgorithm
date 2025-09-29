package com.grupo5e.morapack.experimentos;

/**
 * Experimento comparativo simple entre ALNS y Tabu Search
 */
public class ExperimentoComparativo {

    private static final boolean EJECUTAR_ALNS = true;
    private static final boolean EJECUTAR_TABU = false;

    public static void main(String[] args) {
        System.out.println("=== EXPERIMENTO COMPARATIVO SIMPLE ===");
        System.out.println("ALNS: " + (EJECUTAR_ALNS ? "ACTIVADO" : "DESACTIVADO"));
        System.out.println("TABU: " + (EJECUTAR_TABU ? "ACTIVADO" : "DESACTIVADO"));
        System.out.println();

        if (EJECUTAR_ALNS) {
            ejecutarAlgoritmo("ALNS");
        }

        if (EJECUTAR_TABU) {
            ejecutarAlgoritmo("TABU");
        }

        System.out.println("=== EXPERIMENTO COMPLETADO ===");
    }

    private static void ejecutarAlgoritmo(String nombreAlgoritmo) {
        System.out.println("--- Ejecutando " + nombreAlgoritmo + " ---");

        AlgoritmoOptimizacion algoritmo;

        if ("ALNS".equals(nombreAlgoritmo)) {
            algoritmo = new ALNSSolverExperimental();
        } else if ("TABU".equals(nombreAlgoritmo)) {
            algoritmo = new TabuSearchSolver();
        } else {
            System.out.println("Algoritmo no reconocido: " + nombreAlgoritmo);
            return;
        }

        try {
            ResultadoAlgoritmo resultado = algoritmo.resolver();
            System.out.println("✅ " + resultado);

            // Mostrar métricas detalladas
            mostrarMetricasDetalladas(resultado);

        } catch (Exception e) {
            System.out.println("❌ Error ejecutando " + nombreAlgoritmo + ": " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
    }

    private static void mostrarMetricasDetalladas(ResultadoAlgoritmo resultado) {
        System.out.println("    Métricas detalladas:");
        System.out.printf("    - Fitness: %,d%n", resultado.getFitnessFinal());
        System.out.printf("    - Paquetes asignados: %d/%d (%.1f%%)%n",
                resultado.getPaquetesAsignados(), resultado.getTotalPaquetes(),
                resultado.getPorcentajePaquetesAsignados());
        System.out.printf("    - Tasa entregas a tiempo: %.1f%%%n",
                resultado.getTasaEntregasATiempo() * 100);
        System.out.printf("    - Tiempo promedio entrega: %.1f horas%n",
                resultado.getTiempoPromedioEntrega());
        System.out.printf("    - Iteraciones: %d%n", resultado.getIteracionesTotales());
        System.out.printf("    - Mejoras encontradas: %d%n", resultado.getMejorasEncontradas());
        System.out.printf("    - Tiempo ejecución: %.1f segundos%n",
                resultado.getTiempoEjecucionMs() / 1000.0);
    }
}