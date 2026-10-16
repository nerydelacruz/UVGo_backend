# Integración de Claude para importar materiales en UVGo

12 de octubre de 2026

## Qué se implementó

Se añadió una primera versión de importación de materiales desde PDF. El servidor Java extrae el texto y consulta la API de Claude. El usuario revisa el resultado antes de incorporarlo al kit. Analizar un documento no guarda artículos ni modifica la base de datos.

En el detalle de un **kit base**, el botón **Importar materiales desde PDF** abre la carga y revisión. Permite corregir nombres, descripciones, cantidades y unidades; eliminar filas; añadir materiales omitidos; y consultar el fragmento de origen. Los campos incompletos y las referencias que no coinciden con el texto se señalan para revisión.

Después de confirmar la revisión, **Incorporar materiales** añade las filas al borrador existente. **Guardar cambios** utiliza la operación actual de personalización para crear una copia del kit. La unidad se incorpora a la descripción y la referencia del PDF a las observaciones, sin añadir columnas a la base de datos. La entrada manual sigue disponible.

## Cambios principales en el código

| Parte | Responsabilidad |
| --- | --- |
| `LectorPdf` | Lee el PDF con Apache PDFBox 3.0.8 y limita su contenido. |
| `ProveedorMateriales` y `ClaudeMateriales` | Separan la aplicación del proveedor; envían una solicitud a Messages API y validan el resultado estructurado. |
| `MaterialExtraido` | Representa un borrador independiente del artículo guardado, con valores desconocidos y advertencias. |
| `ExtraccionMaterialesController` | Expone `POST /uvgo/api/extracciones/materiales`, multipart, campo `archivo`. |
| `extraccion.service.ts` | Envía el archivo y valida la forma de la respuesta con Zod. |
| `ImportarMateriales` | Presenta el formulario de revisión y exige confirmación. |
| `CompleteKitList` | Incorpora los materiales al borrador y evita acciones simultáneas durante el guardado. |

Se conserva la recepción anterior `POST /extracciones/pdf`. También se corrigió la reutilización del catálogo: una descripción diferente crea una variante, en lugar de sobrescribir un artículo compartido por otros kits. Se eliminaron importaciones sin uso que impedían compilar y se documentó una excepción de lint necesaria para el tema de Emotion.

## Configuración del servidor

En las variables de entorno de la configuración de ejecución de Java/IntelliJ, definir:

| Variable | Valor |
| --- | --- |
| `ANTHROPIC_API_KEY` | Una clave nueva creada en Claude Console. |
| `UVGO_IA_ENABLED` | `true` |
| `ANTHROPIC_MODEL` | Opcional; predeterminado `claude-haiku-4-5`. |

Reiniciar el backend después de configurar las variables. No usar el prefijo `VITE_` para la clave. Spring Boot no carga automáticamente archivos `.env`; configurar las variables del proceso. Sin habilitación o sin clave, la extracción devuelve un mensaje controlado y el resto del sistema continúa funcionando. La clave compartida en una captura debe revocarse y sustituirse; no se incluyó en ningún archivo.

## Límites y protecciones

- PDF con texto seleccionable, hasta 5 MiB, 20 páginas, 40,000 caracteres y 50 materiales por análisis.
- Rechazo de documentos vacíos, dañados, cifrados o sin texto suficiente. No incluye OCR; un PDF mixto puede requerir revisión manual de sus partes escaneadas.
- Una extracción simultánea por instancia. Máximo 6,000 tokens de salida y espera de lectura de 60 segundos. No hay reintentos automáticos.
- Las respuestas truncadas o inválidas se rechazan. Los datos faltantes no se completan automáticamente; el usuario debe revisarlos.
- No se guardan PDF ni respuestas en tablas. El contenedor puede crear archivos temporales durante la carga; el texto se envía a Anthropic.
- El módulo no añade autenticación. Debe permanecer en un entorno controlado hasta integrar autorización y cuotas por usuario antes de exponer la operación de pago públicamente.

## Verificación y alcance pendiente

Pasaron las 17 pruebas de recepción, extracción y guardado aislado. Verifican lectura de PDF real, contrato HTTP con Claude simulado, validación de respuestas, cantidades, referencias, errores y conservación del kit base. La prueba de navegador pasó con respuestas simuladas: carga, revisión obligatoria, guardado explícito, errores, entrada manual y vista móvil, sin errores de JavaScript. No consumen saldo de Claude ni acreditan la precisión del modelo con documentos reales.

La compilación (`npm run build`) y la revisión estática (`npm run lint`) del frontend pasaron. La compilación conserva una advertencia de tamaño de los paquetes existentes. Las pruebas del módulo se ejecutan con `gradlew.bat test --tests "*extraccion.*" --tests "*RecepcionPdfTests"`. En esta sesión se redirigieron los resultados a una carpeta temporal fuera de OneDrive para evitar bloqueos de archivos.

**Limitación previa detectada:** las migraciones V1–V3 no coinciden con las entidades actuales del catálogo y las relaciones de artículos. Ocho pruebas anteriores del backend ya fallaban antes de esta integración. No se alteraron esas migraciones ni se ejecutaron cambios contra la base real. La prueba de conservación del kit usa una base H2 aislada creada desde las entidades; no valida una migración de PostgreSQL. Es necesario resolver esa diferencia antes de afirmar que una instalación nueva desde cero queda verificada.

La comprobación real con Claude queda pendiente de configurar una clave nueva y analizar documentos de ejemplo. La calidad de extracción siempre requiere revisión humana.

## Referencias

- [Salidas estructuradas de Claude](https://platform.claude.com/docs/en/build-with-claude/structured-outputs)
- [Messages API](https://platform.claude.com/docs/en/api/messages/create)
- [Apache PDFBox](https://pdfbox.apache.org/3.0/migration.html)
