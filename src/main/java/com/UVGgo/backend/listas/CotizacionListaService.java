package com.UVGgo.backend.listas;

import com.UVGgo.backend.api.ApiException;
import com.UVGgo.backend.pedido.PedidoDtos;
import com.UVGgo.backend.pedido.PedidoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;

@Service
public class CotizacionListaService {
    private final ListaMaterialesRepository listas;
    private final PedidoService pedidos;
    private final ObjectMapper json;

    public CotizacionListaService(ListaMaterialesRepository listas, PedidoService pedidos, ObjectMapper json) {
        this.listas=listas; this.pedidos=pedidos; this.json=json;
    }

    // Bloqueo y vínculo en la misma transacción: dos envíos simultáneos producen una sola solicitud.
    @Transactional
    public PedidoDtos.Detalle solicitar(UUID listaId, UUID usuario, PedidoDtos.DatosLista datos) {
        var lista=listas.bloquearPorPropietario(listaId,usuario).orElseThrow(ApiException::noEncontrado);
        if (lista.getSolicitudId()!=null) return pedidos.consultar(lista.getSolicitudId(),usuario,false);
        var materiales=json.readValue(lista.getMateriales(),new TypeReference<List<ListaMaterialesDtos.Material>>() {});
        var copia=materiales.stream().map(m -> new PedidoDtos.MaterialLibre(m.nombre(),m.descripcion(),
                m.cantidad(),m.unidad(),m.observaciones(),m.referencia())).toList();
        var solicitud=pedidos.crearDesdeLista(usuario,datos,copia);
        lista.vincularSolicitud(solicitud.id());
        return solicitud;
    }
}
