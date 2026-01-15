package pt.monitorizapt.domain;

/**
 * Enum describing the supported environmental sensor types.
 * Helps standardize units and labels across the application.
 */
public enum SensorTipo {
    TEMPERATURA("temperatura", "Celsius", "Temperatura"),
    HUMIDADE("humidade", "%", "Humidade"),
    QUALIDADE_AR("qualidade_ar", "AQI", "Qualidade do Ar");

    // The exact string required by the JSON protocol
    private final String tipoJson;
    // The standard unit (e.g., Celsius, %)
    private final String unidadePadrao;
    // Pretty label for the User Interface
    private final String etiqueta;

    SensorTipo(String tipoJson, String unidadePadrao, String etiqueta) {
        this.tipoJson = tipoJson;
        this.unidadePadrao = unidadePadrao;
        this.etiqueta = etiqueta;
    }

    public String tipoJson() {
        return tipoJson;
    }

    public String unidadePadrao() {
        return unidadePadrao;
    }

    public String etiqueta() {
        return etiqueta;
    }
}