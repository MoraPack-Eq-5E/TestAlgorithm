package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Usuario;

import java.util.List;

public interface UsuarioService {
    List<Usuario> listar();
    Long insertar(Usuario usuario);
    Usuario buscarPorId(Long id);
    void eliminar(Long id);
}
