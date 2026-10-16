package com.UVGgo.backend.extraccion;

import java.util.List;
import java.util.concurrent.Semaphore;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/extracciones")
public class ExtraccionMaterialesController {
    private final LectorPdf lector;
    private final ProveedorMateriales proveedor;
    // Límite por instancia: evita análisis simultáneos y presión de memoria.
    private final Semaphore turno = new Semaphore(1);

    public ExtraccionMaterialesController(LectorPdf lector, ProveedorMateriales proveedor) {
        this.lector = lector;
        this.proveedor = proveedor;
    }

    @PostMapping(value = "/materiales", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Resultado extraer(@RequestParam("archivo") MultipartFile archivo) {
        if (!turno.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Hay un análisis en curso. Espere a que termine antes de intentarlo otra vez.");
        }
        try {
            return new Resultado(proveedor.extraer(lector.leer(archivo)),
                    "Revise todos los materiales antes de incorporarlos al kit. Todavía no se ha guardado ningún artículo.");
        } finally { turno.release(); }
    }

    public record Resultado(List<MaterialExtraido> materiales, String mensaje) {}
}
