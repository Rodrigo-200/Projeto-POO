package pt.monitorizapt.sensors;

import java.util.concurrent.ThreadLocalRandom;

import pt.monitorizapt.domain.DadosSensor;
import pt.monitorizapt.domain.SensorAbstrato;
import pt.monitorizapt.domain.SensorLocalizacao;
import pt.monitorizapt.domain.SensorTipo;
import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.util.JsonPayloadBuilder;

public class SensorQualidadeAr extends SensorAbstrato {
    public SensorQualidadeAr(SensorLocalizacao localizacao,
                             JsonPayloadBuilder payloadBuilder,
                             MqttClientManager mqttClientManager) {
        super(SensorTipo.QUALIDADE_AR, localizacao, payloadBuilder, mqttClientManager);
    }

    @Override
    protected DadosSensor gerarDadosEspecificos() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double valor = random.nextDouble(5.0, 45.0);
        
        // 15% chance of pollution spike
        if (random.nextDouble() < 0.15) {
            valor = random.nextDouble(51.0, 120.0);
        } else if (random.nextDouble() < 0.05) {
            valor = random.nextDouble(-10.0, 0.0); // Simulation of hardware error
        }
        
        boolean alerta = valor > 50.0;
        return new DadosSensor(valor, SensorTipo.QUALIDADE_AR.unidadePadrao(), alerta, System.currentTimeMillis());
    }
}