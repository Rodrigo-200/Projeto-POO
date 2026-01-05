package pt.monitorizapt.sensors;

import pt.monitorizapt.domain.DadosSensor;
import pt.monitorizapt.domain.SensorAbstrato;
import pt.monitorizapt.domain.SensorLocalizacao;
import pt.monitorizapt.domain.SensorTipo;
import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.util.JsonPayloadBuilder;

import java.util.concurrent.ThreadLocalRandom;

public class SensorHumidade extends SensorAbstrato {
    public SensorHumidade(SensorLocalizacao localizacao,
                          JsonPayloadBuilder payloadBuilder,
                          MqttClientManager mqttClientManager) {
        super(SensorTipo.HUMIDADE, localizacao, payloadBuilder, mqttClientManager);
    }

    @Override
    protected DadosSensor gerarDadosEspecificos() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double valor = random.nextDouble(45.0, 78.0);
        if (random.nextDouble() < 0.10) {
            valor = random.nextDouble(0.0, 15.0);
        } else if (random.nextDouble() < 0.10) {
            valor = random.nextDouble(81.0, 95.0);
        }
        boolean alerta = valor > 80.0;
        return new DadosSensor(valor, SensorTipo.HUMIDADE.unidadePadrao(), alerta, System.currentTimeMillis());
    }
}
