package com.UVGgo.backend.ubicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.UVGgo.backend.usuario.*;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UbicacionWebSocketTests {
    private static final String PASSWORD="Prueba-local-123!";
    @LocalServerPort int puerto;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder passwords;

    private String correo() { return "ws."+UUID.randomUUID()+"@uvg.edu.gt"; }
    private String loginAdmin() throws Exception {
        String correo=correo();
        usuarios.saveAndFlush(new Administrador("Administrador ficticio",correo,"55550000",passwords.encode(PASSWORD),"PRUEBA"));
        return login(correo);
    }
    private String loginEstudiante() throws Exception {
        String correo=correo();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Estudiante ficticio\",\"correo\":\""+correo+"\",\"telefono\":\"55550000\",\"carne\":\""
                        +Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits())+"\",\"contrasena\":\""+PASSWORD+"\"}"))
                .andExpect(status().isCreated());
        return login(correo);
    }
    private String login(String correo) throws Exception {
        return json.readTree(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\""+correo+"\",\"contrasena\":\""+PASSWORD+"\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }

    /** Cliente STOMP; "rechazo" se completa con el ERROR del servidor o con el cierre de la conexión. */
    private static class Cliente extends StompSessionHandlerAdapter {
        final CompletableFuture<StompSession> conectado=new CompletableFuture<>();
        final CompletableFuture<String> rechazo=new CompletableFuture<>();
        @Override public void afterConnected(StompSession s,StompHeaders h) { conectado.complete(s); }
        @Override public void handleFrame(StompHeaders h,Object payload) { rechazo.complete(String.valueOf(h.getFirst("message"))); }
        @Override public void handleTransportError(StompSession s,Throwable e) { rechazo.complete("transporte: "+e.getMessage()); }
        @Override public void handleException(StompSession s,StompCommand c,StompHeaders h,byte[] p,Throwable e) { rechazo.complete(e.getMessage()); }
    }
    private Cliente conectar(String token) {
        var stomp=new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new JacksonJsonMessageConverter());
        var cliente=new Cliente();
        var headers=new StompHeaders();
        if (token!=null) headers.add("Authorization","Bearer "+token);
        stomp.connectAsync("ws://localhost:"+puerto+"/uvgo/api/ws",new WebSocketHttpHeaders(),headers,cliente);
        return cliente;
    }

    @Test void elAdministradorPublicaYOtroAdministradorRecibeLaPosicion() throws Exception {
        UUID pedido=UUID.randomUUID();
        String token=loginAdmin();
        var oyente=conectar(token);
        var publicador=conectar(token);
        StompSession escucha=oyente.conectado.get(5,TimeUnit.SECONDS);
        var recibida=new CompletableFuture<UbicacionDtos.Ubicacion>();
        escucha.subscribe("/topic/pedidos/"+pedido+"/ubicacion",new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders h) { return UbicacionDtos.Ubicacion.class; }
            @Override public void handleFrame(StompHeaders h,Object p) { recibida.complete((UbicacionDtos.Ubicacion) p); }
        });
        Thread.sleep(300); // la suscripción llega al broker de forma asíncrona
        publicador.conectado.get(5,TimeUnit.SECONDS)
                .send("/app/pedidos/"+pedido+"/ubicacion",new UbicacionDtos.Posicion(14.6075,-90.488,45.0));

        var u=recibida.get(5,TimeUnit.SECONDS);
        assertThat(u.latitud()).isEqualTo(14.6075);
        assertThat(u.longitud()).isEqualTo(-90.488);
        assertThat(u.rumbo()).isEqualTo(45.0);
        assertThat(u.marca()).isNotNull();

        mvc.perform(get("/api/v1/pedidos/"+pedido+"/ubicacion").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("latitud").value(14.6075));
    }
    @Test void sinPosicionPublicadaLaConsultaResponde204() throws Exception {
        mvc.perform(get("/api/v1/pedidos/"+UUID.randomUUID()+"/ubicacion").header("Authorization","Bearer "+loginAdmin()))
                .andExpect(status().isNoContent());
    }
    @Test void laConsultaRestSoloLaVeUnAdministrador() throws Exception {
        UUID pedido=UUID.randomUUID();
        mvc.perform(get("/api/v1/pedidos/"+pedido+"/ubicacion")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/pedidos/"+pedido+"/ubicacion").header("Authorization","Bearer "+loginEstudiante()))
                .andExpect(status().isForbidden());
    }
    @Test void sinTokenNoSeAbreLaConexion() throws Exception {
        var cliente=conectar(null);
        assertThat(cliente.rechazo.get(5,TimeUnit.SECONDS)).isNotNull();
        assertThat(cliente.conectado).isNotDone();
    }
    @Test void conTokenInvalidoNoSeAbreLaConexion() throws Exception {
        var cliente=conectar("no.es.un.jwt");
        assertThat(cliente.rechazo.get(5,TimeUnit.SECONDS)).isNotNull();
        assertThat(cliente.conectado).isNotDone();
    }
    @Test void unEstudianteNoPuedeEscucharLaUbicacion() throws Exception {
        var cliente=conectar(loginEstudiante());
        StompSession s=cliente.conectado.get(5,TimeUnit.SECONDS);
        s.subscribe("/topic/pedidos/"+UUID.randomUUID()+"/ubicacion",new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders h) { return String.class; }
            @Override public void handleFrame(StompHeaders h,Object p) { }
        });
        assertThat(cliente.rechazo.get(5,TimeUnit.SECONDS)).isNotNull();
    }
    @Test void unAdministradorNoPuedeSuscribirseADestinosAjenos() throws Exception {
        var cliente=conectar(loginAdmin());
        StompSession s=cliente.conectado.get(5,TimeUnit.SECONDS);
        s.subscribe("/topic/otra-cosa",new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders h) { return String.class; }
            @Override public void handleFrame(StompHeaders h,Object p) { }
        });
        assertThat(cliente.rechazo.get(5,TimeUnit.SECONDS)).isNotNull();
    }
}
