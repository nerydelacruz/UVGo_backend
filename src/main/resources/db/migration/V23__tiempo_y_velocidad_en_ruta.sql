-- Tiempo en movimiento y velocidad máxima del piloto en cada pedido. Con los kilómetros (V22) dan la
-- velocidad promedio del recorrido en los reportes.
ALTER TABLE solicitudes ADD COLUMN segundos_en_ruta BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE solicitudes ADD COLUMN velocidad_maxima NUMERIC(6,2);
