package es.alesqui.dbexporter.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import es.alesqui.dbexporter.service.TableExportService;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseStructureExporterRunner implements CommandLineRunner {
    
    private final TableExportService tableExportService;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando exportación de tablas GST...");
        tableExportService.exportAllTables();
        log.info("Exportación completada.");
    }
}

