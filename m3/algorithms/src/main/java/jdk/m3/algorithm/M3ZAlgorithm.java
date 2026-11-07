// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.m3.algorithm;

import jdk.m3.algorithm.M3ProgressChecks;
import jdk.m3.algorithm.M3ProgressMonitor;

/**
 * Strings: Z-Algorithm.
 *
 * @since 1.0.0
 */
//FLLDCIM: RELOCATE - donor blob 16dfc2abb3f8 at commit 78cf2ad4c7e5 (repository, path, URL and digests: m3/algorithms/recipe/catalogue-kernels-20261107/PROVENANCE.tsv); recipe m3/algorithms/recipe/catalogue-kernels-20261107/relocate.py v1
public final class M3ZAlgorithm {

    private M3ZAlgorithm() {}

    // --- Z-ALGORITHM ---

    /**
     * Z-array: z[i] = length of longest substring starting from i that matches
     * prefix.
     */
    public static int[] zArray(String s) {
        int n = s.length();
        int[] z = new int[n];
        int l = 0, r = 0;

        for (int i = 1; i < n; i++) {
            if (i < r) {
                z[i] = Math.min(r - i, z[i - l]);
            }
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) {
                z[i]++;
            }
            if (i + z[i] > r) {
                l = i;
                r = i + z[i];
            }
        }
        return z;
    }

    /**
     * First occurrence using extended Z matching over the pattern and text.
     * Keeps only the pattern's Z table, never concatenates the input strings,
     * and accepts every UTF-16 code unit (including '$' and unpaired surrogates).
     * Time O(n + m), auxiliary memory O(m).
     */
    public static int zSearch(String text, String pattern) {
        if (pattern.isEmpty()) return 0;
        // Historical String concatenation converts a null text to the four
        // UTF-16 units "null". Keep that existing observable contract.
        text = String.valueOf(text);
        int n = text.length(), m = pattern.length();
        if (m > n) return -1;
        int[] patternZ = zArray(pattern);
        int left = 0, right = 0;
        for (int i = 0; i <= n - m; i++) {
            int matched = 0;
            if (i < right) {
                matched = Math.min(right - i, patternZ[i - left]);
            }
            while (matched < m && text.charAt(i + matched) == pattern.charAt(matched)) {
                matched++;
            }
            if (i + matched > right) {
                left = i;
                right = i + matched;
            }
            if (matched == m) return i;
        }
        return -1;
    }

    /** Progress-aware compatibility entrypoint. */
    public static int[] zArray(String s, M3ProgressMonitor monitor) {
        M3ProgressMonitor checked = M3ProgressChecks.nonNull(monitor);
        M3ProgressChecks.checkCanceled(checked);
        checked.beginTask("zArray", 1L);
        try {
            var result = zArray(s);
            M3ProgressChecks.checkCanceled(checked);
            checked.worked(1L);
            return result;
        } finally {
            checked.done();
        }
    }

    /** Progress-aware compatibility entrypoint. */
    public static int zSearch(String text, String pattern, M3ProgressMonitor monitor) {
        M3ProgressMonitor checked = M3ProgressChecks.nonNull(monitor);
        M3ProgressChecks.checkCanceled(checked);
        checked.beginTask("zSearch", 1L);
        try {
            var result = zSearch(text, pattern);
            M3ProgressChecks.checkCanceled(checked);
            checked.worked(1L);
            return result;
        } finally {
            checked.done();
        }
    }

}
