// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.mindex.compiler.ast;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class M3FileSourceFactsTest {
    @Test void preservesEveryUtf16CodeUnitAndLength() throws Exception {
        String source = "\ufeffclass A { // fake class B {}\r\n String s = \"\\u0000\"; }"
                + new String(new char[] {'\ud800', '\udfff', '\ud800'});
        String expected;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write("M3_FILE_SOURCE_UTF16_V1".getBytes(StandardCharsets.US_ASCII));
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(source.length());
            for (char unit : source.toCharArray()) out.writeChar(unit);
            expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        }
        M3FileSourceFacts key = M3FileSourceFacts.capture("memory:///A.java", source,
                "a".repeat(64), "javac-syntax-v1", 1, "PARSE");
        assertEquals(expected, key.sourceSha256());
        assertEquals(source.length(), key.utf16Length());
        assertNotEquals(M3FileSourceFacts.sourceSha256(new String(new char[] {'\ud800'})),
                M3FileSourceFacts.sourceSha256(new String(new char[] {'\ud801'})));
        assertNotEquals(M3FileSourceFacts.sourceSha256(""), M3FileSourceFacts.sourceSha256("\0"));
        assertNotEquals(M3FileSourceFacts.sourceSha256("a\r\n"), M3FileSourceFacts.sourceSha256("a\n"));
    }

    @Test void contextFramingPreservesOrderAndBoundaries() {
        assertNotEquals(M3FileSourceFacts.contextSha256(List.of("ab", "c")),
                M3FileSourceFacts.contextSha256(List.of("a", "bc")));
        assertNotEquals(M3FileSourceFacts.contextSha256(List.of("A.jar", "B.jar")),
                M3FileSourceFacts.contextSha256(List.of("B.jar", "A.jar")));
        assertNotEquals(M3FileSourceFacts.contextSha256(List.of()),
                M3FileSourceFacts.contextSha256(List.of("")));
        assertEquals(M3FileSourceFacts.contextSha256(List.of("javac21", "--release", "21")),
                M3FileSourceFacts.contextSha256(List.of("javac21", "--release", "21")));
    }

    @Test void snapshotsMutableCharacterIngressAndRejectsMalformedMetadata() {
        StringBuilder source = new StringBuilder("class A {}");
        M3FileSourceFacts captured = M3FileSourceFacts.capture("memory:///A.java", source,
                "a".repeat(64), "javac", 1, "PARSE");
        source.append(" class B {}");
        assertEquals(M3FileSourceFacts.sourceSha256("class A {}"), captured.sourceSha256());
        assertThrows(IllegalArgumentException.class, () -> new M3FileSourceFacts("", "a".repeat(64),
                0, "b".repeat(64), "javac", 1, "PARSE"));
        assertThrows(IllegalArgumentException.class, () -> new M3FileSourceFacts("a", "A".repeat(64),
                0, "b".repeat(64), "javac", 1, "PARSE"));
        assertThrows(IllegalArgumentException.class, () -> new M3FileSourceFacts("a", "a".repeat(64),
                -1, "b".repeat(64), "javac", 1, "PARSE"));
        assertThrows(IllegalArgumentException.class, () -> new M3FileSourceFacts("a", "a".repeat(64),
                0, "b".repeat(64), "javac", 0, "PARSE"));
    }
}
