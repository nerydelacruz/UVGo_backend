package com.UVGgo.backend.listas;

import com.UVGgo.backend.config.SeguridadConfig;
import com.UVGgo.backend.extraccion.*;
import com.UVGgo.backend.usuario.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=ListasMaterialesTests.Config.class, properties={
    "spring.profiles.active=test", "spring.config.import=",
    "spring.datasource.url=jdbc:h2:mem:listas;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
    "uvgo.security.jwt-secret=dXZnby10ZXN0LW9ubHktMzItYnl0ZS1rZXktbm90LXByb2R1Y3Rpb24="
})
@AutoConfigureMockMvc
class ListasMaterialesTests {
    @Configuration @EnableAutoConfiguration
    @EntityScan(basePackages={"com.UVGgo.backend.listas","com.UVGgo.backend.usuario","com.UVGgo.backend.auth"})
    @EnableJpaRepositories(basePackages={"com.UVGgo.backend.listas","com.UVGgo.backend.usuario","com.UVGgo.backend.auth"})
    @Import({ListaMaterialesController.class, SeguridadConfig.class, ExtraccionMaterialesController.class,
            LectorPdf.class, RecepcionPdfService.class, RecepcionPdfErrores.class})
    static class Config {}
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired ObjectMapper json;
    @MockitoBean ProveedorMateriales proveedor;
    private static final String CUERPO = """
        {"nombre":"Laboratorio","revisada":true,"materiales":[{"nombre":"Guantes",
        "descripcion":"Talla M","cantidad":2,"unidad":"par","observaciones":"Para uso individual","referencia":"dos pares de guantes"}]}
        """;
    private UUID usuario() {
        String unico=UUID.randomUUID().toString();
        return usuarios.save(new Estudiante("Prueba",unico+"@uvg.edu.gt","55550000","hash",unico.substring(0,20),"Ingeniería")).getId();
    }
    @Test void guardaYRecuperaSoloParaSuPropietario() throws Exception {
        UUID propietario=usuario(), otro=usuario();
        var resultado=mvc.perform(post("/api/v1/listas-materiales").with(jwt().jwt(j -> j.subject(propietario.toString())))
            .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
            .andExpect(status().isCreated()).andReturn();
        String id=json.readTree(resultado.getResponse().getContentAsString()).path("id").asText();
        mvc.perform(get("/api/v1/listas-materiales/"+id).with(jwt().jwt(j -> j.subject(propietario.toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.materiales[0].unidad").value("par"))
            .andExpect(jsonPath("$.materiales[0].cantidad").value(2));
        mvc.perform(get("/api/v1/listas-materiales/"+id).with(jwt().jwt(j -> j.subject(otro.toString()))))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/listas-materiales").with(jwt().jwt(j -> j.subject(otro.toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.listas.length()").value(0));
    }
    @Test void requiereSesionParaGuardarConsultarYAnalizar() throws Exception {
        mvc.perform(get("/api/v1/listas-materiales")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/listas-materiales").contentType(MediaType.APPLICATION_JSON).content(CUERPO)).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/v1/extracciones/materiales").file("archivo",new byte[]{1})).andExpect(status().isUnauthorized());
    }
    @Test void rechazaListaSinRevisionOCantidadValida() throws Exception {
        UUID id=usuario();
        for (String invalido:List.of(CUERPO.replace("true","false"),CUERPO.replace("\"cantidad\":2","\"cantidad\":0"),
                CUERPO.replace("\"cantidad\":2","\"cantidad\":2.0001"),CUERPO.replace("Talla M"," "))) {
            mvc.perform(post("/api/v1/listas-materiales").with(jwt().jwt(j -> j.subject(id.toString())))
                .contentType(MediaType.APPLICATION_JSON).content(invalido)).andExpect(status().isBadRequest());
        }
    }
    @Test void extraePdfSinGuardarLista() throws Exception {
        byte[] pdf;
        try (var documento=new PDDocument(); var salida=new ByteArrayOutputStream()) {
            var pagina=new PDPage(); documento.addPage(pagina);
            try (var texto=new PDPageContentStream(documento,pagina)) {
                texto.beginText(); texto.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);
                texto.newLineAtOffset(50,700); texto.showText("dos pares de guantes"); texto.endText();
            }
            documento.save(salida); pdf=salida.toByteArray();
        }
        when(proveedor.extraer(contains("dos pares de guantes"))).thenReturn(List.of(
            new MaterialExtraido("Guantes","Talla M",new BigDecimal("2"),"par",null,"dos pares de guantes",List.of())));
        UUID id=usuario();
        mvc.perform(multipart("/api/v1/extracciones/materiales").file(new MockMultipartFile("archivo","lista.pdf","application/pdf",pdf))
            .with(jwt().jwt(j -> j.subject(id.toString())))).andExpect(status().isOk())
            .andExpect(jsonPath("$.materiales[0].nombre").value("Guantes"));
        mvc.perform(get("/api/v1/listas-materiales").with(jwt().jwt(j -> j.subject(id.toString()))))
            .andExpect(jsonPath("$.listas.length()").value(0));
    }
}
