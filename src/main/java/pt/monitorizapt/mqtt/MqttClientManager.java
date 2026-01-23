package pt.monitorizapt.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import pt.monitorizapt.domain.SensorLocalizacao;

/**
 * Manages the MQTT connection lifecycle.
 * Wraps the Eclipse Paho library to provide a simpler, async API for the rest of the app.
 */
public class MqttClientManager {
    private final String brokerUrl;
    // Unique ID prevents the broker from kicking us out if another client has the same name
    // RM = Rodrigo Martins
    private final String clientId = "MonitorizaPT_RM_" + UUID.randomUUID();
    
    // Thread-safe map to store command handlers (Topic -> Action)
    private final Map<String, Consumer<String>> commandHandlers = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<Boolean>> connectionListeners = new CopyOnWriteArrayList<>();

    private MqttClient client;

    public MqttClientManager(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    /**
     * Connects to the broker asynchronously.
     * Crucial: We use CompletableFuture to avoid freezing the UI during network operations.
     */
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
        // MemoryPersistence is used because we don't need to save messages to disk if the app crashes
        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        
        // Callback to handle connection events (Loss/Recovery)
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                notifyConnection(true);
                // If we reconnected automatically, we must re-subscribe to topics
                reapplySubscriptions();
            }

            @Override
            public void connectionLost(Throwable cause) {
                notifyConnection(false);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                // Handled individually via subscribeInternal listeners
            }

            @Override
            public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
            }
        });

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true); // Paho tries to reconnect automatically
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);

        client.connect(options);
        notifyConnection(true);
        reapplySubscriptions();
    }

    /**
     * Ensures that if the connection drops and comes back, we start listening 
     * to the command topics again.
     */
    private void reapplySubscriptions() {
        commandHandlers.forEach((topic, handler) -> subscribeInternal(topic, handler));
    }

    public void publish(String topic, String payload) {
        try {
            if (client == null || !client.isConnected()) {
                connectAsync(); // Try to reconnect if disconnected
                return;
            }
            // QoS 0 (Fire and forget) is sufficient for sensor data
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
        // Listener that triggers the specific handler when a message arrives on this topic
        IMqttMessageListener listener = (receivedTopic, message) -> 
            handler.accept(new String(message.getPayload(), StandardCharsets.UTF_8));
        
        try {
            client.subscribe(topic, listener);
        } catch (MqttException ignored) {
        }
    }

    /**
     * Async check used by the "Test Connection" button in the UI.
     */
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