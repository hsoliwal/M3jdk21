/* SPDX-License-Identifier: Apache-2.0 */

/*
 * @test
 * @summary M3 leaf 2 (java.desktop): the "m3.redrawSuspended" client property defers repaint
 *          requests of a subtree into one union and repaints it once when cleared.
 * @modules java.desktop/javax.swing:open
 * @run main/othervm -Djava.awt.headless=true M3RedrawSuspendTest
 */

import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.RepaintManager;

/**
 * Headless, white-box: reads the RepaintManager's deferred map through reflection. Runs on a JDK
 * built from this branch, or on a stock JDK 21 with the patched classes:
 * {@code java --patch-module java.desktop=<patched classes> --add-opens java.desktop/javax.swing=ALL-UNNAMED M3RedrawSuspendTest}.
 */
public final class M3RedrawSuspendTest {
	private static final String KEY = "m3.redrawSuspended";

	public static void main (String [] args) throws Exception {
		JPanel parent = new JPanel (null);
		JLabel child = new JLabel ("x");
		child.setBounds (10, 20, 50, 30);
		parent.add (child);
		parent.setBounds (0, 0, 400, 300);
		RepaintManager rm = RepaintManager.currentManager (parent);
		Map<?, ?> deferred = deferred (rm);
		AtomicInteger suspended = suspended ();
		int base = suspended.get ();

		parent.repaint (1, 1, 5, 5);
		check (deferred.isEmpty (), "nothing is deferred while no component is suspended");

		parent.putClientProperty (KEY, Boolean.TRUE);
		check (suspended.get () == base + 1, "suspend counted");
		child.repaint (1, 2, 3, 4);
		parent.repaint (100, 100, 5, 5);
		Rectangle r = (Rectangle) deferred.get (parent);
		check (r != null, "damage deferred to the suspended ancestor");
		check (r.equals (new Rectangle (11, 22, 94, 83)), "union in the ancestor's coordinates, got " + r);

		parent.putClientProperty (KEY, Boolean.TRUE);
		check (suspended.get () == base + 1, "setting the same value again does not count twice");

		parent.putClientProperty (KEY, null);
		check (suspended.get () == base, "resume counted");
		check (!deferred.containsKey (parent), "resume flushes the union");

		JPanel outer = new JPanel (null);
		outer.setBounds (0, 0, 800, 600);
		parent.setLocation (7, 9);
		outer.add (parent);
		outer.putClientProperty (KEY, Boolean.TRUE);
		parent.putClientProperty (KEY, Boolean.TRUE);
		child.repaint (0, 0, 2, 2);
		check (deferred.containsKey (parent) && !deferred.containsKey (outer), "the nearest suspended ancestor collects");
		parent.putClientProperty (KEY, Boolean.FALSE);
		Rectangle o = (Rectangle) deferred.get (outer);
		check (o != null && o.equals (new Rectangle (17, 29, 2, 2)), "a nested resume hands the union to the outer batch, got " + o);
		outer.putClientProperty (KEY, null);
		check (deferred.isEmpty () && suspended.get () == base, "all flushed");
		System.out.println ("PASS M3RedrawSuspendTest");
	}

	private static Map<?, ?> deferred (RepaintManager rm) throws Exception {
		Field f = RepaintManager.class.getDeclaredField ("m3Deferred");
		f.setAccessible (true);
		return (Map<?, ?>) f.get (rm);
	}

	private static AtomicInteger suspended () throws Exception {
		Field f = RepaintManager.class.getDeclaredField ("M3_SUSPENDED");
		f.setAccessible (true);
		return (AtomicInteger) f.get (null);
	}

	private static void check (boolean ok, String what) {
		if (!ok) throw new RuntimeException ("FAIL: " + what);
	}
}
