// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0

/**
 * Native-gate probe of recipe planner-native-20261107, run with the source launcher against the module classes:
 * prints whether M3NativePrecomputeOps loaded the pair kernel (available) and the tuple kernel (tuplesAvailable)
 * from java.library.path, so the gate can show which path each dump took.
 */
//FLLDCIM: TOP - gate probe of recipe m3/ports/precompute/recipe/planner-native-20261107
public final class NativeProbe {
    private NativeProbe() { }

    public static void main(String[] args) {
        System.out.println("NATIVE_PROBE available=" + jdk.m3.precompute.M3NativePrecomputeOps.available()
                + " tuples=" + jdk.m3.precompute.M3NativePrecomputeOps.tuplesAvailable());
    }
}
