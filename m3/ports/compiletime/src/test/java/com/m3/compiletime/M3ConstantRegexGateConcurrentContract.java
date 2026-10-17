// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Actual gate source under normal and concurrent Java21 execution.
 * Local Boolean stack is an independent RPN oracle; stock Pattern remains
 * authoritative for find/matches/replaceAll/split.
 */
public final class M3ConstantRegexGateConcurrentContract {
    private static final long SEED = 0x4D33435452584741L;
    private static final AtomicLong CHECKS = new AtomicLong();

    private M3ConstantRegexGateConcurrentContract() {}

    public static void main(String[] args) throws Exception {
        noPerGateMutablePrimitiveStack();
        int[] widths = {1, 2, 16, 63, 64, 65, 96};
        for (int width : widths) {
            for (boolean and : new boolean[] {true, false}) {
                M3ConstantRegexGate gate = synthetic(width, and);
                Random random = new Random(SEED + width * 17L + (and ? 1L : 0L));
                for (int i = 0; i < 600; i++) {
                    String text = sample(width, random);
                    agree(gate, text, syntheticFormula(width, and, text));
                }
            }
        }
        concurrentSharedStaticGate();
        realImageAndJdkPatternParity();
        System.out.println("M3_CONSTANT_REGEX_GATE_CONCURRENT_PASS checks=" + CHECKS.get()
                + " source=java21 shared_gate=true packed_and_wide=true");
    }

    private static void noPerGateMutablePrimitiveStack() {
        for (Field field : M3ConstantRegexGate.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && field.getType() == boolean[].class) {
                throw new AssertionError("mutable precompute stack shared across callers: " + field);
            }
            CHECKS.incrementAndGet();
        }
    }

    private static M3ConstantRegexGate synthetic(int width, boolean conjunction)
            throws Exception {
        String[] atoms = new String[width];
        int[] code = new int[width + 1];
        for (int i = 0; i < width; i++) {
            atoms[i] = "m3:" + String.format(java.util.Locale.ROOT, "%03d", i) + "|";
            code[i] = (M3ConstantRegexImage.OP_ATOM << 24) | i;
        }
        code[width] = ((conjunction
                ? M3ConstantRegexImage.OP_AND : M3ConstantRegexImage.OP_OR) << 24) | width;
        M3ConstantRegexImage.Entry image = new M3ConstantRegexImage.Entry(
                M3RegexRequirement.Kind.REQUIRED, true, code, atoms, new long[width][]);
        Constructor<?> ctor = M3ConstantRegexGate.class
                .getDeclaredConstructor(Pattern.class, Optional.class);
        ctor.setAccessible(true);
        return (M3ConstantRegexGate) ctor.newInstance(Pattern.compile("unused"),
                Optional.of(image));
    }

    private static String sample(int width, Random random) {
        StringBuilder subject = new StringBuilder();
        for (int i = 0; i < width; i++) {
            if (random.nextInt(5) >= 2) {
                subject.append("m3:")
                        .append(String.format(java.util.Locale.ROOT, "%03d", i))
                        .append('|');
            }
        }
        if ((random.nextInt(7) & 1) == 0) subject.append("prefix/suffix");
        return subject.toString();
    }

    private static boolean syntheticFormula(int width, boolean conjunction, String input) {
        boolean result = conjunction;
        for (int i = 0; i < width; i++) {
            String token = "m3:" + String.format(java.util.Locale.ROOT, "%03d", i) + "|";
            boolean found = input.contains(token);
            result = conjunction ? result && found : result || found;
        }
        return result;
    }

    private static void agree(M3ConstantRegexGate gate, String subject, boolean expected) {
        boolean actual = gate.holds(subject);
        CHECKS.incrementAndGet();
        if (actual != expected) {
            throw new AssertionError("wrong shared regex precompute necessary condition, input="
                    + subject + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void concurrentSharedStaticGate() throws Exception {
        final M3ConstantRegexGate compact = synthetic(64, true);
        final M3ConstantRegexGate wide = synthetic(96, false);
        List<String> corpus = new ArrayList<>();
        Random r = new Random(SEED);
        for (int i = 0; i < 64; i++) corpus.add(sample(96, r));
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < 8; worker++) {
                final int id = worker;
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int round = 0; round < 2_000; round++) {
                        String source = corpus.get((round * 31 + id * 17) & 63);
                        boolean compactExpected = syntheticFormula(64, true, source);
                        boolean wideExpected = syntheticFormula(96, false, source);
                        agree(compact, source, compactExpected);
                        agree(wide, source, wideExpected);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) future.get();
        }
    }

    private static void realImageAndJdkPatternParity() throws Exception {
        List<String> patterns = List.of("foo\\d+bar", "cat|dog", "hello");
        List<M3ConstantRegexImageWriter.Item> items = new ArrayList<>();
        for (String p : patterns) {
            items.add(new M3ConstantRegexImageWriter.Item(p, 0,
                    M3RegexRequirement.analyze(p, 0)));
        }
        M3ConstantRegexImage image = M3ConstantRegexImage.verify(ByteBuffer.wrap(
                M3ConstantRegexImageWriter.write(items,
                        M3ConstantRegexImageWriter.sha256Ints("concurrency-proof"))));
        List<String> samples = List.of("", "foo2bar", "foo999bar", "foo-only",
                "cat", "hot dog", "hello", "say hello", "a\nb", "dogcat");
        for (String p : patterns) {
            M3ConstantRegexGate gate = M3ConstantRegexGate.compile(p, 0, image);
            Pattern oracle = Pattern.compile(p);
            for (String s : samples) {
                compare(oracle.matcher(s).find(), gate.find(s), p + " find");
                compare(oracle.matcher(s).matches(), gate.matches(s), p + " matches");
                if (!oracle.matcher(s).replaceAll("#").equals(gate.replaceAll(s, "#"))) {
                    throw new AssertionError(p + " replaceAll");
                }
                CHECKS.incrementAndGet();
                if (!java.util.Arrays.equals(oracle.split(s), gate.split(s))) {
                    throw new AssertionError(p + " split");
                }
                CHECKS.incrementAndGet();
            }
        }
    }

    private static void compare(boolean expected, boolean actual, String label) {
        CHECKS.incrementAndGet();
        if (expected != actual) throw new AssertionError(label);
    }
}
