package com.UVGgo.backend.archivo;

import com.UVGgo.backend.api.ApiException;
import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;

class AlmacenAzureBlobTests {
    // Construye el cliente real de Azure (sin red): detecta conflictos de dependencias del SDK.
    @Test void construyeClienteDeAzure() {
        String clave=Base64.getEncoder().encodeToString("clave-ficticia-de-prueba".getBytes());
        assertThatCode(() -> new AlmacenAzureBlob("DefaultEndpointsProtocol=https;AccountName=uvgoprueba;AccountKey="+clave
                +";EndpointSuffix=core.windows.net","comprobantes")).doesNotThrowAnyException();
    }
    @Test void sinConfiguracionResponde503() {
        assertThatThrownBy(() -> new AlmacenAzureBlob("","comprobantes").guardar("a.pdf",new byte[]{1},"application/pdf"))
                .isInstanceOf(ApiException.class).hasMessageContaining("AZURE_STORAGE_CONNECTION_STRING");
    }
    @Test void reconoceTiposPorContenido() {
        assertThat(TipoArchivo.detectar("%PDF-1.7".getBytes())).contains(TipoArchivo.PDF);
        assertThat(TipoArchivo.detectar(new byte[]{(byte)0xFF,(byte)0xD8,(byte)0xFF,0})).contains(TipoArchivo.JPEG);
        assertThat(TipoArchivo.detectar("<html>".getBytes())).isEmpty();
        assertThat(TipoArchivo.detectar(new byte[]{'%'})).isEmpty();
    }
}
