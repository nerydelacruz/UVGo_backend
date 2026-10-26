-- Comprobante de pago de una solicitud confirmada. El archivo se guarda en Azure Blob Storage;
-- aquí solo su ruta dentro del contenedor y sus datos. El administrador lo valida y marca PAGADO.
ALTER TABLE solicitudes ADD COLUMN comprobante_ruta VARCHAR(300);
ALTER TABLE solicitudes ADD COLUMN comprobante_nombre VARCHAR(255);
ALTER TABLE solicitudes ADD COLUMN comprobante_tipo VARCHAR(100);
ALTER TABLE solicitudes ADD COLUMN comprobante_tamano BIGINT;
ALTER TABLE solicitudes ADD COLUMN fecha_comprobante TIMESTAMP WITH TIME ZONE;
ALTER TABLE solicitudes ADD COLUMN fecha_pago TIMESTAMP WITH TIME ZONE;
