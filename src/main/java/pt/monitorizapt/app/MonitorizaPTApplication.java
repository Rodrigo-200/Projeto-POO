package pt.monitorizapt.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.SwingUtilities;

import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.service.SensorController;
import pt.monitorizapt.ui.MonitorizaPTFrame;

public final class MonitorizaPTApplication {
    private MonitorizaPTApplication() {
    }

    public static void main(String[] args) {
        // Swing UI must be initialized on the Event Dispatch Thread to avoid concurrency issues.
        SwingUtilities.invokeLater(() -> {
            
            // Load settings from external file, fallback to defaults
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream("config.properties")) {
                props.load(fis);
            } catch (IOException e) {
                System.out.println("Config file not found. Using internal defaults.");
            }

            // Priority: 1. System Property (-D) -> 2. config.properties -> 3. Hardcoded Default
            String defaultBroker = props.getProperty("broker.url", "tcp://broker.hivemq.com:1883");
            String brokerUrl = System.getProperty("monitorizapt.broker", defaultBroker);

            // Initialize infrastructure (MQTT) and Logic (Controller) before the UI
            MqttClientManager mqttClientManager = new MqttClientManager(brokerUrl);
            SensorController controller = new SensorController(mqttClientManager);
            
            MonitorizaPTFrame frame = new MonitorizaPTFrame(controller, mqttClientManager);
            frame.setVisible(true);
        });
    }
}