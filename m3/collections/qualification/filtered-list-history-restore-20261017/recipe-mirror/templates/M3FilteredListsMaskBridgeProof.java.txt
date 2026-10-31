/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;

/**
 * Java 21 reference proof: live filtered List semantics against detached
 * primitive index masks and the incumbent opt-in C/JNI selector.
 * No Java object, predicate or comparator is passed across JNI.
 */
public final class M3FilteredListsMaskBridgeProof {
    private static long checks;
    private M3FilteredListsMaskBridgeProof() { }

    private static void eq(Object want, Object got, String why) {
        checks++;
        if (!Objects.equals(want, got)) {
            throw new AssertionError(why + ": want=" + want + " got=" + got);
        }
    }
    private static void ok(boolean condition, String why) {
        checks++;
        if (!condition) throw new AssertionError(why);
    }
    private static void fail(Class<? extends Throwable> type, Runnable action, String why) {
        checks++;
        try { action.run(); }
        catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError(why + " wrong exception", thrown);
        }
        throw new AssertionError(why + " did not refuse");
    }

    private static void sample(boolean nativeRun) {
        List<Integer> source = new ArrayList<>(
                Arrays.asList(-7, 8, 8, 5, null, 10));
        List<Integer> filtered =
                M3FilteredLists.filtered(source, v -> v != null && (v & 1) == 0);
        int[] physical = {0, 1, 2, 3, 4, 5};
        byte[] state = {0, 1, 1, 0, 0, 1};
        int[] selected = M3IndexMaskCompactor.select(physical, state, 1,
                M3IndexMaskCompactor.Backend.JAVA);
        ok(Arrays.equals(new int[] {1, 2, 5}, selected), "physical ordinal mapping");
        eq(List.of(8, 8, 10), filtered, "filtered duplicate occurrences");
        for (int i = 0; i < selected.length; i++) {
            eq(source.get(selected[i]), filtered.get(i), "physical and logical values");
        }
        Spliterator<Integer> pending = filtered.spliterator();
        source.add(12);
        eq(List.of(8, 8, 10, 12), filtered, "live source update");
        ArrayList<Integer> advanced = new ArrayList<>();
        pending.forEachRemaining(advanced::add);
        eq(filtered, advanced, "late-bound traversal");
        eq(1, M3IndexMaskCompactor.selectedCount(new byte[] {(byte) 0x80, 0, 1}, 128),
                "unsigned high-bit state");
        int[] duplicated = {9, 9, 4};
        byte[] flags = {(byte) 0x80, 0, (byte) 0x81};
        int[] highBits = M3IndexMaskCompactor.select(duplicated, flags, 128,
                M3IndexMaskCompactor.Backend.JAVA);
        ok(Arrays.equals(new int[] {9, 4}, highBits), "stable duplicate-ID occurrences");
        fail(IllegalArgumentException.class, () -> M3IndexMaskCompactor.select(
                physical, new byte[2], 1, M3IndexMaskCompactor.Backend.JAVA),
                "unaligned index/state arrays must fail before JNI");
        if (nativeRun) {
            int[] result = M3IndexMaskCompactor.select(duplicated, flags, 128,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            ok(Arrays.equals(highBits, result), "actual native high-bit and duplicate-ID parity");
        } else if (!M3IndexMaskCompactor.nativeAvailable()) {
            fail(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.select(
                    physical, state, 1, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED),
                    "native-required must not silently fall back");
        }
    }

    private static void randomized(boolean nativeRun) {
        Random random = new Random(0x4d334c4953544a4eL);
        for (int trial = 0; trial < 512; trial++) {
            int size = random.nextInt(128);
            List<Integer> source = new ArrayList<>();
            int[] physical = new int[size];
            byte[] state = new byte[size];
            for (int slot = 0; slot < size; slot++) {
                Integer value = random.nextInt(9) == 0
                        ? null : Integer.valueOf(random.nextInt(61) - 30);
                source.add(value);
                physical[slot] = slot;
                state[slot] = (byte) (value != null && (value & 1) == 0 ? 1 : 0);
            }
            List<Integer> filtered = M3FilteredLists.filtered(
                    source, value -> value != null && (value & 1) == 0);
            int[] selected = M3IndexMaskCompactor.select(
                    physical, state, 1, M3IndexMaskCompactor.Backend.JAVA);
            eq(selected.length, filtered.size(), "selected cardinality");
            for (int ordinal = 0; ordinal < selected.length; ordinal++) {
                eq(source.get(selected[ordinal]), filtered.get(ordinal),
                        "selected physical reference at logical ordinal");
            }
            if (nativeRun) {
                int[] actual = M3IndexMaskCompactor.select(
                        physical, state, 1, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                ok(Arrays.equals(selected, actual), "native index selection parity");
            }
            // Selection never owns, removes, swaps or unroots the source elements.
            eq(size, source.size(), "source size untouched");
        }
    }

    public static void main(String[] args) {
        boolean nativeRun = args.length == 1 && "--native".equals(args[0]);
        if (args.length > 1 || args.length == 1 && !nativeRun) {
            throw new IllegalArgumentException("usage: [--native]");
        }
        if (nativeRun && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("requested native mask selector was not loaded");
        }
        sample(nativeRun);
        randomized(nativeRun);
        System.out.println("PASS M3FilteredListsMaskBridgeProof checks=" + checks
                + " native=" + nativeRun);
    }
}
