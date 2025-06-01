package es.alesqui.dbexporter.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Información de una tabla.
 */
@Data
@Builder
public class TableInfo {
    private String nombre;
    private String esquema;
    private List<ColumnInfo> columnas;
    private List<String> primaryKeyColumns;
    private String primaryKeyName;
    private List<ForeignKeyInfo> foreignKeys;
}

