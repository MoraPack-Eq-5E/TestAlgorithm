package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Producto {
    private int id;
    private StringBuilder vueloAsignado;
    private Estado estado;
}
