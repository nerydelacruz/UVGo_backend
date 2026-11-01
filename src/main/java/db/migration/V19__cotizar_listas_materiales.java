package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.sql.SQLException;
import java.util.ArrayList;

/** Conserva los datos existentes y admite materiales sin catálogo ni enlace web. */
public class V19__cotizar_listas_materiales extends BaseJavaMigration {
    @Override public void migrate(Context context) throws SQLException {
        var conexion=context.getConnection();
        // V13 no nombró el CHECK: PostgreSQL y H2 le asignan nombres distintos.
        var restricciones=new ArrayList<String>();
        try (var consulta=conexion.prepareStatement("""
                SELECT tc.constraint_name, cc.check_clause
                FROM information_schema.table_constraints tc
                JOIN information_schema.check_constraints cc
                  ON tc.constraint_name=cc.constraint_name AND tc.constraint_schema=cc.constraint_schema
                WHERE LOWER(tc.table_name)='items_pedido' AND tc.constraint_schema=?
                """)) {
            consulta.setString(1,conexion.getSchema());
            try (var resultado=consulta.executeQuery()) {
                while (resultado.next()) {
                    String clausula=resultado.getString(2).toLowerCase(java.util.Locale.ROOT);
                    if (clausula.contains("tipo") && clausula.contains("catalogo") && clausula.contains("web"))
                        restricciones.add(resultado.getString(1));
                }
            }
        }
        try (var sql=conexion.createStatement()) {
            sql.execute("ALTER TABLE listas_materiales ADD COLUMN solicitud_id UUID UNIQUE REFERENCES solicitudes(id)");
            for (String nombre:restricciones)
                sql.execute("ALTER TABLE items_pedido DROP CONSTRAINT \""+nombre.replace("\"","\"\"")+"\"");
            sql.execute("ALTER TABLE items_pedido ADD CONSTRAINT items_pedido_tipo_lista_check CHECK (tipo IN ('CATALOGO','WEB','TEXTO','LISTA'))");
            sql.execute("ALTER TABLE items_pedido ALTER COLUMN unidad_medida TYPE VARCHAR(60)");
            sql.execute("ALTER TABLE items_pedido ADD COLUMN nombre_material VARCHAR(120)");
            sql.execute("ALTER TABLE items_pedido ADD COLUMN observaciones_material VARCHAR(1200)");
            sql.execute("ALTER TABLE items_pedido ADD COLUMN referencia_material VARCHAR(500)");
        }
    }
}
