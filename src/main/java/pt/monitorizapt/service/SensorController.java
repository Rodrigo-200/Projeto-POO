package pt.monitorizapt.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import pt.monitorizapt.domain.DadosSensor;
import pt.monitorizapt.domain.Sensor;
import pt.monitorizapt.domain.SensorAbstrato;
import pt.monitorizapt.domain.SensorLocalizacao;
import pt.monitorizapt.domain.SensorTipo;
import pt.monitorizapt.domain.SensorUpdateListener;
import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.sensors.SensorHumidade;
import pt.monitorizapt.sensors.SensorQualidadeAr;
import pt.monitorizapt.sensors.SensorTemperatura;
import pt.monitorizapt.util.JsonPayloadBuilder;

/**
 * The Controller class acts as the bridge between the UI, the Sensors, and MQTT.
 * It manages the lifecycle of all sensors and notifies the UI when data changes.
 */
public class SensorController {
    // Formatter for log messages (thread-safe compared to SimpleDateFormat)
    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    // We use EnumMap for efficiency since keys are Enums
    private final Map<SensorLocalizacao, SensorAbstrato> sensoresPorLocalizacao = new EnumMap<>(SensorLocalizacao.class);
    
    // Thread-safe lists to store UI observers (Snapshot for table, String for logs)
    private final List<Consumer<SensorSnapshot>> snapshotObservers = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> logObservers = new CopyOnWriteArrayList<>();
    
    private final JsonPayloadBuilder payloadBuilder = new JsonPayloadBuilder();
    private final MqttClientManager mqttClientManager;

    public SensorController(MqttClientManager mqttClientManager) {
        this.mqttClientManager = mqttClientManager;
        
        // Initialize all sensors immediately
        criarSensores();
        
        // Start all sensor threads
        sensoresPorLocalizacao.values().forEach(Sensor::iniciar);
        
        // Register MQTT command listeners for each location
        for (SensorLocalizacao localizacao : SensorLocalizacao.values()) {
            mqttClientManager.registerCommandHandler(localizacao, comando -> processarComandoRemoto(localizacao, comando));
        }
        
        // Connect to the broker in background
        mqttClientManager.connectAsync();
    }

    /**
     * Factory method pattern: decides which specific sensor class (Temperature, Humidity, Air)
     * to instantiate based on the location.
     */
    private void criarSensores() {
        for (SensorLocalizacao localizacao : SensorLocalizacao.values()) {
            SensorAbstrato sensor = criarSensorPorLocalizacao(localizacao);
            // Register a listener to update the UI whenever the sensor reads data
            sensor.registrarListener(criarListener());
            sensoresPorLocalizacao.put(localizacao, sensor);
        }
    }

    private SensorAbstrato criarSensorPorLocalizacao(SensorLocalizacao localizacao) {
        return switch (localizacao) {
            case LISBOA_CAMPUS_IPLUSO, COIMBRA_CENTRO, EVORA_UNIVERSIDADE ->
                    new SensorTemperatura(localizacao, payloadBuilder, mqttClientManager);
            case LISBOA_BAIXA, FARO_MARINA ->
                    new SensorHumidade(localizacao, payloadBuilder, mqttClientManager);
            case PORTO_MATOSINHOS, BRAGA_SAMEIRO ->
                    new SensorQualidadeAr(localizacao, payloadBuilder, mqttClientManager);
        };
    }

    private SensorUpdateListener criarListener() {
        return (sensor, dados, payload) -> {
            // Convert domain data into a snapshot (DTO) optimized for the UI table
            SensorSnapshot snapshot = new SensorSnapshot(sensor.getIDUnico(),
                    sensor.getLocalizacao().descricao(),
                    sensor.getTipo(),
                    dados.valor(),
                    dados.unidade(),
                    dados.alerta(),
                    dados.timestamp());
            
            // Notify the Table
            snapshotObservers.forEach(observer -> observer.accept(snapshot));
            
            // Notify the Log text area
            log(String.format("Sensor %s publicou %s", sensor.getIDUnico(), formatValor(sensor.getTipo(), dados)));
        };
    }

    public void registerSnapshotObserver(Consumer<SensorSnapshot> observer) {
        snapshotObservers.add(observer);
    }

    public void registerLogObserver(Consumer<String> observer) {
        logObservers.add(observer);
    }

    // --- Actions triggered by UI buttons ---

    public void ativarLocalizacao(SensorLocalizacao localizacao, long intervaloMillis) {
        SensorAbstrato sensor = sensoresPorLocalizacao.get(localizacao);
        if (sensor == null) {
            return;
        }
        sensor.setIntervaloMillis(intervaloMillis);
        sensor.ativar();
        log(String.format("Sensor %s ativado (intervalo %d ms)", sensor.getIDUnico(), sensor.getIntervaloMillis()));
    }

    public void desativarLocalizacao(SensorLocalizacao localizacao) {
        SensorAbstrato sensor = sensoresPorLocalizacao.get(localizacao);
        if (sensor == null) {
            return;
        }
        sensor.desativar();
        log(String.format("Sensor %s desativado", sensor.getIDUnico()));
    }

    /**
     * Handles commands received via MQTT (e.g., from a mobile app dashboard).
     */
    private void processarComandoRemoto(SensorLocalizacao localizacao, String comandoJson) {
        SensorAbstrato sensor = sensoresPorLocalizacao.get(localizacao);
        if (sensor == null) {
            return;
        }
        sensor.processarComando(comandoJson);
        log(String.format("Comando MQTT aplicado a %s: %s", sensor.getIDUnico(), comandoJson));
    }

    private void log(String mensagem) {
        String linha = LOG_FORMATTER.format(Instant.now()) + " [INFO] " + mensagem;
        logObservers.forEach(observer -> observer.accept(linha));
    }

    private String formatValor(SensorTipo tipo, DadosSensor dados) {
        return switch (tipo) {
            case TEMPERATURA -> String.format("%.2f\u00B0C", dados.valor());
            case HUMIDADE -> String.format("%.2f%%", dados.valor());
            case QUALIDADE_AR -> String.format("%.2f AQI", dados.valor());
        };
    }

    public void shutdown() {
        sensoresPorLocalizacao.values().forEach(Sensor::desligar);
        mqttClientManager.shutdown();
    }
}