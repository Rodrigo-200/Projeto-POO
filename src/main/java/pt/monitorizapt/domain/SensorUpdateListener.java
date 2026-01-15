package pt.monitorizapt.domain;

/**
 * Observer interface.
 * Any class that wants to react when a sensor publishes data (like the UI or Logger)
 * must implement this method.
 */
@FunctionalInterface
public interface SensorUpdateListener {
    void onDadosPublicados(Sensor sensor, DadosSensor dados, String jsonPayload);
}