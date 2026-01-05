package pt.monitorizapt.sensors;

import pt.monitorizapt.domain.DadosSensor;
import pt.monitorizapt.domain.SensorAbstrato;
import pt.monitorizapt.domain.SensorLocalizacao;
import pt.monitorizapt.domain.SensorTipo;
import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.util.JsonPayloadBuilder;

import java.util.concurrent.ThreadLocalRandom;

public class SensorTemperatura extends SensorAbstrato {
    public SensorTemperatura(SensorLocalizacao localizacao,
                             JsonPayloadBuilder payloadBuilder,
                             MqttClientManager mqttClientManager) {
        super(SensorTipo.TEMPERATURA, localizacao, payloadBuilder, mqttClientManager);
    }

    @Override
    protected DadosSensor gerarDadosEspecificos() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double valor = random.nextDouble(16.0, 31.5);
        if (random.nextDouble() < 0.08) {
            valor = random.nextDouble(-45.0, -5.0);
        } else if (random.nextDouble() < 0.12) {
            valor = random.nextDouble(31.5, 48.0);
        }
        boolean alerta = valor > 30.0;
        return new DadosSensor(valor, SensorTipo.TEMPERATURA.unidadePadrao(), alerta, System.currentTimeMillis());
    }
}
