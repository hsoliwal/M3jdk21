/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.tooling.donors;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Random;
import java.util.TreeMap;

/** Exact Java21 lane growth/freeze boundary oracle; test-only reflection. */
public final class DonorTreeLanesGrowthTest {
    private DonorTreeLanesGrowthTest() { }

    public static void main(String[] args) throws Exception {
        long checks = 0;
        int[] boundaries = {0, 1, 2, 255, 256, 257, 511, 512, 513, 1023, 1024, 1025, 4095, 4096, 4097, 25000};
        for (int bound : boundaries) {
            DonorTreeLanes lanes = new DonorTreeLanes();
            TreeMap<String, String[]> expected = new TreeMap<>();
            int[] indices = new int[bound];
            for (int i = 0; i < bound; i++) indices[i] = i;
            Random r = new Random(20261102L + bound);
            for (int i = bound - 1; i > 0; i--) {
                int swap = r.nextInt(i + 1);
                int t = indices[i]; indices[i] = indices[swap]; indices[swap] = t;
            }
            for (int i : indices) {
                String path = String.format(Locale.ROOT, "src/pkg/%08d.java", i);
                String mode = i % 2 == 0 ? "100644" : "100755";
                String type = i % 7 == 0 ? "commit" : "blob";
                String object = String.format(Locale.ROOT, "%040x", i);
                lanes.add(path, mode, type, object);
                expected.put(path, new String[]{mode,type,object});
                int capacity = capacity(lanes);
                require(capacity >= expected.size(), "capacity");
                require(capacity == capacityOf(lanes,"modes")
                        && capacity == capacityOf(lanes,"types")
                        && capacity == capacityOf(lanes,"objects"), "lanes misaligned");
                checks += 2;
            }
            lanes.freeze();
            int i = 0;
            for (var e : expected.entrySet()) {
                require(e.getKey().equals(lanes.path(i)), "path mismatch");
                require(e.getValue()[0].equals(lanes.mode(i)), "mode mismatch");
                require(e.getValue()[1].equals(lanes.type(i)), "type mismatch");
                require(e.getValue()[2].equals(lanes.objectId(i)), "OID mismatch");
                checks += 4;
                i++;
            }
            require(lanes.size() == bound, "size");
            checks++;
            try {
                lanes.add("extra", "100644", "blob", "1");
                throw new AssertionError("frozen add accepted");
            } catch (IllegalStateException expectedFailure) { checks++; }
            try {
                lanes.freeze();
                throw new AssertionError("double freeze accepted");
            } catch (IllegalStateException expectedFailure) { checks++; }
        }
        DonorTreeLanes duplicates = new DonorTreeLanes();
        duplicates.add("x", "100644", "blob", "a");
        duplicates.add("x", "100755", "blob", "b");
        try {
            duplicates.freeze();
            throw new AssertionError("duplicate path accepted");
        } catch (IOException expectedFailure) { checks++; }
        System.out.printf(Locale.ROOT, "M3_ATOMIC_GROWTH_BOUNDARY_PASS\tchecks=%d\tboundaries=%d%n", checks, boundaries.length);
    }

    private static int capacity(DonorTreeLanes lanes) throws Exception {
        return capacityOf(lanes,"paths");
    }
    private static int capacityOf(DonorTreeLanes lanes, String fieldName) throws Exception {
        Field f = DonorTreeLanes.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return ((String[]) f.get(lanes)).length;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
