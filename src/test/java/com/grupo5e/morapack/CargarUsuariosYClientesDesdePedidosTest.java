package com.grupo5e.morapack.utils;

import com.grupo5e.morapack.core.enums.Rol;
import com.grupo5e.morapack.core.enums.TipoDocumento;
import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.core.model.Cliente;
import com.grupo5e.morapack.service.AeropuertoService;
import com.grupo5e.morapack.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * Inserta USUARIOS y CLIENTES a partir de pedidos.txt
 * El cliente se vincula con la ciudad del aeropuerto destino.
 *
 * Formato: diasPrioridad hora minuto codigoDestino cantidadProductos idCliente
 * Ejemplo: 04 16 22 EDDI 100 6084676
 */
@SpringBootTest
public class CargarUsuariosYClientesDesdePedidosTest {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private AeropuertoService aeropuertoService;

    private final String RUTA_ARCHIVO = "src/test/resources/pedidos.txt";

    @Test
    void cargarUsuariosYClientes() {
        try (BufferedReader reader = new BufferedReader(new FileReader(RUTA_ARCHIVO))) {

            // 🔹 Mapa de aeropuertos
            List<Aeropuerto> aeropuertos = aeropuertoService.listar();
            if (aeropuertos.isEmpty()) {
                System.err.println("[ERROR] No hay aeropuertos cargados en la base de datos.");
                return;
            }

            Map<String, Aeropuerto> mapaAeropuertos = new HashMap<>();
            for (Aeropuerto a : aeropuertos) {
                if (a.getCodigoIATA() != null)
                    mapaAeropuertos.put(a.getCodigoIATA().trim().toUpperCase(), a);
            }

            // 🔹 BCrypt para contraseñas
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

            // 🔹 Control para no duplicar
            Set<Long> idsInsertados = new HashSet<>();
            Random random = new Random();
            int totalInsertados = 0;

            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;

                String[] partes = linea.trim().split("\\s+");
                if (partes.length != 6) continue;

                Long idCliente = Long.parseLong(partes[5]);
                String codigoDestino = partes[3].trim().toUpperCase();

                // Evitar duplicados
                if (idsInsertados.contains(idCliente)) continue;

                Aeropuerto aeropuertoDestino = mapaAeropuertos.get(codigoDestino);
                if (aeropuertoDestino == null) {
                    System.err.println("[WARN] Aeropuerto no encontrado: " + codigoDestino);
                    continue;
                }

                Ciudad ciudadRecojo = aeropuertoDestino.getCiudad();

                // --- Crear cliente (hereda de Usuario) ---
                Cliente cliente = new Cliente();
                cliente.setId(idCliente);
                cliente.setNombres("Cliente " + idCliente);
                cliente.setApellidos("Autogenerado");
                cliente.setTipoDocumento(TipoDocumento.ID_NACIONAL);
                cliente.setNumeroDocumento("DNI" + idCliente);
                cliente.setCorreo("cliente" + idCliente + "@morapack.com");
                cliente.setTelefono("+51" + (900000000 + random.nextInt(9999999)));
                cliente.setCiudadRecojo(ciudadRecojo);

                // --- Campos de Usuario ---
                cliente.setUsernameOrEmail(cliente.getCorreo());
                cliente.setPassword(passwordEncoder.encode("123456")); // contraseña genérica
                cliente.setRol(Rol.CLIENTE);
                cliente.setActivo(true);

                try {
                    clienteService.insertar(cliente);
                    idsInsertados.add(idCliente);
                    totalInsertados++;
                    System.out.printf("[OK] Cliente creado ID=%d, Ciudad=%s%n",
                            idCliente, ciudadRecojo.getNombre());
                } catch (Exception e) {
                    System.err.println("[ERROR] No se pudo guardar cliente " + idCliente + ": " + e.getMessage());
                }
            }

            System.out.println("\n✅ Total de clientes insertados: " + totalInsertados);

        } catch (Exception e) {
            System.err.println("[ERROR] Falló la lectura del archivo: " + e.getMessage());
        }
    }
}
