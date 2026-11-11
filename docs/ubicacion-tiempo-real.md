# Pilotos y ubicación en tiempo real

Un **piloto** (conductor) es el tercer tipo de usuario, junto a estudiante y administrador (`piloto.Piloto extends
usuario.Usuario`, rol `PILOTO`). Inicia sesión como cualquier usuario y usa la **app móvil de UVGo** como unidad de
rastreo: ve los pedidos que tiene asignados y publica la posición de su teléfono, que el administrador ve moverse en el
mapa. Todo el tiempo real va por STOMP sobre WebSocket en `/uvgo/api/ws` (código en `piloto/`, `ubicacion/`,
`config/WebSocketConfig` y `config/AutenticacionStomp`).

```
Admin ──POST /pedidos/{id}/piloto──▶ Backend ──/topic/pilotos/{pilotoId}/asignaciones──▶ App del piloto
App del piloto ──SEND /app/pedidos/{id}/ubicacion──▶ Backend ──/topic/pedidos/{id}/ubicacion──▶ Mapa del admin
```

> El acceso anterior del simulador de Webtrack (`X-Piloto-Id` + `SIMULADOR_CLAVE`) **se eliminó**: ya no existe una clave
> compartida. `SIMULADOR_CLAVE` se puede borrar del `.env`.

## Cuentas de piloto (REST)
| Qué | Ruta |
|---|---|
| Registrarse (público, desde la app móvil) | `POST /api/v1/auth/registro-piloto` con `{"nombre","correo","telefono","contrasena"}` → `201 Perfil` (sin token; cualquier correo) |
| Crear la cuenta (admin) | `POST /api/v1/pilotos` con `{"nombre","correo","telefono","contrasena"}` → `201 {id, nombre}` |
| Pilotos disponibles | `GET /api/v1/pilotos` → `[{id, nombre}]` (solo cuentas activas) |
| Asignar o cambiar | `POST /api/v1/pedidos/{pedidoId}/piloto` con `{"version": n, "pilotoId": "uuid"}` |
| Quitar | `DELETE /api/v1/pedidos/{pedidoId}/piloto?version=n` |

- Hay dos formas de crear un piloto: él mismo desde la app (`/auth/registro-piloto`) o un administrador
  (`POST /pilotos`). En ninguna se exige el dominio institucional (eso solo aplica a estudiantes). Ambas
  validan teléfono y contraseña (12–72 caracteres) y responden 409 si el correo ya existe. Un piloto que se registra solo
  **queda activo de inmediato** y aparece en la lista de disponibles; no hay paso de aprobación.
- El piloto entra con `POST /api/v1/auth/login`; el `Perfil` trae `rol: "PILOTO"` y su `id` **es** el `pilotoId` de las
  rutas y destinos de abajo.
- Solo se asigna a un pedido confirmado que aún no se entrega (`CONFIRMADO`, `PAGADO`, `EN_COMPRA`, `LISTO`); si no, 409.
  `pilotoId` debe ser una cuenta de piloto activa (otro tipo de usuario → 404). Usa la `version` del pedido como el resto
  de operaciones. El detalle del pedido trae `piloto: {id, nombre}` (o `null`).
- Por REST un piloto no ve pedidos (`GET /pedidos` le responde vacío): sus entregas le llegan por WebSocket.

## WebSocket
| Qué | Destino | Quién |
|---|---|---|
| Mis pedidos vigentes (una vez, al suscribirse) | `SUBSCRIBE /app/pilotos/{pilotoId}/pedidos` | el propio piloto |
| Asignaciones nuevas o quitadas | `SUBSCRIBE /topic/pilotos/{pilotoId}/asignaciones` | el propio piloto |
| Publicar mi posición | `SEND /app/pedidos/{pedidoId}/ubicacion` | el piloto **asignado** a ese pedido |
| Ver la posición | `SUBSCRIBE /topic/pedidos/{pedidoId}/ubicacion` | administrador |
| Última posición conocida | `GET /api/v1/pedidos/{pedidoId}/ubicacion` (204 si no hay) | administrador |

Asignación que recibe el piloto: `{pedidoId, curso, destino, estado, asignado, marca}`; `asignado: false` avisa que
el pedido dejó de ser suyo. `destino` es el punto de entrega del estudiante (texto, puede ser `null`).
Posición que publica el piloto: `{"latitud":14.6075,"longitud":-90.488,"rumbo":45}` (`rumbo` opcional, grados de
brújula). Los suscriptores la reciben con `marca` (hora del servidor).

## Autenticación (frame CONNECT de STOMP)
El navegador no puede mandar `Authorization` al abrir un WebSocket, así que el JWT va en el CONNECT, para todos los
roles: `Authorization: Bearer <jwt>`. Se valida con el mismo `JwtDecoder` de la API (firma, vencimiento, sesión vigente,
usuario activo con el mismo rol).

Cada rol solo puede usar sus destinos (`AutenticacionStomp`): un piloto no escucha pedidos ajenos ni la ubicación,
y no publica en pedidos que no tiene asignados (se consulta la base en cada posición, así que si le quitan el pedido
deja de poder publicar). Un administrador no publica posiciones; un estudiante no tiene destinos. La sesión se valida
solo al conectar: si se revoca o vence con el WebSocket ya abierto, sigue hasta que se cierre (el token dura 30 min; la
app debe volver a iniciar sesión y reconectar).

## Configuración
- `CORS_ORIGINS` también rige el origen del WebSocket para navegadores. Una app nativa no manda `Origin` y no lo necesita.
- En Azure App Service hay que activar **Web sockets** (Configuración → General) o la conexión falla solo en producción.

## Base de datos
`V21__Pilotos_como_usuarios` (migración Java en `src/main/java/db/migration`) permite `tipo = 'PILOTO'` en `usuarios` y
agrega `solicitudes.piloto_usuario_id`. La tabla `pilotos` y la columna `solicitudes.piloto_id` de V20 (el "Piloto Oscar"
del simulador) **quedan sin uso**; no se borraron porque es un cambio destructivo en la base compartida. Los pedidos que
tenían asignado a Piloto Oscar aparecen sin piloto.

## Limitaciones
- La última posición vive en memoria (`UbicacionService`): se pierde al reiniciar y no se comparte entre instancias.
  Con varias instancias haría falta un broker externo (RabbitMQ/Redis) o persistir la posición.
- No hay endpoint para desactivar o editar un piloto ni para cambiar su contraseña.
- El token dura 30 minutos y no hay refresh: un piloto en ruta tiene que volver a iniciar sesión al vencer.
- No existe un estado "en ruta" en `EstadoPedido`: el pedido sigue `CONFIRMADO`/`PAGADO` mientras el piloto lo entrega.
