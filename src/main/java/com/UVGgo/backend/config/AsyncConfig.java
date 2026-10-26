package com.UVGgo.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// Habilita @Async (envío de correos fuera del hilo de la petición).
@Configuration
@EnableAsync
public class AsyncConfig {}
