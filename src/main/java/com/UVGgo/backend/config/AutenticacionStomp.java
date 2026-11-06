package com.UVGgo.backend.config;

import com.UVGgo.backend.piloto.AccesoPiloto;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Autenticación y autorización del WebSocket. El navegador no puede mandar Authorization al abrir un WebSocket,
 * así que las credenciales viajan en el frame CONNECT de STOMP:
 *  - Administrador: Authorization: Bearer (el mismo JWT de la API; firma, vencimiento, sesión y usuario activo).
 *  - Piloto (simulador): X-Piloto-Id y X-Piloto-Clave (la clave SIMULADOR_CLAVE del servidor).
 * Luego, cada rol solo puede usar sus destinos:
 *  - Administrador: escuchar /topic/pedidos/{id}/ubicacion.
 *  - Piloto: escuchar sus asignaciones (/app/pilotos/{suId}/pedidos y /topic/pilotos/{suId}/asignaciones) y publicar
 *    en /app/pedidos/{id}/ubicacion solo de los pedidos que tiene asignados.
 */
public class AutenticacionStomp implements ChannelInterceptor {
    private static final String UUID_RE="([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})";
    private static final Pattern UBICACION_ESCUCHA=Pattern.compile("^/topic/pedidos/"+UUID_RE+"/ubicacion$");
    private static final Pattern UBICACION_PUBLICA=Pattern.compile("^/app/pedidos/"+UUID_RE+"/ubicacion$");
    private static final Pattern PILOTO_PEDIDOS=Pattern.compile("^/app/pilotos/"+UUID_RE+"/pedidos$");
    private static final Pattern PILOTO_ASIGNACIONES=Pattern.compile("^/topic/pilotos/"+UUID_RE+"/asignaciones$");
    private static final String ADMIN="ROLE_ADMINISTRADOR", PILOTO="ROLE_PILOTO";

    private final JwtDecoder decoder;
    private final AccesoPiloto pilotos;
    public AutenticacionStomp(JwtDecoder decoder,AccesoPiloto pilotos) { this.decoder=decoder; this.pilotos=pilotos; }

    @Override public Message<?> preSend(Message<?> mensaje,MessageChannel canal) {
        StompHeaderAccessor h=MessageHeaderAccessor.getAccessor(mensaje,StompHeaderAccessor.class);
        if (h==null || h.getCommand()==null) return mensaje;
        switch (h.getCommand()) {
            case CONNECT -> h.setUser(autenticar(h));
            case SUBSCRIBE -> autorizarSuscripcion(h);
            case SEND -> autorizarPublicacion(h);
            default -> { }
        }
        return mensaje;
    }

    private Principal autenticar(StompHeaderAccessor h) {
        String bearer=h.getFirstNativeHeader("Authorization");
        if (bearer!=null) return autenticarAdministrador(bearer);
        String pilotoId=h.getFirstNativeHeader("X-Piloto-Id");
        if (pilotoId!=null) {
            Optional<UUID> id=pilotos.autenticar(pilotoId,h.getFirstNativeHeader("X-Piloto-Clave"));
            if (id.isEmpty()) throw new MessageDeliveryException("Credenciales de piloto invalidas");
            return UsernamePasswordAuthenticationToken.authenticated(id.get().toString(),null,List.of(new SimpleGrantedAuthority(PILOTO)));
        }
        throw new MessageDeliveryException("Autenticacion requerida");
    }

    private Principal autenticarAdministrador(String header) {
        if (!header.startsWith("Bearer ")) throw new MessageDeliveryException("Autenticacion requerida");
        try {
            Jwt jwt=decoder.decode(header.substring(7).strip());
            return new JwtAuthenticationToken(jwt,List.of(new SimpleGrantedAuthority("ROLE_"+jwt.getClaimAsString("rol"))));
        } catch (JwtException e) {
            throw new MessageDeliveryException("Token invalido o vencido");
        }
    }

    private void autorizarSuscripcion(StompHeaderAccessor h) {
        String destino=h.getDestination();
        if (destino==null) throw new MessageDeliveryException("Destino no permitido");
        if (tiene(h,ADMIN) && UBICACION_ESCUCHA.matcher(destino).matches()) return;
        if (tiene(h,PILOTO) && (esDelPiloto(PILOTO_PEDIDOS,destino,h) || esDelPiloto(PILOTO_ASIGNACIONES,destino,h))) return;
        throw new MessageDeliveryException(tieneAlguno(h) ? "Destino no permitido" : "Acceso denegado");
    }

    private void autorizarPublicacion(StompHeaderAccessor h) {
        String destino=h.getDestination();
        Matcher m=destino==null ? null : UBICACION_PUBLICA.matcher(destino);
        if (!tiene(h,PILOTO) || m==null || !m.matches()) throw new MessageDeliveryException("Acceso denegado");
        // La base se consulta en cada posición: si le quitan el pedido a mitad del recorrido, deja de poder publicar
        if (!pilotos.asignado(UUID.fromString(h.getUser().getName()),UUID.fromString(m.group(1))))
            throw new MessageDeliveryException("El pedido no esta asignado a este piloto");
    }

    // El piloto solo puede pedir lo suyo: el id del destino debe ser el de su propia credencial
    private boolean esDelPiloto(Pattern patron,String destino,StompHeaderAccessor h) {
        Matcher m=patron.matcher(destino);
        return m.matches() && m.group(1).equalsIgnoreCase(h.getUser().getName());
    }

    private boolean tiene(StompHeaderAccessor h,String rol) {
        return h.getUser() instanceof org.springframework.security.core.Authentication a
                && a.getAuthorities().stream().anyMatch(x -> rol.equals(x.getAuthority()));
    }
    private boolean tieneAlguno(StompHeaderAccessor h) { return tiene(h,ADMIN) || tiene(h,PILOTO); }
}
