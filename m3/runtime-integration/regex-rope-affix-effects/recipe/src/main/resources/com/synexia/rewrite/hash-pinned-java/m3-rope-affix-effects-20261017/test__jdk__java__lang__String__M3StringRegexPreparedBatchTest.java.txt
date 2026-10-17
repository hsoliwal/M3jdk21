/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Prepared regex effects preserve UTF-16 joins, exact row truth and JNI ownership
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm -Xcheck:jni --add-opens=java.base/jdk.internal.mindex=ALL-UNNAMED M3StringRegexPreparedBatchTest
 * @run main/othervm -Xint -Xcheck:jni --add-opens=java.base/jdk.internal.mindex=ALL-UNNAMED M3StringRegexPreparedBatchTest
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3RegexDpPlan;
import jdk.internal.mindex.M3RegexDpPlan.BatchBackend;
import jdk.internal.mindex.M3RegexDpPlan.BatchExecution;
import jdk.internal.mindex.M3RegexDpPlan.BatchLimits;
import jdk.internal.mindex.M3RegexDpPlan.BatchResult;
import jdk.internal.mindex.M3RegexDpPlan.Effect;
import jdk.internal.mindex.M3RegexDpPlan.PreparedBatch;

public final class M3StringRegexPreparedBatchTest {
    private static final BatchLimits LIMITS = BatchLimits.defaults();
    private static final String[] PIECES = { "", "a", "b", "ab", "\u0000", "\n", "\r",
            "\u0085", "\u2028", "\u2029", "\u00e9", "\ud83d", "\ude42", "\ud83d\ude42",
            "\ud83d\ud83d", "\ude42\ude42", "\ude42\ud83d", "a\ud83d", "\ude42b",
            "b".repeat(62), "a".repeat(62), "b".repeat(31) + "\ud83d\ude42" + "b".repeat(30) };
    private static long checks, nativeRows, cudaRows;
    private static BatchBackend strict = BatchBackend.JNI;
    private static boolean nativeAvailable = true;

    public static void main(String[] args) throws Exception {
        if (args.length > 1) throw new AssertionError("unexpected arguments");
        if (args.length == 1 && args[0].equals("java-only")) nativeAvailable = false;
        else if (args.length == 1 && args[0].equals("cuda")) strict = BatchBackend.CUDA;
        else if (args.length != 0) throw new AssertionError("unknown execution mode");
        // Local patch-module qualification can attach the same C owner to a stock JDK.
        // A complete target JDK resolves its CPU entry points from libjava directly.
        String localLibrary = System.getProperty("m3.regex.batch.test.library");
        if (localLibrary != null) {
            Class.forName("jdk.internal.mindex.M3RegexPreparedBatchTestSupport")
                    .getMethod("load", String.class).invoke(null, localLibrary);
        }
        String cudaLibrary = System.getProperty("m3.regex.batch.test.cuda.library");
        if (cudaLibrary != null) M3RegexDpPlan.loadCudaBatchProvider(cudaLibrary);
        semanticParity();
        admissionAndSnapshots();
        checkpointsAndClose();
        if (nativeAvailable) rawNativeBoundary();
        System.out.println("M3_STRING_REGEX_PREPARED_BATCH_PASS checks=" + checks
                + " jniRows=" + nativeRows + " cudaRows=" + cudaRows
                + " requested=" + (nativeAvailable ? strict : "JAVA_ONLY"));
    }

    private static void semanticParity() {
        List<int[]> rows = new ArrayList<>();
        rows.add(new int[0]);
        for (int first = 0; first < PIECES.length; first++) {
            rows.add(new int[] { first });
            for (int second = 0; second < PIECES.length; second++) {
                rows.add(new int[] { first, 0, second });
            }
        }
        Random random = new Random(0x4d33505245504152L);
        for (int trial = 0; trial < 350; trial++) {
            int[] row = new int[random.nextInt(20)];
            for (int i = 0; i < row.length; i++) row[i] = random.nextInt(PIECES.length);
            rows.add(row);
        }
        int[] offsets = new int[rows.size() + 1];
        for (int i = 0; i < rows.size(); i++) offsets[i + 1] = offsets[i] + rows.get(i).length;
        int[] tuples = new int[offsets[rows.size()]];
        for (int i = 0; i < rows.size(); i++) {
            System.arraycopy(rows.get(i), 0, tuples, offsets[i], rows.get(i).length);
        }
        String[] patterns = { "a*", ".*", "a*b*", ".*a.*", "a*.", "a*..", "a*.b*",
                "a*..*b*", "a*" + ".".repeat(62) };
        String[][] contexts = { { null, null }, { "", "" }, { "\ud83d", "\ude42" },
                { "a\ud83d", "\ude42b" }, { "\ude42", "\ud83d" }, { "a", "b" } };
        for (String regex : patterns) {
            M3RegexDpPlan plan = requiredPlan(regex);
            Effect[] effects = new Effect[PIECES.length];
            for (int i = 0; i < effects.length; i++) effects[i] = plan.summarizeUtf16(PIECES[i], null);
            Pattern oracle = Pattern.compile(regex);
            for (String[] context : contexts) {
                Effect prefix = context[0] == null ? null : plan.summarizeUtf16(context[0], null);
                Effect suffix = context[1] == null ? null : plan.summarizeUtf16(context[1], null);
                try (PreparedBatch batch = plan.prepareBatch(prefix, effects, tuples, offsets, suffix, LIMITS, null)) {
                    truth(batch != null, "bounded batch admitted");
                    BatchResult java = batch.matches(BatchBackend.JAVA, null);
                    truth(java.execution() == BatchExecution.JAVA, "Java execution receipt");
                    BatchResult accelerated = nativeAvailable ? batch.matches(strict, null)
                            : batch.matches(BatchBackend.PREFER_JNI, null);
                    BatchExecution expected = nativeAvailable
                            ? strict == BatchBackend.CUDA ? BatchExecution.CUDA : BatchExecution.JNI
                            : BatchExecution.JAVA;
                    truth(accelerated.execution() == expected, "actual execution receipt");
                    if (expected == BatchExecution.JNI) nativeRows += rows.size();
                    if (expected == BatchExecution.CUDA) cudaRows += rows.size();
                    for (int row = 0; row < rows.size(); row++) {
                        StringBuilder text = new StringBuilder();
                        if (context[0] != null) text.append(context[0]);
                        for (int atom : rows.get(row)) text.append(PIECES[atom]);
                        if (context[1] != null) text.append(context[1]);
                        boolean expectedTruth = oracle.matcher(text).matches();
                        truth(java.matches(row) == expectedTruth, "Java UTF16 oracle: " + regex);
                        truth(accelerated.matches(row) == expectedTruth, "native/fallback UTF16 oracle: " + regex);
                        truth(java.utf16Length(row) == text.length(), "UTF16 unit length");
                        truth(accelerated.utf16Length(row) == text.length(), "native/fallback UTF16 unit length");
                    }
                    BatchResult again = batch.matches(nativeAvailable ? strict : BatchBackend.JAVA, null);
                    for (int row = 0; row < rows.size(); row++) {
                        truth(again.matches(row) == java.matches(row), "resident image reuse");
                    }
                }
            }
        }
    }

    private static void admissionAndSnapshots() {
        M3RegexDpPlan plan = requiredPlan("a*.");
        Effect high = plan.summarizeUtf16("\ud83d", null);
        Effect low = plan.summarizeUtf16("\ude42", null);
        Effect[] effects = { high, plan.identity(), low };
        int[] tuples = { 0, 1, 2 };
        int[] offsets = { 0, 3 };
        try (PreparedBatch batch = plan.prepareBatch(effects, tuples, offsets, LIMITS, null)) {
            effects[0] = requiredPlan("b*").identity();
            Arrays.fill(tuples, -1);
            Arrays.fill(offsets, -1);
            truth(batch.matches(BatchBackend.JAVA, null).matches(0), "detached tuple coordinates and pins");
            if (nativeAvailable) truth(batch.matches(strict, null).matches(0), "native split pair through empty");
        }
        Effect[] empty = new Effect[0];
        try (PreparedBatch batch = plan.prepareBatch(high, empty, new int[0], new int[] {0, 0},
                low, LIMITS, null)) {
            truth(batch.matches(BatchBackend.JAVA, null).matches(0), "pair across empty row and affixes");
            if (nativeAvailable) truth(batch.matches(strict, null).matches(0), "native pair across empty row");
        }
        // Fixed affixes are prepared once and do not consume the per-call tuple budget.
        int[] maximumRow = new int[65_536];
        Effect[] identities = { plan.identity() };
        try (PreparedBatch batch = plan.prepareBatch(high, identities, maximumRow,
                new int[] {0, maximumRow.length}, low, LIMITS, null)) {
            truth(batch != null, "maximum tuple row with fixed affixes admitted");
            truth(batch.matches(BatchBackend.JAVA, null).matches(0), "factored Java affixes through maximum empty tuple");
            if (nativeAvailable) {
                truth(batch.matches(strict, null).matches(0), "factored native affixes through maximum empty tuple");
            }
        }
        truth(plan.prepareBatch(high, identities, new int[65_537], new int[] {0, 65_537},
                low, LIMITS, null) == null, "oversized tuple row remains UNKNOWN");
        try (PreparedBatch batch = plan.prepareBatch(empty, new int[0], new int[] {0}, LIMITS, null)) {
            BatchResult result = batch.matches(BatchBackend.CUDA, null);
            truth(result.rowCount() == 0 && result.execution() == BatchExecution.NONE, "empty batch executes nothing");
        }
        truth(plan.prepareBatch(new Effect[] {high}, new int[] {0}, new int[] {0, 1},
                new BatchLimits(0, 1, 1, Long.MAX_VALUE, Long.MAX_VALUE), null) == null, "effect budget refusal");
        truth(plan.prepareBatch(empty, new int[0], new int[] {0, 0},
                new BatchLimits(1, 0, 0, Long.MAX_VALUE, Long.MAX_VALUE), null) == null, "row budget refusal");
        truth(plan.prepareBatch(empty, new int[0], new int[] {0},
                new BatchLimits(1, 1, 1, 0, Long.MAX_VALUE), null) == null, "numeric budget refusal");
        truth(plan.prepareBatch(new Effect[] {high}, new int[] {0}, new int[] {0, 1},
                new BatchLimits(1, 1, 1, Long.MAX_VALUE, 0), null) == null, "work budget refusal");
        expect(IllegalArgumentException.class, () -> new BatchLimits(-1, 0, 0, 0, 0));
        expect(IllegalArgumentException.class, () -> plan.prepareBatch(empty, new int[0], new int[0], LIMITS, null));
        expect(IllegalArgumentException.class, () -> plan.prepareBatch(new Effect[] {high},
                new int[] {1}, new int[] {0, 1}, LIMITS, null));
        expect(IllegalArgumentException.class, () -> plan.prepareBatch(empty,
                new int[0], new int[] {0, -1, 0}, LIMITS, null));
        expect(IllegalArgumentException.class, () -> plan.prepareBatch(
                new Effect[] {requiredPlan("b*").identity()}, new int[0], new int[] {0}, LIMITS, null));
        Effect huge = plan.summarizeUtf16("a", null).repeated(Long.MAX_VALUE, null);
        expect(ArithmeticException.class, () -> plan.prepareBatch(new Effect[] {huge, high},
                new int[] {0, 1}, new int[] {0, 2}, LIMITS, null));
        try (PreparedBatch batch = plan.prepareBatch(new Effect[] {high}, new int[] {0}, new int[] {0, 1}, LIMITS, null)) {
            if (strict != BatchBackend.CUDA) {
                expect(UnsupportedOperationException.class, () -> batch.matches(BatchBackend.CUDA, null));
            }
            if (!nativeAvailable) expect(UnsupportedOperationException.class, () -> batch.matches(BatchBackend.JNI, null));
            Thread.currentThread().interrupt();
            batch.matches(nativeAvailable ? strict : BatchBackend.JAVA, null);
            truth(Thread.currentThread().isInterrupted(), "null checkpoint preserves interrupt state");
            Thread.interrupted();
        }
        truth(M3RegexDpPlan.prepare("(a)\\1") == null, "unsupported regex stays UNKNOWN at admission");
        truth(M3RegexDpPlan.prepare("(?=a).*") == null, "assertion stays UNKNOWN at admission");
    }

    private static void checkpointsAndClose() throws Exception {
        M3RegexDpPlan plan = requiredPlan(".*");
        Effect[] effects = {plan.summarizeUtf16("\ud83d", null), plan.identity(),
                plan.summarizeUtf16("\ude42", null)};
        int[] tuples = new int[60_000 * 3];
        int[] offsets = new int[60_001];
        for (int i = 0; i < 60_000; i++) {
            tuples[3 * i] = 0;
            tuples[3 * i + 1] = 1;
            tuples[3 * i + 2] = 2;
            offsets[i + 1] = 3 * (i + 1);
        }
        PreparedBatch batch = plan.prepareBatch(effects, tuples, offsets, LIMITS, null);
        AtomicInteger javaChecks = new AtomicInteger(), backendChecks = new AtomicInteger();
        batch.matches(BatchBackend.JAVA, javaChecks::incrementAndGet);
        batch.matches(nativeAvailable ? strict : BatchBackend.PREFER_JNI, backendChecks::incrementAndGet);
        truth(javaChecks.get() == backendChecks.get() && javaChecks.get() > 3, "backend checkpoint schedule parity");
        RuntimeException cancelled = new RuntimeException("checkpoint cancellation");
        AtomicInteger calls = new AtomicInteger();
        try {
            batch.matches(nativeAvailable ? strict : BatchBackend.JAVA, () -> {
                if (calls.incrementAndGet() == 3) throw cancelled;
            });
            throw new AssertionError("cancelled execution returned truth");
        } catch (RuntimeException expected) {
            truth(expected == cancelled, "original callback exception identity");
        }
        truth(batch.matches(BatchBackend.JAVA, null).matches(0), "cancel leaves prepared owner usable");
        AtomicInteger closeCalls = new AtomicInteger();
        expect(IllegalStateException.class, () -> batch.matches(nativeAvailable ? strict : BatchBackend.JAVA, () -> {
            if (closeCalls.incrementAndGet() == 3) batch.close();
        }));
        truth(!batch.isOpen(), "callback close completed without lock upgrade");
        batch.close();
        expect(IllegalStateException.class, () -> batch.matches(BatchBackend.JAVA, null));

        PreparedBatch concurrent = plan.prepareBatch(effects, tuples, offsets, LIMITS, null);
        CountDownLatch paused = new CountDownLatch(1), resume = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            AtomicInteger seen = new AtomicInteger();
            try {
                concurrent.matches(nativeAvailable ? strict : BatchBackend.JAVA, () -> {
                    if (seen.incrementAndGet() == 3) {
                        paused.countDown();
                        try { resume.await(); }
                        catch (InterruptedException interrupted) { throw new AssertionError(interrupted); }
                    }
                });
                failure.set(new AssertionError("close between chunks returned a result"));
            } catch (IllegalStateException expected) {
                // A closed owner cannot publish an incomplete batch result.
            } catch (Throwable problem) { failure.set(problem); }
        });
        worker.start();
        paused.await();
        concurrent.close();
        resume.countDown();
        worker.join();
        truth(failure.get() == null, "concurrent close and evaluation: " + failure.get());
    }

    private static void rawNativeBoundary() throws Exception {
        String kind = strict == BatchBackend.CUDA ? "batchCuda" : "batch";
        Method create = nativeMethod(kind + "Create0", long[].class, long[].class, int.class,
                long.class, long.class, int[].class, int[].class, int.class, int.class, long.class);
        Method evaluate = nativeMethod(kind + "Evaluate0", long.class, int.class, int.class, byte[].class, int.class);
        Method close = nativeMethod(kind + "Close0", long.class);
        long[] lanes = {1, 0, 2, 2, 2, 2};
        long[] dots = {0, 0};
        int[] tuples = {0}, offsets = {0, 1};
        long handle = (Long) invoke(create, lanes, dots, 2, 3L, 2L, tuples, offsets, -1, -1, 1024L);
        Arrays.fill(lanes, 0);
        Arrays.fill(dots, -1);
        tuples[0] = 999;
        offsets[0] = 999;
        byte[] answers = {99, 99, 99};
        invoke(evaluate, handle, 0, 1, answers, 1);
        truth(Arrays.equals(answers, new byte[] {99, 1, 99}), "native copies retain no Java pointer");
        expect(IllegalArgumentException.class, () -> invoke(evaluate, handle, 0, 1, answers, 3));
        expect(IllegalArgumentException.class, () -> invoke(evaluate, handle, 0, 16_385, new byte[16_385], 0));
        expect(IllegalArgumentException.class, () -> invoke(evaluate, handle, 0, 2, new byte[2], 0));
        expect(IllegalStateException.class, () -> invoke(evaluate, 42L, 0, 0, new byte[0], 0));
        invoke(close, handle);
        expect(IllegalStateException.class, () -> invoke(close, handle));
        expect(IllegalStateException.class, () -> invoke(evaluate, handle, 0, 0, new byte[0], 0));
        long[] good = {1, 0, 2, 2, 2, 2};
        expect(NullPointerException.class, () -> invoke(create, null, new long[2], 2, 3L, 2L,
                new int[] {0}, new int[] {0, 1}, -1, -1, 1024L));
        expect(IllegalArgumentException.class, () -> invoke(create, good, new long[2], 0, 3L, 2L,
                new int[] {0}, new int[] {0, 1}, -1, -1, 1024L));
        expect(IllegalArgumentException.class, () -> invoke(create, good, new long[2], 2, 3L, 2L,
                new int[] {0}, new int[] {0, 1}, -1, -1, 1L));
        for (int corrupt : new int[] {0, 1, 2, 4}) {
            long[] bad = good.clone();
            bad[corrupt] = corrupt == 0 ? -1L : corrupt == 1 ? 4L : 8L;
            expect(IllegalArgumentException.class, () -> invoke(create, bad, new long[2], 2, 3L, 2L,
                    new int[] {0}, new int[] {0, 1}, -1, -1, 1024L));
        }
        long[] falseEmpty = good.clone();
        falseEmpty[0] = 0;
        expect(IllegalArgumentException.class, () -> invoke(create, falseEmpty, new long[2], 2, 3L, 2L,
                new int[] {0}, new int[] {0, 1}, -1, -1, 1024L));
        expect(IllegalArgumentException.class, () -> invoke(create, good, new long[2], 2, 3L, 2L,
                new int[] {-1}, new int[] {0, 1}, -1, -1, 1024L));
        expect(IllegalArgumentException.class, () -> invoke(create, good, new long[2], 2, 3L, 2L,
                new int[] {0}, new int[] {0, -1, 1}, -1, -1, 1024L));
        long[] overflow = good.clone();
        overflow[0] = Long.MAX_VALUE;
        expect(IllegalArgumentException.class, () -> invoke(create, overflow, new long[2], 2, 3L, 2L,
                new int[] {0, 0}, new int[] {0, 2}, -1, -1, 1024L));
        rawCloseRace(create, evaluate, close, good);
    }

    private static void rawCloseRace(Method create, Method evaluate, Method close, long[] good) throws Exception {
        for (int trial = 0; trial < 12; trial++) {
            long handle = (Long) invoke(create, good, new long[2], 2, 3L, 2L,
                    new int[] {0}, new int[] {0, 1}, -1, -1, 1024L);
            CountDownLatch start = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread[] workers = new Thread[2];
            for (int i = 0; i < workers.length; i++) {
                workers[i] = new Thread(() -> {
                    try {
                        start.await();
                        byte[] result = new byte[1];
                        for (int attempt = 0; attempt < 100; attempt++) {
                            try {
                                invoke(evaluate, handle, 0, 1, result, 0);
                                if (result[0] != 1) throw new AssertionError("leased row answer");
                            } catch (IllegalStateException closed) { break; }
                        }
                    } catch (Throwable problem) { failure.set(problem); }
                });
                workers[i].start();
            }
            start.countDown();
            invoke(close, handle);
            for (Thread worker : workers) worker.join();
            truth(failure.get() == null, "raw JNI close/acquire lease race: " + failure.get());
        }
    }

    private static M3RegexDpPlan requiredPlan(String regex) {
        M3RegexDpPlan result = M3RegexDpPlan.prepare(regex);
        if (result == null) throw new AssertionError("test pattern rejected: " + regex);
        return result;
    }
    private static Method nativeMethod(String name, Class<?>... parameters) throws Exception {
        Method result = M3RegexDpPlan.class.getDeclaredMethod(name, parameters);
        result.setAccessible(true);
        return result;
    }
    private static Object invoke(Method method, Object... args) {
        try { return method.invoke(null, args); }
        catch (InvocationTargetException problem) {
            Throwable cause = problem.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new AssertionError(cause);
        } catch (ReflectiveOperationException problem) { throw new AssertionError(problem); }
    }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable problem) {
            truth(type.isInstance(problem), "expected " + type + ", got " + problem);
            return;
        }
        throw new AssertionError("expected " + type);
    }
    private static void truth(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label + " at check " + checks);
    }
}
