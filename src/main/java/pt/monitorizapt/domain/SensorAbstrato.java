package pt.monitorizapt.domain;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.util.JsonPayloadBuilder;

public abstract class SensorAbstrato implements Sensor, Runnable {
    private static final long INTERVALO_PADRAO = 3333L;
    private static final String OWNER_IDENTIFICADOR = "Rodrigo_Martins_a22508678";
    private static final Gson GSON = new Gson();

    private final SensorTipo tipo;
    private final SensorLocalizacao localizacaoFixa;
    private final JsonPayloadBuilder payloadBuilder;
    private final MqttClientManager mqttClientManager;
    private final List<SensorUpdateListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean loopAtivo = new AtomicBoolean(false);

    private volatile boolean ativo;
    private volatile long intervaloMillis = INTERVALO_PADRAO;
    private volatile DadosSensor ultimaLeitura;
    private Thread worker;

    protected SensorAbstrato(SensorTipo tipo,
                             SensorLocalizacao localizacaoFixa,
                             JsonPayloadBuilder payloadBuilder,
                             MqttClientManager mqttClientManager) {
        this.tipo = tipo;
        this.localizacaoFixa = localizacaoFixa;
        this.payloadBuilder = payloadBuilder;
        this.mqttClientManager = mqttClientManager;
    }

    protected abstract DadosSensor gerarDadosEspecificos();

    @Override
    public final DadosSensor lerDados() {
        ultimaLeitura = gerarDadosEspecificos();
        return ultimaLeitura;
    }

    @Override
    public final void publicarMQTT(String json) {
        if (mqttClientManager != null) {
            mqttClientManager.publish(localizacaoFixa.topicoDados(), json);
        }
    }

    @Override
    public void processarComando(String comandoJSON) {
        try {
            JsonObject objeto = GSON.fromJson(comandoJSON, JsonObject.class);
            if (objeto == null || !objeto.has("acao")) {
                return;
            }
            String acao = objeto.get("acao").getAsString().toUpperCase(Locale.ROOT);
            switch (acao) {
                case "ATIVAR" -> ativar();
                case "DESATIVAR" -> desativar();
                default -> {
                }
            }
            if (objeto.has("intervalo")) {
                long novoIntervalo = objeto.get("intervalo").getAsLong();
                if (novoIntervalo >= 1000L) {
                    setIntervaloMillis(novoIntervalo);
                }
            }
        } catch (JsonParseException | IllegalStateException ignored) {
        }
    }

    @Override
    public final String getOwner() {
        return OWNER_IDENTIFICADOR;
    }

    @Override
    public final SensorTipo getTipo() {
        return tipo;
    }

    @Override
    public final SensorLocalizacao getLocalizacao() {
        return localizacaoFixa;
    }

    @Override
    public final boolean isAtivo() {
        return ativo;
    }

    @Override
    public final void ativar() {
        this.ativo = true;
    }

    @Override
    public final void desativar() {
        this.ativo = false;
    }

    @Override
    public final long getIntervaloMillis() {
        return intervaloMillis;
    }

    @Override
    public final void setIntervaloMillis(long intervaloMillis) {
        this.intervaloMillis = Math.max(1000L, intervaloMillis);
    }

    @Override
    public final void registrarListener(SensorUpdateListener listener) {
        listeners.add(listener);
    }

    @Override
    public final void removerListener(SensorUpdateListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void iniciar() {
        if (loopAtivo.compareAndSet(false, true)) {
            worker = new Thread(this, getIDUnico() + "-loop");
            worker.setDaemon(true);
            worker.start();
        }
    }

    @Override
    public void desligar() {
        loopAtivo.set(false);
        if (worker != null) {
            worker.interrupt();
        }
    }

    @Override
    public String getIDUnico() {
        return "PT-SENSOR-" + localizacaoFixa.idSegmento();
    }

    @Override
    public void run() {
        while (true) {
            if (!loopAtivo.get()) {
                break;
            }
            try {
                if (ativo) {
                    DadosSensor leitura = lerDados();
                    String payload = payloadBuilder.buildPayload(this, leitura);
                    publicarMQTT(payload);
                    notificar(leitura, payload);
                }
                Thread.sleep(intervaloMillis); // requirement: while(true) + sleep(3333)
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                // Keep loop alive even if a single publication fails.
            }
        }
    }

    protected final DadosSensor ultimaLeitura() {
        return ultimaLeitura;
    }

    protected final void notificar(DadosSensor dados, String payload) {
        listeners.forEach(listener -> listener.onDadosPublicados(this, dados, payload));
    }
}
