package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.Continente;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Ciudad {
    private int id;
    private String nombre;
    private Continente continente;
}
