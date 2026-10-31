// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.mindex.compiler.ast;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Exact identity of the compiler's source characters and one consumer projection.
 *
 * <p>The source digest covers raw UTF-16 code units, including unpaired surrogates. It is not
 * presented as the digest of the original file bytes. The record retains no source payload.
 * Source URI, compiler context, provider, schema and phase are separate identity dimensions.</p>
 */
public record M3FileSourceFacts(
        String sourceUri,
        String sourceSha256,
        int utf16Length,
        String contextSha256,
        String providerId,
        int schemaVersion,
        String phase) {
    private static final byte[] SOURCE_DOMAIN =
            "M3_FILE_SOURCE_UTF16_V1".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] CONTEXT_DOMAIN =
            "M3_COMPILER_CONTEXT_UTF16_V1".getBytes(StandardCharsets.US_ASCII);

    public M3FileSourceFacts {
        sourceUri = requireText(sourceUri, "sourceUri", 16_384);
        sourceSha256 = requireSha256(sourceSha256, "sourceSha256");
        if (utf16Length < 0) throw new IllegalArgumentException("negative UTF-16 length");
        contextSha256 = requireSha256(contextSha256, "contextSha256");
        providerId = requireText(providerId, "providerId", 1_024);
        if (schemaVersion < 1) throw new IllegalArgumentException("schemaVersion must be positive");
        phase = requireText(phase, "phase", 64);
    }

    /** Capture metadata from a stable source snapshot already supplied to the compiler. */
    public static M3FileSourceFacts capture(
            String sourceUri,
            CharSequence source,
            String contextSha256,
            String providerId,
            int schemaVersion,
            String phase) {
        String stable = Objects.requireNonNull(source, "source").toString();
        return new M3FileSourceFacts(
                sourceUri, sourceSha256(stable), stable.length(), contextSha256,
                providerId, schemaVersion, phase);
    }

    /** Domain-separated SHA-256 over the length and exact big-endian UTF-16 code units. */
    public static String sourceSha256(CharSequence source) {
        String stable = Objects.requireNonNull(source, "source").toString();
        MessageDigest digest = sha256();
        digest.update(SOURCE_DOMAIN);
        frame(digest, stable);
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Hash an ordered context description without delimiter ambiguity or Unicode replacement.
     *
     * <p>The caller includes the actual compiler/provider versions, ordered options, classpath,
     * module-path and processor content identities, plus source-set or generation identity.
     * This helper cannot discover that environment or prove a caller supplied it completely.</p>
     */
    public static String contextSha256(List<String> orderedFacts) {
        List<String> facts = List.copyOf(Objects.requireNonNull(orderedFacts, "orderedFacts"));
        MessageDigest digest = sha256();
        digest.update(CONTEXT_DOMAIN);
        updateInt(digest, facts.size());
        for (String fact : facts) frame(digest, fact);
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Conservative metadata accounting for the existing cache's payload weight plane. */
    long retainedBytes() {
        return 384L + 2L * (sourceUri.length() + sourceSha256.length()
                + contextSha256.length() + providerId.length() + phase.length());
    }

    static String requireSha256(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.length() != 64) throw new IllegalArgumentException(name + " must be SHA-256");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f')) {
                throw new IllegalArgumentException(name + " must be canonical lowercase SHA-256");
            }
        }
        return value;
    }

    private static String requireText(String value, String name, int limit) {
        Objects.requireNonNull(value, name);
        if (value.isBlank() || value.length() > limit) {
            throw new IllegalArgumentException(name + " must contain 1.." + limit + " characters");
        }
        return value;
    }

    private static void frame(MessageDigest digest, String value) {
        updateInt(digest, value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            digest.update((byte) (c >>> 8));
            digest.update((byte) c);
        }
    }

    private static void updateInt(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
