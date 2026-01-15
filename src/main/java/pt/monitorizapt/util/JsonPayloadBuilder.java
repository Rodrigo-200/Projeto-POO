package pt.monitorizapt.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import pt.monitorizapt.domain.DadosSensor;
import pt.monitorizapt.domain.Sensor;

/**
 * Responsible for constructing the final JSON message sent to the MQTT broker.
 * It enriches the raw sensor data with metadata (Owner, Campus, ID) and a validation hash.
 */
public class JsonPayloadBuilder {
    // We disable HTML escaping to ensure the JSON remains standard (e.g., using < or >)
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public String buildPayload(Sensor sensor, DadosSensor dados) {
        JsonObject objeto = new JsonObject();
        
        // Add Metadata required by the project specification
        objeto.addProperty("campus", sensor.getLocalizacao().descricao());
        objeto.addProperty("sensor", sensor.getIDUnico());
        objeto.addProperty("ID Unico", sensor.getIDUnico());
        objeto.addProperty("Owner", sensor.getOwner());
        objeto.addProperty("tipo", sensor.getTipo().tipoJson());
        
        // Add Sensor Data
        objeto.addProperty("valor", arredondar(dados.valor()));
        objeto.addProperty("unidade", dados.unidade());
        objeto.addProperty("alerta", dados.alerta());
        objeto.addProperty("timestamp", dados.timestamp());

        // --- Integrity Check Logic ---
        // 1. Convert the object to a String
        String semHash = GSON.toJson(objeto);
        
        // 2. Canonicalize: Remove spaces to ensure the hash is consistent regardless of formatting
        String semEspacos = semHash.replace(" ", "");
        
        // 3. Generate the hash and add it to the final object
        objeto.addProperty("hash_validacao", HashUtil.sha256Hex(semEspacos));
        
        return GSON.toJson(objeto);
    }

    /**
     * Rounds doubles to 2 decimal places using BigDecimal.
     * Essential for sensor data to avoid floating point artifacts (e.g., 23.999999994).
     */
    private double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}