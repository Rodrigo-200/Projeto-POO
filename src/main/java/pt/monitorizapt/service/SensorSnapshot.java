package pt.monitorizapt.service;

import pt.monitorizapt.domain.SensorTipo;

/**
 * A "Snapshot" acts as a Data Transfer Object (DTO) for the UI.
 * It contains a frozen state of a sensor at a specific moment.
 * Being a record (immutable), it is safe to pass between the background threads and the Swing UI thread.
 */
public record SensorSnapshot(String id,
                             String localizacao,
                             SensorTipo tipo,
                             double valor,
                             String unidade,
                             boolean alerta,
                             long timestamp) {
    
    // Helper for table display
    public String valorFormatado() {
        return String.format("%.2f %s", valor, unidade);
    }
}