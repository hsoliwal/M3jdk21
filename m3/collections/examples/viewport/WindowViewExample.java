/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3WindowPlan;
import com.m3.collections.M3WindowView;
import java.util.AbstractList;
import java.util.List;

/** A window over a virtual sequence of 10^8 rows: only the window is ever touched; state masks follow the rows. */
public final class WindowViewExample {
    public static void main(String[] args) {
        List<String> rows = new AbstractList<>() {             // nothing stored: row text is computed
            @Override public String get(int i) { return "row " + i; }
            @Override public int size() { return 100_000_000; }
        };
        M3WindowView<String> window = new M3WindowView<>(rows);
        window.move(50_000_000, 10);
        window.flag(3, 1, true);                               // mark row 50,000,003 (for example "bound")
        M3WindowPlan step = window.move(50_000_002, 10);       // scroll by two rows
        System.out.println("window " + window + " retiredHead=" + step.retiredHead() + " insertedTail=" + step.insertedTail());
        if (!window.get(0).equals("row 50000002") || !window.flag(1, 1) || step.retiredHead() != 2)
            throw new AssertionError("window example");
        M3WindowView<String> inner = new M3WindowView<>(window);  // a view of a view
        inner.move(8, 5);
        System.out.println("inner " + inner);
    }
}
