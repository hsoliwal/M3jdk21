// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 per chunk as one packed digest lane (32 bytes per chunk) with a hex view.
 *
 * <p>Input: bytes and {@link M3ChunkCdc.Chunks}. Output: {@link Digests}. Effects: none. Failure:
 * none (SHA-256 is mandatory in every JDK). The digest contract is the specgrep
 * {@code CanonicalHashBackend} one: exact SHA-256, input order preserved; a digest proves equality
 * of chunk bytes, never semantic equivalence.</p>
 */
//FLLDCIM: CDC — exact SHA-256 per chunk; lane of 32-byte digests, hex on demand
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/f7b9599a63da1c1264ecacdcb4c9286e055a0948/synexia-openrewrite-recipes/crates/m3-hash-family-20261030/src/main/java/com/synexia/rewrite/hash/M3ChunkSha.java blob 4090aca56c04; recipe m3/indexdb/recipe/hash-family-20261106/relocate.py v3
public final class M3ChunkSha {
    public static final String ALGORITHM = "CHUNK_SHA256";
    public static final int VERSION = 1;
    public static final int DIGEST_BYTES = 32;

    /** {@code lane[i * 32, (i + 1) * 32)} is the digest of chunk {@code i}. */
    public record Digests(byte[] lane, int count) {
        public String hex(int i) {
            return HexFormat.of().formatHex(lane, i * DIGEST_BYTES, (i + 1) * DIGEST_BYTES);
        }
    }

    private M3ChunkSha() { }

    public static Digests of(byte[] bytes, M3ChunkCdc.Chunks chunks) {
        MessageDigest digest = sha256();
        byte[] lane = new byte[chunks.count() * DIGEST_BYTES];
        for (int i = 0; i < chunks.count(); i++) {
            digest.update(bytes, chunks.offsets()[i], chunks.lengths()[i]);
            try {
                digest.digest(lane, i * DIGEST_BYTES, DIGEST_BYTES);
            } catch (java.security.DigestException impossible) {
                throw new IllegalStateException("SHA-256 output buffer", impossible);
            }
        }
        return new Digests(lane, chunks.count());
    }

    public static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(sha256().digest(bytes));
    }

    static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
