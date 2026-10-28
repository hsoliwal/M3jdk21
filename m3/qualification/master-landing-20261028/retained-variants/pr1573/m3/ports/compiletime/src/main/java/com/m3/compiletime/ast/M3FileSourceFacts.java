// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.compiletime.ast;

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
    /** The source URI differs from the previous snapshot. */
    public static final int SOURCE_URI_CHANGED = 1;
    /** The source SHA-256 or UTF-16 length differs from the previous snapshot. */
    public static final int SOURCE_CONTENT_CHANGED = 1 << 1;
    /** The caller-supplied ordered compilation-context identity differs. */
    public static final int COMPILATION_CONTEXT_CHANGED = 1 << 2;
    /** The compiler/consumer projection provider differs. */
    public static final int PROVIDER_CHANGED = 1 << 3;
    /** The projection schema version differs. */
    public static final int SCHEMA_CHANGED = 1 << 4;
    /** The compiler phase differs. */
    public static final int PHASE_CHANGED = 1 << 5;

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

    /**
     * Report every stored identity dimension that differs from a previous snapshot.
     *
     * <p>Zero means equality of all record components, not ABI or semantic equivalence.
     * Source changes are not classified as constant, signature, body or comment changes.
     * A matching context digest cannot establish that its caller captured every input.
     * This method reads only the immutable facts; it does not inspect files or build outputs.</p>
     *
     * @param previous the previous complete facts value
     * @return the bitwise OR of all applicable {@code *_CHANGED} constants, or zero
     * @throws NullPointerException if {@code previous} is null
     */
    public int changesFrom(M3FileSourceFacts previous) {
        Objects.requireNonNull(previous, "previous");
        int changes = 0;
        if (!sourceUri.equals(previous.sourceUri)) changes |= SOURCE_URI_CHANGED;
        if (!sourceSha256.equals(previous.sourceSha256) || utf16Length != previous.utf16Length) {
            changes |= SOURCE_CONTENT_CHANGED;
        }
        if (!contextSha256.equals(previous.contextSha256)) changes |= COMPILATION_CONTEXT_CHANGED;
        if (!providerId.equals(previous.providerId)) changes |= PROVIDER_CHANGED;
        if (schemaVersion != previous.schemaVersion) changes |= SCHEMA_CHANGED;
        if (!phase.equals(previous.phase)) changes |= PHASE_CHANGED;
        return changes;
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
     * Hash a caller-owned stable immutable source without copying or slicing its characters.
     *
     * <p>The result uses exactly the same domain and UTF-16 framing as {@link #sourceSha256}.
     * The caller must keep the sequence's length and characters stable throughout this call.
     * Mutable inputs continue to use the original snapshot-taking method.</p>
     */
    public static String sourceSha256ImmutableView(CharSequence source) {
        Objects.requireNonNull(source, "source");
        MessageDigest digest = sha256();
        digest.update(SOURCE_DOMAIN);
        frame(digest, source);
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

    private static void frame(MessageDigest digest, CharSequence value) {
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
