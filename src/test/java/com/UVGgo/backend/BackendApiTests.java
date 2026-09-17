package com.UVGgo.backend;

import com.UVGgo.backend.usuario.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder passwords;
    @Autowired org.springframework.core.env.Environment environment;
    private static final String PASSWORD="Prueba-local-123!";

    private String registro(String correo,String carne) {
        return """
                {"nombre":"Estudiante ficticio","correo":"%s","telefono":"55550000","carne":"%s","contrasena":"%s"}
                """.formatted(correo,carne,PASSWORD);
    }
    private String carne() { return Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits()); }
    private String correo() { return "prueba."+UUID.randomUUID()+"@uvg.edu.gt"; }
    private String cuenta() throws Exception {
        String correo=correo();
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro(correo,carne())))
                .andExpect(status().isCreated()).andExpect(jsonPath("rol").value("ESTUDIANTE"))
                .andExpect(jsonPath("contrasenaHash").doesNotExist());
        return login(correo);
    }
    private String login(String correo) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\""+correo+"\",\"contrasena\":\""+PASSWORD+"\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }
    private String solicitud() {
        return """
                {"curso":"Química","nombreCatedratico":"Docente ficticio","fechaLimite":"%s",
                 "materiales":[{"descripcion":"Alcohol","cantidad":1.250,"unidadMedida":"litro"}]}
                """.formatted(LocalDate.now().plusDays(10));
    }
    private JsonNode pedido(String token) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/pedidos").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(solicitud())).andExpect(status().isCreated())
                .andExpect(jsonPath("estado").value("SOLICITADO")).andReturn().getResponse().getContentAsString());
    }
    @Test void registroNormalizaCorreoYGuardaBcrypt() throws Exception {
        String correo=correo();
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro(correo.toUpperCase(),carne())))
                .andExpect(status().isCreated()).andExpect(jsonPath("correo").value(correo))
                .andExpect(jsonPath("correoVerificado").value(false));
        assertThat(passwords.matches(PASSWORD,usuarios.findByCorreo(correo).orElseThrow().getContrasenaHash())).isTrue();
    }
    @Test void rechazaDominioAjenoYSufijoEnganoso() throws Exception {
        for (String correo:new String[]{"alumno@example.com","alumno@uvg.edu.gt.evil.test","alumno@eviluvg.edu.gt"})
            mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro(correo,carne())))
                    .andExpect(status().isBadRequest());
    }
    @Test void correoYCarneSonUnicos() throws Exception {
        String correo=correo(), carne=carne();
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro(correo,carne))).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro(correo,carne()))).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro(correo(),carne))).andExpect(status().isConflict());
    }
    @Test void noAceptaRolDesdeRegistro() throws Exception {
        String body=registro(correo(),carne()).strip().replace("}",",\"rol\":\"ADMINISTRADOR\"}");
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }
    @Test void loginInvalidoYAccesoAnonimo() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"nadie@uvg.edu.gt\",\"contrasena\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("status").value(401));
        mvc.perform(get("/api/v1/pedidos")).andExpect(status().isUnauthorized()).andExpect(jsonPath("status").value(401));
    }
    @Test void logoutRevocaTokenPersistido() throws Exception {
        String token=cuenta();
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout").header("Authorization","Bearer "+token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
    @Test void contactoNoCambiaCredencialInstitucional() throws Exception {
        String token=cuenta();
        mvc.perform(patch("/api/v1/usuarios/me").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"55551111\",\"correoContacto\":\"contacto@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("correoContacto").value("contacto@example.com"));
    }
    @Test void aislamientoDePedidosYListado() throws Exception {
        String propietario=cuenta(), tercero=cuenta(); JsonNode pedido=pedido(propietario); String id=pedido.get("id").asText();
        mvc.perform(get("/api/v1/pedidos/"+id).header("Authorization","Bearer "+tercero)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/pedidos").header("Authorization","Bearer "+tercero))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElementos").value(0));
        mvc.perform(post("/api/v1/pedidos/"+id+"/cancelacion").header("Authorization","Bearer "+tercero)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/pedidos").param("estado","SOLICITADO").header("Authorization","Bearer "+propietario))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElementos").value(1));
    }
    @Test void editaCancelaYRechazaVersionObsoleta() throws Exception {
        String token=cuenta(); String id=pedido(token).get("id").asText();
        String edicion="{\"version\":0,\"solicitud\":"+solicitud().replace("Alcohol","Algodón")+"}";
        JsonNode editado=json.readTree(mvc.perform(put("/api/v1/pedidos/"+id).header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(edicion)).andExpect(status().isOk())
                .andExpect(jsonPath("materiales[0].descripcion").value("Algodón")).andReturn().getResponse().getContentAsString());
        assertThat(editado.get("version").asLong()).isGreaterThan(0);
        mvc.perform(put("/api/v1/pedidos/"+id).header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(edicion)).andExpect(status().isConflict());
        JsonNode cancelado=json.readTree(mvc.perform(post("/api/v1/pedidos/"+id+"/cancelacion").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":"+editado.get("version").asLong()+"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("estado").value("CANCELADO")).andReturn().getResponse().getContentAsString());
        mvc.perform(put("/api/v1/pedidos/"+id).header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":"+cancelado.get("version").asLong()+",\"solicitud\":"+solicitud()+"}"))
                .andExpect(status().isConflict());
    }
    @Test void administradorConsultaPeroNoCreaPedidos() throws Exception {
        String estudiante=cuenta(), id=pedido(estudiante).get("id").asText(), correo=correo();
        usuarios.saveAndFlush(new Administrador("Administrador ficticio",correo,"55550000",passwords.encode(PASSWORD),"PRUEBA"));
        String token=login(correo);
        mvc.perform(get("/api/v1/pedidos/"+id).header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/pedidos").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
                .content(solicitud())).andExpect(status().isForbidden());
    }
    @Test void validaMaterialesFechasYPaginacion() throws Exception {
        String token=cuenta();
        for (String body:new String[]{solicitud().replace("1.250","0"),solicitud().replace("1.250","1.0001"),
                solicitud().replace(LocalDate.now().plusDays(10).toString(),LocalDate.now().minusDays(1).toString())})
            mvc.perform(post("/api/v1/pedidos").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/pedidos").param("tamanio","101").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
    }
    @Test void openApiDescribeContrato() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("paths['/api/v1/pedidos']").exists())
                .andExpect(jsonPath("components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }
    @Test void servidorArrancaYRespondePorHttp() throws Exception {
        var uri=java.net.URI.create("http://localhost:"+environment.getProperty("local.server.port")+"/api/v1/pedidos");
        try (var client=java.net.http.HttpClient.newHttpClient()) {
            var response=client.send(java.net.http.HttpRequest.newBuilder(uri).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(json.readTree(response.body()).get("status").asInt()).isEqualTo(401);
        }
    }
    @Test void dosEdicionesConcurrentesNoSeSobrescriben() throws Exception {
        String token=cuenta(), id=pedido(token).get("id").asText();
        var inicio=new java.util.concurrent.CountDownLatch(1);
        try (var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> editar=() -> {
                inicio.await();
                return mvc.perform(put("/api/v1/pedidos/"+id).header("Authorization","Bearer "+token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"solicitud\":"+solicitud()+"}"))
                        .andReturn().getResponse().getStatus();
            };
            var primero=executor.submit(editar); var segundo=executor.submit(editar); inicio.countDown();
            assertThat(java.util.List.of(primero.get(),segundo.get())).containsExactlyInAnyOrder(200,409);
        }
    }
    @Test void tokenAlteradoNoAutentica() throws Exception {
        String token=cuenta(); String[] partes=token.split("\\.");
        String firma=partes[2]; partes[2]=(firma.charAt(0)=='A' ? "B" : "A")+firma.substring(1);
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization","Bearer "+String.join(".",partes)))
                .andExpect(status().isUnauthorized());
    }
}
