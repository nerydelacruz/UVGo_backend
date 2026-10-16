package com.UVGgo.backend.archivo;

import java.util.*;

// Tipos aceptados para comprobantes. Se reconocen por su contenido, no por el nombre ni el Content-Type del cliente.
public enum TipoArchivo {
    PDF("application/pdf","pdf",new byte[]{'%','P','D','F','-'}),
    PNG("image/png","png",new byte[]{(byte)0x89,'P','N','G','\r','\n',0x1A,'\n'}),
    JPEG("image/jpeg","jpg",new byte[]{(byte)0xFF,(byte)0xD8,(byte)0xFF});
    private final String mime, extension;
    private final byte[] firma;
    TipoArchivo(String mime,String extension,byte[] firma) { this.mime=mime; this.extension=extension; this.firma=firma; }
    public String mime() { return mime; }
    public String extension() { return extension; }
    public static Optional<TipoArchivo> detectar(byte[] contenido) {
        return Arrays.stream(values()).filter(t -> contenido.length>=t.firma.length
                && Arrays.equals(contenido,0,t.firma.length,t.firma,0,t.firma.length)).findFirst();
    }
}
