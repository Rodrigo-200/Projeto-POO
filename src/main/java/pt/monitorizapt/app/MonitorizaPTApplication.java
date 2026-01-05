package pt.monitorizapt.app;

import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.service.SensorController;
import pt.monitorizapt.ui.MonitorizaPTFrame;

import javax.swing.SwingUtilities;

public final class MonitorizaPTApplication {
    private MonitorizaPTApplication() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String brokerUrl = System.getProperty("monitorizapt.broker", "tcp://broker.hivemq.com:1883");
            MqttClientManager mqttClientManager = new MqttClientManager(brokerUrl);
            SensorController controller = new SensorController(mqttClientManager);
            MonitorizaPTFrame frame = new MonitorizaPTFrame(controller, mqttClientManager);
            frame.setVisible(true);
        });
    }
}
