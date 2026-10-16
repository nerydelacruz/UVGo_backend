package com.UVGgo.backend.archivo;

import com.UVGgo.backend.api.ApiException;
import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.function.Supplier;

// Contenedor privado de Azure Blob Storage; los archivos solo se leen a través de la API.
@Component
public class AlmacenAzureBlob implements AlmacenArchivos {
    private static final Logger log=LoggerFactory.getLogger(AlmacenAzureBlob.class);
    private final BlobContainerClient contenedor;
    private volatile boolean contenedorListo;
    public AlmacenAzureBlob(@Value("${uvgo.almacen.connection-string:${AZURE_STORAGE_CONNECTION_STRING:}}") String conexion,
                            @Value("${uvgo.almacen.contenedor:${AZURE_STORAGE_CONTAINER:comprobantes}}") String contenedor) {
        this.contenedor=conexion.isBlank() ? null : new BlobServiceClientBuilder().connectionString(conexion)
                .httpClient(new JdkHttpClientBuilder().build()).buildClient().getBlobContainerClient(contenedor);
        if (this.contenedor==null) log.warn("Subida de archivos deshabilitada: falta AZURE_STORAGE_CONNECTION_STRING");
    }
    @Override public void guardar(String ruta, byte[] contenido, String tipo) {
        azure(() -> {
            if (!contenedorListo) { contenedor().createIfNotExists(); contenedorListo=true; }
            return contenedor().getBlobClient(ruta).uploadWithResponse(new BlobParallelUploadOptions(BinaryData.fromBytes(contenido))
                    .setHeaders(new BlobHttpHeaders().setContentType(tipo)),null,null);
        });
    }
    @Override public byte[] leer(String ruta) { return azure(() -> contenedor().getBlobClient(ruta).downloadContent().toBytes()); }
    @Override public void eliminar(String ruta) { azure(() -> contenedor().getBlobClient(ruta).deleteIfExists()); }
    private BlobContainerClient contenedor() {
        if (contenedor==null) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Falta configurar AZURE_STORAGE_CONNECTION_STRING");
        return contenedor;
    }
    private <T> T azure(Supplier<T> operacion) {
        try { return operacion.get(); }
        catch (ApiException e) { throw e; }
        catch (RuntimeException e) {
            log.error("Error de Azure Blob Storage",e);
            throw new ApiException(HttpStatus.BAD_GATEWAY,"No se pudo acceder al almacenamiento de archivos");
        }
    }
}
