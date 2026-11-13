/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.SplittableRandom;

import jdk.m3.arrays.M3Arrays;
import jdk.m3.arrays.M3IntArrayView;
import jdk.m3.arrays.M3LongArrayView;
import jdk.m3.arrays.M3Utf16ArrayView;

/**
 * census2 P2-001 differential. EXISTING: one seeded trace over the pre-leaf M3Arrays API (chars, ints, longs, join,
 * slices, code points, search, compare, copies, read-only buffers), run on the master and the edited classes; the
 * traces must be identical. STRINGVIEW: stringView(String) (reflective, absent on master) against chars(String), the
 * snapshot oracle, over seeded strings with surrogates and nested slices; every observable must agree.
 * usage: java CensusArraysDifferential <ops> <seed>
 */
public final class CensusArraysDifferential {
    private static final StringBuilder TRACE = new StringBuilder();
    private static long checks;
    private static long failures;

    public static void main(String[] args) throws Exception {
        int ops = Integer.parseInt(args[0]);
        SplittableRandom random = new SplittableRandom(Long.decode(args[1]));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int i = 0; i < ops; i++) {
            TRACE.setLength(0);
            existing(random);
            digest.update(TRACE.toString().getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("EXISTING ops=" + ops + " trace_sha256=" + HexFormat.of().formatHex(digest.digest()));
        Method stringView;
        try {
            stringView = M3Arrays.class.getMethod("stringView", String.class);
        } catch (NoSuchMethodException absent) {
            System.out.println("STRINGVIEW absent");
            return;
        }
        SplittableRandom views = new SplittableRandom(Long.decode(args[1]) ^ 0x5157_5649_4557L);
        for (int i = 0; i < ops; i++) stringViewCase(stringView, text(views), views);
        System.out.println("STRINGVIEW cases=" + ops + " checks=" + checks + " failures=" + failures);
        if (failures != 0) System.exit(1);
    }

    private static String text(SplittableRandom random) {
        int length = random.nextInt(24);
        StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            switch (random.nextInt(5)) {
                case 0 -> text.append((char) (0xD800 + random.nextInt(0x400)));      // lone or paired high
                case 1 -> text.append((char) (0xDC00 + random.nextInt(0x400)));      // lone low
                case 2 -> text.appendCodePoint(0x10000 + random.nextInt(0x10000));   // supplementary pair
                default -> text.append((char) ('a' + random.nextInt(26)));
            }
        }
        return text.toString();
    }

    private static void existing(SplittableRandom random) {
        try {
            switch (random.nextInt(4)) {
                case 0 -> utf16(random);
                case 1 -> ints(random);
                case 2 -> longs(random);
                default -> joined(random);
            }
        } catch (RuntimeException failure) {
            TRACE.append("!").append(failure.getClass().getName());
        }
    }

    private static M3Utf16ArrayView anyUtf16(SplittableRandom random) {
        String text = text(random);
        M3Utf16ArrayView view = random.nextBoolean() ? M3Arrays.chars(text) : M3Arrays.chars(text.toCharArray());
        if (view.length() > 0 && random.nextBoolean()) {
            int from = random.nextInt(view.length() + 1);
            view = view.subSequence(from, from + random.nextInt(view.length() - from + 1));
        }
        return view;
    }

    private static void utf16(SplittableRandom random) {
        M3Utf16ArrayView view = anyUtf16(random);
        observe(view, random);
    }

    private static void observe(M3Utf16ArrayView view, SplittableRandom random) {
        int length = view.length();
        TRACE.append(length).append(':').append(view).append(':').append(view.segmentCount())
             .append(':').append(view.javaHashCode()).append(':').append(view.codePointCount());
        if (length > 0) {
            int at = random.nextInt(length);
            TRACE.append(':').append((int) view.charAt(at)).append(':').append(view.codePointAt(at))
                 .append(':').append(view.codePointBefore(at + 1));
        }
        M3Utf16ArrayView other = anyUtf16(random);
        TRACE.append(':').append(Integer.signum(view.compareTo(other))).append(':').append(view.contentEquals(other))
             .append(':').append(view.indexOf(other, 0)).append(':').append(view.lastIndexOf(other, length));
        StringBuilder buffers = new StringBuilder();
        for (CharBuffer buffer : view.asReadOnlyCharBuffers()) buffers.append(buffer).append('|');
        TRACE.append(':').append(buffers).append(':').append(new String(view.copy()));
    }

    private static int[] intArray(SplittableRandom random) {
        int[] values = new int[random.nextInt(12)];
        for (int i = 0; i < values.length; i++) values[i] = random.nextInt();
        return values;
    }

    private static void ints(SplittableRandom random) {
        M3IntArrayView view = M3Arrays.ints(intArray(random));
        if (view.length() > 0 && random.nextBoolean()) {
            int from = random.nextInt(view.length() + 1);
            view = view.slice(from, from + random.nextInt(view.length() - from + 1));
        }
        TRACE.append(view.length()).append(java.util.Arrays.toString(view.copy()));
        if (view.length() > 0) TRACE.append(view.intAt(random.nextInt(view.length())));
    }

    private static void longs(SplittableRandom random) {
        long[] values = new long[random.nextInt(12)];
        for (int i = 0; i < values.length; i++) values[i] = random.nextLong();
        M3LongArrayView view = M3Arrays.longs(values);
        TRACE.append(view.length()).append(java.util.Arrays.toString(view.copy()));
    }

    private static void joined(SplittableRandom random) {
        int parts = random.nextInt(4);
        M3Utf16ArrayView[] views = new M3Utf16ArrayView[parts];
        for (int i = 0; i < parts; i++) views[i] = anyUtf16(random);
        observe(M3Arrays.join(views), random);
        M3IntArrayView[] intViews = new M3IntArrayView[parts];
        for (int i = 0; i < parts; i++) intViews[i] = M3Arrays.ints(intArray(random));
        TRACE.append('/').append(java.util.Arrays.toString(M3Arrays.join(intViews).copy()));
    }

    private static void check(boolean pass) {
        checks++;
        if (!pass) failures++;
    }

    private static void stringViewCase(Method stringView, String text, SplittableRandom random) throws Exception {
        M3Utf16ArrayView view;
        try {
            view = (M3Utf16ArrayView) stringView.invoke(null, text);
        } catch (InvocationTargetException failure) {
            throw new AssertionError(failure.getCause());
        }
        M3Utf16ArrayView oracle = M3Arrays.chars(text);
        for (int depth = 0; depth < 3; depth++) {
            compare(view, oracle, random);
            if (view.length() == 0) break;
            int from = random.nextInt(view.length() + 1);
            int to = from + random.nextInt(view.length() - from + 1);
            view = view.subSequence(from, to);
            oracle = oracle.subSequence(from, to);
        }
        check(((M3Utf16ArrayView) stringView.invoke(null, text)).subSequence(0, text.length()).toString().equals(text));
    }

    private static void compare(M3Utf16ArrayView view, M3Utf16ArrayView oracle, SplittableRandom random) {
        int length = oracle.length();
        check(view.length() == length);
        check(view.toString().equals(oracle.toString()));
        check(view.javaHashCode() == oracle.javaHashCode());
        check(view.codePointCount() == oracle.codePointCount());
        check(view.contentEquals(oracle) && oracle.contentEquals(view) && view.compareTo(oracle) == 0);
        check(new String(view.copy()).equals(new String(oracle.copy())));
        check(view.segmentCount() == (length == 0 ? 0 : 1));
        StringBuilder buffers = new StringBuilder();
        for (CharBuffer buffer : view.asReadOnlyCharBuffers()) {
            check(buffer.isReadOnly() && buffer.position() == 0 && buffer.capacity() == buffer.remaining());
            buffers.append(buffer);
        }
        check(buffers.toString().equals(oracle.toString()));
        check(view.facts().equals(oracle.facts()));
        for (int i = 0; i < length; i++) check(view.charAt(i) == oracle.charAt(i));
        if (length > 0) {
            int start = random.nextInt(length);
            int count = random.nextInt(length - start + 1);
            char[] got = new char[count + 2];
            char[] want = new char[count + 2];
            view.copyTo(start, got, 1, count);
            oracle.copyTo(start, want, 1, count);
            check(java.util.Arrays.equals(got, want));
        }
        boolean refused;
        try {
            view.charAt(length);
            refused = false;
        } catch (IndexOutOfBoundsException expected) {
            refused = true;
        }
        check(refused);
    }
}
