package es.alesqui.dbexporter.service;

import es.alesqui.dbexporter.model.ColumnInfo;
import es.alesqui.dbexporter.model.ForeignKeyInfo;

import java.util.List;

/**
 * Interface para extraer metadatos de diferentes tipos de base de datos.
 */
public interface DatabaseMetadataExtractor {
    
    /**
     * Obtiene la lista de tablas del esquema.
     */
    List<String> getTables(String schema);
    
    /**
     * Obtiene información detallada de las columnas de una tabla.
     */
    List<ColumnInfo> getColumns(String schema, String tableName);
    
    /**
     * Obtiene las columnas que forman la clave primaria.
     */
    List<String> getPrimaryKeyColumns(String schema, String tableName);
    
    /**
     * Obtiene las foreign keys de una tabla.
     */
    List<ForeignKeyInfo> getForeignKeys(String schema, String tableName);
    
    /**
     * Obtiene el nombre de la constraint de clave primaria.
     */
    String getPrimaryKeyName(String schema, String tableName);
    
    /**
     * Formatea el tipo de dato específico de cada BD.
     */
    String formatColumnType(ColumnInfo columnInfo);
    
    /**
     * Formatea el valor por defecto específico de cada BD.
     */
    String formatDefault(String defaultValue, String dataType);
}
