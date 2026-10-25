/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: GPL-2.0-only
 */
/*
 * @test
 * @summary CopyOnWrite bulk removal preserves snapshot, range and callback contracts at word boundaries
 * @run main/othervm M3CopyOnWriteWordMaskTest
 * @run main/othervm -Xint M3CopyOnWriteWordMaskTest
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

public final class M3CopyOnWriteWordMaskTest {
    private static long checks;
    private static final Object SHARED = "shared";

    public static void main(String[] args) throws Exception {
        for (int n : new int[] {0,1,2,31,32,33,63,64,65,66,127,128,129,512}) {
            for (int mode = 0; mode < 4; mode++) {
                for (int shape = 0; shape < 8; shape++) {
                    boolean[] remove = new boolean[n];
                    for (int i = 0; i < n; i++) remove[i] = switch (shape) {
                        case 0 -> false;
                        case 1 -> true;
                        case 2 -> i == 0;
                        case 3 -> i == n - 1;
                        case 4 -> (i & 1) == 0;
                        case 5 -> i >= n - 64;
                        case 6 -> i >= n - 65;
                        default -> i % 7 == 0;
                    };
                    differential(remove, mode);
                }
            }
        }
        for (int bits = 0; bits < 65536; bits++) {
            boolean[] remove = new boolean[64 + (bits & 1)];
            for (int i = 0; i < remove.length; i++) remove[i] = (bits & (1 << (i & 15))) != 0;
            differential(remove, bits & 3);
        }
        Random random = new Random(0x4d33434f57574f52L);
        for (int trial = 0; trial < 2000; trial++) {
            boolean[] remove = new boolean[random.nextInt(300)];
            for (int i = 0; i < remove.length; i++) remove[i] = random.nextBoolean();
            differential(remove, trial & 3);
        }
        failuresAndReentrancy();
        filteredAndSetClients();
        cloneAndSerialization();
        System.out.println("M3CopyOnWriteWordMaskTest PASS checks=" + checks);
    }

    private static void differential(boolean[] remove, int mode) {
        int prefix = mode == 0 ? 0 : 7;
        int suffix = mode == 0 ? 0 : 9;
        Object[] input = new Object[prefix + remove.length + suffix];
        for (int i = 0; i < input.length; i++) input[i] = i % 11 == 0 ? null
                : i % 7 == 0 ? SHARED : new String("value" + (i % 5));
        CopyOnWriteArrayList<Object> owner = new CopyOnWriteArrayList<>(input);
        List<Object> view = switch (mode) {
            case 0 -> owner;
            case 1 -> owner.subList(prefix, prefix + remove.length);
            case 2 -> owner.subList(prefix, prefix + remove.length).reversed();
            default -> owner.subList(prefix - 1, prefix + remove.length + 1)
                    .subList(1, remove.length + 1);
        };
        Iterator<Object> snapshot = owner.iterator();
        var split = owner.spliterator();
        ArrayList<Object> expected = new ArrayList<>();
        boolean changed = false;
        for (int i = 0; i < input.length; i++) {
            boolean drop = i >= prefix && i < prefix + remove.length && remove[i - prefix];
            if (drop) changed = true; else expected.add(input[i]);
        }
        int[] calls = {0};
        boolean actual = view.removeIf(value -> {
            int i = calls[0]++;
            // Reversed COW views delegate bulk removal in the owner's physical order.
            check(value == input[prefix + i], "predicate occurrence/order");
            return remove[i];
        });
        check(actual == changed, "change flag");
        check(calls[0] == remove.length, "one callback per range occurrence");
        identical(expected, owner, "owner state");
        ArrayList<Object> expectedView = new ArrayList<>(expected.subList(prefix, expected.size() - suffix));
        if (mode == 2) expectedView = new ArrayList<>(expectedView.reversed());
        identical(expectedView, view, "live range state");
        ArrayList<Object> captured = new ArrayList<>();
        snapshot.forEachRemaining(captured::add);
        identical(Arrays.asList(input), captured, "old iterator snapshot");
        captured.clear();
        split.forEachRemaining(captured::add);
        identical(Arrays.asList(input), captured, "old spliterator snapshot");
    }

    private static void failuresAndReentrancy() {
        for (int n : new int[] {1,2,63,64,65,128,129}) {
            List<Integer> seed = new ArrayList<>();
            for (int i = 0; i < n; i++) seed.add(i);
            for (int fail : new int[] {0, n / 2, n - 1}) {
                CopyOnWriteArrayList<Integer> owner = new CopyOnWriteArrayList<>(seed);
                RuntimeException marker = new IllegalStateException("marker");
                int[] calls = {0};
                try {
                    owner.removeIf(value -> {
                        if (calls[0]++ == fail) throw marker;
                        return true;
                    });
                    throw new AssertionError("predicate exception missing");
                } catch (RuntimeException ex) { check(ex == marker, "same predicate failure"); }
                identical(seed, owner, "failure before publication");
                check(calls[0] == fail + 1, "failure callback cutoff");
            }
            for (boolean delete : new boolean[] {false, true}) {
                CopyOnWriteArrayList<Integer> owner = new CopyOnWriteArrayList<>(seed);
                int[] calls = {0};
                try {
                    owner.removeIf(value -> {
                        if (calls[0]++ == 0) owner.add(-1);
                        return delete;
                    });
                    throw new AssertionError("reentrant modification accepted");
                } catch (ConcurrentModificationException expected) {
                    check(calls[0] == n, "reentrant callback tape preserved");
                    check(owner.equals(concat(seed, -1)), "only reentrant change survives");
                }
            }
            CopyOnWriteArrayList<Integer> owner = new CopyOnWriteArrayList<>(seed);
            List<Integer> stale = owner.subList(0, n);
            owner.add(-1);
            int[] calls = {0};
            try {
                stale.removeIf(value -> { calls[0]++; return true; });
                throw new AssertionError("stale sublist accepted");
            } catch (ConcurrentModificationException expected) { check(calls[0] == 0, "stale before callback"); }
        }
    }

    private static List<Integer> concat(List<Integer> seed, int tail) {
        List<Integer> result = new ArrayList<>(seed);
        result.add(tail);
        return result;
    }

    private static void filteredAndSetClients() {
        for (int n : new int[] {1,63,64,65,127,128,129}) {
            List<Integer> seed = new ArrayList<>();
            for (int i = 0; i < n; i++) seed.add(i);
            for (int operation = 0; operation < 4; operation++) {
                CopyOnWriteArrayList<Integer> owner = new CopyOnWriteArrayList<>(seed);
                Collection<Integer> filtered = CollectionViews.filtered(owner, i -> (i & 1) == 0);
                Predicate<Integer> remove = switch (operation) {
                    case 0 -> i -> i % 3 == 0;
                    case 1 -> i -> List.of(0, 62, 64, 128).contains(i);
                    case 2 -> i -> !List.of(0, 62, 64, 128).contains(i);
                    default -> i -> true;
                };
                List<Integer> expected = new ArrayList<>(seed);
                expected.removeIf(i -> (i & 1) == 0 && remove.test(i));
                switch (operation) {
                    case 0 -> filtered.removeIf(remove);
                    case 1 -> filtered.removeAll(List.of(0, 62, 64, 128));
                    case 2 -> filtered.retainAll(List.of(0, 62, 64, 128));
                    default -> filtered.clear();
                }
                check(owner.equals(expected), "filtered bulk client");
                CopyOnWriteArraySet<Integer> set = new CopyOnWriteArraySet<>(seed);
                set.removeIf(i -> (i & 1) == 0 && remove.test(i));
                check(new ArrayList<>(set).equals(expected), "COW set client");
            }
        }
    }

    private static void cloneAndSerialization() throws Exception {
        CopyOnWriteArrayList<Integer> owner = new CopyOnWriteArrayList<>(Arrays.asList(1, null, 2, 3));
        Object clone = owner.clone();
        owner.removeIf(value -> value != null && value % 2 == 1);
        check(clone.equals(Arrays.asList(1, null, 2, 3)), "clone independent");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(owner); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            check(owner.equals(in.readObject()), "serialized contents");
        }
    }

    private static void identical(List<?> expected, List<?> actual, String why) {
        check(expected.size() == actual.size(), why + " length");
        for (int i = 0; i < expected.size(); i++) check(expected.get(i) == actual.get(i), why + " identity");
    }

    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why + " check=" + checks);
    }
}
