package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.enums.Rol;
import com.grupo5e.morapack.core.model.Usuario;
import com.grupo5e.morapack.repository.UsuarioRepository;
import com.grupo5e.morapack.service.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @Override
    @Transactional
    public Long insertar(Usuario usuario) {
        return usuarioRepository.save(usuario).getId();
    }

    @Override
    @Transactional
    public Usuario actualizar(Long id, Usuario usuario) {
        Usuario existente = buscarPorId(id);
        if (existente == null) {
            throw new ResourceNotFoundException("Usuario", "id", id);
        }
        usuario.setId(id);
        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsernameOrEmail(username);
    }

    @Override
    public List<Usuario> buscarPorRol(Rol rol) {
        return usuarioRepository.findByRol(rol);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!existePorId(id)) {
            throw new ResourceNotFoundException("Usuario", "id", id);
        }
        usuarioRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(Long id) {
        return usuarioRepository.existsById(id);
    }
}
