package com.grupo5e.morapack.api.dto;

import com.grupo5e.morapack.core.enums.Rol;
import com.grupo5e.morapack.core.enums.TipoDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para representar un cliente")
public class ClienteDTO {

    @Schema(description = "ID único del cliente", example = "1")
    private Long id;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
    @Schema(description = "Nombres del cliente", example = "Juan Carlos", required = true)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    @Schema(description = "Apellidos del cliente", example = "Pérez González", required = true)
    private String apellidos;

    @NotNull(message = "El tipo de documento es obligatorio")
    @Schema(description = "Tipo de documento de identidad", example = "DNI", required = true)
    private TipoDocumento tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento no puede exceder 20 caracteres")
    @Schema(description = "Número de documento de identidad", example = "12345678", required = true)
    private String numeroDocumento;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    @Schema(description = "Correo electrónico del cliente", example = "juan.perez@email.com", required = true)
    private String correo;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "El teléfono debe estar en formato internacional E.164")
    @Schema(description = "Teléfono en formato internacional", example = "+51987654321")
    private String telefono;

    @Schema(description = "ID de la ciudad de recojo", example = "1")
    private Integer ciudadRecojoId;

    @Schema(description = "Información de la ciudad de recojo")
    private CiudadSimpleDTO ciudadRecojo;

    @NotBlank(message = "El username/email es obligatorio")
    @Schema(description = "Username o email para login", example = "juan.perez@email.com", required = true)
    private String usernameOrEmail;

    @Schema(description = "Contraseña (solo para creación/actualización, no se retorna en consultas)")
    private String password;

    @Schema(description = "Rol del usuario", example = "CLIENTE")
    private Rol rol;

    @Schema(description = "Indica si el usuario está activo", example = "true")
    private Boolean activo;
}

