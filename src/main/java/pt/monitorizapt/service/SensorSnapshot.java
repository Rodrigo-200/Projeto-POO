package pt.monitorizapt.service;

import pt.monitorizapt.domain.SensorTipo;

public record SensorSnapshot(String id,
                             String localizacao,
                             SensorTipo tipo,
                             double valor,
                             String unidade,
                             boolean alerta,
                             long timestamp) {
    public String valorFormatado() {
        return String.format("%.2f %s", valor, unidade);
    }
}
