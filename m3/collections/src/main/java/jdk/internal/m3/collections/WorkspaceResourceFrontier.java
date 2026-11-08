/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.Objects;
import jdk.m3.collections.M3RuntimeHeap;

/**
 * Minimal ancestor resource scope set over caller-owned portable workspace paths.
 *
 * <p>Marker collectors that scan descendants need only topmost selected scopes.
 * Resource coverage is computed before scanning, in O(n log n) path comparisons,
 * not by an O(n^2) pairwise scan. This is an indexed, primitive-only adaptation of
 * resource-scope minimization; no Eclipse IResource or Map.Entry is retained here.
 * A slash sorts before all other characters, ensuring each ancestor's descendants
 * remain adjacent; using ordinary String.compareTo then only checking the previous
 * accepted path is insufficient for sibling names such as '/a-b'.</p>
 *
 * <p>Input paths must already use normalized absolute, case-sensitive workspace
 * syntax; filesystem-specific IPath/adapters and resource mappings stay with their
 * Eclipse owner. Original input and paths are unchanged. Output IDs are returned in
 * hierarchical order. Sort scratch uses an int[] merge lane when heap pressure is
 * normal, and an allocation-free (beyond caller output) heapsort under pressure.
 * JVM/Xmx feedback is sampled once per scope computation, not for each resource.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/WorkspaceResourceFrontier.java}
 * blob {@code 0042ae94c2219daab12d4211acb9d28b597dba91} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class WorkspaceResourceFrontier {
    private WorkspaceResourceFrontier() { }

    public static int minimalScopes(String[] paths, int[] output) {
        return minimalScopes(paths, output, M3RuntimeHeap.current());
    }

    /** Deterministic pressure sample seam for tests; no Java entry objects are admitted. */
    static int minimalScopes(String[] paths, int[] output, M3RuntimeHeap.Snapshot heap) {
        Objects.requireNonNull(paths, "paths");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(heap, "heap");
        if (output.length < paths.length) throw new IllegalArgumentException("output too short");
        for (String path : paths) validate(path);
        int n = paths.length;
        if (n == 0) return 0;
        for (int i = 0; i < n; i++) output[i] = i;
        long peak = 4L * n + 24L;
        if (heap.pressure(peak) != M3RuntimeHeap.Pressure.NORMAL)
            heapSort(output, n, paths);
        else mergeSort(output, n, paths);
        String last = null;
        int kept = 0;
        for (int i = 0; i < n; i++) {
            int original = output[i];
            String path = paths[original];
            if (last == null || !covers(last, path)) {
                output[kept++] = original;
                last = path;
            }
        }
        return kept;
    }

    private static void validate(String path) {
        Objects.requireNonNull(path, "path");
        if (path.isEmpty() || path.charAt(0) != '/' || path.indexOf("//") >= 0 ||
                path.length() > 1 && path.charAt(path.length() - 1) == '/')
            throw new IllegalArgumentException("path must be canonical absolute workspace path");
        if (path.contains("/./") || path.contains("/../") || path.endsWith("/.") ||
                path.endsWith("/.."))
            throw new IllegalArgumentException("dot path segment");
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '\\' || path.charAt(i) == '\u0000')
                throw new IllegalArgumentException("unsupported path separator/control");
        }
    }

    static boolean covers(String parent, String candidate) {
        return parent.equals(candidate) || parent.equals("/") ||
                candidate.length() > parent.length() && candidate.startsWith(parent)
                        && candidate.charAt(parent.length()) == '/';
    }

    /** Segment-aware lexical comparison; slash-before-symbol preserves ancestor grouping. */
    private static int comparePaths(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            char x = a.charAt(i), y = b.charAt(i);
            int rankedX = x == '/' ? -1 : x;
            int rankedY = y == '/' ? -1 : y;
            if (rankedX != rankedY) return Integer.compare(rankedX, rankedY);
        }
        return Integer.compare(a.length(), b.length());
    }

    private static int compare(int first, int second, String[] paths) {
        int result = comparePaths(paths[first], paths[second]);
        return result != 0 ? result : Integer.compare(first, second);
    }

    private static void mergeSort(int[] ids, int n, String[] paths) {
        int[] scratch = new int[n];
        int[] input = ids, output = scratch;
        for (long width = 1; width < n; width <<= 1) {
            for (long start = 0; start < n; start += 2 * width) {
                int a = (int) start;
                int mid = (int) Math.min(n, start + width);
                int end = (int) Math.min(n, start + width * 2);
                int b = mid;
                for (int dest = a; dest < end; dest++) {
                    if (b == end || a < mid && compare(input[a], input[b], paths) <= 0)
                        output[dest] = input[a++];
                    else output[dest] = input[b++];
                }
            }
            int[] temp = input; input = output; output = temp;
        }
        if (input != ids) System.arraycopy(input, 0, ids, 0, n);
    }

    private static void heapSort(int[] ids, int n, String[] paths) {
        for (int pos = (n >>> 1) - 1; pos >= 0; pos--)
            sift(ids, pos, n, paths);
        for (int end = n - 1; end > 0; end--) {
            int value = ids[0]; ids[0] = ids[end]; ids[end] = value;
            sift(ids, 0, end, paths);
        }
    }

    private static void sift(int[] ids, int position, int count, String[] paths) {
        while (position < (count >>> 1)) {
            int child = (position << 1) + 1;
            if (child + 1 < count && compare(ids[child + 1], ids[child], paths) > 0)
                child++;
            if (compare(ids[position], ids[child], paths) >= 0) return;
            int value = ids[position]; ids[position] = ids[child]; ids[child] = value;
            position = child;
        }
    }
}
