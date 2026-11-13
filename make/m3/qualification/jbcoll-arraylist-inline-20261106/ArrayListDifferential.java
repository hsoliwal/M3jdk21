/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.SplittableRandom;
import java.util.function.Predicate;

/**
 * Differential driver of leaf jbcoll-arraylist-inline-20261106 (DIFF_ARRAYLIST v1).
 *
 * <p>One seeded operation stream over a java.util.ArrayList of identity-only elements. The stream covers the code the
 * hunk touches: removeIf on the whole list and on sub-lists, with spans in the inline-mask range (2..128), the scalar
 * deathRow range (129..511) and the M3 compactor range (512+). It also covers removeAll/retainAll (batchRemove) and
 * predicates that throw, that add (CME), or that set an element during removeIf. Iterator and sub-list epoch probes
 * check ConcurrentModificationException timing. Every operation prints one line: index, operation, arguments, result,
 * the predicate's callback count and the hash of the callback identities in call order, the list's size and the hash
 * of its element identities in order, and the probe outcome. The same driver runs on the stock JDK and on a patched
 * java.base (master ArrayList, leaf ArrayList); the outputs must be byte-identical.
 *
 * <p>RNG use per operation is fixed (eight draws); element identities derive from the operation index; the list is
 * re-populated every 128 operations from its own seed, so a divergence stays inside its epoch.
 *
 * <p>usage: java [--patch-module java.base=CLASSES] ArrayListDifferential SEED OPS OUT
 */
public final class ArrayListDifferential {
    private static final int EPOCH = 128;
    private static final int CAP = 1400;
    private static final int[] BOUNDARY = {0, 1, 62, 63, 64, 65, 126, 127, 128, 129, 255, 256, 510, 511, 512, 513};
    private static final String[] OPS = {
        "removeIf", "subRemoveIf", "removeAll", "retainAll", "subRemoveAll", "subRetainAll", "removeIfThrows",
        "removeIfAdds", "removeIfSets", "iteratorProbe", "subListProbe", "add", "addAll", "remove", "subClear"};
    private static final int[] WEIGHTS = {22, 14, 6, 6, 4, 4, 5, 5, 4, 8, 6, 6, 4, 4, 2};

    static final long[] BANDS = new long[4];

    private ArrayListDifferential() { }

    /** Identity-only element: equals/hashCode of Object; id is the printable identity witness. */
    static final class Item {
        final int id;
        final int key;

        Item(int id, int key) {
            this.id = id;
            this.key = key;
        }
    }

    /** Predicate pattern over (call ordinal, element); records the callback identities in call order. */
    static final class Pattern implements Predicate<Item> {
        final int kind;
        final int a;
        final int b;
        int calls;
        int firstTrue = -1;
        long trace = 0xcbf29ce484222325L;

        Pattern(int kind, int a, int b) {
            this.kind = kind;
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean test(Item item) {
            int ordinal = calls++;
            trace = (trace ^ item.id) * 0x100000001b3L;
            boolean verdict = switch (kind) {
                case 0 -> false;
                case 1 -> true;
                case 2 -> ordinal == a;
                case 3 -> ordinal % (1 + a % 9) == b % (1 + a % 9);
                case 4 -> ((item.id * 0x9E3779B1) >>> 24) < (a & 255);
                case 5 -> (ordinal / (1 + a % 70)) % 2 == 0;
                case 6 -> ordinal == BOUNDARY[a % BOUNDARY.length] || ordinal == BOUNDARY[b % BOUNDARY.length];
                case 7 -> ordinal >= a % 140;
                case 8 -> ordinal < a % 140;
                case 9 -> item.key % 3 == b % 3;
                default -> (ordinal & 63) == (a & 63);
            };
            if (verdict && firstTrue < 0) firstTrue = ordinal;
            return verdict;
        }

        /** Counts the removeIf span band (end - first rejected) this predicate drove: 0 none, 1..128, 129..511, 512+. */
        void band() {
            int span = firstTrue < 0 ? 0 : calls - firstTrue;
            BANDS[span == 0 ? 0 : span <= 128 ? 1 : span < 512 ? 2 : 3]++;
        }

        String args() {
            return "p" + kind + "/" + a + "/" + b;
        }

        String callbacks() {
            return "cb=" + calls + "/" + Long.toHexString(trace);
        }
    }

    static final class Draws {
        final int[] value = new int[8];

        void next(SplittableRandom random) {
            for (int i = 0; i < value.length; i++) value[i] = random.nextInt(1 << 24);
        }
    }

    static String state(List<Item> list) {
        long hash = 0xcbf29ce484222325L;
        int size = list.size();
        for (int i = 0; i < size; i++) hash = (hash ^ list.get(i).id) * 0x100000001b3L;
        return "n=" + size + " h=" + Long.toHexString(hash);
    }

    static int size(int draw) {
        int band = draw % 10;
        int r = draw >>> 4;
        if (band < 5) return r % 141;            // inline-mask spans
        if (band < 7) return 129 + r % 383;      // scalar deathRow spans
        return 480 + r % 700;                    // compactor spans
    }

    static ArrayList<Item> populate(long seed, int epoch) {
        SplittableRandom random = new SplittableRandom(seed ^ (epoch * 0x9E3779B97F4A7C15L));
        int n = size(random.nextInt(1 << 24));
        ArrayList<Item> list = new ArrayList<>();
        int base = 1_000_000_000 + epoch * 2000;
        for (int i = 0; i < n; i++) list.add(new Item(base + i, random.nextInt(1000)));
        return list;
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

    /** Elements picked by index from the list (state-dependent, fixed RNG use) for removeAll/retainAll. */
    static Set<Item> picks(List<Item> list, Draws draws) {
        Set<Item> set = Collections.newSetFromMap(new IdentityHashMap<>());
        int n = list.size();
        if (n == 0) return set;
        int count = draws.value[5] % 5 == 0 ? n : draws.value[5] % 97;
        int stride = 1 + draws.value[6] % 7;
        for (int i = 0, at = draws.value[7] % n; i < count && i < n; i++, at = (at + stride) % n) set.add(list.get(at));
        return set;
    }

    static int[] range(List<Item> list, Draws draws) {
        int n = list.size();
        int from = n == 0 ? 0 : draws.value[5] % (n + 1);
        int to = from + (n - from == 0 ? 0 : size(draws.value[6]) % (n - from + 1));
        return new int[] {from, to};
    }

    static String apply(ArrayList<Item> list, int op, Draws draws, int index) {
        Pattern p = new Pattern(draws.value[2] % 11, draws.value[3] % 1024, draws.value[4] % 1024);
        switch (OPS[op]) {
            case "removeIf":
                boolean removed = list.removeIf(p);
                p.band();
                return p.args() + " -> " + removed + " " + p.callbacks();
            case "subRemoveIf": {
                int[] r = range(list, draws);
                List<Item> sub = list.subList(r[0], r[1]);
                boolean changed = sub.removeIf(p);
                p.band();
                return r[0] + ".." + r[1] + " " + p.args() + " -> " + changed + " sub=" + sub.size() + " " + p.callbacks();
            }
            case "removeAll":
                return "-> " + list.removeAll(picks(list, draws));
            case "retainAll":
                return "-> " + list.retainAll(picks(list, draws));
            case "subRemoveAll": {
                int[] r = range(list, draws);
                List<Item> sub = list.subList(r[0], r[1]);
                return r[0] + ".." + r[1] + " -> " + sub.removeAll(picks(list, draws)) + " sub=" + sub.size();
            }
            case "subRetainAll": {
                int[] r = range(list, draws);
                List<Item> sub = list.subList(r[0], r[1]);
                return r[0] + ".." + r[1] + " -> " + sub.retainAll(picks(list, draws)) + " sub=" + sub.size();
            }
            case "removeIfThrows": {
                int at = list.isEmpty() ? 0 : draws.value[5] % (list.size() + 1);
                int[] seen = {0};
                RuntimeException marker = new IllegalStateException("marker");
                try {
                    list.removeIf(item -> {
                        if (seen[0]++ == at) throw marker;
                        return p.test(item);
                    });
                    return "at=" + at + " " + p.args() + " -> returned " + p.callbacks();
                } catch (IllegalStateException thrown) {
                    return "at=" + at + " " + p.args() + " -> thrown=" + (thrown == marker) + " seen=" + seen[0]
                            + " " + p.callbacks();
                }
            }
            case "removeIfAdds": {
                int at = list.isEmpty() ? 0 : draws.value[5] % list.size();
                Item added = new Item(index * 16, 7);
                int[] seen = {0};
                try {
                    list.removeIf(item -> {
                        if (seen[0]++ == at) list.add(added);
                        return p.test(item);
                    });
                    return "at=" + at + " " + p.args() + " -> returned " + p.callbacks();
                } catch (ConcurrentModificationException cme) {
                    return "at=" + at + " " + p.args() + " -> CME seen=" + seen[0] + " " + p.callbacks();
                }
            }
            case "removeIfSets": {
                int at = list.isEmpty() ? 0 : draws.value[5] % list.size();
                int slot = list.isEmpty() ? 0 : draws.value[6] % list.size();
                Item replacement = new Item(index * 16 + 1, 5);
                int[] seen = {0};
                boolean changed = list.removeIf(item -> {
                    if (seen[0]++ == at) list.set(slot, replacement);
                    return p.test(item);
                });
                return "at=" + at + " slot=" + slot + " " + p.args() + " -> " + changed + " " + p.callbacks();
            }
            case "iteratorProbe": {
                Iterator<Item> it = list.iterator();
                int advance = list.isEmpty() ? 0 : draws.value[5] % Math.min(3, list.size());
                for (int i = 0; i < advance; i++) it.next();
                boolean changed = list.removeIf(p);
                p.band();
                String probe;
                try {
                    probe = it.hasNext() ? "next=" + it.next().id : "end";
                } catch (ConcurrentModificationException cme) {
                    probe = "CME";
                }
                return p.args() + " -> " + changed + " " + p.callbacks() + " probe=" + probe;
            }
            case "subListProbe": {
                int[] r = range(list, draws);
                List<Item> sub = list.subList(r[0], r[1]);
                boolean changed = list.removeIf(p);
                p.band();
                String probe;
                try {
                    probe = "size=" + sub.size();
                } catch (ConcurrentModificationException cme) {
                    probe = "CME";
                }
                return r[0] + ".." + r[1] + " " + p.args() + " -> " + changed + " " + p.callbacks() + " probe=" + probe;
            }
            case "add": {
                if (list.size() >= CAP) return "-> skip";
                int at = draws.value[5] % (list.size() + 1);
                list.add(at, new Item(index * 16 + 2, draws.value[6] % 1000));
                return at + " -> void";
            }
            case "addAll": {
                if (list.size() >= CAP) return "-> skip";
                int count = draws.value[5] % 200;
                List<Item> more = new ArrayList<>(count);
                for (int i = 0; i < count; i++) more.add(new Item(index * 16 + 3 + i * 1_048_576, i % 1000));
                int at = draws.value[6] % (list.size() + 1);
                return at + "+" + count + " -> " + list.addAll(at, more);
            }
            case "remove": {
                if (list.isEmpty()) return "-> empty";
                int at = draws.value[5] % list.size();
                if ((draws.value[6] & 1) == 0) return at + " -> " + list.remove(at).id;
                return at + " -> " + list.remove(list.get(at));
            }
            case "subClear": {
                int[] r = range(list, draws);
                list.subList(r[0], r[1]).clear();
                return r[0] + ".." + r[1] + " -> void";
            }
            default:
                throw new AssertionError(op);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("usage: SEED OPS OUT");
        long seed = Long.decode(args[0]);
        int ops = Integer.parseInt(args[1]);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        SplittableRandom random = new SplittableRandom(seed);
        Draws draws = new Draws();
        ArrayList<Item> list = null;
        long callbacks = 0;
        long cmes = 0;
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(Path.of(args[2])), StandardCharsets.UTF_8), 1 << 16)) {
            for (int index = 0; index < ops; index++) {
                if (index % EPOCH == 0) list = populate(seed, index / EPOCH);
                draws.next(random);
                int op = pickOp(draws.value[0]);
                String result;
                try {
                    result = apply(list, op, draws, index);
                } catch (RuntimeException failure) {
                    result = "-> " + failure.getClass().getSimpleName();
                }
                if (result.contains("CME")) cmes++;
                int cb = result.indexOf("cb=");
                if (cb >= 0) callbacks += Long.parseLong(result.substring(cb + 3, result.indexOf('/', cb)));
                String line = index + " " + OPS[op] + " " + result + " | " + state(list) + "\n";
                out.write(line);
                sha.update(line.getBytes(StandardCharsets.UTF_8));
            }
        }
        System.out.println("DIFF_ARRAYLIST seed=" + args[0] + " ops=" + ops + " callbacks=" + callbacks + " cme="
                + cmes + " removeIf_spans none/inline/scalar/compactor=" + BANDS[0] + "/" + BANDS[1] + "/" + BANDS[2]
                + "/" + BANDS[3] + " sha256=" + HexFormat.of().formatHex(sha.digest()));
    }
}
