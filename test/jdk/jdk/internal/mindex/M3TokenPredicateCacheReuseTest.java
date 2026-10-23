/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Exact immutable occurrence regex reuse preserves mutable input and vocabulary observations
 * @modules java.base/jdk.internal.mindex:+open jdk.management
 * @run main/othervm -Xint M3TokenPredicateCacheReuseTest
 */

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3TokenIndex;
import jdk.internal.mindex.M3TokenPattern;
import jdk.internal.mindex.M3TokenPredicateCache;

public final class M3TokenPredicateCacheReuseTest {
    private static final M3TokenPredicateCache.Vocabulary NONE = spelling -> -1;
    private static volatile int sink;
    private static long checks;

    private M3TokenPredicateCacheReuseTest() { }

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "all" : args[0];
        if (!mode.equals("reuse") && !mode.equals("layout")) {
            exactTextOracle();
            mutableSources();
            mutableVocabulary();
            occurrenceContext();
            boundariesAndOwners();
            concurrentReplacements();
            System.out.println("SEMANTICS_PASS checks=" + checks);
        }
        if (mode.equals("all") || mode.equals("reuse")) {
            repeatedWorkReuse();
        }
        if (mode.equals("all") || mode.equals("layout")) {
            boundedPrimitiveStorage();
            packedKeyBoundaries();
        }
        System.out.println("M3TokenPredicateCacheReuseTest PASS mode=" + mode + " checks=" + checks);
    }

    private static void exactTextOracle() {
        String[] words = {"abc", "ABC", "123", "١٢٣", "中文", "λx", "a\u0301", "😀",
                "\ud800", "\udc00", "", "a\nb", "x/y", "\\d+", "if", "null", "a_a", "ß", "aa", "ab"};
        String[] expressions = {".*", ".+", "(?s:.*)", "[a-z]+", "[A-Z]+", "\\d+",
                "(?U)\\d+", "\\w+", "(?U)\\w+", "\\p{L}+", "\\p{IsHan}+", "(?i)abc",
                "(?iu)λx", "a.*", "(.)\\1", "(?:if|null)", "\\A.*\\z", "(?:)", ".", "a\\p{M}*"};
        StringBuilder text = new StringBuilder();
        long[] starts = new long[words.length];
        long[] ends = new long[words.length];
        int[] ids = new int[words.length];
        int[] kinds = new int[words.length];
        int[] adversarialIds = {7, 7, 0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (int i = 0; i < words.length; i++) {
            starts[i] = text.length();
            text.append(words[i]);
            ends[i] = text.length();
            text.append('|');
            ids[i] = adversarialIds[i % adversarialIds.length];
        }
        String source = text.toString();
        M3TokenIndex index = M3TokenIndex.of(19, kinds, ids, starts, ends, new long[words.length]);
        StringBuilder grammar = new StringBuilder();
        Pattern[] oracles = new Pattern[expressions.length];
        for (int r = 0; r < expressions.length; r++) {
            if (r != 0) grammar.append(' ');
            grammar.append("[{text:/").append(expressions[r]).append("/}]");
            oracles[r] = Pattern.compile(expressions[r]);
        }
        M3TokenPattern pattern = M3TokenPattern.compile(grammar.toString());
        for (CharSequence input : new CharSequence[] {source, new StringBuilder(source)}) {
            M3TokenPredicateCache cache = M3TokenPredicateCache.bind(pattern, index, input, NONE);
            for (int pass = 0; pass < 6; pass++) {
                for (int r = 0; r < expressions.length; r++) {
                    for (int j = 0; j < words.length; j++) {
                        int token = pass % 2 == 0 ? j : words.length - j - 1;
                        boolean expected = oracles[r].matcher(words[token]).matches();
                        check(cache.row(r, token) == expected, "oracle row=" + r + " token=" + token);
                    }
                }
            }
        }
    }

    private static void mutableSources() {
        StringBuilder source = new StringBuilder("abc");
        M3TokenPredicateCache cache = bind("[a-z]+", one(3), source, NONE);
        check(cache.row(0, 0), "mutable initial true");
        source.replace(0, 3, "123");
        check(!cache.row(0, 0), "mutable true becomes false");
        source.replace(0, 3, "xyz");
        check(cache.row(0, 0), "mutable false becomes true");
        source.setLength(1);
        check(!cache.row(0, 0), "mutable source shrinks outside range");
        source.append("ab");
        check(cache.row(0, 0), "mutable source range becomes valid");

        ObservedText observed = new ObservedText("abc");
        M3TokenPredicateCache live = bind("[a-z]+", one(3), observed, NONE);
        check(live.row(0, 0), "observed initial");
        int reads = observed.reads;
        check(live.row(0, 0) && observed.reads > reads, "custom CharSequence reads repeat");
        observed.fail = true;
        expect(ObservationFailure.class, () -> live.row(0, 0));
        expect(ObservationFailure.class, () -> live.row(0, 0));
    }

    private static void mutableVocabulary() {
        MutableVocabulary vocabulary = new MutableVocabulary();
        M3TokenIndex index = M3TokenIndex.of(3, new int[] {7}, new int[] {7},
                new long[] {0}, new long[] {1}, new long[] {7}, new int[] {7},
                new String[] {"pos"}, new int[][] {{7}});
        for (String key : new String[] {"kind", "lexeme", "binding", "flags", "pos"}) {
            M3TokenPattern p = M3TokenPattern.compile("[{" + key + ":/ID.*/}]");
            M3TokenPredicateCache cache = M3TokenPredicateCache.bind(p, index, "a", vocabulary);
            vocabulary.text.replace(0, vocabulary.text.length(), "IDENT");
            vocabulary.fail = false;
            int before = vocabulary.calls;
            check(cache.row(0, 0), "symbolic true " + key);
            vocabulary.text.replace(0, vocabulary.text.length(), "NUMBER");
            check(!cache.row(0, 0), "symbolic mutates " + key);
            check(vocabulary.calls == before + 2, "symbol callback repeats " + key);
            vocabulary.fail = true;
            expect(ObservationFailure.class, () -> cache.row(0, 0));
            expect(ObservationFailure.class, () -> cache.row(0, 0));
        }
        M3TokenIndex large = M3TokenIndex.of(3, new int[] {0, 0}, new int[] {0, 0},
                new long[] {0, 1}, new long[] {1, 2}, new long[] {Long.MIN_VALUE, Long.MAX_VALUE});
        M3TokenPredicateCache numeric = M3TokenPredicateCache.bind(
                M3TokenPattern.compile("[{flags:/-?\\d+/}]"), large, "aa", vocabulary);
        int before = vocabulary.calls;
        check(numeric.row(0, 0) && numeric.row(0, 1), "large flag fallback");
        check(vocabulary.calls == before, "large numeric values bypass vocabulary as before");
        M3TokenPredicateCache pure = bind("a", one(1), "a", vocabulary);
        check(pure.row(0, 0) && pure.row(0, 0), "text regex never calls hostile vocabulary");
    }

    private static void occurrenceContext() {
        M3TokenIndex index = M3TokenIndex.of(1, new int[] {1, 2}, new int[] {55, 55},
                new long[] {0, 2}, new long[] {1, 3}, new long[] {4, 0}, new int[] {1, 2},
                new String[] {"pos"}, new int[][] {{2, 3}});
        M3TokenPattern p = M3TokenPattern.compile(
                "[{text:/a/} & {binding:1} & {flags:0x4} & {pos:2}]");
        M3TokenPredicateCache cache = M3TokenPredicateCache.bind(p, index, "a a", NONE);
        int expression = p.nodeX(p.rootNode());
        for (int n = 0; n < 100; n++) {
            check(cache.row(0, 0) && cache.row(0, 1), "equal text remains true per occurrence");
            check(cache.test(expression, 0) && !cache.test(expression, 1), "occurrence attributes remain distinct");
        }
        M3TokenPattern flags = M3TokenPattern.compile("[{text:/abc/}]", M3TokenPattern.CASE_INSENSITIVE);
        check(!M3TokenPredicateCache.bind(flags, one(3), "ABC", NONE).row(0, 0),
                "token exact-text flag does not change regex flags");
        check(bind("(?i)abc", one(3), "ABC", NONE).row(0, 0), "regex inline flags remain effective");
    }

    private static void boundariesAndOwners() {
        long[] starts = {0, 2};
        long[] ends = {1, 3};
        int[] ids = {91, 91};
        M3TokenIndex index = M3TokenIndex.of(7, new int[2], ids, starts, ends, new long[2]);
        M3TokenPredicateCache cache = bind("a", index, "a b", NONE);
        for (int n = 0; n < 100; n++) {
            check(cache.row(0, 0) && !cache.row(0, 1), "same ID is not spelling equality");
        }
        starts[0] = 2;
        ends[0] = 3;
        ids[0] = -77;
        check(cache.row(0, 0), "caller lane mutation cannot change index snapshot");
        check(!bind("a", index, "b a", NONE).row(0, 0), "same generation and coordinates, different owner source");
        check(!bind("b", index, "a b", NONE).row(0, 0), "different bound pattern");
        M3TokenIndex empty = M3TokenIndex.of(0, new int[] {0, 0}, new int[] {-1, -1},
                new long[] {0, 1}, new long[] {0, 1}, new long[2]);
        M3TokenPredicateCache empties = bind("(?:)", empty, "a", NONE);
        check(empties.row(0, 0) && empties.row(0, 1), "empty exact spans including source end");
        M3TokenIndex outside = M3TokenIndex.of(0, new int[] {0, 0}, new int[] {0, 0},
                new long[] {2, Integer.MAX_VALUE}, new long[] {3, (long) Integer.MAX_VALUE + 5}, new long[2]);
        M3TokenPredicateCache invalid = bind(".*", outside, "a", NONE);
        check(!invalid.row(0, 0) && !invalid.row(0, 1), "out-of-source long coordinates return false");
        expect(IndexOutOfBoundsException.class, () -> cache.row(-1, 0));
        expect(IndexOutOfBoundsException.class, () -> cache.row(1, 0));
        expect(IndexOutOfBoundsException.class, () -> cache.row(0, -1));
        expect(IndexOutOfBoundsException.class, () -> cache.row(0, 2));
        expect(IndexOutOfBoundsException.class, () -> cache.test(-1, -1));
    }

    private static void concurrentReplacements() throws InterruptedException {
        int tokens = 1025;
        String source = "ab".repeat(tokens / 2) + "a";
        M3TokenIndex index = units(source.length());
        M3TokenPattern pattern = M3TokenPattern.compile("/a/ /b/");
        M3TokenPredicateCache cache = M3TokenPredicateCache.bind(pattern, index, source, NONE);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch start = new CountDownLatch(1);
        Thread[] threads = new Thread[8];
        for (int t = 0; t < threads.length; t++) {
            final int worker = t;
            threads[t] = new Thread(() -> {
                try {
                    start.await();
                    for (int n = 0; n < 6000; n++) {
                        int token = (n * 257 + worker * 131) % tokens;
                        int row = (n + worker) & 1;
                        boolean expected = source.charAt(token) == (row == 0 ? 'a' : 'b');
                        if (cache.row(row, token) != expected) {
                            throw new AssertionError("concurrent replacement token=" + token + " row=" + row);
                        }
                    }
                } catch (Throwable thrown) {
                    failure.compareAndSet(null, thrown);
                }
            }, "token-regex-reuse-" + t);
            threads[t].start();
        }
        start.countDown();
        for (Thread thread : threads) thread.join();
        if (failure.get() != null) throw new AssertionError("concurrent exact-result isolation", failure.get());
        checks += 48_000;
        for (int token = 0; token < tokens; token++) {
            check(cache.row(0, token) == (source.charAt(token) == 'a'), "collision eviction true/false");
            check(cache.row(1, token) == (source.charAt(token) == 'b'), "collision eviction row identity");
        }
    }

    private static void repeatedWorkReuse() {
        ThreadMXBean bean = allocationBean();
        long positive = repeatedBytes(bean, bind("[a-z]+", one(7), "example", NONE), true);
        long negative = repeatedBytes(bean, bind("[a-z]+", one(7), "1234567", NONE), false);
        System.out.println("REUSE_ALLOCATION positive_bytes=" + positive + " negative_bytes=" + negative
                + " calls_each=20000 limit_each=161024");
        check(positive <= 161_024 && negative <= 161_024,
                "REUSE_EXPECTATION: repeated immutable occurrences still allocate matcher work; positive="
                        + positive + " negative=" + negative);
    }

    private static long repeatedBytes(ThreadMXBean bean, M3TokenPredicateCache cache, boolean expected) {
        for (int n = 0; n < 500; n++) check(cache.row(0, 0) == expected, "reuse warmup answer");
        long id = Thread.currentThread().threadId();
        int count = 0;
        long before = bean.getThreadAllocatedBytes(id);
        for (int n = 0; n < 20_000; n++) {
            if (cache.row(0, 0) == expected) count++;
        }
        long bytes = bean.getThreadAllocatedBytes(id) - before;
        sink = count;
        check(count == 20_000, "repeated result");
        return bytes;
    }

    private static void boundedPrimitiveStorage() throws ReflectiveOperationException {
        Field table = M3TokenPredicateCache.class.getDeclaredField("textRegexResults");
        table.setAccessible(true);
        check(table.getType() == long[].class, "result table is a primitive lane");
        M3TokenPredicateCache single = bind("a", one(1), "a", NONE);
        check(table.get(single) == null, "bind does not allocate result table");
        for (int n = 0; n < 1000; n++) check(single.row(0, 0), "one occurrence reused");
        check(table.get(single) == null, "single repeated occurrence needs no table");
        M3TokenPredicateCache mutable = bind("a", one(1), new StringBuilder("a"), NONE);
        for (int n = 0; n < 5; n++) check(mutable.row(0, 0), "mutable remains correct");
        check(table.get(mutable) == null, "mutable source has no result table");
        String source = "ab".repeat(2048);
        M3TokenPredicateCache many = bind("a", units(source.length()), source, NONE);
        for (int n = 0; n < source.length(); n++) check(many.row(0, n) == ((n & 1) == 0), "bounded corpus");
        long[] results = (long[]) table.get(many);
        check(results != null && results.length <= 256, "result table is bounded by 256 entries");
        check((results.length & (results.length - 1)) == 0, "result table mask is well formed");
        for (Field field : M3TokenPredicateCache.class.getDeclaredFields()) {
            check(!field.getType().getName().startsWith("java.util.Map"), "no per-entry map store");
        }
    }

    private static void packedKeyBoundaries() throws ReflectiveOperationException {
        Method key = M3TokenPredicateCache.class.getDeclaredMethod("textRegexKey", int.class, int.class, int.class);
        key.setAccessible(true);
        for (int count : new int[] {1, 2, 255, 256, 257, 65536, Integer.MAX_VALUE}) {
            Set<Long> unique = new HashSet<>();
            int[] tokens = count == 1 ? new int[] {0} : new int[] {0, count - 1};
            for (int row : new int[] {0, 1, 4095, Integer.MAX_VALUE - 1}) {
                for (int token : tokens) {
                    long encoded = (long) key.invoke(null, row, token, count);
                    check(encoded > 0 && (encoded & 1L) == 0L, "valid exact key excludes empty and result bit");
                    check(unique.add(encoded) && unique.add(encoded | 1L), "distinct row/token/truth keys");
                }
            }
        }
    }

    static ThreadMXBean allocationBean() {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new AssertionError("allocation counter unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        return bean;
    }

    static M3TokenPredicateCache bind(String regex, M3TokenIndex index, CharSequence source,
            M3TokenPredicateCache.Vocabulary vocabulary) {
        return M3TokenPredicateCache.bind(M3TokenPattern.compile("[{text:/" + regex + "/}]"), index, source, vocabulary);
    }

    static M3TokenIndex one(int length) {
        return M3TokenIndex.of(9, new int[] {0}, new int[] {7}, new long[] {0}, new long[] {length}, new long[1]);
    }

    static M3TokenIndex units(int count) {
        long[] starts = new long[count];
        long[] ends = new long[count];
        for (int n = 0; n < count; n++) { starts[n] = n; ends[n] = n + 1L; }
        return M3TokenIndex.of(9, new int[count], new int[count], starts, ends, new long[count]);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError("expected " + type.getName() + " but got " + thrown, thrown);
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static final class ObservationFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static final class ObservedText implements CharSequence {
        private final String value;
        private int reads;
        private boolean fail;
        ObservedText(String value) { this.value = value; }
        public int length() { return value.length(); }
        public char charAt(int index) {
            reads++;
            if (fail) throw new ObservationFailure();
            return value.charAt(index);
        }
        public CharSequence subSequence(int start, int end) { return value.subSequence(start, end); }
    }

    private static final class MutableVocabulary implements M3TokenPredicateCache.Vocabulary {
        private final StringBuilder text = new StringBuilder("IDENT");
        private int calls;
        private boolean fail;
        public int lexemeOf(CharSequence spelling) { return 7; }
        public CharSequence symbolText(String key, int value) {
            calls++;
            if (fail) throw new ObservationFailure();
            return text;
        }
    }
}
