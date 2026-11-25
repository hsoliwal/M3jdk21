/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary BasicTableUI paints a wide, 1M-row JTable with work bounded by the visible cells:
 *          the column axis is resolved once per paint, not once per painted row, the pixels
 *          and model reads equal the per-row getCellRect path, only visible rows are read and
 *          no per-row object or renderer component is retained
 * @run main/othervm -Djava.awt.headless=true -Xmx512m TablePaintColumnWalkTest
 * @run main/othervm -Djava.awt.headless=true -Xmx512m -Dm3.swing.viewport=true TablePaintColumnWalkTest
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.CellRendererPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

public class TablePaintColumnWalkTest {
    private static final int ROWS = 1_000_000;
    private static final int COLUMNS = 4_000;
    private static final int VIEW_WIDTH = 800;
    private static final int VIEW_HEIGHT = 600;

    private static int checks;
    private static int failures;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(TablePaintColumnWalkTest::runAll);
        System.out.println("TABLE_PAINT_COLUMN_WALK checks=" + checks + " failures=" + failures);
        if (failures != 0) {
            throw new AssertionError("Failed checks: " + failures);
        }
    }

    private static void runAll() {
        run("pixels-and-reads-equal-per-row-path", TablePaintColumnWalkTest::differential);
        run("renderer-changes-geometry-mid-paint", TablePaintColumnWalkTest::geometryChangedByRenderer);
        run("column-work-bounded-per-paint", TablePaintColumnWalkTest::columnWork);
        run("only-visible-rows-read", TablePaintColumnWalkTest::visibleRowsOnly);
        run("no-per-row-residency", TablePaintColumnWalkTest::residency);
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
            System.out.println("PASS " + name);
        } catch (Throwable failure) {
            failures++;
            System.out.println("FAIL " + name + ": " + failure);
            failure.printStackTrace(System.out);
        }
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError(what);
        }
    }

    /** A model of ROWS x columns without per-row storage; it records which rows paint reads. */
    static final class CountingModel extends AbstractTableModel {
        private final int rows;
        private final int columns;
        long reads;
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        long readOrderHash = 1;

        CountingModel(int rows, int columns) {
            this.rows = rows;
            this.columns = columns;
        }

        void reset() {
            reads = 0;
            minRow = Integer.MAX_VALUE;
            maxRow = Integer.MIN_VALUE;
            readOrderHash = 1;
        }

        @Override
        public int getRowCount() {
            return rows;
        }

        @Override
        public int getColumnCount() {
            return columns;
        }

        @Override
        public Object getValueAt(int row, int column) {
            reads++;
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
            readOrderHash = readOrderHash * 1_000_003L + (long) row * columns + column;
            return (row % 97) * 31 + column % 89;
        }
    }

    /** Counts every column lookup, which is what a walk over the columns left of the view costs. */
    static final class CountingColumnModel extends DefaultTableColumnModel {
        long lookups;

        @Override
        public TableColumn getColumn(int columnIndex) {
            lookups++;
            return super.getColumn(columnIndex);
        }
    }

    /** Overriding getCellRect keeps BasicTableUI on the per-row getCellRect path (the reference). */
    static final class PerRowCellRectTable extends JTable {
        PerRowCellRectTable(CountingModel model, CountingColumnModel columns) {
            super(model, columns);
        }

        @Override
        public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
            return super.getCellRect(row, column, includeSpacing);
        }
    }

    private static JTable table(boolean perRowReference, int rows, int columns, CountingModel model,
                                CountingColumnModel columnModel) {
        JTable table = perRowReference ? new PerRowCellRectTable(model, columnModel) : new JTable(model, columnModel);
        table.setAutoCreateColumnsFromModel(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSize(table.getPreferredSize());
        // a dragged column paints its well in the parent's background
        new JPanel(null).add(table);
        return table;
    }

    private static BufferedImage paint(JTable table, Rectangle clip) {
        BufferedImage image = new BufferedImage(clip.width, clip.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.MAGENTA);
            g.fillRect(0, 0, clip.width, clip.height);
            g.translate(-clip.x, -clip.y);
            g.setClip(clip.x, clip.y, clip.width, clip.height);
            table.getUI().paint(g, table);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static Rectangle farRightMiddle(JTable table) {
        return new Rectangle(table.getWidth() - VIEW_WIDTH - 37, table.getHeight() / 2 + 5, VIEW_WIDTH, VIEW_HEIGHT);
    }

    private static void configure(JTable table, boolean rtl, int rowMargin, int columnMargin,
                                  boolean variableHeights, int firstRow) {
        table.setComponentOrientation(rtl ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT);
        table.setRowMargin(rowMargin);
        table.getColumnModel().setColumnMargin(columnMargin);
        table.setShowGrid(true);
        table.setGridColor(Color.BLUE);
        if (variableHeights) {
            for (int i = 0; i < 60; i += 3) {
                table.setRowHeight(firstRow + i, 9 + (i * 7) % 23);
            }
        }
        table.setSize(table.getPreferredSize());
    }

    /** Same pixels and the same getValueAt sequence as the per-row getCellRect reference path. */
    private static void differential() {
        int rows = 20_000;
        int columns = 600;
        for (boolean rtl : new boolean[] {false, true}) {
            for (int[] margins : new int[][] {{1, 1}, {0, 0}, {3, 5}, {7, 0}}) {
                for (boolean variable : new boolean[] {false, true}) {
                    for (boolean drag : new boolean[] {false, true}) {
                        CountingModel fastModel = new CountingModel(rows, columns);
                        CountingModel refModel = new CountingModel(rows, columns);
                        JTable fast = table(false, rows, columns, fastModel, new CountingColumnModel());
                        JTable ref = table(true, rows, columns, refModel, new CountingColumnModel());
                        int firstRow = 9_990;
                        configure(fast, rtl, margins[0], margins[1], variable, firstRow);
                        configure(ref, rtl, margins[0], margins[1], variable, firstRow);
                        Rectangle first = fast.getCellRect(firstRow, 0, true);
                        int x = rtl ? 4_000 : fast.getWidth() - VIEW_WIDTH - 4_000;
                        Rectangle clip = new Rectangle(x, first.y + 3, VIEW_WIDTH, VIEW_HEIGHT);
                        if (drag) {
                            int column = fast.columnAtPoint(new java.awt.Point(clip.x + 300, clip.y));
                            fast.getTableHeader().setDraggedColumn(fast.getColumnModel().getColumn(column));
                            fast.getTableHeader().setDraggedDistance(-23);
                            ref.getTableHeader().setDraggedColumn(ref.getColumnModel().getColumn(column));
                            ref.getTableHeader().setDraggedDistance(-23);
                        }
                        String config = "rtl=" + rtl + " margins=" + Arrays.toString(margins)
                                + " variable=" + variable + " drag=" + drag;
                        check(fast.getCellRect(firstRow + 7, 3, false).equals(ref.getCellRect(firstRow + 7, 3, false)),
                                "same geometry " + config);
                        fastModel.reset();
                        refModel.reset();
                        int[] a = pixels(paint(fast, clip));
                        int[] b = pixels(paint(ref, clip));
                        check(Arrays.equals(a, b), "identical pixels " + config);
                        check(fastModel.reads == refModel.reads && fastModel.reads > 0,
                                "same number of reads " + config + ": " + fastModel.reads + " vs " + refModel.reads);
                        check(fastModel.readOrderHash == refModel.readOrderHash, "same read order " + config);
                    }
                }
            }
        }
    }

    /** A renderer that changes the table once, when it is asked for one cell. */
    static final class ChangingRenderer extends DefaultTableCellRenderer {
        private final int row;
        private final int column;
        private final Consumer<JTable> change;
        private boolean changed;

        ChangingRenderer(int row, int column, Consumer<JTable> change) {
            this.row = row;
            this.column = column;
            this.change = change;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int column) {
            if (!changed && row == this.row && column == this.column) {
                changed = true;
                change.accept(table);
            }
            return super.getTableCellRendererComponent(table, value, selected, focus, row, column);
        }
    }

    /**
     * A renderer that changes the column geometry or the orientation while a paint runs: every
     * later row is drawn where the per-row getCellRect path draws it, left to right and right to left.
     */
    private static void geometryChangedByRenderer() {
        int rows = 400;
        int columns = 40;
        String[] names = {"margin", "own column wider", "column before the view wider",
            "column moved across the view", "orientation flipped", "two cancelling widths"};
        for (boolean rtl : new boolean[] {false, true}) {
            for (boolean drag : new boolean[] {false, true}) {
                for (int kind = 0; kind < names.length; kind++) {
                    int[] pixels = null;
                    long readHash = 0;
                    for (boolean perRowReference : new boolean[] {false, true}) {
                        CountingModel model = new CountingModel(rows, columns);
                        JTable table = table(perRowReference, rows, columns, model, new CountingColumnModel());
                        configure(table, rtl, 1, 1, false, 0);
                        Rectangle clip = new Rectangle(rtl ? 900 : 1_100, 16 * 100 + 3, 700, 400);
                        int firstColumn = table.columnAtPoint(new java.awt.Point(clip.x + (rtl ? clip.width - 1 : 0), clip.y));
                        int changeRow = 100 + 6;
                        // visible columns are firstColumn.. in both orientations; the origin of
                        // firstColumn sums the columns before it (LTR) or after it (RTL)
                        int changeColumn = firstColumn + 2;
                        int before = rtl ? Math.min(columns - 1, firstColumn + 12) : Math.max(0, firstColumn - 3);
                        int after = rtl ? Math.max(0, firstColumn - 5) : Math.min(columns - 1, firstColumn + 5);
                        int k = kind;
                        Consumer<JTable> change = t -> {
                            switch (k) {
                                case 0 -> t.getColumnModel().setColumnMargin(6);
                                case 1 -> t.getColumnModel().getColumn(changeColumn).setMinWidth(140);
                                case 2 -> t.getColumnModel().getColumn(before).setWidth(75 + 41);
                                case 3 -> t.getColumnModel().moveColumn(before, after);
                                case 4 -> t.setComponentOrientation(rtl ? ComponentOrientation.LEFT_TO_RIGHT
                                        : ComponentOrientation.RIGHT_TO_LEFT);
                                default -> {
                                    t.getColumnModel().getColumn(before).setWidth(75 + 30);
                                    t.getColumnModel().getColumn(after).setWidth(75 - 30);
                                }
                            }
                        };
                        table.setDefaultRenderer(Object.class, new ChangingRenderer(changeRow, changeColumn, change));
                        if (drag) {
                            int dragged = firstColumn + 4;
                            table.getTableHeader().setDraggedColumn(table.getColumnModel().getColumn(dragged));
                            table.getTableHeader().setDraggedDistance(-17);
                        }
                        model.reset();
                        int[] painted = pixels(paint(table, clip));
                        if (pixels == null) {
                            pixels = painted;
                            readHash = model.readOrderHash;
                        } else {
                            String config = names[kind] + " rtl=" + rtl + " drag=" + drag;
                            if (kind == names.length - 1) {
                                // Cancelling widths keep every value the axis is checked against; the
                                // owner path is documented not to see them, so only report the result.
                                System.out.println("cancelling widths rtl=" + rtl + " drag=" + drag
                                        + " same pixels=" + Arrays.equals(pixels, painted));
                            } else {
                                check(Arrays.equals(pixels, painted), "identical pixels after " + config);
                                check(readHash == model.readOrderHash, "same read order after " + config);
                            }
                        }
                    }
                }
            }
        }
    }

    /** One paint resolves the column axis a bounded number of times, not once per painted row. */
    private static void columnWork() {
        for (boolean rtl : new boolean[] {false, true}) {
            CountingModel fastModel = new CountingModel(ROWS, COLUMNS);
            CountingColumnModel fastColumns = new CountingColumnModel();
            JTable fast = table(false, ROWS, COLUMNS, fastModel, fastColumns);
            CountingModel refModel = new CountingModel(ROWS, COLUMNS);
            CountingColumnModel refColumns = new CountingColumnModel();
            JTable ref = table(true, ROWS, COLUMNS, refModel, refColumns);
            configure(fast, rtl, 1, 1, false, 0);
            configure(ref, rtl, 1, 1, false, 0);
            // the far right is the far end of the column walk in both orientations:
            // the last columns left to right, the first columns right to left
            Rectangle clip = farRightMiddle(fast);
            fastColumns.lookups = 0;
            refColumns.lookups = 0;
            paint(fast, clip);
            paint(ref, clip);
            int visibleRows = VIEW_HEIGHT / fast.getRowHeight() + 2;
            int visibleColumns = VIEW_WIDTH / 75 + 2;
            long bound = 6L * COLUMNS + 8L * visibleRows * visibleColumns;
            System.out.println("column lookups per paint rtl=" + rtl + ": owner path=" + fastColumns.lookups
                    + " per-row reference=" + refColumns.lookups + " bound=" + bound);
            check(fastColumns.lookups <= bound, "column lookups " + fastColumns.lookups + " exceed " + bound);
            check(refColumns.lookups > (long) (visibleRows - 2) * (COLUMNS - visibleColumns),
                    "reference walks the columns per row: " + refColumns.lookups);
        }
    }

    /** Paint reads exactly the cells of the rows and columns that intersect the clip. */
    private static void visibleRowsOnly() {
        CountingModel model = new CountingModel(ROWS, COLUMNS);
        JTable table = table(false, ROWS, COLUMNS, model, new CountingColumnModel());
        for (Rectangle clip : new Rectangle[] {new Rectangle(0, 0, VIEW_WIDTH, VIEW_HEIGHT), farRightMiddle(table),
                new Rectangle(table.getWidth() - VIEW_WIDTH, table.getHeight() - VIEW_HEIGHT, VIEW_WIDTH, VIEW_HEIGHT)}) {
            model.reset();
            paint(table, clip);
            int firstRow = table.rowAtPoint(clip.getLocation());
            int lastRow = table.rowAtPoint(new java.awt.Point(clip.x, clip.y + clip.height - 1));
            int firstColumn = table.columnAtPoint(clip.getLocation());
            int lastColumn = table.columnAtPoint(new java.awt.Point(clip.x + clip.width - 1, clip.y));
            long expected = (long) (lastRow - firstRow + 1) * (lastColumn - firstColumn + 1);
            check(model.minRow == firstRow && model.maxRow == lastRow,
                    "rows read " + model.minRow + ".." + model.maxRow + " visible " + firstRow + ".." + lastRow);
            check(model.reads == expected, "reads " + model.reads + " expected " + expected);
        }
    }

    private static long usedHeapAfterGc() {
        for (int i = 0; i < 4; i++) {
            System.gc();
        }
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    /** No per-row object and no renderer component outlive a paint of a 1M-row table. */
    private static void residency() {
        CountingModel small = new CountingModel(10, 6);
        JTable warm = table(false, 10, 6, small, new CountingColumnModel());
        paint(warm, new Rectangle(0, 0, 200, 100));
        long before = usedHeapAfterGc();
        CountingModel model = new CountingModel(ROWS, 6);
        JTable table = table(false, ROWS, 6, model, new CountingColumnModel());
        for (int y : new int[] {0, table.getHeight() / 2, table.getHeight() - VIEW_HEIGHT}) {
            paint(table, new Rectangle(0, y, 450, VIEW_HEIGHT));
        }
        long retained = usedHeapAfterGc() - before;
        System.out.println("retained by a painted " + ROWS + "-row table: " + retained + " bytes");
        check(retained < 4L * 1024 * 1024, "retained " + retained + " bytes for " + ROWS + " rows");
        CellRendererPane pane = null;
        for (Component c : table.getComponents()) {
            if (c instanceof CellRendererPane p) {
                pane = p;
            }
        }
        check(pane != null, "renderer pane installed");
        check(((Container) pane).getComponentCount() == 0,
                "renderer components left in the pane: " + pane.getComponentCount());
        check(table.getRowCount() == ROWS && model.reads > 0, "table alive " + table.getRowCount());
    }
}
