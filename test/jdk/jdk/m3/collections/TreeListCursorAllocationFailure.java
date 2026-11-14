/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import java.lang.reflect.Field;
import java.util.ListIterator;
import jdk.m3.collections.apache.M3TreeList;

/** Injects path-growth failures into disposable, explicitly instrumented source only. */
public final class TreeListCursorAllocationFailure {
    private static int checks;

    private TreeListCursorAllocationFailure() {
    }

    private static Field field(String name) throws Exception {
        Field field = M3TreeList.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static M3TreeList<Integer> skewed(boolean leftSkew, int size) throws Exception {
        M3TreeList<Integer> list = new M3TreeList<>();
        Object[] elements = new Object[size];
        int[] left = new int[size];
        int[] right = new int[size];
        int[] weight = new int[size];
        int[] priority = new int[size];
        long[] mask = new long[(size + 63) >>> 6];
        for (int i = 0; i < size; i++) {
            elements[i] = i;
            left[i] = leftSkew && i != 0 ? i - 1 : -1;
            right[i] = !leftSkew && i + 1 != size ? i + 1 : -1;
            weight[i] = leftSkew ? i + 1 : size - i;
            priority[i] = leftSkew ? i : size - i;
            mask[i >>> 6] |= 1L << (i & 63);
        }
        field("elements").set(list, elements);
        field("left").set(list, left);
        field("right").set(list, right);
        field("weight").set(list, weight);
        field("priority").set(list, priority);
        field("markMask").set(list, mask);
        field("root").setInt(list, leftSkew ? size - 1 : 0);
        field("size").setInt(list, size);
        field("used").setInt(list, size);
        return list;
    }

    public static void main(String[] args) throws Exception {
        Field failure = field("cursorGrowthFailureForTest");
        int injected = 0;
        for (boolean leftSkew : new boolean[] {false, true}) {
            for (boolean backwards : new boolean[] {false, true}) {
                for (int failAt = 1; failAt <= 15; failAt++) {
                    int size = 4096;
                    M3TreeList<Integer> list = skewed(leftSkew, size);
                    ListIterator<Integer> cursor = list.listIterator(backwards ? size : 0);
                    failure.setInt(null, failAt);
                    for (int step = 0; step < size; step++) {
                        int before = cursor.nextIndex();
                        int expected = backwards ? size - step - 1 : step;
                        Integer actual;
                        try {
                            actual = backwards ? cursor.previous() : cursor.next();
                        } catch (OutOfMemoryError error) {
                            if (!"injected cursor growth".equals(error.getMessage())
                                    || cursor.nextIndex() != before || list.size() != size) {
                                throw new AssertionError("Failed allocation advanced logical state", error);
                            }
                            injected++;
                            failure.setInt(null, 0);
                            actual = backwards ? cursor.previous() : cursor.next();
                        }
                        if (actual != expected) {
                            throw new AssertionError("Retry returned the wrong logical element");
                        }
                        checks++;
                    }
                    for (int step = 0; step < size; step++) {
                        int expected = backwards ? step : size - step - 1;
                        int actual = backwards ? cursor.next() : cursor.previous();
                        if (actual != expected) {
                            throw new AssertionError("Failed growth corrupted reverse traversal");
                        }
                        checks++;
                    }
                }
            }
        }
        if (injected != 60) {
            throw new AssertionError("Expected all 60 allocation failure positions: " + injected);
        }
        System.out.println("PASS injected=" + injected + " checks=" + checks);
    }
}
