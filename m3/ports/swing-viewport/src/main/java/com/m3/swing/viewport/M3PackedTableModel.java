/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.util.Arrays;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import com.m3.swing.viewport.engine.PackedHeapGrowth;

/**
 * {@link javax.swing.table.TableModel} with the row and column API of
 * {@link javax.swing.table.DefaultTableModel} over columnar packed storage
 * ({@link M3PackedColumn}): primitive arrays for int, long, double and boolean cells, a UTF-16 pool
 * for strings, {@code Object[]} for anything else. Every read returns a value equal to the one
 * written (identical for non-primitive, non-string objects). Events match what
 * {@code DefaultTableModel} fires for the same call.
 *
 * <p>{@code getColumnClass} returns {@code Object.class}, as {@code DefaultTableModel} does, unless
 * the model was built with {@link #REPORT_COLUMN_KINDS}; then a column holding only ints reports
 * {@code Integer.class} and so on.</p>
 *
 * <p>Batches ({@link #beginBatch()} … {@link #endBatch()}) coalesce adjacent row inserts and
 * merge updates into one hull event, the debounce rule for data storms.</p>
 */
//FLLDCIM: L2 — columnar packed table; L6 — paint reads chars, never getValueAt
public class M3PackedTableModel extends AbstractTableModel implements M3PackedCells {
	private static final long serialVersionUID = 1L;
	/** Construction flag: {@code getColumnClass} reports the packed kind of each column. */
	public static final int REPORT_COLUMN_KINDS = 1;
	private static final int NONE = Integer.MIN_VALUE;

	private final int flags;
	private M3PackedColumn [] columns = new M3PackedColumn [0];
	private Object [] identifiers = new Object [0];
	private int columnCount, rowCount;
	private int batchDepth, pendingType = NONE, pendingFirst, pendingLast, pendingColumn;

	public M3PackedTableModel () {
		this (0, 0, 0);
	}

	public M3PackedTableModel (int rowCount, int columnCount) {
		this (rowCount, columnCount, 0);
	}

	public M3PackedTableModel (int rowCount, int columnCount, int flags) {
		this.flags = flags;
		this.rowCount = rowCount;
		setColumnCountQuiet (columnCount);
	}

	public M3PackedTableModel (Object [] columnNames, int rowCount) {
		this (rowCount, 0, 0);
		setIdentifiersQuiet (columnNames);
	}

	public M3PackedTableModel (Object [][] data, Object [] columnNames) {
		this (0, 0, 0);
		setDataVectorQuiet (data, columnNames);
	}

	// ---- TableModel ------------------------------------------------------------------------------

	@Override
	public int getRowCount () {
		return rowCount;
	}

	@Override
	public int getColumnCount () {
		return columnCount;
	}

	@Override
	public String getColumnName (int column) {
		Object id = column >= 0 && column < identifiers.length ? identifiers [column] : null;
		return id == null ? super.getColumnName (column) : id.toString ();
	}

	@Override
	public Class<?> getColumnClass (int column) {
		if ((flags & REPORT_COLUMN_KINDS) == 0) return Object.class;
		switch (columns [column].kind ()) {
			case M3PackedColumn.INT: return Integer.class;
			case M3PackedColumn.LONG: return Long.class;
			case M3PackedColumn.DOUBLE: return Double.class;
			case M3PackedColumn.BOOLEAN: return Boolean.class;
			case M3PackedColumn.STRING: return String.class;
			default: return Object.class;
		}
	}

	@Override
	public boolean isCellEditable (int row, int column) {
		return true;
	}

	@Override
	public Object getValueAt (int row, int column) {
		checkRow (row);
		return column (column).get (row);
	}

	@Override
	public void setValueAt (Object aValue, int row, int column) {
		checkRow (row);
		column (column).set (row, aValue);
		fire (TableModelEvent.UPDATE, row, row, column);
	}

	// ---- M3PackedCells ---------------------------------------------------------------------------

	@Override
	public int cellChars (int row, int column, char [] dst) {
		return columns [column].chars (row, dst);
	}

	// ---- DefaultTableModel row API ---------------------------------------------------------------

	public void addRow (Object [] rowData) {
		insertRow (rowCount, rowData);
	}

	public void addRow (List<?> rowData) {
		insertRow (rowCount, rowData == null ? null : rowData.toArray ());
	}

	public void insertRow (int row, Object [] rowData) {
		if (row < 0 || row > rowCount) throw new ArrayIndexOutOfBoundsException (row + " > " + rowCount);
		for (int c = 0; c < columnCount; c++) {
			columns [c].insert (row, 1);
			if (rowData != null && c < rowData.length) columns [c].set (row, rowData [c]);
		}
		rowCount++;
		fire (TableModelEvent.INSERT, row, row, TableModelEvent.ALL_COLUMNS);
	}

	public void removeRow (int row) {
		checkRow (row);
		for (int c = 0; c < columnCount; c++) columns [c].remove (row, 1);
		rowCount--;
		fire (TableModelEvent.DELETE, row, row, TableModelEvent.ALL_COLUMNS);
	}

	/** Moves rows {@code [start, end]} so that {@code start} lands at {@code to}. */
	public void moveRow (int start, int end, int to) {
		int shift = to - start;
		int first = shift < 0 ? to : start;
		int last = shift < 0 ? end : to + end - start;
		if (first < 0 || last >= rowCount || first > last + 1) throw new ArrayIndexOutOfBoundsException ("move " + start + ".." + end + " to " + to);
		for (int c = 0; c < columnCount; c++) columns [c].rotate (first, last + 1, shift);
		fire (TableModelEvent.UPDATE, first, last, TableModelEvent.ALL_COLUMNS);
	}

	public void setRowCount (int rows) {
		setNumRows (rows);
	}

	public void setNumRows (int rows) {
		int old = rowCount;
		if (old == rows) return;
		if (rows < 0) throw new ArrayIndexOutOfBoundsException (rows);
		for (int c = 0; c < columnCount; c++) columns [c].setSize (rows);
		rowCount = rows;
		if (rows <= old) fire (TableModelEvent.DELETE, rows, old - 1, TableModelEvent.ALL_COLUMNS);
		else fire (TableModelEvent.INSERT, old, rows - 1, TableModelEvent.ALL_COLUMNS);
	}

	// ---- DefaultTableModel column API ------------------------------------------------------------

	public void setColumnIdentifiers (Object [] newIdentifiers) {
		setIdentifiersQuiet (newIdentifiers);
		fireStructure ();
	}

	public void setColumnCount (int count) {
		setColumnCountQuiet (count);
		fireStructure ();
	}

	public void addColumn (Object columnName) {
		addColumn (columnName, (Object []) null);
	}

	public void addColumn (Object columnName, Object [] columnData) {
		int c = columnCount;
		setColumnCountQuiet (c + 1);
		identifiers [c] = columnName;
		if (columnData != null) {
			if (columnData.length > rowCount) {
				for (int k = 0; k <= c; k++) columns [k].setSize (columnData.length);
				rowCount = columnData.length;
			}
			for (int r = 0; r < columnData.length; r++) columns [c].set (r, columnData [r]);
		}
		fireStructure ();
	}

	public void setDataVector (Object [][] data, Object [] columnNames) {
		setDataVectorQuiet (data, columnNames);
		fireStructure ();
	}

	// ---- batches and diagnostics -----------------------------------------------------------------

	public void beginBatch () {
		batchDepth++;
	}

	public void endBatch () {
		if (batchDepth == 0) throw new IllegalStateException ("endBatch without beginBatch");
		if (--batchDepth == 0) flush ();
	}

	/** Bytes held by the packed columns (arrays and bit-set containers, headers estimated). */
	public long packedBytes () {
		long b = 0;
		for (int c = 0; c < columnCount; c++) b += columns [c].bytes ();
		return b;
	}

	// ---- internals -------------------------------------------------------------------------------

	private M3PackedColumn column (int column) {
		if (column < 0 || column >= columnCount) throw new ArrayIndexOutOfBoundsException (column + " >= " + columnCount);
		return columns [column];
	}

	private void checkRow (int row) {
		if (row < 0 || row >= rowCount) throw new ArrayIndexOutOfBoundsException (row + " >= " + rowCount);
	}

	private void setColumnCountQuiet (int count) {
		if (count < 0) throw new ArrayIndexOutOfBoundsException (count);
		if (count > columns.length) {
			int cap = PackedHeapGrowth.capacity (columns.length, count, 4);
			columns = Arrays.copyOf (columns, cap);
			identifiers = Arrays.copyOf (identifiers, cap);
		}
		for (int c = columnCount; c < count; c++) columns [c] = new M3PackedColumn (rowCount);
		for (int c = count; c < columnCount; c++) {
			columns [c] = null;
			identifiers [c] = null;
		}
		columnCount = count;
	}

	private void setIdentifiersQuiet (Object [] ids) {
		int n = ids == null ? 0 : ids.length;
		setColumnCountQuiet (n);
		for (int c = 0; c < n; c++) identifiers [c] = ids [c];
	}

	private void setDataVectorQuiet (Object [][] data, Object [] columnNames) {
		Arrays.fill (columns, null);
		Arrays.fill (identifiers, null);
		columnCount = 0;
		rowCount = data == null ? 0 : data.length;
		setIdentifiersQuiet (columnNames);
		for (int r = 0; r < rowCount; r++) {
			Object [] row = data [r];
			for (int c = 0; row != null && c < Math.min (row.length, columnCount); c++) columns [c].set (r, row [c]);
		}
	}

	private void fireStructure () {
		flush ();
		fireTableStructureChanged ();
	}

	private void fire (int type, int first, int last, int column) {
		if (batchDepth == 0) {
			fireTableChanged (new TableModelEvent (this, first, last, column, type));
			return;
		}
		if (pendingType == type && type == TableModelEvent.INSERT && first >= pendingFirst && first <= pendingLast + 1) {
			pendingLast += last - first + 1;
			return;
		}
		if (pendingType == type && type == TableModelEvent.UPDATE) {
			pendingFirst = Math.min (pendingFirst, first);
			pendingLast = Math.max (pendingLast, last);
			if (pendingColumn != column) pendingColumn = TableModelEvent.ALL_COLUMNS;
			return;
		}
		flush ();
		pendingType = type;
		pendingFirst = first;
		pendingLast = last;
		pendingColumn = column;
	}

	private void flush () {
		if (pendingType == NONE) return;
		int type = pendingType;
		pendingType = NONE;
		fireTableChanged (new TableModelEvent (this, pendingFirst, pendingLast, pendingColumn, type));
	}
}
