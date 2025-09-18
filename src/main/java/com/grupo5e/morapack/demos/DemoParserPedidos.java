package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.service.ParserPedidos;

/**
 * Demo para mostrar el funcionamiento del ParserPedidos
 */
public class DemoParserPedidos {
    
    public static void main(String[] args) {
        System.out.println("🚀 DEMO: ParserPedidos");
        System.out.println("=" .repeat(50));
        
        // Probar validación de formato
        System.out.println("🔍 Probando validación de formato:");
        String[] lineasPrueba = {
            "01-08-30-SVMI-150-CLI001",  // ✅ Válida
            "32-25-70-INVALID-9999-CLI999", // ❌ Inválida
            "01-08-30-SVMI-150-CLI001",  // ✅ Válida
            "invalid-format",             // ❌ Inválida
            "01-08-30-SVMI-150-CLI001"   // ✅ Válida
        };
        
        for (String linea : lineasPrueba) {
            boolean esValida = Pedido.validarFormatoArchivo(linea);
            System.out.println("   " + (esValida ? "✅" : "❌") + " " + linea);
        }
        
        System.out.println("\n📁 Probando creación de pedidos desde archivo:");
        
        // Crear pedidos manualmente para demostrar
        try {
            Pedido pedido1 = Pedido.crearDesdeArchivo("01-08-30-SVMI-150-CLI001", "demo.txt", 1);
            System.out.println("✅ Pedido 1: " + pedido1.toString());
            
            Pedido pedido2 = Pedido.crearDesdeArchivo("02-14-15-SBBR-075-CLI002", "demo.txt", 2);
            System.out.println("✅ Pedido 2: " + pedido2.toString());
            
            Pedido pedido3 = Pedido.crearDesdeArchivo("03-16-45-SEQM-200-CLI003", "demo.txt", 3);
            System.out.println("✅ Pedido 3: " + pedido3.toString());
            
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println("\n📊 Probando validación de archivo completo:");
        
        // Validar el archivo de ejemplo
        ParserPedidos.ResultadoValidacion resultado = ParserPedidos.validarArchivo("data/pedidos_mes1.txt");
        System.out.println("   " + resultado.toString());
        
        if (!resultado.isValido()) {
            System.out.println("   Errores encontrados:");
            for (String error : resultado.getErrores()) {
                System.out.println("     - " + error);
            }
        }
        
        System.out.println("\n🎯 Demo completado exitosamente!");
    }
}
