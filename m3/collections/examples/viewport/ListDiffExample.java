/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3ListDiff;
import com.m3.collections.M3Patch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Snapshot A to snapshot B as remove/insert runs and moves; the patch is applied to a plain list. */
public final class ListDiffExample {
    public static void main(String[] args) {
        long[] a = {1, 2, 3, 4, 5, 6};
        long[] b = {1, 9, 3, 2, 6, 7, 8};
        M3Patch patch = M3ListDiff.diff(a, b);
        List<Long> list = new ArrayList<>(Arrays.stream(a).boxed().toList());
        patch.applyTo(new M3Patch.Target() {
            @Override public void remove(int index, int count) {
                System.out.println("remove " + count + " at " + index);
                list.subList(index, index + count).clear();
            }
            @Override public void insert(int index, int count) {
                System.out.println("insert " + count + " at " + index);
                for (int i = 0; i < count; i++) list.add(index + i, b[index + i]);
            }
        });
        System.out.println("kept=" + patch.keptCount() + " moves=" + patch.moveCount() + " result=" + list);
        if (!list.equals(Arrays.stream(b).boxed().toList())) throw new AssertionError("diff example");
    }
}
