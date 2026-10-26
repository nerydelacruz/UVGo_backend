package com.UVGgo.backend.pedido;

import com.UVGgo.backend.api.ApiException;
import com.UVGgo.backend.archivo.*;
import com.UVGgo.backend.domain.Articulo;
import com.UVGgo.backend.domain.repository.ArticuloRepository;
import com.UVGgo.backend.usuario.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service("solicitudPedidoService")
@Transactional
public class PedidoService {
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final ArticuloRepository articulos;
    private final AlmacenArchivos almacen;
    private final ApplicationEventPublisher eventos;
    public PedidoService(PedidoRepository pedidos,UsuarioRepository usuarios,ArticuloRepository articulos,
                         AlmacenArchivos almacen,ApplicationEventPublisher eventos) {
        this.pedidos=pedidos; this.usuarios=usuarios; this.articulos=articulos; this.almacen=almacen; this.eventos=eventos;
    }
    public PedidoDtos.Detalle crear(UUID usuario,PedidoDtos.Solicitud s) {
        Usuario u=usuarios.findById(usuario).orElseThrow(ApiException::noEncontrado);
        if (!(u instanceof Estudiante e)) throw new ApiException(HttpStatus.FORBIDDEN,"Solo estudiantes pueden solicitar cotizaciones");
        Pedido p=pedidos.saveAndFlush(new Pedido(e,s,catalogo(s)));
        eventos.publishEvent(CorreosPedido.recibida(p));
        buscarEnCatalogo(p,s);
        return PedidoDtos.Detalle.de(p);
    }
    @Transactional(readOnly=true)
    public PedidoDtos.Detalle consultar(UUID id,UUID usuario,boolean admin) { return PedidoDtos.Detalle.de(visible(id,usuario,admin)); }
    @Transactional(readOnly=true)
    public PedidoDtos.Pagina<PedidoDtos.Detalle> listar(UUID usuario,boolean admin,EstadoPedido estado,String curso,
                                                       LocalDate fechaLimite,int pagina,int tamanio) {
        Specification<Pedido> filtro=(root,query,cb) -> cb.conjunction();
        if (!admin) filtro=filtro.and((root,q,cb) -> cb.equal(root.get("estudiante").get("id"),usuario));
        if (estado!=null) filtro=filtro.and((root,q,cb) -> cb.equal(root.get("estado"),estado));
        if (curso!=null) filtro=filtro.and((root,q,cb) -> cb.equal(cb.lower(root.get("curso")),curso.strip().toLowerCase(java.util.Locale.ROOT)));
        if (fechaLimite!=null) filtro=filtro.and((root,q,cb) -> cb.equal(root.get("fechaLimite"),fechaLimite));
        Page<Pedido> page=pedidos.findAll(filtro,PageRequest.of(pagina,tamanio,Sort.by(Sort.Order.desc("fechaSolicitud"),Sort.Order.desc("id"))));
        return new PedidoDtos.Pagina<>(page.getContent().stream().map(PedidoDtos.Detalle::de).toList(),pagina,tamanio,page.getTotalElements(),page.getTotalPages());
    }
    public PedidoDtos.Detalle editar(UUID id,UUID usuario,PedidoDtos.Edicion r) {
        Pedido p=visible(id,usuario,false); comprobarVersion(p,r.version()); p.actualizar(r.solicitud(),catalogo(r.solicitud()));
        pedidos.flush(); buscarEnCatalogo(p,r.solicitud()); return PedidoDtos.Detalle.de(p);
    }
    public PedidoDtos.Detalle cancelar(UUID id,UUID usuario,boolean admin,long version) {
        Pedido p=visible(id,usuario,admin); comprobarVersion(p,version);
        boolean yaCancelado=p.getEstado()==EstadoPedido.CANCELADO;
        p.cancelar(); pedidos.flush();
        if (!yaCancelado) eventos.publishEvent(CorreosPedido.cancelada(p));
        return PedidoDtos.Detalle.de(p);
    }
    public PedidoDtos.Detalle cotizar(UUID id,PedidoDtos.Cotizacion r) {
        Pedido p=pedidos.findById(id).orElseThrow(ApiException::noEncontrado); comprobarVersion(p,r.version());
        Map<UUID,BigDecimal> precios=new HashMap<>();
        for (var precio:r.precios())
            if (precios.put(precio.itemId(),precio.precioUnitario())!=null)
                throw new ApiException(HttpStatus.BAD_REQUEST,"Producto repetido en la cotización");
        p.cotizar(precios,r.cargoServicio(),r.notas()); pedidos.flush();
        eventos.publishEvent(CorreosPedido.cotizada(p));
        return PedidoDtos.Detalle.de(p);
    }
    public PedidoDtos.Detalle aceptar(UUID id,UUID usuario,long version) {
        Pedido p=visible(id,usuario,false); comprobarVersion(p,version); p.aceptar(); pedidos.flush();
        eventos.publishEvent(CorreosPedido.confirmada(p));
        return PedidoDtos.Detalle.de(p);
    }
    public PedidoDtos.Detalle adjuntarComprobante(UUID id,UUID usuario,long version,MultipartFile archivo) {
        Pedido p=visible(id,usuario,false); comprobarVersion(p,version);
        if (archivo.getSize()>ArchivosConfig.TAMANO_MAXIMO)
            throw new ApiException(HttpStatus.CONTENT_TOO_LARGE,"El archivo supera el tamaño máximo de 5 MB");
        byte[] contenido;
        try { contenido=archivo.getBytes(); }
        catch (IOException e) { throw new ApiException(HttpStatus.BAD_REQUEST,"No se pudo leer el archivo"); }
        if (contenido.length==0) throw new ApiException(HttpStatus.BAD_REQUEST,"El archivo está vacío");
        TipoArchivo tipo=TipoArchivo.detectar(contenido).orElseThrow(() ->
                new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"El comprobante debe ser PDF, PNG o JPG"));
        String ruta="solicitudes/"+p.getId()+"/"+UUID.randomUUID()+"."+tipo.extension();
        String anterior=p.adjuntarComprobante(ruta,nombre(archivo.getOriginalFilename(),tipo),tipo.mime(),contenido.length);
        almacen.guardar(ruta,contenido,tipo.mime());
        // Si la transacción falla se borra el archivo nuevo; si se confirma, el anterior.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int estado) {
                String sobrante=estado==STATUS_COMMITTED ? anterior : ruta;
                if (sobrante!=null) try { almacen.eliminar(sobrante); } catch (RuntimeException ignorada) {}
            }
        });
        pedidos.flush();
        return PedidoDtos.Detalle.de(p);
    }
    @Transactional(readOnly=true)
    public PedidoDtos.Archivo comprobante(UUID id,UUID usuario,boolean admin) {
        Pedido p=visible(id,usuario,admin);
        if (p.getComprobanteRuta()==null) throw ApiException.noEncontrado();
        return new PedidoDtos.Archivo(p.getComprobanteNombre(),p.getComprobanteTipo(),almacen.leer(p.getComprobanteRuta()));
    }
    public PedidoDtos.Detalle registrarPago(UUID id,long version) {
        Pedido p=pedidos.findById(id).orElseThrow(ApiException::noEncontrado); comprobarVersion(p,version);
        p.registrarPago(); pedidos.flush();
        eventos.publishEvent(CorreosPedido.pagada(p));
        return PedidoDtos.Detalle.de(p);
    }
    // Nombre original sin rutas ni caracteres de control, para mostrarlo y descargarlo.
    private static String nombre(String original,TipoArchivo tipo) {
        String limpio=original==null ? "" : original.replaceAll(".*[/\\\\]","").replaceAll("\\p{Cntrl}","").strip();
        if (limpio.length()>255) limpio=limpio.substring(limpio.length()-255);
        return limpio.isEmpty() ? "comprobante."+tipo.extension() : limpio;
    }
    // Los materiales de texto libre se buscan en el catálogo en segundo plano, después de guardar la solicitud.
    private void buscarEnCatalogo(Pedido p,PedidoDtos.Solicitud s) {
        List<SolicitudGuardada.Material> materiales=new ArrayList<>();
        for (int i=0;i<s.productos().size();i++) {
            PedidoDtos.Producto pr=s.productos().get(i);
            if (pr.texto()!=null) materiales.add(new SolicitudGuardada.Material(p.getItems().get(i).getId(),pr.texto(),pr.codigo(),pr.termino()));
        }
        if (materiales.isEmpty()) { p.terminarBusquedaCatalogo(); return; }
        p.iniciarBusquedaCatalogo();
        eventos.publishEvent(new SolicitudGuardada(p.getId(),materiales));
    }
    // Carga los artículos del catálogo que pide la solicitud; todos deben existir.
    private Map<Integer,Articulo> catalogo(PedidoDtos.Solicitud s) {
        Set<Integer> ids=s.productos().stream().map(PedidoDtos.Producto::articuloId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Integer,Articulo> encontrados=new HashMap<>();
        for (Integer articuloId:ids)
            encontrados.put(articuloId,articulos.getById(articuloId).orElseThrow(() ->
                    new ApiException(HttpStatus.BAD_REQUEST,"El artículo "+articuloId+" no existe en el catálogo")));
        return encontrados;
    }
    private Pedido visible(UUID id,UUID usuario,boolean admin) {
        Pedido p=pedidos.findById(id).orElseThrow(ApiException::noEncontrado);
        if (!admin && !p.getEstudiante().getId().equals(usuario)) throw ApiException.noEncontrado();
        return p;
    }
    private void comprobarVersion(Pedido p,long version) {
        if (p.getVersion()!=version) throw ApiException.conflicto("El pedido cambió; consulte su versión actual");
    }
}
