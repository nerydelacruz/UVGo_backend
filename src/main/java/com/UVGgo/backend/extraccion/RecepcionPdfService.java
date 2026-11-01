package com.UVGgo.backend.extraccion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecepcionPdfService {
    private final long maxBytes;

    public RecepcionPdfService(@Value("${spring.servlet.multipart.max-file-size:5MB}") DataSize limite) {
        this.maxBytes = limite.toBytes();
    }

    public PdfRecibido recibir(MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío.");
        }
        if (archivo.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño permitido.");
        }
        String nombre = archivo.getOriginalFilename();
        String tipo = archivo.getContentType();
        if (nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith(".pdf")
                || (tipo != null && !tipo.equalsIgnoreCase("application/pdf")
                    && !tipo.equalsIgnoreCase("application/octet-stream"))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Seleccione un archivo PDF.");
        }
        // Validación preliminar. La lectura estructural se hará al extraer el texto.
        try (var entrada = archivo.getInputStream()) {
            String cabecera = new String(entrada.readNBytes(8), StandardCharsets.US_ASCII);
            if (!cabecera.matches("%PDF-[12]\\.[0-9]")) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "El contenido del archivo no tiene una cabecera PDF válida.");
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo.", ex);
        }
        // El nombre es solo informativo: no se utiliza como ruta de almacenamiento.
        String nombreVisible = nombre.replace('\\', '/');
        nombreVisible = nombreVisible.substring(nombreVisible.lastIndexOf('/') + 1);
        return new PdfRecibido(nombreVisible, archivo.getSize(), "RECIBIDO",
                "PDF recibido. La extracción de materiales todavía no está habilitada.");
    }

    public record PdfRecibido(String nombre, long tamanoBytes, String estado, String mensaje) {}
}
