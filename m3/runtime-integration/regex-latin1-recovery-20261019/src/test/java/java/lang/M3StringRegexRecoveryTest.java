// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package java.lang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import jdk.internal.misc.Unsafe;

/**
 * Fresh actual-owner probe. The graph oracle follows the published Apache-2.0
 * RegexGraph fixture at M3JDK21 46f8ad628ce66fbc8b41895f0146edbe00e3aca5.
 * Test storage is temporary native memory behind the actual canonical atom API.
 * This fixture does not replace String.class or establish full M3VM acceptance.
 */
public final class M3StringRegexRecoveryTest {
    private static final Unsafe MEMORY = Unsafe.getUnsafe();
    private static final int STAR = 1 << 16;
    private static final Constructor<?> PLAN;
    private static final MethodHandle KERNEL;
    private static final MethodHandle CLOSURE;
    private static final Field NATIVE_UNAVAILABLE;
    private static long checks;
    private static long digest;
    private static volatile long sink;

    static {
        try {
            Class<?> plan = Class.forName("java.lang.M3StringRegexDp$Plan");
            PLAN = plan.getDeclaredConstructor(int[].class, int.class);
            PLAN.setAccessible(true);
            Method advance = M3StringRegexDp.class.getDeclaredMethod("advanceJava",
                    M3StringOwner.class, int.class, int.class, plan, long.class);
            advance.setAccessible(true);
            KERNEL = MethodHandles.lookup().unreflect(advance).asType(MethodType.methodType(
                    long.class, M3StringOwner.class, int.class, int.class, Object.class, long.class));
            Method close = M3StringRegexDp.class.getDeclaredMethod("close", long.class, long.class);
            close.setAccessible(true);
            CLOSURE = MethodHandles.lookup().unreflect(close);
            NATIVE_UNAVAILABLE = M3StringRegexDp.class.getDeclaredField("nativeUnavailable");
            NATIVE_UNAVAILABLE.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private M3StringRegexRecoveryTest() { }

    private static final class Storage implements AutoCloseable {
        private final List<Long> addresses = new ArrayList<>();
        private long nextIdentity = 1;
        private long nativeBytes;

        M3String atom(String spelling, int width, boolean bigEndian) {
            char[] units = spelling.toCharArray();
            boolean latin = spelling.chars().allMatch(unit -> unit <= 255);
            if (width == 1 && !latin) throw new IllegalArgumentException("fixture byte domain");
            long bytes = Math.max(1L, (long) units.length * width);
            long address = MEMORY.allocateMemory(bytes);
            addresses.add(address);
            nativeBytes += bytes;
            MEMORY.setMemory(address, bytes, (byte) 0);
            for (int i = 0; i < units.length; i++) {
                long p = address + (long) i * width;
                int c = units[i];
                if (width == 1) {
                    MEMORY.putByte(p, (byte) c);
                } else if (bigEndian) {
                    MEMORY.putByte(p, (byte) (c >>> 8));
                    MEMORY.putByte(p + 1, (byte) c);
                } else {
                    MEMORY.putByte(p, (byte) c);
                    MEMORY.putByte(p + 1, (byte) (c >>> 8));
                }
            }
            long identity = nextIdentity++;
            byte coder = String.COMPACT_STRINGS && latin ? String.LATIN1 : String.UTF16;
            M3StringAtom owner = new M3StringAtom(this, address, (byte) width, bigEndian,
                    units.length, coder, spelling.hashCode(), identity, identity * 97);
            return M3String.whole(owner);
        }

        M3String join(M3String left, M3String right) {
            long identity = nextIdentity++;
            return M3String.whole(new M3StringTuple(left, right, identity, identity * 101));
        }

        @Override public void close() {
            for (long address : addresses) MEMORY.freeMemory(address);
            addresses.clear();
            nativeBytes = 0;
        }
    }

    private static void equal(long actual, long expected, String label) {
        checks++;
        if (actual != expected) {
            throw new AssertionError(label + ": actual=" + actual + " expected=" + expected);
        }
        digest = Long.rotateLeft(digest, 7) ^ actual ^ label.hashCode();
    }

    private static long closeGraph(long bits, int[] atoms) {
        long last;
        do {
            last = bits;
            for (int i = 0; i < atoms.length; i++) {
                if ((atoms[i] & STAR) != 0 && (bits & (1L << i)) != 0) {
                    bits |= 1L << (i + 1);
                }
            }
        } while (last != bits);
        return bits;
    }

    private static boolean line(char c) {
        return c == 10 || c == 13 || c == 133 || c == 8232 || c == 8233;
    }

    private static void closureCase(int count, long stars, long bits) throws Throwable {
        long expected = bits;
        long previous;
        do {
            previous = expected;
            expected |= (expected & stars) << 1;
        } while (previous != expected);
        long actual = (long) CLOSURE.invokeExact(bits, stars);
        equal(actual, expected, "actual closure graph");
        equal((long) CLOSURE.invokeExact(actual, stars), actual, "actual closure fixed point");
        equal(actual & ~((1L << (count + 1)) - 1), 0, "closure coordinate bound");
    }

    /** Corpus retained from the published dual-licensed closure jtreg fixture. */
    private static void closureSpectrum() throws Throwable {
        long exhaustive = 0;
        for (int count = 1; count <= 9; count++) {
            for (long stars = 0; stars < (1L << count); stars++) {
                for (long bits = 0; bits < (1L << (count + 1)); bits++) {
                    closureCase(count, stars, bits);
                    exhaustive++;
                }
            }
        }
        equal(exhaustive, 699_048, "complete exhaustive closure corpus");
        for (int low = 0; low < 32; low++) {
            for (int high = low; high < 32; high++) {
                long stars = ((1L << (high - low + 1)) - 1) << low;
                for (int bit = 0; bit <= 32; bit++) {
                    closureCase(32, stars, 1L << bit);
                    closureCase(32, stars, (1L << bit) | (1L << 32));
                }
                closureCase(32, stars, (1L << 33) - 1);
                closureCase(32, stars, 0);
            }
        }
        Random random = new Random(0x4d33434c4f535552L);
        for (int trial = 0; trial < 20_000; trial++) {
            closureCase(32, Integer.toUnsignedLong(random.nextInt()),
                    random.nextLong() & ((1L << 33) - 1));
        }
    }

    private static long graph(String body, int[] atoms, long incoming) {
        long state = incoming;
        for (int at = 0; at < body.length(); at++) {
            char unit = body.charAt(at);
            if (Character.isSurrogate(unit)) return -1;
            long next = 0;
            for (int token = 0; token < atoms.length; token++) {
                char required = (char) atoms[token];
                boolean accepts = required == '.' ? !line(unit) : required == unit;
                if (accepts && (state & (1L << token)) != 0) {
                    next |= 1L << ((atoms[token] & STAR) == 0 ? token + 1 : token);
                }
            }
            state = closeGraph(next, atoms);
        }
        return state;
    }

    private static Object plan(int[] atoms) throws ReflectiveOperationException {
        int units = atoms.length;
        for (int atom : atoms) if ((atom & STAR) != 0) units++;
        return PLAN.newInstance(atoms.clone(), units);
    }

    private static int[] atoms(int count, int style, long stars) {
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            char literal = style == 0 ? 'a' : style == 1 ? '.' : i % 2 == 0 ? 'a' : 'b';
            result[i] = literal | ((stars & (1L << i)) != 0 ? STAR : 0);
        }
        return result;
    }

    private static long advance(M3String text, Object plan, long state) throws Throwable {
        return (long) KERNEL.invokeExact(text.owner(), text.start(), text.length(), plan, state);
    }

    private static void direct(String body, int[] atoms, long seeds) throws Throwable {
        Object compiled = plan(atoms);
        long incoming = closeGraph(seeds, atoms);
        long expected = graph(body, atoms, incoming);
        boolean latin = body.chars().allMatch(unit -> unit <= 255);
        for (int format = latin ? 0 : 1; format < 3; format++) {
            int width = format == 0 ? 1 : 2;
            try (Storage storage = new Storage()) {
                M3String padded = storage.atom("X" + body + "Y", width, format == 2);
                M3String view = padded.slice(1, padded.length() - 1);
                equal(advance(view, compiled, incoming), expected, "actual Java kernel");
                if (body.length() <= 6) {
                    for (int cut = 0; cut <= body.length(); cut++) {
                        long left = advance(view.slice(0, cut), compiled, incoming);
                        long joined = left < 0 ? left
                                : advance(view.slice(cut, view.length()), compiled, left);
                        equal(joined, expected, "continuous Java frontier");
                    }
                }
                if (width == 2 && latin && String.COMPACT_STRINGS) {
                    equal(view.owner().coder, String.LATIN1, "physical width separate from coder");
                }
            }
        }
    }

    private static void kernelSpectrum() throws Throwable {
        String[] bodies = {"", "a", "b", "aaab", "abab", "\0", "\n", "\r", "\u0085",
                "\u00ff", "\u0100", "\u2028", "\u2029", "b\ud800", "a\udfff",
                "\ud83d\ude42", "a".repeat(255), "a".repeat(256), "a".repeat(8192),
                "b" + "a".repeat(8190) + "\ud800"};
        for (int count : new int[]{1, 2, 7, 8, 16, 31, 32}) {
            long all = (1L << count) - 1;
            for (long stars : new long[]{1, all, (all & 0xaaaa_aaaaL) | 1}) {
                for (int style = 0; style < 3; style++) {
                    int[] tokens = atoms(count, style, stars);
                    for (String body : bodies) {
                        direct(body, tokens, 1);
                        direct(body, tokens, 0);
                        direct(body, tokens, 1L << count);
                    }
                }
            }
        }
        Random random = new Random(0x4d335245434f5631L);
        char[] alphabet = {'a', 'b', 'A', '0', '\0', '\n', '\r', '\u0085',
                '\u00ff', '\u0100', '\u2028', '\u2029', '\ud800', '\udfff'};
        for (int trial = 0; trial < 1024; trial++) {
            int count = 1 + random.nextInt(32);
            long stars = (Integer.toUnsignedLong(random.nextInt()) & ((1L << count) - 1)) | 1;
            char[] units = new char[random.nextInt(65)];
            for (int i = 0; i < units.length; i++) units[i] = alphabet[random.nextInt(alphabet.length)];
            direct(new String(units), atoms(count, random.nextInt(3), stars),
                    random.nextLong() & ((1L << (count + 1)) - 1));
        }
    }

    private static int expectedMatch(String body, String regex) {
        if (body.length() > 8192 || body.chars().anyMatch(unit -> Character.isSurrogate((char) unit))) {
            return -1;
        }
        return Pattern.matches(regex, body) ? 1 : 0;
    }

    private static void matching(String body, String regex) {
        int expected = expectedMatch(body, regex);
        boolean latin = body.chars().allMatch(unit -> unit <= 255);
        for (int format = latin ? 0 : 1; format < 3; format++) {
            try (Storage storage = new Storage()) {
                M3String text = storage.atom("Q!" + body + "?R", format == 0 ? 1 : 2, format == 2);
                M3String view = text.slice(2, body.length() + 2);
                equal(M3StringRegexDp.tryMatch(view, regex), expected, "actual tryMatch cold");
                equal(M3StringRegexDp.tryMatch(view, regex), expected, "actual tryMatch warm");
                int cut = body.length() / 2;
                M3String tuple = storage.join(view.slice(0, cut), view.slice(cut, view.length()));
                equal(M3StringRegexDp.tryMatch(tuple, regex), expected, "tuple continuous state");
                M3String nested = storage.join(text.slice(0, 2), storage.join(tuple, text.slice(text.length() - 2, text.length())));
                equal(M3StringRegexDp.tryMatch(nested.slice(2, nested.length() - 2), regex),
                        expected, "nested tuple range");
            }
        }
    }

    private static void ownerSpectrum() throws Exception {
        String[] regexes = {"a*", "a*b", ".*", "a*b*", "a*b.*", "a*".repeat(32),
                "a*".repeat(33), "a*".repeat(64), ".*".repeat(32), "a*b".repeat(32)};
        String[] bodies = {"", "a", "b", "aaab", "abab", "\0", "\n", "\r", "\u0085",
                "\u0100", "\u2028", "\u2029", "b\ud800", "\ud83d\ude42",
                "a".repeat(255), "a".repeat(256), "a".repeat(8192), "a".repeat(8193)};
        for (String regex : regexes) for (String body : bodies) matching(body, regex);
        try (Storage storage = new Storage()) {
            M3String latin = storage.atom("b".repeat(256), 1, false);
            M3String wide = storage.atom("a".repeat(255) + "\ud800", 2, true);
            M3String joined = storage.join(latin, wide);
            equal(M3StringRegexDp.tryMatch(joined, "a*"), -1, "later UTF16 tuple child still refuses");
            equal(M3StringRegexDp.tryMatch(joined, "a*"), -1, "cached UNKNOWN keeps tuple refusal");
            M3String repeated = storage.join(latin.slice(1, 255), latin.slice(1, 255));
            equal(M3StringRegexDp.tryMatch(repeated, "a*"), 0, "repeated atom occurrence");
            M3String view = storage.atom("abbaabbaabbaabba", 1, false);
            for (int from = 0; from <= view.length(); from++) {
                for (int to = from; to <= view.length(); to++) {
                    String body = "abbaabbaabbaabba".substring(from, to);
                    equal(M3StringRegexDp.tryMatch(view.slice(from, to), "a*b*"),
                            expectedMatch(body, "a*b*"), "exact cached owner range");
                }
            }
            for (String refused : new String[]{"", "a", "aa", "*a", "a**", "a?", "[ab]*",
                    "(a*)", "a*$", "a*".repeat(65), "\u0100*"}) {
                equal(M3StringRegexDp.tryMatch(view, refused), -1, "grammar stays with Pattern");
            }
        }
        Method retained = M3StringRegexDp.class.getDeclaredMethod("maximumRetainedPrimitiveBytes");
        retained.setAccessible(true);
        long bytes = (long) retained.invoke(null);
        equal(bytes, 588544L, "retained primitive budget");
    }

    public static final class BenchmarkFixture implements AutoCloseable {
        private final Storage storage = new Storage();
        private final M3StringOwner owner;
        private final int length = 8192;
        private final Object plan;
        private final long incoming;
        private final long expected;

        private BenchmarkFixture(String scenario, int width) throws Throwable {
            int count = scenario.equals("small-live") ? 7 : 32;
            int[] tokens = atoms(count, 0, (1L << count) - 1);
            char[] units = new char[length];
            Arrays.fill(units, 'a');
            if (scenario.equals("early-dead")) units[0] = 'b';
            if (scenario.equals("late-dead")) units[length - 1] = 'b';
            String body = new String(units);
            incoming = scenario.equals("incoming-dead") ? 0 : closeGraph(1, tokens);
            expected = graph(body, tokens, incoming);
            plan = plan(tokens);
            owner = storage.atom(body, width, false).owner();
            if (run(1) != expected) throw new AssertionError("benchmark setup oracle");
        }

        public long run(int repetitions) throws Throwable {
            long sum = 0;
            for (int i = 0; i < repetitions; i++) {
                sum += (long) KERNEL.invokeExact(owner, 0, length, plan, incoming);
            }
            sink ^= sum;
            if (sum != expected * repetitions) throw new AssertionError("benchmark result");
            return sum;
        }

        public long nativeBytes() { return storage.nativeBytes; }
        @Override public void close() { storage.close(); }
    }

    public static BenchmarkFixture benchmark(String scenario, int width) throws Throwable {
        return new BenchmarkFixture(scenario, width);
    }

    public static void main(String[] args) throws Throwable {
        boolean nativeRequested = args.length == 2 && args[0].equals("native");
        if (args.length != 0 && !nativeRequested) throw new IllegalArgumentException("no args or native <library>");
        if (nativeRequested) System.load(args[1]);
        NATIVE_UNAVAILABLE.setBoolean(null, !nativeRequested);
        closureSpectrum();
        kernelSpectrum();
        ownerSpectrum();
        if (nativeRequested && NATIVE_UNAVAILABLE.getBoolean(null)) {
            throw new AssertionError("required owner JNI silently fell back to Java");
        }
        System.out.println("M3_REGEX_RECOVERY_OWNER_PASS checks=" + checks
                + " digest=" + Long.toUnsignedString(digest, 16)
                + " actual_owners=true owner_native_requested=" + nativeRequested
                + " modified_String_loaded=false full_image=false");
    }
}
