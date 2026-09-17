package com.UVGgo.backend.pedido;

import com.UVGgo.backend.api.ApiException;
import com.UVGgo.backend.usuario.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class PedidoService {
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    public PedidoService(PedidoRepository pedidos,UsuarioRepository usuarios) { this.pedidos=pedidos; this.usuarios=usuarios; }
    public PedidoDtos.Detalle crear(UUID usuario,PedidoDtos.Solicitud s) {
        Usuario u=usuarios.findById(usuario).orElseThrow(ApiException::noEncontrado);
        if (!(u instanceof Estudiante e)) throw new ApiException(HttpStatus.FORBIDDEN,"Solo estudiantes pueden solicitar materiales");
        return PedidoDtos.Detalle.de(pedidos.saveAndFlush(new Pedido(e,s)));
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
        Pedido p=visible(id,usuario,false); comprobarVersion(p,r.version()); p.actualizar(r.solicitud());
        pedidos.flush(); return PedidoDtos.Detalle.de(p);
    }
    public PedidoDtos.Detalle cancelar(UUID id,UUID usuario,boolean admin,long version) {
        Pedido p=visible(id,usuario,admin); comprobarVersion(p,version); p.cancelar();
        pedidos.flush(); return PedidoDtos.Detalle.de(p);
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
