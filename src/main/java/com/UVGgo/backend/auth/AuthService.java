package com.UVGgo.backend.auth;

import com.UVGgo.backend.api.ApiException;
import com.UVGgo.backend.piloto.Piloto;
import com.UVGgo.backend.usuario.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class AuthService {
    private final UsuarioRepository usuarios;
    private final SesionRepository sesiones;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String dominio;
    private final long minutos;
    private final String dummyHash;
    public AuthService(UsuarioRepository usuarios, SesionRepository sesiones, PasswordEncoder passwords,
                       JwtEncoder encoder, @Value("${uvgo.security.institutional-domain}") String dominio,
                       @Value("${uvgo.security.token-minutes}") long minutos) {
        this.usuarios=usuarios; this.sesiones=sesiones; this.passwords=passwords; this.encoder=encoder;
        this.dominio=dominio.toLowerCase(Locale.ROOT); this.minutos=minutos;
        this.dummyHash=passwords.encode(UUID.randomUUID().toString());
    }
    public AuthDtos.Perfil registrar(AuthDtos.Registro r) {
        String correo=correoInstitucional(r.correo()); comprobarContrasena(r.contrasena());
        return AuthDtos.Perfil.de(usuarios.saveAndFlush(new Estudiante(r.nombre().strip(), correo,
                r.telefono(), passwords.encode(r.contrasena()), r.carne(), r.carrera())));
    }
    /** Un piloto se registra solo desde la app móvil. A diferencia del estudiante, puede usar cualquier correo. */
    public AuthDtos.Perfil registrarPiloto(AuthDtos.RegistroPiloto r) {
        String correo=normalizar(r.correo()); comprobarContrasena(r.contrasena());
        return AuthDtos.Perfil.de(usuarios.saveAndFlush(new Piloto(r.nombre().strip(), correo,
                r.telefono(), passwords.encode(r.contrasena()))));
    }
    private String correoInstitucional(String valor) {
        String correo=normalizar(valor);
        if (!correo.substring(correo.lastIndexOf('@')+1).equals(dominio))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Se requiere correo del dominio institucional configurado");
        return correo;
    }
    private static void comprobarContrasena(String contrasena) {
        if (contrasena.getBytes(StandardCharsets.UTF_8).length>72)
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña no puede superar 72 bytes UTF-8");
    }
    public AuthDtos.Token login(AuthDtos.Login r) {
        if (r.contrasena().getBytes(StandardCharsets.UTF_8).length>72)
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        Usuario u=usuarios.findByCorreo(normalizar(r.correo())).orElse(null);
        boolean coincide=passwords.matches(r.contrasena(), u==null ? dummyHash : u.getContrasenaHash());
        if (!coincide || u==null || !u.isActivo())
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        return emitir(u);
    }
    /**
     * Cambia un token vigente por uno nuevo con el plazo completo, sin pedir la contraseña. La app móvil lo llama
     * antes de que venza para que la sesión no se corte a media entrega. La sesión anterior no se revoca: vence sola
     * en minutos, y así no fallan las peticiones que iban en camino con el token viejo.
     */
    public AuthDtos.Token renovar(Jwt jwt) {
        Usuario u=usuario(UUID.fromString(jwt.getSubject()));
        if (!u.isActivo()) throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        return emitir(u);
    }
    private AuthDtos.Token emitir(Usuario u) {
        Instant ahora=Instant.now(), vence=ahora.plus(Duration.ofMinutes(minutos));
        UUID id=UUID.randomUUID();
        sesiones.save(new Sesion(id,u.getId(),vence));
        JwtClaimsSet claims=JwtClaimsSet.builder().issuer("uvgo").subject(u.getId().toString())
                .id(id.toString()).issuedAt(ahora).expiresAt(vence).claim("rol",u.obtenerRol()).build();
        String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new AuthDtos.Token(token,"Bearer",vence,AuthDtos.Perfil.de(u));
    }
    public void logout(Jwt jwt) { sesiones.findById(UUID.fromString(jwt.getId())).ifPresent(Sesion::revocar); }
    public Usuario usuario(UUID id) { return usuarios.findById(id).orElseThrow(ApiException::noEncontrado); }
    public AuthDtos.Perfil contacto(UUID id, AuthDtos.Contacto r) {
        Usuario u=usuario(id); u.actualizarContacto(r.telefono(), r.correoContacto()); return AuthDtos.Perfil.de(u);
    }
    private static String normalizar(String correo) { return correo.strip().toLowerCase(Locale.ROOT); }
}
