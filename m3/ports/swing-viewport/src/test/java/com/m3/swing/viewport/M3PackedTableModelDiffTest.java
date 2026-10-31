/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.junit.jupiter.api.Test;

/** Differential: the same seeded operations on DefaultTableModel and M3PackedTableModel. */
class M3PackedTableModelDiffTest {
	static final class Recorder implements TableModelListener {
		final List<String> events = new ArrayList<> ();

		@Override
		public void tableChanged (TableModelEvent e) {
			events.add (e.getType () + ":" + e.getFirstRow () + ":" + e.getLastRow () + ":" + e.getColumn ());
		}
	}

	/** A non-primitive caller object: must come back identical. */
	static final class Payload {
		final int id;

		Payload (int id) {
			this.id = id;
		}

		@Override
		public String toString () {
			return "payload-" + id;
		}
	}

	private static Object value (Random rnd, int column) {
		int k = rnd.nextInt (100);
		if (k < 8) return null;
		if (k < 10) return new Payload (rnd.nextInt (5));
		if (k < 12) return "s" + rnd.nextInt (50);
		switch (column % 5) {
			case 0: return rnd.nextInt ();
			case 1: return rnd.nextLong ();
			case 2: return rnd.nextDouble () * 1e6;
			case 3: return rnd.nextBoolean ();
			default: return "text-" + rnd.nextInt (1000);
		}
	}

	private static Object [] row (Random rnd, int columns) {
		Object [] r = new Object [Math.max (0, columns + rnd.nextInt (3) - 1)];
		for (int c = 0; c < r.length; c++) r [c] = value (rnd, c);
		return r;
	}

	@Test
	void randomOperationsMatchDefaultTableModel () {
		for (long seed = 1; seed <= 30; seed++) run (seed, 400);
	}

	private static void run (long seed, int steps) {
		Random rnd = new Random (seed);
		Object [] names = {"a", "b", "c", "d", "e"};
		DefaultTableModel ref = new DefaultTableModel (names, 3);
		M3PackedTableModel m3 = new M3PackedTableModel (names, 3);
		Recorder refEvents = new Recorder (), m3Events = new Recorder ();
		ref.addTableModelListener (refEvents);
		m3.addTableModelListener (m3Events);
		for (int step = 0; step < steps; step++) {
			int rows = ref.getRowCount (), cols = ref.getColumnCount ();
			int op = rnd.nextInt (20);
			switch (op) {
				case 0: case 1: case 2: { Object [] r = row (rnd, cols); ref.addRow (r); m3.addRow (r); break; }
				case 3: case 4: { int i = rnd.nextInt (rows + 1); Object [] r = rnd.nextInt (10) == 0 ? null : row (rnd, cols); ref.insertRow (i, r); m3.insertRow (i, r); break; }
				case 5: case 6: if (rows > 0) { int i = rnd.nextInt (rows); ref.removeRow (i); m3.removeRow (i); } break;
				case 7: case 8: case 9: case 10: if (rows > 0 && cols > 0) { int i = rnd.nextInt (rows), c = rnd.nextInt (cols); Object v = value (rnd, c); ref.setValueAt (v, i, c); m3.setValueAt (v, i, c); } break;
				case 11: case 12: if (rows > 1) {
					int start = rnd.nextInt (rows), end = start + rnd.nextInt (rows - start), span = end - start;
					int to = rnd.nextInt (rows - span);
					ref.moveRow (start, end, to);
					m3.moveRow (start, end, to);
				} break;
				case 13: if (rnd.nextInt (4) == 0) { int n = rnd.nextInt (rows + 5); ref.setRowCount (n); m3.setRowCount (n); } break;
				case 14: if (cols < 9) { Object n = "col" + step; ref.addColumn (n); m3.addColumn (n); } break;
				case 15: if (cols < 9) {
					Object [] data = new Object [rnd.nextInt (rows + 3)];
					for (int k = 0; k < data.length; k++) data [k] = value (rnd, cols);
					Object n = rnd.nextBoolean () ? null : "dc" + step;
					ref.addColumn (n, data);
					m3.addColumn (n, data);
				} break;
				case 16: if (rnd.nextInt (5) == 0) { int n = 1 + rnd.nextInt (7); ref.setColumnCount (n); m3.setColumnCount (n); } break;
				case 17: if (rnd.nextInt (8) == 0) {
					Object [] ids = new Object [1 + rnd.nextInt (6)];
					for (int k = 0; k < ids.length; k++) ids [k] = rnd.nextInt (4) == 0 ? null : "id" + k;
					ref.setColumnIdentifiers (ids);
					m3.setColumnIdentifiers (ids);
				} break;
				default: if (rnd.nextInt (10) == 0) {
					Object [][] data = new Object [rnd.nextInt (6)][];
					for (int r = 0; r < data.length; r++) data [r] = row (rnd, 4);
					Object [] ids = {"x", "y", "z", "w"};
					ref.setDataVector (data, ids);
					m3.setDataVector (data, ids);
				} break;
			}
			compare ("seed " + seed + " step " + step + " op " + op, ref, m3);
			assertEquals (refEvents.events, m3Events.events, "seed " + seed + " step " + step + " op " + op);
		}
	}

	private static void compare (String where, TableModel ref, M3PackedTableModel m3) {
		assertEquals (ref.getRowCount (), m3.getRowCount (), where);
		assertEquals (ref.getColumnCount (), m3.getColumnCount (), where);
		char [] buf = new char [64];
		for (int c = 0; c < ref.getColumnCount (); c++) {
			assertEquals (ref.getColumnName (c), m3.getColumnName (c), where);
			assertSame (ref.getColumnClass (c), m3.getColumnClass (c), where);
			for (int r = 0; r < ref.getRowCount (); r++) {
				Object a = ref.getValueAt (r, c), b = m3.getValueAt (r, c);
				assertEquals (a, b, where + " cell " + r + "," + c);
				if (a instanceof Payload) assertSame (a, b, where);
				assertEquals (ref.isCellEditable (r, c), m3.isCellEditable (r, c), where);
				int n = m3.cellChars (r, c, buf);
				assertEquals (a == null ? "" : a.toString (), new String (buf, 0, n), where + " chars " + r + "," + c);
			}
		}
	}

	@Test
	void reportColumnKindsAndPromotion () {
		M3PackedTableModel m = new M3PackedTableModel (2, 2, M3PackedTableModel.REPORT_COLUMN_KINDS);
		m.setValueAt (7, 0, 0);
		assertSame (Integer.class, m.getColumnClass (0));
		m.setValueAt ("seven", 1, 0);
		assertSame (Object.class, m.getColumnClass (0));
		assertEquals (7, m.getValueAt (0, 0));
		assertEquals ("seven", m.getValueAt (1, 0));
		m.setValueAt ("a", 0, 1);
		assertSame (String.class, m.getColumnClass (1));
	}

	@Test
	void batchCoalescesInsertsAndUpdates () {
		M3PackedTableModel m = new M3PackedTableModel (new Object [] {"n", "s"}, 0);
		Recorder rec = new Recorder ();
		m.addTableModelListener (rec);
		m.beginBatch ();
		for (int i = 0; i < 1000; i++) m.addRow (new Object [] {i, "r" + i});
		m.endBatch ();
		assertEquals (List.of (TableModelEvent.INSERT + ":0:999:" + TableModelEvent.ALL_COLUMNS), rec.events);
		rec.events.clear ();
		m.beginBatch ();
		m.setValueAt (1, 3, 0);
		m.setValueAt (2, 700, 0);
		m.setValueAt ("z", 500, 1);
		m.endBatch ();
		assertEquals (List.of (TableModelEvent.UPDATE + ":3:700:" + TableModelEvent.ALL_COLUMNS), rec.events);
	}
}
