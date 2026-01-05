package pt.monitorizapt.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import pt.monitorizapt.domain.SensorLocalizacao;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class MqttClientManager {
    private final String brokerUrl;
    private final String clientId = "MonitorizaPT-" + UUID.randomUUID();
    private final Map<String, Consumer<String>> commandHandlers = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<Boolean>> connectionListeners = new CopyOnWriteArrayList<>();

    private MqttClient client;

    public MqttClientManager(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public CompletableFuture<Void> connectAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                connectInternal();
            } catch (MqttException e) {
                notifyConnection(false);
            }
        });
    }

    private synchronized void connectInternal() throws MqttException {
        if (client != null && client.isConnected()) {
            return;
        }
        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                notifyConnection(true);
                reapplySubscriptions();
            }

            @Override
            public void connectionLost(Throwable cause) {
                notifyConnection(false);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                // handled via listener registration
            }

            @Override
            public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
            }
        });

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);

        client.connect(options);
        notifyConnection(true);
        reapplySubscriptions();
    }

    private void reapplySubscriptions() {
        commandHandlers.forEach((topic, handler) -> subscribeInternal(topic, handler));
    }

    public void publish(String topic, String payload) {
        try {
            if (client == null || !client.isConnected()) {
                connectAsync();
                return;
            }
            client.publish(topic, payload.getBytes(StandardCharsets.UTF_8), 0, false);
        } catch (MqttException ignored) {
        }
    }

    public void registerCommandHandler(SensorLocalizacao localizacao, Consumer<String> handler) {
        String topic = localizacao.topicoComandos();
        commandHandlers.put(topic, handler);
        subscribeInternal(topic, handler);
    }

    private void subscribeInternal(String topic, Consumer<String> handler) {
        if (client == null || !client.isConnected()) {
            return;
        }
        IMqttMessageListener listener = (receivedTopic, message) -> handler.accept(new String(message.getPayload(), StandardCharsets.UTF_8));
        try {
            client.subscribe(topic, listener);
        } catch (MqttException ignored) {
        }
    }

    public CompletableFuture<Boolean> testConnectionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (isConnected()) {
                return true;
            }
            try {
                connectInternal();
                return true;
            } catch (MqttException e) {
                notifyConnection(false);
                return false;
            }
        });
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public void registerConnectionListener(Consumer<Boolean> listener) {
        connectionListeners.add(listener);
    }

    private void notifyConnection(boolean connected) {
        connectionListeners.forEach(listener -> listener.accept(connected));
    }

    public synchronized void shutdown() {
        if (client != null) {
            try {
                client.disconnectForcibly(1000, 1000);
                client.close(true);
            } catch (MqttException ignored) {
            }
        }
    }
}
