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
				tableInfo.getForeignKeys().size());
	}

	/**
	 * Recopila toda la información de una tabla (columnas, PK, FK, comentarios).
	 */
	private TableInfo getTableInfo(String tableName) {
		return TableInfo.builder().nombre(tableName).comentario(metadataExtractor.getTableComment(schema, tableName))
				.columnas(metadataExtractor.getColumns(schema, tableName))
				.primaryKeyColumns(metadataExtractor.getPrimaryKeyColumns(schema, tableName))
				.primaryKeyName(metadataExtractor.getPrimaryKeyName(schema, tableName))
				.foreignKeys(
						includeForeignKeys ? metadataExtractor.getForeignKeys(schema, tableName) : new ArrayList<>())
				.build();
	}

	/**
	 * Genera formato descriptivo optimizado para NotebookLM. Este formato prioriza
	 * la claridad y el contexto para modelos de lenguaje.
	 */
	private String generateDescriptiveFormat(TableInfo tableInfo) {
		StringBuilder desc = new StringBuilder();

		// === ENCABEZADO ===
		desc.append("==============================================\n");
		desc.append("TABLE: ").append(tableInfo.getNombre()).append("\n");
		desc.append("==============================================\n\n");

		// === DESCRIPCIÓN DE LA TABLA ===
		if (tableInfo.getComentario() != null && !tableInfo.getComentario().trim().isEmpty()) {
			desc.append("TABLE DESCRIPTION:\n");
			desc.append(tableInfo.getComentario().trim()).append("\n\n");
		}

		// === COLUMNAS ===
		desc.append("COLUMNS:\n");
		desc.append("------------------\n");

		int colNum = 1;
		for (ColumnInfo col : tableInfo.getColumnas()) {
			desc.append(colNum++).append(". ").append(col.getNombre()).append("\n");

			// Tipo de dato
			desc.append("   - Type: ").append(col.getTipo());
			if (col.getLongitud() != null && col.getLongitud() > 0) {
				desc.append("(").append(col.getLongitud());
				if (col.getEscala() != null && col.getEscala() > 0) {
					desc.append(",").append(col.getEscala());
				}
				desc.append(")");
			}
			desc.append("\n");

			// Nullable
			desc.append("   - Nullable: ").append(col.isNullable() ? "YES" : "NO").append("\n");

			// Valor por defecto
			if (col.getValorDefault() != null && !col.getValorDefault().trim().isEmpty()) {
				String defaultValue = col.getValorDefault().trim();
				// Limpiar valores por defecto para mejor legibilidad
				if (defaultValue.startsWith("'") && defaultValue.endsWith("'")) {
					defaultValue = defaultValue.substring(1, defaultValue.length() - 1);
				}
				desc.append("   - Default value: ").append(defaultValue).append("\n");
			}

			// Es Primary Key?
			if (tableInfo.getPrimaryKeyColumns().contains(col.getNombre())) {
				desc.append("   - Constraint: PRIMARY KEY");
				if (tableInfo.getPrimaryKeyColumns().size() > 1) {
					desc.append(" (part of composite key)");
				}
				desc.append("\n");
			}

			// Foreign Keys
			for (ForeignKeyInfo fk : tableInfo.getForeignKeys()) {
				if (fk.getColumnasOrigen().contains(col.getNombre())) {
					int index = fk.getColumnasOrigen().indexOf(col.getNombre());
					desc.append("   - Foreign Key: references ").append(fk.getTablaDestino()).append(".")
							.append(fk.getColumnasDestino().get(index));

					if (!"NO ACTION".equals(fk.getOnDeleteAction())) {
						desc.append(" (ON DELETE ").append(fk.getOnDeleteAction()).append(")");
					}
					desc.append("\n");
				}
			}

			// Comentario/Descripción de la columna
			if (col.getComentario() != null && !col.getComentario().trim().isEmpty()) {
				desc.append("   - Description: ").append(col.getComentario().trim()).append("\n");
			}

			desc.append("\n");
		}

		// === PRIMARY KEY ===
		if (!tableInfo.getPrimaryKeyColumns().isEmpty()) {
			desc.append("PRIMARY KEY:\n");
			desc.append("------------------\n");
			desc.append("- Constraint name: ")
					.append(tableInfo.getPrimaryKeyName() != null ? tableInfo.getPrimaryKeyName() : "[Unnamed]")
					.append("\n");
			desc.append("- Columns: ").append(String.join(", ", tableInfo.getPrimaryKeyColumns())).append("\n");
			desc.append("- Type: ").append(tableInfo.getPrimaryKeyColumns().size() > 1 ? "Composite key" : "Simple key")
					.append("\n\n");
		}

		// === FOREIGN KEYS ===
		if (!tableInfo.getForeignKeys().isEmpty()) {
			desc.append("FOREIGN KEYS:\n");
			desc.append("------------------\n");

			int fkNum = 1;
			for (ForeignKeyInfo fk : tableInfo.getForeignKeys()) {
				desc.append(fkNum++).append(". ").append(fk.getNombre()).append("\n");
				desc.append("   - Source columns: ").append(String.join(", ", fk.getColumnasOrigen())).append("\n");
				desc.append("   - Target table: ").append(fk.getTablaDestino()).append("\n");
				desc.append("   - Target columns: ").append(String.join(", ", fk.getColumnasDestino())).append("\n");

				if (!"NO ACTION".equals(fk.getOnDeleteAction()) || !"NO ACTION".equals(fk.getOnUpdateAction())) {
					desc.append("   - Rules: ");
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
		desc.append("SUMMARY:\n");
		desc.append("------------------\n");
		desc.append("- Total columns: ").append(tableInfo.getColumnas().size()).append("\n");
		desc.append("- Has primary key: ").append(tableInfo.getPrimaryKeyColumns().isEmpty() ? "NO" : "YES")
				.append("\n");

		if (!tableInfo.getPrimaryKeyColumns().isEmpty()) {
			desc.append("- Primary key type: ")
					.append(tableInfo.getPrimaryKeyColumns().size() > 1 ? "Composite" : "Simple").append("\n");
		}

		desc.append("- Number of foreign keys: ").append(tableInfo.getForeignKeys().size()).append("\n");

		// Contar columnas obligatorias
		long requiredColumns = tableInfo.getColumnas().stream().filter(col -> !col.isNullable()).count();
		desc.append("- Required columns (NOT NULL): ").append(requiredColumns).append("\n");

		// Contar columnas con valores por defecto
		long columnsWithDefaults = tableInfo.getColumnas().stream()
				.filter(col -> col.getValorDefault() != null && !col.getValorDefault().trim().isEmpty()).count();
		desc.append("- Columns with default values: ").append(columnsWithDefaults).append("\n");

		return desc.toString();
	}
}