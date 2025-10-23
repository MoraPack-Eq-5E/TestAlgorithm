// AuthController.java
package com.grupo5e.morapack.api.auth;

import com.grupo5e.morapack.api.auth.dto.AuthRequest;
import com.grupo5e.morapack.api.auth.dto.AuthResponse;
import com.grupo5e.morapack.core.model.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwt;

    public AuthController(AuthService authService, JwtUtil jwt) {
        this.authService = authService;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        Usuario user = authService.validateCredentials(req.email(), req.password());
        if (user == null) {
            return ResponseEntity.status(401).body("{\"message\":\"Credenciales inválidas\"}");
        }
        String token = jwt.generateToken(user.getUsernameOrEmail());
        // Opcional: refreshToken real; por ahora un placeholder
        String refresh = jwt.generateToken(user.getUsernameOrEmail());

        return ResponseEntity.ok(
                new AuthResponse(
                        user.getId(),
                        user.getUsernameOrEmail(),
                        "Usuario",               // cámbialo cuando tengas nombre real
                        user.getRol(),
                        token,
                        refresh
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        // El “subject” del token es el email
        String email = auth.getName();
        return ResponseEntity.ok(new Object() {
            public final String email_ = email;
        });
    }
}
