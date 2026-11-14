/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary Token-symbol DFAs reuse RX7 primitive rows, masks and page continuation
 * @modules java.base/jdk.internal.mindex
 *          jdk.compiler/com.sun.tools.javac.parser
 *          jdk.compiler/com.sun.tools.javac.util
 * @run main/othervm --add-opens=java.base/jdk.internal.mindex=ALL-UNNAMED M3RX7TokenSymbolsContract
 */
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.util.Context;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import jdk.internal.mindex.M3RX7;

public class M3RX7TokenSymbolsContract {
    private static final String PROGRAM = "0123456789abcdef".repeat(4);
    private static final String ALPHABET = "fedcba9876543210".repeat(4);
    private static long checks;
    private static int nativeCalls;

    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected=" + expected + ", actual=" + actual);
        }
        checks++;
    }

    private static void rejects(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable error) {
            if (!type.isInstance(error)) throw new AssertionError("wrong refusal", error);
            checks++;
            return;
        }
        throw new AssertionError("missing refusal: " + type);
    }

    private static M3RX7.TokenIndex index(int symbols, short[] rows, byte[] flags) {
        M3RX7.TokenIndex result = M3RX7.fromTokenDfa(PROGRAM, ALPHABET, 0, symbols, rows, flags);
        if (result == null) throw new AssertionError("unexpected resource refusal");
        return result;
    }

    private static int oracle(short[] rows, int symbols, int state, int[] ids, int from, int size) {
        for (int at = from; at < from + size; at++) {
            if (state >= 0) state = rows[state * symbols + ids[at]];
        }
        return state;
    }

    // Independent forward BFS, not the production reverse-coordinate implementation.
    private static boolean canAccept(short[] rows, int symbols, byte[] flags, int initial) {
        if (initial < 0) return false;
        boolean[] seen = new boolean[flags.length];
        int[] queue = new int[flags.length];
        int tail = 1;
        queue[0] = initial;
        seen[initial] = true;
        for (int head = 0; head < tail; head++) {
            int state = queue[head];
            if (flags[state] != 0) return true;
            for (int symbol = 0; symbol < symbols; symbol++) {
                int next = rows[state * symbols + symbol];
                if (next >= 0 && !seen[next]) {
                    seen[next] = true;
                    queue[tail++] = next;
                }
            }
        }
        return false;
    }

    private static void pages() {
        // First native page ends at state 1; a later-page OOME must retry from 0.
        M3RX7.TokenIndex value = index(1, new short[]{1, 2, 0}, new byte[]{1, 0, 0});
        int[] ids = new int[8193];
        equal(0, value.advance(ALPHABET, 0, ids, 0, ids.length));
        equal(true, value.matches(ids));
        int state = value.initialState();
        long absoluteTokenOffset = (long) Integer.MAX_VALUE + 17;
        for (int at = 0; at < ids.length;) {
            int size = Math.min(997, ids.length - at);
            state = value.advance(state, ids, at, size);
            at += size;
            absoluteTokenOffset += size;
        }
        equal(0, state);
        equal((long) Integer.MAX_VALUE + 17 + ids.length, absoluteTokenOffset);
        equal(-1, value.advance(-1, ids, ids.length, 0));
        rejects(IllegalArgumentException.class,
                () -> value.advance(PROGRAM, 0, ids, 0, 1));
        rejects(IndexOutOfBoundsException.class,
                () -> value.advance(-1, new int[]{0, 5}, 0, 2));
        rejects(IndexOutOfBoundsException.class,
                () -> value.advance(3, ids, 0, 0));
        rejects(IndexOutOfBoundsException.class,
                () -> value.advance(0, ids, 1, Integer.MAX_VALUE));
        equal(PROGRAM, value.programRoot());
        equal(ALPHABET, value.alphabetRoot());
    }

    private static int[] javaTokens(String source) {
        Scanner lexer = ScannerFactory.instance(new Context()).newScanner(source, false);
        int[] ids = new int[4];
        int count = 0;
        for (lexer.nextToken(); lexer.token().kind != TokenKind.EOF; lexer.nextToken()) {
            if (count == ids.length) ids = Arrays.copyOf(ids, ids.length * 2);
            // Actual javac token kind, not a regex guessing at quotes/comments.
            ids[count++] = switch (lexer.token().kind) {
                case IDENTIFIER -> lexer.token().name().contentEquals("foo") ? 0 : 4;
                case LPAREN -> 1;
                case STRINGLITERAL -> 2;
                default -> 3;
            };
            equal(true, lexer.token().pos >= 0 && lexer.token().endPos <= source.length());
        }
        return Arrays.copyOf(ids, count);
    }

    private static void lexicalCases() {
        short[] rows = new short[15];
        Arrays.fill(rows, (short) -1);
        rows[0] = 1;
        rows[5 + 1] = 2;
        M3RX7.TokenIndex value = index(5, rows, new byte[]{0, 0, 1});
        equal(true, value.matches(javaTokens("foo (")));
        equal(true, value.matches(javaTokens("foo /* foo ( */ (")));
        equal(true, value.matches(javaTokens("\\u0066oo (")));
        equal(false, value.matches(javaTokens("\"foo\" (")));
        equal(false, value.matches(javaTokens("foobar (")));
        equal(false, value.matches(javaTokens("/* foo ( */")));
        // Same payload identity must not turn a STRING_LITERAL into an IDENTIFIER.
        equal(false, value.matches(new int[]{2, 1}));
        Arrays.fill(rows, (short) 0);
        equal(true, value.matches(new int[]{0, 1}));
        equal(false, value.matches(new int[]{2, 1}));
    }

    private static void generated() {
        Random random = new Random(0x4D33544F4B454EL);
        int[] widths = {0, 1, 2, 5, 63, 64, 65, 127, 128, 129, 137};
        for (int trial = 0; trial < 220; trial++) {
            int symbols = widths[trial % widths.length];
            int states = 1 + random.nextInt(8);
            short[] rows = new short[states * symbols];
            byte[] flags = new byte[states];
            for (int i = 0; i < rows.length; i++) rows[i] = (short) (random.nextInt(states + 1) - 1);
            for (int i = 0; i < states; i++) flags[i] = (byte) random.nextInt(2);
            M3RX7.TokenIndex value = index(symbols, rows, flags);
            for (int state = 0; state < states; state++) {
                for (int symbol = 0; symbol < symbols; symbol++) {
                    equal((int) rows[state * symbols + symbol], value.nextState(state, symbol));
                    boolean allowed = (value.allowedMaskWord(state, symbol >>> 6)
                            & (1L << (symbol & 63))) != 0;
                    equal(canAccept(rows, symbols, flags, rows[state * symbols + symbol]), allowed);
                }
                if (symbols % 64 != 0) {
                    equal(0L, value.allowedMaskWord(state, symbols >>> 6) >>> (symbols % 64));
                }
            }
            for (int sample = 0; sample < 15; sample++) {
                int size = symbols == 0 ? 0 : random.nextInt(24);
                int[] ids = new int[size + 2];
                ids[0] = ids[size + 1] = -99; // Outside the selected page, never consumed.
                for (int i = 1; i <= size; i++) ids[i] = random.nextInt(symbols);
                int expected = oracle(rows, symbols, 0, ids, 1, size);
                equal(expected, value.advance(0, ids, 1, size));
                for (int cut = 0; cut <= size; cut++) {
                    int first = value.advance(0, ids, 1, cut);
                    equal(expected, value.advance(first, ids, 1 + cut, size - cut));
                }
            }
            Arrays.fill(rows, (short) -1);
            Arrays.fill(flags, (byte) 9);
            // Mutated construction buffers cannot alter or invalidate the frozen index.
            equal(PROGRAM, value.programRoot());
            value.matches(new int[0]);
        }
    }

    private static void validation() {
        rejects(IllegalArgumentException.class,
                () -> M3RX7.fromTokenDfa("bad", ALPHABET, 0, 0, new short[0], new byte[]{1}));
        rejects(IllegalArgumentException.class,
                () -> index(1, new short[]{2}, new byte[]{1}));
        rejects(IllegalArgumentException.class,
                () -> index(1, new short[]{0}, new byte[]{2}));
        rejects(IllegalArgumentException.class,
                () -> index(2, new short[]{0}, new byte[]{1}));
        equal(null, M3RX7.fromTokenDfa(PROGRAM, ALPHABET, 0, 1_048_577,
                new short[1_048_577], new byte[]{1}));
        M3RX7.TokenIndex empty = index(0, new short[0], new byte[]{1});
        equal(true, empty.matches(new int[0]));
        equal(0, empty.allowedMaskWords());
        M3RX7.TokenIndex blocked = index(1, new short[]{1, -1, -1}, new byte[]{0, 0, 1});
        equal(0L, blocked.allowedMaskWord(0, 0));
        // The predecessor's spelling-based conservative mask contract is unchanged.
        short[] ascii = new short[3 * 128];
        Arrays.fill(ascii, (short) -1);
        ascii['a'] = 1;
        ascii[128 + 'b'] = 2;
        M3RX7 old = M3RX7.fromAsciiDfa(PROGRAM, 0, ascii, new byte[]{0, 0, 1});
        M3RX7.TokenIndex spelling = old.prepareTokenIndex(new byte[]{'a'}, new int[]{0}, new int[]{1});
        equal(1L, Objects.requireNonNull(spelling).allowedMaskWord(0, 0));
        equal(null, spelling.alphabetRoot());
    }

    private static void nativeParity() throws Exception {
        Method method = M3RX7.class.getDeclaredMethod("advanceTokens0", short[].class,
                int.class, int.class, int[].class, int.class, int.class);
        method.setAccessible(true);
        Random random = new Random(72819L);
        for (int trial = 0; trial < 400; trial++) {
            int symbols = 1 + random.nextInt(137), states = 1 + random.nextInt(8);
            short[] rows = new short[states * symbols];
            for (int i = 0; i < rows.length; i++) rows[i] = (short) (random.nextInt(states + 1) - 1);
            int[] ids = new int[2 + random.nextInt(4095)];
            for (int i = 0; i < ids.length; i++) ids[i] = random.nextInt(symbols);
            for (int start = -1; start < states; start++) {
                equal(oracle(rows, symbols, start, ids, 1, ids.length - 2),
                        method.invoke(null, rows, symbols, start, ids, 1, ids.length - 2));
                nativeCalls++;
            }
        }
        try {
            method.invoke(null, new short[]{0}, 1, -1, new int[]{2}, 0, 1);
            throw new AssertionError("native dead state must still validate IDs");
        } catch (InvocationTargetException error) {
            equal(IllegalArgumentException.class, error.getCause().getClass());
        }
        try {
            method.invoke(null, new short[]{-2}, 1, 0, new int[]{0}, 0, 1);
            throw new AssertionError("native malformed transition must refuse");
        } catch (InvocationTargetException error) {
            equal(IllegalArgumentException.class, error.getCause().getClass());
        }
    }

    public static void main(String[] args) throws Exception {
        String library = System.getProperty("m3.test.native.library");
        if (library != null) {
            Class.forName("jdk.internal.mindex.M3RX7TestLoader")
                    .getMethod("load", String.class).invoke(null, library);
        }
        pages();
        lexicalCases();
        generated();
        validation();
        if (args.length != 0) nativeParity();
        System.out.println("M3RX7TokenSymbolsContract PASS checks=" + checks + " nativeCalls=" + nativeCalls);
    }
}
