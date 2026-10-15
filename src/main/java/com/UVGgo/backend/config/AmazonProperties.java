package com.UVGgo.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amazon.creators")
public record AmazonProperties(
        String credentialId,
        String credentialSecret,
        String version,
        String partnerTag,
        String marketplace) {
}
