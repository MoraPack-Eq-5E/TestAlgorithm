package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.usernameOrEmail) = LOWER(:email)")
    Optional<Usuario> findByUsernameOrEmail(@Param("email") String email);
}
