/* *****************************************************************************
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
package jdk.m3.collections.vavr.collection;

import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import jdk.m3.collections.vavr.Function1;

/**
 * Lockstep machinery for BitSetWordOpsOracleTest: element mappings, one plan shared by a patched
 * side and a pre-patch reference side, receivers built from the same words, and logged sources.
 */
//FLLDCIM: LEETCODIFY — shared lockstep kit for the vavr BitSet word-ops oracle
@SuppressWarnings({"unchecked", "rawtypes", "auxiliaryclass"})
final class BitSetOracleKit {

    static final StringBuilder LOG = new StringBuilder();
    static final int SPAN = 64 * 40;
    private static final int[] LIMITS = {64, 128, SPAN};

    static final Mapping DEFAULT = exact("default", BitSet.Builder.DEFAULT, Integer.MAX_VALUE, -5);
    static final Mapping CHARS = exact("chars", BitSet.withCharacters(), 65535, null);
    static final Mapping BYTES = exact("bytes", BitSet.withBytes(), 127, (byte) -5);
    static final Mapping SHORTS = exact("shorts", BitSet.withShorts(), 32767, (short) -5);
    static final Mapping LONGS = exact("longs", BitSet.withLongs(), Integer.MAX_VALUE, -5L);
    static final Mapping ENUMS = new Mapping("enums", BitSet.withEnum(ChronoField.class),
            i -> ChronoField.values()[i], 29, null, false);
    static final Mapping BOOLS = new Mapping("bools", builderOf(BitSet.ofAll(true)), i -> i != 0,
            1, null, false);
    static final Mapping IDENTITY = loose("identity", BitSet.withRelations(i -> i, i -> i));
    static final Mapping COUNTED = loose("counted",
            BitSet.withRelations(BitSetOracleKit::countFrom, BitSetOracleKit::countTo));
    static final Mapping SHIFTED = loose("shifted", BitSet.withRelations(i -> i + 1, i -> i));
    static final Mapping THROWING = loose("throwing",
            BitSet.withRelations(i -> i, BitSetOracleKit::throwTo));
    static final Mapping[] ALL = {DEFAULT, CHARS, BYTES, SHORTS, LONGS, ENUMS, BOOLS, IDENTITY,
        COUNTED, SHIFTED, THROWING};

    private BitSetOracleKit() {
    }

    /** One element mapping: the function pair, the element factory and the bit domain. */
    static final class Mapping {
        final String name;
        final Function1<Integer, Object> from;
        final Function1<Object, Integer> to;
        final IntFunction<Object> elem;
        final int maxBit;
        final Optional<Object> negative;
        final boolean exact;

        Mapping(String name, BitSet.Builder<?> b, IntFunction<Object> elem, int maxBit,
                Object negative, boolean exact) {
            this.name = name;
            this.from = (Function1) b.fromInt;
            this.to = (Function1) b.toInt;
            this.elem = elem;
            this.maxBit = maxBit;
            this.negative = Optional.ofNullable(negative);
            this.exact = exact;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static Mapping exact(String name, BitSet.Builder<?> b, int maxBit, Object negative) {
        return new Mapping(name, b, i -> b.fromInt.apply(i), maxBit, negative, true);
    }

    private static Mapping loose(String name, BitSet.Builder<?> b) {
        return new Mapping(name, b, Integer::valueOf, SPAN - 2, -5, false);
    }

    private static BitSet.Builder<Object> builderOf(BitSet<?> set) {
        BitSetModule.AbstractBitSet<Object> a = (BitSetModule.AbstractBitSet<Object>) set;
        return new BitSet.Builder<>(a.fromInt, a.toInt);
    }

    private static Object countFrom(Integer i) {
        LOG.append('f').append(i).append(' ');
        return i;
    }

    private static Integer countTo(Object v) {
        LOG.append('t').append(v).append(' ');
        return (Integer) v;
    }

    private static Integer throwTo(Object v) {
        if (Objects.equals(v, 77)) {
            throw new IllegalStateException("toInt 77");
        }
        return (Integer) v;
    }

    /** The random choices of one lockstep step, shared by both sides. */
    static final class Plan {
        final long[] recvWords;
        final long[] argWords;
        final long[] otherWords;
        final long[] countedWords;
        final java.util.List<Object> order;
        final java.util.List<Object> special;
        final int n;
        final int j;
        final int mod;

        Plan(long[][] words, java.util.List<Object> order, java.util.List<Object> special,
                int[] knobs) {
            this.recvWords = words[0];
            this.argWords = words[1];
            this.otherWords = words[2];
            this.countedWords = words[3];
            this.order = order;
            this.special = special;
            this.n = knobs[0];
            this.j = knobs[1];
            this.mod = knobs[2];
        }
    }

    static Plan randomPlan(Random r, Mapping m, boolean ragged) {
        int[] recv = bits(r, m);
        int[] arg = argBits(r, m, recv);
        int extra = ragged || r.nextInt(8) == 0 ? 1 + r.nextInt(2) : 0;
        long[][] words = {words(recv, extra), words(arg, r.nextInt(8) == 0 ? 1 : 0),
            words(bits(r, DEFAULT), 0), words(bits(r, COUNTED), 0)};
        java.util.List<Object> order = order(r, m, arg);
        int[] knobs = {pickN(r, recv.length), 1 + r.nextInt(recv.length + 3), 2 + r.nextInt(4)};
        return new Plan(words, order, special(r, m, order), knobs);
    }

    static Plan fixedPlan(Mapping m, int[] recv, int[] arg, java.util.List<Object> special) {
        java.util.List<Object> order = new ArrayList<>();
        IntStream.of(arg).forEach(b -> order.add(m.elem.apply(b)));
        long[][] words = {words(recv, 0), words(arg, 0), words(new int[] {3}, 0),
            words(new int[] {4}, 0)};
        return new Plan(words, order, special, new int[] {1, 2, 3});
    }

    static int[] bits(Random r, Mapping m) {
        int bound = Math.min(m.maxBit, SPAN - 1) + 1;
        int limit = Math.min(bound, LIMITS[r.nextInt(LIMITS.length)]);
        int size = r.nextInt(4) == 0 ? r.nextInt(4) : r.nextInt(301);
        return r.ints(size, 0, limit).distinct().sorted().toArray();
    }

    private static int[] argBits(Random r, Mapping m, int[] recv) {
        return switch (r.nextInt(5)) {
            case 0 -> bits(r, m);
            case 1 -> IntStream.of(recv).filter(b -> r.nextBoolean()).toArray();
            case 2 -> IntStream.concat(IntStream.of(recv), IntStream.of(bits(r, m)))
                    .distinct().sorted().toArray();
            case 3 -> recv.clone();
            default -> new int[0];
        };
    }

    static long[] words(int[] bits, int extra) {
        int max = bits.length == 0 ? 0 : bits[bits.length - 1];
        long[] w = new long[(max >> 6) + 1 + extra];
        IntStream.of(bits).forEach(b -> w[b >> 6] |= 1L << b);
        return w;
    }

    private static java.util.List<Object> order(Random r, Mapping m, int[] arg) {
        java.util.List<Object> out = new ArrayList<>();
        IntStream.of(arg).forEach(b -> out.add(m.elem.apply(b)));
        if (r.nextBoolean()) {
            java.util.Collections.shuffle(out, r);
        }
        if (!out.isEmpty() && r.nextInt(4) == 0) {
            out.add(out.get(r.nextInt(out.size())));
        }
        return out;
    }

    private static java.util.List<Object> special(Random r, Mapping m,
            java.util.List<Object> order) {
        java.util.List<Object> out = new ArrayList<>(order);
        Object odd = m.negative.isPresent() && r.nextBoolean() ? m.negative.get() : null;
        switch (r.nextInt(5)) {
            case 0 -> out.add(0, odd);
            case 1 -> out.add(Math.min(1, out.size()), odd);
            case 2 -> out.add(odd);
            case 3 -> out.subList(0, out.size()).clear();
            default -> out.add(r.nextInt(out.size() + 1), odd);
        }
        if (out.isEmpty()) {
            out.add(odd);
        }
        return out;
    }

    private static int pickN(Random r, int len) {
        int[] picks = {-1, 0, 1, r.nextInt(len + 1), len - 1, len, len + 1};
        return picks[r.nextInt(picks.length)];
    }

    /** One side of the lockstep: patched classes, or the pre-patch reference copy. */
    static final class Side {
        final boolean ref;
        final Mapping m;
        final Plan plan;
        final BitSet<Object> empty;
        final BitSet<Object> recv;
        final BitSet<Object> arg;
        final BitSet<Object> other;
        final BitSet<Object> counted;

        Side(boolean ref, Mapping m, Plan plan) {
            this.ref = ref;
            this.m = m;
            this.plan = plan;
            this.empty = emptyOf(m, ref);
            this.recv = make(empty, plan.recvWords);
            this.arg = make(empty, plan.argWords);
            this.other = make(emptyOf(m == DEFAULT ? CHARS : DEFAULT, ref), plan.otherWords);
            this.counted = make(emptyOf(COUNTED, ref), plan.countedWords);
        }

        BitSet<Object> ofAll(Iterable<Object> values) {
            if (ref) {
                Objects.requireNonNull(values, "values is null");
                return empty.addAll(values);
            }
            return new BitSet.Builder<>(m.from, m.to).ofAll(values);
        }

        BitSet<Object> of(Object[] values) {
            return ref ? empty.addAll(Array.wrap(values)) : new BitSet.Builder<>(m.from, m.to)
                    .of(values);
        }

        BitSet<Object> tabulate(int n, Function<Integer, Object> f) {
            if (ref) {
                Objects.requireNonNull(f, "f is null");
                return empty.addAll(Collections.tabulate(n, f));
            }
            return new BitSet.Builder<>(m.from, m.to).tabulate(n, f);
        }

        BitSet<Object> fill(int n, Supplier<Object> s) {
            if (ref) {
                Objects.requireNonNull(s, "s is null");
                return empty.addAll(Collections.fill(n, s));
            }
            return new BitSet.Builder<>(m.from, m.to).fill(n, s);
        }
    }

    static BitSet<Object> emptyOf(Mapping m, boolean ref) {
        return ref ? new RefBitSetModule.RefBitSet1<>(m.from, m.to, 0L)
                : new BitSet.Builder<>(m.from, m.to).empty();
    }

    /** Sets share the plan's word arrays: no set ever writes the array it was built on. */
    static BitSet<Object> make(BitSet<Object> empty, long[] words) {
        if (empty instanceof BitSetModule.AbstractBitSet<Object> a) {
            return a.fromBitMaskNoCopy(words);
        }
        return ((RefBitSetModule.RefAbstractBitSet<Object>) empty).fromBitMaskNoCopy(words);
    }

    /** Argument sources; every call builds a fresh instance, so lazy sources log per side. */
    enum ArgKind {
        SAME(s -> s.arg), SELF(s -> s.recv), OTHER(s -> s.other), COUNTED_SET(s -> s.counted),
        JLIST(s -> new ArrayList<>(s.plan.order)), VLIST(s -> List.ofAll(s.plan.order)),
        VARRAY(s -> Array.ofAll(s.plan.order)), VECTOR(s -> Vector.ofAll(s.plan.order)),
        HASHSET(s -> HashSet.ofAll(s.plan.order)), LOGGED(s -> logged(s.plan.order)),
        VITER(s -> Iterator.ofAll(logged(s.plan.order).iterator())),
        LAZY(s -> lazy(s.plan.order, 0)), VIEW(s -> lazy(s.plan.order, 0).asJava()),
        SPECIAL(s -> logged(s.plan.special)), SPECIAL_LIST(s -> List.ofAll(s.plan.special)),
        SPECIAL_LAZY(s -> lazy(s.plan.special, 0)),
        SPECIAL_VIEW(s -> lazy(s.plan.special, 0).asJava()),
        EMPTY(s -> java.util.List.of()), NULL(s -> null);

        final Function<Side, Iterable<Object>> make;

        ArgKind(Function<Side, Iterable<Object>> make) {
            this.make = make;
        }
    }

    static Iterable<Object> logged(java.util.List<Object> items) {
        return () -> {
            LOG.append("it ");
            return new LogIterator(items);
        };
    }

    /** A plain java.util.Iterator that logs every hasNext and next call. */
    static final class LogIterator implements java.util.Iterator<Object> {
        private final java.util.List<Object> items;
        private int at;

        LogIterator(java.util.List<Object> items) {
            this.items = items;
        }

        @Override
        public boolean hasNext() {
            LOG.append("h ");
            return at < items.size();
        }

        @Override
        public Object next() {
            LOG.append("n ");
            return items.get(at++);
        }
    }

    static Stream<Object> lazy(java.util.List<Object> items, int from) {
        if (from == items.size()) {
            return Stream.empty();
        }
        return Stream.cons(items.get(from), () -> {
            LOG.append('s').append(from + 1).append(' ');
            return lazy(items, from + 1);
        });
    }

    static Predicate<Object> pred(Side s) {
        int[] calls = {0};
        return x -> {
            LOG.append("p ");
            if (++calls[0] == s.plan.j) {
                throw new IllegalStateException("pred " + calls[0]);
            }
            return Math.floorMod(Objects.hashCode(x), s.plan.mod) != 0;
        };
    }

    static Function<Object, Integer> classifier(Side s) {
        return x -> {
            LOG.append("c ");
            return Math.floorMod(Objects.hashCode(x), s.plan.mod);
        };
    }

    static Supplier<Object> supplier(java.util.List<Object> items) {
        int[] at = {0};
        return () -> {
            LOG.append("g ");
            return items.get(at[0]++);
        };
    }
}
