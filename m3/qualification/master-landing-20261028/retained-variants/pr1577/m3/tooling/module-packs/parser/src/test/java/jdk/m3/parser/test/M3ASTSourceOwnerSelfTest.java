// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package jdk.m3.parser.test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3File;

/** Qualifies the existing String source reference and, when required, its physical M3 range. */
public final class M3ASTSourceOwnerSelfTest {
    private static final boolean REQUIRE_M3 = Boolean.getBoolean("m3.owner.probe");
    private static final Field STRING_M3;
    private static final Field OWNER;
    private static final Field COORDINATE;
    private static int checks;
    private static int ranges;
    private static int ownerRanges;

    static {
        try {
            if (REQUIRE_M3) {
                STRING_M3 = field(String.class, "m3");
                Class<?> storage = Class.forName("java.lang.M3String");
                OWNER = field(storage, "owner");
                COORDINATE = field(storage, "value");
            } else {
                STRING_M3 = null;
                OWNER = null;
                COORDINATE = null;
            }
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        char[] latin = "!!alpha_beta:42\\path??".toCharArray();
        String latinSource = new String(latin);
        char[] latinExpected = latin.clone();
        Arrays.fill(latin, 'x');
        verify(latinSource, latinExpected);
        String wideSource = new String("##\u0100A\uD83D\uDE00\uD800X\uDC00\uFFFF_suffix$$".toCharArray());
        verify(wideSource, wideSource.toCharArray());
        String left = new String("left_source_prefix:".toCharArray());
        String right = new String("right_source_suffix".toCharArray());
        String composite = left.concat(right);
        verify(composite, composite.toCharArray());
        String existingSlice = composite.substring(2, composite.length() - 3);
        if (REQUIRE_M3) {
            Object compositeStorage = STRING_M3.get(composite);
            Object sliceStorage = STRING_M3.get(existingSlice);
            check(compositeStorage != null && sliceStorage != null, "existing slice is admitted");
            check(OWNER.get(compositeStorage) == OWNER.get(sliceStorage),
                    "existing source slice shares composite owner");
            long baseCoordinate = COORDINATE.getLong(compositeStorage);
            long sliceCoordinate = (((baseCoordinate >>> 32) + 2) << 32)
                    | Integer.toUnsignedLong(existingSlice.length());
            check(COORDINATE.getLong(sliceStorage) == sliceCoordinate,
                    "existing source slice retains nonzero offset");
        }
        verify(existingSlice, existingSlice.toCharArray());
        verify(new String(new char[0]), new char[0]);
        snapshotOnce();
        printClassHash(String.class);
        if (REQUIRE_M3) {
            for (String name : new String[] { "M3String", "M3StringOwner",
                    "M3StringPrecomputeBridge", "M3StringPool" }) {
                printClassHash(Class.forName("java.lang." + name));
            }
        }
        System.out.println("M3_AST_SOURCE_OWNER_PASS require_m3=" + REQUIRE_M3
                + " checks=" + checks + " ranges=" + ranges + " owner_ranges=" + ownerRanges
                + " runtime=" + System.getProperty("java.runtime.version")
                + " vm_info=" + System.getProperty("java.vm.info"));
    }

    private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static void verify(String source, char[] expected) throws Exception {
        sameUnits(source, expected, 0, expected.length);
        Object storage = null;
        if (REQUIRE_M3 && !source.isEmpty()) {
            storage = STRING_M3.get(source);
            check(storage != null, "source must already be M3 admitted");
        }
        M3File file = M3File.of(URI.create("memory:/m3-source-owner"), source);
        check(file.text() == source, "retain original String reference");
        check(file.toString() == source, "String projection retains reference");
        check(file.utf16Sha256().equals(hashUtf16(expected)), "exact UTF-16 digest");
        int length = source.length();
        int spanCount = (length + 1) * (length + 2) / 2;
        M3ASTIndex.Builder builder = M3ASTIndex.builder(
                file, M3ASTIndex.Domain.ECLIPSE_JDT, spanCount + 3);
        int root = builder.add(-1, "root", 0, length, M3ASTIndex.EXACT_RANGE);
        String[] representatives = new String[37];
        for (int from = 0; from <= length; from++) {
            for (int to = from; to <= length; to++) {
                int kind = (from * 31 + to) % representatives.length;
                String spelling = new String(("span" + kind).toCharArray());
                if (representatives[kind] == null) representatives[kind] = spelling;
                builder.add(root, spelling, from, to, M3ASTIndex.EXACT_RANGE);
            }
        }
        int unknown = builder.add(root, "unknown", -1, -1, M3ASTIndex.SYNTAX_ONLY);
        int approximate = builder.add(root, "approximate", 0, length, 0);
        M3ASTIndex index = builder.seal();
        check(index.source() == file, "index retains existing M3File");
        check(index.source().text() == source, "index reaches original String");
        check(index.sourceText(root) == source, "full range retains String identity");
        check(index.childCount(root) == spanCount + 2, "occurrence child count");
        int row = 1;
        for (int from = 0; from <= length; from++) {
            for (int to = from; to <= length; to++, row++) {
                int kind = (from * 31 + to) % representatives.length;
                check(index.kind(row) == representatives[kind], "first kind String reference");
                check(index.parent(row) == root, "occurrence parent");
                check(index.start(row) == from && index.end(row) == to, "UTF-16 coordinates");
                String result = index.sourceText(row);
                sameUnits(result, expected, from, to);
                if (from == 0 && to == length) {
                    check(result == source, "whole source result identity");
                } else if (from == to) {
                    check(result == "", "empty subrange result identity");
                }
                if (REQUIRE_M3 && from != to) {
                    check(STRING_M3.get(source) == storage, "source handle retained");
                    Object projected = STRING_M3.get(result);
                    check(projected != null, "projected String carries M3 storage");
                    check(OWNER.get(projected) == OWNER.get(storage), "same physical M3 owner");
                    long originalCoordinate = COORDINATE.getLong(storage);
                    long expectedCoordinate = ((originalCoordinate >>> 32) + from) << 32;
                    expectedCoordinate |= Integer.toUnsignedLong(to - from);
                    check(COORDINATE.getLong(projected) == expectedCoordinate,
                            "packed source-relative range");
                    ownerRanges++;
                }
                ranges++;
            }
        }
        refuses(IllegalStateException.class, () -> index.sourceText(unknown),
                "unverified/unknown source range");
        refuses(IllegalStateException.class, () -> index.sourceText(approximate),
                "unverified/unknown source range");
        refuses(IndexOutOfBoundsException.class, () -> index.sourceText(-1), null);
        refuses(IndexOutOfBoundsException.class, () -> index.sourceText(index.nodeCount()), null);
        refuses(IndexOutOfBoundsException.class, () -> file.subSequence(-1, 0), null);
        refuses(IndexOutOfBoundsException.class, () -> file.subSequence(0, length + 1), null);
        if (REQUIRE_M3 && !source.isEmpty()) {
            check(STRING_M3.get(source) == storage, "navigation preserves source handle");
        }
    }

    private static void snapshotOnce() {
        final class Source implements CharSequence {
            private final String text = "snapshot_source";
            private int calls;
            @Override public int length() { return text.length(); }
            @Override public char charAt(int index) { return text.charAt(index); }
            @Override public CharSequence subSequence(int start, int end) {
                return text.subSequence(start, end);
            }
            @Override public String toString() { calls++; return text; }
        }
        Source original = new Source();
        M3File file = M3File.of(URI.create("memory:/once"), original);
        check(original.calls == 1, "CharSequence snapshots once");
        check(file.text() == original.text, "snapshot String reference retained");
        StringBuilder mutable = new StringBuilder("mutable_source");
        M3File snapshot = M3File.of(URI.create("memory:/mutable"), mutable);
        mutable.setCharAt(0, 'X');
        check(snapshot.text().equals("mutable_source"), "snapshot independent of later mutation");
    }

    private static String hashUtf16(char[] value) throws Exception {
        byte[] bytes = new byte[value.length * 2];
        for (int i = 0; i < value.length; i++) {
            bytes[i * 2] = (byte) (value[i] >>> 8);
            bytes[i * 2 + 1] = (byte) value[i];
        }
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static void sameUnits(String actual, char[] expected, int from, int to) {
        check(actual.length() == to - from, "UTF-16 length");
        for (int i = from; i < to; i++) {
            check(actual.charAt(i - from) == expected[i], "UTF-16 code unit");
        }
    }

    private static void printClassHash(Class<?> type) throws Exception {
        try (InputStream input = type.getResourceAsStream(type.getSimpleName() + ".class")) {
            if (input == null) throw new AssertionError("class resource unavailable: " + type);
            System.out.println("RUNTIME_CLASS " + type.getName() + " sha256="
                    + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(input.readAllBytes())));
        }
    }

    private static void refuses(Class<? extends Throwable> type, Runnable action, String message) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (!type.isInstance(failure)) throw new AssertionError("wrong refusal", failure);
            if (message != null && !message.equals(failure.getMessage())) {
                throw new AssertionError("wrong refusal message", failure);
            }
            return;
        }
        throw new AssertionError("missing refusal: " + type.getName());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
