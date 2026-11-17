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
package jdk.m3.collections.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import jdk.m3.collections.apache.CollectionUtils;

/**
 * Element types, probes, inputs and lockstep fact recorders for CollectionUtilsBulkOracleTest.
 * Every check runs the pre-patch reference and the patched CollectionUtils on equal inputs.
 */
final class CollectionUtilsBulkFixtures {

    static final int INTS = 0;
    static final int KEYS = 1;
    static final int NO_HASH = 2;
    static final int COLLIDE = 3;
    static final int COLLIDE_CMP = 4;
    static final int ASYM = 5;
    static final int MIXED = 6;
    static final int THROW_HASH = 7;
    static final int MODES = 8;
    /** Element modes whose hashCode agrees with equals; the patched budget holds for them. */
    static final int[] CONSISTENT = {INTS, KEYS, COLLIDE, MIXED};
    static final int F_ITER2 = 1;
    static final int F_NEXT2 = 2;
    static final int F_ITER1 = 3;
    static final int F_BOTH = 4;
    static final int F_NEXT1 = 5;
    static final int EXACT_ARRAY_LIST = 0;
    static final int RECLOG = 1;
    static final int RECLOG_VETO = 2;
    private static final int DEQUE = 4;
    private static final int TREE = 6;
    private static final int INPUT_KINDS = 11;
    private static final int DELEGATE_KINDS = 7;
    /** Delegate kinds that never hash their elements, for ThrowHash cases. */
    private static final int[] PLAIN_KINDS = {0, 1, 2, 5, 6};
    private static final int UNIVERSE = 12;
    private static final int WIDE_UNIVERSE = 32;
    private static final int CMP_CLASSES = 16;
    private static final int MAX_SIZE = 40;
    private static final int[][] OVERFLOW = {
        {Integer.MAX_VALUE, 1}, {Integer.MAX_VALUE - 1, 2}, {Integer.MAX_VALUE - 2, 3}};

    private CollectionUtilsBulkFixtures() {
    }

    /** An element with an id; subclasses choose equals and hashCode. */
    abstract static class Elem {
        final int id;

        Elem(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + id;
        }
    }

    /** Equal and hashed by id. */
    static final class Key extends Elem {
        Key(int id) {
            super(id);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && k.id == id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    /** Equal by id with the identity hashCode: breaks the hashCode contract. */
    @SuppressWarnings("overrides")
    static final class NoHash extends Elem {
        NoHash(int id) {
            super(id);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof NoHash n && n.id == id;
        }
    }

    /** Equal by id, one constant hash, not Comparable: many of them make tree bins. */
    static final class Collide extends Elem {
        Collide(int id) {
            super(id);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Collide c && c.id == id;
        }

        @Override
        public int hashCode() {
            return 7;
        }
    }

    /** Constant hash, equal by id modulo 16, compared by id: compareTo disagrees with equals. */
    static final class CollideCmp extends Elem implements Comparable<CollideCmp> {
        CollideCmp(int id) {
            super(id);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CollideCmp c && c.id % CMP_CLASSES == id % CMP_CLASSES;
        }

        @Override
        public int hashCode() {
            return 7;
        }

        @Override
        public int compareTo(CollideCmp o) {
            return Integer.compare(id, o.id);
        }
    }

    /** Equals the Integer with its id, while Integer.equals(Asym) is false. */
    static final class Asym extends Elem {
        Asym(int id) {
            super(id);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Integer i ? i == id : o instanceof Asym a && a.id == id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    /** Equal by id; hashCode throws IllegalStateException("h" + id). */
    static final class ThrowHash extends Elem {
        ThrowHash(int id) {
            super(id);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ThrowHash t && t.id == id;
        }

        @Override
        public int hashCode() {
            throw new IllegalStateException("h" + id);
        }
    }

    /** ArrayList subclass logging remove(int) indexes; the veto variant refuses the 2nd one. */
    static final class RecLog<E> extends ArrayList<E> {
        private static final long serialVersionUID = 1L;
        final ArrayList<Integer> removed = new ArrayList<>();
        private final boolean veto;

        RecLog(Collection<E> xs, boolean veto) {
            super(xs);
            this.veto = veto;
        }

        @Override
        public E remove(int index) {
            removed.add(index);
            if (veto && removed.size() == 2) {
                throw new UnsupportedOperationException("veto");
            }
            return super.remove(index);
        }
    }

    /** Delegating collection that logs and counts iterator traffic and injects faults. */
    static final class Probe<E> extends AbstractCollection<E> {
        int iterators;
        int nexts;
        boolean failIterator;
        int failNext;
        private final Collection<E> delegate;
        private final List<String> log;
        private final String tag;
        private final String onIterator;
        private final String onHasNext;
        private final String onNext;
        private final String onIsEmpty;

        Probe(Collection<E> delegate, String tag, List<String> log) {
            this.delegate = delegate;
            this.log = log;
            this.tag = tag;
            this.onIterator = tag + ".iterator";
            this.onHasNext = tag + ".hasNext";
            this.onNext = tag + ".next";
            this.onIsEmpty = tag + ".isEmpty";
        }

        @Override
        public Iterator<E> iterator() {
            log.add(onIterator);
            iterators++;
            if (failIterator) {
                throw new IllegalStateException("it" + tag);
            }
            return new Cursor(delegate.iterator());
        }

        @Override
        public boolean isEmpty() {
            log.add(onIsEmpty);
            return delegate.isEmpty();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        private final class Cursor implements Iterator<E> {
            private final Iterator<E> it;

            Cursor(Iterator<E> it) {
                this.it = it;
            }

            @Override
            public boolean hasNext() {
                log.add(onHasNext);
                return it.hasNext();
            }

            @Override
            public E next() {
                log.add(onNext);
                if (++nexts == failNext) {
                    throw new IllegalStateException("next" + tag + "#" + nexts);
                }
                return it.next();
            }
        }
    }

    /** Compares by reference identity, so equal-but-distinct elements stay distinct. */
    record Id(Object ref) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Id other && other.ref == ref;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(ref);
        }
    }

    /** Which probe throws: a code F_* and, for a next() fault, the failing call number. */
    record Fault(int code, int k) {
        static final Fault NONE = new Fault(0, 0);
    }

    /** One containsAll case: element lists, delegate kinds and an injected fault. */
    record ContainsCase(int mode, List<Object> xs1, List<Object> xs2, int kind1, int kind2,
            Fault fault) {
        ContainsCase withoutFault() {
            return new ContainsCase(mode, xs1, xs2, kind1, kind2, Fault.NONE);
        }
    }

    /** What one containsAll call showed: result, coll2 traffic, order prefix and coll1 cost. */
    record ContainsRun(Object result, List<String> coll2Events, List<String> prefix,
            int coll1Iterators, int coll1Nexts) {
    }

    /** One removeCount (or removeRange when range) case on an input kind. */
    record RemoveCase(int kind, List<Object> xs, boolean range, int a, int b) {
        Collection<Object> run(boolean patched, Collection<Object> in) {
            if (range) {
                return patched ? CollectionUtils.removeRange(in, a, b)
                        : CollectionUtilsBulkReference.removeRange(in, a, b);
            }
            return patched ? CollectionUtils.removeCount(in, a, b)
                    : CollectionUtilsBulkReference.removeCount(in, a, b);
        }
    }

    static Object copy(Object e) {
        return switch (e) {
            case Key k -> new Key(k.id);
            case NoHash n -> new NoHash(n.id);
            case Collide c -> new Collide(c.id);
            case CollideCmp c -> new CollideCmp(c.id);
            case Asym a -> new Asym(a.id);
            case ThrowHash t -> new ThrowHash(t.id);
            case null, default -> e;
        };
    }

    static Object element(Random r, int mode) {
        int id = r.nextInt(UNIVERSE);
        int m = mode == MIXED || mode == THROW_HASH ? CONSISTENT[r.nextInt(3)] : mode;
        return switch (m) {
            case INTS -> r.nextInt(8) == 0 ? null : Integer.valueOf(id);
            case KEYS -> new Key(id);
            case NO_HASH -> new NoHash(id);
            case COLLIDE -> new Collide(id);
            case COLLIDE_CMP -> new CollideCmp(r.nextInt(WIDE_UNIVERSE));
            default -> r.nextBoolean() ? new Asym(id) : Integer.valueOf(id);
        };
    }

    static List<Object> elements(Random r, int mode, int size) {
        return IntStream.range(0, size).mapToObj(i -> element(r, mode)).toList();
    }

    /** A shuffled sub-multiset of xs, as the same instances or as equal copies. */
    static List<Object> subMultiset(Random r, List<Object> xs) {
        boolean copies = r.nextBoolean();
        List<Object> picked = new ArrayList<>(xs.stream()
                .filter(x -> r.nextInt(3) != 0)
                .map(x -> copies ? copy(x) : x)
                .toList());
        Collections.shuffle(picked, r);
        return picked;
    }

    static List<Object> withThrowHash(Random r, List<Object> xs) {
        List<Object> out = new ArrayList<>(xs);
        out.add(r.nextInt(out.size() + 1), new ThrowHash(r.nextInt(3)));
        return out;
    }

    static ContainsCase containsCase(Random r, int mode) {
        List<Object> xs1 = elements(r, mode, r.nextInt(MAX_SIZE + 1));
        List<Object> xs2 = r.nextInt(4) == 0 ? subMultiset(r, xs1)
                : elements(r, mode, r.nextInt(MAX_SIZE + 1));
        if (mode == THROW_HASH) {
            int where = r.nextInt(3);
            xs1 = where == 1 ? xs1 : withThrowHash(r, xs1);
            xs2 = where == 0 ? xs2 : withThrowHash(r, xs2);
        }
        int kind1 = delegateKind(r, mode);
        int kind2 = delegateKind(r, mode);
        return new ContainsCase(mode, xs1, xs2, kind1, kind2, fault(r, xs2.size()));
    }

    static int delegateKind(Random r, int mode) {
        return mode == THROW_HASH ? PLAIN_KINDS[r.nextInt(PLAIN_KINDS.length)]
                : r.nextInt(DELEGATE_KINDS);
    }

    static Fault fault(Random r, int size2) {
        return r.nextInt(8) != 0 ? Fault.NONE
                : new Fault(1 + r.nextInt(F_BOTH), 1 + r.nextInt(size2 + 1));
    }

    static Collection<Object> delegate(int kind, List<Object> xs) {
        return switch (kind) {
            case 0 -> new ArrayList<>(xs);
            case 1 -> new LinkedList<>(xs);
            case 2 -> xs.contains(null) ? new ArrayList<>(xs) : new ArrayDeque<>(xs);
            case 3 -> new HashSet<>(xs);
            case 4 -> new LinkedHashSet<>(xs);
            case 5 -> Arrays.asList(xs.toArray());
            default -> Collections.unmodifiableCollection(new ArrayList<>(xs));
        };
    }

    static Object attempt(Supplier<?> action) {
        try {
            return action.get();
        } catch (RuntimeException e) {
            return describe(e);
        }
    }

    static String describe(Exception e) {
        return "ex:" + e.getClass().getName() + ":" + e.getMessage();
    }

    static void assertIdentical(Object[] expected, Object[] actual) {
        assertEquals(Arrays.stream(expected).map(Id::new).toList(),
                Arrays.stream(actual).map(Id::new).toList());
    }

    static void arm(Probe<Object> p1, Probe<Object> p2, Fault f) {
        p1.failIterator = f.code() == F_ITER1 || f.code() == F_BOTH;
        p2.failIterator = f.code() == F_ITER2 || f.code() == F_BOTH;
        p1.failNext = f.code() == F_NEXT1 ? f.k() : 0;
        p2.failNext = f.code() == F_NEXT2 ? f.k() : 0;
    }

    static ContainsRun runContains(BiPredicate<Collection<?>, Collection<?>> impl,
            Collection<Object> d1, Collection<Object> d2, Fault fault) {
        List<String> log = new ArrayList<>();
        Probe<Object> p1 = new Probe<>(d1, "1", log);
        Probe<Object> p2 = new Probe<>(d2, "2", log);
        arm(p1, p2, fault);
        Object[] before1 = d1.toArray();
        Object[] before2 = d2.toArray();
        Object result = attempt(() -> impl.test(p1, p2));
        assertIdentical(before1, d1.toArray());
        assertIdentical(before2, d2.toArray());
        int first = log.indexOf("1.iterator");
        return new ContainsRun(result, log.stream().filter(e -> e.startsWith("2.")).toList(),
                new ArrayList<>(first < 0 ? log : log.subList(0, first + 1)),
                p1.iterators, p1.nexts);
    }

    static boolean brute(Collection<?> coll1, Collection<?> coll2) {
        return coll2.stream().allMatch(x -> coll1.stream().anyMatch(p -> Objects.equals(x, p)));
    }

    /** Lockstep: same result or exception, coll2 traffic and order up to 1.iterator. */
    static void checkContains(ContainsCase c, String at) {
        Collection<Object> d1 = delegate(c.kind1(), c.xs1());
        Collection<Object> d2 = delegate(c.kind2(), c.xs2());
        ContainsRun ref = runContains(CollectionUtilsBulkReference::containsAll, d1, d2,
                c.fault());
        ContainsRun pat = runContains(CollectionUtils::containsAll, d1, d2, c.fault());
        Supplier<String> where = () -> at + " " + c;
        assertEquals(ref.result(), pat.result(), where);
        assertEquals(ref.coll2Events(), pat.coll2Events(), where);
        assertEquals(ref.prefix(), pat.prefix(), where);
        if (pat.result() instanceof Boolean b) {
            assertEquals(brute(d1, d2), b, where);
        }
    }

    /** Patched-only cost: one coll1 iterator on true, at most two, at most 2n next calls. */
    static void checkBudget(ContainsCase c, String at) {
        Collection<Object> d1 = delegate(c.kind1(), c.xs1());
        Collection<Object> d2 = delegate(c.kind2(), c.xs2());
        ContainsRun pat = runContains(CollectionUtils::containsAll, d1, d2, Fault.NONE);
        Supplier<String> where = () -> at + " " + c + " " + pat;
        assertTrue(pat.coll1Iterators() <= 2, where);
        assertTrue(pat.coll1Nexts() <= 2 * d1.size(), where);
        if (Boolean.TRUE.equals(pat.result()) && !d2.isEmpty()) {
            assertEquals(1, pat.coll1Iterators(), where);
        }
    }

    static void checkSameInstance(ContainsCase c, String at) {
        Collection<Object> d = delegate(c.kind1(), c.xs1());
        Object ref = attempt(() -> CollectionUtilsBulkReference.containsAll(d, d));
        Object pat = attempt(() -> CollectionUtils.containsAll(d, d));
        assertEquals(ref, pat, at);
        assertEquals(Boolean.TRUE, pat, at);
    }

    static void checkTargeted(List<Object> xs1, List<Object> xs2, Fault fault, Object expected) {
        ContainsCase c = new ContainsCase(MIXED, xs1, xs2, 0, 0, fault);
        checkContains(c, "targeted");
        ContainsRun pat = runContains(CollectionUtils::containsAll, new ArrayList<>(xs1),
                new ArrayList<>(xs2), fault);
        assertEquals(expected, pat.result(), c::toString);
    }

    static RemoveCase removeCase(Random r, boolean range) {
        int kind = r.nextInt(3) == 0 ? EXACT_ARRAY_LIST : r.nextInt(INPUT_KINDS);
        List<Object> xs = IntStream.range(0, r.nextInt(MAX_SIZE + 1))
                .mapToObj(i -> removeElement(r, kind)).toList();
        int size = input(kind, xs).size();
        if (!range && r.nextInt(50) == 0) {
            int[] pair = OVERFLOW[r.nextInt(OVERFLOW.length)];
            return new RemoveCase(kind, xs, false, pair[0], pair[1]);
        }
        return new RemoveCase(kind, xs, range, r.nextInt(size + 5) - 2, r.nextInt(size + 5) - 2);
    }

    static Object removeElement(Random r, int kind) {
        int pick = kind == TREE ? 0 : r.nextInt(kind == DEQUE ? 2 : 3);
        return switch (pick) {
            case 0 -> Integer.valueOf(r.nextInt(6));
            case 1 -> new String("s" + r.nextInt(4));
            default -> null;
        };
    }

    static Collection<Object> input(int kind, List<Object> xs) {
        return switch (kind) {
            case EXACT_ARRAY_LIST -> new ArrayList<>(xs);
            case RECLOG -> new RecLog<>(xs, false);
            case RECLOG_VETO -> new RecLog<>(xs, true);
            case 3 -> new LinkedList<>(xs);
            case DEQUE -> new ArrayDeque<>(xs);
            case 5 -> new LinkedHashSet<>(xs);
            case TREE -> new TreeSet<>(xs);
            case 7 -> new CopyOnWriteArrayList<>(xs);
            case 8 -> Arrays.asList(xs.toArray());
            case 9 -> Collections.unmodifiableList(new ArrayList<>(xs));
            default -> Collections.synchronizedList(new ArrayList<>(xs));
        };
    }

    static String serialized(Object o) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(o);
        } catch (IOException e) {
            return describe(e);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    static Supplier<Object> viewOf(Collection<Object> input) {
        if (input instanceof List<Object> list) {
            List<Object> view = list.subList(0, list.size());
            return view::size;
        }
        return () -> "no-view";
    }

    static List<Object> resultFacts(Collection<Object> result) {
        List<Object> facts = new ArrayList<>();
        facts.add(result.getClass().getName());
        facts.add(result.stream().map(Id::new).toList());
        facts.add(serialized(result));
        return facts;
    }

    /** Result, input contents, outstanding iterator and view, subclass log, serial bytes. */
    static List<Object> removeFacts(Collection<Object> input,
            Function<Collection<Object>, Collection<Object>> op) {
        Iterator<Object> pre = input.iterator();
        Supplier<Object> view = viewOf(input);
        List<Object> facts = new ArrayList<>();
        facts.add(attempt(() -> resultFacts(op.apply(input))));
        facts.add(Arrays.stream(input.toArray()).map(Id::new).toList());
        facts.add(attempt(() -> new Id(pre.next())));
        facts.add(attempt(view));
        facts.add(input instanceof RecLog<?> rec ? rec.removed.toString() : "no-log");
        facts.add(serialized(input));
        return facts;
    }

    static void checkRemove(RemoveCase c, String at) {
        List<Object> ref = removeFacts(input(c.kind(), c.xs()), in -> c.run(false, in));
        List<Object> pat = removeFacts(input(c.kind(), c.xs()), in -> c.run(true, in));
        assertEquals(ref, pat, () -> at + " " + c);
    }
}
