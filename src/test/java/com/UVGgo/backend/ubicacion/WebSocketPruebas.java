package com.UVGgo.backend.ubicacion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.UVGgo.backend.pedido.*;
import com.UVGgo.backend.usuario.*;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

/** Utilidades para probar el WebSocket y la asignación de pilotos de punta a punta (servidor real, cliente STOMP). */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="SIMULADOR_CLAVE=clave-de-prueba-0123456789")
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class WebSocketPruebas {
    static final String PASSWORD="Prueba-local-123!";
    static final String CLAVE="clave-de-prueba-0123456789";
    /** Piloto Oscar, sembrado por la migración V20. */
    static final UUID OSCAR=UUID.fromString("00000000-0000-4000-8000-000000000001");
    @LocalServerPort int puerto;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired PedidoRepository pedidos;
    @Autowired PasswordEncoder passwords;

    String correo() { return "ws."+UUID.randomUUID()+"@uvg.edu.gt"; }
    String loginAdmin() throws Exception {
        String correo=correo();
        usuarios.saveAndFlush(new Administrador("Administrador ficticio",correo,"55550000",passwords.encode(PASSWORD),"PRUEBA"));
        return login(correo);
    }
    String loginEstudiante() throws Exception { return login(estudiante().getCorreo()); }
    Estudiante estudiante() {
        return usuarios.saveAndFlush(new Estudiante("Estudiante ficticio",correo(),"55550000",passwords.encode(PASSWORD),
                Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits()),"Carrera de prueba"));
    }
    String login(String correo) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\""+correo+"\",\"contrasena\":\""+PASSWORD+"\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }
    /** Pedido ya confirmado (cotizado y aceptado), listo para asignarle piloto. */
    Pedido pedidoConfirmado() {
        Pedido p=pedidoSolicitado();
        Map<UUID,BigDecimal> precios=new HashMap<>();
        p.getItems().forEach(i -> precios.put(i.getId(),new BigDecimal("10.00")));
        p.cotizar(precios,new BigDecimal("5.00"),null);
        p.aceptar();
        return pedidos.saveAndFlush(p);
    }
    Pedido pedidoSolicitado() {
        var solicitud=new PedidoDtos.Solicitud("Curso de prueba","Docente ficticio",LocalDate.now().plusDays(10),null,"Edificio A",
                List.of(new PedidoDtos.Producto(null,null,"Resistencia 10K",null,null,2)));
        return pedidos.saveAndFlush(new Pedido(estudiante(),solicitud,Map.of()));
    }

    /** Cliente STOMP; "rechazo" se completa con el ERROR del servidor o con el cierre de la conexión. */
    static class Cliente extends StompSessionHandlerAdapter {
        final CompletableFuture<StompSession> conectado=new CompletableFuture<>();
        final CompletableFuture<String> rechazo=new CompletableFuture<>();
        @Override public void afterConnected(StompSession s,StompHeaders h) { conectado.complete(s); }
        @Override public void handleFrame(StompHeaders h,Object payload) { rechazo.complete(String.valueOf(h.getFirst("message"))); }
        @Override public void handleTransportError(StompSession s,Throwable e) { rechazo.complete("transporte: "+e.getMessage()); }
        @Override public void handleException(StompSession s,StompCommand c,StompHeaders h,byte[] p,Throwable e) { rechazo.complete(e.getMessage()); }
        StompSession sesion() throws Exception { return conectado.get(5,TimeUnit.SECONDS); }
    }
    Cliente conectar(Map<String,String> headers) {
        var stomp=new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new JacksonJsonMessageConverter());
        var cliente=new Cliente();
        var connect=new StompHeaders();
        headers.forEach(connect::add);
        stomp.connectAsync("ws://localhost:"+puerto+"/uvgo/api/ws",new WebSocketHttpHeaders(),connect,cliente);
        return cliente;
    }
    Cliente conectarAdmin(String token) { return conectar(Map.of("Authorization","Bearer "+token)); }
    Cliente conectarPiloto(UUID id,String clave) { return conectar(Map.of("X-Piloto-Id",id.toString(),"X-Piloto-Clave",clave)); }
    Cliente conectarPiloto() { return conectarPiloto(OSCAR,CLAVE); }

    /** Se suscribe y entrega los mensajes en un futuro (el primero que llegue). */
    <T> CompletableFuture<T> escuchar(StompSession s,String destino,Class<T> tipo) throws Exception {
        var futuro=new CompletableFuture<T>();
        s.subscribe(destino,new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders h) { return tipo; }
            @SuppressWarnings("unchecked") @Override public void handleFrame(StompHeaders h,Object p) { futuro.complete((T) p); }
        });
        Thread.sleep(300); // la suscripción llega al broker de forma asíncrona
        return futuro;
    }
}
