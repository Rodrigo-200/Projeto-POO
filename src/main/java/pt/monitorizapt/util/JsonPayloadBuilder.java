package pt.monitorizapt.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import pt.monitorizapt.domain.DadosSensor;
import pt.monitorizapt.domain.Sensor;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class JsonPayloadBuilder {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public String buildPayload(Sensor sensor, DadosSensor dados) {
        JsonObject objeto = new JsonObject();
        objeto.addProperty("campus", sensor.getLocalizacao().descricao());
        objeto.addProperty("sensor", sensor.getIDUnico());
        objeto.addProperty("ID Unico", sensor.getIDUnico());
        objeto.addProperty("Owner", sensor.getOwner());
        objeto.addProperty("tipo", sensor.getTipo().tipoJson());
        objeto.addProperty("valor", arredondar(dados.valor()));
        objeto.addProperty("unidade", dados.unidade());
        objeto.addProperty("alerta", dados.alerta());
        objeto.addProperty("timestamp", dados.timestamp());

        String semHash = GSON.toJson(objeto);
        String semEspacos = semHash.replace(" ", "");
        objeto.addProperty("hash_validacao", HashUtil.sha256Hex(semEspacos));
        return GSON.toJson(objeto);
    }

    private double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
