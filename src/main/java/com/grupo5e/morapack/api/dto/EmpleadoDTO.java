package com.grupo5e.morapack.api.dto;

import com.grupo5e.morapack.core.enums.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para representar un empleado")
public class EmpleadoDTO {

    @Schema(description = "ID único del empleado", example = "1")
    private Long id;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
    @Schema(description = "Nombres del empleado", example = "María", required = true)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    @Schema(description = "Apellidos del empleado", example = "García", required = true)
    private String apellidos;

    @Size(max = 50, message = "El puesto no puede exceder 50 caracteres")
    @Schema(description = "Puesto o cargo del empleado", example = "Supervisor")
    private String puesto;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "El teléfono debe estar en formato internacional E.164")
    @Schema(description = "Teléfono en formato internacional", example = "+51987654321")
    private String telefono;

    @NotBlank(message = "El username/email es obligatorio")
    @Schema(description = "Username o email para login", example = "maria.garcia", required = true)
    private String usernameOrEmail;

    @Schema(description = "Contraseña (solo para creación/actualización)")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    @Schema(description = "Rol del empleado", example = "EMPLEADO", required = true)
    private Rol rol;

    @Schema(description = "Indica si el empleado está activo", example = "true")
    private Boolean activo;
}

