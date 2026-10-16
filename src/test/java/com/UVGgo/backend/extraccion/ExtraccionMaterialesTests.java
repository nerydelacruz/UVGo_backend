package com.UVGgo.backend.extraccion;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.unit.DataSize;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExtraccionMaterialesTests {
    private final LectorPdf lector = new LectorPdf(new RecepcionPdfService(DataSize.ofMegabytes(5)));
    private final ProveedorMateriales proveedor = mock(ProveedorMateriales.class);

    static byte[] pdf(String texto, int paginas) throws Exception {
        try (var documento = new PDDocument(); var salida = new ByteArrayOutputStream()) {
            for (int i = 0; i < paginas; i++) {
                var pagina = new PDPage(); documento.addPage(pagina);
                try (var contenido = new PDPageContentStream(documento, pagina)) {
                    contenido.beginText();
                    contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contenido.newLineAtOffset(40, 750); contenido.showText(texto); contenido.endText();
                }
            }
            documento.save(salida); return salida.toByteArray();
        }
    }

    @Test void leePdfRealYLlamaUnaVezSinPersistencia() throws Exception {
        when(proveedor.extraer(anyString())).thenReturn(List.of());
        var mvc = MockMvcBuilders.standaloneSetup(new ExtraccionMaterialesController(lector, proveedor))
                .setControllerAdvice(new RecepcionPdfErrores()).build();
        mvc.perform(multipart("/extracciones/materiales").file(new MockMultipartFile(
                "archivo", "lista.pdf", "application/pdf", pdf("Dos pares de guantes", 1))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.materiales").isArray());
        verify(proveedor).extraer("Dos pares de guantes");
    }

    @Test void rechazaPdfVacioDanadoSinTextoYDemasiadasPaginasAntesDeConsumirIA() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ExtraccionMaterialesController(lector, proveedor))
                .setControllerAdvice(new RecepcionPdfErrores()).build();
        for (byte[] bytes : List.of(pdf("", 1), pdf("Materiales de laboratorio", 21), "%PDF-1.4 roto".getBytes())) {
            mvc.perform(multipart("/extracciones/materiales").file(new MockMultipartFile(
                    "archivo", "lista.pdf", "application/pdf", bytes)))
                    .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.mensaje").exists());
        }
        mvc.perform(multipart("/extracciones/materiales")).andExpect(status().isBadRequest());
        verifyNoInteractions(proveedor);
    }

    @Test void limitaTextoSinTruncarloSilenciosamente() throws Exception {
        var archivo = new MockMultipartFile("archivo", "lista.pdf", "application/pdf", pdf("a".repeat(2100), 20));
        assertThatThrownBy(() -> lector.leer(archivo)).hasMessageContaining("40,000");
    }
}
