/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.swing.viewport;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.event.RowSorterListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.junit.jupiter.api.Test;

/** Frozen post-event geometry and ownership probes using real Swing model/UI owners. */
final class M3ViewportBindingTrackingTest {
	private record Sample (int first, int rows, int overscan, String key) { }

	/* RowSorter has no public listener getter. Observe registrations while retaining
	 * the real TableRowSorter implementation and its ordinary listener dispatch. */
	private static final class Sorter extends TableRowSorter<DefaultTableModel> {
		final List<RowSorterListener> subscriptions = new ArrayList<> ();
		Sorter (DefaultTableModel model) { super (model); }
		@Override public void addRowSorterListener (RowSorterListener listener) {
			super.addRowSorterListener (listener);
			subscriptions.add (listener);
		}
		@Override public void removeRowSorterListener (RowSorterListener listener) {
			super.removeRowSorterListener (listener);
			for (int i = subscriptions.size () - 1; i >= 0; i--) {
				if (subscriptions.get (i) == listener) { subscriptions.remove (i); break; }
			}
		}
	}

	private static int sorterListeners (RowSorter<?> sorter) { return ((Sorter)sorter).subscriptions.size (); }

	private static final class Table extends JTable {
		private static final long serialVersionUID = 1L;
		boolean showing = true;
		Table () {
			super (new DefaultTableModel (new Object [][] {{"e"}, {"d"}, {"c"}, {"b"}, {"a"}}, new Object [] {"name"}));
			setAutoCreateRowSorter (true);
			setRowSorter (new Sorter ((DefaultTableModel)getModel ()));
			getRowSorter ().toggleSortOrder (0);
			((TableRowSorter<?>)getRowSorter ()).setSortsOnUpdates (true);
			setRowHeight (20);
			setSize (100, 100);
		}
		@Override public boolean isShowing () { return showing; }
		@Override public Rectangle getVisibleRect () { return new Rectangle (0, 0, 100, 100); }
	}

	private static final class ListView extends JList<Integer> {
		private static final long serialVersionUID = 1L;
		ListView () {
			super (new DefaultListModel<> ());
			for (int height : new int [] {10, 20, 30, 40}) model ().addElement (height);
			setCellRenderer ((list, value, index, selected, focused) -> {
				JLabel cell = new JLabel (value.toString ());
				cell.setPreferredSize (new Dimension (100, value));
				return cell;
			});
			setFixedCellWidth (100);
			setSize (100, 300);
		}
		DefaultListModel<Integer> model () { return (DefaultListModel<Integer>)getModel (); }
		@Override public boolean isShowing () { return true; }
		@Override public Rectangle getVisibleRect () { return new Rectangle (0, 10, 100, 45); }
	}

	private static final class Tree extends JTree {
		private static final long serialVersionUID = 1L;
		Tree () {
			super (new DefaultTreeModel (root ("child", "tail")));
			setRootVisible (false);
			setRowHeight (20);
			setSize (150, 200);
			collapseRow (0);
		}
		static DefaultMutableTreeNode root (String child, String tail) {
			DefaultMutableTreeNode root = new DefaultMutableTreeNode ("root");
			DefaultMutableTreeNode branch = new DefaultMutableTreeNode ("branch");
			branch.add (new DefaultMutableTreeNode (child));
			root.add (branch);
			root.add (new DefaultMutableTreeNode (tail));
			return root;
		}
		@Override public boolean isShowing () { return true; }
		@Override public Rectangle getVisibleRect () { return new Rectangle (0, 21, 150, 18); }
	}

	private static final class Tracked<V extends JComponent> implements AutoCloseable {
		final V view;
		final List<Sample> samples = new ArrayList<> ();
		final M3ViewportBinding binding;
		Runnable afterPush;
		Tracked (V view, Class<?> type, IntFunction<String> key) {
			this.view = view;
			binding = install (view, type, 2, (first, rows, overscan) -> {
				assertTrue (SwingUtilities.isEventDispatchThread ());
				samples.add (new Sample (first, rows, overscan, rows == 0 ? "-" : key.apply (first)));
				if (afterPush != null) afterPush.run ();
			});
		}
		Sample latest () { return samples.getLast (); }
		@Override public void close () {
			try { edt (() -> { binding.close (); return null; }); }
			catch (Exception failure) { throw new AssertionError (failure); }
		}
	}

	/* The identical frozen probes select legacy manual factories only in the labelled
	 * baseline experiment. Normal product tests require all new public overloads. */
	private static M3ViewportBinding install (JComponent view, Class<?> type, int overscan, M3ViewportBinding.Target target) {
		String name = Boolean.getBoolean ("m3.binding.manualBaseline") ? "install" : "installTracked";
		try {
			return (M3ViewportBinding)M3ViewportBinding.class.getMethod (name, type, int.class, M3ViewportBinding.Target.class)
					.invoke (null, view, overscan, target);
		} catch (InvocationTargetException failure) {
			Throwable cause = failure.getCause ();
			if (cause instanceof RuntimeException exception) throw exception;
			if (cause instanceof Error error) throw error;
			throw new AssertionError (cause);
		} catch (ReflectiveOperationException failure) {
			throw new AssertionError ("missing binding factory: " + name, failure);
		}
	}

	private static <T> T edt (Callable<T> action) throws Exception {
		FutureTask<T> task = new FutureTask<> (action);
		SwingUtilities.invokeAndWait (task);
		try { return task.get (); }
		catch (ExecutionException failure) {
			Throwable cause = failure.getCause ();
			if (cause instanceof Exception exception) throw exception;
			if (cause instanceof Error error) throw error;
			throw new AssertionError (cause);
		}
	}

	private static void drain () throws Exception { edt (() -> null); }
	private static Tracked<Table> table () throws Exception {
		return edt (() -> {
			Table table = new Table ();
			return new Tracked<> (table, JTable.class, row -> table.convertRowIndexToModel (row) + ":" + table.getValueAt (row, 0));
		});
	}

	@Test void tableEventsCoalesceAfterTheSorterFinishes () throws Exception {
		try (Tracked<Table> state = table ()) {
			edt (() -> {
				assertEquals (new Sample (0, 5, 2, "4:a"), state.latest ());
				DefaultTableModel model = (DefaultTableModel)state.view.getModel ();
				model.setValueAt ("z", 4, 0);
				model.setValueAt ("y", 0, 0);
				assertEquals (1, state.samples.size (), "data events must defer the target");
				return null;
			});
			drain ();
			edt (() -> {
				assertEquals (2, state.samples.size (), "one push for the mutation burst");
				assertEquals (new Sample (0, 5, 2, "3:b"), state.latest ());
				((TableRowSorter<?>)state.view.getRowSorter ()).setRowFilter (RowFilter.regexFilter ("[bc]", 0));
				return null;
			});
			drain ();
			edt (() -> { assertEquals (new Sample (0, 2, 2, "3:b"), state.latest ()); return null; });
		}
	}

	@Test void variableListGeometryAndReplacementUseCurrentData () throws Exception {
		try (Tracked<ListView> state = edt (() -> {
			ListView list = new ListView ();
			return new Tracked<> (list, JList.class, row -> list.getModel ().getElementAt (row).toString ());
		})) {
			DefaultListModel<Integer> old = edt (() -> state.view.model ());
			edt (() -> {
				assertEquals (new Sample (1, 2, 2, "20"), state.latest ());
				old.set (0, 40); old.set (1, 50);
				assertEquals (1, state.samples.size ()); return null;
			});
			drain ();
			edt (() -> {
				assertEquals (new Sample (0, 2, 2, "40"), state.latest ());
				DefaultListModel<Integer> replacement = new DefaultListModel<> ();
				replacement.addElement (80); state.view.setModel (replacement);
				return null;
			});
			drain ();
			edt (() -> {
				assertEquals (new Sample (0, 1, 2, "80"), state.latest ());
				assertEquals (0, old.getListDataListeners ().length);
				old.clear (); state.view.model ().clear (); return null;
			});
			drain ();
			edt (() -> { assertEquals (new Sample (0, 0, 0, "-"), state.latest ()); assertEquals (4, state.samples.size ()); return null; });
		}
	}

	@Test void treeExpansionInsertionAndReplacementTrackViewRows () throws Exception {
		try (Tracked<Tree> state = edt (() -> {
			Tree tree = new Tree ();
			return new Tracked<> (tree, JTree.class, row -> tree.getPathForRow (row).getLastPathComponent ().toString ());
		})) {
			DefaultTreeModel old = edt (() -> (DefaultTreeModel)state.view.getModel ());
			edt (() -> { assertEquals (new Sample (1, 1, 2, "tail"), state.latest ()); state.view.expandRow (0); return null; });
			drain ();
			edt (() -> {
				assertEquals (new Sample (1, 1, 2, "child"), state.latest ());
				DefaultMutableTreeNode branch = (DefaultMutableTreeNode)((DefaultMutableTreeNode)old.getRoot ()).getChildAt (0);
				old.insertNodeInto (new DefaultMutableTreeNode ("inserted"), branch, 0); return null;
			});
			drain ();
			edt (() -> {
				assertEquals (new Sample (1, 1, 2, "inserted"), state.latest ());
				state.view.setModel (new DefaultTreeModel (Tree.root ("new child", "new tail")));
				state.view.collapseRow (0); return null;
			});
			drain ();
			edt (() -> {
				assertEquals (new Sample (1, 1, 2, "new tail"), state.latest ());
				assertEquals (0, old.getTreeModelListeners ().length);
				old.reload (); return null;
			});
			drain ();
			edt (() -> { assertEquals (4, state.samples.size ()); return null; });
		}
	}

	@Test void replacedTableModelAndSorterCannotScheduleOldWork () throws Exception {
		try (Tracked<Table> state = table ()) {
			DefaultTableModel old = edt (() -> (DefaultTableModel)state.view.getModel ());
			RowSorter<?> sorter = edt (() -> state.view.getRowSorter ());
			edt (() -> {
				old.addTableModelListener (event -> { }); sorter.addRowSorterListener (event -> { });
				old.setValueAt ("queued old", 0, 0);
				state.view.setModel (new DefaultTableModel (new Object [][] {{"new"}}, new Object [] {"name"})); return null;
			});
			drain ();
			edt (() -> {
				assertEquals (new Sample (0, 1, 2, "0:new"), state.latest ());
				assertEquals (1, old.getTableModelListeners ().length, "unrelated listener retained");
				assertEquals (1, sorterListeners (sorter), "unrelated listener retained");
				old.setValueAt ("obsolete", 0, 0); sorter.toggleSortOrder (0); return null;
			});
			drain ();
			edt (() -> { assertEquals (2, state.samples.size ()); return null; });
		}
	}

	private static int modelListeners (JComponent view) {
		if (view instanceof JTable table) return ((DefaultTableModel)table.getModel ()).getTableModelListeners ().length;
		if (view instanceof JList<?> list) return ((DefaultListModel<?>)list.getModel ()).getListDataListeners ().length;
		return ((DefaultTreeModel)((JTree)view).getModel ()).getTreeModelListeners ().length;
	}

	@Test void closeReleasesSubscriptionsAndCancelsQueuedCallbacks () throws Exception {
		List<List<Integer>> observed = new ArrayList<> ();
		edt (() -> {
			for (JComponent view : new JComponent [] {new Table (), new ListView (), new Tree ()}) {
				int properties = view.getPropertyChangeListeners ().length, hierarchy = view.getHierarchyListeners ().length;
				int model = modelListeners (view);
				int expansion = view instanceof JTree tree ? tree.getTreeExpansionListeners ().length : 0;
				int sorters = view instanceof JTable table ? sorterListeners (table.getRowSorter ()) : 0;
				Class<?> type = view instanceof JTable ? JTable.class : view instanceof JList<?> ? JList.class : JTree.class;
				List<Integer> calls = new ArrayList<> (); observed.add (calls);
				M3ViewportBinding binding = install (view, type, 1, (a, b, c) -> calls.add (a));
				try {
					assertEquals (model + 1, modelListeners (view));
					assertEquals (properties + 1, view.getPropertyChangeListeners ().length);
					if (view instanceof JTable table) ((DefaultTableModel)table.getModel ()).fireTableDataChanged ();
					if (view instanceof JList<?> list) ((DefaultListModel<?>)list.getModel ()).clear ();
					if (view instanceof JTree tree) tree.expandRow (0);
				} finally { binding.close (); }
				binding.close ();
				assertEquals (model, modelListeners (view)); assertEquals (properties, view.getPropertyChangeListeners ().length);
				assertEquals (hierarchy, view.getHierarchyListeners ().length);
				if (view instanceof JTree tree) assertEquals (expansion, tree.getTreeExpansionListeners ().length);
				if (view instanceof JTable table) assertEquals (sorters, sorterListeners (table.getRowSorter ()));
			}
			return null;
		});
		drain ();
		edt (() -> { observed.forEach (calls -> assertEquals (1, calls.size ())); return null; });
	}

	@Test void failedInstallAndInvalidInputsLeaveNoSubscriptions () throws Exception {
		assertFalse (SwingUtilities.isEventDispatchThread ());
		assertThrows (IllegalStateException.class, () -> install (null, JTable.class, 0, (a, b, c) -> { }));
		edt (() -> {
			Table table = new Table ();
			int properties = table.getPropertyChangeListeners ().length, hierarchy = table.getHierarchyListeners ().length;
			int model = modelListeners (table), sorter = sorterListeners (table.getRowSorter ());
			assertThrows (IllegalArgumentException.class, () -> install (table, JTable.class, 0,
					(a, b, c) -> { throw new IllegalArgumentException ("failed target"); }));
			assertThrows (NullPointerException.class, () -> install (table, JTable.class, 0, null));
			assertThrows (IllegalArgumentException.class, () -> install (table, JTable.class, -1, (a, b, c) -> { }));
			assertEquals (properties, table.getPropertyChangeListeners ().length);
			assertEquals (hierarchy, table.getHierarchyListeners ().length);
			assertEquals (model, modelListeners (table)); assertEquals (sorter, sorterListeners (table.getRowSorter ()));
			return null;
		});
	}

	@Test void hiddenChangesWaitForTheExistingShowingNotification () throws Exception {
		try (Tracked<Table> state = table ()) {
			edt (() -> { state.view.showing = false; ((DefaultTableModel)state.view.getModel ()).setRowCount (0); return null; });
			drain ();
			edt (() -> {
				assertEquals (1, state.samples.size ()); assertEquals (1, state.binding.deferred ());
				state.view.showing = true;
				state.binding.hierarchyChanged (new HierarchyEvent (state.view, HierarchyEvent.HIERARCHY_CHANGED,
						state.view, null, HierarchyEvent.SHOWING_CHANGED));
				assertEquals (new Sample (0, 0, 0, "-"), state.latest ()); return null;
			});
		}
	}

	@Test void targetPopulationDoesNotFeedBackButExternalChangesStillPush () throws Exception {
		try (Tracked<Table> state = table ()) {
			edt (() -> {
				state.afterPush = () -> ((DefaultTableModel)state.view.getModel ()).setValueAt ("same", 0, 0);
				state.binding.sync (); assertEquals (2, state.samples.size (), "explicit sync is immediate"); return null;
			});
			drain (); drain ();
			edt (() -> {
				assertEquals (2, state.samples.size (), "own synchronous data events cannot form a loop");
				((DefaultTableModel)state.view.getModel ()).setValueAt ("external", 1, 0); return null;
			});
			drain (); drain ();
			edt (() -> { assertEquals (3, state.samples.size ()); return null; });
		}
	}

	@Test void failedDeferredTargetDoesNotLatchTheQueue () throws Exception {
		AtomicReference<Throwable> observed = new AtomicReference<> ();
		Thread.UncaughtExceptionHandler previous = edt (() -> {
			Thread thread = Thread.currentThread ();
			Thread.UncaughtExceptionHandler old = thread.getUncaughtExceptionHandler ();
			thread.setUncaughtExceptionHandler ((owner, failure) -> observed.set (failure)); return old;
		});
		try (Tracked<Table> state = table ()) {
			edt (() -> {
				state.afterPush = () -> { throw new IllegalArgumentException ("deferred target failed"); };
				((DefaultTableModel)state.view.getModel ()).setValueAt ("first", 0, 0); return null;
			});
			drain ();
			assertNotNull (observed.get ()); assertEquals ("deferred target failed", observed.get ().getMessage ());
			edt (() -> { state.afterPush = null; ((DefaultTableModel)state.view.getModel ()).setValueAt ("second", 0, 0); return null; });
			drain ();
			edt (() -> { assertEquals (3, state.samples.size ()); return null; });
		} finally { edt (() -> { Thread.currentThread ().setUncaughtExceptionHandler (previous); return null; }); }
	}

	@Test void replacingTheModelInsideTargetStillRebindsOwnership () throws Exception {
		try (Tracked<Table> state = table ()) {
			DefaultTableModel old = edt (() -> (DefaultTableModel)state.view.getModel ());
			edt (() -> {
				state.afterPush = () -> {
					state.afterPush = null;
					state.view.setModel (new DefaultTableModel (new Object [][] {{"replacement"}}, new Object [] {"name"}));
				};
				state.binding.sync (); return null;
			});
			drain ();
			edt (() -> {
				assertEquals (2, state.samples.size ()); assertEquals (0, old.getTableModelListeners ().length);
				((DefaultTableModel)state.view.getModel ()).setValueAt ("later", 0, 0); return null;
			});
			drain ();
			edt (() -> { assertEquals (new Sample (0, 1, 2, "0:later"), state.latest ()); assertEquals (3, state.samples.size ()); return null; });
		}
	}

	@Test void legacyUniformAndMeasuredFactoriesRemainManual () throws Exception {
		List<M3ViewportBinding> bindings = new ArrayList<> ();
		List<Integer> calls = new ArrayList<> ();
		try {
			edt (() -> {
				Table table = new Table (); ListView list = new ListView (); Tree tree = new Tree ();
				int tables = modelListeners (table), lists = modelListeners (list), trees = modelListeners (tree);
				bindings.add (M3ViewportBinding.install ((JComponent)table, 20, 0, (a, b, c) -> calls.add (0)));
				bindings.add (M3ViewportBinding.install (table, 0, (a, b, c) -> calls.add (1)));
				bindings.add (M3ViewportBinding.install (list, 0, (a, b, c) -> calls.add (2)));
				bindings.add (M3ViewportBinding.install (tree, 0, (a, b, c) -> calls.add (3)));
				assertEquals (List.of (0, 1, 2, 3), calls);
				assertEquals (tables, modelListeners (table)); assertEquals (lists, modelListeners (list)); assertEquals (trees, modelListeners (tree));
				((DefaultTableModel)table.getModel ()).fireTableDataChanged (); list.model ().clear (); tree.expandRow (0); return null;
			});
			drain ();
			assertEquals (List.of (0, 1, 2, 3), calls);
		} finally { edt (() -> { bindings.forEach (M3ViewportBinding::close); return null; }); }
	}
}
