/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.swing.viewport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.junit.jupiter.api.Test;

/** Real Swing component layout, ownership, and typed binding contracts on the EDT. */
final class M3ViewportBindingMeasuredTest {
	private static final class Table extends JTable {
		private static final long serialVersionUID = 1L;
		final Rectangle clip = new Rectangle (0, 12, 120, 45);
		boolean showing = true;
		Table () {
			super (new DefaultTableModel (new Object [][] {{"e"}, {"d"}, {"c"}, {"b"}, {"a"}}, new Object [] {"name"}));
			int [] heights = {10, 30, 20, 40, 10};
			for (int row = 0; row < heights.length; row++) setRowHeight (row, heights [row]);
			setSize (200, 110);
		}
		@Override public boolean isShowing () { return showing; }
		@Override public Rectangle getVisibleRect () { return new Rectangle (clip); }
	}

	private static final class ListView extends JList<String> {
		private static final long serialVersionUID = 1L;
		ListView () {
			super (new String [] {"a", "b", "c", "d", "e", "f"});
			setFixedCellHeight (10);
			setFixedCellWidth (100);
			setSize (100, 60);
		}
		@Override public boolean isShowing () { return true; }
		@Override public Rectangle getVisibleRect () { return new Rectangle (0, 15, 100, 20); }
	}

	private static final class Tree extends JTree {
		private static final long serialVersionUID = 1L;
		Tree () {
			super (new DefaultTreeModel (root ()));
			setRootVisible (false);
			setRowHeight (20);
			setSize (200, 100);
		}
		private static DefaultMutableTreeNode root () {
			DefaultMutableTreeNode root = new DefaultMutableTreeNode ("root");
			for (int i = 0; i < 4; i++) root.add (new DefaultMutableTreeNode ("row " + i));
			return root;
		}
		@Override public boolean isShowing () { return true; }
		@Override public Rectangle getVisibleRect () { return new Rectangle (0, 21, 200, 18); }
	}

	private static M3ViewportBinding.Target capture (List<String> calls) {
		return (first, rows, overscan) -> calls.add (first + ":" + rows + ":" + overscan);
	}

	@Test
	void tableUsesItsActualVariableHeightRows () throws Exception {
		SwingUtilities.invokeAndWait (() -> {
			Table table = new Table ();
			List<String> calls = new ArrayList<> ();
			try (M3ViewportBinding binding = M3ViewportBinding.install (table, 2, capture (calls))) {
				assertEquals (List.of ("1:2:2"), calls);
				assertEquals (1, binding.pushes ());
			}
		});
	}

	@Test
	void listUsesComponentLayoutVisibleIndices () throws Exception {
		SwingUtilities.invokeAndWait (() -> {
			List<String> calls = new ArrayList<> ();
			try (M3ViewportBinding binding = M3ViewportBinding.install (new ListView (), 2, capture (calls))) {
				assertEquals (List.of ("1:3:2"), calls);
				assertEquals (1, binding.pushes ());
			}
		});
	}

	@Test
	void treeUsesExpandedRowGeometry () throws Exception {
		SwingUtilities.invokeAndWait (() -> {
			List<String> calls = new ArrayList<> ();
			try (M3ViewportBinding binding = M3ViewportBinding.install (new Tree (), 1, capture (calls))) {
				assertEquals (List.of ("1:1:1"), calls);
				assertEquals (1, binding.pushes ());
			}
		});
	}

	@Test
	void sortedTableWindowStaysInViewCoordinates () throws Exception {
		SwingUtilities.invokeAndWait (() -> {
			Table table = new Table ();
			table.setAutoCreateRowSorter (true);
			table.getRowSorter ().toggleSortOrder (0);
			List<String> calls = new ArrayList<> ();
			try (M3ViewportBinding binding = M3ViewportBinding.install (table, 0, capture (calls))) {
				int [] measured = new int [4];
				M3DesktopViewportAdapters.fromTable (table, 0, measured, 0);
				assertEquals (List.of (measured [0] + ":" + (measured [1] - measured [0]) + ":0"), calls);
				assertEquals (4, table.convertRowIndexToModel (0), "sorter remains active and unchanged");
				assertEquals (1, binding.pushes ());
			}
		});
	}

	@Test
	void measuredHiddenViewDefersUntilShowing () throws Exception {
		SwingUtilities.invokeAndWait (() -> {
			Table table = new Table ();
			table.showing = false;
			List<String> calls = new ArrayList<> ();
			try (M3ViewportBinding binding = M3ViewportBinding.install (table, 2, capture (calls))) {
				assertTrue (calls.isEmpty ());
				assertEquals (1, binding.deferred ());
				table.showing = true;
				binding.hierarchyChanged (new HierarchyEvent (table, HierarchyEvent.HIERARCHY_CHANGED,
						table, table.getParent (), HierarchyEvent.SHOWING_CHANGED));
				assertEquals (List.of ("1:2:2"), calls);
			}
		});
	}

	@Test
	void measuredEmptyClipCannotRequestOverscan () throws Exception {
		SwingUtilities.invokeAndWait (() -> {
			Table table = new Table ();
			table.clip.height = 0;
			List<String> calls = new ArrayList<> ();
			try (M3ViewportBinding binding = M3ViewportBinding.install (table, 100, capture (calls))) {
				assertEquals (List.of ("0:0:0"), calls);
				assertEquals (1, binding.pushes ());
			}
		});
	}

	@Test
	void closeRemovesOnlyOwnedListenersAndIgnoresQueuedCallbacks () throws Exception {
		SwingUtilities.invokeAndWait (() -> {
			Table table = new Table ();
			JViewport viewport = new JViewport ();
			viewport.setView (table);
			List<String> calls = new ArrayList<> ();
			javax.swing.event.ChangeListener other = event -> calls.add ("other");
			viewport.addChangeListener (other);
			M3ViewportBinding binding = M3ViewportBinding.install (table, 2, capture (calls));
			assertTrue (Arrays.asList (viewport.getChangeListeners ()).contains (binding));
			assertTrue (Arrays.asList (table.getHierarchyListeners ()).contains (binding));
			binding.close ();
			binding.close ();
			assertFalse (Arrays.asList (viewport.getChangeListeners ()).contains (binding));
			assertFalse (Arrays.asList (table.getHierarchyListeners ()).contains (binding));
			assertTrue (Arrays.asList (viewport.getChangeListeners ()).contains (other));
			int pushes = binding.pushes ();
			int deferred = binding.deferred ();
			calls.clear ();
			binding.sync ();
			binding.stateChanged (new ChangeEvent (viewport));
			binding.hierarchyChanged (new HierarchyEvent (table, HierarchyEvent.HIERARCHY_CHANGED,
					table, viewport, HierarchyEvent.PARENT_CHANGED | HierarchyEvent.SHOWING_CHANGED));
			assertTrue (calls.isEmpty ());
			assertEquals (pushes, binding.pushes ());
			assertEquals (deferred, binding.deferred ());
			assertFalse (Arrays.asList (viewport.getChangeListeners ()).contains (binding));
		});
	}

	@Test
	void failedInstallationDoesNotLeakListeners () throws Exception {
		SwingUtilities.invokeAndWait (() -> {
			Table table = new Table ();
			JViewport viewport = new JViewport ();
			viewport.setView (table);
			int changes = viewport.getChangeListeners ().length;
			int hierarchy = table.getHierarchyListeners ().length;
			assertThrows (IllegalArgumentException.class, () -> M3ViewportBinding.install (table, 2,
					(first, rows, overscan) -> { throw new IllegalArgumentException ("source rejected"); }));
			assertEquals (changes, viewport.getChangeListeners ().length);
			assertEquals (hierarchy, table.getHierarchyListeners ().length);
		});
	}

	@Test
	void invalidMeasuredInputsRefuseBeforeListenerRegistration () throws Exception {
		SwingUtilities.invokeAndWait (() -> {
			Table table = new Table ();
			int hierarchy = table.getHierarchyListeners ().length;
			assertThrows (NullPointerException.class, () -> M3ViewportBinding.install (table, 1, null));
			assertThrows (IllegalArgumentException.class, () -> M3ViewportBinding.install (table, -1, (a, b, c) -> { }));
			assertEquals (hierarchy, table.getHierarchyListeners ().length);
		});
	}

	@Test
	void measuredInstallationRequiresEdt () {
		assertFalse (SwingUtilities.isEventDispatchThread ());
		assertThrows (IllegalStateException.class, () -> M3ViewportBinding.install ((JTable)null, 1, (a, b, c) -> { }));
	}
}
