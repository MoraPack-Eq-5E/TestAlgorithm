package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.Continente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ciudades")
public class Ciudad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String codigo; //DE 4 LETRAS SEGUN EL txt
    private String nombre; //nombre de la ciudad
    private String pais; //pais de la ciudad

    @Enumerated(EnumType.STRING)
    private Continente continente;

}
