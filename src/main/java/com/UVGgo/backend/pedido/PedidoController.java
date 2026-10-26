package com.UVGgo.backend.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@RestController("solicitudPedidoController")
@Validated
@RequestMapping("/api/v1/pedidos")
public class PedidoController {
    private final PedidoService pedidos;
    public PedidoController(PedidoService pedidos) { this.pedidos=pedidos; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ESTUDIANTE')")
    public PedidoDtos.Detalle crear(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody PedidoDtos.Solicitud r) {
        return pedidos.crear(id(jwt),r);
    }
    @GetMapping
    public PedidoDtos.Pagina<PedidoDtos.Detalle> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required=false) EstadoPedido estado, @RequestParam(required=false) @Size(max=150) String curso,
            @RequestParam(required=false) LocalDate fechaLimite,
            @RequestParam(defaultValue="0") @Min(0) int pagina, @RequestParam(defaultValue="20") @Min(1) @Max(100) int tamanio) {
        return pedidos.listar(id(jwt),admin(jwt),estado,curso,fechaLimite,pagina,tamanio);
    }
    @GetMapping("/{pedidoId}")
    public PedidoDtos.Detalle detalle(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID pedidoId) {
        return pedidos.consultar(pedidoId,id(jwt),admin(jwt));
    }
    @PutMapping("/{pedidoId}") @PreAuthorize("hasRole('ESTUDIANTE')")
    public PedidoDtos.Detalle editar(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID pedidoId,@Valid @RequestBody PedidoDtos.Edicion r) {
        return pedidos.editar(pedidoId,id(jwt),r);
    }
    @PostMapping("/{pedidoId}/cancelacion")
    public PedidoDtos.Detalle cancelar(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID pedidoId,@Valid @RequestBody PedidoDtos.Cancelacion r) {
        return pedidos.cancelar(pedidoId,id(jwt),admin(jwt),r.version());
    }
    @PostMapping("/{pedidoId}/cotizacion") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PedidoDtos.Detalle cotizar(@PathVariable UUID pedidoId,@Valid @RequestBody PedidoDtos.Cotizacion r) {
        return pedidos.cotizar(pedidoId,r);
    }
    @PostMapping("/{pedidoId}/aceptacion") @PreAuthorize("hasRole('ESTUDIANTE')")
    public PedidoDtos.Detalle aceptar(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID pedidoId,@Valid @RequestBody PedidoDtos.Aceptacion r) {
        return pedidos.aceptar(pedidoId,id(jwt),r.version());
    }
    // multipart/form-data: campo "archivo" (PDF, PNG o JPG, máx. 5 MB) y la versión actual de la solicitud.
    @PutMapping(value="/{pedidoId}/comprobante",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasRole('ESTUDIANTE')")
    public PedidoDtos.Detalle adjuntarComprobante(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID pedidoId,
                                                  @RequestParam @Min(0) long version,@RequestPart MultipartFile archivo) {
        return pedidos.adjuntarComprobante(pedidoId,id(jwt),version,archivo);
    }
    @GetMapping("/{pedidoId}/comprobante")
    public ResponseEntity<byte[]> comprobante(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID pedidoId) {
        PedidoDtos.Archivo a=pedidos.comprobante(pedidoId,id(jwt),admin(jwt));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(a.tipo()))
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.inline().filename(a.nombre(),StandardCharsets.UTF_8).build().toString())
                .body(a.contenido());
    }
    @PostMapping("/{pedidoId}/pago") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PedidoDtos.Detalle registrarPago(@PathVariable UUID pedidoId,@Valid @RequestBody PedidoDtos.Pago r) {
        return pedidos.registrarPago(pedidoId,r.version());
    }
    private UUID id(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    private boolean admin(Jwt jwt) { return "ADMINISTRADOR".equals(jwt.getClaimAsString("rol")); }
}
