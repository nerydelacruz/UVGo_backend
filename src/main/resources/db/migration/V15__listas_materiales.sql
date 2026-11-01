CREATE TABLE listas_materiales (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    nombre VARCHAR(120) NOT NULL,
    creada TIMESTAMP WITH TIME ZONE NOT NULL,
    materiales TEXT NOT NULL
);
CREATE INDEX idx_listas_materiales_usuario_fecha ON listas_materiales(usuario_id, creada DESC);
