/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */
// Template of gen_atom_tests.py / gen_atom_perf.py: the generated copy replaces this header.

/*
 * @test
 * @summary JIT/perf harness of the hot atoms of sun.swing.table (view/model conversions of the sorter lanes, the
 *          lane sort, row heights by index and position): one static final MethodHandle per atom called with
 *          invokeExact, warm-up, interleaved rounds, machine load, the stock TableRowSorter and SizeSequence as JDK
 *          references; prints PERF lines. Run with -XX:+PrintCompilation to see the tier reached.
 * @modules java.desktop/sun.swing.table:open
 * @run main/othervm -Djava.awt.headless=true -Dperf.rounds=1 -Dperf.warmup=1 M3TableAtomsPerf
 */

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.swing.RowSorter;
import javax.swing.SizeSequence;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import sun.swing.table.M3SorterLanes;
import sun.swing.table.M3SparseSizes;

public class M3TableAtomsPerf {

    static final int ROWS = 1 << 17;
    static final int KEY_RANGE = 1 << 10;
    static final int SORT_N = 1 << 12;
    static final int SIZE_ROWS = 1 << 20;
    static final int DEFAULT_SIZE = 16;
    static final int CUSTOM = 1 << 10;
    static final int MAX_SIZE = 64;

    /** The sorter members over an int lane of keys: ascending keys, ties by model index, no filter. */
    static final class KeyHost implements M3SorterLanes.Host {
        final int[] keys;
        boolean sorted;
        final M3SorterLanes lanes = new M3SorterLanes();

        KeyHost(int[] keys) {
            this.keys = keys;
            lanes.bind(this);
        }

        @Override
        public int compare(int model1, int model2) {
            int r = Integer.compare(keys[model1], keys[model2]);
            return r != 0 ? r : model1 - model2;
        }

        @Override
        public boolean include(int modelRow) {
            return true;
        }

        @Override
        public int modelRowCount() {
            return keys.length;
        }

        @Override
        public boolean unsorted() {
            return false;
        }

        @Override
        public boolean filtered() {
            return false;
        }

        @Override
        public boolean sorted() {
            return sorted;
        }

        @Override
        public void sorted(boolean sorted) {
            this.sorted = sorted;
        }

        @Override
        public void prepareSort() {
        }

        @Override
        public void prepareResort() {
        }

        @Override
        public void sort() {
            lanes.sort();
        }

        @Override
        public void changed(int[] lastViewToModel) {
        }
    }

    static final int[] KEYS = new int[ROWS];
    static final KeyHost HOST;
    static final M3SorterLanes LANES;
    static final TableRowSorter<DefaultTableModel> STOCK_SORTER;
    static final int[] SORT_SRC = new int[SORT_N];
    static final int[] SORT_WORK = new int[SORT_N];
    static final Integer[] SORT_BOXED_SRC = new Integer[SORT_N];
    static final Integer[] SORT_BOXED = new Integer[SORT_N];
    static final Comparator<Integer> ORDER;
    static final int[] SORTED_LANE;
    static final int[] ROW_STREAM = new int[PerfSupport.STREAM];
    static final M3SparseSizes SIZES = new M3SparseSizes(SIZE_ROWS, DEFAULT_SIZE);
    static final SizeSequence STOCK_SIZES = new SizeSequence(SIZE_ROWS, DEFAULT_SIZE);
    static final int[] SIZE_ROW_STREAM = new int[PerfSupport.STREAM];
    static final int[] POSITION_STREAM = new int[PerfSupport.STREAM];
    static final int[] CUSTOM_STREAM = new int[PerfSupport.STREAM];

    static {
        Random rnd = new Random(AtomSupport.SEED_BASE);
        for (int i = 0; i < ROWS; i++) {
            KEYS[i] = rnd.nextInt(KEY_RANGE);
        }
        HOST = new KeyHost(KEYS);
        LANES = HOST.lanes;
        HOST.sort();
        DefaultTableModel model = new DefaultTableModel(new Object[] {"key"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return Integer.class;
            }
        };
        for (int k : KEYS) {
            model.addRow(new Object[] {k});
        }
        STOCK_SORTER = new TableRowSorter<>(model);
        STOCK_SORTER.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        for (int i = 0; i < SORT_N; i++) {
            SORT_SRC[i] = i;
        }
        for (int i = SORT_N - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = SORT_SRC[i];
            SORT_SRC[i] = SORT_SRC[j];
            SORT_SRC[j] = t;
        }
        for (int i = 0; i < SORT_N; i++) {
            SORT_BOXED_SRC[i] = SORT_SRC[i];
        }
        ORDER = HOST::compare;
        SORTED_LANE = LANES.snapshot();
        for (int c = 0; c < CUSTOM; c++) {
            int i = rnd.nextInt(SIZE_ROWS);
            int s = DEFAULT_SIZE + 1 + rnd.nextInt(MAX_SIZE);
            SIZES.setSize(i, s);
            STOCK_SIZES.setSize(i, s);
        }
        int total = SIZES.getPosition(SIZE_ROWS);
        for (int i = 0; i < PerfSupport.STREAM; i++) {
            ROW_STREAM[i] = rnd.nextInt(ROWS);
            SIZE_ROW_STREAM[i] = rnd.nextInt(SIZE_ROWS);
            POSITION_STREAM[i] = rnd.nextInt(total);
            CUSTOM_STREAM[i] = DEFAULT_SIZE + 1 + rnd.nextInt(MAX_SIZE);
        }
        if (!Arrays.equals(STOCK_SIZES.getSizes(), SIZES.getSizes())) {
            throw new AssertionError("the sparse store and the SizeSequence must hold the same sizes");
        }
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
