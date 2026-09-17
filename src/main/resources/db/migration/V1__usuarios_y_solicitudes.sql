CREATE TABLE usuarios (
    id UUID PRIMARY KEY,
    tipo VARCHAR(31) NOT NULL CHECK (tipo IN ('ESTUDIANTE', 'ADMINISTRADOR')),
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(254) NOT NULL UNIQUE,
    telefono VARCHAR(25) NOT NULL,
    correo_contacto VARCHAR(254),
    contrasena_hash VARCHAR(100) NOT NULL,
    activo BOOLEAN NOT NULL,
    correo_verificado BOOLEAN NOT NULL,
    fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL,
    carne VARCHAR(30) UNIQUE,
    carrera VARCHAR(120),
    codigo_administrador VARCHAR(40),
    CHECK (tipo <> 'ESTUDIANTE' OR carne IS NOT NULL)
);
CREATE TABLE sesiones (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    vence_en TIMESTAMP WITH TIME ZONE NOT NULL,
    revocada BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_sesiones_usuario ON sesiones(usuario_id);
CREATE TABLE pedidos (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    estudiante_id UUID NOT NULL REFERENCES usuarios(id),
    curso VARCHAR(150) NOT NULL,
    nombre_catedratico VARCHAR(150) NOT NULL,
    fecha_limite DATE NOT NULL,
    fecha_solicitud TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL,
    estado VARCHAR(30) NOT NULL CHECK (estado IN ('SOLICITADO','COTIZADO','CONFIRMADO','EN_COMPRA','LISTO','ENTREGADO','PAGADO','CANCELADO')),
    observaciones VARCHAR(2000),
    punto_entrega_preferido VARCHAR(150)
);
CREATE INDEX idx_pedidos_estudiante ON pedidos(estudiante_id, fecha_solicitud);
CREATE INDEX idx_pedidos_filtros ON pedidos(estado, fecha_limite);
CREATE TABLE items_pedido (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL REFERENCES pedidos(id),
    posicion INTEGER,
    descripcion VARCHAR(500) NOT NULL,
    cantidad NUMERIC(12,3) NOT NULL CHECK (cantidad > 0),
    unidad_medida VARCHAR(40) NOT NULL
);
