package pt.monitorizapt.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CompletableFuture;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import pt.monitorizapt.domain.SensorLocalizacao;
import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.service.SensorController;
import pt.monitorizapt.service.SensorSnapshot;

/**
 * The main View class.
 * It initializes the visual components and wires up the events from the Controller.
 */
public class MonitorizaPTFrame extends JFrame {
    private final SensorController controller;
    private final MqttClientManager mqttClientManager;
    private final SensorTableModel tableModel = new SensorTableModel();

    // UI Components
    private final JLabel estadoMqttLabel = new JLabel("Estado MQTT: VERMELHO");
    private final JButton testarBrokerButton = new JButton("TESTAR BROKER");
    private final JComboBox<SensorLocalizacao> localizacaoCombo = new JComboBox<>(SensorLocalizacao.values());
    private final JLabel intervaloLabel = new JLabel("Intervalo (ms):");
    private final JTextField intervaloField = new JTextField("3333", 6);
    private final JButton iniciarButton = new JButton("INICIAR");
    private final JButton pararButton = new JButton("PARAR");
    private final JButton limparLogsButton = new JButton("LIMPAR LOGS");
    private final JTextArea logArea = new JTextArea();

    public MonitorizaPTFrame(SensorController controller, MqttClientManager mqttClientManager) {
        super("MonitorizaPT - Sensores Ambientais v1.0");
        this.controller = controller;
        this.mqttClientManager = mqttClientManager;
        configurarJanela();
        registrarCallbacks();
    }

    private void configurarJanela() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        // BorderLayout divides container into North, South, East, West, Center
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null); // Centers on screen

        add(criarPainelNorte(), BorderLayout.NORTH);
        add(criarTabela(), BorderLayout.CENTER);
        add(criarPainelSul(), BorderLayout.SOUTH);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Ensures threads stop cleanly when the window is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.shutdown();
            }
        });
    }

    private JPanel criarPainelNorte() {
        JPanel painel = new JPanel(new BorderLayout(10, 0));
        estadoMqttLabel.setOpaque(true);
        estadoMqttLabel.setBackground(Color.RED.darker());
        estadoMqttLabel.setForeground(Color.WHITE);
        estadoMqttLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        painel.add(estadoMqttLabel, BorderLayout.WEST);
        painel.add(testarBrokerButton, BorderLayout.EAST);
        return painel;
    }

    private JScrollPane criarTabela() {
        JTable tabela = new JTable(tableModel);
        tabela.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(tabela);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return scrollPane;
    }

    private JPanel criarPainelSul() {
        JPanel painelSul = new JPanel(new BorderLayout(10, 10));

        JPanel painelControlo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        painelControlo.add(localizacaoCombo);
        painelControlo.add(intervaloLabel);
        intervaloField.setPreferredSize(new Dimension(80, 24));
        painelControlo.add(intervaloField);
        painelControlo.add(iniciarButton);
        painelControlo.add(pararButton);
        painelControlo.add(limparLogsButton);

        painelSul.add(painelControlo, BorderLayout.NORTH);
        JScrollPane scrollLogs = new JScrollPane(logArea);
        scrollLogs.setPreferredSize(new Dimension(100, 150));
        painelSul.add(scrollLogs, BorderLayout.CENTER);
        return painelSul;
    }

    /**
     * Connects UI events to Controller methods and listens for updates.
     */
    private void registrarCallbacks() {
        // CRITICAL: Data comes from background threads (Sensors/MQTT).
        // Swing is NOT thread-safe. We must use invokeLater to update UI components
        // on the Event Dispatch Thread (EDT).
        controller.registerSnapshotObserver(snapshot -> SwingUtilities.invokeLater(() -> atualizarTabela(snapshot)));
        controller.registerLogObserver(log -> SwingUtilities.invokeLater(() -> appendLog(log)));
        mqttClientManager.registerConnectionListener(conectado -> SwingUtilities.invokeLater(() -> atualizarEstado(conectado)));

        testarBrokerButton.addActionListener(event -> testarBroker());
        iniciarButton.addActionListener(event -> iniciarSensorSelecionado());
        pararButton.addActionListener(event -> pararSensorSelecionado());
        limparLogsButton.addActionListener(event -> logArea.setText(""));
    }

    private void atualizarTabela(SensorSnapshot snapshot) {
        tableModel.upsert(snapshot);
    }

    private void appendLog(String linha) {
        logArea.append(linha + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength()); // Auto-scroll
    }

    private void atualizarEstado(boolean conectado) {
        if (conectado) {
            estadoMqttLabel.setText("Estado MQTT: VERDE");
            estadoMqttLabel.setBackground(new Color(0, 128, 0));
        } else {
            estadoMqttLabel.setText("Estado MQTT: VERMELHO");
            estadoMqttLabel.setBackground(Color.RED.darker());
        }
    }

    private void testarBroker() {
        testarBrokerButton.setEnabled(false);
        // Perform network test asynchronously to prevent UI freezing
        CompletableFuture<Boolean> resultado = mqttClientManager.testConnectionAsync();
        resultado.whenComplete((conectado, erro) -> SwingUtilities.invokeLater(() -> {
            testarBrokerButton.setEnabled(true);
            if (erro != null) {
                appendLog("Falha ao testar broker: " + erro.getMessage());
                atualizarEstado(false);
            } else {
                appendLog(conectado ? "Broker disponível" : "Broker indisponível");
                atualizarEstado(conectado);
            }
        }));
    }

    private void iniciarSensorSelecionado() {
        SensorLocalizacao localizacao = (SensorLocalizacao) localizacaoCombo.getSelectedItem();
        long intervalo = lerIntervalo();
        controller.ativarLocalizacao(localizacao, intervalo);
    }

    private void pararSensorSelecionado() {
        SensorLocalizacao localizacao = (SensorLocalizacao) localizacaoCombo.getSelectedItem();
        controller.desativarLocalizacao(localizacao);
    }

    private long lerIntervalo() {
        try {
            return Long.parseLong(intervaloField.getText().trim());
        } catch (NumberFormatException ex) {
            intervaloField.setText("3333");
            return 3333L;
        }
    }
}