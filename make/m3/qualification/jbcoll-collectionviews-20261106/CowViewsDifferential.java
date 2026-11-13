/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.SplittableRandom;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Differential driver of leaf jbcoll-collectionviews-20261106 (DIFF_COW_VIEWS v1).
 *
 * <p>One seeded operation stream over filtered views of 26 backing shapes: the CopyOnWrite owners and their
 * subclasses and sub-lists (the removeMatching dispatch the census hunk touches), the List/Deque/Set/Sorted/
 * Navigable owners, and keySet/values/entrySet views of HashMap, TreeMap and IdentityHashMap. Mode {@code oracle}
 * runs every operation through an independent model written on stock JDK 21 calls only (a filtered view is the
 * backing restricted to the predicate: bulk removal = backing.removeIf(visible && condition), membership =
 * a stored equivalent that is visible); mode {@code views} runs the same stream through java.util.CollectionViews
 * (a patched java.base). Every operation prints one line: index, subject, operation, result, and after a mutation the
 * owner's size and a 64-bit content hash (key, tag and identity of every stored element in owner order; sorted for
 * the identity-hashed owners, whose iteration order is not reproducible between two JVM runs).
 *
 * <p>RNG use per operation is fixed (eight draws), element identities derive from the operation index, and every
 * subject is re-populated every 4096 operations from its own seed, so a divergence stays inside its subject and
 * epoch. java.util.CollectionViews is resolved only in views mode (nested class ViewsMode).
 *
 * <p>usage: java [--patch-module java.base=CLASSES] CowViewsDifferential oracle|views SEED OPS OUT
 */
public final class CowViewsDifferential {
    private static final int EPOCH = 4096;
    private static final int KEYS = 32;
    private static final int TAGS = 8;
    private static final int CAP = 96;
    private static final String[] OPS = {
        "removeIf", "removeAll", "retainAll", "clear", "remove", "contains", "add", "addAll",
        "size", "isEmpty", "iterate", "streamCount", "iteratorRemove"};
    private static final int[] WEIGHTS = {14, 10, 10, 1, 12, 12, 10, 8, 5, 3, 6, 6, 3};

    private CowViewsDifferential() { }

    /** Element: equality, hash and order by key; tag drives the view predicate; id is the identity witness. */
    static final class Item implements Comparable<Item> {
        final int key;
        final int tag;
        final int id;

        Item(int key, int tag, int id) {
            this.key = key;
            this.tag = tag;
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Item item && item.key == key;
        }

        @Override
        public int hashCode() {
            return key;
        }

        @Override
        public int compareTo(Item other) {
            return Integer.compare(key, other.key);
        }

        @Override
        public String toString() {
            return key + ":" + tag + "#" + id;
        }
    }

    static final class CowListSub<E> extends CopyOnWriteArrayList<E> {
        private static final long serialVersionUID = 1L;
    }

    static final class CowSetSub<E> extends CopyOnWriteArraySet<E> {
        private static final long serialVersionUID = 1L;
    }

    /** One backing shape: a fresh owner per epoch, the collection the view filters, and the owner's printable state. */
    abstract static class Subject {
        final String name;
        final boolean identity;
        final boolean entries;
        final boolean nested;
        Object owner;
        Collection<Object> backing;
        Collection<Object> view;

        Subject(String name, boolean identity, boolean entries, boolean nested) {
            this.name = name;
            this.identity = identity;
            this.entries = entries;
            this.nested = nested;
        }

        abstract void populate(List<Item> keys, List<Item> values);

        Iterable<?> state() {
            return owner instanceof Map<?, ?> map ? map.entrySet() : (Iterable<?>) owner;
        }
    }

    /** A plain owner that is itself the backing. */
    static Subject plain(String name, Supplier<Collection<Object>> factory, boolean identity, boolean nested) {
        return new Subject(name, identity, false, nested) {
            @Override
            void populate(List<Item> keys, List<Item> values) {
                Collection<Object> fresh = factory.get();
                fresh.addAll(keys);
                owner = fresh;
                backing = fresh;
            }
        };
    }

    static Subject subList(String name, Supplier<List<Object>> factory) {
        return new Subject(name, false, false, false) {
            @Override
            void populate(List<Item> keys, List<Item> values) {
                List<Object> fresh = factory.get();
                fresh.addAll(keys);
                int from = Math.min(2, fresh.size());
                int to = Math.max(from, fresh.size() - 2);
                owner = fresh;
                backing = fresh.subList(from, to);
            }
        };
    }

    static Subject sortedView(String name, int kind) {
        return new Subject(name, false, false, false) {
            @Override
            void populate(List<Item> keys, List<Item> values) {
                TreeSet<Object> fresh = new TreeSet<>();
                fresh.addAll(keys);
                owner = fresh;
                NavigableSet<Object> navigable = fresh;
                Item low = new Item(4, 0, -1);
                Item high = new Item(KEYS - 4, 0, -2);
                backing = kind == 0 ? navigable.descendingSet() : navigable.subSet(low, true, high, false);
            }
        };
    }

    static Subject mapView(String name, Supplier<Map<Object, Object>> factory, int kind, boolean identity) {
        return new Subject(name, identity, kind == 2, false) {
            @Override
            void populate(List<Item> keys, List<Item> values) {
                Map<Object, Object> fresh = factory.get();
                for (int i = 0; i < keys.size(); i++) fresh.put(keys.get(i), values.get(i));
                owner = fresh;
                @SuppressWarnings("unchecked")
                Collection<Object> entrySet = (Collection<Object>) (Collection<?>) fresh.entrySet();
                backing = kind == 0 ? fresh.keySet() : kind == 1 ? fresh.values() : entrySet;
            }
        };
    }

    static List<Subject> subjects() {
        List<Subject> all = new ArrayList<>();
        all.add(plain("ArrayList", ArrayList::new, false, false));
        all.add(plain("LinkedList", LinkedList::new, false, false));
        all.add(plain("ArrayDeque", ArrayDeque::new, false, false));
        all.add(plain("CopyOnWriteArrayList", CopyOnWriteArrayList::new, false, false));
        all.add(plain("CopyOnWriteArraySet", CopyOnWriteArraySet::new, false, false));
        all.add(plain("CowListSub", CowListSub::new, false, false));
        all.add(plain("CowSetSub", CowSetSub::new, false, false));
        all.add(plain("nested:ArrayList", ArrayList::new, false, true));
        all.add(plain("nested:CopyOnWriteArrayList", CopyOnWriteArrayList::new, false, true));
        all.add(plain("nested:CowListSub", CowListSub::new, false, true));
        all.add(plain("nested:CowSetSub", CowSetSub::new, false, true));
        all.add(subList("ArrayList.subList", ArrayList::new));
        all.add(subList("CopyOnWriteArrayList.subList", CopyOnWriteArrayList::new));
        all.add(subList("CowListSub.subList", CowListSub::new));
        all.add(plain("HashSet", HashSet::new, false, false));
        all.add(plain("LinkedHashSet", LinkedHashSet::new, false, false));
        all.add(plain("TreeSet", TreeSet::new, false, false));
        all.add(sortedView("TreeSet.descendingSet", 0));
        all.add(sortedView("TreeSet.subSet", 1));
        all.add(plain("identitySet", () -> Collections.newSetFromMap(new IdentityHashMap<>()), true, false));
        all.add(mapView("HashMap.keySet", HashMap::new, 0, false));
        all.add(mapView("HashMap.values", HashMap::new, 1, false));
        all.add(mapView("HashMap.entrySet", HashMap::new, 2, false));
        all.add(mapView("TreeMap.keySet", TreeMap::new, 0, false));
        all.add(mapView("TreeMap.values", TreeMap::new, 1, false));
        all.add(mapView("IdentityHashMap.keySet", IdentityHashMap::new, 0, true));
        all.add(mapView("IdentityHashMap.values", IdentityHashMap::new, 1, true));
        return all;
    }

    // ------------------------------------------------------------------ predicates and element codes

    static Item itemOf(Object element) {
        return element instanceof Map.Entry<?, ?> entry ? (Item) entry.getValue() : (Item) element;
    }

    static boolean visible(Object element) {
        return itemOf(element).tag % 3 != 0;
    }

    static boolean innerVisible(Object element) {
        return itemOf(element).tag != 7;
    }

    static boolean outerVisible(Object element) {
        return itemOf(element).tag % 3 != 0;
    }

    static long code(Object element) {
        if (element instanceof Map.Entry<?, ?> entry) {
            return code(entry.getKey()) * 1_000_003L + code(entry.getValue());
        }
        Item item = (Item) element;
        return (item.key * 131L + item.tag) * 16_777_259L + item.id;
    }

    /** Owner-order hash; identity-hashed owners (unordered between runs) hash their sorted element codes. */
    static String digest(Iterable<?> elements, boolean unordered) {
        List<Long> codes = new ArrayList<>();
        for (Object element : elements) codes.add(code(element));
        if (unordered) Collections.sort(codes);
        long hash = 0xcbf29ce484222325L;
        for (long value : codes) hash = (hash ^ value) * 0x100000001b3L;
        return "n=" + codes.size() + " h=" + Long.toHexString(hash);
    }

    // ------------------------------------------------------------------ the two view factories

    interface Views {
        Collection<Object> filtered(Collection<Object> backing, Predicate<Object> predicate);
    }

    /** Resolves java.util.CollectionViews; loaded only in views mode. */
    static final class ViewsMode implements Views {
        @Override
        public Collection<Object> filtered(Collection<Object> backing, Predicate<Object> predicate) {
            return java.util.CollectionViews.filtered(backing, predicate);
        }
    }

    /** The independent stock-JDK model of a filtered view (see the class comment). */
    static final class OracleMode implements Views {
        private final Subject subject;

        OracleMode(Subject subject) {
            this.subject = subject;
        }

        @Override
        public Collection<Object> filtered(Collection<Object> backing, Predicate<Object> predicate) {
            if (backing instanceof OracleView inner) {
                Predicate<Object> first = inner.predicate;
                return new OracleView(inner.backing, element -> first.test(element) && predicate.test(element),
                        subject.identity);
            }
            return new OracleView(backing, predicate, subject.identity);
        }
    }

    static final class OracleView extends AbstractCollection<Object> {
        final Collection<Object> backing;
        final Predicate<Object> predicate;
        final boolean identity;

        OracleView(Collection<Object> backing, Predicate<Object> predicate, boolean identity) {
            this.backing = backing;
            this.predicate = predicate;
            this.identity = identity;
        }

        private boolean same(Object candidate, Object stored) {
            return identity ? candidate == stored : java.util.Objects.equals(candidate, stored);
        }

        /** The stored equivalent of a Set member (a Set holds at most one), or this view itself when absent. */
        private Object storedEquivalent(Object candidate) {
            for (Object stored : backing) {
                if (same(candidate, stored)) return stored;
            }
            return this;
        }

        @Override
        public Iterator<Object> iterator() {
            List<Object> selected = new ArrayList<>();
            for (Object stored : backing) {
                if (predicate.test(stored)) selected.add(stored);
            }
            Iterator<Object> cursor = selected.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return cursor.hasNext();
                }

                @Override
                public Object next() {
                    return cursor.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("filtered iterator removal");
                }
            };
        }

        @Override
        public int size() {
            int count = 0;
            for (Object stored : backing) {
                if (predicate.test(stored)) count++;
            }
            return count;
        }

        @Override
        public boolean contains(Object candidate) {
            if (backing instanceof Set<?>) {
                Object stored = storedEquivalent(candidate);
                return stored != this && predicate.test(stored);
            }
            for (Object stored : backing) {
                if (same(candidate, stored) && predicate.test(stored)) return true;
            }
            return false;
        }

        @Override
        public boolean add(Object element) {
            if (!predicate.test(element)) throw new IllegalArgumentException("element rejected by view predicate");
            return backing.add(element);
        }

        @Override
        public boolean addAll(Collection<?> elements) {
            List<Object> copy = new ArrayList<>(elements);
            for (Object element : copy) {
                if (!predicate.test(element)) {
                    throw new IllegalArgumentException("element rejected by view predicate");
                }
            }
            return backing.addAll(copy);
        }

        @Override
        public boolean remove(Object candidate) {
            if (backing instanceof Set<?>) {
                Object stored = storedEquivalent(candidate);
                return stored != this && predicate.test(stored) && backing.remove(stored);
            }
            if (backing instanceof List<Object> list) {
                for (int index = 0; index < list.size(); index++) {
                    Object stored = list.get(index);
                    if (same(candidate, stored) && predicate.test(stored)) {
                        list.remove(index);
                        return true;
                    }
                }
                return false;
            }
            for (Iterator<Object> cursor = backing.iterator(); cursor.hasNext(); ) {
                Object stored = cursor.next();
                if (same(candidate, stored) && predicate.test(stored)) {
                    cursor.remove();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean removeIf(Predicate<? super Object> condition) {
            return backing.removeIf(element -> predicate.test(element) && condition.test(element));
        }

        @Override
        public boolean removeAll(Collection<?> elements) {
            return backing.removeIf(element -> predicate.test(element) && elements.contains(element));
        }

        @Override
        public boolean retainAll(Collection<?> elements) {
            return backing.removeIf(element -> predicate.test(element) && !elements.contains(element));
        }

        @Override
        public void clear() {
            backing.removeIf(predicate);
        }
    }

    // ------------------------------------------------------------------ the stream

    static final class Draws {
        final int[] value = new int[8];

        void next(SplittableRandom random) {
            for (int i = 0; i < value.length; i++) value[i] = random.nextInt(1 << 20);
        }
    }

    /** A candidate: an existing owner element (by index) or a fresh element whose identity derives from the op. */
    static Object candidate(Subject subject, int pick, int key, int tag, int id) {
        if ((pick & 1) == 0) {
            List<Object> stored = new ArrayList<>(subject.backing);
            if (subject.identity) stored.sort((a, b) -> Long.compare(code(a), code(b)));
            if (!stored.isEmpty()) return stored.get((pick >>> 1) % stored.size());
        }
        Item item = new Item(key % KEYS, tag % TAGS, id);
        if (subject.entries) return Map.entry(new Item(key % KEYS, (tag >>> 3) % TAGS, id + 1), item);
        return item;
    }

    static void populate(Subject subject, long seed, int epoch, int index, Views views) {
        SplittableRandom random = new SplittableRandom(seed ^ (epoch * 0x9E3779B97F4A7C15L) ^ (index * 0xBF58476D1CE4E5B9L));
        int size = random.nextInt(49);
        List<Item> keys = new ArrayList<>();
        List<Item> values = new ArrayList<>();
        int base = 100_000_000 + epoch * 10_000 + index * 200;
        for (int i = 0; i < size; i++) {
            keys.add(new Item(random.nextInt(KEYS), random.nextInt(TAGS), base + 2 * i));
            values.add(new Item(random.nextInt(KEYS), random.nextInt(TAGS), base + 2 * i + 1));
        }
        subject.populate(keys, values);
        if (subject.nested) {
            subject.view = views.filtered(views.filtered(subject.backing, CowViewsDifferential::innerVisible),
                    CowViewsDifferential::outerVisible);
        } else {
            subject.view = views.filtered(subject.backing, CowViewsDifferential::visible);
        }
    }

    static int pickOp(int draw) {
        int total = 0;
        for (int weight : WEIGHTS) total += weight;
        int point = draw % total;
        for (int op = 0; op < WEIGHTS.length; op++) {
            point -= WEIGHTS[op];
            if (point < 0) return op;
        }
        return 0;
    }

    static List<Object> candidates(Subject subject, Draws draws, int base, int count) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int mix = draws.value[2 + i % 6] ^ (i * 0x45d9f3b);
            list.add(candidate(subject, mix, mix >>> 3, mix >>> 9, base + 2 * i));
        }
        return list;
    }

    static String apply(Subject subject, int op, Draws draws, int index) {
        Collection<Object> view = subject.view;
        int idBase = index * 16;
        int modulus = 2 + draws.value[1] % 5;
        int residue = draws.value[1] % modulus;
        Predicate<Object> condition = element -> itemOf(element).key % modulus == residue;
        switch (OPS[op]) {
            case "removeIf":
                return "m" + modulus + "r" + residue + " -> " + view.removeIf(condition);
            case "removeAll": {
                List<Object> list = candidates(subject, draws, idBase, draws.value[1] % 6);
                return list + " -> " + view.removeAll(list);
            }
            case "retainAll": {
                List<Object> list = candidates(subject, draws, idBase, draws.value[1] % 6);
                return list + " -> " + view.retainAll(list);
            }
            case "clear":
                view.clear();
                return "-> void";
            case "remove": {
                Object probe = candidates(subject, draws, idBase, 1).get(0);
                return probe + " -> " + view.remove(probe);
            }
            case "contains": {
                Object probe = candidates(subject, draws, idBase, 1).get(0);
                return probe + " -> " + view.contains(probe);
            }
            case "add": {
                Object element = candidates(subject, draws, idBase, 1).get(0);
                if (subject.backing.size() >= CAP) return element + " -> skip";
                return element + " -> " + view.add(element);
            }
            case "addAll": {
                List<Object> list = candidates(subject, draws, idBase, draws.value[1] % 4);
                if (subject.backing.size() >= CAP) return list + " -> skip";
                return list + " -> " + view.addAll(list);
            }
            case "size":
                return "-> " + view.size();
            case "isEmpty":
                return "-> " + view.isEmpty();
            case "iterate":
                return "-> " + digest(view, subject.identity);
            case "streamCount":
                return "m" + modulus + "r" + residue + " -> " + view.stream().filter(condition).count();
            case "iteratorRemove": {
                Iterator<Object> cursor = view.iterator();
                if (!cursor.hasNext()) return "-> empty";
                cursor.next();
                cursor.remove();
                return "-> removed";
            }
            default:
                throw new AssertionError(op);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("usage: oracle|views SEED OPS OUT");
        boolean oracle = switch (args[0]) {
            case "oracle" -> true;
            case "views" -> false;
            default -> throw new IllegalArgumentException(args[0]);
        };
        long seed = Long.decode(args[1]);
        int ops = Integer.parseInt(args[2]);
        List<Subject> subjects = subjects();
        Views shared = oracle ? null : new ViewsMode();
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        SplittableRandom random = new SplittableRandom(seed);
        Draws draws = new Draws();
        long mutations = 0;
        long exceptions = 0;
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(Path.of(args[3])), StandardCharsets.UTF_8), 1 << 16)) {
            for (int index = 0; index < ops; index++) {
                if (index % EPOCH == 0) {
                    for (int s = 0; s < subjects.size(); s++) {
                        Subject subject = subjects.get(s);
                        populate(subject, seed, index / EPOCH, s, oracle ? new OracleMode(subject) : shared);
                    }
                }
                draws.next(random);
                Subject subject = subjects.get(draws.value[0] % subjects.size());
                int op = pickOp(draws.value[0] >>> 5);
                String result;
                try {
                    result = apply(subject, op, draws, index);
                } catch (RuntimeException failure) {
                    result = "-> " + failure.getClass().getSimpleName();
                    exceptions++;
                }
                boolean mutating = op <= 4 || op == 6 || op == 7 || op == 12;
                String line = index + " " + subject.name + " " + OPS[op] + " " + result
                        + (mutating ? " | " + digest(subject.state(), subject.identity) : "") + "\n";
                if (mutating) mutations++;
                out.write(line);
                sha.update(line.getBytes(StandardCharsets.UTF_8));
            }
        }
        System.out.println("DIFF_COW_VIEWS mode=" + args[0] + " seed=" + args[1] + " ops=" + ops + " subjects="
                + subjects.size() + " mutations=" + mutations + " exceptions=" + exceptions + " sha256="
                + HexFormat.of().formatHex(sha.digest()));
    }
}
