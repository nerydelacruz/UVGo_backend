package com.UVGgo.backend.ubicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.UVGgo.backend.pedido.EstadoPedido;
import com.UVGgo.backend.pedido.Pedido;
import com.UVGgo.backend.piloto.PilotoDtos;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AsignacionPilotoTests extends WebSocketPruebas {
    private ResultActions asignar(String token,Pedido p,long version,UUID piloto) throws Exception {
        return mvc.perform(post("/api/v1/pedidos/"+p.getId()+"/piloto").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":"+version+",\"pilotoId\":\""+piloto+"\"}"));
    }

    @Test void laListaDePilotosDisponiblesIncluyeAPilotoOscar() throws Exception {
        mvc.perform(get("/api/v1/pilotos").header("Authorization","Bearer "+loginAdmin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.nombre=='Piloto Oscar')].id").value(OSCAR.toString()));
    }
    @Test void soloUnAdministradorVeLosPilotosYAsigna() throws Exception {
        Pedido p=pedidoConfirmado(); String estudiante=loginEstudiante();
        mvc.perform(get("/api/v1/pilotos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/pilotos").header("Authorization","Bearer "+estudiante)).andExpect(status().isForbidden());
        asignar(estudiante,p,p.getVersion(),OSCAR).andExpect(status().isForbidden());
    }
    @Test void asignarGuardaElPilotoYSeVeEnElDetalle() throws Exception {
        Pedido p=pedidoConfirmado(); String admin=loginAdmin();
        mvc.perform(get("/api/v1/pedidos/"+p.getId()).header("Authorization","Bearer "+admin)).andExpect(jsonPath("piloto").doesNotExist());
        asignar(admin,p,p.getVersion(),OSCAR).andExpect(status().isOk())
                .andExpect(jsonPath("piloto.id").value(OSCAR.toString())).andExpect(jsonPath("piloto.nombre").value("Piloto Oscar"));
        mvc.perform(get("/api/v1/pedidos/"+p.getId()).header("Authorization","Bearer "+admin))
                .andExpect(jsonPath("piloto.nombre").value("Piloto Oscar"));
    }
    @Test void elPilotoRecibeLaAsignacionEnTiempoReal() throws Exception {
        Pedido p=pedidoConfirmado(); String admin=loginAdmin();
        var recibida=escuchar(conectarPiloto().sesion(),"/topic/pilotos/"+OSCAR+"/asignaciones",PilotoDtos.Asignacion.class);

        asignar(admin,p,p.getVersion(),OSCAR).andExpect(status().isOk());

        var a=recibida.get(5,TimeUnit.SECONDS);
        assertThat(a.pedidoId()).isEqualTo(p.getId());
        assertThat(a.asignado()).isTrue();
        assertThat(a.curso()).isEqualTo("Curso de prueba");
        assertThat(a.destino()).isEqualTo("Edificio A");
        assertThat(a.estado()).isEqualTo(EstadoPedido.CONFIRMADO);
    }
    @Test void alConectarseElPilotoRecibeSusPedidosVigentes() throws Exception {
        Pedido mio=pedidoConfirmado(), ajeno=pedidoConfirmado(), entregado=pedidoConfirmado();
        String admin=loginAdmin();
        asignar(admin,mio,mio.getVersion(),OSCAR).andExpect(status().isOk());
        asignar(admin,entregado,entregado.getVersion(),OSCAR).andExpect(status().isOk());
        var e=pedidos.findById(entregado.getId()).orElseThrow();
        org.springframework.test.util.ReflectionTestUtils.setField(e,"estado",EstadoPedido.ENTREGADO); pedidos.saveAndFlush(e);

        var lista=escuchar(conectarPiloto().sesion(),"/app/pilotos/"+OSCAR+"/pedidos",PilotoDtos.Asignacion[].class).get(5,TimeUnit.SECONDS);
        assertThat(lista).extracting(PilotoDtos.Asignacion::pedidoId).contains(mio.getId()).doesNotContain(ajeno.getId(),entregado.getId());
    }
    @Test void quitarOReasignarAvisaAlPilotoAnterior() throws Exception {
        Pedido p=pedidoConfirmado(); String admin=loginAdmin();
        asignar(admin,p,p.getVersion(),OSCAR).andExpect(status().isOk());
        var piloto=conectarPiloto().sesion();
        var aviso=new java.util.concurrent.CompletableFuture<PilotoDtos.Asignacion>();
        piloto.subscribe("/topic/pilotos/"+OSCAR+"/asignaciones",new org.springframework.messaging.simp.stomp.StompFrameHandler() {
            @Override public java.lang.reflect.Type getPayloadType(org.springframework.messaging.simp.stomp.StompHeaders h) { return PilotoDtos.Asignacion.class; }
            @Override public void handleFrame(org.springframework.messaging.simp.stomp.StompHeaders h,Object x) { aviso.complete((PilotoDtos.Asignacion) x); }
        });
        Thread.sleep(300);
        long version=pedidos.findById(p.getId()).orElseThrow().getVersion();
        mvc.perform(delete("/api/v1/pedidos/"+p.getId()+"/piloto?version="+version).header("Authorization","Bearer "+admin))
                .andExpect(status().isOk()).andExpect(jsonPath("piloto").doesNotExist());
        var a=aviso.get(5,TimeUnit.SECONDS);
        assertThat(a.pedidoId()).isEqualTo(p.getId());
        assertThat(a.asignado()).isFalse();
    }
    @Test void soloSeAsignaAPedidosConfirmadosQueAunNoSeEntregan() throws Exception {
        Pedido sinConfirmar=pedidoSolicitado();
        asignar(loginAdmin(),sinConfirmar,sinConfirmar.getVersion(),OSCAR).andExpect(status().isConflict());
    }
    @Test void rechazaVersionVencidaYPilotoInexistente() throws Exception {
        Pedido p=pedidoConfirmado(); String admin=loginAdmin();
        asignar(admin,p,p.getVersion()+5,OSCAR).andExpect(status().isConflict());
        asignar(admin,p,p.getVersion(),UUID.randomUUID()).andExpect(status().isNotFound());
    }
}
