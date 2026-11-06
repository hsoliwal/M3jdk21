// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Arrays;
import java.util.Objects;

/** Singly-linked-list algorithms over indexed primitive arrays. */
//FLLDCIM: RELOCATE - donor blob 5dfc1d9e5185 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class M3LinkedListAlgorithms {
    private M3LinkedListAlgorithms() {}

    public record IntLinkedList(int[] values, int[] next, int head) {
        public IntLinkedList {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(next, "next");
            if (values.length != next.length) throw new IllegalArgumentException("list array lengths differ");
            if (values.length == 0) {
                if (head != -1) throw new IllegalArgumentException("empty list head must be -1");
            } else if (head < -1 || head >= values.length) {
                throw new IllegalArgumentException("head out of range");
            }
            for (int link : next) {
                if (link < -1 || link >= values.length) {
                    throw new IllegalArgumentException("next index out of range");
                }
            }
            values = values.clone();
            next = next.clone();
        }

        @Override public int[] values() { return values.clone(); }
        @Override public int[] next() { return next.clone(); }

        int[] rawValues() { return values; }
        int[] rawNext() { return next; }

        public static IntLinkedList linear(int[] values) {
            Objects.requireNonNull(values, "values");
            int[] next = new int[values.length];
            for (int i = 0; i < values.length; i++) next[i] = i + 1 < values.length ? i + 1 : -1;
            return new IntLinkedList(values, next, values.length == 0 ? -1 : 0);
        }
    }

    public static int cycleStartIndex(IntLinkedList list) {
        Objects.requireNonNull(list, "list");
        if (list.head() < 0) return -1;
        int[] next = list.rawNext();
        int slow = list.head();
        int fast = list.head();
        do {
            slow = step(next, slow);
            fast = step(next, step(next, fast));
            if (slow < 0 || fast < 0) return -1;
        } while (slow != fast);
        slow = list.head();
        while (slow != fast) {
            slow = next[slow];
            fast = next[fast];
        }
        return slow;
    }

    public static boolean hasCycle(IntLinkedList list) {
        return cycleStartIndex(list) >= 0;
    }

    public static int[] toArray(IntLinkedList list) {
        Objects.requireNonNull(list, "list");
        requireAcyclic(list);
        int length = 0;
        int[] next = list.rawNext();
        for (int node = list.head(); node >= 0; node = next[node]) length++;
        int[] output = new int[length];
        int cursor = 0;
        int[] values = list.rawValues();
        for (int node = list.head(); node >= 0; node = next[node]) output[cursor++] = values[node];
        return output;
    }

    public static IntLinkedList reordered(IntLinkedList list) {
        Objects.requireNonNull(list, "list");
        int[] values = toArray(list);
        if (values.length < 3) return IntLinkedList.linear(values);
        int[] reordered = new int[values.length];
        int left = 0;
        int right = values.length - 1;
        int cursor = 0;
        while (left <= right) {
            reordered[cursor++] = values[left++];
            if (left <= right) reordered[cursor++] = values[right--];
        }
        return IntLinkedList.linear(reordered);
    }

    private static int step(int[] next, int node) {
        return node < 0 ? -1 : next[node];
    }

    private static void requireAcyclic(IntLinkedList list) {
        if (cycleStartIndex(list) >= 0) throw new IllegalArgumentException("operation requires acyclic list");
    }
}
