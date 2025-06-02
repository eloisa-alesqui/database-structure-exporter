package es.alesqui.dbexporter.service;

import es.alesqui.dbexporter.model.ColumnInfo;
import es.alesqui.dbexporter.model.ForeignKeyInfo;
import es.alesqui.dbexporter.model.TableInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para exportar la estructura de tablas a archivos de texto.
 * Genera archivos en formato descriptivo optimizado para NotebookLM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableExportService {
    
    @Qualifier("db2Extractor") // Cambiar según la BD: db2Extractor, postgresExtractor, etc.
    private final DatabaseMetadataExtractor metadataExtractor;
    
    @Value("${app.schema}")
    private String schema;
    
    @Value("${app.output.directory}")
    private String outputDirectory;
    
    @Value("${app.include.foreign.keys:true}")
    private boolean includeForeignKeys;
    
    /**
     * Exporta todas las tablas del esquema configurado a archivos individuales.
     */
    public void exportAllTables() {
        try {
            // Crear directorio de salida
            Path outputPath = Paths.get(outputDirectory);
            Files.createDirectories(outputPath);
            log.info("Directorio de salida creado: {}", outputPath.toAbsolutePath());
            
            // Obtener lista de tablas
            List<String> tables = metadataExtractor.getTables(schema);
            log.info("Se encontraron {} tablas en el esquema {}", tables.size(), schema);
            
            // Contador de progreso
            int processed = 0;
            int errors = 0;
            
            // Procesar cada tabla
            for (String tableName : tables) {
                try {
                    log.info("Procesando tabla {}/{}: {}", ++processed, tables.size(), tableName);
                    exportTable(tableName);
                } catch (Exception e) {
                    errors++;
                    log.error("Error procesando tabla {}: {}", tableName, e.getMessage(), e);
                }
            }
            
            log.info("========================================");
            log.info("Proceso completado:");
            log.info("- Tablas procesadas: {}", processed - errors);
            log.info("- Errores: {}", errors);
            log.info("- Directorio de salida: {}", outputPath.toAbsolutePath());
            log.info("========================================");
            
        } catch (IOException e) {
            log.error("Error creando directorio de salida: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Exporta una tabla específica a un archivo de texto en formato descriptivo.
     */
    private void exportTable(String tableName) throws IOException {
        // Obtener información completa de la tabla
        TableInfo tableInfo = getTableInfo(tableName);
        
        // Generar contenido descriptivo
        String content = generateDescriptiveFormat(tableInfo);
        
        // Escribir archivo
        Path filePath = Paths.get(outputDirectory, tableName + ".txt");
        Files.write(filePath, content.getBytes("UTF-8"));
        
        log.debug("Archivo creado: {} ({} columnas, {} PKs, {} FKs)", 
            filePath.getFileName(), 
            tableInfo.getColumnas().size(),
            tableInfo.getPrimaryKeyColumns().size(),
            tableInfo.getForeignKeys().size()
        );
    }
    
    /**
     * Recopila toda la información de una tabla (columnas, PK, FK, comentarios).
     */
    private TableInfo getTableInfo(String tableName) {
        return TableInfo.builder()
            .nombre(tableName)
            .comentario(metadataExtractor.getTableComment(schema, tableName))
            .columnas(metadataExtractor.getColumns(schema, tableName))
            .primaryKeyColumns(metadataExtractor.getPrimaryKeyColumns(schema, tableName))
            .primaryKeyName(metadataExtractor.getPrimaryKeyName(schema, tableName))
            .foreignKeys(includeForeignKeys ? 
                metadataExtractor.getForeignKeys(schema, tableName) : 
                new ArrayList<>())
            .build();
    }
    
    /**
     * Genera formato descriptivo optimizado para NotebookLM.
     * Este formato prioriza la claridad y el contexto para modelos de lenguaje.
     */
    private String generateDescriptiveFormat(TableInfo tableInfo) {
        StringBuilder desc = new StringBuilder();
        
        // === ENCABEZADO ===
        desc.append("==============================================\n");
        desc.append("TABLA: ").append(tableInfo.getNombre()).append("\n");
        desc.append("==============================================\n\n");
        
        // === DESCRIPCIÓN DE LA TABLA ===        
        if (tableInfo.getComentario() != null && !tableInfo.getComentario().trim().isEmpty()) {
        	desc.append("DESCRIPCIÓN DE LA TABLA:\n");
            desc.append(tableInfo.getComentario().trim()).append("\n\n");
        } 
        
        // === COLUMNAS ===
        desc.append("COLUMNAS:\n");
        desc.append("------------------\n");
        
        int colNum = 1;
        for (ColumnInfo col : tableInfo.getColumnas()) {
            desc.append(colNum++).append(". ").append(col.getNombre()).append("\n");
            
            // Tipo de dato
            desc.append("   - Tipo: ").append(col.getTipo());
            if (col.getLongitud() != null && col.getLongitud() > 0) {
                desc.append("(").append(col.getLongitud());
                if (col.getEscala() != null && col.getEscala() > 0) {
                    desc.append(",").append(col.getEscala());
                }
                desc.append(")");
            }
            desc.append("\n");
            
            // Nullable
            desc.append("   - Nullable: ").append(col.isNullable() ? "SÍ" : "NO").append("\n");
            
            // Valor por defecto
            if (col.getValorDefault() != null && !col.getValorDefault().trim().isEmpty()) {
                String defaultValue = col.getValorDefault().trim();
                // Limpiar valores por defecto para mejor legibilidad
                if (defaultValue.startsWith("'") && defaultValue.endsWith("'")) {
                    defaultValue = defaultValue.substring(1, defaultValue.length() - 1);
                }
                desc.append("   - Valor por defecto: ").append(defaultValue).append("\n");
            }
            
            // Es Primary Key?
            if (tableInfo.getPrimaryKeyColumns().contains(col.getNombre())) {
                desc.append("   - Constraint: PRIMARY KEY");
                if (tableInfo.getPrimaryKeyColumns().size() > 1) {
                    desc.append(" (parte de clave compuesta)");
                }
                desc.append("\n");
            }
            
            for (ForeignKeyInfo fk : tableInfo.getForeignKeys()) {
                if (fk.getColumnasOrigen().contains(col.getNombre())) {
                    int index = fk.getColumnasOrigen().indexOf(col.getNombre());
                    desc.append("   - Foreign Key: referencia a ")
                        .append(fk.getTablaDestino()).append(".")
                        .append(fk.getColumnasDestino().get(index));
                    
                    if (!"NO ACTION".equals(fk.getOnDeleteAction())) {
                        desc.append(" (ON DELETE ").append(fk.getOnDeleteAction()).append(")");
                    }
                    desc.append("\n");
                }
            }
            
            // Comentario/Descripción de la columna
            if (col.getComentario() != null && !col.getComentario().trim().isEmpty()) {
                desc.append("   - Descripción: ").append(col.getComentario().trim()).append("\n");
            }
            
            desc.append("\n");
        }
        
        // === PRIMARY KEY ===
        if (!tableInfo.getPrimaryKeyColumns().isEmpty()) {
            desc.append("PRIMARY KEY:\n");
            desc.append("------------------\n");
            desc.append("- Nombre del constraint: ").append(
                tableInfo.getPrimaryKeyName() != null ? tableInfo.getPrimaryKeyName() : "[Sin nombre]"
            ).append("\n");
            desc.append("- Columnas: ").append(String.join(", ", tableInfo.getPrimaryKeyColumns())).append("\n");
            desc.append("- Tipo: ").append(
                tableInfo.getPrimaryKeyColumns().size() > 1 ? "Clave compuesta" : "Clave simple"
            ).append("\n\n");
        }
        
        // === FOREIGN KEYS ===
        if (!tableInfo.getForeignKeys().isEmpty()) {
            desc.append("FOREIGN KEYS:\n");
            desc.append("------------------\n");
            
            int fkNum = 1;
            for (ForeignKeyInfo fk : tableInfo.getForeignKeys()) {
                desc.append(fkNum++).append(". ").append(fk.getNombre()).append("\n");
                desc.append("   - Columnas origen: ").append(String.join(", ", fk.getColumnasOrigen())).append("\n");
                desc.append("   - Tabla destino: ").append(fk.getTablaDestino()).append("\n");
                desc.append("   - Columnas destino: ").append(String.join(", ", fk.getColumnasDestino())).append("\n");
                
                if (!"NO ACTION".equals(fk.getOnDeleteAction()) || !"NO ACTION".equals(fk.getOnUpdateAction())) {
                    desc.append("   - Reglas: ");
                    if (!"NO ACTION".equals(fk.getOnDeleteAction())) {
                        desc.append("ON DELETE ").append(fk.getOnDeleteAction());
                    }
                    if (!"NO ACTION".equals(fk.getOnUpdateAction())) {
                        if (!"NO ACTION".equals(fk.getOnDeleteAction())) {
                            desc.append(", ");
                        }
                        desc.append("ON UPDATE ").append(fk.getOnUpdateAction());
                    }
                    desc.append("\n");
                }
                desc.append("\n");
            }
        }
        
        // === RESUMEN ===
        desc.append("RESUMEN:\n");
        desc.append("------------------\n");
        desc.append("- Total de columnas: ").append(tableInfo.getColumnas().size()).append("\n");
        desc.append("- Tiene clave primaria: ").append(
            tableInfo.getPrimaryKeyColumns().isEmpty() ? "NO" : "SÍ"
        ).append("\n");
        
        if (!tableInfo.getPrimaryKeyColumns().isEmpty()) {
            desc.append("- Tipo de clave primaria: ").append(
                tableInfo.getPrimaryKeyColumns().size() > 1 ? "Compuesta" : "Simple"
            ).append("\n");
        }
        
        desc.append("- Número de foreign keys: ").append(tableInfo.getForeignKeys().size()).append("\n");
        
        // Contar columnas obligatorias
        long requiredColumns = tableInfo.getColumnas().stream()
            .filter(col -> !col.isNullable())
            .count();
        desc.append("- Columnas obligatorias (NOT NULL): ").append(requiredColumns).append("\n");
        
        // Contar columnas con valores por defecto
        long columnsWithDefaults = tableInfo.getColumnas().stream()
            .filter(col -> col.getValorDefault() != null && !col.getValorDefault().trim().isEmpty())
            .count();
        desc.append("- Columnas con valor por defecto: ").append(columnsWithDefaults).append("\n");
        
        return desc.toString();
    }
}
