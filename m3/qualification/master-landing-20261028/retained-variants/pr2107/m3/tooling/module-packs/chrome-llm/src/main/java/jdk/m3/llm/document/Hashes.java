/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK21 from m3-flldcim/tools/m3-text-pipeline/src/main/java/jdk/internal/m3/flldcim/tools/Hashes.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.document;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.CRC32C;

/** Versioned, domain-separated fingerprints. None is a behavioral-equivalence proof. */
public final class Hashes {
    private Hashes() { }
    public static String sha(byte[] value) { return HexFormat.of().formatHex(digest().digest(value)); }
    public static String sha(String value) { return sha(value.getBytes(StandardCharsets.UTF_8)); }
    public static String framed(String domain, List<String> fields) {
        MessageDigest md = digest();
        add(md, domain);
        fields.forEach(v -> add(md, v));
        return HexFormat.of().formatHex(md.digest());
    }
    private static void add(MessageDigest md, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        md.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
        md.update(bytes);
    }
    public static String fast(byte[] value) {
        CRC32C crc = new CRC32C(); crc.update(value, 0, value.length);
        return Long.toUnsignedString(crc.getValue(), 16);
    }
    public static long simHash(List<String> features) {
        long[] votes = new long[64];
        for (String feature : features) {
            long hash = ByteBuffer.wrap(digest().digest(feature.getBytes(StandardCharsets.UTF_8))).getLong();
            for (int bit = 0; bit < 64; bit++) votes[bit] += ((hash >>> bit) & 1) == 0 ? -1 : 1;
        }
        long value = 0;
        for (int bit = 0; bit < 64; bit++) if (votes[bit] > 0) value |= 1L << bit;
        return value;
    }
    public static int distance(long left, long right) { return Long.bitCount(left ^ right); }
    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
