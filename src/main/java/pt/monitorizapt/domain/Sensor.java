package pt.monitorizapt.domain;

public interface Sensor {
    String getIDUnico();

    DadosSensor lerDados();

    void publicarMQTT(String json);

    void processarComando(String comandoJSON);

    String getOwner();

    SensorTipo getTipo();

    SensorLocalizacao getLocalizacao();

    boolean isAtivo();

    void ativar();

    void desativar();

    long getIntervaloMillis();

    void setIntervaloMillis(long intervaloMillis);

    void registrarListener(SensorUpdateListener listener);

    void removerListener(SensorUpdateListener listener);

    void iniciar();

    void desligar();
}
