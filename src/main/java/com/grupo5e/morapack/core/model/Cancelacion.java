package com.grupo5e.morapack.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Cancelacion {

    private int diasCancelado;
    private String codigoIATAOrigen;
    private String codigoIATADestino;
    private int hora;
    private int minuto;

}
