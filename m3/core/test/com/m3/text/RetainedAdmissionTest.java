/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Exact-payload and stock-JVM Route A checks; not a modified-JDK acceptance test. */
public final class RetainedAdmissionTest {
    private static long checks;

    private RetainedAdmissionTest() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " at check " + checks);
    }

    private static void equal(Object actual, Object expected, String message) {
        check(Objects.equals(actual, expected), message + ": expected=" + expected + ", actual=" + actual);
    }

    private static Object field(Object object, String name) throws ReflectiveOperationException {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static Object staticField(String name) throws ReflectiveOperationException {
        Field field = M3Text.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("expected " + type.getName() + ", got " + error, error);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void admission() throws ReflectiveOperationException {
        final Object interner = staticField("INTERNER");
        final int[] reads = {0};
        CharSequence mutable = new CharSequence() {
            @Override public int length() { return 3; }
            @Override public char charAt(int index) {
                check(!Thread.holdsLock(interner), "caller code under interner monitor");
                return reads[0]++ < 3 ? "abc".charAt(index) : "xyz".charAt(index);
            }
            @Override public CharSequence subSequence(int a, int b) { throw new UnsupportedOperationException(); }
        };
        M3Text captured = M3Text.from(mutable);
        equal(reads[0], 3, "one caller traversal");
        equal(captured.asString(), "abc", "captured text");
        equal(captured.hashCode(), captured.asString().hashCode(), "snapshot hash");
        check(captured == M3Text.fromString("abc"), "stable canonical facade");
        StringBuilder builder = new StringBuilder("mutable\u0000\ud800");
        String oracle = builder.toString();
        M3Text value = M3Text.from(builder);
        builder.setLength(0);
        equal(value.asString(), oracle, "admission isolates subsequent mutations");
        char[] output = value.toCharArray();
        output[0] = 'X';
        equal(value.asString(), oracle, "writable output independent");
        expect(NullPointerException.class, () -> M3Text.from(null));
        expect(NullPointerException.class, () -> value.getBytes(null));
        CharSequence failed = new CharSequence() {
            @Override public int length() { return 2; }
            @Override public char charAt(int index) { throw new IllegalStateException("deliberate read failure"); }
            @Override public CharSequence subSequence(int a, int b) { throw new UnsupportedOperationException(); }
        };
        expect(IllegalStateException.class, () -> M3Text.from(failed));
        M3Text collidingA = M3Text.fromString("Aa");
        M3Text collidingB = M3Text.fromString("BB");
        equal(collidingA.hashCode(), collidingB.hashCode(), "real Java hash collision");
        check(!collidingA.equals(collidingB), "hash collision is not equality");
        check(!collidingA.equals("Aa") && !"Aa".equals(collidingA), "equals symmetry across String boundary");
    }

    private static void geometry() throws ReflectiveOperationException {
        LocalM3Arena arena = new LocalM3Arena();
        LocalM3StringPiece root = arena.copyUtf16("a\ud83d\ude00b\u0000c".toCharArray());
        M3StringPiece joined = M3StringPiece.join(root.subSequence(0, 2), root.subSequence(2, root.length()));
        equal(joined.flatten(), root.flatten(), "adjacent ranges content");
        equal(((Object[]) field(joined, "leaves")).length, 1, "adjacent same atom coalesced");
        Object leaf = ((Object[]) field(joined, "leaves"))[0];
        check(field(leaf, "backing") == field(root, "backing"), "coalescing keeps exact payload object");
        M3StringPiece repeated = M3StringPiece.join(root, root);
        equal(((Object[]) field(repeated, "leaves")).length, 2, "repeat is not adjacency");
        LocalM3StringPiece another = new LocalM3Arena().copyUtf16(root.flatten().toCharArray());
        M3StringPiece independent = M3StringPiece.join(root, another);
        equal(((Object[]) field(independent, "leaves")).length, 2, "different owners never coalesce");
        StorageIdentity sameNumericId = new StorageIdentity(new UUID(0, 7), 1, 1);
        LocalM3StringPiece fakeOne = new LocalM3StringPiece(new byte[]{1, 2}, LocalM3StringPiece.Encoding.LATIN1, sameNumericId, 0, 1);
        LocalM3StringPiece fakeTwo = new LocalM3StringPiece(new byte[]{3, 4}, LocalM3StringPiece.Encoding.LATIN1, sameNumericId, 1, 1);
        check(!fakeOne.adjacentTo(fakeTwo), "equal numeric handles cannot prove shared ownership");
        expect(IllegalArgumentException.class, () -> fakeOne.coalesce(fakeTwo));
        M3StringPiece[] arguments = {root, another};
        M3StringPiece snapshot = M3StringPiece.join(arguments);
        arguments[0] = null;
        equal(snapshot.flatten(), root.flatten().repeat(2), "caller directory is snapshotted");
        for (int a = 0; a <= repeated.length(); a++) {
            for (int b = a; b <= repeated.length(); b++) {
                M3StringPiece range = repeated.subSequence(a, b);
                equal(range.flatten(), repeated.flatten().substring(a, b), "UTF-16 ranges");
                M3PieceCursor cursor = new M3PieceCursor(range, 0, range.length());
                int i = 0;
                while (cursor.hasNext()) equal(cursor.next(), range.charAt(i++), "cursor units");
                expect(java.util.NoSuchElementException.class, cursor::next);
            }
        }
        expect(IndexOutOfBoundsException.class, () -> repeated.charAt(-1));
        expect(IndexOutOfBoundsException.class, () -> repeated.charAt(repeated.length()));
        expect(IndexOutOfBoundsException.class, () -> new M3PieceCursor(repeated, 3, 2));
        LocalM3StringPiece megabyte = arena.copyLatin1(new byte[1 << 20]);
        M3StringPiece[] many = new M3StringPiece[2047];
        Arrays.fill(many, megabyte);
        M3StringPiece huge = M3StringPiece.join(many);
        equal(huge.length(), 2047 * (1 << 20), "huge logical text reuses a single payload");
        equal(huge.charAt(huge.length() - 1), '\u0000', "last huge character");
        expect(ArithmeticException.class, () -> M3StringPiece.join(huge, huge));
        expect(OutOfMemoryError.class, () -> M3Text.fromString("x".repeat(1024)).repeat(Integer.MAX_VALUE));
        expect(IllegalArgumentException.class, () -> M3Text.fromString("x").repeat(-1));
        equal(M3Text.fromString("x").repeat(0), M3Text.empty(), "zero repeat");
    }

    private record HeldRange(M3StringPiece range, WeakReference<M3Text> facade,
                             WeakReference<LocalM3StringPiece> root, StorageIdentity identity,
                             WeakReference<byte[]> payload, String text) { }

    private static HeldRange retainedFixture() throws ReflectiveOperationException {
        String text = new String("root-lifetime-\ud83d\ude00-unique-001".toCharArray());
        M3Text value = M3Text.fromString(text);
        LocalM3StringPiece root = (LocalM3StringPiece) field(value, "piece");
        M3StringPiece range = root.subSequence(2, 3);
        return new HeldRange(range, new WeakReference<>(value), new WeakReference<>(root),
                root.storageIdentity(), new WeakReference<>((byte[]) field(root, "backing")), text);
    }

    private static void awaitCleared(Reference<?> reference) throws InterruptedException {
        for (int i = 0; i < 160 && !reference.refersTo(null); i++) {
            System.gc();
            Thread.sleep(5);
        }
        check(reference.refersTo(null), "weak referent was not collected; GC gate did not execute as required");
    }

    private static WeakReference<LocalM3StringPiece> lifetime() throws Exception {
        HeldRange held = retainedFixture();
        awaitCleared(held.facade());
        check(!held.root().refersTo(null), "live range retains canonical root object");
        M3Text readmitted = M3Text.fromString(held.text());
        LocalM3StringPiece readmittedPiece = (LocalM3StringPiece) field(readmitted, "piece");
        equal(readmittedPiece.storageIdentity(), held.identity(), "facade GC does not reassign live atom");
        check(field(readmittedPiece, "backing") == held.payload().get(), "facade GC does not duplicate payload");
        equal(held.range().flatten(), held.text().substring(2, 3), "retained tiny range remains valid");
        Reference.reachabilityFence(held.range());
        Reference.reachabilityFence(readmitted);
        return held.root();
    }

    private static void concurrentAdmission() throws Exception {
        LocalM3Arena arena = (LocalM3Arena) staticField("LOCAL_ARENA");
        AtomicLong sequence = (AtomicLong) field(arena, "sequence");
        String text = "concurrent-new-atom-" + "\ud800x\udc00".repeat(1024);
        long before = sequence.get();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<M3Text>> work = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 32; i++) {
                work.add(pool.submit(() -> {
                    start.await();
                    return M3Text.fromString(new String(text.toCharArray()));
                }));
            }
            start.countDown();
            M3Text first = work.get(0).get(15, TimeUnit.SECONDS);
            for (Future<M3Text> item : work) check(item.get(15, TimeUnit.SECONDS) == first, "concurrent canonical facade");
            equal(sequence.get() - before, 1L, "single admission allocation, not racing duplicate atoms");
            String joined = text + "suffix";
            M3Text shared = first.concat(M3Text.fromString("suffix"));
            List<Future<Boolean>> readers = new ArrayList<>();
            for (int thread = 0; thread < 8; thread++) {
                final int seed = thread;
                readers.add(pool.submit(() -> {
                    Random random = new Random(seed);
                    for (int j = 0; j < 10000; j++) {
                        int index = random.nextInt(joined.length());
                        if (shared.charAt(index) != joined.charAt(index)) return false;
                    }
                    return true;
                }));
            }
            for (Future<Boolean> item : readers) check(item.get(15, TimeUnit.SECONDS), "concurrent verified leaf hints");
        }
    }

    private static void unicodeDifferential() {
        char[] all = new char[65536];
        for (int i = 0; i < all.length; i++) all[i] = (char) i;
        M3Text universe = M3Text.fromString(new String(all));
        for (int i = 0; i < all.length; i++) equal(universe.charAt(i), all[i], "every UTF-16 unit");
        Random random = new Random(0x4d3341444d49544cL);
        Charset[] encodings = {StandardCharsets.UTF_8, StandardCharsets.UTF_16,
                StandardCharsets.UTF_16LE, StandardCharsets.UTF_16BE, StandardCharsets.US_ASCII,
                Charset.forName("ISO-2022-JP")};
        for (int trial = 0; trial < 12000; trial++) {
            int size = random.nextInt(96);
            char[] chars = new char[size];
            for (int i = 0; i < size; i++) {
                chars[i] = switch (random.nextInt(5)) {
                    case 0 -> (char) ('a' + random.nextInt(4));
                    case 1 -> (char) (0xd800 + random.nextInt(2048));
                    default -> (char) random.nextInt(65536);
                };
            }
            String flat = new String(chars);
            M3Text actual = M3Text.empty();
            int position = 0;
            while (position < size) {
                int next = Math.min(size, position + 1 + random.nextInt(9));
                actual = actual.concat(M3Text.fromString(flat.substring(position, next)));
                position = next;
            }
            equal(actual.asString(), flat, "segmented content");
            equal(actual.hashCode(), flat.hashCode(), "segmentation-independent hash");
            check(Arrays.equals(actual.toCharArray(), chars), "direct mutable output");
            check(Arrays.equals(actual.codePoints().toArray(), flat.codePoints().toArray()), "code points across seams");
            check(Arrays.equals(actual.chars().toArray(), flat.chars().toArray()), "char stream");
            equal(actual.compareTo(M3Text.fromString(flat)), 0, "alternate segmentation compare");
            int a = random.nextInt(size + 1), b = a + random.nextInt(size - a + 1);
            M3Text slice = actual.substring(a, b);
            equal(slice.asString(), flat.substring(a, b), "random slice");
            equal(slice.hashCode(), flat.substring(a, b).hashCode(), "random slice hash");
            int nested = random.nextInt(b - a + 1);
            equal(slice.substring(nested).asString(), flat.substring(a + nested, b), "nested slice");
            String needle = random.nextBoolean() ? flat.substring(a, b) : "not-present\u0000";
            equal(actual.indexOf(needle), flat.indexOf(needle), "forward search");
            equal(actual.lastIndexOf(needle), flat.lastIndexOf(needle), "reverse search");
            equal(actual.startsWith(needle), flat.startsWith(needle), "prefix");
            equal(actual.endsWith(needle), flat.endsWith(needle), "suffix");
            check(actual.contentEquals(new StringBuilder(flat)), "content equality");
            char[] destination = new char[size + 6];
            actual.getChars(0, size, destination, 3);
            check(Arrays.equals(Arrays.copyOfRange(destination, 3, size + 3), chars), "destination copy");
            if (trial % 40 == 0) {
                for (Charset encoding : encodings) {
                    check(Arrays.equals(actual.getBytes(encoding), flat.getBytes(encoding)), "whole-input encoding " + encoding);
                }
                equal(actual.repeat(3).asString(), flat.repeat(3), "repeat");
            }
        }
    }

    private static void regex() {
        String text = "A\ud83d\ude00B aa\n\ud800x\u0000 aa";
        M3Text view = M3Text.empty();
        for (int i = 0; i < text.length(); i++) view = view.concat(M3Text.fromString(text.substring(i, i + 1)));
        String[] expressions = {"(.)\\1", "(?=\\x{1F600})", "(?<=A)\\x{1F600}(?=B)",
                "(?m)^.*$", "(?s)A.*aa", "(?U)\\b\\w+\\b", "(a)(a)", "\\x00", "\\ud800", "."};
        for (String expression : expressions) {
            Pattern pattern = Pattern.compile(expression);
            for (boolean transparent : new boolean[]{false, true}) {
                for (boolean anchoring : new boolean[]{false, true}) {
                    Matcher a = pattern.matcher(view).region(1, text.length() - 1)
                            .useTransparentBounds(transparent).useAnchoringBounds(anchoring);
                    Matcher b = pattern.matcher(text).region(1, text.length() - 1)
                            .useTransparentBounds(transparent).useAnchoringBounds(anchoring);
                    while (true) {
                        boolean found = a.find();
                        equal(found, b.find(), "regex find " + expression);
                        if (!found) break;
                        equal(a.start(), b.start(), "regex start");
                        equal(a.end(), b.end(), "regex end");
                        for (int group = 0; group <= a.groupCount(); group++) equal(a.group(group), b.group(group), "regex group");
                    }
                    equal(pattern.matcher(view).replaceAll("_"), pattern.matcher(text).replaceAll("_"), "regex replacement");
                    check(Arrays.equals(pattern.split(view, -1), pattern.split(text, -1)), "regex split");
                }
            }
        }
        Object nullText = new Object() { @Override public String toString() { return null; } };
        equal(M3TextCompilerRuntime.concatObjectsToString("a", nullText, "b"), "a" + nullText + "b", "null-returning toString");
    }

    public static void main(String[] args) throws Exception {
        admission();
        geometry();
        WeakReference<LocalM3StringPiece> root = lifetime();
        awaitCleared(root);
        concurrentAdmission();
        unicodeDifferential();
        regex();
        System.out.println("M3_RETAINED_ADMISSION_PASS checks=" + checks + " seededCases=12000 allUtf16Units=65536 concurrentReads=80000");
    }
}
