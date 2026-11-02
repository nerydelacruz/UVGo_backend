package com.UVGgo.backend.config;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * El navegador no puede mandar el header Authorization al abrir un WebSocket, así que el JWT viaja en el
 * frame CONNECT de STOMP. Se valida con el mismo JwtDecoder de la API (firma, vencimiento, sesión vigente
 * y usuario activo). Después, solo un administrador puede suscribirse a la ubicación o publicarla, y solo
 * en los destinos de ubicación de un pedido.
 */
public class AutenticacionStomp implements ChannelInterceptor {
    private static final String UUID_RE="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final Pattern SUSCRIPCION=Pattern.compile("^/topic/pedidos/"+UUID_RE+"/ubicacion$");
    private static final Pattern PUBLICACION=Pattern.compile("^/app/pedidos/"+UUID_RE+"/ubicacion$");
    private static final String ADMIN="ROLE_ADMINISTRADOR";

    private final JwtDecoder decoder;
    public AutenticacionStomp(JwtDecoder decoder) { this.decoder=decoder; }

    @Override public Message<?> preSend(Message<?> mensaje,MessageChannel canal) {
        StompHeaderAccessor h=MessageHeaderAccessor.getAccessor(mensaje,StompHeaderAccessor.class);
        if (h==null || h.getCommand()==null) return mensaje;
        switch (h.getCommand()) {
            case CONNECT -> h.setUser(autenticar(h.getFirstNativeHeader("Authorization")));
            case SUBSCRIBE -> exigir(h,SUSCRIPCION);
            case SEND -> exigir(h,PUBLICACION);
            default -> { }
        }
        return mensaje;
    }

    private JwtAuthenticationToken autenticar(String header) {
        if (header==null || !header.startsWith("Bearer ")) throw new MessageDeliveryException("Autenticacion requerida");
        try {
            Jwt jwt=decoder.decode(header.substring(7).strip());
            return new JwtAuthenticationToken(jwt,List.of(new SimpleGrantedAuthority("ROLE_"+jwt.getClaimAsString("rol"))));
        } catch (JwtException e) {
            throw new MessageDeliveryException("Token invalido o vencido");
        }
    }

    private void exigir(StompHeaderAccessor h,Pattern destinoPermitido) {
        if (!(h.getUser() instanceof JwtAuthenticationToken t) || t.getAuthorities().stream().noneMatch(a -> ADMIN.equals(a.getAuthority()))) {
            throw new MessageDeliveryException("Acceso denegado");
        }
        String destino=h.getDestination();
        if (destino==null || !destinoPermitido.matcher(destino).matches()) throw new MessageDeliveryException("Destino no permitido");
    }
}
