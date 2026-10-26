package com.UVGgo.backend;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ArticuloApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private static final String BATA = """
            {"nombre":"Bata blanca","descripcion":"Bata de laboratorio talla M","categoria":"Laboratorio",
             "precio":85.50,"moneda":"GTQ","idExterno":"BATA-M"}
            """;

    private int crear(String body) throws Exception {
        return json.readTree(mvc.perform(post("/articulos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asInt();
    }

    @Test
    void creaConsultaEditaYEliminaUnArticulo() throws Exception {
        int id = crear(BATA);
        mvc.perform(get("/articulos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Bata blanca"))
                .andExpect(jsonPath("$.precio").value(85.50))
                .andExpect(jsonPath("$.kitId").doesNotExist());
        mvc.perform(put("/articulos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(BATA.replace("Bata blanca", "Bata azul")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("Bata azul"));
        mvc.perform(delete("/articulos/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/articulos/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/articulos/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void buscaPorNombreYPagina() throws Exception {
        crear(BATA);
        crear("{\"nombre\":\"Guantes de nitrilo\",\"descripcion\":\"Caja de 100\"}");
        mvc.perform(get("/articulos").param("q", "GUANTES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Guantes de nitrilo"));
        mvc.perform(get("/articulos").param("tamano", "1"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Bata blanca"));
        mvc.perform(get("/articulos").param("tamano", "1").param("pagina", "1"))
                .andExpect(jsonPath("$[0].nombre").value("Guantes de nitrilo"));
        mvc.perform(get("/articulos").param("tamano", "101")).andExpect(status().isBadRequest());
    }

    @Test
    void rechazaArticulosInvalidosYIdExternoRepetido() throws Exception {
        for (String body : new String[]{"{}", "{\"nombre\":\" \",\"descripcion\":\"x\"}",
                "{\"nombre\":\"x\",\"descripcion\":\"x\",\"precio\":-1}"}) {
            mvc.perform(post("/articulos").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        crear(BATA);
        mvc.perform(post("/articulos").contentType(MediaType.APPLICATION_JSON).content(BATA))
                .andExpect(status().isConflict());
    }

    @Test
    void importaProductosDeBrightDataSinDuplicar() throws Exception {
        String exportacion = """
                [
                  {"asin":"B0TEST1","title":"Audífonos inalámbricos","description":"Bluetooth 5.3",
                   "final_price":29.99,"currency":"USD","image_url":"https://img/1.jpg",
                   "seller_name":"Amazon","url":"https://amazon.com/dp/B0TEST1",
                   "categories":["Electronics","Headphones"],"rating":4.5},
                  {"sku":"W-55","product_name":"Calculadora científica","price":"$1,299.90",
                   "main_image":"https://img/2.jpg","category":"Oficina"},
                  {"asin":"B0VACIO","description":"Sin título"}
                ]
                """;
        mvc.perform(post("/articulos/importar/brightdata").contentType(MediaType.APPLICATION_JSON).content(exportacion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creados").value(2))
                .andExpect(jsonPath("$.actualizados").value(0))
                .andExpect(jsonPath("$.omitidos").value(1));

        mvc.perform(get("/articulos").param("q", "audífonos"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idExterno").value("B0TEST1"))
                .andExpect(jsonPath("$[0].precio").value(29.99))
                .andExpect(jsonPath("$[0].moneda").value("USD"))
                .andExpect(jsonPath("$[0].tienda").value("Amazon"))
                .andExpect(jsonPath("$[0].categoria").value("Headphones"))
                .andExpect(jsonPath("$[0].imagen").value("https://img/1.jpg"));
        mvc.perform(get("/articulos").param("q", "calculadora"))
                .andExpect(jsonPath("$[0].precio").value(1299.90))
                .andExpect(jsonPath("$[0].descripcion").value("Calculadora científica"))
                .andExpect(jsonPath("$[0].categoria").value("Oficina"));

        // Reimportar el mismo producto con otro precio lo actualiza, no lo duplica.
        mvc.perform(post("/articulos/importar/brightdata").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"asin\":\"B0TEST1\",\"title\":\"Audífonos inalámbricos\",\"final_price\":24.99}]"))
                .andExpect(jsonPath("$.creados").value(0))
                .andExpect(jsonPath("$.actualizados").value(1));
        mvc.perform(get("/articulos").param("q", "audífonos"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].precio").value(24.99));
    }

    @Test
    void importaElFormatoDelScraperDeElectronicaRych() throws Exception {
        String producto = """
                [{"product_name":"R-PICO Raspberry PI Pico 32 bit","product_description":"Placa con RP2040",
                  "price":{"value":55,"currency":"GTQ","symbol":"Q"},"currency":"Q",
                  "image_url":"https://electronicarych.com/web/image/1","availability":"Not Available For Sale",
                  "product_page_url":"https://electronicarych.com/shop/r-pico-14700",
                  "input":{"url":"https://electronicarych.com/shop","max_page":10}},
                 {"product_name":"PL-343BL Plug Banana","price":{"value":11.5,"currency":"GTQ","symbol":"Q"},
                  "product_page_url":"https://www.electronicarych.com/shop/pl-343bl-14710"}]
                """;
        mvc.perform(post("/articulos/importar/brightdata").contentType(MediaType.APPLICATION_JSON).content(producto))
                .andExpect(jsonPath("$.creados").value(2));
        mvc.perform(get("/articulos").param("q", "r-pico"))
                .andExpect(jsonPath("$[0].precio").value(55))
                .andExpect(jsonPath("$[0].moneda").value("GTQ"))
                .andExpect(jsonPath("$[0].tienda").value("electronicarych.com"))
                .andExpect(jsonPath("$[0].url").value("https://electronicarych.com/shop/r-pico-14700"))
                .andExpect(jsonPath("$[0].idExterno").value("https://electronicarych.com/shop/r-pico-14700"))
                .andExpect(jsonPath("$[0].imagen").value("https://electronicarych.com/web/image/1"));
        mvc.perform(get("/articulos").param("q", "plug banana"))
                .andExpect(jsonPath("$[0].precio").value(11.5))
                .andExpect(jsonPath("$[0].tienda").value("electronicarych.com"))
                .andExpect(jsonPath("$[0].descripcion").value("PL-343BL Plug Banana"));

        // Volver a importar el mismo run actualiza por URL en vez de duplicar.
        mvc.perform(post("/articulos/importar/brightdata").contentType(MediaType.APPLICATION_JSON).content(producto))
                .andExpect(jsonPath("$.creados").value(0))
                .andExpect(jsonPath("$.actualizados").value(2));
    }
}
