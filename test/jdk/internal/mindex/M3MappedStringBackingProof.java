/*
 * Copyright (c) 2026, Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */

package jdk.internal.mindex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.CRC32;

/** Focused proof for the exact M3 mapped UTF-16/UTF-8 backing sources. */
public final class M3MappedStringBackingProof {
    private static final int TEXT_MAGIC = 0x4d49584d;
    private static final int TEXT_VERSION = 3;
    private static final int TEXT_HEADER = 64;
    private static final int TEXT_RECORD_MAGIC = 0x4d495852;
    private static final int TEXT_RECORD_HEADER = 120;
    private static final int BYTE_MAGIC = 0x4d49424d;
    private static final int BYTE_VERSION = 1;
    private static final int BYTE_HEADER = 64;
    private static final int BYTE_RECORD_MAGIC = 0x4d494252;
    private static final int BYTE_RECORD_HEADER = 32;

    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("check " + checks);
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable body) {
        try {
            body.run();
            throw new AssertionError("missing " + type.getName());
        } catch (Throwable failure) {
            check(type.isInstance(failure));
        }
    }

    private static int align8(int value) {
        return (value + 7) & ~7;
    }

    private static int javaHash(String value) {
        return value.hashCode();
    }

    private static int hash31Power(String value) {
        int power = 1;
        for (int index = 0; index < value.length(); index++) {
            power *= 31;
        }
        return power;
    }

    private static int unpairedSurrogates(String value) {
        int count = 0;
        boolean previousHigh = false;
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            if (Character.isLowSurrogate(unit) && previousHigh) {
                count--;
            } else if (Character.isSurrogate(unit)) {
                count++;
            }
            previousHigh = Character.isHighSurrogate(unit);
        }
        return count;
    }

    private static byte[] utf16Bytes(String value) {
        ByteBuffer bytes = ByteBuffer.allocate(value.length() * Character.BYTES)
                .order(ByteOrder.BIG_ENDIAN);
        for (int index = 0; index < value.length(); index++) {
            bytes.putChar(value.charAt(index));
        }
        return bytes.array();
    }

    private static int crc(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return (int) crc.getValue();
    }

    private static int commitCrc(long generation, long committed, int rows) {
        ByteBuffer bytes = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(generation).putLong(committed).putInt(rows);
        return crc(bytes.array());
    }

    private static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] textStore(String[] values, int[] languages, boolean aliasSecond)
            throws IOException {
        ByteArrayOutputStream records = new ByteArrayOutputStream();
        for (int row = 0; row < values.length; row++) {
            String value = values[row];
            byte[] payload = utf16Bytes(value);
            byte[] encoded = utf8(value);
            int recordBytes = aliasSecond && row == 1
                    ? TEXT_RECORD_HEADER
                    : align8(TEXT_RECORD_HEADER + payload.length);
            ByteBuffer record = ByteBuffer.allocate(recordBytes).order(ByteOrder.BIG_ENDIAN);
            record.putInt(TEXT_RECORD_MAGIC);
            record.putInt(recordBytes);
            record.putInt(languages[row]);
            record.putInt(value.length());
            record.putInt(encoded.length);
            record.putInt(value.codePointCount(0, value.length()));
            record.putInt(unpairedSurrogates(value));
            record.putInt(javaHash(value));
            record.putInt(hash31Power(value));
            record.putInt(value.isEmpty() ? -1 : value.charAt(0));
            record.putInt(value.isEmpty() ? -1 : value.charAt(value.length() - 1));
            record.putInt(aliasSecond && row == 1 ? 1 : 0);
            record.position(56);
            record.putInt(crc(payload));
            record.position(112);
            record.putLong(1L);
            if (!(aliasSecond && row == 1)) {
                record.position(TEXT_RECORD_HEADER);
                record.put(payload);
            }
            records.write(record.array());
        }
        byte[] body = records.toByteArray();
        ByteBuffer file = ByteBuffer.allocate(TEXT_HEADER + body.length).order(ByteOrder.BIG_ENDIAN);
        file.putInt(TEXT_MAGIC).putInt(TEXT_VERSION).putInt(TEXT_HEADER);
        file.position(16);
        file.putLong(1L).putLong(file.capacity()).putInt(values.length)
                .putInt(commitCrc(1L, file.capacity(), values.length));
        file.position(TEXT_HEADER);
        file.put(body);
        return file.array();
    }

    private static byte[] byteStore(String value) throws IOException {
        byte[] payload = utf8(value);
        int recordBytes = align8(BYTE_RECORD_HEADER + payload.length);
        ByteBuffer file = ByteBuffer.allocate(BYTE_HEADER + recordBytes).order(ByteOrder.BIG_ENDIAN);
        file.putInt(BYTE_MAGIC).putInt(BYTE_VERSION).putInt(BYTE_HEADER);
        file.position(16);
        file.putLong(1L).putLong(file.capacity()).putInt(1)
                .putInt(commitCrc(1L, file.capacity(), 1));
        file.position(BYTE_HEADER);
        file.putInt(BYTE_RECORD_MAGIC).putInt(recordBytes).putInt(payload.length)
                .putInt(javaHash(value)).putInt(crc(payload));
        file.position(BYTE_HEADER + BYTE_RECORD_HEADER);
        file.put(payload);
        return file.array();
    }

    private static Path writeStore(Path directory, String name, String[] values, int[] languages)
            throws IOException {
        Path text = directory.resolve(name);
        Files.write(text, textStore(values, languages, true));
        Files.write(Path.of(text.toString() + ".bytes"), byteStore(values[0]));
        return text;
    }

    private static void assertUtf8(ByteBuffer actual, byte[] expected) {
        check(actual.isReadOnly());
        byte[] bytes = new byte[actual.remaining()];
        actual.get(bytes);
        check(Arrays.equals(expected, bytes));
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("m3-utf8-backing-proof-");
        Files.createDirectories(directory);
        String scalar = "A\uD83D\uDE00\uD800";
        String[] values = {scalar, scalar};
        int[] languages = {7, 7};
        Path text = writeStore(directory, "valid.mixm", values, languages);

        try (M3MappedStringBacking backing = M3MappedStringBacking.open(text)) {
            long first = (7L << 32) | 1L;
            long alias = (7L << 32) | 2L;
            check(backing.size() == 2);
            check(backing.languageId(first) == 7 && backing.languageId(alias) == 7);
            check(backing.length(first) == scalar.length());
            check(backing.utf8Length(first) == utf8(scalar).length);
            check(backing.codePointCount(first) == scalar.codePointCount(0, scalar.length()));
            check(backing.unpairedSurrogateCount(first) == 1);
            check(backing.hashCode(first) == scalar.hashCode());
            check(backing.hash31Power(first) == hash31Power(scalar));
            check(backing.firstUtf16Unit(first) == scalar.charAt(0));
            check(backing.lastUtf16Unit(first) == scalar.charAt(scalar.length() - 1));
            check(backing.charAt(first, 1) == scalar.charAt(1));
            check(backing.materialize(first).equals(scalar));
            check(backing.materialize(alias).equals(scalar));
            check(backing.contentEquals(first, scalar));
            check(backing.compare(first, alias) == 0);
            check(backing.utf16View(first).isReadOnly());
            check(backing.utf16View(first).toString().equals(scalar));
            assertUtf8(backing.utf8View(first), utf8(scalar));
            M3MappedStringBacking.View view = backing.view(first, 1, scalar.length() - 1);
            check(view.length() == scalar.length() - 2);
            check(view.charAt(0) == scalar.charAt(1));
            check(view.toString().equals(scalar.substring(1, scalar.length() - 1)));
            check(view.subSequence(1, view.length()).codePointCount() == 1);
            expect(IndexOutOfBoundsException.class, () -> backing.charAt(first, scalar.length()));
            expect(IllegalArgumentException.class, () -> backing.length((8L << 32) | 1L));
        }

        M3MappedStringBacking closed = M3MappedStringBacking.open(text);
        closed.close();
        expect(IllegalStateException.class, () -> closed.length((7L << 32) | 1L));

        Path corrupt = writeStore(directory, "corrupt.mixm", values, languages);
        byte[] corruptBytes = Files.readAllBytes(corrupt);
        corruptBytes[TEXT_HEADER + TEXT_RECORD_HEADER] ^= 1;
        Files.write(corrupt, corruptBytes);
        expect(IllegalStateException.class, () -> M3MappedStringBacking.open(corrupt));

        System.out.println("M3_MAPPED_UTF8_BACKING_PROOF_PASS checks=" + checks
                + " rows=2 alias=1 utf8=verified corruption=reject");
    }
}

