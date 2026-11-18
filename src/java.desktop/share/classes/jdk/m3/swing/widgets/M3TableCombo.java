/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * A native Swing multi-column table combo inspired by Nebula TableCombo.
 * Accepts JTable or SwingX JXTable without copying rows or replacing sorters,
 * renderers, column models, listeners, or the underlying data model.
 * All row coordinates exposed by this API are <em>model</em> indices.
 */
public final class M3TableCombo extends M3PopupSelector<JTable> {
    private static final long serialVersionUID = 1L;
    private final JTable table;
    private int displayModelColumn;
    // Preserve this existing serialized field; its interface does not extend Serializable.
    @SuppressWarnings("serial")
    private ListSelectionModel observedSelection;
    // Preserve this existing serialized field; its interface does not extend Serializable.
    @SuppressWarnings("serial")
    private TableModel observedModel;
    // Preserve this existing serialized field; its interface does not extend Serializable.
    @SuppressWarnings("serial")
    private final ListSelectionListener selectionListener = event -> {
        if (!event.getValueIsAdjusting()) refreshText();
    };
    // Preserve this existing serialized field; its interface does not extend Serializable.
    @SuppressWarnings("serial")
    private final TableModelListener dataListener = event -> refreshText();

    public M3TableCombo(TableModel model) { this(new JTable(Objects.requireNonNull(model, "model"))); }
    public M3TableCombo(JTable table) {
        super(table);
        this.table = table;
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trackSelection();
        trackModel();
        table.addPropertyChangeListener("selectionModel", e -> {
            trackSelection();
            refreshText();
        });
        table.addPropertyChangeListener("model", e -> {
            trackModel();
            refreshText();
        });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1 && table.rowAtPoint(event.getPoint()) >= 0) {
                    commitSelection();
                }
            }
        });
        table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ENTER"), "m3.commit");
        table.getActionMap().put("m3.commit", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent event) { commitSelection(); }
        });
        refreshText();
    }

    private void trackSelection() {
        ListSelectionModel current = table.getSelectionModel();
        if (observedSelection == current) return;
        if (observedSelection != null) observedSelection.removeListSelectionListener(selectionListener);
        observedSelection = current;
        current.addListSelectionListener(selectionListener);
    }

    private void trackModel() {
        TableModel current = table.getModel();
        if (observedModel == current) return;
        if (observedModel != null) observedModel.removeTableModelListener(dataListener);
        observedModel = current;
        current.addTableModelListener(dataListener);
    }

    public JTable getTable() { return table; }
    public int getDisplayModelColumn() { return displayModelColumn; }

    public void setDisplayModelColumn(int column) {
        requireEdt();
        if (column < 0 || column >= table.getModel().getColumnCount()) {
            throw new IndexOutOfBoundsException("model column: " + column);
        }
        displayModelColumn = column;
        refreshText();
    }

    /** A row filtered out by JTable's current RowSorter cannot be selected. */
    public boolean selectModelRow(int modelRow) {
        requireEdt();
        if (modelRow == -1) {
            table.clearSelection();
            refreshText();
            return true;
        }
        if (modelRow < 0 || modelRow >= table.getModel().getRowCount()) {
            throw new IndexOutOfBoundsException("model row: " + modelRow);
        }
        int viewRow = table.convertRowIndexToView(modelRow);
        if (viewRow < 0) return false;
        table.getSelectionModel().setSelectionInterval(viewRow, viewRow);
        refreshText();
        return true;
    }

    public int getSelectedModelRow() {
        int row = table.getSelectedRow();
        return row < 0 || row >= table.getRowCount() ? -1 : table.convertRowIndexToModel(row);
    }

    /** Dispatches a user-style Selection event for the already highlighted row. */
    public void commitSelection() {
        requireEdt();
        if (table.getSelectedRow() >= 0) {
            refreshText();
            fireSelection();
        }
    }

    public void refreshText() {
        requireEdt();
        int modelRow = getSelectedModelRow();
        TableModel model = table.getModel();
        if (modelRow < 0 || modelRow >= model.getRowCount() || displayModelColumn >= model.getColumnCount()) {
            display("");
            return;
        }
        Object value = model.getValueAt(modelRow, displayModelColumn);
        display(value == null ? "" : value.toString());
    }

    @Override protected int rowCount() { return table.getRowCount(); }
    @Override protected int rowHeight() { return table.getRowHeight(); }
}
