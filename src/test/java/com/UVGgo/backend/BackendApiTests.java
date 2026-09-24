package com.UVGgo.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import com.UVGgo.backend.persistence.crud.KitCrudRepository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendApiTests {
    @Autowired MockMvc mvc;
    @Autowired KitCrudRepository kits;
    @Autowired ObjectMapper json;
    @Autowired EntityManager entityManager;

    private String datosArticulo(String nombre, String cantidad) {
        return """
                {"nombre":"%s","descripcion":"Material de laboratorio","cantidad":%s,
                 "categoria":"Laboratorio","observacionesCotizacion":"Buscar presentación pequeña"}
                """.formatted(nombre, cantidad);
    }

    private JsonNode crearKitConArticulo() throws Exception {
        String body = """
                {"name":"Kit base de prueba","course":"Química","price":100,
                 "articulos":[%s]}
                """.formatted(datosArticulo("Bata", "1"));
        return json.readTree(mvc.perform(post("/kits").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BASE"))
                .andExpect(jsonPath("$.articulos[0].fechaCreacion").isNotEmpty())
                .andReturn().getResponse().getContentAsString());
    }

    private String datosPersonalizacion(Integer usuarioId, String... articulos) {
        return """
                {%s"name":"Mi kit de Química","description":"Kit ajustado","course":"Química","price":120,
                 "articulos":[%s]}
                """.formatted(usuarioId == null ? "" : "\"usuarioId\":" + usuarioId + ",", String.join(",", articulos));
    }

    private org.springframework.test.web.servlet.ResultActions personalizar(int kitId, String body) throws Exception {
        String conKitId = "{\"kitId\":" + kitId + "," + body.strip().substring(1);
        return mvc.perform(put("/kits").contentType(MediaType.APPLICATION_JSON).content(conKitId));
    }

    @Test
    @Transactional
    void personalizaConDatosYArticulosEnviadosYListaPorUsuario() throws Exception {
        JsonNode base = crearKitConArticulo();
        int baseId = base.get("kitId").asInt();
        entityManager.flush();
        entityManager.clear();

        personalizar(baseId, datosPersonalizacion(7, datosArticulo("Bata talla M", "1"), datosArticulo("Guantes", "2")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kitBaseId").value(baseId))
                .andExpect(jsonPath("$.usuarioId").value(7))
                .andExpect(jsonPath("$.estado").value("PERSONALIZADO"))
                .andExpect(jsonPath("$.name").value("Mi kit de Química"))
                .andExpect(jsonPath("$.price").value(120))
                .andExpect(jsonPath("$.articulos.length()").value(2))
                .andExpect(jsonPath("$.articulos[0].nombre").value("Bata talla M"))
                .andExpect(jsonPath("$.articulos[1].cantidad").value(2));
        personalizar(baseId, datosPersonalizacion(8, datosArticulo("Bata", "1")))
                .andExpect(status().isCreated());
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/kits/personalizados").param("usuarioId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Mi kit de Química"))
                .andExpect(jsonPath("$[0].usuarioId").value(7));
        mvc.perform(get("/kits/personalizados").param("usuarioId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/kits/" + baseId))
                .andExpect(jsonPath("$.name").value("Kit base de prueba"))
                .andExpect(jsonPath("$.articulos.length()").value(1))
                .andExpect(jsonPath("$.articulos[0].nombre").value("Bata"));
    }

    @Test
    @Transactional
    void personalizaSinUsuarioYListaTodosLosPersonalizados() throws Exception {
        int baseId = crearKitConArticulo().get("kitId").asInt();
        entityManager.flush();
        entityManager.clear();
        personalizar(baseId, datosPersonalizacion(null, datosArticulo("Bata", "1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kitBaseId").value(baseId))
                .andExpect(jsonPath("$.estado").value("PERSONALIZADO"))
                .andExpect(jsonPath("$.usuarioId").doesNotExist());
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/kits/personalizados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Mi kit de Química"))
                .andExpect(jsonPath("$[0].estado").value("PERSONALIZADO"));
    }

    @Test
    @Transactional
    void personalizarValidaDatos() throws Exception {
        int baseId = crearKitConArticulo().get("kitId").asInt();
        personalizar(baseId, datosPersonalizacion(0, datosArticulo("Bata", "1"))).andExpect(status().isBadRequest());
        mvc.perform(put("/kits").contentType(MediaType.APPLICATION_JSON).content(datosPersonalizacion(null, datosArticulo("Bata", "1"))))
                .andExpect(status().isBadRequest());
        personalizar(baseId, datosPersonalizacion(7, datosArticulo("Bata", "0"))).andExpect(status().isBadRequest());
        personalizar(baseId, "{\"usuarioId\":7,\"course\":\"Química\",\"price\":10}").andExpect(status().isBadRequest());
        mvc.perform(post("/kits").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":7,\"name\":\"Kit\",\"course\":\"Curso\",\"price\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").doesNotExist())
                .andExpect(jsonPath("$.estado").value("BASE"));
    }

    @Test
    @Transactional
    void personalizaYAdministraArticulosSinModificarElKitBase() throws Exception {
        JsonNode base = crearKitConArticulo();
        int baseId = base.get("kitId").asInt();
        int articuloBaseId = base.get("articulos").get(0).get("id").asInt();
        entityManager.flush();
        entityManager.clear();

        JsonNode copia = json.readTree(personalizar(baseId, datosPersonalizacion(7, datosArticulo("Bata", "1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kitBaseId").value(baseId))
                .andExpect(jsonPath("$.estado").value("PERSONALIZADO"))
                .andExpect(jsonPath("$.articulos.length()").value(1))
                .andReturn().getResponse().getContentAsString());
        int copiaId = copia.get("kitId").asInt();
        int articuloCopiaId = copia.get("articulos").get(0).get("id").asInt();
        assertThat(copiaId).isNotEqualTo(baseId);
        assertThat(articuloCopiaId).isNotEqualTo(articuloBaseId);
        assertThat(copia.get("articulos").get(0).get("kitId").asInt()).isEqualTo(copiaId);

        JsonNode adicional = json.readTree(mvc.perform(post("/kits/" + copiaId + "/articulos")
                .contentType(MediaType.APPLICATION_JSON).content(datosArticulo("Guantes", "2")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kitId").value(copiaId))
                .andReturn().getResponse().getContentAsString());
        int adicionalId = adicional.get("id").asInt();
        mvc.perform(put("/kits/" + copiaId + "/articulos/" + adicionalId)
                .contentType(MediaType.APPLICATION_JSON).content(datosArticulo("Guantes de nitrilo", "3")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Guantes de nitrilo"))
                .andExpect(jsonPath("$.cantidad").value(3))
                .andExpect(jsonPath("$.fechaCreacion").value(adicional.get("fechaCreacion").asText()));
        mvc.perform(delete("/kits/" + copiaId + "/articulos/" + articuloCopiaId))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/kits/" + copiaId + "/articulos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Guantes de nitrilo"));
        mvc.perform(get("/kits/" + baseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BASE"))
                .andExpect(jsonPath("$.articulos.length()").value(1))
                .andExpect(jsonPath("$.articulos[0].id").value(articuloBaseId))
                .andExpect(jsonPath("$.articulos[0].nombre").value("Bata"));
        personalizar(copiaId, datosPersonalizacion(7, datosArticulo("Bata", "1")))
                .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    void rechazaArticulosInvalidosYArticulosDeOtroKit() throws Exception {
        JsonNode primero = crearKitConArticulo();
        JsonNode segundo = crearKitConArticulo();
        int kitId = primero.get("kitId").asInt();
        int ajeno = segundo.get("articulos").get(0).get("id").asInt();
        String ruta = "/kits/" + kitId + "/articulos";
        for (String body : new String[]{"{}", datosArticulo(" ", "1"), datosArticulo("Bata", "0"),
                datosArticulo("Bata", "-1"), datosArticulo("Bata", "1.0001")}) {
            mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(put(ruta + "/" + ajeno).contentType(MediaType.APPLICATION_JSON)
                .content(datosArticulo("Cambio", "1"))).andExpect(status().isNotFound());
        mvc.perform(delete(ruta + "/" + ajeno)).andExpect(status().isNotFound());
        mvc.perform(get("/kits/2147483647/articulos")).andExpect(status().isNotFound());
        personalizar(2147483647, datosPersonalizacion(7, datosArticulo("Bata", "1"))).andExpect(status().isNotFound());
        mvc.perform(post("/kits/2147483647/articulos").contentType(MediaType.APPLICATION_JSON)
                .content(datosArticulo("Bata", "1"))).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void agregaKitSinSobrescribirElIdEnviado() throws Exception {
        var existente = kits.findAllByOrderByNombreAsc().getFirst();
        String nombreOriginal = existente.getNombre();
        long cantidadOriginal = kits.count();
        mvc.perform(post("/kits").contentType(MediaType.APPLICATION_JSON).content("""
                {"kitId":%d,"name":"Kit nuevo","description":"Materiales de prueba",
                 "course":"Biología","price":99.50}
                """.formatted(existente.getIdKit())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kitId").value(greaterThan(0)))
                .andExpect(jsonPath("$.name").value("Kit nuevo"))
                .andExpect(jsonPath("$.price").value(99.50))
                .andExpect(jsonPath("$.active").value(true));
        assertThat(kits.count()).isEqualTo(cantidadOriginal + 1);
        assertThat(kits.findById(existente.getIdKit()).orElseThrow().getNombre()).isEqualTo(nombreOriginal);
        assertThat(kits.findAllByOrderByNombreAsc()).anySatisfy(kit -> {
            assertThat(kit.getNombre()).isEqualTo("Kit nuevo");
            assertThat(kit.getCurso()).isEqualTo("Biología");
        });
    }

    @Test
    @Transactional
    void permiteCrearKitInactivo() throws Exception {
        mvc.perform(post("/kits").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Kit inactivo","course":"Biología","price":0,"active":false}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @Transactional
    void rechazaKitsInvalidosSinGuardarlos() throws Exception {
        long cantidadOriginal = kits.count();
        for (String body : new String[]{
                "{}",
                "{\"name\":\" \",\"course\":\"Curso\",\"price\":10}",
                "{\"name\":\"Kit\",\"course\":\" \",\"price\":10}",
                "{\"name\":\"Kit\",\"course\":\"Curso\",\"price\":-1}",
                "{\"name\":\"Kit\",\"course\":\"Curso\",\"price\":1.001}",
                "{\"name\":\"Kit\",\"course\":\"Curso\",\"price\":100000000}",
                "{\"name\":\"" + "a".repeat(121) + "\",\"course\":\"Curso\",\"price\":10}"
        }) {
            mvc.perform(post("/kits").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertThat(kits.count()).isEqualTo(cantidadOriginal);
    }

    @Test void listaKitsOrdenadosPorNombre() throws Exception {
        mvc.perform(get("/kits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Kit de Dibujo Técnico"))
                .andExpect(jsonPath("$[0].kitId").isNumber())
                .andExpect(jsonPath("$[0].course").value("Dibujo Técnico"))
                .andExpect(jsonPath("$[0].price").value(120.50))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].nombre").doesNotExist());
    }
}
