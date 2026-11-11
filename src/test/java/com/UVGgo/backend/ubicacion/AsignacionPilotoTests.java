package com.UVGgo.backend.ubicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.UVGgo.backend.pedido.EstadoPedido;
import com.UVGgo.backend.pedido.Pedido;
import com.UVGgo.backend.piloto.Piloto;
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

    @Test void laListaDePilotosDisponiblesIncluyeLasCuentasDePiloto() throws Exception {
        Piloto piloto=piloto("Piloto "+UUID.randomUUID());
        mvc.perform(get("/api/v1/pilotos").header("Authorization","Bearer "+loginAdmin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.nombre=='"+piloto.getNombre()+"')].id").value(piloto.getId().toString()));
    }
    @Test void unAdministradorCreaLaCuentaYElPilotoIniciaSesionComoCualquierUsuario() throws Exception {
        String correo="piloto."+UUID.randomUUID()+"@uvg.edu.gt";
        String alta="{\"nombre\":\"Piloto nuevo\",\"correo\":\""+correo+"\",\"telefono\":\"5555 0000\",\"contrasena\":\""+PASSWORD+"\"}";
        mvc.perform(post("/api/v1/pilotos").header("Authorization","Bearer "+loginAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(alta))
                .andExpect(status().isCreated()).andExpect(jsonPath("nombre").value("Piloto nuevo")).andExpect(jsonPath("id").exists());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\""+correo+"\",\"contrasena\":\""+PASSWORD+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("usuario.rol").value("PILOTO"))
                .andExpect(jsonPath("usuario.carne").doesNotExist());
        // El mismo correo no se puede registrar dos veces
        mvc.perform(post("/api/v1/pilotos").header("Authorization","Bearer "+loginAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(alta))
                .andExpect(status().isConflict());
    }
    @Test void unPilotoSeRegistraSoloYQuedaDisponibleParaAsignar() throws Exception {
        // Un piloto no necesita correo del dominio institucional
        String correo="piloto."+UUID.randomUUID()+"@example.com", nombre="Piloto "+UUID.randomUUID();
        String registro="{\"nombre\":\""+nombre+"\",\"correo\":\""+correo+"\",\"telefono\":\"5555 0000\",\"contrasena\":\""+PASSWORD+"\"}";
        mvc.perform(post("/api/v1/auth/registro-piloto").contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isCreated()).andExpect(jsonPath("rol").value("PILOTO")).andExpect(jsonPath("accessToken").doesNotExist());
        mvc.perform(get("/api/v1/pilotos").header("Authorization","Bearer "+loginAdmin()))
                .andExpect(jsonPath("$[?(@.nombre=='"+nombre+"')]").exists());
        assertThat(login(correo)).isNotBlank();
        // Mismo correo otra vez → 409
        mvc.perform(post("/api/v1/auth/registro-piloto").contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isConflict());
    }
    @Test void soloUnAdministradorCreaPilotos() throws Exception {
        String alta="{\"nombre\":\"Piloto nuevo\",\"correo\":\"piloto."+UUID.randomUUID()+"@uvg.edu.gt\",\"telefono\":\"5555 0000\",\"contrasena\":\""+PASSWORD+"\"}";
        mvc.perform(post("/api/v1/pilotos").contentType(MediaType.APPLICATION_JSON).content(alta)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/pilotos").header("Authorization","Bearer "+loginEstudiante())
                .contentType(MediaType.APPLICATION_JSON).content(alta)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pilotos").header("Authorization","Bearer "+login(piloto().getCorreo()))
                .contentType(MediaType.APPLICATION_JSON).content(alta)).andExpect(status().isForbidden());
    }
    @Test void unPilotoNoVeLosPedidosDeLosEstudiantesPorRest() throws Exception {
        Pedido p=pedidoConfirmado(); String piloto=login(piloto().getCorreo());
        mvc.perform(get("/api/v1/pedidos/"+p.getId()).header("Authorization","Bearer "+piloto)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/pedidos").header("Authorization","Bearer "+piloto))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElementos").value(0));
    }
    @Test void soloUnAdministradorVeLosPilotosYAsigna() throws Exception {
        Pedido p=pedidoConfirmado(); String estudiante=loginEstudiante(); UUID piloto=piloto().getId();
        mvc.perform(get("/api/v1/pilotos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/pilotos").header("Authorization","Bearer "+estudiante)).andExpect(status().isForbidden());
        asignar(estudiante,p,p.getVersion(),piloto).andExpect(status().isForbidden());
    }
    @Test void asignarGuardaElPilotoYSeVeEnElDetalle() throws Exception {
        Pedido p=pedidoConfirmado(); String admin=loginAdmin(); Piloto piloto=piloto("Piloto Oscar");
        mvc.perform(get("/api/v1/pedidos/"+p.getId()).header("Authorization","Bearer "+admin)).andExpect(jsonPath("piloto").doesNotExist());
        asignar(admin,p,p.getVersion(),piloto.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("piloto.id").value(piloto.getId().toString())).andExpect(jsonPath("piloto.nombre").value("Piloto Oscar"));
        mvc.perform(get("/api/v1/pedidos/"+p.getId()).header("Authorization","Bearer "+admin))
                .andExpect(jsonPath("piloto.nombre").value("Piloto Oscar"));
    }
    @Test void elPilotoRecibeLaAsignacionEnTiempoReal() throws Exception {
        Pedido p=pedidoConfirmado(); String admin=loginAdmin(); Piloto piloto=piloto();
        var recibida=escuchar(conectarPiloto(piloto).sesion(),"/topic/pilotos/"+piloto.getId()+"/asignaciones",PilotoDtos.Asignacion.class);

        asignar(admin,p,p.getVersion(),piloto.getId()).andExpect(status().isOk());

        var a=recibida.get(5,TimeUnit.SECONDS);
        assertThat(a.pedidoId()).isEqualTo(p.getId());
        assertThat(a.asignado()).isTrue();
        assertThat(a.curso()).isEqualTo("Curso de prueba");
        assertThat(a.destino()).isEqualTo("Edificio A");
        assertThat(a.estado()).isEqualTo(EstadoPedido.CONFIRMADO);
    }
    @Test void alConectarseElPilotoRecibeSusPedidosVigentes() throws Exception {
        Pedido mio=pedidoConfirmado(), ajeno=pedidoConfirmado(), entregado=pedidoConfirmado();
        String admin=loginAdmin(); Piloto piloto=piloto();
        asignar(admin,mio,mio.getVersion(),piloto.getId()).andExpect(status().isOk());
        asignar(admin,entregado,entregado.getVersion(),piloto.getId()).andExpect(status().isOk());
        var e=pedidos.findById(entregado.getId()).orElseThrow();
        org.springframework.test.util.ReflectionTestUtils.setField(e,"estado",EstadoPedido.ENTREGADO); pedidos.saveAndFlush(e);

        var lista=escuchar(conectarPiloto(piloto).sesion(),"/app/pilotos/"+piloto.getId()+"/pedidos",PilotoDtos.Asignacion[].class).get(5,TimeUnit.SECONDS);
        assertThat(lista).extracting(PilotoDtos.Asignacion::pedidoId).contains(mio.getId()).doesNotContain(ajeno.getId(),entregado.getId());
    }
    @Test void quitarOReasignarAvisaAlPilotoAnterior() throws Exception {
        Pedido p=pedidoConfirmado(); String admin=loginAdmin(); Piloto cuenta=piloto();
        asignar(admin,p,p.getVersion(),cuenta.getId()).andExpect(status().isOk());
        var piloto=conectarPiloto(cuenta).sesion();
        var aviso=new java.util.concurrent.CompletableFuture<PilotoDtos.Asignacion>();
        piloto.subscribe("/topic/pilotos/"+cuenta.getId()+"/asignaciones",new org.springframework.messaging.simp.stomp.StompFrameHandler() {
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
        asignar(loginAdmin(),sinConfirmar,sinConfirmar.getVersion(),piloto().getId()).andExpect(status().isConflict());
    }
    @Test void rechazaVersionVencidaYPilotoInexistente() throws Exception {
        Pedido p=pedidoConfirmado(); String admin=loginAdmin();
        asignar(admin,p,p.getVersion()+5,piloto().getId()).andExpect(status().isConflict());
        asignar(admin,p,p.getVersion(),UUID.randomUUID()).andExpect(status().isNotFound());
        // Un usuario que no es piloto (aquí, un estudiante) tampoco se puede asignar
        asignar(admin,p,p.getVersion(),estudiante().getId()).andExpect(status().isNotFound());
    }
}
