package com.UVGgo.backend;

import com.UVGgo.backend.extraccion.RecepcionPdfController;
import com.UVGgo.backend.extraccion.RecepcionPdfService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecepcionPdfController.class)
@Import(RecepcionPdfService.class)
class RecepcionPdfTests {
    @Autowired MockMvc mvc;

    private MockMultipartFile archivo(String nombre, String tipo, byte[] bytes) {
        return new MockMultipartFile("archivo", nombre, tipo, bytes);
    }

    // Documento mínimo con una página vacía; no requiere librerías de PDF.
    private byte[] pdf() {
        StringBuilder texto = new StringBuilder("%PDF-1.4\n");
        String[] objetos = {
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << >> >>"
        };
        int[] offsets = new int[objetos.length];
        for (int i = 0; i < objetos.length; i++) {
            offsets[i] = texto.length();
            texto.append(i + 1).append(" 0 obj\n").append(objetos[i]).append("\nendobj\n");
        }
        int xref = texto.length();
        texto.append("xref\n0 4\n0000000000 65535 f \n");
        for (int offset : offsets) texto.append(String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offset));
        texto.append("trailer\n<< /Size 4 /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        return texto.toString().getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void recibePdfYDevuelveSoloMetadatos() throws Exception {
        mvc.perform(multipart("/extracciones/pdf").file(archivo("materiales.pdf", "application/pdf", pdf())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("materiales.pdf"))
                .andExpect(jsonPath("$.tamanoBytes").value(pdf().length))
                .andExpect(jsonPath("$.estado").value("RECIBIDO"))
                .andExpect(jsonPath("$.articulos").doesNotExist());
    }

    @Test
    void admiteTipoGenericoYEliminaRutaDelNombre() throws Exception {
        mvc.perform(multipart("/extracciones/pdf").file(archivo("C:\\temporal\\LISTA.PDF", "application/octet-stream", pdf())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("LISTA.PDF"));
    }

    @Test
    void rechazaArchivoVacio() throws Exception {
        mvc.perform(multipart("/extracciones/pdf").file(archivo("lista.pdf", "application/pdf", new byte[0])))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value("El archivo está vacío."));
    }

    @Test
    void solicitaElCampoArchivo() throws Exception {
        mvc.perform(multipart("/extracciones/pdf"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void rechazaExtensionTipoYCabeceraIncorrectos() throws Exception {
        for (MockMultipartFile invalido : new MockMultipartFile[] {
                archivo("lista.txt", "application/pdf", pdf()),
                archivo("lista.pdf", "image/png", pdf()),
                archivo("lista.pdf", "application/pdf", "Texto renombrado".getBytes(StandardCharsets.UTF_8))}) {
            mvc.perform(multipart("/extracciones/pdf").file(invalido))
                    .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.mensaje").exists());
        }
    }

    @Test
    void rechazaMasDeCincoMegabytes() throws Exception {
        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(pdf(), 0, grande, 0, pdf().length);
        mvc.perform(multipart("/extracciones/pdf").file(archivo("lista.pdf", "application/pdf", grande)))
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.mensaje").exists());
    }
}
