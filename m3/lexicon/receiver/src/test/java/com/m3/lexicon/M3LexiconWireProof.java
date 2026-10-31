// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Deterministic executable Java/JNI oracle; compiled with -Xlint:all -Werror. */
public final class M3LexiconWireProof {
    private static long checks;
    private M3LexiconWireProof() { }

    public static void main(String[] arguments) {
        List<byte[]> atoms = List.of(bytes("Hello"), bytes(" "), bytes("world"), bytes("!"),
                bytes("\n"), bytes("class A { /* not a method */ }"),
                bytes("(?<name>[A-Z]+)"), new byte[] { 0, (byte) 0xff }, bytes("\uD83D\uDE00"));
        M3LexiconWire sender = new M3LexiconWire(atoms);
        M3LexiconWire receiver = new M3LexiconWire(atoms);
        check(Arrays.equals(sender.fingerprint(), receiver.fingerprint()), "lexicon hash");
        List<byte[]> reorder = new ArrayList<>(atoms);
        Collections.reverse(reorder);
        M3LexiconWire different = new M3LexiconWire(reorder);
        check(!Arrays.equals(different.fingerprint(), sender.fingerprint()), "different order hash");
        int[] words = { 1, 2, 3, 4 };
        byte[] frame = sender.encodeCwids(words);
        check(Arrays.equals(receiver.decode(frame), bytes("Hello world!")), "zero-content transfer");
        refused(() -> different.decode(frame), "foreign dictionary");
        refused(() -> sender.encodeCwids(new int[] { 0 }), "zero CWID");
        refused(() -> sender.encodeCwids(new int[] { 200 }), "oversized CWID");
        refused(() -> new M3LexiconWire(List.of(bytes("dup"), bytes("dup"))), "duplicate atom");
        refused(() -> new M3LexiconWire(List.of(new byte[0])), "empty atom");
        refused(() -> new M3LexiconWire(atoms, 0, 80).encodeCwids(words), "content capacity");
        byte[] mixed = sender.encode(new byte[] { 0, 1, 2, 0 },
                new int[] { 1, 3, 0, 4 }, new int[] { 0, 1, 0, 0 },
                new int[] { 0, 3, 2, 0 }, new byte[] { 0, -1 });
        check(Arrays.equals(receiver.decode(mixed), new byte[] {
                'H', 'e', 'l', 'l', 'o', 'o', 'r', 'l', 0, -1, '!' }), "mixed slices and literals");
        refused(() -> sender.encode(new byte[] { 0 }, new int[] { 1 }, new int[] { 1 },
                new int[] { 0 }, new byte[0]), "noncanonical row");
        refused(() -> sender.encode(new byte[] { 1 }, new int[] { 1 }, new int[] { 1 },
                new int[] { 100 }, new byte[0]), "slice bounds");
        refused(() -> sender.encode(new byte[] { 2 }, new int[] { 1 }, new int[] { 0 },
                new int[] { 1 }, new byte[] { 1 }), "literal id");
        refused(() -> sender.encode(new byte[] { 3 }, new int[] { 0 }, new int[] { 0 },
                new int[] { 1 }, new byte[] { 1 }), "unknown tag");
        check(Arrays.equals(sender.decode(sender.encodeLiteral(new byte[0])), new byte[0]), "empty");
        check(Arrays.equals(sender.decode(sender.encodeLiteral(new byte[] { -1, 0, 1 })),
                new byte[] { -1, 0, 1 }), "unpaired or binary literal");
        for (int len = 0; len < frame.length; len++) {
            byte[] shortened = Arrays.copyOf(frame, len);
            refused(() -> receiver.decode(shortened), "truncated frame");
        }
        for (int i = 0; i < frame.length; i++) {
            byte[] corrupt = frame.clone();
            corrupt[i] ^= 1;
            refused(() -> receiver.decode(corrupt), "corrupt frame byte " + i);
        }
        byte[] longer = Arrays.copyOf(frame, frame.length + 1);
        refused(() -> receiver.decode(longer), "trailing payload");
        Random random = new Random(0x4d33464d54323031L);
        for (int iteration = 0; iteration < 2048; iteration++) {
            int length = random.nextInt(80);
            int[] cwids = new int[length];
            byte[] expected = new byte[4096];
            int offset = 0;
            for (int i = 0; i < cwids.length; i++) {
                cwids[i] = random.nextInt(atoms.size()) + 1;
                byte[] atom = atoms.get(cwids[i] - 1);
                System.arraycopy(atom, 0, expected, offset, atom.length);
                offset += atom.length;
            }
            check(Arrays.equals(receiver.decode(sender.encodeCwids(cwids)),
                    Arrays.copyOf(expected, offset)), "id property " + iteration);
            byte[] literal = new byte[random.nextInt(128)];
            random.nextBytes(literal);
            check(Arrays.equals(receiver.decode(sender.encodeLiteral(literal)), literal),
                    "literal property " + iteration);
        }
        byte[] mutated = atoms.get(0);
        // The supplier's list and words are detached by the immutable lexicon constructor.
        check(!Arrays.equals(mutated, new byte[] { '!'}), "supplier visible only");
        byte[] exported = sender.arenaCopy();
        exported[0] ^= 1;
        check(Arrays.equals(receiver.decode(frame), bytes("Hello world!")), "export detached");

        if (arguments.length > 0) {
            M3LexiconWireNative.load(Path.of(arguments[0]));
            ByteBuffer arena = ByteBuffer.allocateDirect(sender.arenaCopy().length);
            arena.put(sender.arenaCopy());
            int[] offsets = sender.offsetsCopy();
            int[] sizes = sender.lengthsCopy();
            for (int iteration = 0; iteration < 1024; iteration++) {
                int[] cwids = new int[random.nextInt(96)];
                int length = 0;
                for (int i = 0; i < cwids.length; i++) {
                    cwids[i] = random.nextInt(atoms.size()) + 1;
                    length += sizes[cwids[i] - 1];
                }
                ByteBuffer javaOutput = ByteBuffer.allocateDirect(length + 4);
                ByteBuffer nativeOutput = ByteBuffer.allocateDirect(length + 4);
                int j = M3LexiconWireNative.expandJava(arena, offsets, sizes, cwids, javaOutput);
                int n = M3LexiconWireNative.expand(arena, offsets, sizes, cwids, nativeOutput);
                check(j == length && n == length, "JNI length " + iteration);
                for (int byteIndex = 0; byteIndex < length; byteIndex++) {
                    check(javaOutput.get(byteIndex) == nativeOutput.get(byteIndex), "JNI parity");
                }
            }
            ByteBuffer pristine = ByteBuffer.allocateDirect(64);
            pristine.put(0, (byte) 77);
            refused(() -> M3LexiconWireNative.expand(arena, offsets, sizes,
                    new int[] { 0 }, pristine), "native zero ID");
            check(pristine.get(0) == 77, "native refusal before output");
            refused(() -> M3LexiconWireNative.expand(arena, offsets, sizes,
                    new int[] { 1 }, arena), "native alias");
            refused(() -> M3LexiconWireNative.expand(arena, new int[] { -1 },
                    new int[] { 9 }, new int[] { 1 }, pristine), "native invalid offset");
        }
        System.out.println("M3_LEXICON_WIRE_PASS checks=" + checks
                + " native=" + (arguments.length > 0));
    }

    private static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }
    private static void check(boolean condition, String name) {
        checks++;
        if (!condition) throw new AssertionError(name);
    }
    private static void refused(Runnable action, String name) {
        checks++;
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("missing refusal: " + name);
    }
}
