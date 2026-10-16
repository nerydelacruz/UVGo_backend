package com.UVGgo.backend.correo;

import com.azure.communication.email.*;
import com.azure.communication.email.models.*;
import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.core.util.polling.LongRunningOperationStatus;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import java.time.Duration;

@Service
public class CorreoService {
    private static final Logger log=LoggerFactory.getLogger(CorreoService.class);
    private final EmailClient cliente;
    private final String remitente;
    public CorreoService(@Value("${uvgo.correo.connection-string:${AZURE_EMAIL_CONNECTION_STRING:}}") String conexion,
                         @Value("${uvgo.correo.remitente:${AZURE_EMAIL_SENDER:}}") String remitente) {
        this.remitente=remitente;
        this.cliente=conexion.isBlank() || remitente.isBlank() ? null : new EmailClientBuilder()
                .connectionString(conexion).httpClient(new JdkHttpClientBuilder().build()).buildClient();
        if (cliente==null) log.warn("Correo deshabilitado: faltan AZURE_EMAIL_CONNECTION_STRING o AZURE_EMAIL_SENDER");
    }
    // Asíncrono y después del commit: un fallo de Azure no deshace ni demora la operación que lo originó.
    @Async @TransactionalEventListener(fallbackExecution=true)
    public void enviar(Correo c) {
        if (cliente==null) { log.info("Correo no enviado (deshabilitado): '{}' a {}",c.asunto(),c.para()); return; }
        try {
            EmailMessage mensaje=new EmailMessage().setSenderAddress(remitente).setToRecipients(c.para())
                    .setSubject(c.asunto()).setBodyPlainText(c.texto()).setBodyHtml(c.html());
            var resultado=cliente.beginSend(mensaje).waitForCompletion(Duration.ofMinutes(2));
            if (resultado.getStatus()!=LongRunningOperationStatus.SUCCESSFULLY_COMPLETED)
                log.error("Azure no completó el correo '{}' a {}: {}",c.asunto(),c.para(),resultado.getStatus());
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el correo '{}' a {}",c.asunto(),c.para(),e);
        }
    }
}
