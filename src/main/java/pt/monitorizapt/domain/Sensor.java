package pt.monitorizapt.domain;

/**
 * Interface defining the mandatory behavior for all environmental sensors.
 */
public interface Sensor {
    String getIDUnico();

    DadosSensor lerDados();

    void publicarMQTT(String json);

    // Handles remote commands (e.g. "ATIVAR", "DESATIVAR")
    void processarComando(String comandoJSON);

    String getOwner();

    SensorTipo getTipo();

    SensorLocalizacao getLocalizacao();

    boolean isAtivo();

    void ativar();

    void desativar();

    long getIntervaloMillis();

    void setIntervaloMillis(long intervaloMillis);

    // Observer pattern: allows UI to react to new data
    void registrarListener(SensorUpdateListener listener);

    void removerListener(SensorUpdateListener listener);

    // Thread lifecycle management
    void iniciar();

    void desligar();
}