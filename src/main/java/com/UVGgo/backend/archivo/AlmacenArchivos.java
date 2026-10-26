package com.UVGgo.backend.archivo;

// Almacenamiento de archivos subidos por los usuarios (p. ej. comprobantes de pago).
public interface AlmacenArchivos {
    void guardar(String ruta, byte[] contenido, String tipo);
    byte[] leer(String ruta);
    void eliminar(String ruta);
}
