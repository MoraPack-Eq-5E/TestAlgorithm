// AuthServiceImpl.java
package com.grupo5e.morapack.api.auth;

import com.grupo5e.morapack.core.model.Usuario;
import com.grupo5e.morapack.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder encoder;

    public AuthServiceImpl(UsuarioRepository usuarioRepository, BCryptPasswordEncoder encoder) {
        this.usuarioRepository = usuarioRepository;
        this.encoder = encoder;
    }

    @Override
    public Usuario validateCredentials(String email, String rawPassword) {
        var userOpt = usuarioRepository.findByUsernameOrEmail(email);
        if (userOpt.isEmpty()) return null;

        var user = userOpt.get();
        if (!user.isActivo()) return null;

        return encoder.matches(rawPassword, user.getPassword()) ? user : null;
    }
}
