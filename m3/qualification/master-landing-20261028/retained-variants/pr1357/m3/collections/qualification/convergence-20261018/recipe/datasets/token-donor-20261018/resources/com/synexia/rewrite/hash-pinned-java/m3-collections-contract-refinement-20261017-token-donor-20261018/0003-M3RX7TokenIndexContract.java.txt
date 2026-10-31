/*
 * @test
 * @summary Composite DFA transitions over packed tokens and disjoint UTF-16 chunks
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm M3RX7TokenIndexContract
 */
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import jdk.internal.mindex.M3RX7;

public class M3RX7TokenIndexContract {
    private static int verified;
    private static final String ROOT = "0123456789abcdef".repeat(4);

    private static final class NoFlatten implements CharSequence {
        private final String source;
        NoFlatten(String source) { this.source = source; }
        @Override public int length() { return source.length(); }
        @Override public char charAt(int at) { return source.charAt(at); }
        @Override public CharSequence subSequence(int first, int last) {
            throw new AssertionError("do not materialize subSequence");
        }
        @Override public String toString() { throw new AssertionError("do not flatten"); }
    }

    private static void equal(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("expected=" + expected + ", actual=" + actual);
        }
        verified++;
    }

    private static void rejected(Runnable task) {
        try { task.run(); } catch (RuntimeException expected) { verified++; return; }
        throw new AssertionError("expected validation failure");
    }

    private static M3RX7 prepare() {
        // DFA for a*b, plus a useless but non-dead x sink state.
        short[] transitions = new short[3 * 128];
        Arrays.fill(transitions, (short) -1);
        transitions['a'] = 0;
        transitions['b'] = 1;
        transitions['x'] = 2;
        transitions[2 * 128 + 'x'] = 2;
        return M3RX7.fromAsciiDfa(ROOT, 0, transitions, new byte[]{0, 1, 0});
    }

    public static void main(String[] args) {
        M3RX7 dfa = prepare();
        String[] tokens = {"a", "aa", "b", "ab", "x", "ba", "aab", "xx", "aaa", "bb"};
        int[] starts = new int[tokens.length], lengths = new int[tokens.length];
        int count = 0;
        for (int i = 0; i < tokens.length; i++) {
            starts[i] = count;
            lengths[i] = tokens[i].length();
            count += lengths[i];
        }
        byte[] packed = new byte[count];
        for (int i = 0; i < tokens.length; i++) {
            byte[] bytes = tokens[i].getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(bytes, 0, packed, starts[i], bytes.length);
        }
        M3RX7.TokenIndex index = dfa.prepareTokenIndex(packed, starts, lengths);
        if (index == null) throw new AssertionError("unexpected budget refusal");
        equal(10, index.tokenCount());
        equal(1, index.allowedMaskWords());
        equal(0, index.initialState());
        // x reaches a nonaccepting sink, so it is disallowed despite having a DFA edge.
        equal((1L << 0) | (1L << 1) | (1L << 2) | (1L << 3)
                | (1L << 6) | (1L << 8), index.allowedMaskWord(0, 0));
        equal(2, index.nextState(0, 4));
        equal(-1, index.nextState(0, 5));
        equal(true, index.matches(new int[]{1, 3}));
        equal(true, index.matches(new int[]{1, 2}));
        equal(false, index.matches(new int[]{2, 0}));
        equal(true, index.isFinal(index.nextState(0, 2)));
        equal(0L, index.allowedMaskWord(2, 0));

        // Token partitions are irrelevant to the composed DFA truth.
        Random random = new Random(0x3A5_2026L);
        int[] legal = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int trial = 0; trial < 12_000; trial++) {
            int[] ids = new int[random.nextInt(12)];
            StringBuilder text = new StringBuilder();
            for (int k = 0; k < ids.length; k++) {
                ids[k] = legal[random.nextInt(legal.length)];
                text.append(tokens[ids[k]]);
            }
            equal(dfa.matchesCandidate(text), index.matches(ids) ? 1 : 0);
            equal(dfa.matchesCandidate(text), dfa.matchesCandidate(new NoFlatten(text.toString())));
            int split = text.length() == 0 ? 0 : random.nextInt(text.length() + 1);
            CharSequence[] pieces = {new NoFlatten("_" + text.substring(0, split)),
                    new NoFlatten(text.substring(split) + "_")};
            equal(dfa.matchesCandidate(text), dfa.matchesChunksCandidate(pieces,
                    new int[]{1, 0}, new int[]{split, text.length() - split}));
        }
        // UTF-16 non-ASCII refuses; never silently decides Java regex truth.
        equal(-1, dfa.matchesCandidate(new NoFlatten("a\ud83d\ude00b")));
        equal(-1, dfa.matchesChunksCandidate(
                new CharSequence[]{new NoFlatten("a"), new NoFlatten("\ud83d\ude00b")},
                new int[]{0, 0}, new int[]{1, 3}));
        rejected(() -> dfa.matchesChunksCandidate(new CharSequence[]{"ab"}, new int[]{1}, new int[]{3}));
        rejected(() -> dfa.prepareTokenIndex(packed, starts, new int[0]));
        rejected(() -> dfa.prepareTokenIndex(new byte[]{(byte) 0xc3}, new int[]{0}, new int[]{1}));
        rejected(() -> dfa.prepareTokenIndex(new byte[]{'a'}, new int[]{0}, new int[]{0}));
        rejected(() -> index.nextState(0, 10));
        rejected(() -> index.allowedMaskWord(0, 1));
        // The index has no vocabulary/payload owner: subsequent caller mutations are irrelevant.
        Arrays.fill(packed, (byte) 'x');
        Arrays.fill(starts, 0);
        equal(true, index.matches(new int[]{1, 3}));
        System.out.println("M3RX7TokenIndexContract PASS checks=" + verified);
    }
}
