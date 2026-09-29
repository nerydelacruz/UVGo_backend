package com.UVGgo.backend.web.controller.pedidos;

import com.UVGgo.backend.domain.pedidos.Pedido;
import com.UVGgo.backend.domain.pedidos.PedidoDetalle;
import com.UVGgo.backend.domain.service.pedidos.PedidoService;
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
                .map(d -> new PedidoDetalle(null, d.kitId(), d.kitArticuloId(), d.cantidad()))
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
}
