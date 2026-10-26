# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

UVGo backend: API REST en Spring Boot 4 / Java 21 para que estudiantes de la UVG pidan artículos (catálogo propio + productos externos de SerpApi y BrightData). Código, nombres y mensajes están en español; mantener ese idioma.

## Comandos

```bash
./gradlew bootRun                     # arranca con el perfil de spring.profiles.active (dev por defecto)
./gradlew test                        # suite completa en H2 (perfil test); excluye el tag "postgres"
./gradlew test --tests 'com.UVGgo.backend.pedidos.PedidoApiTests'            # una clase
./gradlew test --tests 'com.UVGgo.backend.ArticuloApiTests.buscaPorNombreYPagina'  # un método
./gradlew postgresTest                # pruebas @Tag("postgres"); exige TEST_DB_URL/USER/PASSWORD de una base exclusiva
docker compose up -d                  # PostgreSQL 17 local en 127.0.0.1:5432 (requiere DB_PASSWORD en .env)
```

- Context path: `/uvgo/api`. Swagger UI: `http://localhost:<puerto>/uvgo/api/swagger-ui.html`.
- Puerto: `dev` = 8070 (base PostgreSQL compartida en Azure), `local` = 8080 (usa `DB_URL`/`DB_USER`/`DB_PASSWORD`).
- Las claves se leen de `.env` (ver `.env.example`) vía `spring.config.import`. `JWT_SECRET` es obligatorio: Base64 de ≥32 bytes (`openssl rand -base64 32`).
- `src/main/resources/application*.properties` está en `.gitignore`: los cambios ahí no llegan al equipo; si se agrega una propiedad nueva, avisar y reflejarla en `.env.example`.
- Perfil `demo` (`DatosDemo`) crea cuentas ficticias admin/estudiante; requiere `DEMO_PASSWORD`.

## Arquitectura

Hay **dos estilos conviviendo** en el mismo paquete raíz `com.UVGgo.backend`; respetar el estilo del módulo que se toque:

1. **Capas (código de David)** — artículos, búsqueda externa y el módulo `/pedidos` original:
   `web/controller` → `domain/service` → `domain/repository` (interfaz) → `persistence/*EntityRepository` (impl) → `persistence/crud` (Spring Data) + `persistence/entity`, con mappers MapStruct en `persistence/mapper`. El dominio expone records (`domain/Articulo`, `domain/pedidos/Pedido`). Rutas públicas sin `/api/v1`. Doc del módulo: `docs/pedidos.md`. Los kits se eliminaron (V9); no reintroducirlos.
2. **Por feature (`auth`, `usuario`, `pedido`)** — entidades JPA usadas directamente por el servicio, DTOs como records anidados (`AuthDtos`, `PedidoDtos`), rutas bajo `/api/v1/**` que exigen JWT.
   - `usuario.Usuario` es abstracta con herencia `SINGLE_TABLE` (`Estudiante`, `Administrador`); `obtenerRol()` alimenta el claim `rol`.
   - El paquete `pedido` (singular) son las **solicitudes** del estudiante (tabla `solicitudes` + `items_pedido`, IDs UUID), distinto del módulo `domain/pedidos` (tabla `pedidos` + `pedido_detalles`, IDs enteros). El bean se llama `solicitudPedidoController` para no chocar con el otro `PedidoController`.
   - Flujo de cotización (V13): el estudiante crea una solicitud con `productos` (cada uno `articuloId` del catálogo, `web` con título/URL **o** `texto` libre, p. ej. un material leído de un PDF (V16); se guarda una copia en `items_pedido`) → el admin `POST /{id}/cotizacion` pone `precioUnitario` por ítem + `cargoServicio` (en GTQ) → `COTIZADO` → el estudiante `POST /{id}/aceptacion` → `CONFIRMADO` (ya es pedido) o rechaza con `/cancelacion`. Ya confirmado, el estudiante sube el comprobante de pago (`PUT /{id}/comprobante`, multipart `archivo` + `version`; PDF/PNG/JPG ≤5 MB, tipo detectado por contenido) y el admin lo descarga (`GET /{id}/comprobante`) y registra el pago (`POST /{id}/pago`) → `PAGADO`. Cada paso envía correo (`CorreosPedido`).

**Seguridad (`config/SeguridadConfig`)**: resource server JWT HS256 firmado por la propia app (issuer `uvgo`). Cada token tiene un `jti` que corresponde a una fila en `sesiones`; el decoder valida en cada request que la sesión esté vigente y que el usuario siga activo con el mismo rol, así que logout = revocar sesión. Autorización por rol con `@PreAuthorize("hasRole('ESTUDIANTE'|'ADMINISTRADOR')")`. Registro solo con correo del dominio `INSTITUTIONAL_DOMAIN`; contraseñas BCrypt con límite de 72 bytes.

**Errores**: lanzar `api.ApiException(HttpStatus, mensaje)`; `api.ErroresApi` convierte todo a `ProblemDetail` (validación → 400, integridad → 409, optimistic lock → 409). Jackson rechaza campos desconocidos (`fail-on-unknown-properties=true`), así que los DTOs deben declarar todo lo que acepten.

**Integraciones externas** (`domain/service`): `BusquedaExternaService` usa SerpApi (Google Shopping); `BrightDataService` importa productos scrapeados a `articulos` (`ResultadoImportacion`).

**Correo (`correo`)**: Azure Communication Services (`azure-communication-email`, con el cliente HTTP del JDK en lugar de Netty). Los servicios publican un `Correo` como evento (`ApplicationEventPublisher`) y `CorreoService` lo envía `@Async` tras el commit; un fallo de Azure solo se registra en el log. Sin `AZURE_EMAIL_CONNECTION_STRING`/`AZURE_EMAIL_SENDER` no envía nada (así corren las pruebas). Textos de solicitudes en `pedido/CorreosPedido`.

**Archivos (`archivo`)**: `AlmacenArchivos` con implementación `AlmacenAzureBlob` (contenedor privado, `AZURE_STORAGE_CONNECTION_STRING`/`AZURE_STORAGE_CONTAINER`); la base solo guarda la ruta del blob. Sin configuración, subir/descargar responde 503. Las pruebas lo sustituyen por un almacén en memoria (`BackendApiTests.AlmacenEnMemoria`). El límite de 5 MB está en `ArchivosConfig`, no en properties.

## Base de datos y Flyway

- Migraciones en `src/main/resources/db/migration`. La base de Azure (perfil `dev`) es **compartida con el equipo** y tiene V4–V8 aplicadas cuyos archivos no están en el repo; por eso `spring.flyway.ignore-migration-patterns=*:missing`.
- Antes de crear una migración, usar el siguiente número libre según `flyway_schema_history` en Azure (no reutilizar números de V4–V8 aunque falten en el repo). Nunca editar una migración ya aplicada.
- Cambios destructivos (DROP, borrar columnas/datos) en la base compartida se consultan antes con el usuario.
- Las pruebas usan H2 en modo PostgreSQL con las mismas migraciones: el SQL debe ser compatible con ambos.
