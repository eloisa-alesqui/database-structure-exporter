package es.alesqui.dbexporter.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Información de una foreign key.
 */
@Data
@Builder
public class ForeignKeyInfo {
    private String nombre;                    // Nombre de la constraint
    private List<String> columnasOrigen;      // Columnas de la tabla actual
    private String tablaDestino;              // Tabla referenciada
    private List<String> columnasDestino;     // Columnas referenciadas
    private String onDeleteAction;            // CASCADE, RESTRICT, etc.
    private String onUpdateAction;            // CASCADE, RESTRICT, etc.
}

