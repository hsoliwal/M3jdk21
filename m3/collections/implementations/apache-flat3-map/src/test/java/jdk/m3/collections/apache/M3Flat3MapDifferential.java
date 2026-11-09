/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.apache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.apache.commons.collections4.IterableMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.Flat3Map;

/**
 * Seeded lockstep differential of census leaf L075 (m3/collections/qualification/flat3map-single-owner-20261106).
 * {@code full}: Commons {@code Flat3Map} 4.5.0 against the facade {@link M3Flat3Map} over the whole Map/IterableMap
 * API; {@code subset}: Commons against the core {@code jdk.m3.collections.Flat3Map} over the API both share. Every
 * op draws its parameters once and applies them to both sides; each side's records feed its own SHA-256. Records
 * are order-insensitive where Commons itself is mode-dependent (iteration order, toString layout): entries are
 * sorted before they are written, and iterator removals and setValue calls are chosen by key/value predicates. Raw
 * iteration-order mismatches are only counted ({@code orderDiffs}). Exceptions are recorded by class name; every
 * record ends with a state stamp (size and Map.hashCode) so a silent divergence surfaces at the op that caused it.
 *
 * <p>usage: java -cp ... jdk.m3.collections.apache.M3Flat3MapDifferential full|subset [ops] [dumpDir]
 */
final class M3Flat3MapDifferential {
    /** Named seed: "L075" and the leaf date. */
    static final long SEED = 0x4C303735_20261106L;
    static final int OPS = 1_000_000;
    private static final int KEYS = 12;
    private static final int VALUES = 10;
    private static final int PHASE = 512;
    private static final String[] NAMES = {"put", "get", "remove", "containsKey", "containsValue", "size", "clear",
        "putAll", "entryIter", "keyIter", "valueIter", "entryIterRemove", "keyIterRemove", "valueIterRemove",
        "mapIter", "clone", "equals", "hashCode", "serial", "toString", "defaults", "misuse", "views"};
    /** Cumulative weights out of 1000, one per NAMES entry. */
    private static final int[] WEIGHTS = {200, 100, 120, 60, 40, 30, 6, 30, 30, 20, 20, 30, 20, 20, 40, 15, 30, 20,
        9, 20, 80, 20, 40};
    private static final boolean[] SUBSET = {true, true, true, true, true, true, true, false, true, true, true,
        false, true, false, false, true, false, false, false, false, false, false, false};

    private M3Flat3MapDifferential() { }

    /** Key with a chosen hashCode: ids 6..11 collide with each other and with id 0. */
    record Key(int id, int hash) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public int hashCode() { return hash; }

        @Override
        public boolean equals(Object other) { return other instanceof Key key && key.id == id; }

        @Override
        public String toString() { return "K" + id; }
    }

    /** Parameters drawn once per op. */
    record Op(int index, int code, Key key, Key other, Integer value, Integer next, int bits) { }

    /** One side of the lockstep: the map under test and its record digest. */
    interface Side {
        String apply(Op op, Side peer);

        Object peerView();

        List<String> order();
    }

    /** Result of one run. */
    record Result(String mode, int ops, String left, String right, long lines, long orderDiffs) {
        boolean identical() { return left.equals(right); }

        @Override
        public String toString() {
            return "DIFF mode=" + mode + " ops=" + ops + " seed=0x" + Long.toHexString(SEED) + " lines=" + lines
                    + " left_sha256=" + left + " right_sha256=" + right + " identical=" + identical()
                    + " orderDiffs=" + orderDiffs;
        }
    }

    public static void main(String[] args) throws IOException {
        String mode = args.length > 0 ? args[0] : "full";
        int ops = args.length > 1 ? Integer.parseInt(args[1]) : OPS;
        Path dump = args.length > 2 ? Path.of(args[2]) : null;
        Result result = run(mode, ops, dump);
        System.out.println(result);
        if (!result.identical()) throw new AssertionError(result.toString());
    }

    static Result run(String mode, int ops, Path dumpDir) throws IOException {
        boolean subset = mode.equals("subset");
        Side left = new MapSide(Flat3Map::new, map -> ((Flat3Map<Key, Integer>) map).clone());
        Side right = subset ? new CoreSide()
                : new MapSide(M3Flat3Map::new, map -> ((M3Flat3Map<Key, Integer>) map).clone());
        Digest leftDigest = new Digest(dumpDir, mode + "-commons.txt");
        Digest rightDigest = new Digest(dumpDir, mode + (subset ? "-core.txt" : "-facade.txt"));
        Random random = new Random(SEED);
        long orderDiffs = 0;
        try (leftDigest; rightDigest) {
            for (int i = 0; i < ops; i++) {
                Op op = draw(random, i);
                if (subset && !SUBSET[op.code]) continue;
                leftDigest.add(op, left.apply(op, right));
                rightDigest.add(op, right.apply(op, left));
                if (!left.order().equals(right.order())) orderDiffs++;
            }
        }
        return new Result(mode, ops, leftDigest.hex(), rightDigest.hex(), leftDigest.lines, orderDiffs);
    }

    static Op draw(Random random, int index) {
        int roll = random.nextInt(1000);
        int code = 0;
        for (int sum = WEIGHTS[0]; roll >= sum; sum += WEIGHTS[++code]) { }
        int bits = random.nextInt();
        if (code == 0 && (index / PHASE) % 2 == 1 && (bits & 3) != 0) code = 2;
        return new Op(index, code, key(random), key(random), value(random), value(random), bits);
    }

    private static Key key(Random random) {
        int id = random.nextInt(KEYS + 1);
        return id == KEYS ? null : new Key(id, id < KEYS / 2 ? id * 31 : id % 3);
    }

    private static Integer value(Random random) {
        int v = random.nextInt(VALUES + 1);
        return v == VALUES ? null : v;
    }

    static boolean pick(Object key, Object value, int bits) {
        return ((String.valueOf(key) + '=' + value).hashCode() ^ bits) % 3 == 0;
    }

    static String canonical(Iterable<? extends Map.Entry<?, ?>> entries, List<String> order) {
        List<String> rows = new ArrayList<>();
        order.clear();
        for (Map.Entry<?, ?> entry : entries) {
            rows.add(entry.getKey() + "=" + entry.getValue() + "#" + entry.hashCode());
            order.add(String.valueOf(entry.getKey()));
        }
        Collections.sort(rows);
        return rows.size() + rows.toString();
    }

    static String sorted(Iterable<?> items) {
        List<String> rows = new ArrayList<>();
        for (Object item : items) rows.add(String.valueOf(item));
        Collections.sort(rows);
        return rows.size() + rows.toString();
    }

    static String thrown(RuntimeException failure) { return "!" + failure.getClass().getSimpleName(); }

    /** A Commons-API map side: Commons Flat3Map or the facade. */
    static final class MapSide implements Side {
        private final UnaryOperator<IterableMap<Key, Integer>> cloner;
        private final List<String> order = new ArrayList<>();
        private IterableMap<Key, Integer> map;

        MapSide(Supplier<IterableMap<Key, Integer>> factory, UnaryOperator<IterableMap<Key, Integer>> cloner) {
            this.cloner = cloner;
            this.map = factory.get();
        }

        @Override
        public Object peerView() { return map; }

        @Override
        public List<String> order() { return order; }

        @Override
        public String apply(Op op, Side peer) {
            order.clear();
            String record;
            try {
                record = MapOps.apply(this, op, peer);
            } catch (RuntimeException failure) {
                record = thrown(failure);
            }
            return record + " |" + map.size() + ":" + map.hashCode();
        }

        IterableMap<Key, Integer> map() { return map; }

        void replace(IterableMap<Key, Integer> next) { map = next; }

        IterableMap<Key, Integer> cloneMap() { return cloner.apply(map); }

        String canonical() { return M3Flat3MapDifferential.canonical(map.entrySet(), order); }
    }

    /** Map-level ops shared by both MapSide instances. */
    static final class MapOps {
        private MapOps() { }

        static String apply(MapSide side, Op op, Side peer) {
            IterableMap<Key, Integer> m = side.map();
            return switch (op.code) {
                case 0 -> String.valueOf(m.put(op.key, op.value));
                case 1 -> String.valueOf(m.get(op.key));
                case 2 -> String.valueOf(m.remove(op.key));
                case 3 -> String.valueOf(m.containsKey(op.key));
                case 4 -> String.valueOf(m.containsValue(op.value));
                case 5 -> m.size() + "/" + m.isEmpty();
                case 6 -> { m.clear(); yield side.canonical(); }
                case 7 -> putAll(side, op);
                case 8 -> side.canonical();
                case 9 -> sorted(m.keySet());
                case 10 -> sorted(m.values());
                case 11 -> entryIterRemove(side, op);
                case 12 -> keyIterRemove(side, op);
                case 13 -> valueIterRemove(side, op);
                case 14 -> mapIter(side, op);
                case 15 -> cloneOp(side, op);
                case 16 -> equalsOp(side, op, peer);
                case 17 -> m.hashCode() + "/" + (m.hashCode() == new HashMap<>(m).hashCode());
                case 18 -> serial(side);
                case 19 -> toStringOp(m);
                case 20 -> Defaults.apply(side, op);
                case 21 -> misuse(m, op);
                default -> views(m, op);
            };
        }

        private static String putAll(MapSide side, Op op) {
            Map<Key, Integer> source = new LinkedHashMap<>();
            Random local = new Random(op.bits);
            for (int n = local.nextInt(7); n > 0; n--) source.put(key(local), value(local));
            side.map().putAll(source);
            return source.size() + ":" + side.canonical();
        }

        private static String entryIterRemove(MapSide side, Op op) {
            List<String> log = new ArrayList<>();
            for (Iterator<Map.Entry<Key, Integer>> it = side.map().entrySet().iterator(); it.hasNext();) {
                Map.Entry<Key, Integer> entry = it.next();
                Key key = entry.getKey();
                if (pick(key, entry.getValue(), op.bits)) {
                    it.remove();
                    log.add("-" + key);
                } else if (pick(key, entry.getValue(), op.bits >>> 7)) {
                    log.add("~" + key + ":" + entry.setValue(op.next) + ">" + entry.getValue());
                }
            }
            Collections.sort(log);
            return log + side.canonical();
        }

        private static String keyIterRemove(MapSide side, Op op) {
            List<String> log = new ArrayList<>();
            for (Iterator<Key> it = side.map().keySet().iterator(); it.hasNext();) {
                Key key = it.next();
                if (pick(key, null, op.bits)) {
                    it.remove();
                    log.add("-" + key);
                }
            }
            Collections.sort(log);
            return log + side.canonical();
        }

        private static String valueIterRemove(MapSide side, Op op) {
            int removed = 0;
            for (Iterator<Integer> it = side.map().values().iterator(); it.hasNext();) {
                if (pick(null, it.next(), op.bits)) {
                    it.remove();
                    removed++;
                }
            }
            return removed + ":" + side.canonical();
        }

        private static String mapIter(MapSide side, Op op) {
            List<String> log = new ArrayList<>();
            MapIterator<Key, Integer> it = side.map().mapIterator();
            while (it.hasNext()) {
                Key key = it.next();
                Integer value = it.getValue();
                boolean consistent = key == it.getKey() && value == side.map().get(key);
                if (pick(key, value, op.bits)) {
                    it.remove();
                    log.add("-" + key + consistent + after(it));
                } else if (pick(key, value, op.bits >>> 5)) {
                    log.add("~" + key + consistent + ":" + it.setValue(op.next) + ">" + it.getValue());
                }
            }
            Collections.sort(log);
            return log + side.canonical();
        }

        private static String after(MapIterator<Key, Integer> it) {
            try {
                return String.valueOf(it.getKey());
            } catch (RuntimeException failure) {
                return thrown(failure);
            }
        }

        private static String cloneOp(MapSide side, Op op) {
            IterableMap<Key, Integer> original = side.map();
            IterableMap<Key, Integer> copy = side.cloneMap();
            String text = M3Flat3MapDifferential.canonical(copy.entrySet(), new ArrayList<>()) + copy.equals(original)
                    + original.equals(copy);
            boolean had = original.containsKey(op.other);
            int size = original.size();
            copy.put(op.other, op.next);
            copy.remove(op.key);
            text += "|" + (had == original.containsKey(op.other)) + (size == original.size());
            if ((op.bits & 1) == 1) side.replace(copy);
            return text + side.canonical();
        }

        private static String equalsOp(MapSide side, Op op, Side peer) {
            IterableMap<Key, Integer> m = side.map();
            Map<Key, Integer> copy = new HashMap<>(m);
            Map<Key, Integer> perturbed = new HashMap<>(m);
            perturbed.merge(op.key, VALUES, (a, b) -> a == null ? b : a + 1);
            return "" + m.equals(copy) + copy.equals(m) + m.equals(perturbed) + perturbed.equals(m)
                    + m.equals(peer.peerView()) + m.equals(null) + m.equals("map") + m.equals(Map.of());
        }

        private static String serial(MapSide side) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                    out.writeObject(side.map());
                }
                try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                    @SuppressWarnings("unchecked")
                    IterableMap<Key, Integer> copy = (IterableMap<Key, Integer>) in.readObject();
                    String text = copy.equals(side.map()) + "/" + copy.getClass().equals(side.map().getClass());
                    side.replace(copy);
                    return text + side.canonical();
                }
            } catch (IOException | ClassNotFoundException failure) {
                throw new IllegalStateException(failure);
            }
        }

        private static String toStringOp(Map<Key, Integer> m) {
            String text = m.toString();
            if (!text.startsWith("{") || !text.endsWith("}")) return "?" + text;
            String body = text.substring(1, text.length() - 1);
            List<String> parts = new ArrayList<>(body.isEmpty() ? List.of() : List.of(body.split(", ?")));
            Collections.sort(parts);
            return parts.size() + parts.toString();
        }

        private static String misuse(IterableMap<Key, Integer> m, Op op) {
            List<String> log = new ArrayList<>();
            log.add(probe(() -> m.entrySet().iterator().remove()));
            log.add(probe(() -> m.mapIterator().getKey()));
            log.add(probe(() -> m.mapIterator().getValue()));
            log.add(probe(() -> m.mapIterator().setValue(op.value)));
            log.add(probe(() -> {
                Iterator<Key> it = m.keySet().iterator();
                while (it.hasNext()) it.next();
                it.next();
            }));
            log.add(doubleRemove(m, op));
            return log + "" + m.size();
        }

        /** Removes every picked key and calls remove() a second time on each: order-independent. */
        private static String doubleRemove(IterableMap<Key, Integer> m, Op op) {
            List<String> seconds = new ArrayList<>();
            for (Iterator<Key> it = m.keySet().iterator(); it.hasNext();) {
                if (pick(it.next(), null, op.bits)) {
                    it.remove();
                    seconds.add(probe(it::remove));
                }
            }
            return sorted(seconds);
        }

        private static String views(IterableMap<Key, Integer> m, Op op) {
            Map.Entry<Key, Integer> probe = new AbstractMap.SimpleEntry<>(op.key, op.value);
            return "" + m.keySet().contains(op.key) + m.values().contains(op.value) + m.entrySet().contains(probe)
                    + m.keySet().size() + m.entrySet().size() + m.values().size() + m.keySet().remove(op.other)
                    + m.isEmpty() + m.keySet().isEmpty() + "/" + sorted(m.keySet());
        }

        private static String probe(Runnable action) {
            try {
                action.run();
                return "ok";
            } catch (RuntimeException failure) {
                return thrown(failure);
            }
        }
    }

    /** java.util.Map default methods (Commons and the facade both use the JDK defaults). */
    static final class Defaults {
        private Defaults() { }

        static String apply(MapSide side, Op op) {
            IterableMap<Key, Integer> m = side.map();
            Integer v = op.value;
            String out = switch (Integer.remainderUnsigned(op.bits, 11)) {
                case 0 -> String.valueOf(m.getOrDefault(op.key, -1));
                case 1 -> String.valueOf(m.putIfAbsent(op.key, v));
                case 2 -> String.valueOf(m.replace(op.key, v));
                case 3 -> String.valueOf(m.replace(op.key, op.next, v));
                case 4 -> String.valueOf(m.remove(op.key, v));
                case 5 -> String.valueOf(m.computeIfAbsent(op.key, k -> v));
                case 6 -> String.valueOf(m.computeIfPresent(op.key, (k, old) -> v));
                case 7 -> String.valueOf(m.compute(op.key, (k, old) -> old == null ? v : null));
                case 8 -> String.valueOf(m.merge(op.key, v, (a, b) -> a + b > VALUES ? null : a + b));
                case 9 -> forEach(m);
                default -> {
                    m.replaceAll((k, old) -> old == null ? op.next : Integer.valueOf((old + 1) % VALUES));
                    yield "replaced";
                }
            };
            return out + side.canonical();
        }

        private static String forEach(IterableMap<Key, Integer> m) {
            List<String> rows = new ArrayList<>();
            m.forEach((k, v) -> rows.add(k + "=" + v));
            return sorted(rows);
        }
    }

    /** The core over the shared subset: put/get/remove/contains/size/clear, cursor iteration, removeSlot, copy. */
    static final class CoreSide implements Side {
        private final List<String> order = new ArrayList<>();
        private jdk.m3.collections.Flat3Map<Key, Integer> map = new jdk.m3.collections.Flat3Map<>();

        @Override
        public Object peerView() { return map; }

        @Override
        public List<String> order() { return order; }

        @Override
        public String apply(Op op, Side peer) {
            order.clear();
            return applyOp(op) + " |" + map.size() + ":" + stateHash();
        }

        /** Map.hashCode formula over the core's cursor, so the per-op state stamp matches the Commons side. */
        private int stateHash() {
            int hash = 0;
            for (int s = map.nextSlot(0); s >= 0; s = map.nextSlot(s + 1)) {
                hash += java.util.Objects.hashCode(map.slotKey(s)) ^ java.util.Objects.hashCode(map.slotValue(s));
            }
            return hash;
        }

        private String applyOp(Op op) {
            try {
                return switch (op.code) {
                    case 0 -> String.valueOf(map.put(op.key, op.value));
                    case 1 -> String.valueOf(map.get(op.key));
                    case 2 -> String.valueOf(map.remove(op.key));
                    case 3 -> String.valueOf(map.containsKey(op.key));
                    case 4 -> String.valueOf(map.containsValue(op.value));
                    case 5 -> map.size() + "/" + map.isEmpty();
                    case 6 -> { map.clear(); yield canonical(); }
                    case 8 -> canonical();
                    case 9 -> sorted(column(true));
                    case 10 -> sorted(column(false));
                    case 12 -> keyIterRemove(op);
                    case 15 -> copy(op);
                    default -> throw new IllegalStateException("not in the shared subset: " + NAMES[op.code]);
                };
            } catch (RuntimeException failure) {
                return thrown(failure);
            }
        }

        private List<Object> column(boolean keys) {
            List<Object> items = new ArrayList<>();
            for (int s = map.nextSlot(0); s >= 0; s = map.nextSlot(s + 1)) {
                items.add(keys ? map.slotKey(s) : map.slotValue(s));
            }
            return items;
        }

        private String canonical() { return canonical(map); }

        private String canonical(jdk.m3.collections.Flat3Map<Key, Integer> source) {
            List<Map.Entry<Key, Integer>> entries = new ArrayList<>();
            for (int s = source.nextSlot(0); s >= 0; s = source.nextSlot(s + 1)) {
                entries.add(new AbstractMap.SimpleEntry<>(source.slotKey(s), source.slotValue(s)));
            }
            return M3Flat3MapDifferential.canonical(entries, source == map ? order : new ArrayList<>());
        }

        private String keyIterRemove(Op op) {
            List<String> log = new ArrayList<>();
            for (int s = map.nextSlot(0); s >= 0; ) {
                Key key = map.slotKey(s);
                if (pick(key, null, op.bits)) {
                    s = map.nextSlot(map.removeSlot(s));
                    log.add("-" + key);
                } else {
                    s = map.nextSlot(s + 1);
                }
            }
            Collections.sort(log);
            return log + canonical();
        }

        private String copy(Op op) {
            jdk.m3.collections.Flat3Map<Key, Integer> copy = map.copy();
            String text = canonical(copy) + true + true;
            boolean had = map.containsKey(op.other);
            int size = map.size();
            copy.put(op.other, op.next);
            copy.remove(op.key);
            text += "|" + (had == map.containsKey(op.other)) + (size == map.size());
            if ((op.bits & 1) == 1) map = copy;
            return text + canonical();
        }
    }

    /** SHA-256 of a side's records, optionally also written to a dump file. */
    static final class Digest implements AutoCloseable {
        private final MessageDigest sha;
        private final Writer writer;
        private long lines;

        Digest(Path dir, String name) throws IOException {
            try {
                sha = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException(impossible);
            }
            writer = dir == null ? null : Files.newBufferedWriter(dir.resolve(name), StandardCharsets.UTF_8);
        }

        void add(Op op, String record) {
            String line = op.index + " " + NAMES[op.code] + " " + record + "\n";
            sha.update(line.getBytes(StandardCharsets.UTF_8));
            lines++;
            if (writer == null) return;
            try {
                writer.write(line);
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }

        String hex() { return HexFormat.of().formatHex(sha.digest()); }

        @Override
        public void close() throws IOException {
            if (writer != null) writer.close();
        }
    }
}
