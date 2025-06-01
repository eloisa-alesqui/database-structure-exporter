# Database Structure Exporter

Herramienta para exportar la estructura de tablas de bases de datos a archivos de texto optimizados para NotebookLM.

## Características

- Exporta estructura de tablas (columnas, tipos de datos, constraints)
- Incluye información de foreign keys
- Genera un archivo por tabla para facilitar la carga en NotebookLM
- Actualmente soporta DB2

## Requisitos

- Java 8 o superior
- Maven 3.x
- Acceso a una base de datos DB2

## Configuración

1. Clona el repositorio
2. Copia `application.properties.example` a `application.properties`
3. Configura tus credenciales de base de datos

## Uso

```bash
mvn spring-boot:run