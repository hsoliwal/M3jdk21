/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.tools.nlp.tests;

import com.m3.text.LocalM3Arena;
import com.m3.text.LocalM3StringPiece;
import com.m3.text.M3StringPiece;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import opennlp.tools.tokenize.M3Tokenizers;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;

/**
 * Executes the real OpenNLP APIs and the actual M3 immutable text implementation.
 * The independent oracle loads the unmodified, source-built OpenNLP 2.4.0 JAR in
 * an isolated loader. It cannot call the scanner being qualified. Arguments are
 * that original JAR followed by its genuine SLF4J API and provider JARs.
 *
 * <p>This is tokenizer/UTF-16/ownership evidence, not model accuracy or a benchmark.
 * Helpful NPE messages are deliberately excluded from the outcome: their presence
 * changes with HotSpot fast-throw state. Exception types and complete Span values
 * remain compared, including OpenNLP's existing newline behavior.</p>
 */
public final class OpenNlpCompositeIntegrationChecks {
    private static long observations;

    private OpenNlpCompositeIntegrationChecks() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("original OpenNLP, SLF4J API and provider JARs required");
        }
        try (OriginalOracle original = new OriginalOracle(args)) {
            try {
                for (boolean newlines : new boolean[] {false, true}) {
                    original.newlines(newlines);
                    SimpleTokenizer.INSTANCE.setKeepNewLines(newlines);
                    allUtf16Units(original);
                    examplesAndSeams(original);
                    randomizedComposites(original);
                    legacyStringSnapshots(original);
                    actualM3Ownership(original);
                }
                SimpleTokenizer.INSTANCE.setKeepNewLines(false);
                original.newlines(false);
                cancellationAndInterruption();
                concurrentImmutableReaders(original);
                same(original.spans(null), outcome(() -> SimpleTokenizer.INSTANCE.tokenizePos(null)),
                        "legacy null exception type");
                expect(NullPointerException.class, () -> M3Tokenizers.simpleSpans(null));
                System.out.println("M3_OPENNLP_COMPOSITE_PASS observations=" + observations
                        + " utf16Units=65536 newlineModes=2 randomComposites=10000"
                        + " independentOriginal=true actualM3Arena=true");
            } finally {
                SimpleTokenizer.INSTANCE.setKeepNewLines(false);
                original.newlines(false);
            }
        }
    }

    private static void allUtf16Units(OriginalOracle original) throws Exception {
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            String text = "A" + (char) unit + "9";
            Outcome expected = original.spans(text);
            same(expected, outcome(() -> SimpleTokenizer.INSTANCE.tokenizePos(text)),
                    "original String code unit " + unit);
            HostileSequence split = new HostileSequence(text.substring(0, 1), text.substring(1));
            same(expected, outcome(() -> M3Tokenizers.simpleSpans(split)),
                    "borrowed code unit " + unit);
            require(split.reads > 0, "nonempty borrowed text was read");
        }
    }

    private static void examplesAndSeams(OriginalOracle original) throws Exception {
        String[] samples = {"", " ", "a", "Cats running...", "+++--==", "१२३abc９９",
                "A\uD83D\uDE00B", "\uD800\uDC00\uDC00", "\uD800", "\uDC00", "é\u0301",
                "\u0000\t\n\r\f\u0085\u00a0\u2028\u2029", "\n", "\f", "a\u001cb",
                "x\r\ny", "abc \uD801\uDC00 z", "same same", "𐐀界\u200d字"};
        for (String text : samples) {
            Outcome expected = original.spans(text);
            same(expected, outcome(() -> SimpleTokenizer.INSTANCE.tokenizePos(text)), "example String");
            for (int split = 0; split <= text.length(); split++) {
                HostileSequence composite = new HostileSequence(text.substring(0, split),
                        text.substring(split));
                same(expected, outcome(() -> M3Tokenizers.simpleSpans(composite)), "every UTF-16 seam");
                same(expected, outcome(() -> M3Tokenizers.simpleSpans(composite, () -> { })),
                        "checkpoint every seam");
            }
        }
    }

    private static void randomizedComposites(OriginalOracle original) throws Exception {
        Random random = new Random(0x4d334e4c50L);
        for (int trial = 0; trial < 5000; trial++) {
            char[] units = new char[random.nextInt(257)];
            for (int i = 0; i < units.length; i++) {
                units[i] = (char) random.nextInt(65536);
            }
            String text = new String(units);
            int a = random.nextInt(units.length + 1);
            int b = a + random.nextInt(units.length - a + 1);
            HostileSequence composite = new HostileSequence(text.substring(0, a),
                    new HostileSequence(text.substring(a, b), text.substring(b)));
            same(original.spans(text), outcome(() -> M3Tokenizers.simpleSpans(composite, () -> { })),
                    "random composite");
        }
    }

    private static void legacyStringSnapshots(OriginalOracle original) throws Exception {
        char[] mutable = "First second ٣...".toCharArray();
        String snapshot = new String(mutable);
        Arrays.fill(mutable, 'x');
        StringBuilder builder = new StringBuilder(snapshot);
        String builderSnapshot = builder.toString();
        builder.setLength(0);
        builder.append("changed");
        same(original.spans(snapshot), outcome(() -> SimpleTokenizer.INSTANCE.tokenizePos(builderSnapshot)),
                "String input retains its independent immutable snapshot");
        require(Arrays.equals(original.tokens(snapshot), SimpleTokenizer.INSTANCE.tokenize(builderSnapshot)),
                "legacy String[] values retain original behavior");
    }

    private static void actualM3Ownership(OriginalOracle original) throws Exception {
        LocalM3Arena arena = new LocalM3Arena();
        String[] samples = {"", "alpha beta", "A\uD83D\uDE00Z", "\uD800x\uDC00",
                "१२३é\u0301...", "x\r\ny", "\u0000\u00a0\u2028"};
        for (String text : samples) {
            char[] mutable = text.toCharArray();
            LocalM3StringPiece owner = arena.copyUtf16(mutable);
            Arrays.fill(mutable, 'z');
            require(owner.length() == text.length(), "M3 owner length");
            for (int i = 0; i < text.length(); i++) {
                require(owner.charAt(i) == text.charAt(i), "arena copied mutable admission input");
            }
            Outcome expected = original.spans(text);
            for (int split = 0; split <= text.length(); split++) {
                LocalM3StringPiece left = owner.subSequence(0, split);
                LocalM3StringPiece right = owner.subSequence(split, owner.length());
                require(left.storageIdentity().equals(owner.storageIdentity()), "left backing identity retained");
                require(right.storageIdentity().equals(owner.storageIdentity()), "right backing identity retained");
                M3StringPiece[] leaves = {left, M3StringPiece.join(arena.copyLatin1(new byte[0]), right)};
                M3StringPiece composite = M3StringPiece.join(leaves);
                leaves[0] = arena.copyLatin1(new byte[] {'x'});
                HostileSequence observed = new HostileSequence(composite, "");
                same(expected, outcome(() -> M3Tokenizers.simpleSpans(observed, () -> { })),
                        "actual M3 arena composite with copied caller directory");
                same(expected, outcome(() -> M3Tokenizers.simpleSpans(composite)),
                        "actual M3StringPiece direct API");
            }
        }
        M3StringPiece latin = arena.copyLatin1(new byte[] {'A', (byte) 0xe9, ' ', '9'});
        same(original.spans("Aé 9"), outcome(() -> M3Tokenizers.simpleSpans(latin)), "M3 Latin1 decoding");
    }

    private static void cancellationAndInterruption() {
        AtomicInteger callbacks = new AtomicInteger();
        HostileSequence large = new HostileSequence("a".repeat(4096), " b");
        CancellationException sentinel = new CancellationException("host progress cancellation");
        CancellationException actual = expect(CancellationException.class,
                () -> M3Tokenizers.simpleSpans(large, () -> {
                    if (callbacks.incrementAndGet() == 3) {
                        throw sentinel;
                    }
                }));
        require(actual == sentinel, "host cancellation object propagated unchanged");
        require(large.reads > 0 && large.reads <= 256, "cancellation stops in bounded checkpoint interval");
        require(callbacks.get() == 3, "entry and scanner checkpoints ran");
        AtomicInteger empty = new AtomicInteger();
        require(M3Tokenizers.simpleSpans("", empty::incrementAndGet).length == 0, "empty spans");
        require(empty.get() == 2, "empty input still checks before and after tokenization");
        boolean previous = Thread.interrupted();
        try {
            Thread.currentThread().interrupt();
            expect(CancellationException.class, () -> M3Tokenizers.simpleSpans("abc"));
            require(Thread.currentThread().isInterrupted(), "tokenizer preserves interruption status");
        } finally {
            Thread.interrupted();
            if (previous) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void concurrentImmutableReaders(OriginalOracle original) throws Exception {
        String text = "alpha\uD83D\uDE00 beta १२३... ".repeat(100);
        LocalM3Arena arena = new LocalM3Arena();
        M3StringPiece owner = arena.copyUtf16(text.toCharArray());
        M3StringPiece composite = M3StringPiece.join(owner.subSequence(0, 6),
                owner.subSequence(6, owner.length()));
        Outcome expected = original.spans(text);
        try (var executor = Executors.newFixedThreadPool(4)) {
            var jobs = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int worker = 0; worker < 4; worker++) {
                jobs.add(executor.submit(() -> {
                    for (int round = 0; round < 200; round++) {
                        if (!expected.equals(outcome(() -> M3Tokenizers.simpleSpans(composite, () -> { })))) {
                            return false;
                        }
                    }
                    return true;
                }));
            }
            for (var job : jobs) {
                require(job.get(), "concurrent readers agree with independent original");
            }
        }
        observations += 800;
    }

    private record SpanValue(int start, int end, String type, long probabilityBits) { }
    private record Outcome(List<SpanValue> spans, String exceptionType) { }

    @FunctionalInterface
    private interface SpanCall {
        Span[] get();
    }

    private static Outcome outcome(SpanCall call) {
        try {
            List<SpanValue> values = new ArrayList<>();
            for (Span span : call.get()) {
                values.add(new SpanValue(span.getStart(), span.getEnd(), span.getType(),
                        Double.doubleToLongBits(span.getProb())));
            }
            return new Outcome(values, null);
        } catch (RuntimeException failure) {
            return new Outcome(null, failure.getClass().getName());
        }
    }

    private static final class HostileSequence implements CharSequence {
        private final CharSequence left;
        private final CharSequence right;
        private int reads;

        HostileSequence(CharSequence left, CharSequence right) {
            this.left = left;
            this.right = right;
        }

        @Override public int length() {
            return left.length() + right.length();
        }

        @Override public char charAt(int index) {
            Objects.checkIndex(index, length());
            reads++;
            return index < left.length() ? left.charAt(index) : right.charAt(index - left.length());
        }

        @Override public CharSequence subSequence(int start, int end) {
            throw new AssertionError("tokenization must not call subSequence");
        }

        @Override public String toString() {
            throw new AssertionError("tokenization must not flatten immutable input");
        }
    }

    /** Loads genuine unmodified scanner, Span, StringUtil, and logging implementations. */
    private static final class OriginalOracle implements AutoCloseable {
        private final URLClassLoader loader;
        private final Object tokenizer;
        private final Method spans;
        private final Method tokens;
        private final Method newlines;
        private final Method start;
        private final Method end;
        private final Method type;
        private final Method probability;

        OriginalOracle(String[] jars) throws Exception {
            URL[] urls = new URL[jars.length];
            for (int i = 0; i < jars.length; i++) {
                urls[i] = Path.of(jars[i]).toUri().toURL();
            }
            loader = new URLClassLoader(urls, null);
            Class<?> tokenizerClass = loader.loadClass("opennlp.tools.tokenize.SimpleTokenizer");
            require(tokenizerClass != SimpleTokenizer.class, "original scanner is independently loaded");
            tokenizer = tokenizerClass.getField("INSTANCE").get(null);
            spans = tokenizerClass.getMethod("tokenizePos", String.class);
            tokens = tokenizerClass.getMethod("tokenize", String.class);
            tokens.setAccessible(true);
            newlines = tokenizerClass.getMethod("setKeepNewLines", boolean.class);
            newlines.setAccessible(true);
            Class<?> spanClass = loader.loadClass("opennlp.tools.util.Span");
            start = spanClass.getMethod("getStart");
            end = spanClass.getMethod("getEnd");
            type = spanClass.getMethod("getType");
            probability = spanClass.getMethod("getProb");
            try {
                tokenizerClass.getDeclaredMethod("tokenizePosSequence", CharSequence.class);
                throw new AssertionError("oracle must be the original unmodified scanner");
            } catch (NoSuchMethodException expected) {
                // A baseline JAR containing the rewritten scanner is explicitly refused.
            }
        }

        void newlines(boolean keep) throws Exception {
            newlines.invoke(tokenizer, keep);
        }

        Outcome spans(String text) throws Exception {
            try {
                Object[] result = (Object[]) spans.invoke(tokenizer, text);
                List<SpanValue> values = new ArrayList<>();
                for (Object span : result) {
                    values.add(new SpanValue((Integer) start.invoke(span), (Integer) end.invoke(span),
                            (String) type.invoke(span), Double.doubleToLongBits((Double) probability.invoke(span))));
                }
                return new Outcome(values, null);
            } catch (InvocationTargetException failure) {
                if (failure.getCause() instanceof RuntimeException cause) {
                    return new Outcome(null, cause.getClass().getName());
                }
                throw failure;
            }
        }

        String[] tokens(String text) throws Exception {
            return (String[]) tokens.invoke(tokenizer, text);
        }

        @Override public void close() throws IOException {
            loader.close();
        }
    }

    private static void same(Outcome expected, Outcome actual, String what) {
        require(expected.equals(actual), what + ": expected=" + expected + ", actual=" + actual);
    }

    private static void require(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError(what);
        }
        observations++;
    }

    private static <T extends Throwable> T expect(Class<T> type, Runnable call) {
        try {
            call.run();
        } catch (Throwable failure) {
            require(type.isInstance(failure), "expected " + type.getName() + ", got " + failure);
            return type.cast(failure);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
