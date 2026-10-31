/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @bug 8299722
 * @summary DefaultRowSorter.getComparator(int) must return null, not throw
 *          ArrayIndexOutOfBoundsException, for a model column added after a comparator was set.
 * @run main/othervm -Djava.awt.headless=true M3RowSorterGetComparatorTest
 */

import java.util.Comparator;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * JDK-8299722. {@code setComparator} sizes the comparators array to the model's column count of
 * that moment; the sorter is not a model listener, so growing the model leaves the array stale.
 * {@code getComparator} bound-checks against the model (its documented contract: an
 * IndexOutOfBoundsException only for a column outside the range of the underlying model) and then
 * reads the stale array: for a new, valid column it threw ArrayIndexOutOfBoundsException instead
 * of returning null.
 */
public final class M3RowSorterGetComparatorTest {
    public static void main(String[] args) {
        DefaultTableModel model = new DefaultTableModel(1, 2);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(1, Comparator.comparing(String::valueOf));
        model.setColumnCount(4);
        if (sorter.getComparator(1) == null) {
            throw new RuntimeException("comparator of column 1 lost");
        }
        // a valid model column must not throw; TableRowSorter then falls back to the
        // column-class default, so only the absence of the exception is asserted
        sorter.getComparator(3);
        boolean thrown = false;
        try {
            sorter.getComparator(4);
        } catch (IndexOutOfBoundsException e) {
            thrown = true;
        }
        if (!thrown) {
            throw new RuntimeException("column 4 is outside the model and must throw");
        }
        System.out.println("passed");
    }
}
