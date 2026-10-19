/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3RangeSet;
import java.util.Arrays;

/** Selection algebra on sorted ranges: select-all, ranges, invert and shifting inserts/removes, no per-row objects. */
public final class RangeSetExample {
    private static final System.Logger LOG = System.getLogger ("RangeSetExample");
    public static void main(String[] args) {
        M3RangeSet selection = new M3RangeSet();
        selection.setLogicalCount(1_000_000_000);
        selection.selectAll();                                 // O(1)
        selection.deselectRange(10, 999_999_990);             // two runs remain
        LOG.log (System.Logger.Level.INFO, String.valueOf ("selected=" + selection.selectedCount() + " ranges=" + selection.rangeCount()));
        selection.invert();                                    // O(1)
        selection.insert(5, 3);                                // rows shift
        selection.remove(0, 2);
        LOG.log (System.Logger.Level.INFO, String.valueOf ("after invert/insert/remove: selected=" + selection.selectedCount()
                + " first range [" + selection.rangeStart(0) + ", " + selection.rangeEndExclusive(0) + ")"));
        M3RangeSet small = new M3RangeSet();
        small.setLogicalCount(20);
        small.selectRange(2, 5);
        small.setSelected(9, true);
        LOG.log (System.Logger.Level.INFO, String.valueOf ("small=" + Arrays.toString(small.toArray())));
        if (selection.selectedCount() != 999_999_980 || small.toArray().length != 4) throw new AssertionError("range example");
    }
}
