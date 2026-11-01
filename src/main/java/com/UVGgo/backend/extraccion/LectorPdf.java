package com.UVGgo.backend.extraccion;

import java.io.IOException;
import java.io.Writer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LectorPdf {
    private final RecepcionPdfService recepcion;

    public LectorPdf(RecepcionPdfService recepcion) { this.recepcion = recepcion; }

    public String leer(MultipartFile archivo) {
        recepcion.recibir(archivo);
        try (var pdf = Loader.loadPDF(archivo.getBytes())) {
            if (pdf.isEncrypted() || !pdf.getCurrentAccessPermission().canExtractContent()) {
                throw error("Use un PDF sin contraseña ni restricciones de extracción.");
            }
            if (pdf.getNumberOfPages() < 1 || pdf.getNumberOfPages() > 20) {
                throw error("El PDF debe contener entre 1 y 20 páginas.");
            }
            var salida = new TextoLimitado();
            var lector = new PDFTextStripper();
            lector.setSortByPosition(true);
            lector.writeText(pdf, salida);
            String texto = salida.texto.toString().strip();
            if (texto.length() < 10) {
                throw error("El PDF no tiene texto suficiente. Los documentos escaneados requieren OCR y no se admiten todavía.");
            }
            return texto;
        } catch (InvalidPasswordException ex) {
            throw error("Use un PDF sin contraseña.");
        } catch (IOException ex) {
            throw error("No se pudo leer el PDF. Compruebe que no esté dañado.");
        }
    }

    private static ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, mensaje);
    }

    private static final class TextoLimitado extends Writer {
        private final StringBuilder texto = new StringBuilder();
        @Override public void write(char[] buffer, int inicio, int longitud) {
            if (texto.length() + longitud > 40000) {
                throw error("El PDF supera los 40,000 caracteres. Divídalo en documentos más pequeños.");
            }
            texto.append(buffer, inicio, longitud);
        }
        @Override public void flush() {}
        @Override public void close() {}
    }
}
