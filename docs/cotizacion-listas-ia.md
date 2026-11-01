# Cotización de listas de materiales con IA

## Resultado y uso

Una lista revisada y guardada en «Materiales con IA» puede convertirse en una solicitud. El administrador asigna precios mediante el flujo existente y el estudiante consulta su estado desde la lista. La IA propone materiales; el equipo de UVGo revisa disponibilidad y determina los precios.

1. Analizar el PDF, revisar cantidades y unidades, y guardar la lista. También funciona con listas manuales.
2. Abrir la lista guardada; completar curso, catedrático y fecha límite. Entrega y observaciones generales son opcionales.
3. Pulsar «Solicitar cotización de esta lista». La pantalla indica el correo de contacto de la cuenta, o su correo principal si no hay uno alternativo.
4. Usar «Ver solicitud y su estado». El vínculo persiste al recargar y obtiene el detalle por identificador, incluso fuera de la primera página de solicitudes.
5. Como administrador, abrir Solicitudes y «De una lista». Introducir el precio por la unidad indicada, cargo por servicio y notas; enviar la cotización.
6. El estudiante revisa el total en Mis solicitudes y continúa con la aceptación y comprobante existentes.

Cada lista tiene una única solicitud. Reintentar devuelve la misma, incluso si fue cancelada. Para otra solicitud se debe guardar otra lista. No se reemplazan los materiales de la solicitud original.

## API y persistencia

`POST /api/v1/listas-materiales/{id}/solicitud` requiere JWT de estudiante y una lista propia.

```json
{
  "curso": "Química",
  "nombreCatedratico": "Docente",
  "fechaLimite": "2099-12-01",
  "puntoEntregaPreferido": "Campus",
  "observaciones": "Materiales para laboratorio"
}
```

Fecha: hoy o posterior. Curso y catedrático: obligatorios, máximo 150 caracteres cada uno. Entrega: 150; observaciones: 2000. Devuelve `200` con `PedidoDtos.Detalle`, al crear o repetir una petición válida. Sin sesión: `401`; rol no autorizado: `403`; lista ajena o inexistente: `404`; datos inválidos: `400`.

El servidor lee los materiales guardados, no acepta su sustitución en esta petición. Copia nombre, descripción, cantidad decimal, unidad, observaciones y referencia a `items_pedido`, con origen `LISTA`. No inventa precios ni URLs. Cantidades: hasta tres decimales; cada subtotal se redondea a centavos antes de sumar.

`listas_materiales.solicitud_id` guarda el vínculo único con clave foránea. El detalle devuelve `solicitudId`, inicialmente nulo. El bloqueo por propietario, creación y vínculo se ejecutan en una transacción: dos peticiones simultáneas producen una solicitud y un evento de recepción. El endpoint antiguo de edición impide sustituir materiales de lista por productos de catálogo/web.

Se reutilizan `/api/v1/pedidos`, permisos, versionado y estados. El módulo antiguo `/pedidos` no participa.

## Migración y activación

Flyway descubre `src/main/java/db/migration/V19__cotizar_listas_materiales.java`. Agrega vínculo y campos, amplía la unidad a 60 caracteres y permite `LISTA`, conservando `TEXTO`. Busca el CHECK anterior en metadatos porque PostgreSQL y H2 generan nombres distintos cuando no se especifica uno. Conserva los datos existentes.

El 1 de noviembre se consultó el historial compartido en modo de solo lectura: V16–V18 ya estaban aplicadas por el equipo. Se recuperaron sus archivos originales desde `origin/main` y se renumeró nuestra migración Java, aún no aplicada, a V19. Se añadió compatibilidad de lectura y visualización con los materiales `TEXTO` existentes. Esto resuelve el choque de V16; no se ejecutó `repair`, no se cambió el historial ni se desactivó la validación. Desplegar backend y migración antes del frontend.

**No se aplicó V19 a la base compartida durante la corrección.** La prueba de migración cubre V18 → V19 con una lista y un material `TEXTO` previos. Al iniciar normalmente el backend, Flyway aplicará la nueva versión. Si IntelliJ conserva la clase Java V16 anterior, detener la ejecución y reconstruir el proyecto; no borrar tablas ni alterar el historial para resolver clases obsoletas.

Se validaron los archivos contra el historial real con PostgreSQL en modo de solo lectura: sin discrepancias en las versiones aplicadas y únicamente V19 pendiente. La comprobación independiente permite versiones pendientes porque no ejecuta `migrate`; la configuración de validación de la aplicación no se modificó. La reconstrucción normal también pasó después de retirar el atributo de solo lectura de los directorios generados dentro de `build`.

Un segundo intento de arranque detectó `KitEntityRepository.class` obsoleto en `build`, que referenciaba `KitRepository.class` ya eliminado. Compilar incrementalmente no había retirado todos los restos. Se quitó el atributo de solo lectura de los archivos y directorios generados de `build` y se ejecutó `./gradlew clean test` en la salida habitual de IntelliJ. La limpieza eliminó las clases antiguas de kits. Este error ocurre antes de Flyway y no se soluciona restaurando el módulo de kits ni modificando la base.

## Correo

Se reutilizan `CorreosPedido` y `CorreoService`: aviso de recepción al crear y de cotización lista al cotizar. El destinatario es `correoContacto`, o `correo` si el primero está vacío. El aviso incluye identificador y total; el correo existente no contiene enlace directo al frontend.

Configurar `AZURE_EMAIL_CONNECTION_STRING` y `AZURE_EMAIL_SENDER`, o las propiedades equivalentes `uvgo.correo.connection-string` y `uvgo.correo.remitente`. No guardar sus valores en Git.

El envío ocurre de forma asíncrona después del commit. Sin configuración queda deshabilitado; si Azure falla, se registra el error y la cotización permanece guardada. El servicio existente no tiene bandeja persistente ni reintentos garantizados. La solicitud puede consultarse aunque no llegue el correo. Las pruebas comprueban evento, destinatario y contenido; **no certifican entrega real**.

## Verificación

- Backend: 70 pruebas aprobadas en H2 aislado. Incluyen todos los campos, cantidad 2.125, reintentos y concurrencia sin duplicados, permisos, validación, cotización y evento de correo. La migración conserva una lista previa y admite el nuevo origen.
- Frontend: compilación TypeScript/Vite y ESLint. Vite mantiene la advertencia por tamaño del paquete general.
- Edge con API simulada: formulario, error recuperable sin perder datos, envío, vínculo tras recarga, detalle fuera de primera página, cantidades/unidades, móvil sin desbordamiento y precio editable por administrador. Sin errores JavaScript en el recorrido.
- No se realizaron llamadas a Claude, correos reales, commits, pushes ni despliegues.

Pruebas nuevas: `CotizacionListaTests` y `CotizacionListaMigracionTests`. Repetir con `./gradlew test` en backend; `npm run build` y `npm run lint` en frontend. Esta ejecución usó una carpeta temporal de Gradle porque la carpeta habitual de resultados estaba bloqueada.

Para completar la validación del entorno del equipo, crear una solicitud con una cuenta de prueba, cotizarla como administrador, verificar total y recepción real del aviso. Un evento generado no equivale a un mensaje entregado.
