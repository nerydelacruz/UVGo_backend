package com.UVGgo.backend.piloto;

import com.UVGgo.backend.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Alta de cuentas de piloto. Solo la usa un administrador; el piloto luego entra por /auth/login. */
@Service
@Transactional
public class PilotoService {
    private final PilotoRepository pilotos;
    private final PasswordEncoder passwords;
    public PilotoService(PilotoRepository pilotos,PasswordEncoder passwords) { this.pilotos=pilotos; this.passwords=passwords; }

    /** Un correo ya registrado (de cualquier tipo de usuario) termina en 409 por la restricción de unicidad. */
    public PilotoDtos.Resumen registrar(PilotoDtos.Alta r) {
        if (r.contrasena().getBytes(StandardCharsets.UTF_8).length>72)
            throw new ApiException(HttpStatus.BAD_REQUEST,"La contraseña no puede superar 72 bytes UTF-8");
        String correo=r.correo().strip().toLowerCase(Locale.ROOT);
        return PilotoDtos.Resumen.de(pilotos.saveAndFlush(
                new Piloto(r.nombre().strip(),correo,r.telefono(),passwords.encode(r.contrasena()))));
    }
}
