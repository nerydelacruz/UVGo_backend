# Pedidos

Módulo independiente para crear, listar, consultar y cancelar pedidos. Cada pedido
guarda una lista fija de referencias a kits y artículos con sus cantidades.

## Alcance

- No consulta ni modifica kits, artículos ni usuarios.
- `kitId` es el ID del kit. `kitArticuloId` corresponde al campo `id` que devuelve
  la API `/kits/{kitId}/articulos`: identifica la relación kit-artículo, no el ID
  del catálogo compartido.
- Por ahora solo se valida que los IDs sean positivos. No se comprueba que existan
  ni que el artículo pertenezca al kit indicado.
- No guarda nombres o descripciones históricos, precios, datos del cliente ni
  información de entrega. Los IDs por sí solos no permiten reconstruir el
  contenido original si después cambia o se elimina el kit.
- No incluye autenticación ni control de acceso. Listar o consultar no identifica
  al propietario del pedido; esa integración queda pendiente del módulo de usuarios.
- Cotización, historial, notificaciones y entrega quedan para Seguimiento de pedidos.
  Ese módulo deberá usar el mismo estado persistido, no mantener otro en paralelo.

## Probar desde Postman

Con el perfil `dev`, la URL base es `http://localhost:8070/uvgo/api`.
La aplicación necesita una base configurada. La migración `V4__pedidos.sql` crea
únicamente `pedidos` y `pedido_detalles` al arrancar con Flyway; no corrige ni
modifica las tablas de kits. Usa una base de desarrollo antes de aplicar migraciones.

### Crear

`POST /pedidos`, con `Content-Type: application/json`:

```json
{
  "detalles": [
    { "kitId": 15, "kitArticuloId": 32, "cantidad": 2 },
    { "kitId": 15, "kitArticuloId": 33, "cantidad": 1.5 }
  ]
}
```

Devuelve `201` con una estructura como esta (IDs y fechas son asignados por el servidor):

```json
{
  "pedidoId": 1,
  "estado": "PENDIENTE_COTIZACION",
  "fechaCreacion": "2026-09-28T18:00:00Z",
  "fechaCancelacion": null,
  "detalles": [
    { "id": 1, "kitId": 15, "kitArticuloId": 32, "cantidad": 2 },
    { "id": 2, "kitId": 15, "kitArticuloId": 33, "cantidad": 1.5 }
  ]
}
```

La lista debe contener al menos un detalle. Los IDs son enteros positivos y
`cantidad` admite de `0.001` a `999999999.999`, con hasta tres decimales.
No se permite repetir la pareja `kitId` y `kitArticuloId` dentro del mismo pedido.
Un pedido puede contener referencias de varios kits. La creación se guarda como
una sola transacción. Cada POST válido crea un nuevo pedido; todavía no hay
prevención de duplicados por doble clic o reintentos.

### Consultar

- `GET /pedidos`: devuelve una lista con los detalles, ordenada por fecha de
  creación descendente; en caso de empate, por ID descendente. No tiene paginación.
- `GET /pedidos/1`: devuelve un pedido con sus detalles.

### Cancelar

`POST /pedidos/1/cancelar`, sin cuerpo, devuelve `200` con el pedido en estado
`CANCELADO` y su `fechaCancelacion`.

Solo pueden cancelarse pedidos pendientes. Sus detalles se conservan. Repetir la
cancelación devuelve el pedido ya cancelado sin modificar la fecha original.
La actualización comprueba el estado también en la base de datos para evitar
sobrescribir otro estado si dos operaciones coinciden.

No hay rutas para editar detalles, eliminar físicamente pedidos ni asignar estados
arbitrarios.

### Errores

| Código | Situación |
|---|---|
| `400` | Cuerpo inválido, IDs/cantidades inválidos o referencias duplicadas. |
| `404` | Pedido inexistente al consultar o cancelar. |
| `409` | Estado incompatible con la cancelación, incluido un cambio concurrente. |
| `405` | Método no disponible, como PUT o DELETE sobre un pedido. |

## Pruebas automatizadas

Desde la raíz del proyecto en PowerShell:

```powershell
.\gradlew.bat test --tests 'com.UVGgo.backend.pedidos.PedidoApiTests'
```

Las pruebas usan H2 con el perfil `test` y las migraciones Flyway. No usan la base
remota del perfil `dev` ni dependen de crear kits. Comprueban persistencia,
validación, orden, consulta, cancelación y conservación de detalles y fechas.

Para ejecutar la suite completa:

```powershell
.\gradlew.bat test
```

Los fallos de kits detectados antes de incorporar Pedidos corresponden al desajuste
de sus entidades y migraciones. Este módulo no modifica esas funciones.

## Estructura

Flujo: `PedidoController → PedidoService → PedidoRepository → PedidoEntityRepository
→ PedidoCrudRepository`.

Las clases están en subpaquetes `pedidos` dentro de las capas existentes. El DTO de
creación es propio; las respuestas utilizan los records de dominio `Pedido` y
`PedidoDetalle`. MapStruct convierte las entidades persistidas a esos records.
La única clave foránea nueva relaciona los detalles con su pedido.
