/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.VariableHeightLayoutCache;

import org.junit.jupiter.api.Test;

/**
 * Differential: the same seeded selection operations on DefaultTreeSelectionModel and
 * M3TreeSelectionModel, over identical trees, each with a stock VariableHeightLayoutCache as row
 * mapper. Order-insensitive comparisons where DefaultTreeSelectionModel's order is hash order.
 */
class M3TreeSelectionModelDiffTest {
	static final class Fixture {
		final DefaultTreeModel refModel = new DefaultTreeModel (new DefaultMutableTreeNode ("n0"));
		final M3PackedTreeModel m3Model = new M3PackedTreeModel ("n0");
		final List<TreePath> refPaths = new ArrayList<> (), m3Paths = new ArrayList<> ();
		final VariableHeightLayoutCache refRows = new VariableHeightLayoutCache (), m3Rows = new VariableHeightLayoutCache ();

		Fixture (Random rnd, int nodes) {
			refPaths.add (new TreePath (refModel.getRoot ()));
			m3Paths.add (m3Model.pathOf (m3Model.rootKey ()));
			List<DefaultMutableTreeNode> r = new ArrayList<> ();
			List<M3TreeNodeProxy> m = new ArrayList<> ();
			r.add ((DefaultMutableTreeNode) refModel.getRoot ());
			m.add ((M3TreeNodeProxy) m3Model.getRoot ());
			for (int i = 1; i < nodes; i++) {
				int p = rnd.nextInt (r.size ());
				int index = rnd.nextInt (r.get (p).getChildCount () + 1);
				DefaultMutableTreeNode child = new DefaultMutableTreeNode ("n" + i);
				refModel.insertNodeInto (child, r.get (p), index);
				M3TreeNodeProxy proxy = m3Model.insertNodeInto ("n" + i, m.get (p), index);
				r.add (child);
				m.add (proxy);
				refPaths.add (new TreePath (child.getPath ()));
				m3Paths.add (m3Model.pathOf (proxy.key ()));
			}
			for (VariableHeightLayoutCache c : new VariableHeightLayoutCache [] {refRows, m3Rows}) c.setRootVisible (true);
			refRows.setModel (refModel);
			m3Rows.setModel (m3Model);
			for (int i = 0; i < nodes; i++) {
				if (rnd.nextInt (3) != 0) {
					refRows.setExpandedState (refPaths.get (i), true);
					m3Rows.setExpandedState (m3Paths.get (i), true);
				}
			}
		}
	}

	static String set (Object [] paths) {
		TreeSet<String> s = new TreeSet<> ();
		if (paths != null) for (Object p : paths) s.add (((TreePath) p).getLastPathComponent ().toString ());
		return s.toString ();
	}

	static String rows (int [] rows) {
		if (rows == null) return "null";
		int [] c = rows.clone ();
		Arrays.sort (c);
		return Arrays.toString (c);
	}

	static String last (TreePath p) {
		return p == null ? "null" : p.getLastPathComponent ().toString ();
	}

	@Test
	void discontiguous () {
		for (long seed = 1; seed <= 25; seed++) run (seed, TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	}

	@Test
	void contiguous () {
		for (long seed = 1; seed <= 25; seed++) run (seed, TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
	}

	@Test
	void single () {
		for (long seed = 1; seed <= 25; seed++) run (seed, TreeSelectionModel.SINGLE_TREE_SELECTION);
	}

	private static void run (long seed, int mode) {
		Random rnd = new Random (seed);
		Fixture f = new Fixture (rnd, 40);
		DefaultTreeSelectionModel ref = new DefaultTreeSelectionModel ();
		M3TreeSelectionModel m3 = new M3TreeSelectionModel (f.m3Model);
		ref.setSelectionMode (mode);
		m3.setSelectionMode (mode);
		ref.setRowMapper (f.refRows);
		m3.setRowMapper (f.m3Rows);
		List<TreeSelectionEvent> refEvents = new ArrayList<> (), m3Events = new ArrayList<> ();
		ref.addTreeSelectionListener (refEvents::add);
		m3.addTreeSelectionListener (m3Events::add);
		boolean leadKnown = true;
		int n = f.refPaths.size ();
		for (int step = 0; step < 300; step++) {
			int count = 1 + rnd.nextInt (rnd.nextInt (8) == 0 ? 6 : 3);
			TreePath [] rp = new TreePath [count], mp = new TreePath [count];
			for (int k = 0; k < count; k++) {
				int i = rnd.nextInt (n);
				if (rnd.nextInt (12) == 0) continue;
				rp [k] = f.refPaths.get (i);
				mp [k] = f.m3Paths.get (i);
			}
			int op = rnd.nextInt (7);
			boolean before = leadKnown;
			refEvents.clear ();
			m3Events.clear ();
			String refError = apply (op, rnd.nextInt (5) == 0, ref, rp), m3Error = apply (op, false, m3, mp);
			String where = "seed " + seed + " mode " + mode + " step " + step + " op " + op;
			assertEquals (refError, m3Error, where + " failure");
			boolean removal = op == 4 || op == 5, fired = !refEvents.isEmpty ();
			if (op <= 1) leadKnown = true;
			else if (removal) leadKnown = fired ? ref.isSelectionEmpty () : leadKnown;
			else if (fired) leadKnown = true;
			assertEquals (set (ref.getSelectionPaths ()), set (m3.getSelectionPaths ()), where);
			assertEquals (ref.getSelectionCount (), m3.getSelectionCount (), where);
			assertEquals (ref.isSelectionEmpty (), m3.isSelectionEmpty (), where);
			for (int i = 0; i < n; i++) assertEquals (ref.isPathSelected (f.refPaths.get (i)), m3.isPathSelected (f.m3Paths.get (i)), where);
			assertEquals (rows (ref.getSelectionRows ()), rows (m3.getSelectionRows ()), where);
			assertEquals (ref.getMinSelectionRow (), m3.getMinSelectionRow (), where);
			assertEquals (ref.getMaxSelectionRow (), m3.getMaxSelectionRow (), where);
			for (int row = 0; row < f.refRows.getRowCount (); row++) assertEquals (ref.isRowSelected (row), m3.isRowSelected (row), where + " row " + row);
			assertEquals (refEvents.size (), m3Events.size (), where + " events");
			for (int e = 0; e < refEvents.size (); e++) {
				TreeSelectionEvent a = refEvents.get (e), b = m3Events.get (e);
				assertEquals (added (a, true), added (b, true), where + " added");
				assertEquals (added (a, false), added (b, false), where + " removed");
				if (removal) continue;
				assertEquals (last (a.getNewLeadSelectionPath ()), last (b.getNewLeadSelectionPath ()), where + " new lead");
				if (before) assertEquals (last (a.getOldLeadSelectionPath ()), last (b.getOldLeadSelectionPath ()), where + " old lead");
			}
			if (leadKnown) {
				assertEquals (last (ref.getLeadSelectionPath ()), last (m3.getLeadSelectionPath ()), where + " lead");
				assertEquals (ref.getLeadSelectionRow (), m3.getLeadSelectionRow (), where + " lead row");
			} else if (m3.isSelectionEmpty ()) {
				assertNull (m3.getLeadSelectionPath (), where);
			} else {
				assertTrue (m3.isPathSelected (m3.getLeadSelectionPath ()), where + " lead is selected");
			}
		}
	}

	/** Applies one operation; returns the failure class name or null. Clear runs when {@code clear} (decided once, for both). */
	private static boolean clearDecision;

	private static String apply (int op, boolean clear, TreeSelectionModel m, TreePath [] p) {
		if (m instanceof DefaultTreeSelectionModel) clearDecision = clear;
		try {
			switch (op) {
				case 0: m.setSelectionPaths (p); break;
				case 1: m.setSelectionPath (p [0]); break;
				case 2: case 3: m.addSelectionPaths (p); break;
				case 4: m.removeSelectionPaths (p); break;
				case 5: m.removeSelectionPath (p [0]); break;
				default: if (clearDecision) m.clearSelection (); break;
			}
			return null;
		} catch (RuntimeException e) {
			return e.getClass ().getName ();
		}
	}

	static String added (TreeSelectionEvent e, boolean wanted) {
		TreeSet<String> s = new TreeSet<> ();
		for (TreePath p : e.getPaths ()) if (e.isAddedPath (p) == wanted) s.add (p.getLastPathComponent ().toString ());
		return s.toString ();
	}

	@Test
	void bulkRangeAndPatternCountArithmetically () {
		M3VirtualTreeModel model = new M3VirtualTreeModel (new int [] {1000, 1000}, "root", (k, c) -> "k" + k);
		M3TreeSelectionModel sel = new M3TreeSelectionModel (model);
		List<TreeSelectionEvent> events = new ArrayList<> ();
		sel.addTreeSelectionListener (events::add);
		sel.selectKeyRange (0, model.keyCount ());
		assertEquals (model.keyCount (), sel.getSelectionCount ());
		assertEquals (1, events.size ());
		assertTrue (events.get (0) instanceof M3TreeSelectionModel.M3TreeSelectionEvent);
		sel.clearSelection ();
		assertEquals (0, sel.getSelectionCount ());
		sel.selectPattern (1000, model.keyCount (), 3, 0b001);
		assertEquals ((1_000_000 + 2) / 3, sel.getSelectionCount ());
		assertTrue (sel.isKeySelected (1000));
		assertTrue (!sel.isKeySelected (1001));
		model.close ();
	}
}
