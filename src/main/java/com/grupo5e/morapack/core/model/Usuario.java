package com.grupo5e.morapack.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.grupo5e.morapack.core.enums.Rol;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.JOINED)  // Herencia en BD
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Credenciales
    @Column(nullable = false, unique = true, length = 100)
    private String usernameOrEmail; // Para clientes puede ser correo, para empleados un username

    @Column(nullable = false)
    private String password; // Encriptada con BCrypt

    @Enumerated(EnumType.STRING)
    private Rol rol; // CLIENTE, EMPLEADO, ADMIN

    private boolean activo = true;
}
