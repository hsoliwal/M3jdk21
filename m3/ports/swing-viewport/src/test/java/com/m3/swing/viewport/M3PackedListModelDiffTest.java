/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.junit.jupiter.api.Test;

/** Differential: the same seeded operations on DefaultListModel and M3PackedListModel. */
class M3PackedListModelDiffTest {
	static final class Recorder implements ListDataListener {
		final List<String> events = new ArrayList<> ();

		@Override
		public void intervalAdded (ListDataEvent e) {
			add (e);
		}

		@Override
		public void intervalRemoved (ListDataEvent e) {
			add (e);
		}

		@Override
		public void contentsChanged (ListDataEvent e) {
			add (e);
		}

		void add (ListDataEvent e) {
			events.add (e.getType () + ":" + e.getIndex0 () + ":" + e.getIndex1 ());
		}
	}

	@Test
	void randomOperationsMatchDefaultListModel () {
		for (long seed = 1; seed <= 40; seed++) run (seed, 600);
	}

	private static void run (long seed, int steps) {
		Random rnd = new Random (seed);
		DefaultListModel<String> ref = new DefaultListModel<> ();
		M3PackedListModel<String> m3 = new M3PackedListModel<> ();
		Recorder refEvents = new Recorder (), m3Events = new Recorder ();
		ref.addListDataListener (refEvents);
		m3.addListDataListener (m3Events);
		String [] pool = new String [32];
		for (int i = 0; i < pool.length; i++) pool [i] = (i % 7 == 0) ? null : "v" + i;
		for (int step = 0; step < steps; step++) {
			int size = ref.size ();
			String v = pool [rnd.nextInt (pool.length)];
			int op = rnd.nextInt (13);
			switch (op) {
				case 0: ref.addElement (v); m3.addElement (v); break;
				case 1: { int i = rnd.nextInt (size + 1); ref.add (i, v); m3.add (i, v); break; }
				case 2: { int i = rnd.nextInt (size + 1); ref.insertElementAt (v, i); m3.insertElementAt (v, i); break; }
				case 3: if (size > 0) { int i = rnd.nextInt (size); assertSame (ref.set (i, v), m3.set (i, v)); } break;
				case 4: if (size > 0) { int i = rnd.nextInt (size); ref.setElementAt (v, i); m3.setElementAt (v, i); } break;
				case 5: if (size > 0) { int i = rnd.nextInt (size); assertSame (ref.remove (i), m3.remove (i)); } break;
				case 6: if (size > 0) { int i = rnd.nextInt (size); ref.removeElementAt (i); m3.removeElementAt (i); } break;
				case 7: assertEquals (ref.removeElement (v), m3.removeElement (v)); break;
				case 8: if (size > 0) { int a = rnd.nextInt (size), b = a + rnd.nextInt (size - a); ref.removeRange (a, b); m3.removeRange (a, b); } break;
				case 9: if (rnd.nextInt (20) == 0) { ref.clear (); m3.clear (); } break;
				case 10: { int n = rnd.nextInt (size + 4); ref.setSize (n); m3.setSize (n); break; }
				case 11: { List<String> c = new ArrayList<> (); for (int k = rnd.nextInt (4); k > 0; k--) c.add (pool [rnd.nextInt (pool.length)]); ref.addAll (c); m3.addAll (c); break; }
				default: { List<String> c = new ArrayList<> (); for (int k = rnd.nextInt (4); k > 0; k--) c.add (pool [rnd.nextInt (pool.length)]); int i = rnd.nextInt (size + 1); ref.addAll (i, c); m3.addAll (i, c); break; }
			}
			compare (seed, step, ref, m3, pool);
			assertEquals (refEvents.events, m3Events.events, "seed " + seed + " step " + step + " op " + op);
		}
	}

	private static void compare (long seed, int step, DefaultListModel<String> ref, M3PackedListModel<String> m3, String [] pool) {
		String where = "seed " + seed + " step " + step;
		assertEquals (ref.getSize (), m3.getSize (), where);
		assertEquals (ref.isEmpty (), m3.isEmpty (), where);
		for (int i = 0; i < ref.size (); i++) assertSame (ref.getElementAt (i), m3.getElementAt (i), where + " index " + i);
		for (String v : pool) {
			assertEquals (ref.indexOf (v), m3.indexOf (v), where);
			assertEquals (ref.lastIndexOf (v), m3.lastIndexOf (v), where);
			assertEquals (ref.contains (v), m3.contains (v), where);
		}
		assertEquals (ref.toString (), m3.toString (), where);
		char [] buf = new char [16];
		for (int i = 0; i < ref.size (); i++) {
			String e = ref.get (i);
			assertEquals (e == null ? 0 : e.length (), m3.cellChars (i, 0, buf), where);
		}
	}

	@Test
	void batchCoalescesAdjacentAddsAndHullsChanges () {
		M3PackedListModel<String> m3 = new M3PackedListModel<> ();
		Recorder rec = new Recorder ();
		m3.addListDataListener (rec);
		m3.beginBatch ();
		for (int i = 0; i < 100; i++) m3.addElement ("r" + i);
		m3.endBatch ();
		assertEquals (List.of (ListDataEvent.INTERVAL_ADDED + ":0:99"), rec.events);
		rec.events.clear ();
		m3.beginBatch ();
		m3.set (5, "a");
		m3.set (90, "b");
		m3.set (40, "c");
		m3.endBatch ();
		assertEquals (List.of (ListDataEvent.CONTENTS_CHANGED + ":5:90"), rec.events);
		rec.events.clear ();
		m3.beginBatch ();
		m3.add (10, "x");
		m3.remove (0);
		m3.endBatch ();
		assertEquals (List.of (ListDataEvent.INTERVAL_ADDED + ":10:10", ListDataEvent.INTERVAL_REMOVED + ":0:0"), rec.events);
	}

	@Test
	void listModelContract () {
		ListModel<String> m = new M3PackedListModel<> ();
		assertEquals (0, m.getSize ());
	}
}
