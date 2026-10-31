/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */
/*
 * @test
 * @summary Integrated filtered/sorted collection contracts, masks, ranges and Java 21 views
 * @run main M3CollectionViewsTest
 */
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class M3CollectionViewsTest {
    private static int checks;
    private static final Predicate<Integer> EVEN = value -> value == null || (value & 1) == 0;
    public static void main(String[] args) {
        collections(); listIterators(); listRandomized(); sets(); maps(); deques();
        snapshots(); sortedLive(); boundedNavigation(); deepComposition(); failures();
        System.out.println("M3CollectionViewsTest checks=" + checks);
    }
    private static void eq(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError("expected=" + expected + " actual=" + actual);
    }
    private static void yes(boolean value) { eq(true, value); }
    private static void throwsType(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + type);
    }
    private static <E> List<E> filtered(Collection<E> source, Predicate<? super E> test) {
        ArrayList<E> values = new ArrayList<>();
        for (E value : source) if (test.test(value)) values.add(value);
        return values;
    }
    private static int physical(List<Integer> values, int logical) {
        int seen = 0;
        for (int i = 0; i < values.size(); i++)
            if (EVEN.test(values.get(i)) && seen++ == logical) return i;
        if (seen == logical) return values.size();
        throw new AssertionError();
    }
    private static void collections() {
        List<Integer> source = new ArrayList<>(Arrays.asList(1, 2, 3, 4, null, 5));
        Collection<Integer> view = Collections.filteredCollection(source, EVEN);
        eq(Arrays.asList(2, 4, null), new ArrayList<>(view));
        yes(view.contains(null));
        throwsType(IllegalArgumentException.class, () -> view.add(7));
        throwsType(IllegalArgumentException.class, () -> view.addAll(Arrays.asList(6, 7, 8)));
        eq(Arrays.asList(1, 2, 3, 4, null, 5), source);
        Iterator<Integer> iterator = view.iterator();
        eq(2, iterator.next()); yes(iterator.hasNext());
        throwsType(UnsupportedOperationException.class, iterator::remove);
        view.removeAll(Arrays.asList(2, 3));
        eq(Arrays.asList(1, 3, 4, null, 5), source);
        view.retainAll(Arrays.asList(4));
        eq(List.of(1, 3, 4, 5), source);
        view.clear(); eq(List.of(1, 3, 5), source);
        source.add(8); eq(List.of(8), new ArrayList<>(view));
        eq(List.of(8), view.parallelStream().toList());
        yes(!view.spliterator().hasCharacteristics(Spliterator.SIZED));
        throwsType(IllegalStateException.class, () -> view.spliterator().getComparator());
        AtomicInteger threshold = new AtomicInteger(3);
        Collection<Integer> changing = Collections.filteredCollection(source, x -> x >= threshold.get());
        eq(List.of(3, 5, 8), new ArrayList<>(changing));
        threshold.set(7); eq(List.of(8), new ArrayList<>(changing));
        source.set(0, 10); eq(List.of(10, 8), new ArrayList<>(changing));
        String[] actual = Collections.filteredCollection(List.of("a", "bb", "ccc"), x -> x.length() > 1).toArray(new String[4]);
        eq(Arrays.asList("bb", "ccc", null, null), Arrays.asList(actual));
    }
    private static void listIterators() {
        for (boolean linked : new boolean[] {false, true}) {
            List<Integer> source = linked ? new LinkedList<>() : new ArrayList<>();
            source.addAll(List.of(1, 2, 3, 4, 5, 6, 7));
            List<Integer> view = Collections.filteredList(source, EVEN);
            ListIterator<Integer> iterator = view.listIterator();
            eq(0, iterator.nextIndex()); yes(!iterator.hasPrevious());
            eq(2, iterator.next()); yes(iterator.hasNext()); yes(iterator.hasNext());
            iterator.remove(); eq(List.of(1, 3, 4, 5, 6, 7), source);
            eq(0, iterator.nextIndex());
            throwsType(IllegalStateException.class, iterator::remove);
            eq(4, iterator.next()); yes(iterator.hasPrevious()); yes(iterator.hasNext());
            iterator.set(8); eq(List.of(1, 3, 8, 5, 6, 7), source);
            eq(8, iterator.previous()); yes(iterator.hasPrevious() == false);
            iterator.remove(); eq(List.of(1, 3, 5, 6, 7), source);
            iterator.add(10); eq(List.of(1, 3, 5, 10, 6, 7), source);
            eq(1, iterator.nextIndex()); eq(6, iterator.next());
            yes(!iterator.hasNext()); yes(!iterator.hasNext()); iterator.set(12);
            eq(List.of(1, 3, 5, 10, 12, 7), source);
            iterator.remove(); eq(List.of(1, 3, 5, 10, 7), source);
            iterator.add(14); eq(List.of(1, 3, 5, 10, 7, 14), source);
            eq(14, iterator.previous()); eq(10, iterator.previous());
            throwsType(NoSuchElementException.class, iterator::previous);
            ListIterator<Integer> end = view.listIterator(view.size());
            yes(!end.hasNext()); eq(14, end.previous());
            yes(end.hasNext()); end.remove(); eq(List.of(1, 3, 5, 10, 7), source);
            List<Integer> slice = view.subList(0, 1); slice.set(0, 16);
            eq(List.of(1, 3, 5, 16, 7), source);
            view.reversed().addFirst(18); eq(List.of(1, 3, 5, 16, 7, 18), source);
            eq(List.of(18, 16), new ArrayList<>(view.reversed()));
            eq(view, view.reversed().reversed());
            view.sort(Comparator.reverseOrder()); eq(List.of(1, 3, 5, 18, 7, 16), source);
        }
    }
    private static void listRandomized() {
        for (boolean linked : new boolean[] {false, true}) {
            Random random = new Random(278163);
            List<Integer> source = linked ? new LinkedList<>() : new ArrayList<>();
            List<Integer> model = new ArrayList<>();
            List<Integer> view = Collections.filteredList(source, EVEN);
            for (int step = 0; step < 2500; step++) {
                int n = filtered(model, EVEN).size();
                int at = random.nextInt(n + 1);
                Integer value = random.nextInt(9) == 0 ? null : 2 * random.nextInt(12);
                switch (random.nextInt(8)) {
                    case 0 -> { int appended = random.nextInt(15); source.add(appended); model.add(appended); }
                    case 1 -> { int p = physical(model, at); view.add(at, value); model.add(p, value); }
                    case 2 -> { if (at < n) eq(model.set(physical(model, at), value), view.set(at, value)); }
                    case 3 -> { if (at < n) eq(model.remove(physical(model, at)), view.remove(at)); }
                    case 4 -> {
                        ListIterator<Integer> iterator = view.listIterator(at);
                        if (at < n) {
                            int p = physical(model, at); eq(model.get(p), iterator.next());
                            iterator.hasNext(); iterator.hasPrevious(); iterator.remove(); model.remove(p);
                        }
                    }
                    case 5 -> {
                        ListIterator<Integer> iterator = view.listIterator(at);
                        iterator.hasNext(); iterator.hasPrevious(); iterator.add(value);
                        model.add(physical(model, at), value);
                    }
                    case 6 -> {
                        List<Integer> reverse = view.reversed();
                        eq(filtered(model, EVEN).reversed(), new ArrayList<>(reverse));
                    }
                    case 7 -> {
                        if (at < n) {
                            int to = at + random.nextInt(n - at + 1);
                            eq(filtered(model, EVEN).subList(at, to), new ArrayList<>(view.subList(at, to)));
                        }
                    }
                }
                eq(model, source); eq(filtered(model, EVEN), new ArrayList<>(view));
            }
        }
    }
    private static void sets() {
        Set<Integer> source = new LinkedHashSet<>(Arrays.asList(1, 2, null, 3, 4));
        Set<Integer> view = Collections.filteredSet(source, EVEN);
        eq(new HashSet<>(Arrays.asList(2, null, 4)), view);
        eq(new HashSet<>(Arrays.asList(2, null, 4)).hashCode(), view.hashCode());
        view.remove(null); yes(!source.contains(null));
        Comparator<Integer> order = Comparator.nullsFirst(Comparator.naturalOrder());
        for (boolean descending : new boolean[] {false, true}) {
            NavigableSet<Integer> tree = new TreeSet<>(order);
            tree.addAll(Arrays.asList(null, -3, -2, -1, 0, 1, 2, 3, 4));
            NavigableSet<Integer> base = descending ? tree.descendingSet() : tree;
            NavigableSet<Integer> selected = Collections.filteredNavigableSet(base, EVEN);
            TreeSet<Integer> oracle = new TreeSet<>(base.comparator()); oracle.addAll(filtered(base, EVEN));
            eq(new ArrayList<>(oracle), new ArrayList<>(selected));
            eq(oracle.first(), selected.first()); eq(oracle.last(), selected.last());
            for (Integer key : Arrays.asList(null, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5)) {
                eq(oracle.lower(key), selected.lower(key)); eq(oracle.floor(key), selected.floor(key));
                eq(oracle.ceiling(key), selected.ceiling(key)); eq(oracle.higher(key), selected.higher(key));
            }
            eq(new ArrayList<>(oracle.descendingSet()), new ArrayList<>(selected.descendingSet()));
            eq(new ArrayList<>(selected), new ArrayList<>(selected.reversed().reversed()));
            eq(base.spliterator().hasCharacteristics(Spliterator.SORTED),
                    selected.spliterator().hasCharacteristics(Spliterator.SORTED));
            if (base.spliterator().hasCharacteristics(Spliterator.SORTED))
                eq(base.comparator(), selected.spliterator().getComparator());
            else throwsType(IllegalStateException.class, () -> selected.spliterator().getComparator());
            int before = tree.size(); eq(oracle.pollFirst(), selected.pollFirst()); eq(before - 1, tree.size());
            eq(oracle.pollLast(), selected.pollLast());
            eq(new ArrayList<>(oracle), new ArrayList<>(selected));
        }
        NavigableSet<Integer> bounds = new TreeSet<>(List.of(0, 1, 2, 3, 4, 5, 6));
        for (boolean lo : new boolean[] {false, true}) for (boolean hi : new boolean[] {false, true}) {
            NavigableSet<Integer> selected = Collections.filteredNavigableSet(bounds, EVEN).subSet(2, lo, 4, hi);
            eq(filtered(bounds.subSet(2, lo, 4, hi), EVEN), new ArrayList<>(selected));
        }
        NavigableSet<Integer> child = Collections.filteredNavigableSet(bounds.subSet(1, false, 5, false), EVEN);
        throwsType(IllegalArgumentException.class, () -> child.add(6));
        throwsType(IllegalArgumentException.class, () -> child.headSet(6, true));
        TreeSet<String> words = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        words.addAll(List.of("ALPHA", "bravo", "CHARLIE"));
        SortedSet<String> upper = Collections.filteredSortedSet(words, x -> Character.isUpperCase(x.charAt(0)));
        yes(upper.contains("alpha")); yes(upper.remove("alpha")); yes(!words.contains("ALPHA"));
        eq("CHARLIE", upper.first()); eq("CHARLIE", upper.last());
        throwsType(IllegalArgumentException.class, () -> upper.add("BRAVO"));
        throwsType(IllegalArgumentException.class, () -> upper.addAll(List.of("ECHO", "BRAVO")));
        yes(!words.contains("ECHO"));
        String hidden = new String("same"), eligible = new String("same");
        Set<String> collisionSource = new HashSet<>(List.of(hidden));
        Set<String> collisionView = Collections.filteredSet(collisionSource, value -> value == eligible);
        throwsType(IllegalArgumentException.class, () -> collisionView.add(eligible));
        yes(collisionView.isEmpty()); eq(Set.of(hidden), collisionSource);
    }
    private static void maps() {
        LinkedHashMap<String,Integer> source = new LinkedHashMap<>();
        source.put("a", 1); source.put("b", 2); source.put("c", null); source.put("d", 4);
        Map<String,Integer> view = Collections.filteredMap(source, (k,v) -> EVEN.test(v));
        eq(List.of("b", "c", "d"), new ArrayList<>(view.keySet()));
        yes(view.containsKey("c")); eq(null, view.getOrDefault("c", 99)); eq(99, view.getOrDefault("a", 99));
        throwsType(IllegalArgumentException.class, () -> view.put("e", 3));
        LinkedHashMap<String,Integer> batch = new LinkedHashMap<>(); batch.put("x", 8); batch.put("y", 9);
        throwsType(IllegalArgumentException.class, () -> view.putAll(batch)); yes(!source.containsKey("x"));
        eq(null, view.put("a", 6)); eq(6, source.get("a"));
        Map.Entry<String,Integer> entry = view.entrySet().iterator().next();
        throwsType(IllegalArgumentException.class, () -> entry.setValue(7)); eq(6, entry.setValue(8));
        yes(view.entrySet().contains(Map.entry("a", 8)));
        yes(!view.entrySet().contains(Map.entry("a", 6)));
        view.values().remove(2); yes(!source.containsKey("b"));
        source.put("hidden", 3); view.keySet().removeAll(List.of("hidden", "c"));
        yes(source.containsKey("hidden")); yes(!source.containsKey("c"));
        view.compute("d", (k,v) -> 10); eq(10, source.get("d"));
        throwsType(IllegalArgumentException.class, () -> view.compute("d", (k,v) -> 11)); eq(10, source.get("d"));
        view.clear(); eq(Map.of("hidden", 3), source);
        TreeMap<String,Integer> tree = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        tree.put("ALPHA", 2); tree.put("bravo", 3); tree.put("CHARLIE", 4); tree.put("DELTA", null);
        NavigableMap<String,Integer> navigable = Collections.filteredNavigableMap(tree,
                (k,v) -> Character.isUpperCase(k.charAt(0)) && EVEN.test(v));
        yes(navigable.entrySet().contains(Map.entry("alpha", 2)));
        eq(2, navigable.get("alpha")); yes(!navigable.containsKey("BRAVO"));
        eq("ALPHA", navigable.firstKey()); eq("DELTA", navigable.lastKey());
        eq("ALPHA", navigable.floorKey("bravo")); eq("CHARLIE", navigable.ceilingKey("bravo"));
        eq("ALPHA", navigable.lowerKey("CHARLIE")); eq("DELTA", navigable.higherKey("CHARLIE"));
        throwsType(UnsupportedOperationException.class, () -> navigable.firstEntry().setValue(6));
        eq(List.of("DELTA", "CHARLIE", "ALPHA"), new ArrayList<>(navigable.descendingKeySet()));
        eq(navigable, navigable.reversed().reversed());
        eq(List.of("CHARLIE"), new ArrayList<>(navigable.subMap("alpha", false, "delta", false).keySet()));
        throwsType(IllegalArgumentException.class, () -> navigable.headMap("charlie", false).put("DELTA", 8));
        Map.Entry<String,Integer> removed = navigable.pollLastEntry(); eq("DELTA", removed.getKey()); eq(null, removed.getValue());
        yes(navigable.navigableKeySet().remove("alpha")); yes(!tree.containsKey("ALPHA"));
        navigable.clear(); eq(Map.of("bravo", 3), tree);
        Object a = new String("same"), b = new String("same"), av = new String("value"), bv = new String("value");
        IdentityHashMap<Object,Object> identity = new IdentityHashMap<>(); identity.put(a, av); identity.put(b, bv);
        Map<Object,Object> identities = Collections.filteredMap(identity, (k,v) -> k == b);
        yes(!identities.containsKey(a)); yes(identities.containsKey(b));
        yes(!identities.containsValue(av)); yes(identities.containsValue(bv));
        yes(!identities.remove(b, av)); yes(identities.remove(b, bv)); yes(identity.containsKey(a));
        for (int wrapping = 0; wrapping < 4; wrapping++) {
            IdentityHashMap<Object,Object> same = new IdentityHashMap<>(); same.put(a,av);
            Map<Object,Object> wrapper = switch (wrapping) {
                case 1 -> Collections.unmodifiableMap(same);
                case 2 -> Collections.synchronizedMap(same);
                case 3 -> Collections.checkedMap(same,Object.class,Object.class);
                default -> same;
            };
            Map<Object,Object> selected = Collections.filteredMap(wrapper,(k,v) -> true);
            IdentityHashMap<Object,Object> differentValue = new IdentityHashMap<>(); differentValue.put(a,bv);
            yes(!selected.equals(differentValue)); yes(!differentValue.equals(selected));
            yes(selected.equals(same)); yes(same.equals(selected)); eq(same.hashCode(),selected.hashCode());
            yes(selected.keySet().equals(same.keySet())); yes(same.keySet().equals(selected.keySet()));
            eq(same.keySet().hashCode(),selected.keySet().hashCode());
        }
        LinkedHashMap<String,Integer> accesses = new LinkedHashMap<>(4,0.75f,true);
        accesses.put("a",2); accesses.put("hidden",1); accesses.put("b",4);
        Map<String,Integer> accessView = Collections.filteredMap(Collections.synchronizedMap(accesses), (k,v) -> EVEN.test(v));
        yes(accessView.containsKey("a")); eq(List.of("a","hidden","b"), new ArrayList<>(accesses.keySet()));
        eq(2,accessView.get("a")); eq(List.of("hidden","b","a"), new ArrayList<>(accesses.keySet()));
        eq(null,accessView.get("hidden")); eq(List.of("hidden","b","a"), new ArrayList<>(accesses.keySet()));
        eq(4,accessView.getOrDefault("b",99)); eq(List.of("hidden","a","b"), new ArrayList<>(accesses.keySet()));
    }
    private static void deques() {
        Deque<Integer> source = new LinkedList<>(Arrays.asList(1, null, 3, 2, 5, 4, 7));
        Deque<Integer> view = Collections.filteredDeque(source, EVEN);
        eq(null, view.getFirst()); eq(4, view.getLast());
        int before = source.size(); eq(null, view.pollFirst()); eq(before - 1, source.size());
        eq(4, view.removeLast()); eq(List.of(1, 3, 2, 5, 7), new ArrayList<>(source));
        view.addFirst(6); view.addLast(8); eq(List.of(6, 1, 3, 2, 5, 7, 8), new ArrayList<>(source));
        eq(List.of(8, 2, 6), new ArrayList<>(view.reversed()));
        yes(view.removeLastOccurrence(2)); yes(!view.removeFirstOccurrence(3));
        view.push(10); eq(10, view.pop());
        view.clear(); eq(List.of(1, 3, 5, 7), new ArrayList<>(source));
        throwsType(NoSuchElementException.class, view::removeFirst);
        throwsType(NoSuchElementException.class, view::getLast);
        eq(null, view.pollLast());
    }
    record Rank(int group, int occurrence) { }
    private static void snapshots() {
        List<Integer> source = new ArrayList<>();
        for (int n : new int[] {0,1,2,63,64,65,127,128,129,513}) {
            source.clear(); for (int i = 0; i < n; i++) source.add((i * 37) % 97);
            AtomicInteger calls = new AtomicInteger();
            List<Integer> actual = Collections.filteredSortedSnapshot(source,
                    x -> { calls.incrementAndGet(); return EVEN.test(x); }, Comparator.reverseOrder());
            eq(n, calls.get());
            List<Integer> expected = filtered(source, EVEN); expected.sort(Comparator.reverseOrder());
            eq(expected, actual); eq(expected.reversed(), actual.reversed());
            for (int i = 0; i <= actual.size(); i++) {
                eq(expected.subList(i, actual.size()), actual.subList(i, actual.size()));
                eq(expected.reversed().subList(0, i), actual.reversed().subList(0, i));
            }
            source.clear(); eq(expected, actual);
            throwsType(UnsupportedOperationException.class, () -> actual.addAll(List.of()));
            throwsType(UnsupportedOperationException.class, () -> actual.removeIf(x -> false));
            throwsType(UnsupportedOperationException.class, () -> actual.sort(null));
        }
        List<Rank> ranks = new ArrayList<>(); for (int i = 0; i < 500; i++) ranks.add(new Rank(i % 7, i));
        List<Rank> expected = new ArrayList<>(ranks); expected.sort(Comparator.comparingInt(Rank::group));
        List<Rank> actual = Collections.sortedSnapshot(ranks, Comparator.comparingInt(Rank::group));
        eq(expected, actual); eq(ranks, new ArrayList<>(ranks));
        for (int i = 0; i < actual.size(); i++) yes(actual.get(i) == expected.get(i));
        List<Integer> nulls = Arrays.asList(null, 2, 1, null, 4);
        eq(Arrays.asList(null, 2, null, 4), Collections.filteredSnapshot(nulls, EVEN));
        eq(Arrays.asList(null, null, 1, 2, 4), Collections.sortedSnapshot(nulls,
                Comparator.nullsFirst(Comparator.naturalOrder())));
    }
    private static void sortedLive() {
        List<Integer> source = new ArrayList<>(List.of(3, 1, 2, 1));
        List<Integer> view = Collections.sortedList(source, null);
        eq(List.of(1, 1, 2, 3), view); eq(List.of(3, 1, 2, 1), source);
        Iterator<Integer> old = view.iterator(); source.set(0, 0);
        eq(List.of(0, 1, 1, 2), view);
        List<Integer> observed = new ArrayList<>(); old.forEachRemaining(observed::add);
        eq(List.of(1, 1, 2, 3), observed);
        List<Integer> span = view.subList(1, 3); eq(List.of(1, 1), span);
        source.set(1, 9); eq(List.of(1, 2), span);
        eq(List.of(9, 2, 1, 0), view.reversed());
        eq(view, view.reversed().reversed());
        eq(span.reversed(), view.reversed().subList(1, 3));
        source.add(-1); eq(-1, view.getFirst());
        throwsType(UnsupportedOperationException.class, () -> view.set(0, 99));
        throwsType(UnsupportedOperationException.class, view::clear);
        throwsType(UnsupportedOperationException.class, () -> view.add(0, 3));
    }
    private static void boundedNavigation() {
        Comparator<Integer> comparator = Comparator.nullsFirst(Comparator.naturalOrder());
        for (boolean descending : new boolean[] {false, true}) {
            NavigableSet<Integer> set = new TreeSet<>(comparator);
            set.addAll(Arrays.asList(null, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6));
            NavigableSet<Integer> slice = set.subSet(-2, true, 5, false);
            NavigableSet<Integer> backing = descending ? slice.descendingSet() : slice;
            NavigableSet<Integer> view = Collections.filteredNavigableSet(backing, EVEN);
            NavigableSet<Integer> expected = new TreeSet<>(backing.comparator());
            expected.addAll(filtered(backing, EVEN));
            for (Integer key : Arrays.asList(null, -99, -3, -2, -1, 0, 1, 2, 3, 4, 5, 99)) {
                eq(expected.contains(key), view.contains(key));
                eq(expected.lower(key), view.lower(key)); eq(expected.floor(key), view.floor(key));
                eq(expected.ceiling(key), view.ceiling(key)); eq(expected.higher(key), view.higher(key));
            }
            yes(!view.remove(99)); yes(!view.remove(-99));
            eq(expected.pollFirst(), view.removeFirst()); eq(expected.pollLast(), view.removeLast());
            eq(new ArrayList<>(expected), new ArrayList<>(view));
            SortedSet<Integer> sorted = Collections.filteredSortedSet(backing, EVEN);
            while (!sorted.isEmpty()) sorted.removeLast();
            yes(filtered(backing, EVEN).isEmpty());
            throwsType(NoSuchElementException.class, sorted::removeFirst);
            NavigableMap<Integer,Integer> map = new TreeMap<>(comparator);
            for (Integer key : Arrays.asList(null, -3, -2, -1, 0, 1, 2, 3, 4, 5)) map.put(key, key);
            NavigableMap<Integer,Integer> range = map.subMap(-2, true, 5, false);
            NavigableMap<Integer,Integer> original = descending ? range.descendingMap() : range;
            NavigableMap<Integer,Integer> selected = Collections.filteredNavigableMap(original, (k,v) -> EVEN.test(v));
            NavigableMap<Integer,Integer> oracle = new TreeMap<>(original.comparator());
            for (Map.Entry<Integer,Integer> e : original.entrySet()) if (EVEN.test(e.getValue())) oracle.put(e.getKey(),e.getValue());
            for (Integer key : Arrays.asList(null, -99, -3, -2, -1, 0, 1, 2, 3, 4, 5, 99)) {
                eq(oracle.lowerEntry(key), selected.lowerEntry(key)); eq(oracle.floorEntry(key), selected.floorEntry(key));
                eq(oracle.ceilingEntry(key), selected.ceilingEntry(key)); eq(oracle.higherEntry(key), selected.higherEntry(key));
            }
            SortedMap<Integer,Integer> sortedMap = Collections.filteredSortedMap(original, (k,v) -> EVEN.test(v));
            eq(oracle.pollFirstEntry(), sortedMap.pollFirstEntry());
            eq(oracle.pollLastEntry(), sortedMap.pollLastEntry());
            eq(oracle, sortedMap);
            sortedMap.clear(); eq(null, sortedMap.pollFirstEntry()); eq(null, sortedMap.pollLastEntry());
        }
    }
    private static void deepComposition() {
        List<Integer> source = new ArrayList<>(List.of(1,2,3,4));
        List<Integer> view = source;
        for (int i = 0; i < 20000; i++) view = Collections.filteredList(view, x -> x > 0);
        eq(source, view); view.remove(Integer.valueOf(2)); eq(List.of(1,3,4), source);
        Map<Integer,Integer> map = new HashMap<>(Map.of(1,2,3,4));
        Map<Integer,Integer> filtered = map;
        for (int i = 0; i < 20000; i++) filtered = Collections.filteredMap(filtered, (k,v) -> v > 0);
        eq(map, filtered); filtered.remove(1); eq(Map.of(3,4), map);
    }
    private static void failures() {
        throwsType(NullPointerException.class, () -> Collections.filteredCollection(null, EVEN));
        throwsType(NullPointerException.class, () -> Collections.filteredList(List.of(), null));
        throwsType(NullPointerException.class, () -> Collections.filteredSnapshot(List.of(), null));
        throwsType(NullPointerException.class, () -> Collections.filteredMap(Map.of(), null));
        List<Integer> source = new ArrayList<>(List.of(1,2,3,4,5,6));
        List<Integer> view = Collections.filteredList(source, EVEN);
        throwsType(IndexOutOfBoundsException.class, () -> view.add(4, 8));
        throwsType(IllegalArgumentException.class, () -> view.addAll(List.of(8,9)));
        eq(List.of(1,2,3,4,5,6), source);
        throwsType(IllegalStateException.class, () -> view.removeIf(x -> { if (x == 4) throw new IllegalStateException(); return true; }));
        eq(List.of(1,2,3,4,5,6), source);
        throwsType(IllegalStateException.class, () -> Collections.sortedSnapshot(source, (a,b) -> { throw new IllegalStateException(); }));
        eq(List.of(1,2,3,4,5,6), source);
        List<Integer> readonly = Collections.filteredList(Collections.unmodifiableList(source), EVEN);
        throwsType(UnsupportedOperationException.class, () -> readonly.add(8));
        throwsType(UnsupportedOperationException.class, () -> readonly.removeIf(x -> false));
        List<Integer> failFastSource = new ArrayList<>(List.of(2,4));
        ListIterator<Integer> iterator = Collections.filteredList(failFastSource, EVEN).listIterator();
        eq(2, iterator.next()); failFastSource.add(6);
        throwsType(ConcurrentModificationException.class, iterator::next);
    }
}
