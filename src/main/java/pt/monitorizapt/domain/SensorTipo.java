package pt.monitorizapt.domain;

/**
 * Enum describing the supported environmental sensor types.
 */
public enum SensorTipo {
    TEMPERATURA("temperatura", "Celsius", "Temperatura"),
    HUMIDADE("humidade", "%", "Humidade"),
    QUALIDADE_AR("qualidade_ar", "AQI", "Qualidade do Ar");

    private final String tipoJson;
    private final String unidadePadrao;
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
