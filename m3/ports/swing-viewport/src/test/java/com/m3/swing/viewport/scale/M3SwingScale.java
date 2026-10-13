/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport.scale;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.LongSupplier;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.DefaultListModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.m3.swing.viewport.M3PackedList;
import com.m3.swing.viewport.M3PackedListModel;
import com.m3.swing.viewport.M3PackedTable;
import com.m3.swing.viewport.M3PackedTableModel;
import com.m3.swing.viewport.M3PackedTree;
import com.m3.swing.viewport.M3TreeSelectionModel;
import com.m3.swing.viewport.M3TreeUI;
import com.m3.swing.viewport.M3VirtualListModel;
import com.m3.swing.viewport.M3VirtualTableModel;
import com.m3.swing.viewport.M3VirtualTreeModel;
import com.m3.swing.viewport.engine.residency.ItemResidency;

/**
 * Headless scale runs (no window): builds a model and its component, paints 800x600 frames into a
 * {@code BufferedImage} at the start, middle and end, and prints one TSV row. Allocation per frame
 * comes from {@code com.sun.management.ThreadMXBean}; "materialized" counts objects the model
 * created for paint (values, proxies, TreePaths, fills, page-ins); target 0 for the M3 modes.
 *
 * <pre>M3SwingScale MODE ITEMS     MODE: table-virtual table-packed table-default list-virtual
 *                                   list-packed list-default tree-virtual tree-default
 * tree modes take ITEMS as a fan-out list, e.g. 1000,1000,1000</pre>
 */
public final class M3SwingScale {
	static final int W = 800, H = 600, WARM = 5, FRAMES = 30;
	static final com.sun.management.ThreadMXBean THREADS = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean ();
	static final BufferedImage IMAGE = new BufferedImage (W, H, BufferedImage.TYPE_INT_ARGB);

	private M3SwingScale () {
	}

	/** Per-run counters: what paint asked the model to create. */
	static final class Counters {
		long getValueAt, getElementAt, getChild;
	}

	public static void main (String [] args) throws Exception {
		Locale.setDefault (Locale.ROOT);
		String mode = args [0];
		String items = args [1];
		long t0 = System.nanoTime ();
		Run run = switch (mode) {
			case "table-virtual" -> tableVirtual (Integer.parseInt (items));
			case "table-packed" -> tablePacked (Integer.parseInt (items));
			case "table-default" -> tableDefault (Integer.parseInt (items));
			case "list-virtual" -> listVirtual (Integer.parseInt (items));
			case "list-packed" -> listPacked (Integer.parseInt (items));
			case "list-default" -> listDefault (Integer.parseInt (items));
			case "tree-virtual" -> treeVirtual (fanout (items));
			case "tree-default" -> treeDefault (fanout (items));
			default -> throw new IllegalArgumentException (mode);
		};
		run.buildMs = (System.nanoTime () - t0) / 1e6;
		run.measure ();
		run.after ();
		System.out.println (run.header ());
		System.out.println (run.row (mode));
	}

	static int [] fanout (String s) {
		return Arrays.stream (s.split (",")).mapToInt (Integer::parseInt).toArray ();
	}

	/** One measured configuration. */
	abstract static class Run {
		JComponent view;
		long items;
		int rowHeight, rows;
		double buildMs;
		final double [] frameMs = new double [3 * FRAMES];
		final long [] frameBytes = new long [3 * FRAMES];
		long materialized;
		double heapMb;
		String extra = "";
		LongSupplier madeByPaint = () -> 0;

		/** Moves the residency viewport (never paint's job). */
		void viewport (int firstRow, int visibleRows) {
		}

		void after () throws Exception {
		}

		void measure () {
			view.setSize (W, (int) Math.min (Integer.MAX_VALUE, (long) rows * rowHeight));
			int visible = H / rowHeight + 1;
			int [] firsts = {0, rows / 2, Math.max (0, rows - visible)};
			int f = 0;
			for (int first : firsts) {
				viewport (first, visible);
				int y = first * rowHeight;
				for (int i = 0; i < WARM; i++) frame (y);
				long before = madeByPaint.getAsLong ();
				for (int i = 0; i < FRAMES; i++, f++) {
					long b0 = THREADS.getCurrentThreadAllocatedBytes ();
					long n0 = System.nanoTime ();
					frame (y);
					frameMs [f] = (System.nanoTime () - n0) / 1e6;
					frameBytes [f] = THREADS.getCurrentThreadAllocatedBytes () - b0;
				}
				materialized += madeByPaint.getAsLong () - before;
			}
			heapMb = heapAfterGc ();
		}

		void frame (int y) {
			Graphics2D g = IMAGE.createGraphics ();
			try {
				g.translate (0, -y);
				g.setClip (0, y, W, H);
				view.paint (g);
			} finally {
				g.dispose ();
			}
		}

		String header () {
			return "mode\titems\trows\tbuild_ms\theap_after_gc_mb\tframe_p50_ms\tframe_p95_ms\talloc_bytes_per_frame_p50\talloc_bytes_per_frame_max\tmaterialized_per_paint\textra";
		}

		String row (String mode) {
			double [] ms = frameMs.clone ();
			long [] bytes = frameBytes.clone ();
			Arrays.sort (ms);
			Arrays.sort (bytes);
			return String.format ("%s\t%d\t%d\t%.0f\t%.1f\t%.3f\t%.3f\t%d\t%d\t%.2f\t%s", mode, items, rows, buildMs, heapMb, ms [ms.length / 2],
					ms [(int) (ms.length * 0.95)], bytes [bytes.length / 2], bytes [bytes.length - 1], materialized / (double) (3 * FRAMES), extra);
		}
	}

	static double heapAfterGc () {
		for (int i = 0; i < 3; i++) System.gc ();
		return ManagementFactory.getMemoryMXBean ().getHeapMemoryUsage ().getUsed () / 1048576.0;
	}

	static String text (int row, int column) {
		return "row " + row + " col " + column;
	}

	// ---- tables --------------------------------------------------------------------------------

	static Run tableVirtual (int n) {
		int flags = ItemResidency.RESIDENCY_SWAP | ItemResidency.RESIDENCY_DESTROY | ItemResidency.IDENTITY_PIN_ALL | ItemResidency.PRESSURE_SAMPLED;
		M3VirtualTableModel model = new M3VirtualTableModel (new String [] {"a", "b", "c"}, n, M3SwingScale::text, flags, 4096);
		M3PackedTable table = new M3PackedTable (model);
		Run r = new Run () {
			@Override
			void viewport (int first, int visible) {
				model.setViewport (first, visible, visible / 2);
			}

			@Override
			void after () {
				extra = swapRun (model, n) + " fills=" + model.fills () + " placeholders=" + table.packedRenderer ().placeholders ();
			}
		};
		r.view = table;
		r.items = n;
		r.rows = n;
		r.rowHeight = table.getRowHeight ();
		r.madeByPaint = () -> model.materializedDuringPaint ();
		return r;
	}

	/** The engine's flat swap scenario: scroll 200,000 rows, edit every 10th, scroll back and time swap-ins. */
	static String swapRun (M3VirtualTableModel model, int n) {
		int region = Math.min (200_000, n), window = 40;
		int start = n / 2 - region / 2;
		for (int first = start; first < start + region; first += window) {
			model.setViewport (first, window, window / 2);
			for (int r = first; r < first + window; r += 10) {
				if ((r - start) % 10 == 0) model.setValueAt ("edited " + r, r, 0);
			}
		}
		int swapIns0 = model.stat (ItemResidency.STAT_SWAP_IN);
		long [] us = new long [region / 10];
		int k = 0;
		for (int first = start + region - window; first >= start; first -= window) {
			model.setViewport (first, window, window / 2);
			for (int r = first; r < first + window; r += 10) {
				if ((r - start) % 10 != 0 || k >= us.length) continue;
				long t = System.nanoTime ();
				Object v = model.getValueAt (r, 0);
				us [k++] = System.nanoTime () - t;
				if (!("edited " + r).equals (v)) throw new IllegalStateException ("reproduction failed at row " + r + ": " + v);
			}
		}
		long [] s = Arrays.copyOf (us, k);
		Arrays.sort (s);
		return String.format ("swap_out=%d swap_in=%d swap_in_us_p50=%.1f p95=%.1f p99=%.1f reproduced=%d", model.stat (ItemResidency.STAT_SWAP_OUT),
				model.stat (ItemResidency.STAT_SWAP_IN) - swapIns0, s [s.length / 2] / 1e3, s [(int) (s.length * 0.95)] / 1e3, s [(int) (s.length * 0.99)] / 1e3, k);
	}

	static Run tablePacked (int n) {
		M3PackedTableModel model = new M3PackedTableModel (new Object [] {"id", "name", "flag"}, 0);
		model.beginBatch ();
		for (int i = 0; i < n; i++) model.addRow (new Object [] {i, "name " + (i % 1000), (i & 1) == 0});
		model.endBatch ();
		M3PackedTable table = new M3PackedTable (model);
		Run r = new Run () {
			@Override
			void after () {
				extra = "packed_bytes=" + model.packedBytes ();
			}
		};
		r.view = table;
		r.items = n;
		r.rows = n;
		r.rowHeight = table.getRowHeight ();
		return r;
	}

	static Run tableDefault (int n) {
		Counters c = new Counters ();
		DefaultTableModel model = new DefaultTableModel (new Object [] {"id", "name", "flag"}, 0) {
			private static final long serialVersionUID = 1L;

			@Override
			public Object getValueAt (int row, int column) {
				c.getValueAt++;
				return super.getValueAt (row, column);
			}
		};
		for (int i = 0; i < n; i++) model.addRow (new Object [] {i, "name " + (i % 1000), (i & 1) == 0});
		JTable table = new JTable (model);
		Run r = new Run () {
		};
		r.view = table;
		r.items = n;
		r.rows = n;
		r.rowHeight = table.getRowHeight ();
		r.madeByPaint = () -> c.getValueAt;
		return r;
	}

	// ---- lists ---------------------------------------------------------------------------------

	static Run listVirtual (int n) {
		M3VirtualListModel model = new M3VirtualListModel (n, M3SwingScale::text);
		M3PackedList<String> list = new M3PackedList<> (model);
		Run r = new Run () {
			@Override
			void viewport (int first, int visible) {
				model.setViewport (first, visible, visible / 2);
			}

			@Override
			void after () {
				extra = "fills=" + model.fills () + " placeholders=" + list.packedRenderer ().placeholders ();
			}
		};
		r.view = list;
		r.items = n;
		r.rows = n;
		r.rowHeight = list.getFixedCellHeight ();
		r.madeByPaint = () -> model.materializedDuringPaint ();
		return r;
	}

	static Run listPacked (int n) {
		M3PackedListModel<String> model = new M3PackedListModel<> ();
		model.beginBatch ();
		for (int i = 0; i < n; i++) model.addElement ("item " + i);
		model.endBatch ();
		M3PackedList<String> list = new M3PackedList<> (model);
		Run r = new Run () {
		};
		r.view = list;
		r.items = n;
		r.rows = n;
		r.rowHeight = list.getFixedCellHeight ();
		return r;
	}

	static Run listDefault (int n) {
		Counters c = new Counters ();
		DefaultListModel<String> model = new DefaultListModel<> () {
			private static final long serialVersionUID = 1L;

			@Override
			public String getElementAt (int index) {
				c.getElementAt++;
				return super.getElementAt (index);
			}
		};
		java.util.List<String> all = new java.util.ArrayList<> (n);
		for (int i = 0; i < n; i++) all.add ("item " + i);
		model.addAll (all);
		JList<String> list = new JList<> (model);
		list.setFixedCellHeight (18);
		list.setFixedCellWidth (256);
		Run r = new Run () {
		};
		r.view = list;
		r.items = n;
		r.rows = n;
		r.rowHeight = 18;
		r.madeByPaint = () -> c.getElementAt;
		return r;
	}

	// ---- trees ---------------------------------------------------------------------------------

	static Run treeVirtual (int [] fanout) {
		M3VirtualTreeModel model = new M3VirtualTreeModel (fanout, "root", (k, c) -> "node " + k);
		M3PackedTree tree = new M3PackedTree (model);
		model.expandToDepth (fanout.length - 1 > 1 ? 1 : fanout.length - 1);
		int lastTop = fanout [0] - 1;
		int [] rows = {0};
		Run r = new Run () {
			@Override
			void viewport (int first, int visible) {
				model.setViewportRows (first, visible, visible / 2);
			}

			@Override
			void after () {
				M3TreeSelectionModel sel = (M3TreeSelectionModel) tree.getSelectionModel ();
				sel.selectKeyRange (0, model.keyCount ());
				int all = sel.getSelectionCount ();
				sel.clearSelection ();
				sel.selectPattern (0, model.keyCount (), 3, 0b001);
				int third = sel.getSelectionCount ();
				int before = tree.getRowCount ();
				tree.collapseRow (0);
				int collapsed = tree.getRowCount ();
				M3TreeUI ui = (M3TreeUI) tree.getUI ();
				extra = String.format ("keys=%d select_all=%d every_3rd=%d rows=%d collapse_first=%d shape_path_nodes=%d proxies=%d tree_paths=%d fills=%d placeholders=%d",
						model.keyCount (), all, third, before, collapsed, model.shapePathNodes (), model.proxiesCreated (), ui.rows ().pathsCreated (),
						model.fills (), ui.stamp ().placeholders ());
			}
		};
		tree.setSize (W, H);
		// The end region: expand the last top-level node's last child if there is a third level.
		if (fanout.length > 2) {
			int lastChild = model.keyAtRow (model.visibleRowCount () - 1);
			model.setExpandedKey (lastChild, true);
		}
		rows [0] = tree.getRowCount ();
		r.view = tree;
		long keys = model.keyCount ();
		r.items = keys;
		r.rows = rows [0];
		r.rowHeight = tree.getRowHeight ();
		M3TreeUI ui = (M3TreeUI) tree.getUI ();
		r.madeByPaint = () -> model.materializedDuringPaint () + model.proxiesCreated () + ui.rows ().pathsCreated ();
		if (lastTop < 0) throw new IllegalStateException ();
		return r;
	}

	static Run treeDefault (int [] fanout) {
		Counters c = new Counters ();
		DefaultMutableTreeNode root = new DefaultMutableTreeNode ("root");
		build (root, fanout, 0, new long [] {0});
		DefaultTreeModel model = new DefaultTreeModel (root) {
			private static final long serialVersionUID = 1L;

			@Override
			public Object getChild (Object parent, int index) {
				c.getChild++;
				return super.getChild (parent, index);
			}
		};
		JTree tree = new JTree (model);
		tree.setRootVisible (false);
		tree.setShowsRootHandles (true);
		tree.setRowHeight (18);
		tree.setLargeModel (true);
		if (fanout.length > 1) {
			for (int i = 0; i < root.getChildCount (); i++) tree.expandPath (new TreePath (((DefaultMutableTreeNode) root.getChildAt (i)).getPath ()));
		}
		long nodes = 1;
		long level = 1;
		for (int f : fanout) {
			level *= f;
			nodes += level;
		}
		Run r = new Run () {
		};
		tree.setSize (W, H);
		r.view = tree;
		r.items = nodes - 1;
		r.rows = tree.getRowCount ();
		r.rowHeight = 18;
		r.madeByPaint = () -> c.getChild;
		r.extra = "getChild_counts_TreePath_rows";
		return r;
	}

	static void build (DefaultMutableTreeNode node, int [] fanout, int depth, long [] next) {
		if (depth == fanout.length) return;
		for (int i = 0; i < fanout [depth]; i++) {
			DefaultMutableTreeNode child = new DefaultMutableTreeNode ("node " + next [0]++);
			node.add (child);
			build (child, fanout, depth + 1, next);
		}
	}
}
