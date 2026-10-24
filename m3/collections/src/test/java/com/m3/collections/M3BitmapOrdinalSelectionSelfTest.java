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
package com.m3.collections;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.function.LongPredicate;

/** Independent occurrence/stability oracle and hostile BitSet clone-origin witnesses. */
public final class M3BitmapOrdinalSelectionSelfTest {
    private static final long SEED = 0x4d334249544f5244L;
    private static long checks;
    private static final StringBuilder TAPE = new StringBuilder();

    private M3BitmapOrdinalSelectionSelfTest() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static BitSet bits(int... positions) {
        BitSet result = new BitSet();
        for (int position : positions) result.set(position);
        return result;
    }

    private static void expected(M3LongIndexProjection image, List<Integer> ids, long[] values) {
        check(image.size() == ids.size(), "selected occurrence count");
        int[] physical = image.physicalIndices();
        long[] materialized = image.toArray();
        check(physical.length == ids.size() && materialized.length == ids.size(), "materialization size");
        for (int i = 0; i < ids.size(); i++) {
            check(physical[i] == ids.get(i), "physical occurrence identity");
            check(image.physicalIndex(i) == ids.get(i), "indexed physical identity");
            check(image.get(i) == values[ids.get(i)], "indexed value");
            check(materialized[i] == values[ids.get(i)], "materialized value");
        }
    }

    private static List<Integer> selected(List<Integer> ids, BitSet bits) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) if (bits.get(i)) result.add(ids.get(i));
        return result;
    }

    private static List<Integer> sort(List<Integer> ids, long[] values, boolean descending) {
        List<Integer> result = new ArrayList<>(ids);
        Comparator<Integer> comparator = Comparator.comparingLong(i -> values[i]);
        result.sort(descending ? comparator.reversed() : comparator);
        return result;
    }

    private static void randomized() {
        Random random = new Random(SEED);
        for (int trial = 0; trial < 512; trial++) {
            long[] values = new long[random.nextInt(129)];
            List<Integer> ids = new ArrayList<>();
            for (int i = 0; i < values.length; i++) {
                values[i] = switch (i % 17) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(11) - 5L;
                };
                ids.add(i);
            }
            M3LongIndexProjection view = M3LongIndexProjection.snapshot(values);
            for (int pass = 0; pass < 6; pass++) {
                boolean descending = ((trial + pass) & 1) != 0;
                view = descending ? view.sortedDescending() : view.sortedAscending();
                ids = sort(ids, values, descending);
                expected(view, ids, values);
                int from = random.nextInt(ids.size() + 1);
                int count = random.nextInt(ids.size() - from + 1);
                view = view.slice(from, count);
                ids = new ArrayList<>(ids.subList(from, from + count));
                BitSet first = new BitSet(), second = new BitSet();
                for (int i = 0; i < ids.size(); i++) {
                    if (random.nextBoolean()) first.set(i);
                    if (random.nextBoolean()) second.set(i);
                }
                M3LongBitmapFilterPlan a = M3LongBitmapFilterPlan.fromBits(view, first);
                M3LongBitmapFilterPlan b = M3LongBitmapFilterPlan.fromBits(view, second);
                expected(a.projection(), selected(ids, first), values);
                check(a.projection() == a.projection(), "memoized projection identity");
                BitSet[] reference = {(BitSet) first.clone(), (BitSet) first.clone(),
                        (BitSet) first.clone(), (BitSet) first.clone(), (BitSet) first.clone()};
                reference[0].and(second);
                reference[1].or(second);
                reference[2].xor(second);
                reference[3].andNot(second);
                reference[4].flip(0, ids.size());
                M3LongBitmapFilterPlan[] plans = {a.and(b), a.or(b), a.xor(b), a.minus(b), a.not()};
                for (int operation = 0; operation < plans.length; operation++) {
                    expected(plans[operation].projection(), selected(ids, reference[operation]), values);
                    check(plans[operation].size() == reference[operation].cardinality(), "cached cardinality");
                }
                int[] callbacks = {0};
                M3LongBitmapFilterPlan refined = a.refine(value -> {
                    callbacks[0]++;
                    return value >= 0;
                });
                check(callbacks[0] == first.cardinality(), "refine evaluates selected occurrences once");
                List<Integer> refinedIds = selected(ids, first);
                refinedIds.removeIf(id -> values[id] < 0);
                expected(refined.projection(), refinedIds, values);
                expected(a.sortedAscending(), sort(selected(ids, first), values, false), values);
                expected(a.sortedDescending(), sort(selected(ids, first), values, true), values);
                first.flip(0, ids.size());
                expected(a.projection(), selected(ids, a.bitsCopy()), values);
                BitSet escaped = a.bitsCopy();
                escaped.flip(0, ids.size());
                expected(a.projection(), selected(ids, a.bitsCopy()), values);
                view = plans[1].projection();
                ids = selected(ids, reference[1]);
            }
        }
    }

    /** A legal virtual clone can expose a plain object whose lifetime remains external. */
    private static final class CloneSource extends BitSet {
        private static final long serialVersionUID = 1L;
        private final BitSet replacement;
        private final String name;

        CloneSource(String name, BitSet validated, BitSet replacement) {
            super.or(validated);
            this.name = name;
            this.replacement = replacement;
        }

        @Override public Object clone() {
            TAPE.append(name).append(".clone;");
            return replacement;
        }

        @Override public int length() {
            TAPE.append(name).append(".length;");
            return super.length();
        }

        @Override public int cardinality() {
            TAPE.append(name).append(".cardinality;");
            return super.cardinality();
        }

        @Override public int nextSetBit(int fromIndex) {
            TAPE.append(name).append(".next(").append(fromIndex).append(");");
            return super.nextSetBit(fromIndex);
        }
    }

    private static M3LongIndexProjection domain() {
        return M3LongIndexProjection.snapshot(new long[] {9, 1, 7, 1, 5, 3, 5, 2})
                .sortedAscending().slice(1, 4);
    }

    private static void sameCurrentSelection(M3LongIndexProjection source,
            M3LongBitmapFilterPlan plan, BitSet current, String label) {
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) if (current.get(i)) expected.add(source.physicalIndex(i));
        int[] actual = plan.projection().physicalIndices();
        check(actual.length == expected.size(), label + " changed-cardinality admission");
        for (int i = 0; i < actual.length; i++) check(actual[i] == expected.get(i), label + " identity");
        TAPE.append(label).append('=').append(Arrays.toString(actual)).append(';');
    }

    private static void outOfRange(M3LongBitmapFilterPlan plan, String label) {
        try {
            plan.projection();
            throw new AssertionError(label + " must retain byte-mask range refusal");
        } catch (ArrayIndexOutOfBoundsException expected) {
            TAPE.append(label).append('!').append(expected.getClass().getName()).append(';');
            check(true, label);
        }
    }

    private static M3LongBitmapFilterPlan combine(M3LongBitmapFilterPlan left,
            M3LongBitmapFilterPlan right, int operation) {
        return switch (operation) {
            case 0 -> left.and(right);
            case 1 -> left.or(right);
            case 2 -> left.xor(right);
            case 3 -> left.minus(right);
            case 4 -> left.not();
            case 5 -> left.refine(value -> true);
            default -> throw new AssertionError();
        };
    }

    private static void hostileOrigins() {
        M3LongIndexProjection source = domain();
        BitSet external = bits(0, 2);
        M3LongBitmapFilterPlan direct = M3LongBitmapFilterPlan.fromBits(source,
                new CloneSource("direct", bits(0, 2), external));
        check(direct.size() == 2, "original cached count");
        external.set(1);
        sameCurrentSelection(source, direct, external, "direct-after-mutation");
        check(direct.size() == 2, "opaque cached-size contract retained");

        for (int operation = 0; operation < 6; operation++) {
            BitSet escaped = bits(0, 2);
            CloneSource selected = new CloneSource("selected" + operation, bits(0, 2), escaped);
            M3LongBitmapFilterPlan opaque = M3LongBitmapFilterPlan.fromBits(source,
                    new CloneSource("root" + operation, bits(0, 2), selected));
            M3LongBitmapFilterPlan result = combine(opaque, M3LongBitmapFilterPlan.all(source), operation);
            // The algebra's virtual clone result can still be externally mutated after capture.
            escaped.flip(1);
            sameCurrentSelection(source, result, escaped, "operation" + operation);
        }

        BitSet outside = bits(0, 6); // physical owner has eight slots; logical window has four.
        M3LongBitmapFilterPlan opaqueOutside = M3LongBitmapFilterPlan.fromBits(source,
                new CloneSource("outside", bits(0), outside));
        outOfRange(opaqueOutside, "outside-window");
        for (int operation : new int[] {1, 2}) {
            M3LongBitmapFilterPlan trusted = M3LongBitmapFilterPlan.fromBits(source, bits(1));
            outOfRange(combine(trusted, opaqueOutside, operation), "trusted-left" + operation);
            outOfRange(combine(opaqueOutside, trusted, operation), "trusted-right" + operation);
        }

        BitSet normal = bits(0, 2);
        M3LongBitmapFilterPlan captured = M3LongBitmapFilterPlan.fromBits(source, normal);
        normal.clear();
        sameCurrentSelection(source, captured, bits(0, 2), "plain-source-snapshot");
        M3LongBitmapFilterPlan recaptured = M3LongBitmapFilterPlan.fromBits(source, captured.bitsCopy());
        sameCurrentSelection(source, recaptured, bits(0, 2), "plain-recapture");
    }

    private static void identityAndNativeBoundary() throws ReflectiveOperationException {
        M3LongIndexProjection source = domain();
        M3LongBitmapFilterPlan all = M3LongBitmapFilterPlan.all(source);
        M3LongIndexProjection projection = all.projection();
        for (String name : List.of("values", "indices")) {
            Field field = M3LongIndexProjection.class.getDeclaredField(name);
            field.setAccessible(true);
            check(field.get(source) == field.get(projection), "all-selected immutable " + name + " sharing");
        }
        check(all.projection() == all.projection(M3IndexMaskCompactor.Backend.JAVA), "Java cache identity");
        check(M3LongBitmapFilterPlan.none(source).projection().isEmpty(), "empty selection");
        try {
            all.and(M3LongBitmapFilterPlan.all(domain()));
            throw new AssertionError("different domain");
        } catch (IllegalArgumentException expected) {
            check(true, "strict source identity");
        }
        if (M3IndexMaskCompactor.nativeAvailable()) {
            check(Arrays.equals(projection.physicalIndices(),
                    all.projection(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED).physicalIndices()),
                    "required native parity");
        } else {
            try {
                all.projection(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                throw new AssertionError("required native must not use Java cache");
            } catch (UnsatisfiedLinkError expected) {
                check(true, "required native refusal");
            }
        }
        int[] calls = {0};
        LongPredicate predicate = value -> { calls[0]++; return value >= 0; };
        M3LongBitmapFilterPlan prepared = M3LongBitmapFilterPlan.where(source, predicate);
        check(calls[0] == source.size(), "one preparation per occurrence");
        prepared.projection(); prepared.sortedAscending(); prepared.sortedDescending();
        check(calls[0] == source.size(), "no predicate replay during reuse");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        checks = 0;
        TAPE.setLength(0);
        randomized();
        hostileOrigins();
        identityAndNativeBoundary();
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(TAPE.toString().getBytes(StandardCharsets.UTF_8)));
        System.out.println("M3_BITMAP_ORDINAL_SELECTION_PASS checks=" + checks + " seed=" + SEED
                + " opaqueTranscript=" + digest + " native=" + M3IndexMaskCompactor.nativeAvailable());
    }
}
