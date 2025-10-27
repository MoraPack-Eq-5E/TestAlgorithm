package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Usuario;
import com.grupo5e.morapack.core.enums.Rol;

import java.util.List;
import java.util.Optional;

public interface UsuarioService {
    List<Usuario> listar();
    Long insertar(Usuario usuario);
    Usuario actualizar(Long id, Usuario usuario);
    Usuario buscarPorId(Long id);
    Optional<Usuario> buscarPorUsername(String username);
    List<Usuario> buscarPorRol(Rol rol);
    void eliminar(Long id);
    boolean existePorId(Long id);
}
