// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3AhoCorasick;
import jdk.m3.tools.cpu.M3BipartiteMatching;
import java.util.Arrays;
import java.util.Random;

/** Brute-force max-matching and JDK String.indexOf independent algorithm oracles. */
public final class M3MatchingSearchContract {
    private static int checks;
    private M3MatchingSearchContract() { }
    private static void ok(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason + " check=" + checks);
        checks++;
    }
    private static int brute(int at, int left, int[] offsets, int[] targets, boolean[] used) {
        if (at == left) return 0;
        int best = brute(at + 1, left, offsets, targets, used);
        for (int e = offsets[at]; e < offsets[at + 1]; e++) {
            int v = targets[e];
            if (!used[v]) {
                used[v] = true;
                best = Math.max(best, 1 + brute(at + 1, left, offsets, targets, used));
                used[v] = false;
            }
        }
        return best;
    }
    private static void matching(int l, int r, int[] offsets, int[] edges) {
        var result = M3BipartiteMatching.maximum(l, r, offsets, edges);
        int[] a = result.leftToRight(), b = result.rightToLeft();
        ok(a.length == l && b.length == r, "matching shape");
        int count = 0;
        for (int u = 0; u < l; u++) if (a[u] >= 0) {
            ok(a[u] < r && b[a[u]] == u, "forward/reverse mates");
            boolean edge = false;
            for (int k = offsets[u]; k < offsets[u + 1]; k++) if (edges[k] == a[u]) edge = true;
            ok(edge, "missing matching edge");
            count++;
        }
        for (int v = 0; v < r; v++) if (b[v] >= 0)
            ok(b[v] < l && a[b[v]] == v, "reverse/forward mates");
        ok(count == result.size(), "matching cardinality");
        if (l <= 7 && r <= 7) ok(count == brute(0, l, offsets, edges, new boolean[r]), "optimality");
        if (a.length > 0) {
            a[0] = 999;
            ok(!Arrays.equals(a, result.leftToRight()), "matching result detached");
        }
    }
    private static CharSequence view(String original) {
        return new CharSequence() {
            @Override public int length() { return original.length(); }
            @Override public char charAt(int index) { return original.charAt(index); }
            @Override public CharSequence subSequence(int start, int end) {
                return view(original.substring(start, end));
            }
            @Override public String toString() { throw new AssertionError("UTF-16 M3 view flattened"); }
        };
    }
    private static void search(String value, String[] patterns) {
        CharSequence[] views = new CharSequence[patterns.length];
        for (int p = 0; p < patterns.length; p++) views[p] = view(patterns[p]);
        var compiled = M3AhoCorasick.compile(views);
        int[] found = compiled.firstOffsets(view(value));
        ok(found.length == patterns.length, "pattern count");
        for (int p = 0; p < patterns.length; p++)
            ok(found[p] == value.indexOf(patterns[p]), "indexOf parity");
        ok(Arrays.equals(found, compiled.firstOffsets(view(value))), "immutable plan reuse");
        if (found.length > 0) {
            found[0] = Integer.MAX_VALUE;
            ok(!Arrays.equals(found, compiled.firstOffsets(view(value))), "detached query results");
        }
    }
    private static void refuses(Runnable fn) {
        try { fn.run(); throw new AssertionError("expected argument refusal"); }
        catch (IllegalArgumentException | NullPointerException expected) { checks++; }
    }
    public static void main(String[] args) {
        matching(0, 0, new int[]{0}, new int[]{});
        matching(2, 3, new int[]{0, 2, 3}, new int[]{0, 1, 1});
        Random rng = new Random(0x4D334D415443484CL);
        for (int trial = 0; trial < 8_000; trial++) {
            int l = rng.nextInt(8), r = rng.nextInt(8);
            int[] offsets = new int[l + 1], edges = new int[l * (r + 1)];
            int at = 0;
            for (int u = 0; u < l; u++) {
                int count = r == 0 ? 0 : rng.nextInt(r + 2);
                for (int j = 0; j < count; j++) edges[at++] = rng.nextInt(r);
                offsets[u + 1] = at;
            }
            matching(l, r, offsets, Arrays.copyOf(edges, at));
        }
        int n = 768;
        int[] off = new int[n+1], edge = new int[n * 3];
        for (int i = 0; i < n; i++) {
            edge[3*i] = i;
            edge[3*i+1] = (i + 1) % n;
            edge[3*i+2] = (i + n - 1) % n;
            off[i+1] = 3*(i+1);
        }
        matching(n,n,off,edge);
        search("", new String[]{"", "a", "\ud800"});
        search("a\0b\r\n\ud800\udc00", new String[]{"a\0", "\0b", "\r\n", "\ud800", "\udc00", "", "missing"});
        char[] alphabet = {'a','b','c', '\0','\u00e9','\ud800','\udc00','\n'};
        for (int trial = 0; trial < 12_000; trial++) {
            int size = rng.nextInt(70);
            StringBuilder input = new StringBuilder();
            for (int j = 0; j < size; j++) input.append(alphabet[rng.nextInt(alphabet.length)]);
            String text = input.toString();
            int count = rng.nextInt(12);
            String[] patterns = new String[count];
            for (int p = 0; p < count; p++) {
                if (rng.nextBoolean() && !text.isEmpty()) {
                    int start = rng.nextInt(text.length());
                    patterns[p] = text.substring(start, start + rng.nextInt(text.length()-start+1));
                } else {
                    StringBuilder word = new StringBuilder();
                    for (int j = rng.nextInt(12); j > 0; j--) word.append(alphabet[rng.nextInt(alphabet.length)]);
                    patterns[p] = word.toString();
                }
            }
            search(text, patterns);
        }
        search("x".repeat(70_000), new String[]{"x".repeat(65_536)});
        refuses(() -> M3BipartiteMatching.maximum(1, 1, new int[]{0, 1}, new int[]{2}));
        refuses(() -> M3BipartiteMatching.maximum(1, 1, new int[]{0, -1}, new int[]{}));
        refuses(() -> M3AhoCorasick.compile(new CharSequence[]{"x".repeat(65_537)}));
        refuses(() -> M3AhoCorasick.compile(new CharSequence[]{null}));
        refuses(() -> M3AhoCorasick.compile(new CharSequence[]{"a"}).firstOffsets(view("a".repeat(8_000_001))));
        System.out.println("M3_WAVE5_MATCHING_SEARCH_PASS checks=" + checks);
    }
}
