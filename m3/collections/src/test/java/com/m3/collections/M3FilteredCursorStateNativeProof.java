// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Required existing JNI selector oracle. Every Java predicate stays in Java;
 * only precomputed primitive row-position masks cross the JNI boundary.
 */
public final class M3FilteredCursorStateNativeProof {
    private static long checks;

    private M3FilteredCursorStateNativeProof() { }

    public static void main(String[] args) {
        if (!M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("existing M3 index-mask JNI selector unavailable");
        }
        Random random = new Random(0x4d33434d534b2026L);
        for (int trial = 0; trial < 700; trial++) {
            int size = random.nextInt(180);
            List<Integer> values = new ArrayList<>(size);
            byte[] accepted = new byte[size];
            for (int index = 0; index < size; index++) {
                int value = random.nextInt(15) - 7;
                values.add(value);
                if ((value & 1) == 0) accepted[index] = 1;
            }
            int[] selected = M3IndexMaskCompactor.selectPositions(
                    accepted, 1, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            int[] javaSelected = M3IndexMaskCompactor.selectPositions(
                    accepted, 1, M3IndexMaskCompactor.Backend.JAVA);
            equal(true, Arrays.equals(javaSelected, selected),
                    "existing JNI selector exactly equals Java primitive oracle");
            List<Integer> expected = new ArrayList<>(selected.length);
            for (int physical : selected) expected.add(values.get(physical));
            equal(expected, copy(M3FilteredLists.filtered(
                    new ArrayList<>(values), value -> (value & 1) == 0)),
                    "live list selection versus existing JNI ordinal masks");
            equal(expected, copy(M3FilteredViews.filter(
                    (Collection<Integer>) values, value -> (value & 1) == 0)),
                    "read-only backed wrapper selection versus existing JNI masks");
            Deque<Integer> deque = new ArrayDeque<>(values);
            equal(expected, copy(M3FilteredDeques.filtered(
                    deque, value -> (value & 1) == 0)),
                    "deque selection versus existing JNI masks");
            if (selected.length > 0) {
                int sourceOrdinal = selected[0];
                selected[0] = -1;
                int[] fresh = M3IndexMaskCompactor.selectPositions(
                        accepted, 1, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                equal(sourceOrdinal, fresh[0],
                        "JNI position result is detached from later native selection");
                equal(expected.getFirst(), values.get(sourceOrdinal),
                        "original source is unaffected by native output mutation");
            }
        }
        System.out.println("PASS M3FilteredCursorStateNativeProof checks=" + checks
                + " modes=Java-backed+native-required");
    }

    private static <T> List<T> copy(Iterable<T> source) {
        List<T> result = new ArrayList<>();
        for (T item : source) result.add(item);
        return result;
    }

    private static void equal(Object expected, Object actual, String why) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(why + " expected=" + expected + " actual=" + actual);
        }
    }
}
