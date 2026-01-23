package pt.monitorizapt.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Service responsible for persisting sensor data into CSV files.
 * It organizes files by Location and Date to ensure manageability and facilitate auditing.
 */
public class CsvLogService {
    
    // Directory where CSV files will be stored (relative to project root)
    private static final String LOG_DIRECTORY = "registos_csv";
    
    // Date formatter for the FILENAME (Daily rotation, e.g., 2024-01-23)
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());

    // Date formatter for the CONTENT (ISO-8601 standard)
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public CsvLogService() {
        criarDiretoria();
    }

    private void criarDiretoria() {
        File directory = new File(LOG_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Thread-safe method that writes the snapshot to the disk.
     * Synchronized to prevent concurrent write issues from multiple sensor threads.
     */
    public synchronized void registarLeitura(SensorSnapshot dados) {
        String fileDate = FILE_DATE_FORMATTER.format(Instant.now());
        
        // Sanitizes the location name to be OS-safe
        // Ex: "Lisboa - Campus IPLuso" -> "Lisboa___Campus_IPLuso"
        String safeLocationName = dados.localizacao().replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Path structure: registos_csv/Lisboa___Campus_IPLuso_2024-01-23.csv
        String filePath = String.format("%s/%s_%s.csv", LOG_DIRECTORY, safeLocationName, fileDate);

        File file = new File(filePath);
        boolean isNewFile = !file.exists();

        // Try-with-resources ensures the file writer is closed properly
        // 'true' in FileWriter constructor enables append mode
        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            // If the file is new, write the CSV header first
            if (isNewFile) {
                out.println("TIMESTAMP_ISO;TIMESTAMP_UNIX;SENSOR_ID;LOCALIZACAO;TIPO;VALOR;UNIDADE;ALERTA");
            }

            // Write the data row.
            // Using semicolon (;) as separator because comma is the decimal separator in PT locale.
            out.printf("%s;%d;%s;%s;%s;%.2f;%s;%s%n",
                    ISO_FORMATTER.format(Instant.ofEpochMilli(dados.timestamp())),
                    dados.timestamp(),
                    dados.id(),
                    dados.localizacao(),
                    dados.tipo().name(), // Uses the technical ENUM name 
                    dados.valor(),
                    dados.unidade(),
                    dados.alerta() ? "SIM" : "NAO"
            );

        } catch (IOException e) {
            // In a real scenario, this should be logged to a dedicated error stream
            System.err.println("CRITICAL ERROR: Failed to write to CSV: " + e.getMessage());
        }
    }
}