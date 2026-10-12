/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Verifies Synexia's versioned LangDex source-export envelope without admitting
 * its rows to an M3 lexicon or assigning M3 record identities.
 *
 * <p>The receiver consumes the original wire bytes, validates their grammar,
 * and returns only the source receipt. It intentionally does not expose parsed
 * rows, normalize source fields, resolve concept links, or synthesize precompute
 * payloads. Callers must not treat a successful receipt as catalog admission.</p>
 *
 * <p>Input is the ASCII-only UTF-8/LF format emitted by the pinned Synexia
 * producer. Lines are capped at 16 MiB to bound per-row validation memory. The
 * input stream remains open after this method returns.</p>
 */
public final class SynexiaLangDexSourceEnvelope {
    private static final String MAGIC = "#M3-LANGDEX-SOURCE-1";
    private static final String HEADER =
            "row_type\tlexeme_id\tglottocode_b64\tlemma_b64\tsource_b64\tconcept_id";
    private static final String UNUSED = "-";
    private static final String NULL_VALUE = "~";
    private static final String VALUE_PREFIX = "b64:";
    private static final int MAX_LINE_BYTES = 16 * 1024 * 1024;
    private static final Pattern REVISION = Pattern.compile("[0-9a-f]{40}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    /** The producer's dataset revision, row counts, and exact wire digest. */
    public record Receipt(
            String sourceRevision,
            long lexemeRows,
            long conceptLinkRows,
            String sha256) {
        public Receipt {
            if (sourceRevision == null || !REVISION.matcher(sourceRevision).matches()) {
                throw new IllegalArgumentException(
                        "sourceRevision must be a lowercase 40-digit Git SHA");
            }
            if (lexemeRows < 0 || conceptLinkRows < 0) {
                throw new IllegalArgumentException("row counts must be nonnegative");
            }
            if (sha256 == null || !SHA256.matcher(sha256).matches()) {
                throw new IllegalArgumentException("sha256 must be lowercase 64-digit hex");
            }
        }
    }

    private SynexiaLangDexSourceEnvelope() {}

    /**
     * Verifies one complete producer artifact against its independently supplied
     * receipt. No mapping, catalog mutation, or precompute interpretation occurs.
     *
     * @param input original producer bytes; left open
     * @param expected receipt produced for those bytes by the source owner
     * @return the validated receipt
     * @throws IOException if the wire grammar or any expected receipt field differs
     */
    public static Receipt verify(InputStream input, Receipt expected) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(expected, "expected");

        MessageDigest digest = sha256();
        InputStream bytes = new BufferedInputStream(new DigestInputStream(input, digest));
        String first = readAsciiLine(bytes);
        String second = readAsciiLine(bytes);
        String expectedFirst = MAGIC + "\t" + expected.sourceRevision();
        if (!expectedFirst.equals(first)) {
            throw new IOException("LangDex source magic or revision mismatch");
        }
        if (!HEADER.equals(second)) {
            throw new IOException("LangDex source header mismatch");
        }

        long lexemeRows = 0;
        long conceptLinkRows = 0;
        String line;
        while ((line = readAsciiLine(bytes)) != null) {
            String[] fields = line.split("\t", -1);
            if (fields.length != 6) {
                throw new IOException("LangDex source row must have exactly six columns");
            }
            if ("LEXEME".equals(fields[0])) {
                if (lexemeRows >= expected.lexemeRows()) {
                    throw new IOException("LangDex lexeme row count exceeds receipt");
                }
                requireCanonicalLong(fields[1], true);
                decodeNullable(fields[2]);
                decodeNullable(fields[3]);
                decodeNullable(fields[4]);
                requireUnused(fields[5]);
                lexemeRows = increment(lexemeRows);
            } else if ("CONCEPT_LINK".equals(fields[0])) {
                if (conceptLinkRows >= expected.conceptLinkRows()) {
                    throw new IOException("LangDex concept-link row count exceeds receipt");
                }
                requireCanonicalLong(fields[1], true);
                requireUnused(fields[2]);
                requireUnused(fields[3]);
                requireUnused(fields[4]);
                requireCanonicalLong(fields[5], true);
                conceptLinkRows = increment(conceptLinkRows);
            } else {
                throw new IOException("Unknown LangDex source row type");
            }
        }

        if (lexemeRows != expected.lexemeRows()
                || conceptLinkRows != expected.conceptLinkRows()) {
            throw new IOException("LangDex source row counts differ from receipt");
        }
        String actualSha256 = HexFormat.of().formatHex(digest.digest());
        if (!expected.sha256().equals(actualSha256)) {
            throw new IOException("LangDex source digest differs from receipt");
        }
        return new Receipt(expected.sourceRevision(), lexemeRows, conceptLinkRows, actualSha256);
    }

    private static String readAsciiLine(InputStream input) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        while (true) {
            int next = input.read();
            if (next < 0) {
                if (line.size() == 0) return null;
                throw new IOException("LangDex source line is not LF-terminated");
            }
            if (next == '\n') {
                return line.toString(java.nio.charset.StandardCharsets.US_ASCII);
            }
            if (next == '\r' || next > 0x7f) {
                throw new IOException("LangDex source wire must be ASCII with LF newlines");
            }
            if (line.size() >= MAX_LINE_BYTES) {
                throw new IOException("LangDex source line exceeds the 16 MiB limit");
            }
            line.write(next);
        }
    }

    private static void requireCanonicalLong(String value, boolean nullable) throws IOException {
        if (nullable && NULL_VALUE.equals(value)) return;
        try {
            long parsed = Long.parseLong(value);
            if (!Long.toString(parsed).equals(value)) {
                throw new IOException("LangDex integer is not canonical decimal");
            }
        } catch (NumberFormatException invalid) {
            throw new IOException("LangDex integer is outside canonical signed-64-bit range", invalid);
        }
    }

    private static void decodeNullable(String value) throws IOException {
        if (NULL_VALUE.equals(value)) return;
        if (!value.startsWith(VALUE_PREFIX)) {
            throw new IOException("LangDex text must use the null marker or b64: encoding");
        }
        String encoded = value.substring(VALUE_PREFIX.length());
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            if (!Base64.getEncoder().encodeToString(decoded).equals(encoded)) {
                throw new IOException("LangDex text Base64 is not canonical");
            }
            java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(decoded));
        } catch (IllegalArgumentException invalid) {
            throw new IOException("LangDex text is not valid standard Base64", invalid);
        } catch (java.nio.charset.CharacterCodingException invalidUtf8) {
            throw new IOException("LangDex text is not valid UTF-8", invalidUtf8);
        }
    }

    private static void requireUnused(String value) throws IOException {
        if (!UNUSED.equals(value)) {
            throw new IOException("LangDex unused field must be '-'");
        }
    }

    private static long increment(long value) throws IOException {
        try {
            return Math.addExact(value, 1L);
        } catch (ArithmeticException overflow) {
            throw new IOException("LangDex row count overflow", overflow);
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("SHA-256 is required by the Java platform", impossible);
        }
    }
}
