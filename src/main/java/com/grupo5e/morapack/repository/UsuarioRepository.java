package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
