package es.alesqui.dbexporter.model;

import lombok.Builder;
import lombok.Data;

/**
 * Información de una columna.
 */
@Data
@Builder
public class ColumnInfo {
    private String nombre;
    private String tipo;
    private Integer longitud;
    private Integer escala;
    private boolean nullable;
    private String valorDefault;
    private String comentario;
}
