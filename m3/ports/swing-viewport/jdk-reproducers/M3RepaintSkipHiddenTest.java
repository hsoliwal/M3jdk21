/* SPDX-License-Identifier: Apache-2.0 */

/*
 * @test
 * @summary M3 leaf 2 (java.desktop): RepaintManager skips dirty roots that stopped showing before
 *          building their access-control context; nothing that was painted before is skipped.
 * @key headful
 * @run main/othervm M3RepaintSkipHiddenTest
 */

import java.awt.Graphics;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Headful. 200 components in a dialog are marked dirty and the dialog is hidden in the same EDT
 * task, so the next paint cycle sees 200 non-showing dirty roots. The result must be identical with
 * and without the change (nothing painted while hidden, the visible frame painted); the change
 * only removes the per-root work, reported as bytes allocated by the paint cycle.
 */
public final class M3RepaintSkipHiddenTest {
	static final AtomicInteger HIDDEN_PAINTS = new AtomicInteger (), VISIBLE_PAINTS = new AtomicInteger ();

	static final class Counted extends JComponent {
		private static final long serialVersionUID = 1L;
		private final AtomicInteger counter;

		Counted (AtomicInteger counter) {
			this.counter = counter;
			setOpaque (true);
		}

		@Override
		protected void paintComponent (Graphics g) {
			counter.incrementAndGet ();
			g.fillRect (0, 0, getWidth (), getHeight ());
		}
	}

	public static void main (String [] args) throws Exception {
		com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean ();
		JFrame [] frame = new JFrame [1];
		JDialog [] dialog = new JDialog [1];
		Counted [] parts = new Counted [200];
		Counted [] shown = new Counted [1];
		SwingUtilities.invokeAndWait (() -> {
			frame [0] = new JFrame ("M3RepaintSkipHiddenTest");
			shown [0] = new Counted (VISIBLE_PAINTS);
			frame [0].add (shown [0]);
			frame [0].setSize (200, 120);
			frame [0].setLocation (40, 40);
			frame [0].setVisible (true);
			dialog [0] = new JDialog (frame [0]);
			dialog [0].setLayout (null);
			for (int i = 0; i < parts.length; i++) {
				parts [i] = new Counted (HIDDEN_PAINTS);
				parts [i].setBounds ((i % 20) * 10, (i / 20) * 10, 9, 9);
				dialog [0].add (parts [i]);
			}
			dialog [0].setSize (220, 140);
			dialog [0].setLocation (260, 40);
			dialog [0].setVisible (true);
		});
		Thread.sleep (1500);
		long [] bytes = new long [5];
		for (int round = 0; round < bytes.length; round++) {
			long [] before = new long [1];
			SwingUtilities.invokeAndWait (() -> {
				HIDDEN_PAINTS.set (0);
				VISIBLE_PAINTS.set (0);
				for (Counted c : parts) c.repaint ();
				shown [0].repaint ();
				dialog [0].setVisible (false);
				before [0] = threads.getCurrentThreadAllocatedBytes ();
			});
			long [] after = new long [1];
			Thread.sleep (300);
			SwingUtilities.invokeAndWait (() -> after [0] = threads.getCurrentThreadAllocatedBytes ());
			bytes [round] = after [0] - before [0];
			if (HIDDEN_PAINTS.get () != 0) throw new RuntimeException ("FAIL: hidden components painted " + HIDDEN_PAINTS.get ());
			if (VISIBLE_PAINTS.get () < 1) throw new RuntimeException ("FAIL: the visible frame was not painted");
			SwingUtilities.invokeAndWait (() -> dialog [0].setVisible (true));
			Thread.sleep (500);
		}
		SwingUtilities.invokeAndWait (() -> {
			dialog [0].dispose ();
			frame [0].dispose ();
		});
		java.util.Arrays.sort (bytes);
		System.out.println ("PASS M3RepaintSkipHiddenTest edt_bytes_per_cycle_median=" + bytes [bytes.length / 2] + " hidden_roots=" + parts.length);
	}
}
