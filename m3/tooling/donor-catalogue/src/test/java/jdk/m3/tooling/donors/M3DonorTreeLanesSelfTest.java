/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.donors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/** Dependency-free independent TreeMap/TreeSet behavioral oracle for primitive lanes. */
public final class M3DonorTreeLanesSelfTest {
    private M3DonorTreeLanesSelfTest() { }

    public static void main(String[] args) throws Exception {
        long checks = 0;
        M3DonorTreeLanes lanes = new M3DonorTreeLanes();
        var expected = new TreeMap<String, String[]>();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < 25_000; ++i) order.add(i);
        Collections.shuffle(order, new Random(20261102));
        for (int number : order) {
            String path = "src/" + (number % 7 == 0 ? "\uD83D\uDE80/" : number % 11 == 0 ? "\uE100/" : "java/")
                    + String.format(java.util.Locale.ROOT, "%08d", number) + ".java";
            String mode = number % 3 == 0 ? "100755" : "100644";
            String type = "blob";
            String oid = String.format(java.util.Locale.ROOT, "%040x", number + 1L);
            lanes.add(path, mode, type, oid);
            expected.put(path, new String[]{mode, type, oid});
        }
        lanes.freeze();
        require(lanes.size() == expected.size(), "size");
        checks++;
        int i = 0;
        for (var entry : expected.entrySet()) {
            require(lanes.path(i).equals(entry.getKey()), "UTF-16 path order " + i);
            require(lanes.mode(i).equals(entry.getValue()[0]), "mode " + i);
            require(lanes.type(i).equals(entry.getValue()[1]), "type " + i);
            require(lanes.objectId(i).equals(entry.getValue()[2]), "oid " + i);
            checks += 4;
            i++;
        }
        M3DonorTreeLanes copy = new M3DonorTreeLanes();
        copy.add("alpha", "100644", "blob", "a");
        copy.add("beta", "100644", "blob", "b");
        copy.freeze();
        require(copy.same(0, copy, 0), "same metadata");
        require(!copy.same(0, copy, 1), "different metadata");
        checks += 2;
        M3DonorTreeLanes duplicate = new M3DonorTreeLanes();
        duplicate.add("duplicate", "100644", "blob", "a");
        duplicate.add("duplicate", "100755", "blob", "b");
        try {
            duplicate.freeze();
            throw new AssertionError("duplicate permitted");
        } catch (IOException expectedFailure) {
            checks++;
        }
        try {
            lanes.add("too late", "100644", "blob", "c");
            throw new AssertionError("mutation of frozen lanes permitted");
        } catch (IllegalStateException expectedFailure) {
            checks++;
        }
        System.out.println("M3_DONOR_LANES_PASS\tchecks=" + checks + "\trows=25000");
    }

    private static void require(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
}
