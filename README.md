# Database Structure Exporter

Herramienta para exportar la estructura de tablas de bases de datos a archivos de texto optimizados para su uso con Google NotebookLM. Cada tabla se exporta en un archivo separado con información detallada sobre columnas, tipos de datos, constraints y relaciones.

## 📋 Características

- ✅ Exporta estructura completa de tablas (columnas, tipos de datos, tamaños, nullable)
- ✅ Incluye información detallada de foreign keys y relaciones
- ✅ Genera un archivo separado por tabla para facilitar la carga en NotebookLM
- ✅ Formato optimizado para comprensión por IA
- ✅ Actualmente soporta DB2
- 🔄 Próximamente: PostgreSQL, MySQL, Oracle, SQL Server

## 🛠️ Requisitos

- Java 8 o superior
- Maven 3.x o superior
- Acceso a una base de datos DB2
- Driver JDBC de DB2 (se descarga automáticamente via Maven)

## 📦 Instalación

1. **Clona el repositorio:**
```bash
git clone https://github.com/tu-usuario/database-structure-exporter.git
cd database-structure-exporter
```

2. **Copia el archivo de configuración de ejemplo:**
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

3. **Edita `application.properties` con tus credenciales:**
```properties
# DB2 Configuration
spring.datasource.url=jdbc:db2://tu-servidor:50000/tu-base-datos
spring.datasource.username=tu-usuario
spring.datasource.password=tu-password

# Esquema de base de datos a exportar
app.schema=TU_ESQUEMA

# Directorio donde se guardarán los archivos exportados
app.output.directory=database_structure
```

## 🚀 Uso

### Ejecutar con Maven:
```bash
mvn spring-boot:run
```

### Generar JAR ejecutable:
```bash
mvn clean package
java -jar target/db-exporter-1.0.0.jar
```

## 📁 Resultado

Los archivos se generarán en el directorio configurado con el siguiente formato:

```
database_structure/
├── TABLA1.txt
├── TABLA2.txt
├── TABLA3.txt
└── ...
```

### Ejemplo de archivo generado:
```
==============================================
TABLA: EMPLOYEES
==============================================

DESCRIPCIÓN DE LA TABLA:
Tabla que almacena información de empleados

COLUMNAS:
------------------
1. EMPLOYEE_ID
   - Tipo: DECIMAL(10)
   - Nullable: NO
   - Constraint: PRIMARY KEY
   - Descripción: Código del Empleado

2. FIRST_NAME
   - Tipo: CHARACTER(50)
   - Nullable: NO

3. DEPARTMENT_ID
   - Tipo: DECIMAL(10)
   - Nullable: YES
   - Foreign Key: DEPARTMENTS.DEPARTMENT_ID

[...]

FOREIGN KEYS:
------------------
1. SQL150618215635450
   - Columnas origen: DEPARTMENT_ID
   - Tabla destino: DEPARTMENTS
   - Columnas destino: DEPARTMENT_ID
   - Reglas: ON DELETE CASCADE

RESUMEN:
------------------
- Total de columnas: 16
- Tiene clave primaria: SÍ
- Tipo de clave primaria: Simple
- Número de foreign keys: 1
- Columnas obligatorias (NOT NULL): 10
- Columnas con valor por defecto: 7

```

## 🔧 Configuración Avanzada

### Parámetros de configuración disponibles:

| Parámetro | Descripción | Valor por defecto |
|-----------|-------------|-------------------|
| `app.schema` | Esquema de BD a exportar | Requerido |
| `app.output.directory` | Directorio de salida | `database_structure` |
| `app.include.foreign.keys` | Incluir información de FKs | `true` |
| `spring.datasource.hikari.maximum-pool-size` | Tamaño máximo del pool | `5` |

## 🏗️ Estructura del Proyecto

```
src/
├── main/
│   ├── java/
│   │   └── es/alesqui/dbexporter/
│   │       ├── model/          # Clases de modelo (ColumnInfo, TableInfo, etc.)
│   │       ├── service/        # Servicios de extracción y exportación
│   │       └── runner/         # Runner principal de Spring Boot
│   └── resources/
│       └── application.properties
```

### 🚀 Próximas características planificadas:
- [ ] Soporte para PostgreSQL
- [ ] Soporte para MySQL
- [ ] Soporte para Oracle
- [ ] Soporte para SQL Server
- [ ] Exportación a formato JSON/YAML

### 🐛 Reportar bugs:
Si encuentras un bug, por favor abre un issue con:

- Descripción del problema
- Pasos para reproducirlo
- Versión de Java y Maven
- Logs de error (si los hay)

## 📝 Licencia

Este proyecto está licenciado bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para más detalles.

## 👤 Autor

Eloísa Alés Esquivel
- GitHub: [@eloisa-alesqui](https://github.com/eloisa-alesqui)

---

⭐️ Si este proyecto te resulta útil, considera darle una estrella en GitHub!
