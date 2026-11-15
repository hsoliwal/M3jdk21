/*
 * @test
 * @summary Verify sparse-set membership and dense order after clear, reinsertion and swap deletion.
 * @modules jdk.m3.collections/jdk.m3.collections
 * @run main/othervm IntSparseSetClearTest
 */
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.collections.IntSparseSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Seeded, independent dense-order model, including clear/reinsert and swap-delete. */
public final class IntSparseSetClearTest {
    private static long checks;
    private static long fingerprint;

    private static final class Model {
        final ArrayList<Integer> entries = new ArrayList<>();
        final Map<Integer, Integer> positions = new HashMap<>();
        boolean add(int key) {
            if (positions.containsKey(key)) return false;
            positions.put(key, entries.size());
            entries.add(key);
            return true;
        }
        boolean remove(int key) {
            Integer index = positions.remove(key);
            if (index == null) return false;
            int tail = entries.size() - 1;
            int displaced = entries.get(tail);
            if (index != tail) {
                entries.set(index, displaced);
                positions.put(displaced, index);
            }
            entries.remove(tail);
            return true;
        }
        void clear() { entries.clear(); positions.clear(); }
        int[] snapshot() { return entries.stream().mapToInt(Integer::intValue).toArray(); }
    }

    private static void equal(boolean result, String label) {
        checks++;
        if (!result) throw new AssertionError(label);
    }

    public static void main(String[] args) {
        int rounds = args.length == 0 ? 18_000 : Integer.parseInt(args[0]);
        int transitions = 0;
        for (int capacity : new int[] {0, 1, 2, 8, 31, 256}) {
            var set = new IntSparseSet(capacity, capacity);
            var model = new Model();
            var random = new Random(0x335350415253454CL ^ capacity);
            for (int n = 0; n < rounds; n++) {
                int key = random.nextInt(1024);
                switch (random.nextInt(9)) {
                    case 0, 1, 2 -> equal(set.add(key) == model.add(key), "add");
                    case 3, 4 -> equal(set.remove(key) == model.remove(key), "remove");
                    case 5 -> equal(set.contains(key) == model.positions.containsKey(key), "contains");
                    case 6 -> {
                        set.clear(); model.clear();
                        equal(set.isEmpty() && set.toArray().length == 0, "clear");
                        equal(!set.contains(key), "no old membership");
                        equal(set.add(key) == model.add(key), "reinsert");
                    }
                    case 7 -> equal(!set.remove(-1) && !set.contains(-1), "negative");
                    default -> equal(set.payloadBytes() >= 8, "payload");
                }
                if ((n & 31) == 0 || n % 97 == 0) {
                    int[] actual = set.toArray();
                    int[] expected = model.snapshot();
                    equal(Arrays.equals(actual, expected), "ordered dense array");
                    equal(set.size() == expected.length, "size");
                    equal(set.isEmpty() == (expected.length == 0), "empty");
                    for (int i = 0; i < actual.length; i++) {
                        equal(set.get(i) == expected[i], "get");
                        fingerprint = Long.rotateLeft(fingerprint ^ actual[i], 9);
                    }
                    int probe = random.nextInt(1024);
                    equal(set.contains(probe) == model.positions.containsKey(probe), "stale-index guard");
                }
                transitions++;
            }
        }
        System.out.printf("INT_SPARSE_CLEAR_REPLAY_PASS transitions=%d checks=%d digest=%016x%n", transitions, checks, fingerprint);
    }
}
