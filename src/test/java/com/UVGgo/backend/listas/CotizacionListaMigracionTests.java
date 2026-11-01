package com.UVGgo.backend.listas;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.assertThat;

class CotizacionListaMigracionTests {
    @Test void actualizaUnaBaseV18ConTextoSinPerderDatosYAdmiteListas() throws Exception {
        String url="jdbc:h2:mem:migracion-lista;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        // H2 conserva expresiones CHECK ligadas a la conexión que creó el esquema.
        try (var fuente = new SingleConnectionDataSource(url,"sa","",true)) {
        Flyway.configure().dataSource(fuente).target("18").load().migrate();
        try (var conexion=fuente.getConnection(); var sql=conexion.createStatement()) {
            sql.execute("""
                INSERT INTO usuarios (id,tipo,nombre,correo,telefono,contrasena_hash,activo,correo_verificado,fecha_registro,carne)
                VALUES ('11111111-1111-1111-1111-111111111111','ESTUDIANTE','Prueba','prueba@example.com','123','hash',true,false,CURRENT_TIMESTAMP,'123')
                """);
            sql.execute("""
                INSERT INTO listas_materiales (id,usuario_id,nombre,creada,materiales)
                VALUES ('22222222-2222-2222-2222-222222222222','11111111-1111-1111-1111-111111111111','Anterior',CURRENT_TIMESTAMP,'[]')
                """);
            sql.execute("""
                INSERT INTO solicitudes (id,estudiante_id,curso,nombre_catedratico,fecha_limite,fecha_solicitud,fecha_actualizacion,estado)
                VALUES ('33333333-3333-3333-3333-333333333333','11111111-1111-1111-1111-111111111111','Curso','Docente',CURRENT_DATE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'SOLICITADO')
                """);
            sql.execute("""
                INSERT INTO items_pedido (id,pedido_id,descripcion,cantidad,tipo)
                VALUES ('55555555-5555-5555-5555-555555555555','33333333-3333-3333-3333-333333333333','Material anterior',1,'TEXTO')
                """);
        }
        Flyway.configure().dataSource(fuente).load().migrate();
        try (var conexion=fuente.getConnection(); var sql=conexion.createStatement()) {
            try (var filas=sql.executeQuery("SELECT nombre,solicitud_id FROM listas_materiales")) {
                assertThat(filas.next()).isTrue(); assertThat(filas.getString(1)).isEqualTo("Anterior");
                assertThat(filas.getObject(2)).isNull();
            }
            try (var filas=sql.executeQuery("SELECT tipo FROM items_pedido WHERE descripcion='Material anterior'")) {
                assertThat(filas.next()).isTrue(); assertThat(filas.getString(1)).isEqualTo("TEXTO");
            }
            sql.execute("""
                INSERT INTO items_pedido (id,pedido_id,descripcion,cantidad,unidad_medida,tipo,nombre_material)
                VALUES ('44444444-4444-4444-4444-444444444444','33333333-3333-3333-3333-333333333333','Detalle',2.125,'litro','LISTA','Alcohol')
                """);
            sql.execute("UPDATE listas_materiales SET solicitud_id='33333333-3333-3333-3333-333333333333'");
        }
        }
    }
}
