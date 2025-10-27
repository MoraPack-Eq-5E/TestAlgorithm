package com.grupo5e.morapack.api.mapper;

import com.grupo5e.morapack.api.dto.AeropuertoDTO;
import com.grupo5e.morapack.api.dto.CiudadSimpleDTO;
import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Ciudad;
import org.springframework.stereotype.Component;

@Component
public class AeropuertoMapper {

    public AeropuertoDTO toDTO(Aeropuerto aeropuerto) {
        if (aeropuerto == null) return null;
        
        return AeropuertoDTO.builder()
                .id(aeropuerto.getId())
                .codigoIATA(aeropuerto.getCodigoIATA())
                .zonaHorariaUTC(aeropuerto.getZonaHorariaUTC())
                .latitud(aeropuerto.getLatitud())
                .longitud(aeropuerto.getLongitud())
                .capacidadActual(aeropuerto.getCapacidadActual())
                .capacidadMaxima(aeropuerto.getCapacidadMaxima())
                .ciudadId(aeropuerto.getCiudad() != null ? aeropuerto.getCiudad().getId() : null)
                .ciudad(toCiudadSimpleDTO(aeropuerto.getCiudad()))
                .estado(aeropuerto.getEstado())
                .build();
    }

    public Aeropuerto toEntity(AeropuertoDTO dto) {
        if (dto == null) return null;
        
        Aeropuerto aeropuerto = new Aeropuerto();
        aeropuerto.setId(dto.getId());
        aeropuerto.setCodigoIATA(dto.getCodigoIATA());
        aeropuerto.setZonaHorariaUTC(dto.getZonaHorariaUTC());
        aeropuerto.setLatitud(dto.getLatitud());
        aeropuerto.setLongitud(dto.getLongitud());
        aeropuerto.setCapacidadActual(dto.getCapacidadActual() != null ? dto.getCapacidadActual() : 0);
        aeropuerto.setCapacidadMaxima(dto.getCapacidadMaxima());
        aeropuerto.setEstado(dto.getEstado());
        
        // La ciudad se debe setear desde el controller
        if (dto.getCiudadId() != null) {
            Ciudad ciudad = new Ciudad();
            ciudad.setId(dto.getCiudadId());
            aeropuerto.setCiudad(ciudad);
        }
        
        return aeropuerto;
    }

    private CiudadSimpleDTO toCiudadSimpleDTO(Ciudad ciudad) {
        if (ciudad == null) return null;
        
        return CiudadSimpleDTO.builder()
                .id(ciudad.getId())
                .codigo(ciudad.getCodigo())
                .nombre(ciudad.getNombre())
                .pais(ciudad.getPais())
                .build();
    }
}

