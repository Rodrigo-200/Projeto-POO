package pt.monitorizapt.domain;

/**
 * Known Portuguese locations requested by the assignment.
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

    public String descricao() {
        return descricao;
    }

    public String segmentoTopico() {
        return segmentoTopico;
    }

    public String idSegmento() {
        return segmentoTopico.replace("-", "_").replace(" ", "_").toUpperCase();
    }

    public String topicoDados() {
        return "envira/pt/sensores/dados/" + segmentoTopico;
    }

    public String topicoComandos() {
        return "envira/pt/sensores/comandos/" + segmentoTopico;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
