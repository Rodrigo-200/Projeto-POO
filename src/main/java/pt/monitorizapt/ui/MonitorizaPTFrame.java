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
import javax.swing.JOptionPane;
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
    private static final int MAX_LOG_LINES = 1000;

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
        // We use DO_NOTHING_ON_CLOSE because we want to show a confirmation dialog first
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(900, 650);
        
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

        // Ensures threads stop when the window is closed, with the user confirmation
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmarESair();
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
        // Override JTable to inject custom rendering logic based on cell data
        JTable tabela = new JTable(tableModel) {
            @Override
            public java.awt.Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                java.awt.Component c = super.prepareRenderer(renderer, row, column);

                // We must convert the view row index to the model index in case sorting is enabled later
                int modelRow = convertRowIndexToModel(row);
                
                // Fetch the 'Alerta' status from column index 4
                String statusAlerta = (String) getModel().getValueAt(modelRow, 4);

                // Visual feedback for critical states
                if ("ALERTA".equals(statusAlerta)) {
                    c.setBackground(new Color(255, 200, 200)); 
                    c.setForeground(Color.RED.darker());
                } else {
                    // Reset to default colors when status is normal
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
                
                // Preserve the default selection highlight 
                if (isRowSelected(row)) {
                    c.setBackground(getSelectionBackground());
                    c.setForeground(getSelectionForeground());
                }

                return c;
            }
        };
        
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
        // Data comes from background threads (Sensors/MQTT).
        // Swing isn't thread-safe so we need to use invokeLater to update the UI components
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
        // prevent the log area from growing infinitely to protect memory overflow
        if (logArea.getLineCount() > MAX_LOG_LINES) {
            logArea.setText("");
            logArea.append("[SISTEMA] Logs antigos limpos para libertar memória.\n");
        }
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
        
        // Input Validation: Ensure the interval is a valid number and safe
        long intervalo;
        try {
            intervalo = Long.parseLong(intervaloField.getText().trim());
            if (intervalo < 1000) {
                throw new IllegalArgumentException("Intervalo muito curto (min: 1000ms)");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Por favor insira um número válido para o intervalo.", 
                "Erro de Input", 
                JOptionPane.ERROR_MESSAGE);
            return;
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, 
                e.getMessage(), 
                "Aviso de Segurança", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        controller.ativarLocalizacao(localizacao, intervalo);
    }

    private void pararSensorSelecionado() {
        SensorLocalizacao localizacao = (SensorLocalizacao) localizacaoCombo.getSelectedItem();
        controller.desativarLocalizacao(localizacao);
    }

    private void confirmarESair() {
        int resposta = JOptionPane.showConfirmDialog(this,
                "Tem a certeza que deseja encerrar a monitorização?",
                "Sair",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (resposta == JOptionPane.YES_OPTION) {
            controller.shutdown();
            dispose();
            System.exit(0);
        }
    }
}