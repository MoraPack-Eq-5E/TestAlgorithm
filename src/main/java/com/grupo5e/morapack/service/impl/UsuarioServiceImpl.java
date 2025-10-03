package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.core.model.Usuario;
import com.grupo5e.morapack.repository.EmpleadoRepository;
import com.grupo5e.morapack.repository.UsuarioRepository;
import com.grupo5e.morapack.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @Override
    public Long insertar(Usuario usuario) {
        return usuarioRepository.save(usuario).getId();
    }

    @Override
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.getReferenceById(id);
    }

    @Override
    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }
}
