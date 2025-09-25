package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.EstadoAeropuerto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Aeropuerto {
    private int id;
    private String codigoIATA;
    private String alias;
    private int zonaHorariaUTC;
    private String latitud;
    private String longitud;
    private Ciudad ciudad;
    private EstadoAeropuerto estado;
    private Almacen almacen;
}
