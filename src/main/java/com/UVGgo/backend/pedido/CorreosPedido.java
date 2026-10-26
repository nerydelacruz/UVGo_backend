package com.UVGgo.backend.pedido;

import com.UVGgo.backend.correo.Correo;
import com.UVGgo.backend.usuario.Usuario;
import org.springframework.web.util.HtmlUtils;

// Textos de los correos que recibe el estudiante sobre sus solicitudes de cotización.
final class CorreosPedido {
    private CorreosPedido() {}
    static Correo recibida(Pedido p) {
        return correo(p,"Recibimos tu solicitud de cotización para "+p.getCurso(),
                "Recibimos tu solicitud de cotización para "+p.getCurso()+" ("+p.getItems().size()
                        +" producto(s), fecha límite "+p.getFechaLimite()+"). Te avisaremos cuando esté cotizada.");
    }
    static Correo cotizada(Pedido p) {
        String notas=p.getNotasCotizacion()==null || p.getNotasCotizacion().isBlank() ? "" : " Notas: "+p.getNotasCotizacion();
        return correo(p,"Tu cotización para "+p.getCurso()+" está lista",
                "Tu solicitud para "+p.getCurso()+" fue cotizada por un total de "+PedidoDtos.MONEDA_COTIZACION+" "
                        +p.getTotal().toPlainString()+". Ingresa a UVGo para aceptarla o rechazarla."+notas);
    }
    static Correo confirmada(Pedido p) {
        return correo(p,"Pedido confirmado para "+p.getCurso(),
                "Aceptaste la cotización de "+PedidoDtos.MONEDA_COTIZACION+" "+p.getTotal().toPlainString()
                        +" para "+p.getCurso()+". Tu pedido quedó confirmado y te avisaremos cuando avance.");
    }
    static Correo pagada(Pedido p) {
        return correo(p,"Pago confirmado para "+p.getCurso(),
                "Validamos tu comprobante y registramos el pago de "+PedidoDtos.MONEDA_COTIZACION+" "+p.getTotal().toPlainString()
                        +" para "+p.getCurso()+". Te avisaremos cuando tu pedido avance.");
    }
    static Correo cancelada(Pedido p) {
        return correo(p,"Solicitud de "+p.getCurso()+" cancelada",
                "Tu solicitud de cotización para "+p.getCurso()+" fue cancelada.");
    }
    private static Correo correo(Pedido p,String asunto,String mensaje) {
        Usuario u=p.getEstudiante();
        String para=u.getCorreoContacto()!=null && !u.getCorreoContacto().isBlank() ? u.getCorreoContacto() : u.getCorreo();
        String saludo="Hola "+u.getNombre()+",";
        String pie="Número de solicitud: "+p.getId();
        String texto=saludo+"\n\n"+mensaje+"\n\n"+pie+"\n\nUVGo";
        String html="<p>"+HtmlUtils.htmlEscape(saludo)+"</p><p>"+HtmlUtils.htmlEscape(mensaje)+"</p><p><small>"
                +HtmlUtils.htmlEscape(pie)+"</small></p><p>UVGo</p>";
        return new Correo(para,asunto,texto,html);
    }
}
