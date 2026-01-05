package pt.monitorizapt.service;

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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SensorController {
    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Map<SensorLocalizacao, SensorAbstrato> sensoresPorLocalizacao = new EnumMap<>(SensorLocalizacao.class);
    private final List<Consumer<SensorSnapshot>> snapshotObservers = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> logObservers = new CopyOnWriteArrayList<>();
    private final JsonPayloadBuilder payloadBuilder = new JsonPayloadBuilder();
    private final MqttClientManager mqttClientManager;

    public SensorController(MqttClientManager mqttClientManager) {
        this.mqttClientManager = mqttClientManager;
        criarSensores();
        sensoresPorLocalizacao.values().forEach(Sensor::iniciar);
        for (SensorLocalizacao localizacao : SensorLocalizacao.values()) {
            mqttClientManager.registerCommandHandler(localizacao, comando -> processarComandoRemoto(localizacao, comando));
        }
        mqttClientManager.connectAsync();
    }

    private void criarSensores() {
        for (SensorLocalizacao localizacao : SensorLocalizacao.values()) {
            SensorAbstrato sensor = criarSensorPorLocalizacao(localizacao);
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
            SensorSnapshot snapshot = new SensorSnapshot(sensor.getIDUnico(),
                    sensor.getLocalizacao().descricao(),
                    sensor.getTipo(),
                    dados.valor(),
                    dados.unidade(),
                    dados.alerta(),
                    dados.timestamp());
            snapshotObservers.forEach(observer -> observer.accept(snapshot));
            log(String.format("Sensor %s publicou %s", sensor.getIDUnico(), formatValor(sensor.getTipo(), dados)));
        };
    }

    public void registerSnapshotObserver(Consumer<SensorSnapshot> observer) {
        snapshotObservers.add(observer);
    }

    public void registerLogObserver(Consumer<String> observer) {
        logObservers.add(observer);
    }

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
