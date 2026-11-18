// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Prepared immutable UTF-16 literal KMP across M3StringPiece rope boundaries
 * @modules java.base/jdk.m3.text
 * @run main/othervm RopePreparedLiteralTest
 */

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jdk.m3.text.LocalM3Arena;
import jdk.m3.text.M3RopeTextIndex;
import jdk.m3.text.M3StringPiece;

public class RopePreparedLiteralTest {
    private static final char[] ALPHABET = {
        0, 1, 'a', 'b', (char) 0x00ff,
        (char) 0xd800, (char) 0xdfff, (char) 0xffff
    };
    private static long checks;

    public static void main(String[] args) {
        fixedAndDefensiveContracts();
        Random random = new Random(0x4d33524f50454b4dL);
        LocalM3Arena arena = new LocalM3Arena();
        for (int scenario = 0; scenario < 1_700; scenario++) {
            int length = scenario % 37 == 0 ? 450 : random.nextInt(180);
            char[] sourceChars = new char[length];
            for (int i = 0; i < length; i++) {
                sourceChars[i] = ALPHABET[
                        scenario % 11 == 0 ? i % 2 : random.nextInt(ALPHABET.length)];
            }
            List<M3StringPiece> pieces = new ArrayList<>();
            for (int offset = 0; offset < length;) {
                int end = Math.min(length, offset + 1 + random.nextInt(11));
                char[] part = new char[end - offset];
                System.arraycopy(sourceChars, offset, part, 0, part.length);
                pieces.add(arena.copyUtf16(part));
                if (random.nextBoolean()) pieces.add(arena.copyUtf16(new char[0]));
                offset = end;
            }
            M3RopeTextIndex index = M3RopeTextIndex.of(
                    pieces.toArray(M3StringPiece[]::new));
            String source = new String(sourceChars); // Oracle only.
            same(length, index.utf16Length(), "UTF-16 rope length");
            for (int query = 0; query < 15; query++) {
                int n = random.nextInt(22);
                char[] needle = new char[n];
                for (int i = 0; i < n; i++) {
                    needle[i] = ALPHABET[random.nextInt(ALPHABET.length)];
                }
                String literal = new String(needle);
                M3RopeTextIndex.LiteralPlan plan = M3RopeTextIndex.compileLiteral(
                        noStringConversion(needle));
                same(n, plan.length(), "compiled literal length");
                same(6L * n, plan.primitivePayloadBytes(), "primitive payload budget");
                for (int from : new int[] {0, length / 4, length / 2, length}) {
                    long expected = source.indexOf(literal, from);
                    same(expected, index.findPreparedLiteral(plan, from),
                            "prepared KMP oracle scenario=" + scenario);
                    same(expected, index.findLiteral(literal, from),
                            "existing KMP parity scenario=" + scenario);
                }
            }
        }
        System.out.println("PASS M3 rope compiled UTF-16 literal checks=" + checks);
    }

    private static void fixedAndDefensiveContracts() {
        LocalM3Arena arena = new LocalM3Arena();
        M3StringPiece joined = M3StringPiece.join(
                arena.copyUtf16(new char[] {'x', 0, (char) 0xd800}),
                arena.copyUtf16(new char[] {(char) 0xdc00, 'x', 0}),
                arena.copyUtf16(new char[] {'a', 'b'}));
        M3RopeTextIndex rope = M3RopeTextIndex.of(
                arena.copyUtf16(new char[] {'z'}),
                joined,
                arena.copyUtf16(new char[0]),
                arena.copyUtf16(new char[] {'y'}));
        M3RopeTextIndex.LiteralPlan cross =
                M3RopeTextIndex.compileLiteral("\ud800\udc00x\0a");
        same(3L, rope.findPreparedLiteral(cross, 0), "pair and NUL across leaves");
        same(-1L, rope.findPreparedLiteral(cross, 4), "later prefix refusal");
        same(0L, rope.findPreparedLiteral(M3RopeTextIndex.compileLiteral("z"), 0), "first leaf");
        same(9L, rope.findPreparedLiteral(M3RopeTextIndex.compileLiteral("y"), 0), "last leaf");

        char[] mutable = {'x', 0, 'a'};
        M3RopeTextIndex.LiteralPlan frozen =
                M3RopeTextIndex.compileLiteral(CharBuffer.wrap(mutable));
        mutable[1] = 'b';
        same(5L, rope.findPreparedLiteral(frozen, 0), "frozen pattern ignores mutation");
        M3RopeTextIndex.LiteralPlan empty = M3RopeTextIndex.compileLiteral("");
        same(0L, rope.findPreparedLiteral(empty, 0), "empty pattern at zero");
        same(rope.utf16Length(), rope.findPreparedLiteral(empty, rope.utf16Length()),
                "empty pattern at end");
        same(-1L, M3RopeTextIndex.of().findPreparedLiteral(
                M3RopeTextIndex.compileLiteral("a"), 0), "missing text");
        same(0L, M3RopeTextIndex.of().findPreparedLiteral(empty, 0), "empty text");

        expect(NullPointerException.class, () -> M3RopeTextIndex.compileLiteral(null));
        expect(NullPointerException.class, () -> rope.findPreparedLiteral(null, 0));
        expect(IndexOutOfBoundsException.class, () -> rope.findPreparedLiteral(empty, -1));
        expect(IndexOutOfBoundsException.class,
                () -> rope.findPreparedLiteral(empty, rope.utf16Length() + 1));
        CharSequence huge = new CharSequence() {
            @Override public int length() { return (1 << 20) + 1; }
            @Override public char charAt(int index) {
                throw new AssertionError("bound must be checked before reading pattern");
            }
            @Override public CharSequence subSequence(int begin, int end) {
                throw new AssertionError("no subSequence");
            }
        };
        expect(IllegalArgumentException.class, () -> M3RopeTextIndex.compileLiteral(huge));
    }

    private static CharSequence noStringConversion(char[] source) {
        char[] immutable = source.clone();
        return new CharSequence() {
            @Override public int length() { return immutable.length; }
            @Override public char charAt(int index) { return immutable[index]; }
            @Override public CharSequence subSequence(int begin, int end) {
                throw new AssertionError("prepared literal must not request subSequence");
            }
            @Override public String toString() {
                throw new AssertionError("prepared literal must not flatten");
            }
        };
    }

    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable error) {
            if (kind.isInstance(error)) return;
            throw new AssertionError("wrong exception " + error, error);
        }
        throw new AssertionError("expected " + kind.getName());
    }

    private static void same(long expected, long actual, String description) {
        checks++;
        if (expected != actual) {
            throw new AssertionError(description + " expected=" + expected
                    + " actual=" + actual + " check=" + checks);
        }
    }
}
