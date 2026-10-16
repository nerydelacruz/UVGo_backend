package com.UVGgo.backend.correo;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.assertj.core.api.Assertions.assertThatCode;

class CorreoServiceTests {
    private final Correo correo=new Correo("prueba@uvg.edu.gt","Asunto","Texto","<p>Texto</p>");

    @Test void sinConfiguracionNoEnviaNiFalla() {
        assertThatCode(() -> new CorreoService("","").enviar(correo)).doesNotThrowAnyException();
    }
    // Construye el cliente real de Azure (sin red): detecta conflictos de dependencias del SDK.
    @Test void construyeClienteDeAzure() {
        String clave=Base64.getEncoder().encodeToString("clave-ficticia-de-prueba".getBytes());
        assertThatCode(() -> new CorreoService("endpoint=https://uvgo-prueba.invalid/;accesskey="+clave,
                "DoNotReply@uvgo-prueba.invalid")).doesNotThrowAnyException();
    }
}
