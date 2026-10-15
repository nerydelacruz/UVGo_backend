package com.UVGgo.backend.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth=auth; }
    @PostMapping("/auth/registro") @ResponseStatus(HttpStatus.CREATED)
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    public AuthDtos.Perfil registro(@Valid @RequestBody AuthDtos.Registro r) { return auth.registrar(r); }
    @PostMapping("/auth/login")
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    public AuthDtos.Token login(@Valid @RequestBody AuthDtos.Login r) { return auth.login(r); }
    @PostMapping("/auth/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt) { auth.logout(jwt); }
    @GetMapping("/usuarios/me")
    public AuthDtos.Perfil perfil(@AuthenticationPrincipal Jwt jwt) { return AuthDtos.Perfil.de(auth.usuario(UUID.fromString(jwt.getSubject()))); }
    @PatchMapping("/usuarios/me")
    public AuthDtos.Perfil contacto(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AuthDtos.Contacto r) {
        return auth.contacto(UUID.fromString(jwt.getSubject()),r);
    }
}
