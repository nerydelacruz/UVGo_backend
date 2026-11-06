-- Pilotos (conductores) que pueden entregar un pedido. Por ahora solo existe el que opera el simulador.
CREATE TABLE pilotos (
    id UUID PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);
INSERT INTO pilotos (id, nombre, activo) VALUES ('00000000-0000-4000-8000-000000000001', 'Piloto Oscar', TRUE);
ALTER TABLE solicitudes ADD COLUMN piloto_id UUID REFERENCES pilotos(id);
CREATE INDEX idx_solicitudes_piloto ON solicitudes(piloto_id);
