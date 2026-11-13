// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3SuffixArray;
import jdk.m3.tools.cpu.M3FenwickLong;
import java.util.Arrays;
import java.util.Random;

/** JDK String substring order/occurrence and independent primitive numeric model. */
public final class M3SuffixNumericContract {
    private static long checks;
    private M3SuffixNumericContract() { }
    private static void ok(boolean yes, String why) {
        checks++;
        if (!yes) throw new AssertionError(why + " at check " + checks);
    }
    private static void refuses(Runnable op) {
        try { op.run(); throw new AssertionError("missing refusal"); }
        catch (IllegalArgumentException | IndexOutOfBoundsException | ArithmeticException | NullPointerException expected) {
            checks++;
        }
    }
    private static CharSequence m3View(String source) {
        return new CharSequence() {
            @Override public int length() { return source.length(); }
            @Override public char charAt(int i) { return source.charAt(i); }
            @Override public CharSequence subSequence(int a, int b) { throw new AssertionError("slice forbidden"); }
            @Override public String toString() { throw new AssertionError("M3 view flattened"); }
        };
    }
    private static int[] occurrences(String source, String pattern) {
        int[] result = new int[source.length() + 1];
        int count = 0;
        for (int p = 0; p <= source.length();) {
            int at = source.indexOf(pattern, p);
            if (at < 0) break;
            result[count++] = at;
            p = at + 1;
        }
        return Arrays.copyOf(result, count);
    }
    private static void suffix(String source, Random rng) {
        M3SuffixArray index = M3SuffixArray.index(m3View(source));
        int n = source.length();
        Integer[] natural = new Integer[n];
        for (int i = 0; i < n; i++) natural[i] = i;
        Arrays.sort(natural, (a,b) -> source.substring(a).compareTo(source.substring(b)));
        int[] order = index.suffixOrder(), lcp = index.longestCommonPrefixes();
        ok(order.length == n && lcp.length == n, "suffix geometry");
        for (int i = 0; i < n; i++) {
            ok(order[i] == natural[i], "suffix lexical order");
            int expected = 0;
            if (i > 0) {
                String a = source.substring(order[i]), b = source.substring(order[i - 1]);
                while (expected < Math.min(a.length(), b.length()) && a.charAt(expected) == b.charAt(expected)) expected++;
            }
            ok(lcp[i] == expected, "Kasai LCP");
        }
        for (int p = 0; p < 5; p++) {
            String pattern;
            if (p == 0) pattern = "";
            else if (p == 1 && n != 0) {
                int a = rng.nextInt(n), b = a + rng.nextInt(n - a + 1);
                pattern = source.substring(a,b);
            } else if (p == 2) pattern = "\uffff";
            else if (p == 3) pattern = "\ud800";
            else pattern = String.valueOf((char)rng.nextInt(65536));
            int[] expected = occurrences(source, pattern);
            ok(index.count(m3View(pattern)) == expected.length, "substring count");
            ok(Arrays.equals(index.positions(m3View(pattern), n + 1), expected), "substring positions");
            if (expected.length > 0) refuses(() -> index.positions(m3View(pattern), expected.length - 1));
        }
        if (n > 0) {
            order[0] = -999;
            lcp[0] = -999;
            ok(index.suffixOrder()[0] >= 0 && index.longestCommonPrefixes()[0] >= 0,
                    "returned lanes detached");
        }
    }
    private static void numeric(Random rng) {
        int n = rng.nextInt(100);
        long[] seed = new long[n];
        for (int i = 0; i < n; i++) seed[i] = rng.nextInt(21) - 10;
        M3FenwickLong tree = new M3FenwickLong(seed);
        for (int step = 0; step < 600; step++) {
            int a = rng.nextInt(n + 1), b = a + rng.nextInt(n - a + 1);
            long expected = 0;
            for (int i = a; i < b; i++) expected += seed[i];
            ok(tree.range(a,b) == expected, "range sum oracle");
            long prefix = 0;
            for (int i = 0; i < b; i++) prefix += seed[i];
            ok(tree.prefix(b) == prefix, "prefix sum oracle");
            if (n > 0 && rng.nextInt(3) == 0) {
                int at = rng.nextInt(n), delta = rng.nextInt(41) - 20;
                tree.add(at, delta);
                seed[at] += delta;
                ok(tree.get(at) == seed[at], "point update");
            }
            if (n > 0 && rng.nextInt(10) == 0) {
                int at = rng.nextInt(n), value = rng.nextInt(1_000) - 500;
                tree.set(at, value);
                seed[at] = value;
                ok(tree.get(at) == seed[at], "set update");
            }
        }
        ok(Arrays.equals(tree.values(), seed), "snapshot detached");
    }
    public static void main(String[] args) {
        Random rng = new Random(0x4d33535546534958L);
        suffix("",rng);
        suffix("banana",rng);
        suffix("aaaaaaa",rng);
        suffix("\ud800\udc00\u0000\ud800\udfff\uffff",rng);
        for (int test = 0; test < 6_000; test++) {
            int n = rng.nextInt(64);
            StringBuilder source = new StringBuilder();
            for (int i = 0; i < n; i++) {
                int v = rng.nextInt(13);
                char ch = switch (v) {
                    case 0 -> '\u0000'; case 1 -> '\uffff'; case 2 -> '\ud800'; case 3 -> '\udfff';
                    case 4 -> 'z'; case 5 -> '\u00e9'; default -> (char)('a' + rng.nextInt(5));
                };
                source.append(ch);
            }
            suffix(source.toString(),rng);
        }
        for (int test = 0; test < 400; test++) numeric(rng);
        // Repeated-suffix adversary crosses the 64K UTF-16 and bitmap boundaries.
        String repeated = "x".repeat(65_537);
        M3SuffixArray large = M3SuffixArray.index(m3View(repeated));
        ok(large.count("xx") == 65_536, "large repeated index");
        int[] largeOrder = large.suffixOrder();
        ok(largeOrder[0] == 65_536 && largeOrder[largeOrder.length - 1] == 0,
                "long repeated suffix order");
        ok(Arrays.equals(large.positions("xxxxx", 65_537),
                java.util.stream.IntStream.range(0, 65_533).toArray()), "large matches");
        M3FenwickLong outside = new M3FenwickLong(
                new long[]{Long.MAX_VALUE, 0, Long.MAX_VALUE});
        ok(outside.range(3,3) == 0, "empty range independent of overflowing prefix");
        refuses(() -> outside.prefix(3));
        M3FenwickLong edge = new M3FenwickLong(new long[]{Long.MAX_VALUE, -1});
        ok(edge.range(0,1) == Long.MAX_VALUE, "max prefix");
        long[] prior = edge.values();
        refuses(() -> edge.add(0,1));
        ok(Arrays.equals(prior, edge.values()), "failure atomicity");
        refuses(() -> new M3FenwickLong(new long[]{Long.MAX_VALUE, Long.MAX_VALUE}));
        refuses(() -> edge.range(0,3));
        refuses(() -> edge.set(0, Long.MIN_VALUE));
        ok(Arrays.equals(prior,edge.values()), "rejected set detached");
        refuses(() -> M3SuffixArray.index(m3View("a".repeat(250_001))));
        refuses(() -> M3SuffixArray.index(null));
        refuses(() -> M3SuffixArray.index("ab").positions("a", -1));
        System.out.println("M3_WAVE6_TEXT_NUMERIC_PASS checks="+ checks);
    }
}
