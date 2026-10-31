/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.swing.viewport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.ComponentOrientation;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Random;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import org.junit.jupiter.api.Test;

/** Actual JTable/JViewport layout and pixel hit-testing; no native-peer claims. */
final class M3DesktopViewportColumnsTest {
    private static final class Table extends JTable {
        private static final long serialVersionUID = 1L;
        boolean showing = true;
        Table(int count) { super(new DefaultTableModel(3, count)); }
        @Override public boolean isShowing() { return showing; }
    }

    private record Scene(Table table, JViewport viewport) { }

    private static Scene scene(int... widths) {
        Table table = new Table(widths.length);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        long total = 0;
        for (int i = 0; i < widths.length; ++i) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setMinWidth(0);
            column.setMaxWidth(Integer.MAX_VALUE);
            column.setWidth(widths[i]);
            total += widths[i];
        }
        JViewport viewport = new JViewport();
        viewport.setSize(100, 60);
        viewport.setView(table);
        table.setSize((int)Math.min(Integer.MAX_VALUE, Math.max(300, total)), 120);
        return new Scene(table, viewport);
    }

    private static int[] columns(JTable table, int overscan) {
        int[] window = {-9, -9, -9, -9};
        M3DesktopViewportAdapters.fromTableColumns(table, overscan, window, 0);
        return window;
    }

    private static void pixelOracle(Scene scene) {
        Rectangle clip = scene.table().getVisibleRect();
        int first = Integer.MAX_VALUE;
        int end = 0;
        for (int x = clip.x; x < clip.x + clip.width; ++x) {
            int hit = scene.table().columnAtPoint(new Point(x, clip.y));
            if (hit >= 0) {
                first = Math.min(first, hit);
                end = Math.max(end, hit + 1);
            }
        }
        if (first == Integer.MAX_VALUE) first = 0;
        assertArrayEquals(new int[]{first, end, first, end}, columns(scene.table(), 0));
    }

    @Test void partialAndExactEdgesMatchPixelHitTesting() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(40, 60, 90, 110);
            for (int width : new int[]{1, 40, 60, 100, 170}) {
                scene.viewport().setSize(width, 60);
                for (int x : new int[]{0, 1, 39, 40, 41, 99, 100, 101, 189, 190, 299}) {
                    scene.viewport().setViewPosition(new Point(x, 0));
                    pixelOracle(scene);
                }
            }
        });
    }

    @Test void rtlCoordinatesKeepAscendingViewOrder() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(40, 60, 90, 110);
            scene.table().setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            for (int x : new int[]{0, 1, 39, 40, 99, 100, 190, 200, 260, 299}) {
                scene.viewport().setViewPosition(new Point(x, 0));
                pixelOracle(scene);
            }
            scene.viewport().setSize(40, 60);
            scene.viewport().setViewPosition(new Point(260, 0));
            assertArrayEquals(new int[]{0, 1, 0, 2}, columns(scene.table(), 1));
        });
    }

    @Test void zeroWidthAndRemovedColumnsDoNotEstablishViewportEdges() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(0, 40, 0, 60, 0);
            scene.viewport().setSize(40, 60);
            assertArrayEquals(new int[]{1, 2, 1, 2}, columns(scene.table(), 0));
            scene.viewport().setSize(100, 60);
            assertArrayEquals(new int[]{1, 4, 1, 4}, columns(scene.table(), 0));
            TableColumn hidden = scene.table().getColumnModel().getColumn(1);
            scene.table().removeColumn(hidden);
            assertArrayEquals(new int[]{2, 3, 2, 3}, columns(scene.table(), 0));
            scene.table().removeColumn(scene.table().getColumnModel().getColumn(2));
            assertArrayEquals(new int[4], columns(scene.table(), 5));
        });
    }

    @Test void overscanAndOffsetsKeepCallerCanaries() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(50, 50, 50, 50, 50, 50);
            scene.viewport().setSize(50, 60);
            scene.viewport().setViewPosition(new Point(100, 0));
            int[] output = {-7, -7, -7, -7, -7, -7};
            M3DesktopViewportAdapters.fromTableColumns(scene.table(), 1, output, 1);
            assertArrayEquals(new int[]{-7, 2, 3, 1, 4, -7}, output);
            assertArrayEquals(new int[]{2, 3, 0, 6}, columns(scene.table(), Integer.MAX_VALUE));
        });
    }

    @Test void invisibleEmptyAndBlankViewportsProduceNoRequests() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(40, 60);
            scene.table().showing = false;
            assertArrayEquals(new int[4], columns(scene.table(), 10));
            scene.table().showing = true;
            scene.viewport().setSize(0, 60);
            assertArrayEquals(new int[4], columns(scene.table(), 10));
            scene.viewport().setSize(100, 0);
            assertArrayEquals(new int[4], columns(scene.table(), 10));
            scene.viewport().setSize(100, 60);
            scene.viewport().setViewPosition(new Point(100, 0));
            assertArrayEquals(new int[4], columns(scene.table(), 10));
            assertArrayEquals(new int[4], columns(scene().table(), 10));
        });
    }

    @Test void emptyRowsStillExposeColumnHeaderGeometry() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(40, 60, 90);
            ((DefaultTableModel)scene.table().getModel()).setRowCount(0);
            assertArrayEquals(new int[]{0, 2, 0, 2}, columns(scene.table(), 0));
        });
    }

    @Test void widthAccumulationDoesNotWrapAtSignedIntLimit() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(1_500_000_000, 1_500_000_000, 50);
            scene.viewport().setViewPosition(new Point(2_000_000_000, 0));
            assertArrayEquals(new int[]{1, 2, 1, 2}, columns(scene.table(), 0));
            scene.table().setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            assertArrayEquals(new int[]{0, 1, 0, 1}, columns(scene.table(), 0));
        });
    }

    @Test void columnReorderingAndHidingPreserveModelIdentity() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(50, 50, 50, 50);
            scene.table().moveColumn(3, 0);
            TableColumn hidden = scene.table().getColumnModel().getColumn(2);
            scene.table().removeColumn(hidden);
            int[] window = {0, 3, 0, 3};
            int[] output = {-9, -9, -9, -9, -9};
            assertEquals(3, M3DesktopViewportAdapters.copyTableModelColumns(
                    scene.table(), window, 0, output, 1));
            assertArrayEquals(new int[]{-9, 3, 0, 2, -9}, output);
            scene.table().addColumn(hidden);
            window[1] = 4;
            assertEquals(4, M3DesktopViewportAdapters.copyTableModelColumns(
                    scene.table(), window, 0, output, 0));
            assertArrayEquals(new int[]{3, 0, 2, 1, -9}, output);
        });
    }

    @Test void mappingSupportsWindowOffsetAndAliasedDestination() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(50, 50, 50, 50);
            scene.table().moveColumn(3, 0);
            int[] output = {-9, 1, 4, 0, 4, -9};
            assertEquals(3, M3DesktopViewportAdapters.copyTableModelColumns(
                    scene.table(), output, 1, output, 2));
            assertArrayEquals(new int[]{-9, 1, 0, 1, 2, -9}, output);
        });
    }

    @Test void staleAndInvalidWindowsRefuseBeforeOutputMutation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(50, 50, 50);
            int[] output = {-9, -9, -9, -9};
            for (int[] window : new int[][]{{-1, 2, 0, 3}, {2, 1, 0, 3},
                    {0, 4, 0, 4}, {0, Integer.MAX_VALUE, 0, 3}}) {
                assertThrows(IndexOutOfBoundsException.class,
                        () -> M3DesktopViewportAdapters.copyTableModelColumns(
                                scene.table(), window, 0, output, 0));
                assertArrayEquals(new int[]{-9, -9, -9, -9}, output);
            }
            int[] window = {0, 3, 0, 3};
            for (int offset : new int[]{-1, 2, Integer.MAX_VALUE}) {
                assertThrows(IndexOutOfBoundsException.class,
                        () -> M3DesktopViewportAdapters.copyTableModelColumns(
                                scene.table(), window, 0, output, offset));
                assertArrayEquals(new int[]{-9, -9, -9, -9}, output);
            }
            scene.table().removeColumn(scene.table().getColumnModel().getColumn(0));
            assertThrows(IndexOutOfBoundsException.class,
                    () -> M3DesktopViewportAdapters.copyTableModelColumns(
                            scene.table(), window, 0, output, 0));
            assertArrayEquals(new int[]{-9, -9, -9, -9}, output);
        });
    }

    @Test void argumentAndThreadContractsRemainExplicit() throws Exception {
        assertThrows(IllegalStateException.class,
                () -> M3DesktopViewportAdapters.fromTableColumns(null, 0, new int[4], 0));
        assertThrows(IllegalStateException.class,
                () -> M3DesktopViewportAdapters.copyTableModelColumns(null, null, 0, null, 0));
        SwingUtilities.invokeAndWait(() -> {
            Scene scene = scene(50, 50);
            int[] output = {-9, -9, -9, -9};
            assertThrows(IllegalArgumentException.class,
                    () -> M3DesktopViewportAdapters.fromTableColumns(scene.table(), -1, output, 0));
            assertThrows(IndexOutOfBoundsException.class,
                    () -> M3DesktopViewportAdapters.fromTableColumns(scene.table(), 0, output, 1));
            assertThrows(NullPointerException.class,
                    () -> M3DesktopViewportAdapters.fromTableColumns(null, 0, output, 0));
            assertThrows(NullPointerException.class,
                    () -> M3DesktopViewportAdapters.copyTableModelColumns(scene.table(), null, 0, output, 0));
            assertThrows(IndexOutOfBoundsException.class,
                    () -> M3DesktopViewportAdapters.copyTableModelColumns(scene.table(), output, 1, output, 0));
            assertArrayEquals(new int[]{-9, -9, -9, -9}, output);
            assertEquals(0, M3DesktopViewportAdapters.copyTableModelColumns(
                    scene.table(), new int[]{2, 2, 2, 2}, 0, output, output.length));
        });
    }

    @Test void randomizedLayoutsMatchEveryVisiblePixel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Random random = new Random(0x4d33434f4c554d4eL);
            for (int trial = 0; trial < 128; ++trial) {
                int[] widths = new int[1 + random.nextInt(12)];
                Arrays.setAll(widths, ignored -> random.nextInt(80));
                Scene scene = scene(widths);
                scene.table().setComponentOrientation(trial % 2 == 0
                        ? ComponentOrientation.LEFT_TO_RIGHT : ComponentOrientation.RIGHT_TO_LEFT);
                scene.table().moveColumn(random.nextInt(widths.length), random.nextInt(widths.length));
                scene.viewport().setSize(1 + random.nextInt(130), 60);
                scene.viewport().setViewPosition(new Point(random.nextInt(300), 0));
                pixelOracle(scene);
            }
        });
    }
}
