package com.grupo5e.morapack;

import com.grupo5e.morapack.core.enums.EstadoAeropuerto;
import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.service.AeropuertoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AeropuertoServiceTest {

    @Autowired
    private AeropuertoService aeropuertoService;

    @Test
    void testInsertarAeropuerto() {
        // Crear ciudad dummy (simula que ya existe en BD)
        Ciudad ciudad = new Ciudad();
        ciudad.setId(1); // Debe existir en la BD para que funcione la FK

        // Crear aeropuerto
        Aeropuerto aeropuerto = new Aeropuerto();
        aeropuerto.setCodigo("SPIM");
        aeropuerto.setZonaHorariaUTC(-5);
        aeropuerto.setLatitud("-12.0219");
        aeropuerto.setLongitud("-77.1143");
        aeropuerto.setCapacidadActual(100);
        aeropuerto.setCapacidadMaxima(1000);
        aeropuerto.setEstado(EstadoAeropuerto.DISPONIBLE);
        aeropuerto.setCiudad(ciudad);

        // Guardar aeropuerto
        Long idGenerado = aeropuertoService.insertar(aeropuerto);

        // Validaciones
        assertThat(idGenerado).isNotNull();
        assertThat(idGenerado).isGreaterThan(0);
    }
}
