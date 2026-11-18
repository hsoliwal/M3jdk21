/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary String.splitInclusive retains literal delimiters lazily, preserving M3 composite ranges
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED StringRustSplitInclusiveTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class StringRustSplitInclusiveTest {
    private static void eq(List<String> expected, List<String> actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }

    private static List<String> oracle(String source, String delimiter) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < source.length()) {
            int found = -1;
            for (int at = start; at <= source.length() - delimiter.length(); at++) {
                int offset = 0;
                while (offset < delimiter.length()
                        && source.charAt(at + offset) == delimiter.charAt(offset)) {
                    offset++;
                }
                if (offset == delimiter.length()) {
                    found = at;
                    break;
                }
            }
            int end = found < 0 ? source.length() : found + delimiter.length();
            chunks.add(source.substring(start, end));
            start = end;
        }
        return chunks;
    }

    private static void direct() {
        eq(List.of(), "".splitInclusive(',').toList());
        eq(List.of("abc"), "abc".splitInclusive(',').toList());
        eq(List.of("a,", "b,"), "a,b,".splitInclusive(',').toList());
        eq(List.of(",", ","), ",,".splitInclusive(',').toList());
        eq(List.of("aa", "aa", "a"), "aaaaa".splitInclusive("aa").toList());
        eq(List.of("a::", "b::", "c"), "a::b::c".splitInclusive("::").toList());
        eq(List.of("a\r\n", "b\r\n", "c"),
                "a\r\nb\r\nc".splitInclusive("\r\n").toList());
        eq(List.of("\uD83D\uDE00", "x\uD83D\uDE00"),
                "\uD83D\uDE00x\uD83D\uDE00".splitInclusive("\uD83D\uDE00").toList());
        // Java char operates on UTF-16 code units, unlike Rust's scalar char.
        eq(List.of("\uD83D", "\uDE00x"), "\uD83D\uDE00x".splitInclusive('\uD83D').toList());
        String original = new String(new char[] {'a', 'b', 'c'});
        if (original.splitInclusive("unused").findFirst().orElseThrow() != original) {
            throw new AssertionError("unsplit String must retain receiver identity");
        }
        try {
            original.splitInclusive((String) null);
            throw new AssertionError("null delimiter accepted");
        } catch (NullPointerException expected) {
        }
        try {
            original.splitInclusive("");
            throw new AssertionError("empty delimiter accepted");
        } catch (IllegalArgumentException expected) {
        }
    }

    private static void differential() {
        char[] alphabet = {'a', 'b', 'c', '-', ':', '\n', '\r', '\u0000',
                '\u2028', '\uD83D', '\uDE00', '\u00A0'};
        Random random = new Random(0x51c0115eL);
        for (int trial = 0; trial < 8_000; trial++) {
            int size = random.nextInt(100);
            StringBuilder builder = new StringBuilder(size);
            for (int index = 0; index < size; index++) {
                builder.append(alphabet[random.nextInt(alphabet.length)]);
            }
            String source = builder.toString();
            int count = random.nextInt(4) + 1;
            StringBuilder key = new StringBuilder(count);
            for (int index = 0; index < count; index++) {
                key.append(alphabet[random.nextInt(alphabet.length)]);
            }
            String delimiter = key.toString();
            List<String> expected = oracle(source, delimiter);
            eq(expected, source.splitInclusive(delimiter).toList());
            eq(expected, source.splitInclusive(delimiter).parallel().toList());
            if (delimiter.length() == 1) {
                eq(expected, source.splitInclusive(delimiter.charAt(0)).toList());
            }
        }
    }

    private static void lazy() {
        String source = "a::".repeat(20_000) + "tail";
        AtomicInteger calls = new AtomicInteger();
        try (Stream<String> chunks = source.splitInclusive("::")) {
            eq(List.of("a::"), chunks.peek(chunk -> calls.incrementAndGet())
                    .limit(1).toList());
        }
        if (calls.get() != 1) throw new AssertionError("splitting was eager");
    }

    private static void canonicalRanges() throws ReflectiveOperationException {
        Class<?> storageClass = Class.forName("java.lang.M3String");
        Method admitArrays = storageClass.getDeclaredMethod("admitArrays", char[][].class);
        admitArrays.setAccessible(true);
        // Both two-unit delimiters span distinct canonical source atoms.
        char[][] atoms = {{'a', 'b', '-'}, {'-', 'c', 'd', '-'}, {'-', 'e', 'f'}};
        Object storage = admitArrays.invoke(null, (Object) atoms);
        if (storage == null) throw new AssertionError("canonical composition refused");
        Constructor<String> constructor = String.class.getDeclaredConstructor(storageClass);
        constructor.setAccessible(true);
        String composite = constructor.newInstance(storage);
        Method m3 = String.class.getDeclaredMethod("m3");
        m3.setAccessible(true);
        if (m3.invoke(composite) == null) throw new AssertionError("not an M3 source");
        List<String> chunks = composite.splitInclusive("--").toList();
        eq(List.of("ab--", "cd--", "ef"), chunks);
        for (String chunk : chunks) {
            if (m3.invoke(chunk) == null) {
                throw new AssertionError("M3 split chunk lost its canonical backing");
            }
        }
        // Searching one UTF-16 surrogate unit on M3 storage must agree with flat String.
        char[][] surrogateAtoms = {{'\uD83D'}, {'\uDE00', 'x'}};
        Object surrogateStorage = admitArrays.invoke(null, (Object) surrogateAtoms);
        if (surrogateStorage == null) throw new AssertionError("surrogate atoms refused");
        String surrogateSource = constructor.newInstance(surrogateStorage);
        eq(List.of("\uD83D", "\uDE00x"),
                surrogateSource.splitInclusive('\uD83D').toList());
    }

    public static void main(String[] args) throws Exception {
        direct();
        differential();
        lazy();
        canonicalRanges();
    }
}
