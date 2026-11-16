/*
 * @test
 * @summary PackedBooleanList reverse word search and membership preserve logical size
 * @modules jdk.m3.collections
 * @run main PackedBooleanReverseSearchTest
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jdk.m3.collections.packed.PackedBooleanList;

public class PackedBooleanReverseSearchTest {
    private static long checks;

    private static void same(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    private static int lastIndex(List<Boolean> values, boolean wanted) {
        for (int i = values.size() - 1; i >= 0; i--) {
            if (values.get(i) == wanted) return i;
        }
        return -1;
    }

    private static int firstIndex(List<Boolean> values, boolean wanted) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == wanted) return i;
        }
        return -1;
    }

    private static void verify(PackedBooleanList list, List<Boolean> reference) {
        same(list.size() == reference.size(), "size");
        for (boolean wanted : new boolean[] {false, true}) {
            int first = firstIndex(reference, wanted);
            same(list.indexOf(wanted) == first, "first index " + wanted);
            same(list.contains(wanted) == (first >= 0), "contains " + wanted);
            same(list.lastIndexOf(wanted) == lastIndex(reference, wanted),
                    "last index " + wanted + " size=" + reference.size());
        }
        for (int i = 0; i < reference.size(); i++) {
            same(list.get(i) == reference.get(i), "value at " + i);
        }
    }

    private static void run(int length, int pattern) {
        Random random = new Random(0x7296a9dL + 31L * length + pattern);
        PackedBooleanList bits = new PackedBooleanList(length + 70);
        List<Boolean> expected = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            boolean value = switch (pattern) {
                case 0 -> false;
                case 1 -> true;
                case 2 -> (i & 1) == 0;
                case 3 -> i == 0 || i == length - 1;
                case 4 -> i % 63 == 0;
                case 5 -> i % 64 == 0;
                case 6 -> i % 65 == 0;
                default -> random.nextBoolean();
            };
            bits.add(value);
            expected.add(value);
        }
        verify(bits, expected);
        if (length > 0) {
            bits.set(0, !expected.get(0));
            expected.set(0, !expected.get(0));
            verify(bits, expected);
            int mid = expected.size() / 2;
            bits.removeAt(mid);
            expected.remove(mid);
            verify(bits, expected);
            bits.add(mid, true);
            expected.add(mid, true);
            verify(bits, expected);
        }
        bits.trimToSize();
        verify(bits, expected);
        bits.clear();
        expected.clear();
        verify(bits, expected);
    }

    private static void randomMutations() {
        for (int seed = 0; seed < 32; seed++) {
            Random random = new Random(0x64b001eaL + seed);
            PackedBooleanList bits = new PackedBooleanList(seed & 3);
            List<Boolean> expected = new ArrayList<>();
            for (int step = 0; step < 500; step++) {
                int op = random.nextInt(5);
                if (op == 0 || expected.isEmpty()) {
                    boolean v = random.nextBoolean();
                    int i = random.nextInt(expected.size() + 1);
                    bits.add(i, v);
                    expected.add(i, v);
                } else if (op == 1) {
                    int i = random.nextInt(expected.size());
                    boolean v = random.nextBoolean();
                    same(bits.set(i, v) == expected.set(i, v), "set result");
                } else if (op == 2) {
                    int i = random.nextInt(expected.size());
                    same(bits.removeAt(i) == expected.remove(i), "remove result");
                } else if (op == 3) {
                    boolean v = random.nextBoolean();
                    int i = expected.indexOf(v);
                    same(bits.remove(v) == (i >= 0), "remove(value)");
                    if (i >= 0) expected.remove(i);
                } else if (op == 4 && step % 17 == 0) {
                    bits.trimToSize();
                }
                verify(bits, expected);
            }
        }
    }

    public static void main(String[] args) {
        int[] lengths = {0, 1, 2, 31, 32, 63, 64, 65, 66, 127, 128, 129,
                         130, 191, 192, 193, 255, 256, 257, 1023, 1024, 1025, 4096};
        for (int length : lengths) {
            for (int pattern = 0; pattern < 9; pattern++) {
                run(length, pattern);
            }
        }
        randomMutations();
        System.out.println("PackedBooleanReverseSearchTest PASS checks=" + checks);
    }
}
