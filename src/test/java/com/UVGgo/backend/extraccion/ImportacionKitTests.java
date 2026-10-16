package com.UVGgo.backend.extraccion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Aísla el contrato de guardado de las migraciones históricas desactualizadas. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:importacion;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportacionKitTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test @Transactional
    void confirmarMaterialConMismoNombreNoModificaLaDescripcionDelKitBase() throws Exception {
        String base = """
            {"name":"Kit prueba IA","course":"Laboratorio","price":0,"articulos":[
             {"nombre":"Guantes","descripcion":"Original del kit base","cantidad":1,"categoria":""}]}
            """;
        int id = json.readTree(mvc.perform(post("/kits").contentType(MediaType.APPLICATION_JSON).content(base))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("kitId").asInt();
        String copia = """
            {"kitId":%d,"name":"Kit prueba IA","course":"Laboratorio","price":0,"articulos":[
             {"nombre":"Guantes","descripcion":"Nitrilo (Unidad: par)","cantidad":2,"categoria":"",
              "observacionesCotizacion":"Fuente del PDF: Dos pares de guantes de nitrilo"}]}
            """.formatted(id);
        int copiaId = json.readTree(mvc.perform(put("/kits").contentType(MediaType.APPLICATION_JSON).content(copia))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.articulos[0].cantidad").value(2))
                .andReturn().getResponse().getContentAsString()).path("kitId").asInt();
        mvc.perform(get("/kits/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.articulos[0].descripcion").value("Original del kit base"));
        mvc.perform(get("/kits/" + copiaId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.articulos[0].descripcion").value("Nitrilo (Unidad: par)"));
    }
}
