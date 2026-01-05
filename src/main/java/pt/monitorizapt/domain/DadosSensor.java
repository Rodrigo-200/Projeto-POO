package pt.monitorizapt.domain;

/**
 * Immutable container for a sensor reading.
 */
public record DadosSensor(double valor, String unidade, boolean alerta, long timestamp) {
    public String alertaTexto() {
        return alerta ? "ALERTA" : "OK";
    }
}
