# Database Structure Exporter

A powerful tool to export database table structures to text files optimized for Google NotebookLM. Each table is exported to a separate file with detailed information about columns, data types, constraints, and relationships.

## 📋 Features

- ✅ **Complete table structure export** (columns, data types, sizes, nullable)
- ✅ **Detailed foreign key and relationship information**
- ✅ **One file per table** for easy NotebookLM loading
- ✅ **AI-optimized format** for better comprehension
- ✅ **Currently supports DB2**
- 🔄 **Coming soon**: PostgreSQL, MySQL, Oracle, SQL Server

## 🛠️ Requirements

- Java 8 or higher
- Maven 3.x or higher
- Access to a DB2 database
- DB2 JDBC driver (automatically downloaded via Maven)

## 📦 Installation

1. **Clone the repository:**
```bash
git clone https://github.com/eloisa-alesqui/database-structure-exporter.git
cd database-structure-exporter
```

2. **Copy the example configuration file:**
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

3. **Edit `application.properties` with your credentials:**
```properties
# DB2 Configuration
spring.datasource.url=jdbc:db2://your-server:50000/your-database
spring.datasource.username=your-username
spring.datasource.password=your-password

# Database schema to export
app.schema=YOUR_SCHEMA

# Directory where exported files will be saved
app.output.directory=database_structure
```

## 🚀 Usage

### Run with Maven:
```bash
mvn spring-boot:run
```

### Generate executable JAR:
```bash
mvn clean package
java -jar target/db-exporter-1.0.0.jar
```

## 📁 Output

Files will be generated in the configured directory with the following format:

```
database_structure/
├── TABLE1.txt
├── TABLE2.txt
├── TABLE3.txt
└── ...
```

### Example of generated file:
```
==============================================
TABLE: EMPLOYEES
==============================================

TABLE DESCRIPTION:
Table that stores employee information

COLUMNS:
------------------
1. EMPLOYEE_ID
 - Type: DECIMAL(10)
 - Nullable: NO
 - Constraint: PRIMARY KEY
 - Description: Employee Code

2. FIRST_NAME
 - Type: CHARACTER(50)
 - Nullable: NO

3. DEPARTMENT_ID
 - Type: DECIMAL(10)
 - Nullable: YES
 - Foreign Key: DEPARTMENTS.DEPARTMENT_ID

[...]

FOREIGN KEYS:
------------------
1. SQL150618215635450
 - Source columns: DEPARTMENT_ID
 - Target table: DEPARTMENTS
 - Target columns: DEPARTMENT_ID
 - Rules: ON DELETE CASCADE

SUMMARY:
------------------
- Total columns: 16
- Has primary key: YES
- Primary key type: Simple
- Number of foreign keys: 1
- Required columns (NOT NULL): 10
- Columns with default value: 7
```

## 🔧 Advanced Configuration

### Available configuration parameters:

| Parameter | Description | Default Value |
|-----------|-------------|---------------|
| `app.schema` | Database schema to export | Required |
| `app.output.directory` | Output directory | `database_structure` |
| `app.include.foreign.keys` | Include FK information | `true` |
| `spring.datasource.hikari.maximum-pool-size` | Maximum pool size | `5` |

## 🏗️ Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── es/alesqui/dbexporter/
│   │       ├── model/          # Model classes (ColumnInfo, TableInfo, etc.)
│   │       ├── service/        # Extraction and export services
│   │       └── runner/         # Main Spring Boot runner
│   └── resources/
│       └── application.properties
```

### 🚀 Upcoming Features
- [ ] PostgreSQL database support
- [ ] MySQL database support
- [ ] Oracle database support
- [ ] SQL Server database support
- [ ] JSON/YAML export formats

## 🐛 Bug Reports
If you find a bug, please open an issue with:

- Problem description
- Steps to reproduce
- Java and Maven versions
- Error logs (if any)

## 📝 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

Eloísa Alés Esquivel
- GitHub: [@eloisa-alesqui](https://github.com/eloisa-alesqui)
- LinkedIn: [Connect with me](https://linkedin.com/in/eloisa-ales-esquivel)

---

⭐️ **If you find this project useful, please consider giving it a star on GitHub!**
