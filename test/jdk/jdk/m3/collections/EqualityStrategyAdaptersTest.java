/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary One equality strategy across the collection providers: Commons Equator and Guava
 *          Equivalence convert to and from Eclipse HashingStrategy
 * @modules jdk.m3.collections
 * @run main EqualityStrategyAdaptersTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import jdk.m3.collections.apache.CollectionUtils;
import jdk.m3.collections.apache.Equator;
import jdk.m3.collections.apache.functors.DefaultEquator;
import jdk.m3.collections.eclipse.api.block.HashingStrategy;
import jdk.m3.collections.eclipse.impl.block.factory.HashingStrategies;
import jdk.m3.collections.eclipse.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;
import jdk.m3.collections.guava.base.Equivalence;

public class EqualityStrategyAdaptersTest {

    /** Strings equal when they are equal ignoring case; serializable. */
    static final class CaseInsensitive implements Equator<String>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean equate(String a, String b) {
            return a == null ? b == null : b != null && a.equalsIgnoreCase(b);
        }

        @Override
        public int hash(String s) {
            return s == null ? 0 : s.toLowerCase(Locale.ROOT).hashCode();
        }
    }

    /** The same, not serializable. */
    static final class CaseInsensitiveLocal implements Equator<String> {
        @Override
        public boolean equate(String a, String b) {
            return a.equalsIgnoreCase(b);
        }

        @Override
        public int hash(String s) {
            return s.toLowerCase(Locale.ROOT).hashCode();
        }
    }

    static int checks;

    static void check(boolean condition, String what) {
        checks++;
        if (!condition) {
            throw new AssertionError(what);
        }
    }

    public static void main(String[] args) throws Exception {
        equatorAsStrategy();
        strategyAsEquator();
        equivalenceBothWays();
        roundTrips();
        nulls();
        serialization();
        valueEquality();
        variance();
        System.out.println("EqualityStrategyAdaptersTest: " + checks + " checks passed");
    }

    /** A Commons equator drives an Eclipse strategy set. */
    static void equatorAsStrategy() {
        HashingStrategy<String> s = new CaseInsensitive().asHashingStrategy();
        UnifiedSetWithHashingStrategy<String> set = new UnifiedSetWithHashingStrategy<>(s);
        set.addAll(List.of("Apple", "APPLE", "pear", "Pear", "fig"));
        check(set.size() == 3, "case-insensitive set size " + set.size());
        check(set.contains("aPPle") && set.contains("PEAR") && !set.contains("plum"), "lookups");
        check(s.equals("Fig", "fIG") && !s.equals("fig", "figs"), "equals delegates to equate");
        check(s.computeHashCode("ABC") == "abc".hashCode(), "computeHashCode delegates to hash");
    }

    /** An Eclipse strategy drives Commons CollectionUtils. */
    static void strategyAsEquator() {
        Equator<Object> identity =
                Equator.fromHashingStrategy(HashingStrategies.identityStrategy());
        String a = new String("x");
        String b = new String("x");
        check(!identity.equate(a, b) && identity.equate(a, a), "identity equate");
        check(identity.hash(a) == System.identityHashCode(a), "identity hash");
        List<Object> left = Arrays.asList(a, a);
        check(CollectionUtils.isEqualCollection(left, Arrays.asList(a, a), identity),
                "same elements");
        check(!CollectionUtils.isEqualCollection(left, Arrays.asList(a, b), identity),
                "equal, not same");
        check(CollectionUtils.isEqualCollection(left, Arrays.asList(a, b)),
                "Commons default: equal");
        Equator<String> byLength =
                Equator.fromHashingStrategy(HashingStrategies.fromFunction(String::length));
        check(byLength.equate("abc", "xyz") && !byLength.equate("ab", "abc"),
                "strategy from function");
    }

    /** Guava equivalences in both directions. */
    static void equivalenceBothWays() {
        HashingStrategy<Object> s = Equivalence.identity().asHashingStrategy();
        String a = new String("y");
        String b = new String("y");
        UnifiedSetWithHashingStrategy<Object> set = new UnifiedSetWithHashingStrategy<>(s);
        set.add(a);
        set.add(b);
        set.add(a);
        check(set.size() == 2, "identity strategy set size " + set.size());
        Equivalence<String> ci =
                Equivalence.fromHashingStrategy(new CaseInsensitive().asHashingStrategy());
        check(ci.equivalent("Kiwi", "KIWI") && !ci.equivalent("kiwi", "kiwis"), "equivalent");
        check(ci.hash("Kiwi") == "kiwi".hashCode(), "hash");
        check(ci.wrap("Lime").equals(ci.wrap("LIME")), "wrap uses the strategy");
        check(ci.pairwise().equivalent(List.of("a", "B"), List.of("A", "b")), "pairwise");
        check(ci.equivalentTo("plum").test("PLUM"), "equivalentTo");
    }

    /** Converting twice gives back the original object. */
    static void roundTrips() {
        Equator<String> e = new CaseInsensitive();
        check(Equator.fromHashingStrategy(e.asHashingStrategy()) == e, "equator round trip");
        HashingStrategy<Object> s = HashingStrategies.identityStrategy();
        check(Equator.fromHashingStrategy(s).asHashingStrategy() == s,
                "strategy round trip through Equator");
        Equivalence<Object> q = Equivalence.equals();
        check(Equivalence.fromHashingStrategy(q.asHashingStrategy()) == q,
                "equivalence round trip");
        check(Equivalence.fromHashingStrategy(s).asHashingStrategy() == s,
                "strategy round trip through Equivalence");
        check(Equivalence.fromHashingStrategy(s).equals(Equivalence.fromHashingStrategy(s)),
                "equal adapters of one strategy");
        try {
            Equator.fromHashingStrategy(null);
            check(false, "Equator.fromHashingStrategy(null) must throw");
        } catch (NullPointerException expected) {
            checks++;
        }
        try {
            Equivalence.fromHashingStrategy(null);
            check(false, "Equivalence.fromHashingStrategy(null) must throw");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    /** Nulls: the adapters pass them on; an Equivalence handles them itself. */
    static void nulls() {
        HashingStrategy<Object> fromDefault = DefaultEquator.defaultEquator().asHashingStrategy();
        check(fromDefault.equals(null, null) && !fromDefault.equals(null, "a"),
                "default equator nulls");
        check(fromDefault.computeHashCode(null) == DefaultEquator.HASHCODE_NULL,
                "default equator null hash");
        HashingStrategy<Object> fromEquals = Equivalence.equals().asHashingStrategy();
        check(fromEquals.equals(null, null) && fromEquals.computeHashCode(null) == 0,
                "equivalence nulls");
        Equivalence<String> overStrict =
                Equivalence.fromHashingStrategy(new CaseInsensitiveLocal().asHashingStrategy());
        check(overStrict.equivalent(null, null) && !overStrict.equivalent(null, "a"),
                "null never reaches");
        check(overStrict.hash(null) == 0, "null hash of an equivalence");
    }

    /** Serializable when what they wrap is; a strategy-made equivalence is serializable. */
    static void serialization() throws Exception {
        HashingStrategy<String> s = new CaseInsensitive().asHashingStrategy();
        HashingStrategy<String> back = copy(s);
        check(back.equals("Q", "q") && back.computeHashCode("Q") == "q".hashCode(),
                "strategy copy");
        Equivalence<String> q = Equivalence.fromHashingStrategy(s);
        Equivalence<String> qBack = copy(q);
        check(qBack.equivalent("Rr", "rR"), "equivalence copy");
        Equator<String> fromStrategy = Equator.fromHashingStrategy(s);
        check(copy(fromStrategy).equate("Zz", "zZ"), "an equator made from a strategy serializes");
        Equator<String> byLength =
                Equator.fromHashingStrategy(HashingStrategies.fromFunction(String::length));
        check(copy(byLength).equate("abc", "xyz"),
                "an equator over a strategy of Eclipse serializes");
        try {
            copy(new CaseInsensitiveLocal().asHashingStrategy());
            check(false, "a strategy over a non-serializable equator must not serialize");
        } catch (NotSerializableException expected) {
            checks++;
        }
    }

    /** Two conversions of one object are equal, so Guava wrappers made by them are equal. */
    static void valueEquality() {
        Equator<String> e = new CaseInsensitive();
        check(e.asHashingStrategy().equals(e.asHashingStrategy()), "two strategies of one equator");
        HashingStrategy<Object> id = HashingStrategies.identityStrategy();
        check(Equator.fromHashingStrategy(id).equals(Equator.fromHashingStrategy(id)),
                "two equators");
        check(Equivalence.equals().asHashingStrategy().equals(Equivalence.equals().asHashingStrategy()),
                "two strategies of one equivalence");
        Equivalence<String> first = Equivalence.fromHashingStrategy(e.asHashingStrategy());
        Equivalence<String> second = Equivalence.fromHashingStrategy(e.asHashingStrategy());
        check(first.equals(second) && first.hashCode() == second.hashCode(), "two equivalences");
        check(first.wrap("Fig").equals(second.wrap("FIG")), "wrappers across conversions");
    }

    /** A strategy of a supertype serves a typed equator or equivalence. */
    static void variance() {
        Equivalence<String> identity =
                Equivalence.fromHashingStrategy(HashingStrategies.identityStrategy());
        String a = new String("v");
        check(identity.equivalent(a, a) && !identity.equivalent(a, new String("v")), "identity");
        UnifiedSetWithHashingStrategy<String> set =
                new UnifiedSetWithHashingStrategy<>(new CaseInsensitive().asHashingStrategy());
        Equator<String> fromSet = Equator.fromHashingStrategy(set.hashingStrategy());
        check(fromSet.equate("Rose", "ROSE"), "the strategy of an existing set");
    }

    @SuppressWarnings("unchecked")
    static <T> T copy(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        ByteArrayInputStream input = new ByteArrayInputStream(bytes.toByteArray());
        try (ObjectInputStream in = new ObjectInputStream(input)) {
            return (T) in.readObject();
        }
    }
}
