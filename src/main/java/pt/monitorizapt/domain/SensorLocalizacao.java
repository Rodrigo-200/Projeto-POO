package pt.monitorizapt.domain;

/**
 * Enum defining all valid locations in the system.
 * This acts as the "Single Source of Truth" for location data.
 * It maps user-friendly names (for the UI) to safe strings (for MQTT topics).
 */
public enum SensorLocalizacao {
    LISBOA_CAMPUS_IPLUSO("Lisboa - Campus IPLuso", "Lisboa_Campus_IPLuso"),
    LISBOA_BAIXA("Lisboa - Baixa", "Lisboa_Baixa"),
    PORTO_MATOSINHOS("Porto - Matosinhos", "Porto_Matosinhos"),
    COIMBRA_CENTRO("Coimbra - Centro", "Coimbra_Centro"),
    FARO_MARINA("Faro - Marina", "Faro_Marina"),
    BRAGA_SAMEIRO("Braga - Sameiro", "Braga_Sameiro"),
    EVORA_UNIVERSIDADE("Évora - Universidade", "Evora_Universidade");

    private final String descricao;
    private final String segmentoTopico;

    SensorLocalizacao(String descricao, String segmentoTopico) {
        this.descricao = descricao;
        this.segmentoTopico = segmentoTopico;
    }

    // Used in the UI (ComboBox, Table)
    public String descricao() {
        return descricao;
    }

    public String segmentoTopico() {
        return segmentoTopico;
    }

    /**
     * Sanitizes the location name to be used as a unique ID in the JSON payload.
     * Replaces spaces and dashes with underscores to ensure compatibility.
     */
    public String idSegmento() {
        return segmentoTopico.replace("-", "_").replace(" ", "_").toUpperCase();
    }

    /**
     * Generates the MQTT topic for publishing data.
     * Structure: envira/pt/sensores/dados/{Localizacao}
     */
    public String topicoDados() {
        return "envira/pt/sensores/dados/" + segmentoTopico;
    }

    /**
     * Generates the MQTT topic for subscribing to remote commands.
     * Structure: envira/pt/sensores/comandos/{Localizacao}
     */
    public String topicoComandos() {
        return "envira/pt/sensores/comandos/" + segmentoTopico;
    }

    @Override
    public String toString() {
        return descricao;
    }
}