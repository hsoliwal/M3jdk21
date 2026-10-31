/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.VariableHeightLayoutCache;

import org.junit.jupiter.api.Test;

/** The source-backed tree against a materialized DefaultTreeModel, and its zero-materialization paint. */
class M3VirtualTreeTest {
	private static final int [] FANOUT = {3, 4, 5};

	private static DefaultTreeModel materialize (M3VirtualTreeModel m) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode ("root");
		build (m, m.rootKey (), root);
		return new DefaultTreeModel (root);
	}

	private static void build (M3VirtualTreeModel m, int key, DefaultMutableTreeNode node) {
		for (int i = 0; i < m.childCountOf (key); i++) {
			int c = m.childKeyOf (key, i);
			DefaultMutableTreeNode child = new DefaultMutableTreeNode ("k" + c);
			node.add (child);
			build (m, c, child);
		}
	}

	@Test
	void structureMatchesMaterializedTree () {
		try (M3VirtualTreeModel m = new M3VirtualTreeModel (FANOUT, "root", (k, c) -> "k" + k)) {
			DefaultTreeModel ref = materialize (m);
			M3PackedTreeModelDiffTest.compareTree ("virtual", ref, ref.getRoot (), m, m.getRoot ());
			assertEquals (3 + 12 + 60, m.keyCount ());
		}
	}

	@Test
	void rowsMatchStockLayoutCache () {
		Random rnd = new Random (7);
		try (M3VirtualTreeModel m = new M3VirtualTreeModel (FANOUT, "root", (k, c) -> "k" + k)) {
			DefaultTreeModel ref = materialize (m);
			VariableHeightLayoutCache stock = new VariableHeightLayoutCache ();
			stock.setModel (ref);
			stock.setRootVisible (false);
			stock.setExpandedState (new TreePath (ref.getRoot ()), true);
			M3TreeRows rows = new M3TreeRows (m);
			rows.setRootVisible (false);
			List<TreePath> refPaths = new ArrayList<> ();
			List<Integer> keys = new ArrayList<> ();
			collect (ref, (DefaultMutableTreeNode) ref.getRoot (), m, m.rootKey (), refPaths, keys);
			for (int step = 0; step < 200; step++) {
				int i = 1 + rnd.nextInt (refPaths.size () - 1);
				boolean expand = rnd.nextBoolean ();
				TreePath p = refPaths.get (i);
				if (expand) {
					for (TreePath a = p; a != null; a = a.getParentPath ()) stock.setExpandedState (a, true);
					rows.setExpandedState (rows.pathOfKey (keys.get (i)), true);
				} else {
					stock.setExpandedState (p, false);
					rows.setExpandedState (rows.pathOfKey (keys.get (i)), false);
				}
				assertEquals (stock.getRowCount (), rows.getRowCount (), "step " + step);
				for (int r = 0; r < stock.getRowCount (); r++) {
					assertEquals (stock.getPathForRow (r).getLastPathComponent ().toString (), rows.getPathForRow (r).getLastPathComponent ().toString ());
					assertEquals (r, rows.getRowForPath (rows.getPathForRow (r)));
				}
				for (int j = 1; j < refPaths.size (); j++) {
					assertEquals (stock.getRowForPath (refPaths.get (j)), rows.rowOf (keys.get (j)), "step " + step + " node " + j);
				}
			}
		}
	}

	private static void collect (DefaultTreeModel ref, DefaultMutableTreeNode node, M3VirtualTreeModel m, int key, List<TreePath> paths, List<Integer> keys) {
		paths.add (new TreePath (node.getPath ()));
		keys.add (key);
		for (int i = 0; i < node.getChildCount (); i++) {
			collect (ref, (DefaultMutableTreeNode) node.getChildAt (i), m, m.childKeyOf (key, i), paths, keys);
		}
	}

	@Test
	void pathsAndProxiesAreReusedNotRecreated () {
		try (M3VirtualTreeModel m = new M3VirtualTreeModel (FANOUT, "root", (k, c) -> "k" + k)) {
			M3TreeRows rows = new M3TreeRows (m);
			rows.setRootVisible (false);
			m.expandToDepth (2);
			TreePath a = rows.getPathForRow (10);
			long made = rows.pathsCreated ();
			assertSame (a, rows.getPathForRow (10));
			assertEquals (made, rows.pathsCreated ());
			assertSame (m.getChild (m.getRoot (), 1), m.getChild (m.getRoot (), 1));
		}
	}

	@Test
	void headlessPaintMaterializesNothing () {
		try (M3VirtualTreeModel m = new M3VirtualTreeModel (new int [] {100, 100, 100}, "root", (k, c) -> "node " + k)) {
			M3PackedTree tree = new M3PackedTree (m);
			m.expandToDepth (2);
			tree.setSize (400, 600);
			tree.doLayout ();
			int rows = 600 / tree.getRowHeight () + 1;
			m.setViewportRows (0, rows, 8);
			BufferedImage img = new BufferedImage (400, 600, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics ();
			tree.paint (g);
			long proxies = m.proxiesCreated (), paths = ((M3TreeUI) tree.getUI ()).rows ().pathsCreated ();
			long fills = m.fills ();
			for (int i = 0; i < 5; i++) tree.paint (g);
			g.dispose ();
			assertEquals (0, m.materializedDuringPaint (), "no fill or page-in from paint");
			assertEquals (proxies, m.proxiesCreated (), "no proxy from paint");
			assertEquals (paths, ((M3TreeUI) tree.getUI ()).rows ().pathsCreated (), "no TreePath from paint");
			assertEquals (fills, m.fills (), "no SetData from paint");
			assertEquals (0, ((M3TreeUI) tree.getUI ()).stamp ().placeholders (), "every painted row was resident");
			assertTrue (((M3TreeUI) tree.getUI ()).rowsPainted () > 0);
		}
	}
}
