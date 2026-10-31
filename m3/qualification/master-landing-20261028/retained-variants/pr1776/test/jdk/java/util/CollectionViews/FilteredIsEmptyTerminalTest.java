/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
/*
 * @test
 * @summary M3JDK21 filtered isEmpty retains iterator/predicate tape, short circuit and backed mutation contract
 * @run main FilteredIsEmptyTerminalTest
 */
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.Predicate;

public final class FilteredIsEmptyTerminalTest {
    private static long checks;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label + "; check=" + checks);
    }

    /** Records source traversal without inspecting iterator implementation classes. */
    private static final class Traced<E> extends AbstractCollection<E> {
        final ArrayList<E> entries = new ArrayList<>();
        final ArrayList<String> tape = new ArrayList<>();
        int sizeCalls;

        @Override public int size() {
            sizeCalls++;
            return entries.size();
        }

        @Override public Iterator<E> iterator() {
            tape.add("open");
            Iterator<E> delegate = entries.iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() {
                    boolean answer = delegate.hasNext();
                    tape.add("has:" + answer);
                    return answer;
                }
                @Override public E next() {
                    E answer = delegate.next();
                    tape.add("next");
                    return answer;
                }
            };
        }
    }

    private static void compare(Traced<Integer> source, Predicate<Integer> first,
            Predicate<Integer> second, String label) {
        // Both alternatives traverse precisely one live source cursor; nested
        // views preserve the original predicate order and short-circuit effects.
        Predicate<Integer> tracedFirst = value -> {
            source.tape.add("p1:" + value);
            return first.test(value);
        };
        Predicate<Integer> tracedSecond = value -> {
            source.tape.add("p2:" + value);
            return second.test(value);
        };
        Collection<Integer> outer = CollectionViews.filtered(source, tracedFirst);
        Collection<Integer> view = CollectionViews.filtered(outer, tracedSecond);

        source.tape.clear();
        source.sizeCalls = 0;
        boolean reference = !view.iterator().hasNext();
        List<String> referenceTape = List.copyOf(source.tape);
        check(source.sizeCalls == 0, label + " reference size prepass");

        source.tape.clear();
        source.sizeCalls = 0;
        boolean actual = view.isEmpty();
        check(source.sizeCalls == 0, label + " terminal size prepass");
        check(actual == reference, label + " wrong terminal result");
        check(referenceTape.equals(source.tape), label + " source/callback tape drift");
    }

    private static void randomized() {
        Random random = new Random(0x4d33454d50545931L);
        for (int trial = 0; trial < 2400; trial++) {
            Traced<Integer> source = new Traced<>();
            int n = random.nextInt(96);
            for (int i = 0; i < n; i++) source.entries.add(random.nextInt(28) - 14);
            int modulus = 2 + random.nextInt(6);
            int low = random.nextInt(13) - 6;
            compare(source, value -> value != null && (value & 1) == 0,
                    value -> value >= low && Math.floorMod(value, modulus) != 1,
                    "random-" + trial);
            if ((trial & 7) == 0) {
                source.entries.add(low);
                compare(source, value -> value != null && (value & 1) == 0,
                        value -> value >= low && Math.floorMod(value, modulus) != 1,
                        "live-add-" + trial);
            }
        }
    }

    private static void exceptionsAndUnicode() {
        Traced<Integer> values = new Traced<>();
        values.entries.addAll(List.of(1, 2, 3));
        IllegalStateException sameError = new IllegalStateException("predicate failed");
        Collection<Integer> failing = CollectionViews.filtered(values, v -> {
            if (v == 2) throw sameError;
            return false;
        });
        for (int i = 0; i < 2; i++) {
            try {
                if (i == 0) failing.iterator().hasNext();
                else failing.isEmpty();
                throw new AssertionError("missing predicate exception");
            } catch (IllegalStateException thrown) {
                check(thrown == sameError, "exception identity");
            }
        }
        NavigableSet<String> words = new TreeSet<>(List.of(
                "class Fake { return true; }", "(?<code>.*)", "a\uD83D\uDE00z"));
        Collection<String> selected = CollectionViews.filtered(words,
                text -> text.contains("class") || text.contains("(?<"));
        check(!selected.isEmpty(), "sorted code-like strings must be accepted");
        check(Objects.equals(selected.iterator().next(), "(?<code>.*)"), "sorted encounter order");
        words.clear();
        check(selected.isEmpty(), "sorted backed clear must be visible");
    }

    public static void main(String[] ignored) {
        randomized();
        exceptionsAndUnicode();
        System.out.println("PASS FilteredIsEmptyTerminalTest checks=" + checks);
    }
}
