package pt.monitorizapt.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import pt.monitorizapt.service.SensorSnapshot;

/**
 * Custom table model to display sensor snapshots.
 * Uses a LinkedHashMap to keep rows ordered by insertion but allow fast updates by ID.
 */
public class SensorTableModel extends AbstractTableModel {
    private static final String[] COLUNAS = {"ID", "Localização", "Tipo", "Valor Atual", "Alerta"};
    private final LinkedHashMap<String, SensorSnapshot> linhas = new LinkedHashMap<>();

    @Override
    public int getRowCount() {
        return linhas.size();
    }

    @Override
    public int getColumnCount() {
        return COLUNAS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUNAS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SensorSnapshot snapshot = getSnapshot(rowIndex);
        return switch (columnIndex) {
            case 0 -> snapshot.id();
            case 1 -> snapshot.localizacao();
            case 2 -> snapshot.tipo().etiqueta();
            case 3 -> String.format("%.2f %s", snapshot.valor(), snapshot.unidade());
            case 4 -> snapshot.alerta() ? "ALERTA" : "OK";
            default -> "";
        };
    }

    /**
     * Update or Insert (Upsert) a sensor row.
     */
    public void upsert(SensorSnapshot snapshot) {
        boolean existente = linhas.containsKey(snapshot.id());
        linhas.put(snapshot.id(), snapshot);
        int rowIndex = indexOf(snapshot.id());
        
        if (existente && rowIndex >= 0) {
            // Efficiently notifies the view that only this specific row changed
            fireTableRowsUpdated(rowIndex, rowIndex);
        } else {
            // Notify that structure might have changed (new row)
            fireTableDataChanged();
        }
    }

    private int indexOf(String id) {
        int idx = 0;
        for (String key : linhas.keySet()) {
            if (key.equals(id)) {
                return idx;
            }
            idx++;
        }
        return -1;
    }

    private SensorSnapshot getSnapshot(int rowIndex) {
        int idx = 0;
        for (Map.Entry<String, SensorSnapshot> entry : linhas.entrySet()) {
            if (idx == rowIndex) {
                return entry.getValue();
            }
            idx++;
        }
        throw new IndexOutOfBoundsException("Linha inexistente: " + rowIndex);
    }
}