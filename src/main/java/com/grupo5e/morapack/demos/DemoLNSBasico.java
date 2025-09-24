package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.algorithm.validation.ValidadorRestricciones;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;
import com.grupo5e.morapack.utils.MoraPackDataLoader;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

/**
 * Demo del LNS básico validado con capacidades reales
 */
public class DemoLNSBasico {
    
    public static void main(String[] args) {
        System.out.println("🚀 Demo LNS Básico Validado - MoraPack");
        System.out.println("======================================");
        
        try {
            // Cargar datos
            MoraPackDataLoader.DatosPrueba datos = MoraPackDataLoader.cargarDatosPrueba();
            List<Aeropuerto> aeropuertos = MoraPackDataLoader.cargarAeropuertos();
            List<Vuelo> vuelos = MoraPackDataLoader.cargarVuelos(aeropuertos);
            
            // Si hay pocos paquetes, cargar desde CSV
            if (datos.paquetes.size() < 10) {
                System.out.println("⚠️  Pocos paquetes cargados (" + datos.paquetes.size() + "), cargando desde CSV...");
                datos = cargarDatosDesdeCSV();
            }
            
            ContextoProblema contexto = new ContextoProblema(datos.paquetes, aeropuertos, vuelos, Set.of());
            ValidadorRestricciones validador = new ValidadorRestricciones(aeropuertos, vuelos, Set.of());
            
            // Crear solución inicial vacía (el LNS la llenará automáticamente)
            Solucion solucionInicial = new Solucion();
            
            // Configuración LNS básico
            int iteraciones = 50;
            double temperaturaInicial = 1000.0;
            double factorEnfriamiento = 0.95;
            double porcentajeDestruccion = 0.3; // 30% de paquetes
            
            LNSBasicoValidado lns = new LNSBasicoValidado(
                contexto, 
                validador,
                iteraciones, 
                temperaturaInicial, 
                factorEnfriamiento, 
                porcentajeDestruccion
            );
            
            System.out.println("📊 Configuración:");
            System.out.println("   - Iteraciones: " + iteraciones);
            System.out.println("   - Temperatura inicial: " + temperaturaInicial);
            System.out.println("   - Factor enfriamiento: " + factorEnfriamiento);
            System.out.println("   - % Destrucción: " + (porcentajeDestruccion * 100) + "%");
            System.out.println("   - Paquetes totales: " + datos.paquetes.size());
            System.out.println("   - Aeropuertos: " + aeropuertos.size() + " (con capacidades reales)");
            System.out.println("   - Vuelos: " + vuelos.size());
            System.out.println();
            
            // Ejecutar LNS
            System.out.println("🔄 Ejecutando LNS básico validado...");
            long inicio = System.currentTimeMillis();
            
            Solucion mejorSolucion = lns.resolver(solucionInicial);
            
            long tiempo = System.currentTimeMillis() - inicio;
            
            // Resultados
            System.out.println();
            System.out.println("✅ LNS completado en " + tiempo + "ms");
            System.out.println("📈 Resultados finales:");
            System.out.println("   - Fitness final: " + mejorSolucion.getFitness());
            System.out.println("   - Paquetes ruteados: " + mejorSolucion.getCantidadPaquetes() + "/" + datos.paquetes.size());
            System.out.println("   - Costo total: " + mejorSolucion.getCostoTotal());
            System.out.println("   - Tiempo total: " + mejorSolucion.getTiempoTotalHoras() + " horas");
            System.out.println("   - Violaciones: " + mejorSolucion.getViolacionesRestricciones());
            System.out.println("   - Factible: " + mejorSolucion.isEsFactible());
            
        } catch (Exception e) {
            System.err.println("❌ Error en demo LNS: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carga datos desde el archivo CSV de pedidos generados
     */
    private static MoraPackDataLoader.DatosPrueba cargarDatosDesdeCSV() {
        List<Paquete> paquetes = new ArrayList<>();
        List<Cliente> clientes = new ArrayList<>();
        List<Pedido> pedidos = new ArrayList<>();
        List<String> cancelaciones = new ArrayList<>();
        List<String> demoras = new ArrayList<>();
        
        try {
            List<String> lineas = java.nio.file.Files.readAllLines(java.nio.file.Paths.get("data/pedidos_generados.csv"));
            
            // Saltar la primera línea (header) y limitar a 100 paquetes
            int paquetesCargados = 0;
            int maxPaquetes = 100;
            
            for (int i = 1; i < lineas.size() && paquetesCargados < maxPaquetes; i++) {
                String linea = lineas.get(i).trim();
                if (linea.isEmpty()) continue;
                
                String[] partes = linea.split(",");
                if (partes.length < 6) continue;
                
                // Formato: dd,hh,mm,dest,###,IdClien
                String destino = partes[3].trim();
                String cantidad = partes[4].trim();
                String clienteId = partes[5].trim();
                
                // Crear cliente si no existe
                Cliente cliente = new Cliente(clienteId, "Cliente " + clienteId, "cliente" + clienteId + "@morapack.com", "SPIM");
                if (!clientes.stream().anyMatch(c -> c.getId().equals(clienteId))) {
                    clientes.add(cliente);
                }
                
                // Crear paquetes según la cantidad, respetando el límite
                int cant = Integer.parseInt(cantidad);
                int paquetesACrear = Math.min(cant, maxPaquetes - paquetesCargados);
                
                for (int j = 0; j < paquetesACrear; j++) {
                    String paqueteId = "PKG_" + clienteId + "_" + i + "_" + j;
                    Paquete paquete = new Paquete(paqueteId, null, destino, clienteId); // origen = null (se asigna dinámicamente)
                    paquete.setPrioridad(2); // MEDIA por defecto
                    paquete.setFechaLimiteEntrega(java.time.LocalDateTime.now().plusDays(2));
                    paquetes.add(paquete);
                    paquetesCargados++;
                }
                
                // Si ya llegamos al límite, salir del bucle
                if (paquetesCargados >= maxPaquetes) {
                    break;
                }
            }
            
            System.out.println("✅ Cargados " + paquetes.size() + " paquetes desde CSV (límite: " + maxPaquetes + ")");
            System.out.println("✅ Cargados " + clientes.size() + " clientes desde CSV");
            
        } catch (Exception e) {
            System.err.println("❌ Error al cargar CSV: " + e.getMessage());
            // Usar datos de respaldo si falla
            return MoraPackDataLoader.cargarDatosPrueba();
        }
        
        return new MoraPackDataLoader.DatosPrueba(paquetes, clientes, pedidos, cancelaciones, demoras);
    }
}
