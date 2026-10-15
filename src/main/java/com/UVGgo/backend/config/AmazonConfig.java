package com.UVGgo.backend.config;

import com.amazon.creators.ApiClient;
import com.amazon.creators.api.DefaultApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AmazonProperties.class)
public class AmazonConfig {

    @Bean
    public DefaultApi amazonApi(AmazonProperties props) {
        ApiClient client = new ApiClient();
        client.setCredentialId(props.credentialId());
        client.setCredentialSecret(props.credentialSecret());
        client.setVersion(props.version());
        return new DefaultApi(client);
    }
}
