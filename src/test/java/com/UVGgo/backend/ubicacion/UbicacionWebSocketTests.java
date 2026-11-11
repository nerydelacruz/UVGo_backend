package com.UVGgo.backend.ubicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.UVGgo.backend.pedido.Pedido;
import com.UVGgo.backend.piloto.Piloto;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompSession;

class UbicacionWebSocketTests extends WebSocketPruebas {
    private UbicacionDtos.Posicion posicion() { return new UbicacionDtos.Posicion(14.6075,-90.488,45.0); }

    @Test void elPilotoAsignadoPublicaYElAdministradorRecibeLaPosicion() throws Exception {
        Piloto piloto=piloto();
        Pedido p=pedidoConfirmado(); p.asignarPiloto(piloto); pedidos.saveAndFlush(p);
        String token=loginAdmin();
        StompSession admin=conectarAdmin(token).sesion();
        var recibida=escuchar(admin,"/topic/pedidos/"+p.getId()+"/ubicacion",UbicacionDtos.Ubicacion.class);

        conectarPiloto(piloto).sesion().send("/app/pedidos/"+p.getId()+"/ubicacion",posicion());

        var u=recibida.get(5,TimeUnit.SECONDS);
        assertThat(u.latitud()).isEqualTo(14.6075);
        assertThat(u.longitud()).isEqualTo(-90.488);
        assertThat(u.rumbo()).isEqualTo(45.0);
        assertThat(u.marca()).isNotNull();
        mvc.perform(get("/api/v1/pedidos/"+p.getId()+"/ubicacion").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("latitud").value(14.6075));
    }
    @Test void unPilotoNoPuedePublicarEnUnPedidoQueNoTieneAsignado() throws Exception {
        Pedido p=pedidoConfirmado(); // sin piloto
        StompSession piloto=conectarPiloto(piloto()).sesion();
        var admin=conectarAdmin(loginAdmin()).sesion();
        var recibida=escuchar(admin,"/topic/pedidos/"+p.getId()+"/ubicacion",UbicacionDtos.Ubicacion.class);
        piloto.send("/app/pedidos/"+p.getId()+"/ubicacion",posicion());
        Thread.sleep(800);
        assertThat(recibida).isNotDone();
    }
    @Test void unAdministradorNoPuedePublicarPosiciones() throws Exception {
        Pedido p=pedidoConfirmado(); p.asignarPiloto(piloto()); pedidos.saveAndFlush(p);
        var admin=conectarAdmin(loginAdmin());
        admin.sesion().send("/app/pedidos/"+p.getId()+"/ubicacion",posicion());
        assertThat(admin.rechazo.get(5,TimeUnit.SECONDS)).isNotNull();
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
    @Test void sinCredencialesNoSeAbreLaConexion() throws Exception {
        var cliente=conectar(Map.of());
        assertThat(cliente.rechazo.get(5,TimeUnit.SECONDS)).isNotNull();
        assertThat(cliente.conectado).isNotDone();
    }
    @Test void conTokenInvalidoNoSeAbreLaConexion() throws Exception {
        var cliente=conectarAdmin("no.es.un.jwt");
        assertThat(cliente.rechazo.get(5,TimeUnit.SECONDS)).isNotNull();
        assertThat(cliente.conectado).isNotDone();
    }
    @Test void unPilotoNoPuedeEscucharLasAsignacionesDeOtro() throws Exception {
        var cliente=conectarPiloto(piloto());
        cliente.sesion().subscribe("/topic/pilotos/"+piloto().getId()+"/asignaciones",new org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter() {});
        assertThat(cliente.rechazo.get(5,TimeUnit.SECONDS)).isNotNull();
    }
    @Test void unEstudianteNoPuedePublicarPosiciones() throws Exception {
        Pedido p=pedidoConfirmado(); p.asignarPiloto(piloto()); pedidos.saveAndFlush(p);
        var estudiante=conectarConToken(loginEstudiante());
        estudiante.sesion().send("/app/pedidos/"+p.getId()+"/ubicacion",posicion());
        assertThat(estudiante.rechazo.get(5,TimeUnit.SECONDS)).isNotNull();
    }
    @Test void unEstudianteNoPuedeEscucharLaUbicacion() throws Exception {
        var cliente=conectarAdmin(loginEstudiante());
        cliente.sesion().subscribe("/topic/pedidos/"+UUID.randomUUID()+"/ubicacion",new org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter() {});
        assertThat(cliente.rechazo.get(5,TimeUnit.SECONDS)).isNotNull();
    }
    @Test void unPilotoNoPuedeEscucharLaUbicacion() throws Exception {
        var cliente=conectarPiloto(piloto());
        cliente.sesion().subscribe("/topic/pedidos/"+UUID.randomUUID()+"/ubicacion",new org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter() {});
        assertThat(cliente.rechazo.get(5,TimeUnit.SECONDS)).isNotNull();
    }
    @Test void unAdministradorNoPuedeSuscribirseADestinosAjenos() throws Exception {
        var cliente=conectarAdmin(loginAdmin());
        cliente.sesion().subscribe("/topic/otra-cosa",new org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter() {});
        assertThat(cliente.rechazo.get(5,TimeUnit.SECONDS)).isNotNull();
    }
}
