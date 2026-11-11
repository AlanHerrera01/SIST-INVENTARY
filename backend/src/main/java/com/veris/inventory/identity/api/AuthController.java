package com.veris.inventory.identity.api;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.veris.inventory.identity.application.LocalProfiles;
import com.veris.inventory.identity.application.PasswordLoginService;
import com.veris.inventory.identity.infrastructure.SessionGuard;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final PasswordLoginService passwordLogin;

    public AuthController(PasswordLoginService passwordLogin) {
        this.passwordLogin = passwordLogin;
    }

    public record LoginRequest(String username, String password) {}

    @PostMapping("/api/auth/login")
    public Map<String, Boolean> login(@RequestBody LoginRequest request,
                                      HttpServletRequest servletRequest,
                                      HttpServletResponse servletResponse) {
        if (request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isEmpty()) {
            throw com.veris.inventory.common.error.ApiException.badRequest(
                    "VALIDATION_FAILED", "Usuario y contraseña son obligatorios.");
        }
        passwordLogin.login(request.username().trim(), request.password(), servletRequest, servletResponse);
        return Map.of("ok", true);
    }

    @GetMapping("/api/auth/csrf")
    public Map<String,String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }
    @GetMapping({"/api/auth/me", "/api/v1/me"})
    public LocalProfiles.Profile me(HttpServletRequest request) {
        return (LocalProfiles.Profile) request.getAttribute(SessionGuard.PROFILE);
    }
}
