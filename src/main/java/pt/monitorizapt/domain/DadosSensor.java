package pt.monitorizapt.domain;

/**
 * Immutable container for a sensor reading.
 * Uses Java 'record' feature (simpler than a class, automatically generates 
 * getters, equals, hashCode, and toString).
 */
public record DadosSensor(double valor, String unidade, boolean alerta, long timestamp) {
    
    // Convenience method to format alert status for UI or logs
    public String alertaTexto() {
        return alerta ? "ALERTA" : "OK";
    }
}