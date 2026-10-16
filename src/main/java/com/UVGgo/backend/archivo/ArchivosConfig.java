package com.UVGgo.backend.archivo;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.*;

// Límite de subida en código (no en application.properties, que no se versiona).
@Configuration
public class ArchivosConfig {
    public static final long TAMANO_MAXIMO=5L*1024*1024;
    @Bean MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement("",TAMANO_MAXIMO,TAMANO_MAXIMO+1024*1024,0);
    }
}
