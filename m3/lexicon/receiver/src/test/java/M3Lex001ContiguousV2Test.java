// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import jdk.m3.lexicon.M3Lex001Image;
import jdk.m3.lexicon.M3LexiconFormatException;
import jdk.m3.lexicon.M3LexiconWordIds;

/** Source-built M3LEX receiver differential proof; no external corpora or IDs. */
public final class M3Lex001ContiguousV2Test {
    private static int assertions;

    private M3Lex001ContiguousV2Test() { }

    public static void main(String[] args) throws Exception {
        List<String> words = new ArrayList<>(List.of(
                "", "a", "Aa", "BB", "e\u0301", "\u00e9", "\u0928\u092e\u0938\u094d\u0924\u0947", "\u732b", "\ud83d\ude00"));
        words.add(new String(new char[] {(char) 0xd800}));
        words.add(new String(new char[] {(char) 0xdc00}));
        words.sort(String::compareTo);
        byte[] canonical = M3Lex001Image.write(words);
        var facts = M3Lex001Image.verify(ByteBuffer.wrap(canonical));
        check(facts.version() == 2 && facts.count() == words.size(), "exact v2 count");
        for (int row = 1; row <= words.size(); row++) {
            long id = M3LexiconWordIds.canonicalId(facts.generation64(), row);
            check(M3LexiconWordIds.wordId(id, facts.generation64()) == row,
                    "canonical dense ID roundtrip row " + row);
        }
        var prefixed = prefixHole(canonical);
        refused(prefixed, "noncontiguous", "v2 unused UTF-16 prefix");
        refused(trailingUnused(canonical), "unreferenced trailing", "v2 unused UTF-16 suffix");
        refused(prefixHole(M3Lex001Image.write(List.of("a", "b"))),
                "noncontiguous", "short v2 prefix gap");
        refused(trailingUnused(M3Lex001Image.write(List.of())),
                "unreferenced trailing", "zero-word v2 must own zero payload");
        byte[] corrupt = canonical.clone();
        corrupt[corrupt.length - 1] ^= 1;
        refused(corrupt, "checksum", "integrity and geometry remain independent gates");

        // Only v2 is tightened. v1 preexisting sparse source acceptance is not
        // silently removed by this compatibility-safe v2-only change.
        var legacyContiguous = v1Image(canonical);
        var legacySparse = v1Image(prefixed);
        check(M3Lex001Image.verify(ByteBuffer.wrap(legacyContiguous)).version() == 1,
                "legacy v1 contiguous image remains compatible");
        check(M3Lex001Image.verify(ByteBuffer.wrap(legacySparse)).version() == 1,
                "legacy v1 sparse image remains compatible");
        check(M3Lex001Image.verify(ByteBuffer.wrap(M3Lex001Image.write(List.of()))).count() == 0,
                "empty canonical v2 image still valid");
        System.out.println("M3LEX_V2_CONTIGUOUS_JAVA21_PASS assertions=" + assertions
                + " words=" + words.size() + " test=version2_only");
    }

    private static byte[] prefixHole(byte[] image) throws Exception {
        ByteBuffer header = ByteBuffer.wrap(image).order(ByteOrder.BIG_ENDIAN);
        int count = header.getInt(12);
        int payload = Math.toIntExact(header.getLong(16));
        byte[] mutated = new byte[image.length + 2];
        System.arraycopy(image, 0, mutated, 0, payload);
        mutated[payload] = 0x23; // deliberately unreferenced '#' code unit
        System.arraycopy(image, payload, mutated, payload + 2, image.length - payload);
        ByteBuffer out = ByteBuffer.wrap(mutated).order(ByteOrder.BIG_ENDIAN);
        out.putLong(24, header.getLong(24) + 1);
        for (int row = 0; row < count; row++) {
            int at = 64 + 12 * row;
            out.putInt(at, out.getInt(at) + 1);
        }
        reseal(mutated);
        return mutated;
    }

    private static byte[] trailingUnused(byte[] image) throws Exception {
        byte[] mutated = java.util.Arrays.copyOf(image, image.length + 2);
        mutated[mutated.length - 2] = 0x24; // '$' outside every word
        var out = ByteBuffer.wrap(mutated).order(ByteOrder.BIG_ENDIAN);
        out.putLong(24, out.getLong(24) + 1);
        reseal(mutated);
        return mutated;
    }

    private static byte[] v1Image(byte[] version2) throws Exception {
        var src = ByteBuffer.wrap(version2).order(ByteOrder.BIG_ENDIAN);
        int count = src.getInt(12);
        int payload = Math.toIntExact(src.getLong(16));
        int v1Payload = 64 + 8 * count;
        byte[] legacy = new byte[v1Payload + version2.length - payload];
        System.arraycopy(version2, 0, legacy, 0, 64);
        var out = ByteBuffer.wrap(legacy).order(ByteOrder.BIG_ENDIAN);
        out.putInt(8, 1);
        out.putLong(16, v1Payload);
        for (int row = 0; row < count; row++) {
            out.putInt(64 + 8 * row, src.getInt(64 + 12 * row));
            out.putInt(64 + 8 * row + 4, src.getInt(64 + 12 * row + 4));
        }
        System.arraycopy(version2, payload, legacy, v1Payload, version2.length - payload);
        reseal(legacy);
        return legacy;
    }

    private static void reseal(byte[] image) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        digest.update(image, 0, 32);
        digest.update(image, 64, image.length - 64);
        System.arraycopy(digest.digest(), 0, image, 32, 32);
    }

    private static void refused(byte[] bytes, String fragment, String caseName) throws Exception {
        try {
            M3Lex001Image.verify(ByteBuffer.wrap(bytes));
            throw new AssertionError("accepted malformed image: " + caseName);
        } catch (M3LexiconFormatException expected) {
            check(expected.getMessage().contains(fragment),
                    "expected " + fragment + " for " + caseName + ", got " + expected.getMessage());
        }
    }

    private static void check(boolean pass, String caseName) {
        if (!pass) throw new AssertionError(caseName);
        assertions++;
    }
}
