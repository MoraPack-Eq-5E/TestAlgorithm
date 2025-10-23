// AuthService.java
package com.grupo5e.morapack.api.auth;

import com.grupo5e.morapack.core.model.Usuario;

public interface AuthService {
    Usuario validateCredentials(String email, String rawPassword);
}
