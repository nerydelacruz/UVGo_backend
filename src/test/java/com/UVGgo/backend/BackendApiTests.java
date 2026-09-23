package com.UVGgo.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendApiTests {
    @Autowired MockMvc mvc;

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
