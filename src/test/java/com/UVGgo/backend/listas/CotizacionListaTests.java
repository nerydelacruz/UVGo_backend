package com.UVGgo.backend.listas;

import com.UVGgo.backend.api.ErroresApi;
import com.UVGgo.backend.archivo.AlmacenArchivos;
import com.UVGgo.backend.config.SeguridadConfig;
import com.UVGgo.backend.correo.Correo;
import com.UVGgo.backend.domain.repository.ArticuloRepository;
import com.UVGgo.backend.pedido.*;
import com.UVGgo.backend.usuario.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.*;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.*;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=CotizacionListaTests.Config.class, properties={
    "spring.profiles.active=test", "spring.config.import=",
    "spring.datasource.url=jdbc:h2:mem:cotizacion-lista;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
    "uvgo.security.jwt-secret=dXZnby10ZXN0LW9ubHktMzItYnl0ZS1rZXktbm90LXByb2R1Y3Rpb24="
})
@AutoConfigureMockMvc @RecordApplicationEvents
class CotizacionListaTests {
    @Configuration @EnableAutoConfiguration
    @EntityScan(basePackages={"com.UVGgo.backend.listas","com.UVGgo.backend.usuario","com.UVGgo.backend.auth","com.UVGgo.backend.pedido"})
    @EnableJpaRepositories(basePackages={"com.UVGgo.backend.listas","com.UVGgo.backend.usuario","com.UVGgo.backend.auth","com.UVGgo.backend.pedido"})
    @Import({ListaMaterialesController.class,CotizacionListaController.class,CotizacionListaService.class,
            PedidoService.class,PedidoController.class,SeguridadConfig.class,ErroresApi.class})
    static class Config {}
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PedidoRepository pedidos;
    @Autowired ObjectMapper json;
    @Autowired ApplicationEvents eventos;
    @MockitoBean AlmacenArchivos almacen;
    @MockitoBean ArticuloRepository articulos;

    private UUID usuario() {
        String unico=UUID.randomUUID().toString();
        var estudiante=new Estudiante("Prueba",unico+"@uvg.edu.gt","55550000","hash",unico.substring(0,20),"Ingeniería");
        estudiante.actualizarContacto("55550000","contacto-"+unico+"@example.com");
        return usuarios.save(estudiante).getId();
    }
    private org.springframework.test.web.servlet.request.RequestPostProcessor sesion(UUID id, String rol) {
        return jwt().jwt(j -> j.subject(id.toString()).claim("rol",rol)).authorities(new SimpleGrantedAuthority("ROLE_"+rol));
    }
    private String lista(UUID usuario) throws Exception {
        var respuesta=mvc.perform(post("/api/v1/listas-materiales").with(sesion(usuario,"ESTUDIANTE"))
            .contentType(MediaType.APPLICATION_JSON).content("""
            {"nombre":"Práctica","revisada":true,"materiales":[{"nombre":"Alcohol","descripcion":"Solución al 70%",
             "cantidad":2.125,"unidad":"litro","observaciones":"Envase sellado","referencia":"2.125 litros de alcohol"}]}
            """)).andExpect(status().isCreated()).andReturn();
        return json.readTree(respuesta.getResponse().getContentAsString()).path("id").asText();
    }
    private String datos() {
        return """
            {"curso":"Química","nombreCatedratico":"Docente","fechaLimite":"%s","observaciones":"Lista revisada"}
            """.formatted(LocalDate.now().plusDays(10));
    }
    private JsonNode solicitar(String lista, UUID usuario) throws Exception {
        var respuesta=mvc.perform(post("/api/v1/listas-materiales/"+lista+"/solicitud").with(sesion(usuario,"ESTUDIANTE"))
            .contentType(MediaType.APPLICATION_JSON).content(datos())).andExpect(status().isOk()).andReturn();
        return json.readTree(respuesta.getResponse().getContentAsString());
    }
    @Test void conservaMaterialesYReintentaSinDuplicados() throws Exception {
        UUID usuario=usuario(); String lista=lista(usuario); long antes=pedidos.count();
        JsonNode pedido=solicitar(lista,usuario), repetido=solicitar(lista,usuario);
        assertThat(repetido.path("id").asText()).isEqualTo(pedido.path("id").asText());
        assertThat(pedidos.count()).isEqualTo(antes+1);
        var item=pedido.path("productos").get(0);
        assertThat(item.path("tipo").asText()).isEqualTo("LISTA");
        assertThat(item.path("cantidad").decimalValue()).isEqualByComparingTo("2.125");
        assertThat(item.path("unidad").asText()).isEqualTo("litro");
        assertThat(item.path("nombreMaterial").asText()).isEqualTo("Alcohol");
        assertThat(item.path("observacionesMaterial").asText()).isEqualTo("Envase sellado");
        assertThat(item.path("referenciaMaterial").asText()).isEqualTo("2.125 litros de alcohol");
        assertThat(item.path("precioUnitario").isNull()).isTrue();
        mvc.perform(get("/api/v1/listas-materiales/"+lista).with(sesion(usuario,"ESTUDIANTE")))
            .andExpect(jsonPath("$.solicitudId").value(pedido.path("id").asText()));
        assertThat(eventos.stream(Correo.class).count()).isEqualTo(1);
    }
    @Test void adminCotizaYElAvisoSeDirigeAlCorreoDelSolicitante() throws Exception {
        UUID usuario=usuario(); var pedido=solicitar(lista(usuario),usuario);
        String id=pedido.path("id").asText(), item=pedido.path("productos").get(0).path("id").asText();
        String cotizacion="""
            {"version":0,"precios":[{"itemId":"%s","precioUnitario":10.00}],"cargoServicio":5.00,"notas":"Disponible"}
            """.formatted(item);
        mvc.perform(post("/api/v1/pedidos/"+id+"/cotizacion").with(sesion(usuario,"ESTUDIANTE"))
            .contentType(MediaType.APPLICATION_JSON).content(cotizacion)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pedidos/"+id+"/cotizacion").with(sesion(UUID.randomUUID(),"ADMINISTRADOR"))
            .contentType(MediaType.APPLICATION_JSON).content(cotizacion)).andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("COTIZADO")).andExpect(jsonPath("$.total").value(26.25));
        mvc.perform(get("/api/v1/pedidos/"+id).with(sesion(usuario,"ESTUDIANTE")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.productos[0].subtotal").value(21.25));
        var aviso=eventos.stream(Correo.class).filter(c -> c.asunto().contains("está lista")).findFirst().orElseThrow();
        assertThat(aviso.para()).isEqualTo(usuarios.findById(usuario).orElseThrow().getCorreoContacto());
        assertThat(aviso.texto()).contains(id,"26.25");
    }
    @Test void rechazaAnonimosOtrosPropietariosYAdministradores() throws Exception {
        UUID usuario=usuario(); String ruta="/api/v1/listas-materiales/"+lista(usuario)+"/solicitud";
        mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(datos())).andExpect(status().isUnauthorized());
        mvc.perform(post(ruta).with(sesion(usuario(),"ESTUDIANTE")).contentType(MediaType.APPLICATION_JSON).content(datos()))
            .andExpect(status().isNotFound());
        mvc.perform(post(ruta).with(sesion(usuario,"ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content(datos()))
            .andExpect(status().isForbidden());
    }
    @Test void validaDatosSinCrearSolicitud() throws Exception {
        UUID usuario=usuario(); String ruta="/api/v1/listas-materiales/"+lista(usuario)+"/solicitud";
        long antes=pedidos.count();
        for (String cuerpo:java.util.List.of(datos().replace("Química"," "),datos().replace("Docente"," "),
                datos().replace(LocalDate.now().plusDays(10).toString(),"2000-01-01"))) {
            mvc.perform(post(ruta).with(sesion(usuario,"ESTUDIANTE")).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
        }
        assertThat(pedidos.count()).isEqualTo(antes);
    }
    @Test void dosEnviosSimultaneosCreanUnaSolaSolicitud() throws Exception {
        UUID usuario=usuario(); String lista=lista(usuario); long antes=pedidos.count();
        var inicio=new java.util.concurrent.CountDownLatch(1);
        try (var ejecutor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<String> enviar=() -> {
                inicio.await(); return solicitar(lista,usuario).path("id").asText();
            };
            var primero=ejecutor.submit(enviar); var segundo=ejecutor.submit(enviar); inicio.countDown();
            assertThat(primero.get(15,java.util.concurrent.TimeUnit.SECONDS))
                .isEqualTo(segundo.get(15,java.util.concurrent.TimeUnit.SECONDS));
        }
        assertThat(pedidos.count()).isEqualTo(antes+1);
    }
}
