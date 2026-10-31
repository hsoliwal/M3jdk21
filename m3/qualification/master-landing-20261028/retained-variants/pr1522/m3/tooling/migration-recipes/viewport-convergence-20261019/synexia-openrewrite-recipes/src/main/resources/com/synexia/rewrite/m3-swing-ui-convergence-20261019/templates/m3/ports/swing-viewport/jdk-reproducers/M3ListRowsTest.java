/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 swing-viewport leaf 3: JUnit suite of the JList row offsets (sun.swing.list): the
 *          prefix lane and M3ListRows against the stock BasicListUI loops copied as oracles, the
 *          module guard, the per-array versioning, plus the JList differential; JaCoCo target 99%.
 * @modules java.desktop/sun.swing.list java.desktop/javax.swing.plaf.basic:open java.desktop/sun.swing.list:open
 * @build M3JListViewportDifferential
 * @run junit/othervm -Djava.awt.headless=true -Dm3.swing.viewport=true -XX:-OmitStackTraceInFastThrow M3ListRowsTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import javax.swing.plaf.basic.BasicListUI;

import org.junit.jupiter.api.Test;

import sun.swing.list.M3ListRows;
import sun.swing.list.M3PrefixRowOffsets;

public class M3ListRowsTest {

    /** The stock BasicListUI.getCellBounds loop (oracle). */
    static int stockStart(int[] h, int top, int row) {
        int y = top;
        for (int i = 0; i < row; i++) {
            y += h[i];
        }
        return y;
    }

    /** The stock BasicListUI.convertLocationToRow loop for a non-null cellHeights (oracle). */
    static int stockRow(int[] h, int size, int top, int y0, boolean closest) {
        int y = top;
        int row = 0;
        if (closest && y0 < y) {
            return 0;
        }
        int i;
        for (i = 0; i < size; i++) {
            if ((y0 >= y) && (y0 < y + h[i])) {
                return row;
            }
            y += h[i];
            row += 1;
        }
        return i - 1;
    }

    static int[] heights(Random rnd, int n, int kind) {
        int[] h = new int[n];
        for (int i = 0; i < n; i++) {
            int r = rnd.nextInt(100);
            h[i] = switch (kind) {
                case 0 -> r < 15 ? 0 : 1 + rnd.nextInt(40);
                case 1 -> r < 5 ? -1 - rnd.nextInt(30) : rnd.nextInt(40);
                default -> r < 50 ? (1 << 28) + rnd.nextInt(1 << 28) : rnd.nextInt(40);
            };
        }
        return h;
    }

    @Test
    void enabledFollowsTheSystemProperty() {
        assertEquals(Boolean.getBoolean("m3.swing.viewport"), M3ListRows.ENABLED);
    }

    /** startOf equals the stock running int sum for every row, wrapping included. */
    @Test
    void prefixEqualsTheWrappedLoop() {
        Random rnd = new Random(7);
        for (int t = 0; t < 300; t++) {
            int[] h = heights(rnd, rnd.nextInt(60), t % 3);
            M3PrefixRowOffsets o = new M3PrefixRowOffsets(h);
            assertEquals(h.length, o.rowCount());
            for (int r = 0; r <= h.length; r++) {
                assertEquals(stockStart(h, 0, r), o.startOf(r));
            }
        }
    }

    /** monotone: no negative height and the total fits an int. */
    @Test
    void monotoneFlag() {
        assertTrue(new M3PrefixRowOffsets(new int[0]).monotone());
        assertTrue(new M3PrefixRowOffsets(new int[] {0, 3, 0, Integer.MAX_VALUE - 3}).monotone());
        assertFalse(new M3PrefixRowOffsets(new int[] {0, 3, 0, Integer.MAX_VALUE - 2}).monotone());
        assertFalse(new M3PrefixRowOffsets(new int[] {5, -1, 5}).monotone());
        assertFalse(new M3PrefixRowOffsets(new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE, 2}).monotone());
    }

    /** firstEndingAfter: zero heights are skipped, offsets past the end answer rowCount. */
    @Test
    void firstEndingAfter() {
        M3PrefixRowOffsets o = new M3PrefixRowOffsets(new int[] {0, 5, 0, 0, 3, 0});
        assertEquals(1, o.firstEndingAfter(0));
        assertEquals(1, o.firstEndingAfter(4));
        assertEquals(4, o.firstEndingAfter(5));
        assertEquals(4, o.firstEndingAfter(7));
        assertEquals(6, o.firstEndingAfter(8));
        assertEquals(6, o.firstEndingAfter(Long.MAX_VALUE));
        assertEquals(0, new M3PrefixRowOffsets(new int[0]).firstEndingAfter(0));
    }

    /** rowAt equals the stock loop whenever it answers, and answers whenever the sums cannot wrap. */
    @Test
    void rowAtEqualsTheStockLoop() throws Exception {
        Random rnd = new Random(11);
        BasicListUI ui = new BasicListUI();
        int answered = 0;
        int handedBack = 0;
        for (int t = 0; t < 600; t++) {
            int kind = t % 3;
            int[] h = heights(rnd, 1 + rnd.nextInt(50), kind);
            M3ListRows rows = new M3ListRows();
            assertTrue(rows.indexes(ui, h));
            int size = 1 + rnd.nextInt(h.length);
            int top = rnd.nextInt(5) == 0 ? -rnd.nextInt(20) : rnd.nextInt(20);
            if (t % 50 == 0) {
                top = Integer.MAX_VALUE - 10;
            }
            long total = 0;
            boolean negative = false;
            for (int v : h) {
                total += v;
                negative |= v < 0;
            }
            for (int k = 0; k < 40; k++) {
                int y0 = probe(rnd, h, top, size);
                boolean closest = rnd.nextBoolean();
                int got = rows.rowAt(size, top, y0, closest);
                if (got == M3ListRows.STOCK) {
                    handedBack++;
                    assertTrue(negative || total > Integer.MAX_VALUE || (long) top + total > Integer.MAX_VALUE
                            || (long) top + new M3PrefixRowOffsets(h).startOf(size) > Integer.MAX_VALUE,
                            "handed back without a reason");
                } else {
                    answered++;
                    assertEquals(stockRow(h, size, top, y0, closest), got, "y0 " + y0 + " top " + top);
                }
            }
        }
        assertTrue(answered > 5_000, "answered " + answered);
        assertTrue(handedBack > 1_000, "handed back " + handedBack);
    }

    static int probe(Random rnd, int[] h, int top, int size) {
        int r = rnd.nextInt(10);
        if (r == 0) {
            return top - 1 - rnd.nextInt(5);
        }
        if (r == 1) {
            return rnd.nextBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
        int row = rnd.nextInt(size + 1);
        int y = stockStart(h, top, row);
        return y + rnd.nextInt(3) - 1;
    }

    /** Only java.desktop UIs take the path; the cache is keyed by the array's identity. */
    @Test
    void guardAndVersioning() {
        M3ListRows rows = new M3ListRows();
        int[] h = {4, 5, 6};
        assertFalse(rows.indexes(new Object(), h), "java.base class");
        assertFalse(rows.indexes(this, h), "class outside java.desktop");
        assertFalse(rows.indexes(new BasicListUI() { }, h), "subclass outside java.desktop");
        BasicListUI ui = new BasicListUI();
        assertFalse(rows.indexes(ui, null), "no cellHeights");
        assertTrue(rows.indexes(ui, h));
        assertEquals(9, rows.startOf(2));
        h[0] = 100;
        assertTrue(rows.indexes(ui, h));
        assertEquals(9, rows.startOf(2), "same array, same offsets: BasicListUI never writes it in place");
        int[] next = h.clone();
        assertTrue(rows.indexes(ui, next));
        assertNotEquals(9, rows.startOf(2));
        assertEquals(105, rows.startOf(2));
    }

    /** The JList-level differential (stock loops vs prefix lane through every public query and pixel). */
    @Test
    void jlistDifferential() throws Exception {
        if (!M3ListRows.ENABLED) {
            return;
        }
        String summary = M3JListViewportDifferential.runSeeds(1, 24, 200);
        assertTrue(summary.contains(" failed 0 "), summary);
    }
}
