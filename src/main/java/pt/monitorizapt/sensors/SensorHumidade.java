package pt.monitorizapt.sensors;

import java.util.concurrent.ThreadLocalRandom;

import pt.monitorizapt.domain.DadosSensor;
import pt.monitorizapt.domain.SensorAbstrato;
import pt.monitorizapt.domain.SensorLocalizacao;
import pt.monitorizapt.domain.SensorTipo;
import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.util.JsonPayloadBuilder;

public class SensorHumidade extends SensorAbstrato {
    public SensorHumidade(SensorLocalizacao localizacao,
                          JsonPayloadBuilder payloadBuilder,
                          MqttClientManager mqttClientManager) {
        super(SensorTipo.HUMIDADE, localizacao, payloadBuilder, mqttClientManager);
    }

    @Override
    protected DadosSensor gerarDadosEspecificos() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Normal range for humidity
        double valor = random.nextDouble(45.0, 78.0);
        
        // Simulate sensor errors or extreme weather
        if (random.nextDouble() < 0.10) {
            valor = random.nextDouble(0.0, 15.0); // Too dry
        } else if (random.nextDouble() < 0.10) {
            valor = random.nextDouble(81.0, 95.0); // Too humid
        }
        
        boolean alerta = valor > 80.0;
        return new DadosSensor(valor, SensorTipo.HUMIDADE.unidadePadrao(), alerta, System.currentTimeMillis());
    }
}