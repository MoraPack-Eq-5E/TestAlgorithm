package com.grupo5e.morapack.api.auth.dto;

import com.grupo5e.morapack.core.enums.Rol;

public record AuthResponse(
        Long id,
        String email,
        String name,   // si luego tienes “nombre” en otra entidad, por ahora deja fijo o null
        Rol role,
        String token,
        String refreshToken
) {}
