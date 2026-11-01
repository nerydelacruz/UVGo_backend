package com.UVGgo.backend;

import com.UVGgo.backend.archivo.AlmacenArchivos;
import com.UVGgo.backend.correo.Correo;
import com.UVGgo.backend.usuario.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.*;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
class BackendApiTests {
    @Autowired ApplicationEvents eventos;
    @Autowired AlmacenEnMemoria almacen;

    // Sustituye a Azure Blob Storage en las pruebas.
    static class AlmacenEnMemoria implements AlmacenArchivos {
        final java.util.Map<String,byte[]> archivos=new java.util.concurrent.ConcurrentHashMap<>();
        public void guardar(String ruta,byte[] contenido,String tipo) { archivos.put(ruta,contenido); }
        public byte[] leer(String ruta) { return archivos.get(ruta); }
        public void eliminar(String ruta) { archivos.remove(ruta); }
    }
    @TestConfiguration
    static class Almacen {
        @Bean @Primary AlmacenEnMemoria almacenEnMemoria() { return new AlmacenEnMemoria(); }
    }
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
                 "productos":[{"web":{"titulo":"Alcohol","url":"https://tienda.example/alcohol","precio":25.50,"moneda":"GTQ"},"cantidad":2}]}
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
                .andExpect(jsonPath("productos[0].descripcion").value("Algodón")).andReturn().getResponse().getContentAsString());
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
    @Test void notificaPorCorreoCreacionYCancelacion() throws Exception {
        String correo=correo();
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro(correo,carne())))
                .andExpect(status().isCreated());
        String token=login(correo);
        String id=pedido(token).get("id").asText();
        for (int i=0;i<2;i++)
            mvc.perform(post("/api/v1/pedidos/"+id+"/cancelacion").header("Authorization","Bearer "+token)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"version\":"+(i==0 ? 0 : 1)+"}"));
        var correos=eventos.stream(Correo.class).toList();
        assertThat(correos).extracting(Correo::para).containsOnly(correo);
        assertThat(correos).extracting(Correo::asunto)
                .containsExactly("Recibimos tu solicitud de cotización para Química","Solicitud de Química cancelada");
        assertThat(correos.getFirst().html()).contains(id).doesNotContain("<script");
    }
    private String administrador() throws Exception {
        String correo=correo();
        usuarios.saveAndFlush(new Administrador("Administrador ficticio",correo,"55550000",passwords.encode(PASSWORD),"PRUEBA"));
        return login(correo);
    }
    @Test void aceptaMaterialesDeTextoLibreYRechazaOrigenesInvalidos() throws Exception {
        String estudiante=cuenta();
        String conTexto=solicitud().replaceAll("\"productos\":\\[.*\\]\\}","\"productos\":[{\"texto\":\"74LS08N AND DIP-14\",\"cantidad\":3}]}");
        mvc.perform(post("/api/v1/pedidos").header("Authorization","Bearer "+estudiante)
                .contentType(MediaType.APPLICATION_JSON).content(conTexto)).andExpect(status().isCreated())
                .andExpect(jsonPath("productos[0].tipo").value("TEXTO"))
                .andExpect(jsonPath("productos[0].descripcion").value("74LS08N AND DIP-14"))
                .andExpect(jsonPath("productos[0].cantidad").value(3));
        for (String invalido : List.of(
                conTexto.replace("\"texto\":\"74LS08N AND DIP-14\"","\"texto\":\"   \""),
                conTexto.replace("\"texto\":","\"articuloId\":1,\"texto\":")))
            mvc.perform(post("/api/v1/pedidos").header("Authorization","Bearer "+estudiante)
                    .contentType(MediaType.APPLICATION_JSON).content(invalido)).andExpect(status().isBadRequest());
    }
    @Test void enlazaEnSegundoPlanoLosMaterialesDeTextoConElCatalogo() throws Exception {
        int articuloId=json.readTree(mvc.perform(post("/articulos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Circuito integrado 74LS08N\",\"descripcion\":\"Compuerta AND DIP-14\",\"precio\":8.50,\"moneda\":\"GTQ\",\"tienda\":\"Tienda X\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asInt();
        try {
            String estudiante=cuenta();
            String body=solicitud().replaceAll("\"productos\":\\[.*\\]\\}",
                    "\"productos\":[{\"texto\":\"74LS08N – AND de 2 entradas ×4 (DIP-14)\",\"codigo\":\"74LS08N\",\"cantidad\":3},"
                    +"{\"texto\":\"Pulsador normalmente abierto 6x6x5mm\",\"cantidad\":2}]}");
            String id=json.readTree(mvc.perform(post("/api/v1/pedidos").header("Authorization","Bearer "+estudiante)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                    .andExpect(jsonPath("productos[0].tipo").value("TEXTO"))
                    .andReturn().getResponse().getContentAsString()).get("id").asText();
            // La búsqueda corre después de responder: se espera a que el primer material pase a ser del catálogo
            JsonNode detalle=null;
            for (int intento=0;intento<50;intento++) {
                detalle=json.readTree(mvc.perform(get("/api/v1/pedidos/"+id).header("Authorization","Bearer "+estudiante))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
                if ("CATALOGO".equals(detalle.get("productos").get(0).get("tipo").asText())) break;
                Thread.sleep(100);
            }
            assertThat(detalle.get("productos").get(0).get("tipo").asText()).isEqualTo("CATALOGO");
            assertThat(detalle.get("productos").get(0).get("articuloId").asInt()).isEqualTo(articuloId);
            assertThat(detalle.get("productos").get(0).get("cantidad").asInt()).isEqualTo(3);
            assertThat(detalle.get("verificandoStock").asBoolean()).isFalse();
            // Sin parecido en el catálogo el material sigue como "del PDF"
            assertThat(detalle.get("productos").get(1).get("tipo").asText()).isEqualTo("TEXTO");
        } finally { mvc.perform(delete("/articulos/"+articuloId)).andExpect(status().isNoContent()); }
    }
    @Test void cotizaYAceptaSolicitudConProductosDelCatalogoYDeLaWeb() throws Exception {
        int articuloId=json.readTree(mvc.perform(post("/articulos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Calculadora científica\",\"descripcion\":\"Casio\",\"precio\":150.00,\"moneda\":\"GTQ\",\"tienda\":\"Tienda X\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asInt();
        try { cotizaYAcepta(articuloId); }
        // La base H2 es compartida con ArticuloApiTests: no se deja el artículo en el catálogo.
        finally { mvc.perform(delete("/articulos/"+articuloId)).andExpect(status().isNoContent()); }
    }
    private void cotizaYAcepta(int articuloId) throws Exception {
        String estudiante=cuenta(), admin=administrador();
        String body=solicitud().replace("\"productos\":[","\"productos\":[{\"articuloId\":"+articuloId+",\"cantidad\":1},");
        JsonNode solicitud=json.readTree(mvc.perform(post("/api/v1/pedidos").header("Authorization","Bearer "+estudiante)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("productos[0].tipo").value("CATALOGO"))
                .andExpect(jsonPath("productos[0].descripcion").value("Calculadora científica"))
                .andExpect(jsonPath("productos[0].precioReferencia").value(150.00))
                .andExpect(jsonPath("productos[1].tipo").value("WEB"))
                .andExpect(jsonPath("total").doesNotExist())
                .andReturn().getResponse().getContentAsString());
        String id=solicitud.get("id").asText();
        String item0=solicitud.get("productos").get(0).get("id").asText(), item1=solicitud.get("productos").get(1).get("id").asText();
        String precio0="{\"itemId\":\""+item0+"\",\"precioUnitario\":160.00}", precio1="{\"itemId\":\""+item1+"\",\"precioUnitario\":30.25}";
        String cotizacion="{\"version\":0,\"precios\":["+precio0+","+precio1+"],\"cargoServicio\":15.00,\"notas\":\"Entrega en 3 días\"}";
        mvc.perform(post("/api/v1/pedidos/"+id+"/aceptacion").header("Authorization","Bearer "+estudiante)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}")).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/pedidos/"+id+"/cotizacion").header("Authorization","Bearer "+estudiante)
                .contentType(MediaType.APPLICATION_JSON).content(cotizacion)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pedidos/"+id+"/cotizacion").header("Authorization","Bearer "+admin)
                .contentType(MediaType.APPLICATION_JSON).content(cotizacion.replace(","+precio1,"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("detail").value("Debe indicar un precio para cada producto de la solicitud"));
        mvc.perform(post("/api/v1/pedidos/"+id+"/cotizacion").header("Authorization","Bearer "+admin)
                .contentType(MediaType.APPLICATION_JSON).content(cotizacion)).andExpect(status().isOk())
                .andExpect(jsonPath("estado").value("COTIZADO"))
                .andExpect(jsonPath("productos[1].subtotal").value(60.50))
                .andExpect(jsonPath("total").value(235.50))
                .andExpect(jsonPath("monedaCotizacion").value("GTQ"));
        mvc.perform(put("/api/v1/pedidos/"+id).header("Authorization","Bearer "+estudiante)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1,\"solicitud\":"+solicitud()+"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/pedidos/"+id+"/aceptacion").header("Authorization","Bearer "+estudiante)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}")).andExpect(status().isOk())
                .andExpect(jsonPath("estado").value("CONFIRMADO")).andExpect(jsonPath("fechaConfirmacion").exists());
        mvc.perform(post("/api/v1/pedidos/"+id+"/cotizacion").header("Authorization","Bearer "+admin)
                .contentType(MediaType.APPLICATION_JSON).content(cotizacion.replace("\"version\":0","\"version\":2")))
                .andExpect(status().isConflict());
        assertThat(eventos.stream(Correo.class).map(Correo::asunto)).containsExactly(
                "Recibimos tu solicitud de cotización para Química","Tu cotización para Química está lista",
                "Pedido confirmado para Química");
    }
    // Solicitud de un producto web cotizada en Q 66.00 (2 x 25.50 + 15.00) y aceptada por el estudiante.
    private String confirmada(String estudiante,String admin) throws Exception {
        JsonNode s=pedido(estudiante); String id=s.get("id").asText();
        mvc.perform(post("/api/v1/pedidos/"+id+"/cotizacion").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"precios\":[{\"itemId\":\""+s.get("productos").get(0).get("id").asText()
                        +"\",\"precioUnitario\":25.50}],\"cargoServicio\":15.00}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/pedidos/"+id+"/aceptacion").header("Authorization","Bearer "+estudiante)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}")).andExpect(status().isOk());
        return id;
    }
    private static final byte[] PDF="%PDF-1.4 comprobante ficticio".getBytes();
    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder comprobante(String id,long version,byte[] contenido) {
        var subida=multipart(org.springframework.http.HttpMethod.PUT,"/api/v1/pedidos/"+id+"/comprobante");
        subida.file(new MockMultipartFile("archivo","../boleta pago.pdf","application/pdf",contenido)).param("version",String.valueOf(version));
        return subida;
    }
    @Test void subeComprobanteYAdministradorRegistraPago() throws Exception {
        String estudiante=cuenta(), admin=administrador(), otro=cuenta(), id=confirmada(estudiante,admin);
        mvc.perform(post("/api/v1/pedidos/"+id+"/pago").header("Authorization","Bearer "+admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":2}")).andExpect(status().isConflict());
        mvc.perform(comprobante(id,2,"texto plano".getBytes()).header("Authorization","Bearer "+estudiante))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(comprobante(id,2,new byte[6*1024*1024]).header("Authorization","Bearer "+estudiante))
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(comprobante(id,2,PDF).header("Authorization","Bearer "+otro)).andExpect(status().isNotFound());
        mvc.perform(comprobante(id,2,PDF).header("Authorization","Bearer "+estudiante)).andExpect(status().isOk())
                .andExpect(jsonPath("comprobante.nombre").value("boleta pago.pdf"))
                .andExpect(jsonPath("comprobante.tipo").value("application/pdf"))
                .andExpect(jsonPath("comprobante.tamano").value(PDF.length));
        byte[] png={(byte)0x89,'P','N','G','\r','\n',0x1A,'\n',1,2,3};
        mvc.perform(comprobante(id,3,png).header("Authorization","Bearer "+estudiante)).andExpect(status().isOk())
                .andExpect(jsonPath("comprobante.tipo").value("image/png"));
        assertThat(almacen.archivos.keySet().stream().filter(r -> r.contains(id))).hasSize(1);
        mvc.perform(get("/api/v1/pedidos/"+id+"/comprobante").header("Authorization","Bearer "+admin)).andExpect(status().isOk())
                .andExpect(content().contentType("image/png")).andExpect(content().bytes(png));
        mvc.perform(get("/api/v1/pedidos/"+id+"/comprobante").header("Authorization","Bearer "+otro)).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/pedidos/"+id+"/pago").header("Authorization","Bearer "+estudiante)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":4}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pedidos/"+id+"/pago").header("Authorization","Bearer "+admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":4}")).andExpect(status().isOk())
                .andExpect(jsonPath("estado").value("PAGADO")).andExpect(jsonPath("fechaPago").exists());
        mvc.perform(comprobante(id,5,PDF).header("Authorization","Bearer "+estudiante)).andExpect(status().isConflict());
        assertThat(eventos.stream(Correo.class).map(Correo::asunto)).contains("Pago confirmado para Química");
    }
    @Test void administradorConsultaPeroNoCreaPedidos() throws Exception {
        String estudiante=cuenta(), id=pedido(estudiante).get("id").asText(), token=administrador();
        mvc.perform(get("/api/v1/pedidos/"+id).header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/pedidos").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
                .content(solicitud())).andExpect(status().isForbidden());
    }
    @Test void validaMaterialesFechasYPaginacion() throws Exception {
        String token=cuenta();
        for (String body:new String[]{solicitud().replace("\"cantidad\":2","\"cantidad\":0"),
                solicitud().replace("\"cantidad\":2","\"cantidad\":1000"),
                solicitud().replace("https://tienda.example/alcohol","javascript:alert(1)"),
                solicitud().replace("\"web\":","\"articuloId\":1,\"web\":"),
                solicitud().replace("\"web\":{\"titulo\":\"Alcohol\",\"url\":\"https://tienda.example/alcohol\",\"precio\":25.50,\"moneda\":\"GTQ\"},",""),
                solicitud().replace("\"web\":{\"titulo\":\"Alcohol\",\"url\":\"https://tienda.example/alcohol\",\"precio\":25.50,\"moneda\":\"GTQ\"},","\"articuloId\":999999,"),
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
        var uri=java.net.URI.create("http://localhost:"+environment.getProperty("local.server.port")
                +environment.getProperty("server.servlet.context-path","")+"/api/v1/pedidos");
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
