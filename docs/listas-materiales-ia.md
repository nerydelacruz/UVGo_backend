# Listas de materiales independientes

> Ampliación: las listas guardadas ya pueden generar una solicitud. Ver
> [Cotización de listas con IA](cotizacion-listas-ia.md) para el flujo, API,
> migración y condiciones del aviso por correo. El alcance descrito abajo
> corresponde a la primera etapa de extracción y guardado.

La página de kits fue retirada en la navegación nueva. La importación ahora vive en
`/materiales-ia`, accesible desde Inicio y desde el menú de usuario «Materiales con IA».
Usa el inicio de sesión existente.

## Qué cambió

- Se recuperó el lector PDF y la integración con Claude del trabajo anterior, sin
  recuperar las tablas, modelos ni pantallas de kits eliminadas por el equipo.
- La página permite subir un PDF, corregir los materiales, completar información
  faltante, agregar o quitar filas y confirmar la revisión antes de guardar.
- Cada lista tiene nombre y fecha. El historial permite volver a consultar sus
  materiales después de recargar o iniciar sesión de nuevo.
- El guardado es independiente de kits, inventario, cotizaciones y pedidos.
- Cantidad y unidad se guardan separadas; también se conservan observaciones y
  referencias al texto del PDF. No se almacena el archivo PDF original.
- Los borradores sin guardar permanecen únicamente en la página. Se pierden al
  salir o recargar. Un análisis nuevo pide confirmación antes de reemplazarlos.

## Backend y base de datos

La migración `V15__listas_materiales.sql` crea una tabla nueva con referencia al
usuario y un índice por usuario/fecha. No modifica tablas existentes. Las listas
se consultan por propietario tomado del JWT, nunca de un identificador enviado
por el formulario. Otra cuenta recibe 404 al intentar abrir una lista ajena.

Rutas relativas a `/uvgo/api`:

- `POST /api/v1/extracciones/materiales`: PDF multipart, campo `archivo`.
- `POST /api/v1/listas-materiales`: nombre, materiales y `revisada: true`.
- `GET /api/v1/listas-materiales?pagina=0`: historial de 20 listas por página.
- `GET /api/v1/listas-materiales/{id}`: detalle de una lista propia.

Todas requieren sesión. La extracción no guarda automáticamente. El servidor
valida de 1 a 50 materiales, campos obligatorios, longitudes y cantidades positivas
con hasta tres decimales. Los límites del PDF siguen siendo 5 MB y 20 páginas con
texto seleccionable; no se añadió reconocimiento de fotografías ni escaneos.

## Configuración

Se conservan `UVGO_IA_ENABLED=true`, `ANTHROPIC_API_KEY` y, opcionalmente,
`ANTHROPIC_MODEL` (por defecto `claude-haiku-4-5`) únicamente en el servidor.
La configuración actual de la aplicación también requiere su base de datos y
`JWT_SECRET`. No publicar claves ni copiar variables del servidor al frontend.

Al arrancar el backend, Flyway debe aplicar V15 sobre una base con las migraciones
actuales del equipo. No se ejecutó esta migración contra la base compartida durante
el desarrollo. La base de pruebas es temporal e independiente.

## Comprobación manual

1. Iniciar sesión y abrir «Materiales con IA» desde Inicio.
2. Subir un PDF y pulsar «Analizar con Claude».
3. Verificar que no aparezca en el historial antes de guardarlo.
4. Completar los datos, nombrar la lista, marcar la revisión y guardar.
5. Recargar y abrir la lista del historial: deben conservarse cantidades y unidades.
6. Iniciar sesión con otra cuenta: la lista anterior no debe aparecer.

No se realizaron llamadas pagadas a Claude durante las pruebas de esta adaptación.
Las pruebas automatizadas de extracción simulan al proveedor.

## Validación realizada

- Frontend: `npm.cmd run build` y `npm.cmd run lint`, ambos aprobados. Vite mantiene
  una advertencia sobre el tamaño del paquete general de la aplicación.
- Backend: 11 pruebas aprobadas: 7 de interpretación de Claude y 4 de integración
  de listas, autenticación, aislamiento entre cuentas y extracción de un PDF real
  con respuesta del proveedor simulada. Se usó H2 temporal, sin tocar PostgreSQL.
- Navegador: carga, campos obligatorios, confirmación de revisión, guardado explícito,
  consulta tras recarga, error de Claude, alternativa manual y ancho móvil comprobados
  con respuestas de API simuladas. Sin errores JavaScript durante ese recorrido.
- Las pruebas no sustituyen la comprobación final con el backend conectado a la base
  del equipo y la clave real. La migración V15 no se aplicó a la base compartida.

Los cambios quedaron locales en la rama `feat/listas-materiales-ia` de ambos
repositorios, partiendo de las versiones actuales de `origin/main`. No se hicieron
commits, pushes ni modificaciones de los PR anteriores.
