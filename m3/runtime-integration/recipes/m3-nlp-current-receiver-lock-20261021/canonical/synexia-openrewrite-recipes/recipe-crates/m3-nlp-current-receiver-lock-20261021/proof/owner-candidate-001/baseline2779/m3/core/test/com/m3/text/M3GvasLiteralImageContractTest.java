/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import com.m3.compiletime.M3GvasLiteralImageWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/** Deterministic contract oracle run in independent JVM processes through JNI. */
public final class M3GvasLiteralImageContractTest {
    private static long checks;
    private static final List<String> STRINGS = fixtures();

    private static List<String> fixtures() {
        TreeSet<String> unique = new TreeSet<>(List.of("", "a", "ab", "hello", "HELLO", "a\u0000b",
                "z", "caf\u00e9", "\u00e9", "\u4f60\u597d", "\ud83d\ude80", "\ud800", "\udc00", "e\u0301"));
        unique.addAll(List.of("Aa", "BB", "AaAa", "BBBB", "AaBB", "BBAa"));
        Random random = new Random(0x3a5e20261016L);
        char[] alphabet = {'a', 'Z', '0', 0, '\u00e9', '\u4f60', '\ud800', '\udc00',
                '\ud83d', '\ude80', '\u200d'};
        for (int attempt = 0; attempt < 2048; attempt++) {
            int length = random.nextInt(34);
            StringBuilder text = new StringBuilder(length);
            for (int j = 0; j < length; j++) text.append(alphabet[random.nextInt(alphabet.length)]);
            unique.add(text.toString());
        }
        return List.copyOf(unique);
    }

    private M3GvasLiteralImageContractTest() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }

    private static void expectFailure(Class<? extends Throwable> expected, Action op) throws Exception {
        try {
            op.run();
            throw new AssertionError("no refusal: " + expected.getName());
        } catch (Throwable thrown) {
            check(expected.isInstance(thrown), "wrong refusal: " + thrown);
        }
    }

    private interface Action { void run() throws Exception; }

    private static void deterministicWriter() throws Exception {
        var first = M3GvasLiteralImageWriter.build(STRINGS);
        var shuffled = new ArrayList<>(STRINGS);
        Collections.shuffle(shuffled, new Random(20261016));
        shuffled.addAll(STRINGS);
        var second = M3GvasLiteralImageWriter.build(shuffled);
        check(java.util.Arrays.equals(first.bytes(), second.bytes()), "determinism across order/duplicates");
        check(first.rows().size() == STRINGS.size(), "one canonical row per spelling");
        expectFailure(IllegalArgumentException.class,
                () -> M3GvasLiteralImageWriter.build(STRINGS, 65));
        expectFailure(NullPointerException.class,
                () -> M3GvasLiteralImageWriter.build(java.util.Arrays.asList("ok", null)));
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("emit")) {
            deterministicWriter();
            var result = M3GvasLiteralImageWriter.build(STRINGS);
            M3GvasLiteralImageWriter.writeNew(Path.of(args[1]), result);
            System.out.println("M3_GVAS_PRECOMPUTE_EMITTED sha256=" + result.sha256() + " bytes="
                    + result.bytes().length + " checks=" + checks);
            return;
        }
        if (args.length != 4) throw new IllegalArgumentException("mode library image sha256");
        String mode = args[0];
        Path library = Path.of(args[1]);
        Path path = Path.of(args[2]);
        String digest = args[3];
        long base = M3FixedAddressMapping.DEFAULT_BASE;
        if (mode.equals("collision")) {
            M3FixedAddressMapping.reserveCollisionForProof(base, Files.size(path), library);
            expectFailure(IOException.class,
                    () -> M3FixedAddressMapping.open(path, base, digest, library));
            System.out.println("M3_GVAS_LITERAL_COLLISION_REFUSED checks=" + checks);
            return;
        }
        if (mode.equals("digest")) {
            expectFailure(IOException.class,
                    () -> M3FixedAddressMapping.open(path, base, "0".repeat(64), library));
            System.out.println("M3_GVAS_LITERAL_DIGEST_REFUSED checks=" + checks);
            return;
        }
        M3FixedAddressMapping.Image mapping = M3FixedAddressMapping.open(path, base, digest, library);
        if (mode.equals("corrupt")) {
            expectFailure(IllegalArgumentException.class, () -> M3GvasLiteralImage.open(mapping));
            System.out.println("M3_GVAS_LITERAL_CORRUPTION_REFUSED checks=" + checks);
            return;
        }
        M3GvasLiteralImage image = M3GvasLiteralImage.open(mapping);
        check(image.size() == STRINGS.size(), "unique literal count");
        check(image.hashSlotIndexPrepared(), "primitive slots admitted for bounded fixture");
        long first = 0;
        for (String literal : STRINGS) {
            int row = image.findRow(literal);
            check(row >= 0, "lookup exact UTF-16: " + literal);
            check(image.utf16LengthAt(row) == literal.length(), "precomputed length");
            check(image.hashCodeAt(row) == literal.hashCode(), "precomputed String hash");
            check(image.codePointCountAt(row) == literal.codePointCount(0, literal.length()),
                    "precomputed code point count");
            int power = 1;
            for (int k = 0; k < literal.length(); k++) power *= 31;
            check(image.hash31PowerAt(row) == power, "precomputed hash31Power");
            int flags = 0;
            if (literal.chars().allMatch(c -> c <= 0x7f)) flags |= M3GvasLiteralImage.FLAG_ASCII;
            if (literal.chars().allMatch(c -> c <= 0xff)) flags |= M3GvasLiteralImage.FLAG_LATIN1;
            if (literal.chars().anyMatch(c -> Character.isSurrogate((char) c))) flags |= M3GvasLiteralImage.FLAG_SURROGATE;
            check(image.flagsAt(row) == flags, "precomputed character class flags");
            check(image.addressOf(literal) == image.addressAt(row), "exact canonical address");
            check(image.addressOf(literal) >= base, "same fixed mapped base");
            if (literal.equals("hello")) first = image.addressOf(literal);
        }
        check(image.addressOf("not present") == 0L, "miss not synthesized");
        check(image.findRow("totally missing") == -1, "not found row");
        for (int miss = 0; miss < 2048; miss++) {
            check(image.findRow("M3_HASH_MISS_" + miss) == -1, "exact hash-slot miss");
        }
        expectFailure(IndexOutOfBoundsException.class, () -> image.addressAt(image.size()));
        expectFailure(NullPointerException.class, () -> image.findRow(null));
        check(image.addressOf("\ud800\udc00") == 0L, "surrogate semantics exact");
        ByteBuffer buffer = mapping.readOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        int relative = Math.toIntExact(first - base);
        check(buffer.getChar(relative) == 'h' && buffer.getChar(relative + 2) == 'e',
                "real mapped bytes at computed address");
        check(mapping.address() == base, "strict base");
        check(buffer.isReadOnly() && buffer.isDirect(), "read-only native image");
        if (mode.equals("child")) {
            System.out.printf("M3_GVAS_LITERAL_READY base=0x%x hello=0x%x checks=%d%n", base, first, checks);
            System.out.flush();
            if (System.in.read() == -1) throw new IOException("parent coordination failed");
            check(image.addressOf("hello") == first, "mapping held across handshake");
        }
        System.out.println("M3_GVAS_LITERAL_PASS checks=" + checks);
    }
}
