package com.grupo5e.morapack;

import com.grupo5e.morapack.core.enums.Continente;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.service.CiudadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CiudadServiceTest {

    @Autowired
    private CiudadService ciudadService;

    @Test
    void testInsertarCiudad() {
        Ciudad ciudad = new Ciudad();
        ciudad.setNombre("Lima");
        ciudad.setPais("Perú");
        ciudad.setContinente(Continente.AMERICA);

        Long idGenerado = ciudadService.insertar(ciudad);

        assertThat(idGenerado).isNotNull();
        assertThat(idGenerado).isGreaterThan(0);
    }
}
