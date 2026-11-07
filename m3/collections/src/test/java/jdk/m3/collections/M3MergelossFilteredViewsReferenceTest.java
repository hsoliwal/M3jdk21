/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
//FLLDCIM: MRL - mergeloss2 leaves 42+27+43: reference model of the restored members; recipe qualification/mergeloss2-filtered-views-20261107
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.M3FilteredLists;

/**
 * Reference model of the M3FilteredViews members restored by qualification/mergeloss2-filtered-views-20261107.
 * Leaf 42: descendingIterator() and last() of private filter chains over java.util.TreeMap key views
 * (navigableKeySet, subMap, headMap, tailMap, descendingMap, descendingKeySet) and the matching M3TreeSet views
 * equal the TreeMap view's own descending order with the chain's predicates applied inner to outer, short-circuit
 * callback tape included, at shallow and at 100000-layer depth. Leaf 27: contains() of the generic Collection/Set
 * filters over comparator-ordered sets equals the backing's membership plus every predicate on the stored
 * representative (the predicate sees the stored reference). Leaf 43: a filtered ListIterator over a provider whose
 * iterators refuse both ends without a generation check refuses a write after its delivered element disappeared
 * (ConcurrentModificationException, no write).
 */
final class M3MergelossFilteredViewsReferenceTest {
    private static final int SCENARIOS = 400;
    private static final long SEED = 0x4D4C3246564CL;
    private static final int DEEP = 100_000;

    /** Shape of one key view: case, endpoints in the view's own order, inclusivity. */
    private record Shape(int kind, Integer from, boolean fromInclusive, Integer to, boolean toInclusive) {
        static Shape draw(Random random, boolean reversed) {
            int a = random.nextInt(41) - 20;
            int b = random.nextInt(41) - 20;
            int low = Math.min(a, b);
            int high = Math.max(a, b);
            return new Shape(random.nextInt(7), reversed ? high : low, random.nextBoolean(),
                    reversed ? low : high, random.nextBoolean());
        }

        NavigableSet<Integer> of(NavigableMap<Integer, Integer> map) {
            return switch (kind) {
                case 0 -> map.navigableKeySet();
                case 1 -> map.subMap(from, fromInclusive, to, toInclusive).navigableKeySet();
                case 2 -> map.headMap(to, toInclusive).navigableKeySet();
                case 3 -> map.tailMap(from, fromInclusive).navigableKeySet();
                case 4 -> map.descendingMap().navigableKeySet();
                case 5 -> map.descendingKeySet();
                default -> map.descendingMap().subMap(to, toInclusive, from, fromInclusive).navigableKeySet();
            };
        }

        NavigableSet<Integer> of(NavigableSet<Integer> set) {
            return switch (kind) {
                case 0 -> set;
                case 1 -> set.subSet(from, fromInclusive, to, toInclusive);
                case 2 -> set.headSet(to, toInclusive);
                case 3 -> set.tailSet(from, fromInclusive);
                case 4, 5 -> set.descendingSet();
                default -> set.descendingSet().subSet(to, toInclusive, from, fromInclusive);
            };
        }
    }

    private static boolean accepts(int layer, int salt, Integer value) {
        return ((value * (layer + 3) + salt) & 3) != 0;
    }

    private static NavigableSet<Integer> chain(NavigableSet<Integer> source, int depth, int[] salts,
            List<String> tape) {
        NavigableSet<Integer> view = source;
        for (int layer = 0; layer < depth; layer++) {
            int index = layer;
            view = M3FilteredViews.filter(view, value -> {
                tape.add(index + ":" + value);
                return accepts(index, salts[index], value);
            });
        }
        return view;
    }

    /** Reference: the TreeMap view's descending order, predicates inner to outer, first rejection stops. */
    private static List<Integer> reference(NavigableSet<Integer> keys, int depth, int[] salts, List<String> tape,
            int[] lastTape) {
        List<Integer> out = new ArrayList<>();
        lastTape[0] = -1;
        for (Iterator<Integer> cursor = keys.descendingIterator(); cursor.hasNext();) {
            Integer value = cursor.next();
            boolean visible = true;
            for (int layer = 0; layer < depth && visible; layer++) {
                tape.add(layer + ":" + value);
                visible = accepts(layer, salts[layer], value);
            }
            if (visible) {
                if (out.isEmpty()) lastTape[0] = tape.size();
                out.add(value);
            }
        }
        if (lastTape[0] < 0) lastTape[0] = tape.size();
        return out;
    }

    private static List<Integer> drain(Iterator<Integer> cursor) {
        List<Integer> out = new ArrayList<>();
        while (cursor.hasNext()) out.add(cursor.next());
        assertThrows(NoSuchElementException.class, cursor::next);
        assertThrows(UnsupportedOperationException.class, cursor::remove);
        return out;
    }

    private static void compare(NavigableSet<Integer> source, NavigableSet<Integer> keys, int depth, int[] salts,
            String label) {
        List<String> tape = new ArrayList<>();
        List<String> wanted = new ArrayList<>();
        int[] lastTape = new int[1];
        List<Integer> expected = reference(keys, depth, salts, wanted, lastTape);
        NavigableSet<Integer> view = chain(source, depth, salts, tape);
        assertEquals(expected, drain(view.descendingIterator()), label + " descending order");
        assertEquals(wanted, tape, label + " descending callback tape");
        tape.clear();
        if (expected.isEmpty()) {
            assertThrows(NoSuchElementException.class, view::last, label + " empty last");
        } else {
            assertEquals(expected.get(0), view.last(), label + " last");
        }
        assertEquals(wanted.subList(0, lastTape[0]), tape, label + " last callback tape");
    }

    @Test
    void shallowChainsMatchTreeMapViews() {
        Random random = new Random(SEED);
        for (int scenario = 0; scenario < SCENARIOS; scenario++) {
            Comparator<Integer> order = random.nextBoolean() ? null : Comparator.reverseOrder();
            TreeMap<Integer, Integer> map = new TreeMap<>(order);
            M3TreeSet<Integer> m3 = new M3TreeSet<>(order);
            int size = random.nextInt(24);
            for (int i = 0; i < size; i++) {
                int key = random.nextInt(41) - 20;
                map.put(key, key);
                m3.add(key);
            }
            Shape shape = Shape.draw(random, order != null);
            int depth = 1 + random.nextInt(5);
            int[] salts = random.ints(depth, 0, 4).toArray();
            NavigableSet<Integer> keys = shape.of(map);
            String label = "scenario " + scenario + " " + shape + " depth " + depth;
            compare(keys, keys, depth, salts, label + " TreeMap");
            compare(shape.of(m3), keys, depth, salts, label + " M3TreeSet");
        }
    }

    @Test
    void deepChainsMatchTreeMapViews() {
        TreeMap<Integer, Integer> map = new TreeMap<>();
        for (int key = -4; key <= 4; key++) map.put(key, key);
        int[] calls = new int[1];
        NavigableSet<Integer> view = map.subMap(-3, true, 3, true).navigableKeySet();
        for (int layer = 0; layer < DEEP; layer++) {
            int index = layer;
            view = M3FilteredViews.filter(view, value -> {
                calls[0]++;
                return index != DEEP / 2 || value != 0;
            });
        }
        assertEquals(List.of(3, 2, 1, -1, -2, -3), drain(view.descendingIterator()), "deep descending order");
        assertEquals(6 * DEEP + DEEP / 2 + 1, calls[0], "deep predicate calls, rejection at the middle layer");
        calls[0] = 0;
        assertEquals(3, view.last(), "deep last");
        assertEquals(DEEP, calls[0], "deep last stops at the first visible member");
    }

    @Test
    void genericFilterMembershipFollowsTheStoredRepresentative() {
        Random random = new Random(SEED ^ 27);
        List<String> words = List.of("Alpha", "beta", "GAMMA", "Delta", "epsilon", "Zeta", "eta", "THETA");
        for (int scenario = 0; scenario < SCENARIOS; scenario++) {
            TreeSet<String> tree = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            M3TreeSet<String> m3 = new M3TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (String word : words) {
                if (random.nextInt(4) != 0) {
                    String stored = new String(word);
                    tree.add(stored);
                    m3.add(stored);
                }
            }
            int kind = random.nextInt(4);
            NavigableSet<String> reference = sortedView(tree, kind);
            NavigableSet<String> backing = random.nextBoolean() ? reference : sortedView(m3, kind);
            List<String> seen = new ArrayList<>();
            Predicate<String> inner = value -> { seen.add(value); return Character.isUpperCase(value.charAt(0)); };
            Predicate<String> outer = value -> { seen.add(value); return value.length() > 3; };
            Collection<String> asCollection = backing;
            Set<String> asSet = backing;
            int layers = random.nextInt(3);
            Collection<String> view = switch (layers) {
                case 0 -> M3FilteredViews.filter(asCollection, inner);
                case 1 -> M3FilteredViews.filter(asSet, inner);
                default -> M3FilteredViews.filter(M3FilteredViews.filter(asSet, inner), outer);
            };
            for (String word : words) {
                String probe = random.nextBoolean() ? word.toUpperCase() : word.toLowerCase();
                seen.clear();
                boolean present = reference.contains(probe);
                String stored = present ? reference.ceiling(probe) : null;
                boolean expected = present && Character.isUpperCase(stored.charAt(0))
                        && (layers < 2 || stored.length() > 3);
                assertEquals(expected, view.contains(probe), "scenario " + scenario + " kind " + kind
                        + " layers " + layers + " probe " + probe);
                for (String argument : seen) assertSame(stored, argument, "predicate sees the stored reference");
            }
        }
    }

    /** A provider without structural generation: its iterators refuse both ends by size alone. */
    private static final class PlainList extends AbstractList<Integer> {
        private final List<Integer> items = new ArrayList<>(List.of(1, 2, 3, 4));
        @Override public Integer get(int index) { return items.get(index); }
        @Override public int size() { return items.size(); }
        @Override public Integer set(int index, Integer value) { return items.set(index, value); }
        @Override public void add(int index, Integer value) { items.add(index, value); }
        @Override public Integer remove(int index) { return items.remove(index); }
    }

    @Test
    void clearedProviderWithoutGenerationRefusesDeliveredWrites() {
        for (boolean reverse : List.of(false, true)) {
            for (int operation = 0; operation < 3; operation++) {
                PlainList owner = new PlainList();
                ListIterator<Integer> cursor = M3FilteredLists.filtered(owner, value -> true)
                        .listIterator(reverse ? owner.size() : 0);
                assertEquals(reverse ? 4 : 1, reverse ? cursor.previous() : cursor.next(), "delivered");
                owner.items.clear();
                int selected = operation;
                assertThrows(ConcurrentModificationException.class, () -> {
                    switch (selected) {
                        case 0 -> cursor.remove();
                        case 1 -> cursor.set(10);
                        default -> cursor.add(10);
                    }
                }, "delivered element disappeared, operation " + operation + " reverse " + reverse);
                assertEquals(List.of(), owner.items, "no write after the refusal");
            }
        }
    }

    private static NavigableSet<String> sortedView(NavigableSet<String> set, int kind) {
        return switch (kind) {
            case 0 -> set;
            case 1 -> set.descendingSet();
            case 2 -> set.subSet("b", true, "x", false);
            default -> set.descendingSet().headSet("c", true);
        };
    }
}
