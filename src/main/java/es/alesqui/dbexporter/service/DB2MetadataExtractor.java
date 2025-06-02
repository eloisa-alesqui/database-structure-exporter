package es.alesqui.dbexporter.service;

import es.alesqui.dbexporter.model.ColumnInfo;
import es.alesqui.dbexporter.model.ForeignKeyInfo;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extractor de metadatos para DB2.
 */
@Component("db2Extractor")
@RequiredArgsConstructor
public class DB2MetadataExtractor implements DatabaseMetadataExtractor {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    public List<String> getTables(String schema) {
        String sql = "SELECT TABNAME FROM SYSCAT.TABLES " +
                    "WHERE TABSCHEMA = ? AND TYPE = 'T' ORDER BY TABNAME";
        
        return jdbcTemplate.query(sql, 
            new Object[]{schema.toUpperCase()},
            (rs, rowNum) -> rs.getString("TABNAME").trim()
        );
    }
    
    @Override
    public String getTableComment(String schema, String tableName) {
        String sql = "SELECT REMARKS FROM SYSCAT.TABLES " +
            "WHERE TABSCHEMA = ? AND TABNAME = ?";
        
        try {
            String comment = jdbcTemplate.queryForObject(sql, String.class, 
                schema.toUpperCase(), tableName.toUpperCase());
            
            return (comment != null && !comment.trim().isEmpty()) ? comment.trim() : null;
            
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public List<ColumnInfo> getColumns(String schema, String tableName) {
        String sql = "SELECT COLNAME, TYPENAME, LENGTH, SCALE, NULLS, DEFAULT, REMARKS " +
                    "FROM SYSCAT.COLUMNS " +
                    "WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY COLNO";
        
        return jdbcTemplate.query(sql,
            new Object[]{schema.toUpperCase(), tableName},
            (rs, rowNum) -> ColumnInfo.builder()
                .nombre(rs.getString("COLNAME").trim())
                .tipo(rs.getString("TYPENAME").trim())
                .longitud(rs.getInt("LENGTH"))
                .escala(rs.getInt("SCALE"))
                .nullable("Y".equals(rs.getString("NULLS")))
                .valorDefault(rs.getString("DEFAULT"))
                .comentario(rs.getString("REMARKS"))
                .build()
        );
    }
    
    @Override
    public List<String> getPrimaryKeyColumns(String schema, String tableName) {
        String sql = "SELECT KC.COLNAME " +
                    "FROM SYSCAT.KEYCOLUSE KC " +
                    "JOIN SYSCAT.TABCONST C ON KC.CONSTNAME = C.CONSTNAME " +
                    "  AND KC.TABSCHEMA = C.TABSCHEMA " +
                    "  AND KC.TABNAME = C.TABNAME " +
                    "WHERE C.TABSCHEMA = ? AND C.TABNAME = ? AND C.TYPE = 'P' " +
                    "ORDER BY KC.COLSEQ";
        
        return jdbcTemplate.query(sql,
            new Object[]{schema.toUpperCase(), tableName},
            (rs, rowNum) -> rs.getString("COLNAME").trim()
        );
    }
    
    @Override
    public List<ForeignKeyInfo> getForeignKeys(String schema, String tableName) {
        String sql = "SELECT " +
                    "    R.CONSTNAME, " +
                    "    R.REFTABNAME, " +
                    "    R.DELETERULE, " +
                    "    R.UPDATERULE, " +
                    "    KC1.COLNAME AS FK_COLUMN, " +
                    "    KC2.COLNAME AS REF_COLUMN, " +
                    "    KC1.COLSEQ " +
                    "FROM SYSCAT.REFERENCES R " +
                    "JOIN SYSCAT.KEYCOLUSE KC1 ON R.CONSTNAME = KC1.CONSTNAME " +
                    "    AND R.TABSCHEMA = KC1.TABSCHEMA " +
                    "    AND R.TABNAME = KC1.TABNAME " +
                    "JOIN SYSCAT.KEYCOLUSE KC2 ON R.REFKEYNAME = KC2.CONSTNAME " +
                    "    AND R.REFTABSCHEMA = KC2.TABSCHEMA " +
                    "    AND R.REFTABNAME = KC2.TABNAME " +
                    "    AND KC1.COLSEQ = KC2.COLSEQ " +
                    "WHERE R.TABSCHEMA = ? AND R.TABNAME = ? " +
                    "ORDER BY R.CONSTNAME, KC1.COLSEQ";

        // Mapa para agrupar columnas por FK
        Map<String, ForeignKeyData> fkMap = new HashMap<>();
        
        jdbcTemplate.query(sql, new Object[]{schema.toUpperCase(), tableName}, rs -> {
            String fkName = rs.getString("CONSTNAME").trim();
            
            ForeignKeyData fkData = fkMap.computeIfAbsent(fkName, k -> new ForeignKeyData());
            fkData.nombre = fkName;
            fkData.tablaDestino = rs.getString("REFTABNAME").trim();
            fkData.onDeleteAction = mapDeleteRule(rs.getString("DELETERULE"));
            fkData.onUpdateAction = mapUpdateRule(rs.getString("UPDATERULE"));
            fkData.columnasOrigen.add(rs.getString("FK_COLUMN").trim());
            fkData.columnasDestino.add(rs.getString("REF_COLUMN").trim());
        });
        
        // Convertir a ForeignKeyInfo
        return fkMap.values().stream()
            .map(fkData -> ForeignKeyInfo.builder()
                .nombre(fkData.nombre)
                .tablaDestino(fkData.tablaDestino)
                .onDeleteAction(fkData.onDeleteAction)
                .onUpdateAction(fkData.onUpdateAction)
                .columnasOrigen(fkData.columnasOrigen)
                .columnasDestino(fkData.columnasDestino)
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Clase auxiliar para agrupar datos de FK durante la consulta.
     */
    private static class ForeignKeyData {
        String nombre;
        String tablaDestino;
        String onDeleteAction;
        String onUpdateAction;
        List<String> columnasOrigen = new ArrayList<>();
        List<String> columnasDestino = new ArrayList<>();
    }

    private String mapDeleteRule(String rule) {
        switch (rule) {
            case "C": return "CASCADE";
            case "N": return "NO ACTION";
            case "R": return "RESTRICT";
            case "A": return "SET NULL";
            default: return "NO ACTION";
        }
    }

    private String mapUpdateRule(String rule) {
        switch (rule) {
            case "A": return "NO ACTION";
            case "R": return "RESTRICT";
            default: return "NO ACTION";
        }
    }
    
    @Override
    public String getPrimaryKeyName(String schema, String tableName) {
        String sql = "SELECT CONSTNAME FROM SYSCAT.TABCONST " +
                    "WHERE TABSCHEMA = ? AND TABNAME = ? AND TYPE = 'P'";
        
        List<String> results = jdbcTemplate.query(sql,
            new Object[]{schema.toUpperCase(), tableName},
            (rs, rowNum) -> rs.getString("CONSTNAME").trim()
        );
        
        return results.isEmpty() ? null : results.get(0);
    }
    
    @Override
    public String formatColumnType(ColumnInfo col) {
        switch (col.getTipo()) {
            case "CHARACTER":
                return "CHARACTER(" + col.getLongitud() + ")";
            case "VARCHAR":
                return "VARCHAR(" + col.getLongitud() + ")";
            case "DECIMAL":
                return col.getEscala() > 0 
                    ? "DECIMAL(" + col.getLongitud() + "," + col.getEscala() + ")"
                    : "DECIMAL(" + col.getLongitud() + ",0)";
            default:
                return col.getTipo();
        }
    }
    
    @Override
    public String formatDefault(String defaultValue, String tipo) {
        String valor = defaultValue.trim();
        
        // Casos especiales DB2
        if (valor.toUpperCase().matches("CURRENT (DATE|TIMESTAMP|TIME)")) {
            return " DEFAULT " + valor.toUpperCase();
        }
        
        if (("CHARACTER".equals(tipo) || "VARCHAR".equals(tipo)) && "''".equals(valor)) {
            return " DEFAULT ''";
        }
        
        if (tipo.matches("DECIMAL|INTEGER|SMALLINT|BIGINT") && isNumeric(valor)) {
            return " DEFAULT " + valor;
        }
        
        if (valor.startsWith("'") && valor.endsWith("'")) {
            return " DEFAULT " + valor;
        }
        
        return " DEFAULT " + valor;
    }
    
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
