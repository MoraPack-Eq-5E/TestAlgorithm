package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO genérico para operaciones bulk")
public class BulkRequestDTO<T> {

    @NotEmpty(message = "La lista de items no puede estar vacía")
    @Valid
    @Schema(description = "Lista de elementos a procesar en bulk", required = true)
    private List<T> items;
}

