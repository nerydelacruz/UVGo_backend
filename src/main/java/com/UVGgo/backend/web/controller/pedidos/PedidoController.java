package com.UVGgo.backend.web.controller.pedidos;

import com.UVGgo.backend.domain.pedidos.Pedido;
import com.UVGgo.backend.domain.pedidos.PedidoDetalle;
import com.UVGgo.backend.domain.pedidos.SeguimientoPedido;
import com.UVGgo.backend.domain.service.pedidos.PedidoService;
import com.UVGgo.backend.web.dto.pedidos.ActualizarEstadoRequest;
import com.UVGgo.backend.web.dto.pedidos.CrearPedidoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pedido crear(@Valid @RequestBody CrearPedidoRequest request) {
        return service.crear(request.detalles().stream()
                .map(d -> new PedidoDetalle(null, d.articuloId(), d.cantidad()))
                .toList());
    }

    @GetMapping
    public List<Pedido> listar() {
        return service.listar();
    }

    @GetMapping("/{pedidoId}")
    public Pedido consultar(@PathVariable int pedidoId) {
        return service.consultar(pedidoId);
    }

    @PostMapping("/{pedidoId}/cancelar")
    public Pedido cancelar(@PathVariable int pedidoId) {
        return service.cancelar(pedidoId);
    }

    // Seguimiento (Admin): estado actual y el siguiente al que puede avanzar.
    @GetMapping("/{pedidoId}/estado")
    public SeguimientoPedido consultarEstado(@PathVariable int pedidoId) {
        return service.consultarSeguimiento(pedidoId);
    }

    @PutMapping("/{pedidoId}/estado")
    public Pedido actualizarEstado(@PathVariable int pedidoId,
                                   @Valid @RequestBody ActualizarEstadoRequest request) {
        return service.actualizarEstado(pedidoId, request.estado());
    }
}
