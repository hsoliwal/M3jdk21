/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3BitMask;
import com.m3.collections.M3FlatteningView;
import com.m3.collections.M3PermutationIndex;
import com.m3.collections.M3RadixKey;
import com.m3.collections.M3RangeSet;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

/**
 * A viewport assembled from collections, driving a Swing JTable: M3FlatteningView (tree rows), an M3BitMask filter,
 * an M3PermutationIndex sort and an M3RangeSet selection; the table model only maps rows. The SWT snippet
 * (examples/org.eclipse.swt.snippets/.../m3) drives an SWT Table from the same views.
 * Run with {@code --check} to build and verify the model without a window (works headless).
 */
public final class ViewportFromCollectionsSwingExample {
    private static final System.Logger LOG = System.getLogger ("ViewportFromCollectionsSwingExample");
    record Node(int group, int index) { }

    public static void main(String[] args) {
        M3FlatteningView<Node> tree = new M3FlatteningView<>(new M3FlatteningView.Source<>() {
            @Override public int childCount(Node parent) { return parent == null ? 100 : parent.group() < 0 ? 1_000 : 0; }
            @Override public Node child(Node parent, int index) { return new Node(parent == null ? -1 : parent.index(), index); }
        });
        for (int g = 99; g >= 0; g--) tree.expand(g);            // 100 groups x 1,000 rows: 100,100 rows
        int n = tree.size();
        long[] mask = M3BitMask.ensure(new long[0], n);
        for (int row = 0; row < n; row++) M3BitMask.set(mask, row, tree.get(row).index() % 7 != 0);  // filter
        int[] rows = new int[n];
        int visible = M3BitMask.compact(mask, M3BitMask.words(n), rows, new int[M3BitMask.words(n)]);
        M3PermutationIndex sort = new M3PermutationIndex();
        sort.sort(rows, visible, new M3RadixKey[] {new M3RadixKey() {
            @Override public int radixKeys(int[] r, int count, M3PermutationIndex lanes) {
                for (int i = 0; i < count; i++) lanes.keys[i] = 1_000 - tree.get(r[i]).index();   // index descending
                return 2;
            }
        }}, new boolean[] {false}, 1, (a, b) -> Integer.compare(tree.get(b).index(), tree.get(a).index()));
        M3RangeSet selection = new M3RangeSet();
        selection.setLogicalCount(visible);
        selection.selectRange(0, 3);
        AbstractTableModel model = new AbstractTableModel() {
            @Override public int getRowCount() { return visible; }
            @Override public int getColumnCount() { return 3; }
            @Override public Object getValueAt(int row, int column) {
                Node node = tree.get(rows[row]);
                return switch (column) { case 0 -> node.group(); case 1 -> node.index(); default -> selection.isSelected(row); };
            }
        };
        LOG.log (System.Logger.Level.INFO, String.valueOf ("tree rows=" + n + " visible after filter=" + visible + " first=" + model.getValueAt(0, 1)));
        if (args.length > 0 && args[0].equals("--check")) {
            if (visible >= n || (int) model.getValueAt(0, 1) % 7 == 0) throw new AssertionError("swing example");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("viewport from collections");
            frame.add(new JScrollPane(new JTable(model)));
            frame.setSize(480, 360);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}
