-- Kilómetros que recorrió el piloto con cada pedido. Se acumulan con cada posición GPS que publica
-- (UbicacionService) y alimentan los reportes de envío y de pilotos.
ALTER TABLE solicitudes ADD COLUMN kilometros NUMERIC(10,3) DEFAULT 0 NOT NULL;
