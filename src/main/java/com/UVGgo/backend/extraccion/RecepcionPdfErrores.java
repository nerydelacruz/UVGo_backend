package com.UVGgo.backend.extraccion;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {RecepcionPdfController.class, ExtraccionMaterialesController.class})
public class RecepcionPdfErrores {
    public record ErrorPdf(String mensaje) {}

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorPdf> validacion(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorPdf(ex.getReason()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorPdf> demasiadoGrande(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorPdf("El archivo o la solicitud supera el tamaño permitido (archivo: 5 MB; solicitud: 6 MB)."));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorPdf> faltaArchivo(MissingServletRequestPartException ex) {
        return ResponseEntity.badRequest().body(new ErrorPdf("Adjunte un PDF en el campo 'archivo'."));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorPdf> solicitudInvalida(MultipartException ex) {
        return ResponseEntity.badRequest().body(new ErrorPdf("No se pudo recibir el archivo. Revise la carga e intente de nuevo."));
    }
}
