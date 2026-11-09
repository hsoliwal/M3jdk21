/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */
// Template of gen_atom_tests.py / gen_atom_perf.py: the generated copy replaces this header.

/*
 * @test
 * @summary JIT/perf harness of the hot atoms of sun.swing.list (row lookup, location to row, piece edits, the delta
 *          layout): one static final MethodHandle per atom called with invokeExact, warm-up, interleaved rounds,
 *          machine load; prints PERF lines (ns per operation). Run with -XX:+PrintCompilation to see the tier reached.
 * @modules java.desktop/sun.swing.list:open
 * @run main/othervm -Djava.awt.headless=true -Dm3.swing.viewport=true -Dperf.rounds=1 -Dperf.warmup=1 M3ListAtomsPerf
 */

import java.lang.invoke.MethodHandle;
import java.util.Random;

import javax.swing.CellRendererPane;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.basic.BasicListUI;

import sun.swing.list.M3ListMeasure;
import sun.swing.list.M3ListRows;
import sun.swing.list.M3PrefixRowOffsets;
import sun.swing.list.M3RowPieces;

public class M3ListAtomsPerf {

    static final int ROWS = 1 << 17;
    static final int LANE = 1 << 13;
    static final int MAX_HEIGHT = 40;
    static final int PIECE_EDITS = 32;
    static final int LIST_ROWS = 10_000;
    static final String[] VALUES = {"short", "a somewhat longer row value"};

    static final int[] HEIGHTS = new int[ROWS];
    static final int[] LANE_HEIGHTS = new int[LANE];
    static final M3PrefixRowOffsets PREFIX;
    static final M3ListRows LIST_ROWS_INDEX = new M3ListRows();
    static final Object UI = new BasicListUI();
    static final int TOTAL;
    static final int[] ROW_STREAM = new int[PerfSupport.STREAM];
    static final long[] OFFSET_STREAM = new long[PerfSupport.STREAM];
    static final int[] Y_STREAM = new int[PerfSupport.STREAM];
    static final int[] AT_STREAM = new int[PerfSupport.STREAM];
    static final int[] PIECE_STREAM = new int[PerfSupport.STREAM];
    static final int[] LIST_ROW_STREAM = new int[PerfSupport.STREAM];
    static final M3RowPieces PIECES = new M3RowPieces();
    static final M3RowPieces EDIT = new M3RowPieces();
    static final DefaultListModel<Object> MODEL = new DefaultListModel<>();
    static final JList<Object> LIST;
    static final Object LIST_UI;
    static final M3ListMeasure MEASURE = new M3ListMeasure();
    static final CellRendererPane PANE = new CellRendererPane();

    static {
        Random rnd = new Random(AtomSupport.SEED_BASE);
        for (int i = 0; i < ROWS; i++) {
            HEIGHTS[i] = 1 + rnd.nextInt(MAX_HEIGHT);
        }
        for (int i = 0; i < LANE; i++) {
            LANE_HEIGHTS[i] = 1 + rnd.nextInt(MAX_HEIGHT);
        }
        PREFIX = new M3PrefixRowOffsets(HEIGHTS);
        TOTAL = PREFIX.startOf(ROWS);
        if (!LIST_ROWS_INDEX.indexes(UI, HEIGHTS)) {
            throw new AssertionError("run with -Dm3.swing.viewport=true");
        }
        PIECES.reset(ROWS);
        for (int e = 0; e < PIECE_EDITS; e++) {
            int r = rnd.nextInt(ROWS - 1);
            PIECES.change(r, r);
        }
        for (int i = 0; i < PerfSupport.STREAM; i++) {
            ROW_STREAM[i] = rnd.nextInt(ROWS);
            OFFSET_STREAM[i] = rnd.nextInt(TOTAL);
            Y_STREAM[i] = rnd.nextInt(TOTAL);
            AT_STREAM[i] = rnd.nextInt(ROWS - 1);
            PIECE_STREAM[i] = rnd.nextInt(PIECES.count());
            LIST_ROW_STREAM[i] = rnd.nextInt(LIST_ROWS);
        }
        for (int i = 0; i < LIST_ROWS; i++) {
            MODEL.addElement("row " + i);
        }
        LIST = new JList<>(MODEL);
        LIST_UI = LIST.getUI();
        MODEL.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                MEASURE.added(e.getIndex0(), e.getIndex1());
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                MEASURE.removed(e.getIndex0(), e.getIndex1());
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                MEASURE.changed(e.getIndex0(), e.getIndex1());
            }
        });
        MEASURE.measure(LIST_UI, LIST, PANE);
    }

    /** The stock BasicListUI loops, as the JDK references of the row lookups. */
    static int stockStartOf(int row) {
        int y = 0;
        for (int i = 0; i < row; i++) {
            y += HEIGHTS[i];
        }
        return y;
    }

    static int stockRowAt(int y0) {
        int y = 0;
        for (int i = 0; i < ROWS; i++) {
            if (y0 >= y && y0 < y + HEIGHTS[i]) {
                return i;
            }
            y += HEIGHTS[i];
        }
        return ROWS - 1;
    }

//@@FIELDS@@

//@@METHODS@@

    public static void main(String[] args) throws Throwable {
        PerfSupport.Bench[] benches = {
//@@TABLE@@
        };
        System.out.println("SINK\t" + PerfSupport.measure(benches));
    }
}
