package com.UVGgo.backend.config;

import com.UVGgo.backend.usuario.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;

/** Cuentas ficticias creadas exclusivamente al habilitar explícitamente el perfil demo. */
@Component
@Profile("demo")
public class DatosDemo implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwords;
    private final String password;
    public DatosDemo(UsuarioRepository usuarios,PasswordEncoder passwords,@Value("${DEMO_PASSWORD}") String password) {
        this.usuarios=usuarios; this.passwords=passwords; this.password=password;
    }
    @Override @Transactional
    public void run(String... args) {
        if (password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("DEMO_PASSWORD debe tener al menos 12 caracteres y como máximo 72 bytes UTF-8");
        if (usuarios.findByCorreo("admin.demo@uvg.edu.gt").isEmpty())
            usuarios.save(new Administrador("Administrador ficticio","admin.demo@uvg.edu.gt","55550000",passwords.encode(password),"DEMO"));
        if (usuarios.findByCorreo("estudiante.demo@uvg.edu.gt").isEmpty())
            usuarios.save(new Estudiante("Estudiante ficticio","estudiante.demo@uvg.edu.gt","55550001",passwords.encode(password),"99999999","Carrera de prueba"));
    }
}
