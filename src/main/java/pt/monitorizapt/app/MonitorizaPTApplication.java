package pt.monitorizapt.app;

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
            // allows dynamic broker URL via command line: -Dmonitorizapt.broker="tcp://..."
            String brokerUrl = System.getProperty("monitorizapt.broker", "tcp://broker.hivemq.com:1883");

            // Initialize infrastructure (MQTT) and Logic (Controller) before the UI
            MqttClientManager mqttClientManager = new MqttClientManager(brokerUrl);
            SensorController controller = new SensorController(mqttClientManager);
            
            MonitorizaPTFrame frame = new MonitorizaPTFrame(controller, mqttClientManager);
            frame.setVisible(true);
        });
    }
}