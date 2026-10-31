/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.junit.jupiter.api.Test;

/** Differential: the same seeded operations on DefaultTreeModel and M3PackedTreeModel. */
class M3PackedTreeModelDiffTest {
	/** Records events as text: kind, path values, child indices, child values. */
	static final class Recorder implements TreeModelListener {
		final List<String> events = new ArrayList<> ();

		private void add (String kind, TreeModelEvent e) {
			Object [] path = e.getPath ();
			Object [] children = e.getChildren ();
			events.add (kind + " path=" + (path == null ? "null" : Arrays.toString (path)) + " idx=" + Arrays.toString (e.getChildIndices ())
					+ " ch=" + (children == null ? "null" : Arrays.toString (children)));
		}

		@Override
		public void treeNodesChanged (TreeModelEvent e) {
			add ("changed", e);
		}

		@Override
		public void treeNodesInserted (TreeModelEvent e) {
			add ("inserted", e);
		}

		@Override
		public void treeNodesRemoved (TreeModelEvent e) {
			add ("removed", e);
		}

		@Override
		public void treeStructureChanged (TreeModelEvent e) {
			add ("structure", e);
		}
	}

	@Test
	void randomOperationsMatchDefaultTreeModel () {
		for (long seed = 1; seed <= 30; seed++) {
			run (seed, 500, false);
			run (seed, 300, true);
		}
	}

	private static void run (long seed, int steps, boolean asksAllowsChildren) {
		Random rnd = new Random (seed);
		int [] next = {1};
		DefaultTreeModel ref = new DefaultTreeModel (new DefaultMutableTreeNode ("n0"), asksAllowsChildren);
		M3PackedTreeModel m3 = new M3PackedTreeModel ("n0", asksAllowsChildren);
		List<DefaultMutableTreeNode> refNodes = new ArrayList<> ();
		List<M3TreeNodeProxy> m3Nodes = new ArrayList<> ();
		refNodes.add ((DefaultMutableTreeNode) ref.getRoot ());
		m3Nodes.add ((M3TreeNodeProxy) m3.getRoot ());
		Recorder refEvents = new Recorder (), m3Events = new Recorder ();
		ref.addTreeModelListener (refEvents);
		m3.addTreeModelListener (m3Events);
		for (int step = 0; step < steps; step++) {
			int i = rnd.nextInt (refNodes.size ());
			DefaultMutableTreeNode r = refNodes.get (i);
			M3TreeNodeProxy m = m3Nodes.get (i);
			String where = "seed " + seed + " step " + step + " asks " + asksAllowsChildren;
			int op = rnd.nextInt (12);
			switch (op) {
				case 0: case 1: case 2: case 3: case 4: {
					if (!r.getAllowsChildren ()) break;
					String v = "n" + next [0]++;
					int index = rnd.nextInt (r.getChildCount () + 1);
					DefaultMutableTreeNode child = new DefaultMutableTreeNode (v);
					ref.insertNodeInto (child, r, index);
					M3TreeNodeProxy p = m3.insertNodeInto (v, m, index);
					refNodes.add (child);
					m3Nodes.add (p);
					break;
				}
				case 5: case 6: {
					if (r.getParent () == null) break;
					List<Integer> gone = new ArrayList<> ();
					for (int k = 0; k < refNodes.size (); k++) {
						if (refNodes.get (k) == r || r.isNodeDescendant (refNodes.get (k))) gone.add (k);
					}
					ref.removeNodeFromParent (r);
					m3.removeNodeFromParent (m);
					for (int k = gone.size () - 1; k >= 0; k--) {
						int at = gone.get (k);
						assertFalse (m3Nodes.get (at).isCurrent (), where);
						refNodes.remove (at);
						m3Nodes.remove (at);
					}
					break;
				}
				case 7: {
					String v = "v" + next [0]++;
					ref.valueForPathChanged (new TreePath (r.getPath ()), v);
					m3.valueForPathChanged (m3.pathOf (m.key ()), v);
					break;
				}
				case 8: ref.nodeChanged (r); m3.nodeChanged (m); break;
				case 9: ref.nodeStructureChanged (r); m3.nodeStructureChanged (m); break;
				case 10: if (rnd.nextInt (6) == 0) { ref.reload (); m3.reload (); } break;
				default: {
					if (rnd.nextInt (4) != 0 || r.getParent () == null) break;
					boolean allows = rnd.nextBoolean ();
					List<Integer> gone = new ArrayList<> ();
					if (!allows) {
						for (int k = 0; k < refNodes.size (); k++) {
							if (refNodes.get (k) != r && r.isNodeDescendant (refNodes.get (k))) gone.add (k);
						}
					}
					r.setAllowsChildren (allows);
					m3.setAllowsChildren (m, allows);
					for (int k = gone.size () - 1; k >= 0; k--) {
						refNodes.remove ((int) gone.get (k));
						m3Nodes.remove ((int) gone.get (k));
					}
				}
			}
			compareTree (where + " op " + op, ref, ref.getRoot (), m3, m3.getRoot ());
			assertEquals (refEvents.events, m3Events.events, where + " op " + op);
			assertEquals (refNodes.size (), m3.nodeCount (), where);
		}
	}

	static void compareTree (String where, TreeModel ref, Object r, TreeModel m3, Object m) {
		assertEquals (r.toString (), m.toString (), where);
		int n = ref.getChildCount (r);
		assertEquals (n, m3.getChildCount (m), where + " at " + r);
		assertEquals (ref.isLeaf (r), m3.isLeaf (m), where + " leaf " + r);
		assertEquals (((TreeNode) r).getAllowsChildren (), ((TreeNode) m).getAllowsChildren (), where);
		for (int i = 0; i < n; i++) {
			Object rc = ref.getChild (r, i), mc = m3.getChild (m, i);
			assertSame (mc, m3.getChild (m, i), where + " pinned proxy");
			assertEquals (ref.getIndexOfChild (r, rc), m3.getIndexOfChild (m, mc), where);
			assertSame (m, ((TreeNode) mc).getParent (), where);
			compareTree (where, ref, rc, m3, mc);
		}
		assertEquals (-1, m3.getIndexOfChild (m, null), where);
	}

	@Test
	void removedProxyIsStaleDetachedLeafAndIdsAreRecycled () {
		M3PackedTreeModel m = new M3PackedTreeModel ("root");
		M3TreeNodeProxy a = m.insertNodeInto ("a", (TreeNode) m.getRoot (), 0);
		M3TreeNodeProxy b = m.insertNodeInto ("b", a, 0);
		int bKey = b.key ();
		m.removeNodeFromParent (a);
		assertFalse (a.isCurrent ());
		assertFalse (b.isCurrent ());
		assertEquals ("a", a.toString ());
		assertEquals ("b", b.toString ());
		assertEquals (0, a.getChildCount ());
		assertTrue (a.isLeaf ());
		assertEquals (null, a.getParent ());
		M3TreeNodeProxy c = m.insertNodeInto ("c", (TreeNode) m.getRoot (), 0);
		M3TreeNodeProxy d = m.insertNodeInto ("d", c, 0);
		assertTrue (c.key () == bKey || d.key () == bKey, "ids are recycled");
		assertFalse (b.isCurrent (), "a recycled id does not revive an old proxy");
		assertThrows (IllegalArgumentException.class, () -> m.removeNodeFromParent ((TreeNode) m.getRoot ()));
		assertThrows (IllegalArgumentException.class, () -> m.insertNodeInto ("x", a, 0));
	}

	@Test
	void removalEventSeesTheDetachedSubtree () {
		M3PackedTreeModel m = new M3PackedTreeModel ("root");
		M3TreeNodeProxy a = m.insertNodeInto ("a", (TreeNode) m.getRoot (), 0);
		m.insertNodeInto ("b", a, 0);
		int [] seen = {-1};
		m.addTreeModelListener (new TreeModelListener () {
			@Override
			public void treeNodesRemoved (TreeModelEvent e) {
				seen [0] = ((TreeNode) e.getChildren () [0]).getChildCount ();
			}

			@Override
			public void treeNodesChanged (TreeModelEvent e) {
			}

			@Override
			public void treeNodesInserted (TreeModelEvent e) {
			}

			@Override
			public void treeStructureChanged (TreeModelEvent e) {
			}
		});
		m.removeNodeFromParent (a);
		assertEquals (1, seen [0], "during the event the removed node still answers its subtree");
	}
}
