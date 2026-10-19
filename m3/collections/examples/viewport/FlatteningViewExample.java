/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3FlatteningView;

/** A 10^6-node tree (1,000 roots x 1,000 children, computed, not stored) flattened with expand/collapse. */
public final class FlatteningViewExample {
    private static final System.Logger LOG = System.getLogger ("FlatteningViewExample");
    record Node(int parent, int index) { }                    // parent -1: a root

    public static void main(String[] args) {
        M3FlatteningView<Node> rows = new M3FlatteningView<>(new M3FlatteningView.Source<>() {
            @Override public int childCount(Node parent) { return parent == null ? 1000 : parent.parent() < 0 ? 1000 : 0; }
            @Override public Node child(Node parent, int index) { return new Node(parent == null ? -1 : parent.index(), index); }
        });
        long start = System.nanoTime();
        for (int root = 999; root >= 0; root -= 2) rows.expand(root);   // expand every other root, bottom up
        long micros = (System.nanoTime() - start) / 1000;
        LOG.log (System.Logger.Level.INFO, String.valueOf ("rows=" + rows.size() + " materialized=" + rows.materializedCount() + " expandAllMicros=" + micros));
        LOG.log (System.Logger.Level.INFO, String.valueOf ("row 1001 = " + rows.get(1001) + " depth " + rows.depth(1001)));
        if (rows.size() != 1000 + 500 * 1000 || rows.depth(1001) != 1) throw new AssertionError("flattening example");
        rows.collapse(1);
        LOG.log (System.Logger.Level.INFO, String.valueOf ("after collapsing root 1: rows=" + rows.size()));
    }
}
