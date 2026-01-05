package pt.monitorizapt.domain;

/**
 * Observer that reacts whenever a sensor publishes a payload.
 */
@FunctionalInterface
public interface SensorUpdateListener {
    void onDadosPublicados(Sensor sensor, DadosSensor dados, String jsonPayload);
}
