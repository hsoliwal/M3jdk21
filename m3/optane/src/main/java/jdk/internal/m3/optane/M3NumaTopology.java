// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.optane;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

/** OS NUMA CPU placement discovery. PMem namespace-to-NUMA mapping must be verified separately. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/56cbdc6312594f06e21ef56f0ce33b605383270b/synexia-openrewrite-recipes/recipe-crates/m3-pmem-16source-numa-convergence-20261031/src/main/resources/com/synexia/rewrite/pmem-16source-numa-convergence-20261031/NumaTopology.java.template blob 4972234a700a; recipe m3/optane/recipe/pmem-accelerator-20261107/relocate.py v4
public final class M3NumaTopology {
    public record Node(int id, List<Integer> allowedCpus) { }

    private M3NumaTopology() { }

    /**
     * Parse Linux CPU-list decimal ranges in one bounded, allocation-light pass.
     * Empty input is an empty set. Incomplete fields, malformed ranges and
     * indexes above 65535 refuse instead of silently assigning fewer CPUs.
     * Preserve the previous parser's optional leading + and ASCII token trim.
     */
    public static BitSet parseCpuList(String cpus) {
        BitSet out = new BitSet();
        if (cpus == null || cpus.isBlank()) return out;
        int n = cpus.length();
        int index = 0;
        while (index < n) {
            while (index < n && cpus.charAt(index) <= ' ') index++;
            if (index == n) throw new IllegalArgumentException("empty CPU-list element");
            int low = 0;
            if (cpus.charAt(index) == '+') index++;
            int digits = 0;
            while (index < n) {
                char c = cpus.charAt(index);
                if (c < '0' || c > '9') break;
                int digit = c - '0';
                if (low > (65535 - digit) / 10) throw new IllegalArgumentException("CPU index exceeds 65535");
                low = low * 10 + digit;
                index++;
                digits++;
            }
            if (digits == 0) throw new IllegalArgumentException("CPU index required");
            int high = low;
            if (index < n && cpus.charAt(index) == '-') {
                index++;
                if (index < n && cpus.charAt(index) == '+') index++;
                high = 0;
                digits = 0;
                while (index < n) {
                    char c = cpus.charAt(index);
                    if (c < '0' || c > '9') break;
                    int digit = c - '0';
                    if (high > (65535 - digit) / 10) throw new IllegalArgumentException("CPU index exceeds 65535");
                    high = high * 10 + digit;
                    index++;
                    digits++;
                }
                if (digits == 0 || high < low) throw new IllegalArgumentException("invalid CPU range");
            }
            while (index < n && cpus.charAt(index) <= ' ') index++;
            if (index < n && cpus.charAt(index) != ',') {
                throw new IllegalArgumentException("invalid CPU-list separator");
            }
            out.set(low, high + 1);
            if (index == n) return out;
            if (++index == n) throw new IllegalArgumentException("trailing CPU-list separator");
        }
        return out;
    }

}
