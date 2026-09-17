package com.UVGgo.backend.config;

import com.UVGgo.backend.auth.SesionRepository;
import com.UVGgo.backend.usuario.UsuarioRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.*;

@Configuration
@EnableMethodSecurity
public class SeguridadConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecretKey jwtKey(@Value("${uvgo.security.jwt-secret}") String value) {
        byte[] bytes=Base64.getDecoder().decode(value);
        if (bytes.length<32) throw new IllegalArgumentException("JWT_SECRET requiere al menos 32 bytes aleatorios en Base64");
        return new SecretKeySpec(bytes,"HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder jwtDecoder(SecretKey key, SesionRepository sesiones, UsuarioRepository usuarios) {
        NimbusJwtDecoder decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> vigente=jwt -> {
            try {
                UUID usuario=UUID.fromString(jwt.getSubject());
                boolean ok=sesiones.findById(UUID.fromString(jwt.getId()))
                        .map(s -> s.vigente(usuario,Instant.now())).orElse(false)
                        && usuarios.findById(usuario).map(u -> u.isActivo() && u.obtenerRol().equals(jwt.getClaimAsString("rol"))).orElse(false);
                if (ok) return OAuth2TokenValidatorResult.success();
            } catch (IllegalArgumentException | NullPointerException ignored) { /* Un identificador inválido nunca autentica. */ }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Sesión inválida",null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("uvgo"),vigente));
        return decoder;
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter roles=new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("rol"); roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter=new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return http.csrf(c -> c.disable()).cors(c -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST,"/api/v1/auth/registro","/api/v1/auth/login").permitAll()
                        .requestMatchers("/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request,response,exception) -> {
                            response.setStatus(401); response.setContentType("application/problem+json");
                            response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"Autenticacion requerida\"}");
                        })
                        .accessDeniedHandler((request,response,exception) -> {
                            response.setStatus(403); response.setContentType("application/problem+json");
                            response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,\"detail\":\"Acceso denegado\"}");
                        }))
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((request,response,exception) -> {
                            response.setStatus(401); response.setContentType("application/problem+json");
                            response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"Token invalido o vencido\"}");
                        }))
                .build();
    }
    @Bean CorsConfigurationSource cors(@Value("${uvgo.cors.origins}") String origins) {
        CorsConfiguration c=new CorsConfiguration();
        c.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::strip).toList());
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type")); c.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**",c); return source;
    }
    @Bean OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("UVGo API").version("v1")
                        .description("Bloque inicial: usuarios y solicitudes. Pagos y cotizaciones todavía pendientes."))
                .components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
