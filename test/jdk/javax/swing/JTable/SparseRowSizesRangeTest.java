/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Sparse JTable row-size edits reject overflowing ranges before changing state
 * @modules java.desktop/sun.swing.table
 * @run main/othervm -Djava.awt.headless=true SparseRowSizesRangeTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import sun.swing.table.M3SparseSizes;

public class SparseRowSizesRangeTest {
    private static int checks;
    private static int failures;

    public static void main(String[] args) {
        run("sparse-removal-overflow", SparseRowSizesRangeTest::sparseRemovalOverflow);
        run("dense-removal-overflow", SparseRowSizesRangeTest::denseRemovalOverflow);
        run("sparse-insertion-overflow", SparseRowSizesRangeTest::sparseInsertionOverflow);
        run("maximum-length-insertion", SparseRowSizesRangeTest::maximumLengthInsertion);
        run("ordinary-invalid-and-empty-ranges", SparseRowSizesRangeTest::ordinaryRanges);
        run("maximum-valid-sparse-geometry", SparseRowSizesRangeTest::maximumValidGeometry);
        run("independent-array-model", SparseRowSizesRangeTest::arrayModel);
        System.out.println("SPARSE_ROW_SIZES_RANGE checks=" + checks + " failures=" + failures);
        if (failures != 0) throw new AssertionError("Failed cases: " + failures);
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
            System.out.println("PASS " + name);
        } catch (Throwable failure) {
            failures++;
            System.out.println("FAIL " + name + ": " + failure);
        }
    }

    private static void sparseRemovalOverflow() {
        int[][] ranges = {{Integer.MAX_VALUE, Integer.MAX_VALUE}, {1, Integer.MAX_VALUE},
                {Integer.MAX_VALUE, 1}, {10, Integer.MAX_VALUE}};
        for (int[] range : ranges) {
            M3SparseSizes sizes = new M3SparseSizes(10, 7);
            sizes.setSize(2, 13);
            sizes.setSize(8, 3);
            unchangedAfterRejection(sizes, () -> sizes.removeEntries(range[0], range[1]),
                    "remove " + range[1] + " at " + range[0] + " of 10");
        }
    }

    private static void denseRemovalOverflow() {
        M3SparseSizes sizes = new M3SparseSizes(M3SparseSizes.LIMIT + 8, 7);
        for (int i = 0; i <= M3SparseSizes.LIMIT; i++) sizes.setSize(i, 9);
        check(sizes.count() == -1, "dense setup");
        unchangedAfterRejection(sizes,
                () -> sizes.removeEntries(Integer.MAX_VALUE, Integer.MAX_VALUE),
                "remove " + Integer.MAX_VALUE + " at " + Integer.MAX_VALUE + " of " + sizes.length());
    }

    private static void sparseInsertionOverflow() {
        for (int value : new int[] {7, 11}) {
            M3SparseSizes sizes = new M3SparseSizes(10, 7);
            sizes.setSize(8, 13);
            unchangedAfterRejection(sizes, () -> sizes.insertEntries(2, Integer.MAX_VALUE, value),
                    "insert " + Integer.MAX_VALUE + " at 2 of 10");
        }
    }

    private static void maximumLengthInsertion() {
        M3SparseSizes sizes = new M3SparseSizes(Integer.MAX_VALUE, 1);
        sizes.setSize(Integer.MAX_VALUE - 2, 2);
        try {
            sizes.insertEntries(Integer.MAX_VALUE, 1, 1);
            throw new AssertionError("overflowing logical length accepted");
        } catch (IndexOutOfBoundsException expected) {
            check(expected.getMessage().equals("insert 1 at " + Integer.MAX_VALUE + " of "
                    + Integer.MAX_VALUE), "insert exception message");
        }
        check(sizes.length() == Integer.MAX_VALUE, "maximum length retained");
        check(sizes.count() == 1, "maximum sparse count retained");
        check(sizes.getSize(Integer.MAX_VALUE - 2) == 2, "maximum custom size retained");
        check(sizes.getSize(Integer.MAX_VALUE - 1) == 1, "maximum tail retained");
    }

    private static void ordinaryRanges() {
        M3SparseSizes sizes = new M3SparseSizes(5, 2);
        int[][] removals = {{-1, 1}, {0, -1}, {6, 0}, {4, 2}};
        for (int[] range : removals) {
            unchangedAfterRejection(sizes, () -> sizes.removeEntries(range[0], range[1]),
                    "remove " + range[1] + " at " + range[0] + " of 5");
        }
        int[][] inserts = {{-1, 1}, {0, -1}, {6, 0}};
        for (int[] range : inserts) {
            unchangedAfterRejection(sizes, () -> sizes.insertEntries(range[0], range[1], 2),
                    "insert " + range[1] + " at " + range[0] + " of 5");
        }
        int[] before = sizes.getSizes();
        sizes.insertEntries(5, 0, 9);
        sizes.removeEntries(5, 0);
        check(Arrays.equals(before, sizes.getSizes()), "zero-length tail edits");
        sizes.removeEntries(0, 5);
        sizes.insertEntries(0, 0, 3);
        sizes.removeEntries(0, 0);
        check(sizes.length() == 0 && sizes.getPosition(0) == 0, "empty edits");
    }

    private static void maximumValidGeometry() {
        M3SparseSizes sizes = new M3SparseSizes(Integer.MAX_VALUE - 2, 1);
        sizes.setSize(Integer.MAX_VALUE - 4, 2);
        sizes.insertEntries(Integer.MAX_VALUE - 5, 2, 1);
        check(sizes.length() == Integer.MAX_VALUE, "exact maximum insertion");
        check(sizes.count() == 1, "maximum geometry stays sparse");
        check(sizes.getSize(Integer.MAX_VALUE - 2) == 2, "shift near maximum");
        check(sizes.getSize(Integer.MAX_VALUE - 3) == 1, "shift predecessor");
        sizes.removeEntries(Integer.MAX_VALUE - 3, 3);
        check(sizes.length() == Integer.MAX_VALUE - 3, "maximum tail removal");
        check(sizes.count() == 0, "maximum custom entry removed");
        check(sizes.getPosition(Integer.MAX_VALUE - 3) == Integer.MAX_VALUE - 3,
                "maximum total without dense allocation");
    }

    private static void unchangedAfterRejection(M3SparseSizes sizes, Runnable action, String message) {
        int[] before = sizes.getSizes();
        int count = sizes.count();
        int defaultSize = sizes.defaultSize();
        int total = sizes.getPosition(sizes.length());
        try {
            action.run();
            throw new AssertionError("invalid range accepted: " + message);
        } catch (IndexOutOfBoundsException expected) {
            check(message.equals(expected.getMessage()), "exception message: " + message);
        }
        check(sizes.length() == before.length, "length after rejected range");
        check(sizes.count() == count, "representation after rejected range");
        check(sizes.defaultSize() == defaultSize, "default after rejected range");
        check(Arrays.equals(before, sizes.getSizes()), "contents after rejected range");
        check(sizes.getPosition(sizes.length()) == total, "geometry after rejected range");
    }

    private static void arrayModel() {
        Random random = new Random(0x5357494e47L);
        for (int trial = 0; trial < 12; trial++) {
            int defaultSize = 1 + random.nextInt(12);
            M3SparseSizes sizes = new M3SparseSizes(0, defaultSize);
            List<Integer> model = new ArrayList<>();
            for (int step = 0; step < 300; step++) {
                int operation = random.nextInt(3);
                if (model.isEmpty() || operation == 0 && model.size() < 90) {
                    int at = random.nextInt(model.size() + 1);
                    int n = random.nextInt(5);
                    int value = random.nextBoolean() ? defaultSize : random.nextInt(20);
                    sizes.insertEntries(at, n, value);
                    for (int i = 0; i < n; i++) model.add(at, value);
                } else if (operation == 1) {
                    int at = random.nextInt(model.size() + 1);
                    int n = random.nextInt(model.size() - at + 1);
                    sizes.removeEntries(at, n);
                    model.subList(at, at + n).clear();
                } else {
                    int at = random.nextInt(model.size());
                    int value = random.nextInt(20);
                    sizes.setSize(at, value);
                    model.set(at, value);
                }
                check(sizes.length() == model.size(), "array-model length");
                int total = 0;
                for (int i = 0; i < model.size(); i++) {
                    check(sizes.getSize(i) == model.get(i), "array-model height");
                    check(sizes.getPosition(i) == total, "array-model offset");
                    total += model.get(i);
                }
                check(sizes.getPosition(model.size()) == total, "array-model total");
                for (int p = -1; p <= total + 1; p++) {
                    int expected = 0;
                    int remaining = p;
                    while (expected < model.size() && remaining >= model.get(expected)) {
                        remaining -= model.get(expected++);
                    }
                    check(sizes.getIndex(p) == expected, "array-model position " + p);
                }
            }
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
