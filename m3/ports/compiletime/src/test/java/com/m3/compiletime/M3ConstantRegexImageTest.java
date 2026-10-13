// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class M3ConstantRegexImageTest {
    private static final List<String> PATTERNS = List.of("hello", "foo\\d+bar", "cat|dog", "\\s+", "(unclosed");

    private static byte[] image() {
        return M3ConstantRegexImageWriter.write(PATTERNS.stream()
                .map(p -> new M3ConstantRegexImageWriter.Item(p, 0, M3RegexRequirement.analyze(p, 0))).toList(),
                M3ConstantRegexImageWriter.sha256Ints("test"));
    }

    @Test
    void roundTripsByContentKeyAndIsDeterministic() throws Exception {
        byte[] bytes = image();
        assertArrayEquals(bytes, image());
        M3ConstantRegexImage read = M3ConstantRegexImage.verify(ByteBuffer.wrap(bytes));
        assertEquals(PATTERNS.size(), read.entryCount());
        M3ConstantRegexImage.Entry e = read.lookup("foo\\d+bar", 0).orElseThrow();
        assertEquals(M3RegexRequirement.Kind.REQUIRED, e.kind());
        assertArrayEquals(new String[] {"foo", "bar"}, e.atoms());
        assertArrayEquals(new long[] {((long) 'f' << 32) | ((long) 'o' << 16) | 'o'}, e.atomTrigrams()[0]);
        assertEquals(M3RegexRequirement.Kind.INVALID, read.lookup("(unclosed", 0).orElseThrow().kind());
        assertTrue(read.lookup("hello", Pattern.CASE_INSENSITIVE).isEmpty(), "flags are part of the key");
        assertTrue(read.lookup("hellp", 0).isEmpty());
        assertArrayEquals(M3ConstantRegexImageWriter.sha256Ints("test"), read.inputSetSha256());
    }

    @Test
    void anyByteDriftIsRefused() {
        byte[] bytes = image();
        for (int at : new int[] {0, 40, bytes.length / 2, bytes.length - 1}) {
            byte[] copy = bytes.clone();
            copy[at] ^= 1;
            assertThrows(M3ConstantRegexImage.Refusal.class,
                    () -> M3ConstantRegexImage.verify(ByteBuffer.wrap(copy)));
        }
        assertThrows(M3ConstantRegexImage.Refusal.class,
                () -> M3ConstantRegexImage.verify(ByteBuffer.wrap(new byte[12])));
    }

    @Test
    void imageFromAnotherPatternParserIsRefusedEvenWithAValidTrailer() {
        byte[] bytes = image();
        ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(M3ConstantRegexImage.H_JDK_SHA * 4, b.getInt(M3ConstantRegexImage.H_JDK_SHA * 4) ^ 1);
        int[] sha = M3ConstantRegexImageWriter.sha256Ints(ByteBuffer.wrap(bytes, 0, bytes.length - 32));
        for (int i = 0; i < 8; i++) {
            b.putInt(bytes.length - 32 + i * 4, sha[i]);
        }
        M3ConstantRegexImage.Refusal refusal = assertThrows(M3ConstantRegexImage.Refusal.class,
                () -> M3ConstantRegexImage.verify(ByteBuffer.wrap(bytes)));
        assertTrue(refusal.getMessage().contains("Pattern.class"));
    }

    @Test
    void gateIsStockEquivalentAndPrunesOnlyProvenAbsence() throws Exception {
        M3ConstantRegexImage read = M3ConstantRegexImage.verify(ByteBuffer.wrap(image()));
        M3ConstantRegexGate pure = M3ConstantRegexGate.of(Pattern.compile("hello"), read);
        M3ConstantRegexGate req = M3ConstantRegexGate.of(Pattern.compile("foo\\d+bar"), read);
        M3ConstantRegexGate alt = M3ConstantRegexGate.of(Pattern.compile("cat|dog"), read);
        M3ConstantRegexGate miss = M3ConstantRegexGate.of(Pattern.compile("zzz"), read);
        M3ConstantRegexGate none = M3ConstantRegexGate.of(Pattern.compile("hello"), null);
        assertTrue(pure.hit() && req.hit() && alt.hit());
        assertFalse(miss.hit() || none.hit());
        for (String s : List.of("", "hello", "say hello", "foo12bar", "foobar", "bar foo1", "hot dog", "catalog")) {
            for (M3ConstantRegexGate g : List.of(pure, req, alt, miss, none)) {
                Pattern p = g.pattern();
                assertEquals(p.matcher(s).find(), g.find(s), p + " find " + s);
                assertEquals(p.matcher(s).matches(), g.matches(s), p + " matches " + s);
                assertEquals(p.matcher(s).replaceAll("#"), g.replaceAll(s, "#"));
                assertArrayEquals(p.split(s), g.split(s));
            }
        }
        assertFalse(req.holds("foo only"));
        assertTrue(alt.holds("hot dog"));
    }
}
