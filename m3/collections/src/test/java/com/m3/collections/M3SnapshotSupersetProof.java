/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/** Actual-owner regression for the shared-directory/fused-sort composition boundary. */
public final class M3SnapshotSupersetProof {
    private static long checks;
    private static final Comparator<Integer> ORDER = Comparator.nullsFirst(Integer::compareTo);

    private M3SnapshotSupersetProof() { }

    private static void check(boolean value, String what) {
        checks++;
        if (!value) throw new AssertionError(what);
    }

    private static void equal(M3IndexedListSnapshot<Integer> expected,
            M3IndexedListSnapshot<Integer> actual, String what) {
        check(Arrays.equals(expected.physicalIndices(), actual.physicalIndices()), what + " physical occurrence order");
        check(Arrays.equals(expected.toArray(), actual.toArray()), what + " values");
        check(expected.physicalMaskCopy().equals(actual.physicalMaskCopy()), what + " membership");
        for (int i = 0; i < expected.size(); i++) {
            check(expected.get(i) == actual.get(i), what + " exact object identity");
        }
    }

    private static Object directory(M3IndexedListSnapshot<?> snapshot) throws ReflectiveOperationException {
        Field field = M3IndexedListSnapshot.class.getDeclaredField("positions");
        field.setAccessible(true);
        return field.get(snapshot);
    }

    private static void requireUnchanged(M3IndexedListSnapshot<Integer> source,
            int[] indexes, Object[] values, String what) {
        check(Arrays.equals(indexes, source.physicalIndices()), what + " source indexes changed");
        check(Arrays.equals(values, source.toArray()), what + " source values changed");
    }

    private static void ownership() throws ReflectiveOperationException {
        M3IndexedListSnapshot<Integer> source = M3IndexedListSnapshot.copyOf(new Integer[] {7, 3, 5, 1});
        int[] original = source.physicalIndices();
        Object[] payload = source.toArray();
        M3IndexedListSnapshot<Integer> sibling = source.slice(1, 2);
        int[] siblingBefore = sibling.physicalIndices();
        BitSet membership = source.physicalMaskCopy();
        M3IndexedListSnapshot<Integer> all = source.filtered(value -> true);
        check(all != source, "fresh filter result wrapper");
        check(directory(all) == directory(source), "reuse full immutable directory");
        M3IndexedListSnapshot<Integer> sorted = source.filteredSorted(value -> true, ORDER);
        requireUnchanged(source, original, payload, "all-selected fused sort");
        check(Arrays.equals(siblingBefore, sibling.physicalIndices()), "sibling remains immutable");
        check(membership.equals(source.physicalMaskCopy()), "source membership remains valid");
        check(directory(sorted) != directory(source), "sort cannot publish shared mutable directory");
        equal(source.sorted(ORDER), sorted, "all-selected fused sort");
        M3IndexedListSnapshot<Integer> partialAll = sibling.filtered(value -> true);
        check(((int[]) directory(partialAll)).length == sibling.size(), "partial slice retains exact small directory");
        for (int failAt = 1; failAt <= 5; failAt++) {
            RuntimeException marker = new IllegalStateException("comparator marker " + failAt);
            int stop = failAt;
            int[] calls = {0};
            try {
                source.filteredSorted(value -> true, (left, right) -> {
                    requireUnchanged(source, original, payload, "reentrant comparator");
                    if (++calls[0] == stop) throw marker;
                    return ORDER.compare(left, right);
                });
            } catch (RuntimeException actual) {
                check(actual == marker, "exact comparator exception object");
            }
            requireUnchanged(source, original, payload, "throwing comparator");
        }
    }

    private static void cases(boolean nativeMode) {
        Random random = new Random(0x4d335355504552L);
        int[] sizes = {0,1,2,3,4,5,7,8,9,31,32,33,63,64,65,127,128,129,255,256,257,513};
        for (int trial = 0; trial < 1600; trial++) {
            int length = trial < sizes.length ? sizes[trial] : random.nextInt(300);
            Integer[] values = new Integer[length];
            int[] states = new int[length];
            for (int i = 0; i < length; i++) {
                values[i] = i % 11 == 0 ? null : Integer.valueOf(random.nextInt(27) - 13);
                states[i] = random.nextInt();
            }
            M3IndexedListSnapshot<Integer> source = M3IndexedListSnapshot.copyOf(values);
            if ((trial & 1) != 0) source = source.sorted(ORDER.reversed());
            int from = length == 0 ? 0 : random.nextInt(length + 1);
            int to = from + (length == from ? 0 : random.nextInt(length - from + 1));
            source = source.subList(from, to);
            int[] original = source.physicalIndices();
            Object[] payload = source.toArray();
            int category = trial % 5;
            Predicate<Integer> test = value -> category == 0 || category != 1
                    && (value == null ? category == 2 : (value & 3) == category - 2);
            List<Integer> filterTape = new ArrayList<>();
            List<Integer> fusedTape = new ArrayList<>();
            List<String> referenceComparisons = new ArrayList<>();
            List<String> fusedComparisons = new ArrayList<>();
            M3IndexMaskCompactor.Backend backend = nativeMode
                    ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
            M3IndexedListSnapshot<Integer> expected = source.filtered(value -> {
                filterTape.add(value); return test.test(value);
            }, backend).sorted((a, b) -> { referenceComparisons.add(a + ":" + b); return ORDER.compare(a, b); });
            M3IndexedListSnapshot<Integer> actual = source.filteredSorted(value -> {
                fusedTape.add(value); return test.test(value);
            }, (a,b) -> { fusedComparisons.add(a + ":" + b); return ORDER.compare(a,b); }, backend);
            equal(expected, actual, "fused differential " + trial);
            check(filterTape.equals(fusedTape) && filterTape.size() == source.size(), "predicate tape/count");
            check(referenceComparisons.equals(fusedComparisons), "comparator tape");
            requireUnchanged(source, original, payload, "differential");
            int required = switch (trial % 4) { case 0 -> 0; case 1 -> Integer.MIN_VALUE; case 2 -> 3; default -> -1; };
            int forbidden = trial % 7 == 0 ? required : trial % 3 == 0 ? 8 : 0;
            List<Integer> expectedPhysical = new ArrayList<>();
            for (int physical : original) {
                int word = states[physical];
                if ((word & required) == required && (word & forbidden) == 0) expectedPhysical.add(physical);
            }
            final M3IndexedListSnapshot<Integer> stableSource = source;
            expectedPhysical.sort((a,b) -> ORDER.compare(values[a],values[b]));
            M3IndexPathMasks.Backend pathBackend = nativeMode
                    ? M3IndexPathMasks.Backend.NATIVE_REQUIRED : M3IndexPathMasks.Backend.JAVA;
            M3IndexedListSnapshot<Integer> masked = stableSource.sortedMaskedByPhysicalState(
                    states, required, forbidden, ORDER, pathBackend);
            check(masked.size() == expectedPhysical.size(), "mask cardinality");
            for (int i = 0; i < masked.size(); i++) {
                int physical = expectedPhysical.get(i);
                check(masked.physicalIndex(i) == physical, "mask physical identity");
                check(masked.get(i) == values[physical], "mask object identity");
            }
            equal(source.maskedByPhysicalState(states,required,forbidden,pathBackend).sorted(ORDER),
                    masked,"masked fusion");
            int[] captured = masked.physicalIndices();
            Arrays.fill(states, ~required);
            check(Arrays.equals(captured, masked.physicalIndices()), "state lane not retained");
            requireUnchanged(source, original, payload, "masked differential");
        }
    }

    private static void refusals(boolean missing) {
        M3IndexedListSnapshot<Integer> source = M3IndexedListSnapshot.copyOf(new Integer[] {3,2,1});
        int[] calls = {0};
        RuntimeException marker = new IllegalArgumentException("predicate marker");
        try {
            source.filteredSorted(value -> { calls[0]++; throw marker; }, ORDER);
            throw new AssertionError("predicate failure swallowed");
        } catch (RuntimeException actual) { check(actual == marker && calls[0] == 1,"predicate failure boundary"); }
        try {
            source.maskedByPhysicalState(new int[2],0,0,M3IndexPathMasks.Backend.JAVA);
            throw new AssertionError("wrong physical alignment accepted");
        } catch (IllegalArgumentException expected) { check(true,"alignment refusal"); }
        if (missing) {
            check(!M3IndexMaskCompactor.nativeAvailable(), "must exercise actual missing mask library");
            check(!M3IndexPathMasks.nativeAvailable(), "must exercise actual missing path library");
            calls[0]=0;
            try {
                source.filteredSorted(value -> { calls[0]++; return true; }, ORDER,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                throw new AssertionError("required native fallback");
            } catch (UnsatisfiedLinkError expected) { check(calls[0]==0,"native refusal before predicates"); }
            try {
                source.sortedMaskedByPhysicalState(new int[3],0,0,ORDER,M3IndexPathMasks.Backend.NATIVE_REQUIRED);
                throw new AssertionError("required physical native fallback");
            } catch (UnsatisfiedLinkError expected) { check(true,"physical native refusal"); }
        }
        try { source.filteredSorted(v -> true,ORDER).removeIf(v -> false); throw new AssertionError("immutable noop accepted"); }
        catch (UnsupportedOperationException expected) { check(true,"strict immutable noop refusal"); }
    }

    private static void independentReaders() throws Exception {
        Integer[] values = new Integer[257];
        for (int i=0;i<values.length;i++) values[i]=values.length-i;
        M3IndexedListSnapshot<Integer> source=M3IndexedListSnapshot.copyOf(values);
        int[] before=source.physicalIndices();
        List<Callable<Boolean>> work=new ArrayList<>();
        for (int worker=0;worker<4;worker++) work.add(() -> {
            for (int i=0;i<120;i++) {
                M3IndexedListSnapshot<Integer> result=source.filteredSorted(v -> true, ORDER);
                if (result.get(0)!=1 || !Arrays.equals(before,source.physicalIndices())) return false;
            }
            return true;
        });
        try (var executor=Executors.newFixedThreadPool(4)) {
            for (var result:executor.invokeAll(work)) check(result.get(),"independent immutable readers");
        }
    }

    public static void main(String[] args) throws Exception {
        boolean nativeMode=Arrays.asList(args).contains("--native");
        boolean missing=Arrays.asList(args).contains("--missing");
        if (nativeMode) {
            check(M3IndexMaskCompactor.nativeAvailable(),"native mask backend required");
            check(M3IndexPathMasks.nativeAvailable(),"native physical mask backend required");
        }
        ownership(); cases(nativeMode); refusals(missing); independentReaders();
        System.out.println("M3SnapshotSupersetProof PASS checks="+checks+" native="+nativeMode+" missing="+missing);
    }
}
