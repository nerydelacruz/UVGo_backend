package com.UVGgo.backend.pedido;

import com.UVGgo.backend.domain.Articulo;
import com.UVGgo.backend.domain.repository.ArticuloRepository;
import java.util.*;
import org.slf4j.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.*;

// Busca en el catálogo los materiales de texto libre de una solicitud, sin hacer esperar al estudiante.
@Service
public class CoincidenciaService {
    private static final Logger log=LoggerFactory.getLogger(CoincidenciaService.class);
    private static final int PAGINA=500;
    private final PedidoRepository pedidos;
    private final ArticuloRepository articulos;
    public CoincidenciaService(PedidoRepository pedidos,ArticuloRepository articulos) {
        this.pedidos=pedidos; this.articulos=articulos;
    }

    @Async @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT) @Transactional(propagation=Propagation.REQUIRES_NEW)
    void alGuardar(SolicitudGuardada evento) {
        try { buscar(evento); }
        // Es un complemento: si falla, los materiales siguen como texto y el administrador los cotiza igual.
        catch (RuntimeException e) { log.error("No se pudo buscar en el catálogo la solicitud {}",evento.pedidoId(),e); }
    }

    void buscar(SolicitudGuardada evento) {
        Pedido p=pedidos.findById(evento.pedidoId()).orElse(null);
        if (p==null || p.getEstado()!=EstadoPedido.SOLICITADO) return;
        // Pase lo que pase se quita la marca de "verificando", para que el administrador pueda abrir la solicitud
        try {
            CoincidenciaArticulos coincidencias=new CoincidenciaArticulos(catalogo());
            Map<UUID,ItemPedido> items=new HashMap<>();
            p.getItems().forEach(i -> items.put(i.getId(),i));
            for (SolicitudGuardada.Material m:evento.materiales()) {
                ItemPedido item=items.get(m.itemId());
                if (item==null || item.getTipo()!=ItemPedido.Tipo.TEXTO) continue;
                coincidencias.buscar(m.texto(),m.codigo(),m.termino()).ifPresent(r -> {
                    if (r.segura()) item.enlazar(r.articulo()); else item.sugerir(r.articulo());
                });
            }
        } finally { p.terminarBusquedaCatalogo(); }
    }

    private List<Articulo> catalogo() {
        List<Articulo> todos=new ArrayList<>();
        for (int pagina=0;;pagina++) {
            List<Articulo> lote=articulos.getAll(pagina,PAGINA);
            todos.addAll(lote);
            if (lote.size()<PAGINA) return todos;
        }
    }
}
