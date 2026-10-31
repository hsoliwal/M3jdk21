/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Standalone product-image acceptance fixture. No runtime patch, synthetic owner,
 * forced activation, or production source transformation is used. Reflection is
 * read-only: it observes canonical storage, prepared facts, and helper decisions.
 * Public String.strip() is the preparation operation; public String.matches() is
 * the tested entry point. Pattern over a char[]-backed StringBuilder is the oracle.
 */

import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class M3CommonRegexProductTest {
    private static final Field STRING_M3;
    private static final Field COMPACT_STRINGS;
    private static final Field TABLES;
    private static final Field FACT_LENGTH;
    private static final Field FACT_CODE_POINTS;
    private static final Method OWNER;
    private static final Method START;
    private static final Method FACTS_IF_PREPARED;
    private static final Method TRY_MATCH;
    private static final Method READY;
    private static final AtomicLong CHECKS = new AtomicLong();
    private static final AtomicLong PUBLIC_CHECKS = new AtomicLong();
    private static final AtomicLong ORACLES = new AtomicLong();
    private static final AtomicLong CONCURRENT_DECISIONS = new AtomicLong();
    private static final AtomicLong CONCURRENT_REFUSALS = new AtomicLong();
    private static boolean candidate;
    private static volatile long blackhole;

    static {
        try {
            Class<?> storage = Class.forName("java.lang.M3String");
            Class<?> facts = Class.forName("java.lang.M3StringFacts");
            Class<?> dp = Class.forName("java.lang.M3StringRegexDp");
            STRING_M3 = field(String.class, "m3");
            COMPACT_STRINGS = field(String.class, "COMPACT_STRINGS");
            TABLES = field(dp, "caches");
            FACT_LENGTH = field(facts, "utf16Length");
            FACT_CODE_POINTS = field(facts, "codePointCount");
            OWNER = method(storage, "owner");
            START = method(storage, "start");
            FACTS_IF_PREPARED = method(storage, "factsIfPrepared");
            TRY_MATCH = method(dp, "tryMatch", storage, String.class);
            READY = method(storage, "ready");
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || !(args[0].equals("baseline") || args[0].equals("candidate"))) {
            throw new IllegalArgumentException("baseline|candidate acceptance|resource [iterations]");
        }
        candidate = args[0].equals("candidate");
        check((boolean) READY.invoke(null), "M3 activation must have occurred before main");
        System.out.println("M3_PRODUCT_ENV|mode=" + args[0]
                + "|compact_strings=" + COMPACT_STRINGS.getBoolean(null)
                + "|vm=" + System.getProperty("java.vm.name")
                + "|vm_info=" + System.getProperty("java.vm.info")
                + "|runtime=" + System.getProperty("java.runtime.version"));
        if (args[1].equals("resource")) {
            resources(args.length > 2 ? Integer.parseInt(args[2]) : 32);
        } else if (args[1].equals("acceptance")) {
            coldAndPublicPreparation();
            exactRanges();
            compositionAndSurrogates();
            classBoundariesAndFallbacks();
            largeInputs();
            concurrentRanges();
        } else {
            throw new IllegalArgumentException("unknown task: " + args[1]);
        }
        System.out.println("M3_COMMON_REGEX_PRODUCT_PASS|mode=" + args[0]
                + "|task=" + args[1] + "|checks=" + CHECKS.get()
                + "|public_matches_checks=" + PUBLIC_CHECKS.get()
                + "|oracle_evaluations=" + ORACLES.get()
                + "|concurrent_decisions=" + CONCURRENT_DECISIONS.get()
                + "|concurrent_refusals=" + CONCURRENT_REFUSALS.get());
    }

    private static void coldAndPublicPreparation() throws Exception {
        Text text = atom("fresh-digits", digits(97, 43));
        cold(text);
        // A one-character cold expression must not create optional DP tables.
        {
            Reference<?> tablesRootBefore = tableReference();
            Object tablesBefore = tableReferent(tablesRootBefore);
            try {
                equal(decision(text, "."), -1, "cold single dot delegates");
                publicOracle(text, ".");
                Reference<?> tablesRootAfter = tableReference();
                Object tablesAfter = tableReferent(tablesRootAfter);
                same(tablesAfter, tablesBefore, "cold single dot retains table identity");
                // A cleared new generation must not hide cold table preparation.
                if (tablesRootAfter != tablesRootBefore) {
                    throw new AssertionError("cold single dot retains metadata root identity");
                }
            } finally {
                Reference.reachabilityFence(tablesBefore);
            }
        }
        cold(text);
        equal(decision(text, "\\d+"), -1, "cold digit class delegates");
        publicOracle(text, "\\d+");
        publicOracle(text, "\\D+");
        cold(text);
        Object prepared = prepare(text);
        preparedClass(text, "\\d+");
        preparedClass(text, "\\D+");
        preparedClass(text, "[0-9]{97}");
        preparedClass(text, "[^0-9]{97}");
        preparedClass(text, "\\p{Digit}{97}");
        preparedClass(text, ".{97}");
        preparedClass(text, "\\d{98,}");
        preparedClass(text, "\\d{0,2147483647}");
        // Exercise an admitted regex argument as well as literal regex arguments.
        String canonicalRegex = new String("\\d{97}".toCharArray());
        storage(canonicalRegex);
        preparedClass(text, canonicalRegex);
        same(prepared(text), prepared, "queries retain the prepared facts object");
        System.out.println("M3_PRODUCT_SECTION|cold_and_public_preparation=PASS");
    }

    private static void exactRanges() throws Exception {
        char[] first = digits(101, 67);
        char[] second = repeated('q', 101);
        char[] all = concatenate(first, new char[] {'!'}, second, new char[] {'?'});
        Text parent = atom("range-parent", all);
        Object parentFacts = prepare(parent);
        Text digit = slice(parent, "digit-range", 0, 101);
        Text letter = slice(parent, "letter-range", 102, 203);
        same(owner(digit), owner(parent), "substring keeps the canonical owner");
        same(owner(letter), owner(parent), "other substring keeps the canonical owner");
        equal((int) START.invoke(storage(letter.actual)), 102, "range coordinate is exact");
        cold(digit);
        cold(letter);
        publicOracle(digit, "\\d+");
        publicOracle(letter, "\\d+");
        cold(digit);
        cold(letter);
        Object digitFacts = prepare(digit);
        cold(letter);
        Text sameDigit = slice(parent, "same-digit-coordinate", 0, 101);
        same(prepared(sameDigit), digitFacts, "equal owner and coordinate reuse exact facts");
        preparedClass(digit, "\\d+");
        prepare(letter);
        preparedClass(letter, "\\d+");
        preparedClass(letter, "[a-z]{101}");
        preparedClass(digit, "[a-z]{101}");
        same(prepared(parent), parentFacts, "range publication preserves whole-owner facts");
        System.out.println("M3_PRODUCT_SECTION|exact_ranges=PASS");
    }

    private static void compositionAndSurrogates() throws Exception {
        Text left = atom("surrogate-left", new char[] {'X', '\ud83d'});
        Text right = atom("surrogate-right", new char[] {'\ude42', 'Y'});
        Text joined = new Text("pair-across-concat", left.actual.concat(right.actual),
                concatenate(left.units, right.units));
        check(owner(joined).getClass().getName().equals("java.lang.M3StringTuple"),
                "separate canonical atoms concatenate as a tuple");
        prepare(joined);
        preparedClass(joined, "\\D{3}");
        preparedClass(joined, ".{3}");
        preparedClass(joined, ".{4}");
        preparedClass(joined, "[^0-9]{3}");
        for (Text part : List.of(slice(joined, "whole-pair", 1, 3),
                slice(joined, "lone-high-range", 1, 2),
                slice(joined, "lone-low-range", 2, 3))) {
            prepare(part);
            preparedClass(part, ".");
            preparedClass(part, "\\D");
            preparedClass(part, "\\W");
            preparedClass(part, "\\S");
            preparedClass(part, ".{2}");
        }
        Text reversed = atom("unpaired-low-high", new char[] {'\ude42', '\ud83d'});
        prepare(reversed);
        preparedClass(reversed, ".{2}");
        preparedClass(reversed, "\\D{1}");
        System.out.println("M3_PRODUCT_SECTION|composition_and_surrogates=PASS");
    }

    private static void classBoundariesAndFallbacks() throws Exception {
        Text vertical = atom("vertical-boundaries", new char[] {
                '\r', '\n', '\u000b', '\f', '\u0085', '\u2028', '\u2029'});
        prepare(vertical);
        for (String regex : new String[] {"\\v+", "\\V+", "\\s+", "\\S+", ".{7}"}) {
            preparedClass(vertical, regex);
        }
        Text dotControls = atom("dot-vt-ff", new char[] {'\u000b', '\f'});
        prepare(dotControls);
        preparedClass(dotControls, ".{2}");
        preparedClass(dotControls, "\\v{2}");
        Text horizontal = atom("horizontal-boundaries", new char[] {
                '\t', ' ', '\u00a0', '\u1680', '\u180e', '\u2000', '\u200a',
                '\u202f', '\u205f', '\u3000'});
        prepare(horizontal);
        preparedClass(horizontal, "\\h+");
        preparedClass(horizontal, "\\s+");
        fallback(horizontal, "(?U)\\s+");
        Text unicodeDigit = atom("unicode-digits", new char[] {'\u0661', '\ud835', '\udfd8', '5'});
        prepare(unicodeDigit);
        preparedClass(unicodeDigit, "\\d+");
        preparedClass(unicodeDigit, "\\D+");
        preparedClass(unicodeDigit, "\\w+");
        fallback(unicodeDigit, "(?U)\\d{3}");
        fallback(unicodeDigit, "(?U)\\w+");
        Text ascii = atom("ascii-class-aliases", "Ab9_Ff0".toCharArray());
        prepare(ascii);
        for (String regex : new String[] {"\\w+", "[A-Za-z0-9_]+", "\\p{Alnum}+",
                "\\p{ASCII}+", "\\p{Alpha}+", "[0-9a-f]+", "[0-9A-F]+"}) {
            preparedClass(ascii, regex);
        }
        Text digits = atom("fallback-digit-input", digits(83, 127));
        prepare(digits);
        for (String regex : new String[] {"(?U)\\d+", "^\\d+$", "(\\d+)",
                "\\d++", "\\d+?", "\\d{2,1}", "\\d{2147483648}",
                "\\d{0,2147483648}", "\\d{", "[0-9", "\\d|x", "\\R"}) {
            fallback(digits, regex);
        }
        fallback(digits, "\\d{" + "0".repeat(129) + "83}");
        Text crlf = atom("crlf-R", new char[] {'\r', '\n'});
        prepare(crlf);
        preparedClass(crlf, "\\v{2}");
        preparedClass(crlf, ".{2}");
        fallback(crlf, "\\R");
        fallback(crlf, "(?s).");
        fallback(crlf, "(?s).{2}");
        publicOracle(digits, null);
        Text empty = new Text("empty-public-string", new String(new char[0]), new char[0]);
        for (String regex : new String[] {"\\d*", "\\D+", "\\s{0}", ".?", ".", "[0-9]{0,0}"}) {
            publicOracle(empty, regex);
        }
        System.out.println("M3_PRODUCT_SECTION|class_boundaries_and_fallbacks=PASS");
    }

    private static void largeInputs() throws Exception {
        Text unit = atom("repeat-seed", new char[] {'2', '7', '1'});
        char[] units = new char[3 * 4097];
        for (int i = 0; i < units.length; i++) units[i] = unit.units[i % 3];
        Text repeated = new Text("repeat-over-old-cap", unit.actual.repeat(4097), units);
        check(owner(repeated).getClass().getName().equals("java.lang.M3StringTuple"),
                "repeat keeps a composite canonical owner");
        check(repeated.units.length > 8192, "fixture exceeds old text budget");
        cold(repeated);
        equal(decision(repeated, "\\d+"), -1, "cold long source delegates");
        publicOracle(repeated, "\\d+");
        cold(repeated);
        prepare(repeated);
        preparedClass(repeated, "\\d+");
        preparedClass(repeated, "\\D+");
        preparedClass(repeated, "[0-9]{12291}");
        Text middle = slice(repeated, "long-exact-range", 1, units.length - 1);
        cold(middle);
        publicOracle(middle, "\\d+");
        prepare(middle);
        preparedClass(middle, "[0-9]{12289}");
        preparedClass(middle, "\\d{12290,}");
        char[] almostDigits = digits(16385, 233);
        almostDigits[almostDigits.length - 1] = 'X';
        Text lateFailure = atom("long-late-class-failure", almostDigits);
        cold(lateFailure);
        publicOracle(lateFailure, "\\d+");
        prepare(lateFailure);
        preparedClass(lateFailure, "\\d+");
        preparedClass(lateFailure, "\\w+");
        System.out.println("M3_PRODUCT_SECTION|large_inputs=PASS|max_utf16_units=16385");
    }

    private static void concurrentRanges() throws Exception {
        final int count = 8;
        final int width = 113;
        char[] all = new char[count * (width + 1)];
        for (int r = 0; r < count; r++) {
            for (int j = 0; j < width; j++) {
                all[r * (width + 1) + j] = switch (r) {
                    case 0 -> (char) ('0' + j % 10);
                    case 1 -> (char) ('a' + j % 26);
                    case 2 -> j == width - 1 ? 'Z' : '8';
                    case 3 -> j % 2 == 0 ? ' ' : '\t';
                    case 4 -> j % 2 == 0 ? '\ud83d' : '\ude42';
                    case 5 -> '\u0661';
                    case 6 -> j % 2 == 0 ? '_' : '9';
                    default -> j % 2 == 0 ? '\r' : '\n';
                };
            }
            all[r * (width + 1) + width] = '!';
        }
        Text parent = atom("concurrent-range-parent", all);
        Object parentFacts = prepare(parent);
        Text[] ranges = new Text[count];
        String[] regexes = {"\\d+", "\\D+", "\\w+", "\\W+"};
        boolean[][] expected = new boolean[count][regexes.length];
        for (int r = 0; r < count; r++) {
            ranges[r] = slice(parent, "concurrent-range-" + r,
                    r * (width + 1), r * (width + 1) + width);
            same(owner(ranges[r]), owner(parent), "all concurrent ranges share one owner");
            for (int p = 0; p < regexes.length; p++) expected[r][p] = oracle(ranges[r].units, regexes[p]);
        }
        ExecutorService workers = Executors.newFixedThreadPool(4);
        try {
            for (int round = 0; round < 6; round++) {
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> tasks = new ArrayList<>();
                for (int worker = 0; worker < 4; worker++) {
                    final int lane = worker;
                    final int roundNumber = round;
                    tasks.add(workers.submit(() -> {
                        start.await();
                        for (int j = 0; j < 24; j++) {
                            int r = lane * 2 + (j & 1);
                            int p = (j + roundNumber) % regexes.length;
                            Text text = ranges[r];
                            text.actual.strip();
                            int result = decision(text, regexes[p]);
                            if (!candidate) equal(result, -1, "baseline concurrent common-class refusal");
                            if (result == -1) {
                                // Another range may evict this exact coordinate after strip().
                                CONCURRENT_REFUSALS.incrementAndGet();
                            } else {
                                equal(result == 1, expected[r][p], "concurrent prepared decision");
                                CONCURRENT_DECISIONS.incrementAndGet();
                            }
                            equal(text.actual.matches(regexes[p]), expected[r][p], "concurrent String.matches");
                            PUBLIC_CHECKS.incrementAndGet();
                            Object facts = prepared(text);
                            if (facts != null) verifyFacts(text, facts);
                        }
                        return null;
                    }));
                }
                start.countDown();
                for (Future<?> task : tasks) task.get(60, TimeUnit.SECONDS);
                System.gc();
                same(prepared(parent), parentFacts, "GC and range eviction retain parent facts");
            }
        } finally {
            workers.shutdownNow();
            check(workers.awaitTermination(60, TimeUnit.SECONDS), "workers terminate");
        }
        if (candidate) check(CONCURRENT_DECISIONS.get() != 0, "concurrent prepared lane was exercised");
        System.out.println("M3_PRODUCT_SECTION|concurrent_ranges=PASS|threads=4|ranges=8|rounds=6|gc_requests=6");
    }

    private static void resources(int iterations) throws Exception {
        if (iterations < 1 || iterations > 10000) throw new IllegalArgumentException("bounded iterations required");
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (bean.isThreadCpuTimeSupported() && !bean.isThreadCpuTimeEnabled()) bean.setThreadCpuTimeEnabled(true);
        if (bean.isThreadAllocatedMemorySupported() && !bean.isThreadAllocatedMemoryEnabled()) {
            bean.setThreadAllocatedMemoryEnabled(true);
        }
        Text[] cohort = {
                atom("resource-digits", digits(16385, 521)),
                atom("resource-letters", repeated('k', 16385)),
                atom("resource-late-failure", digits(16385, 733)),
                atom("resource-short-word", repeated('_', 97))};
        // Construct the negative source from its own original units, before measuring.
        char[] negative = cohort[2].units.clone();
        negative[negative.length - 1] = '!';
        cohort[2] = atom("resource-late-failure", negative);
        String[] patterns = {"\\d+", "\\D+", "\\d+", "[A-Za-z0-9_]+"};
        boolean[] expected = new boolean[cohort.length];
        for (int i = 0; i < cohort.length; i++) {
            cold(cohort[i]);
            expected[i] = oracle(cohort[i].units, patterns[i]);
        }
        runWork(cohort, patterns, expected, 2);
        snapshot(bean); // Resolve management counters outside the measured workload.
        System.gc();
        measure("counter_overhead", 0, new Text[0], new String[0], new boolean[0], 0, bean);
        for (int sample = 0; sample < 3; sample++) {
            measure("cold", sample, cohort, patterns, expected, iterations, bean);
        }
        for (Text text : cohort) cold(text);
        Snapshot before = snapshot(bean);
        for (Text text : cohort) text.actual.strip();
        Snapshot after = snapshot(bean);
        printMeasurement("prepare_public_strip", 0, cohort.length, before, after);
        for (int i = 0; i < cohort.length; i++) {
            check(prepared(cohort[i]) != null, "resource facts prepared by public strip");
            equal(decision(cohort[i], patterns[i]), candidate ? (expected[i] ? 1 : 0) : -1,
                    "resource prepared class decision");
        }
        System.gc();
        for (int sample = 0; sample < 3; sample++) {
            measure("prepared", sample, cohort, patterns, expected, iterations, bean);
        }
        Text[] control = {cohort[0]};
        String[] controlPatterns = {"(?U)\\d+"};
        boolean[] controlExpected = {true};
        equal(decision(control[0], controlPatterns[0]), -1, "flagged resource control delegates");
        runWork(control, controlPatterns, controlExpected, 2);
        for (int sample = 0; sample < 3; sample++) {
            measure("prepared_flagged_fallback_control", sample, control, controlPatterns,
                    controlExpected, iterations, bean);
        }
        System.out.println("M3_PRODUCT_RESOURCE_SCOPE|iterations_per_sample=" + iterations
                + "|cohort=4|long_strings=3|long_length=16385|short_length=97|samples_per_phase=3"
                + "|timings=observational|native_memory=process_shutdown_NMT|blackhole=" + blackhole);
    }

    private static void measure(String phase, int sample, Text[] text, String[] patterns,
            boolean[] expected, int iterations, com.sun.management.ThreadMXBean bean) {
        Snapshot before = snapshot(bean);
        runWork(text, patterns, expected, iterations);
        Snapshot after = snapshot(bean);
        printMeasurement(phase, sample, (long) text.length * iterations, before, after);
    }

    private static void runWork(Text[] text, String[] patterns, boolean[] expected, int iterations) {
        long accepted = 0;
        for (int round = 0; round < iterations; round++) {
            for (int i = 0; i < text.length; i++) {
                boolean actual = text[i].actual.matches(patterns[i]);
                if (actual != expected[i]) throw new AssertionError("resource public matches mismatch");
                if (actual) accepted++;
            }
        }
        PUBLIC_CHECKS.addAndGet((long) iterations * text.length);
        blackhole = accepted;
    }

    private static Snapshot snapshot(com.sun.management.ThreadMXBean bean) {
        long cpu = bean.isThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : -1;
        long allocated = bean.isThreadAllocatedMemorySupported() ? bean.getCurrentThreadAllocatedBytes() : -1;
        long heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        return new Snapshot(System.nanoTime(), cpu, allocated, heap);
    }

    private static void printMeasurement(String phase, int sample, long calls, Snapshot before, Snapshot after) {
        System.out.println("M3_PRODUCT_RESOURCE|phase=" + phase + "|sample=" + sample + "|calls=" + calls
                + "|wall_ns=" + (after.wall - before.wall)
                + "|thread_cpu_ns=" + delta(before.cpu, after.cpu)
                + "|thread_allocated_bytes=" + delta(before.allocated, after.allocated)
                + "|heap_before_bytes=" + before.heap + "|heap_after_bytes=" + after.heap);
    }

    private static long delta(long before, long after) { return before < 0 || after < 0 ? -1 : after - before; }
    private record Snapshot(long wall, long cpu, long allocated, long heap) { }
    private record Text(String name, String actual, char[] units) { }

    private static Text atom(String name, char[] units) throws Exception {
        Text text = new Text(name, new String(units), units);
        storage(text.actual);
        return text;
    }

    private static Text slice(Text parent, String name, int begin, int end) throws Exception {
        Text text = new Text(name, parent.actual.substring(begin, end), Arrays.copyOfRange(parent.units, begin, end));
        storage(text.actual);
        return text;
    }

    private static Object storage(String text) throws Exception {
        Object storage = STRING_M3.get(text);
        check(storage != null, "public construction must have canonical M3 storage");
        return storage;
    }

    private static Object owner(Text text) throws Exception { return OWNER.invoke(storage(text.actual)); }
    private static Object prepared(Text text) throws Exception { return FACTS_IF_PREPARED.invoke(storage(text.actual)); }
    private static int decision(Text text, String regex) throws Exception {
        return (int) TRY_MATCH.invoke(null, storage(text.actual), regex);
    }

    private static void cold(Text text) throws Exception {
        check(prepared(text) == null, "expected cold exact range: " + text.name);
    }

    private static Object prepare(Text text) throws Exception {
        text.actual.strip();
        Object facts = prepared(text);
        check(facts != null, "public strip must prepare exact range: " + text.name);
        verifyFacts(text, facts);
        return facts;
    }

    private static void verifyFacts(Text text, Object facts) throws Exception {
        equal(FACT_LENGTH.getInt(facts), text.units.length, "prepared exact UTF-16 length");
        equal(FACT_CODE_POINTS.getInt(facts), Character.codePointCount(text.units, 0, text.units.length),
                "prepared exact code-point count");
    }

    private static void preparedClass(Text text, String regex) throws Exception {
        boolean expected = oracle(text.units, regex);
        int actualDecision = decision(text, regex);
        equal(actualDecision, candidate ? (expected ? 1 : 0) : -1,
                "PREPARED_CLASS_DECISION " + text.name + " / " + regex);
        equal(text.actual.matches(regex), expected, "prepared public String.matches");
        PUBLIC_CHECKS.incrementAndGet();
    }

    private static void fallback(Text text, String regex) throws Exception {
        equal(decision(text, regex), -1, "unsupported expression must delegate");
        publicOracle(text, regex);
    }

    private static boolean oracle(char[] text, String regex) {
        ORACLES.incrementAndGet();
        return Pattern.compile(regex).matcher(new StringBuilder(text.length).append(text)).matches();
    }

    private static void publicOracle(Text text, String regex) {
        Boolean expected = null;
        RuntimeException expectedFailure = null;
        try { expected = oracle(text.units, regex); }
        catch (RuntimeException failure) { expectedFailure = failure; }
        Boolean actual = null;
        RuntimeException actualFailure = null;
        try { actual = text.actual.matches(regex); }
        catch (RuntimeException failure) { actualFailure = failure; }
        PUBLIC_CHECKS.incrementAndGet();
        if (expectedFailure == null) {
            check(actualFailure == null, "public String.matches unexpectedly throws");
            equal(actual.booleanValue(), expected.booleanValue(), "public String.matches oracle");
        } else {
            check(actualFailure != null && actualFailure.getClass() == expectedFailure.getClass(),
                    "exception type parity: " + text.name);
            if (expectedFailure instanceof PatternSyntaxException expectedSyntax) {
                PatternSyntaxException actualSyntax = (PatternSyntaxException) actualFailure;
                equal(actualSyntax.getIndex(), expectedSyntax.getIndex(), "syntax error index parity");
                check(actualSyntax.getDescription().equals(expectedSyntax.getDescription()), "syntax description parity");
                check(actualSyntax.getPattern().equals(expectedSyntax.getPattern()), "syntax pattern parity");
            }
        }
    }

    private static char[] digits(int length, int seed) {
        char[] result = new char[length];
        long state = seed;
        for (int i = 0; i < length; i++) {
            state = state * 1664525L + 1013904223L;
            result[i] = (char) ('0' + (int) ((state >>> 16) % 10));
        }
        return result;
    }

    private static char[] repeated(char value, int count) {
        char[] result = new char[count];
        Arrays.fill(result, value);
        return result;
    }

    private static char[] concatenate(char[]... parts) {
        int length = 0;
        for (char[] part : parts) length = Math.addExact(length, part.length);
        char[] result = new char[length];
        int offset = 0;
        for (char[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }

    /** Observe the current reclaimable root without preparing optional DP metadata. */
    private static Reference<?> tableReference() throws ReflectiveOperationException {
        return (Reference<?>) TABLES.get(null);
    }

    /** Nullable referent observation; SoftReference.get may refresh its timestamp. */
    private static Object tableReferent(Reference<?> root) {
        return root == null ? null : root.get();
    }

    private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static Method method(Class<?> type, String name, Class<?>... arguments) throws ReflectiveOperationException {
        Method result = type.getDeclaredMethod(name, arguments);
        result.setAccessible(true);
        return result;
    }

    private static void same(Object actual, Object expected, String label) { check(actual == expected, label); }
    private static void equal(int actual, int expected, String label) {
        check(actual == expected, label + " actual=" + actual + " expected=" + expected);
    }
    private static void equal(boolean actual, boolean expected, String label) { check(actual == expected, label); }
    private static void check(boolean condition, String label) {
        CHECKS.incrementAndGet();
        if (!condition) throw new AssertionError(label);
    }
}
