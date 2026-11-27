/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.parallel;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Length-framed SHA-256 roots. Framing follows the cpu-gpu receipt convention: every string is a big-endian
 * 4-byte length plus UTF-8 bytes; an int array is its framed decimal length plus big-endian elements.
 */
final class Digests {
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final String BINDINGS_DOMAIN = "M3_PARALLEL_BINDINGS_V1";

    private Digests() {
    }

    static String sha256(String domain, String... values) {
        MessageDigest digest = digest();
        frame(digest, domain);
        for (String value : values) {
            frame(digest, value);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static String bindingsRoot(List<Plan.Binding> bindings) {
        MessageDigest digest = digest();
        frame(digest, BINDINGS_DOMAIN);
        frame(digest, Integer.toString(bindings.size()));
        bindings.forEach(binding -> {
            frame(digest, binding.transfer().name());
            ints(digest, binding.buffer());
        });
        return HexFormat.of().formatHex(digest.digest());
    }

    static String requireSha256(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (!SHA256.matcher(checked).matches()) {
            throw new IllegalArgumentException(field + " must be 64 lowercase hex characters");
        }
        return checked;
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the Java SE platform", impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static void ints(MessageDigest digest, IntBuffer values) {
        int length = values.length();
        frame(digest, Integer.toString(length));
        final int chunkElements = 4096;
        int[] chunk = new int[Math.min(chunkElements, length)];
        ByteBuffer bytes = ByteBuffer.allocate(chunk.length * Integer.BYTES);
        for (int start = 0; start < length; ) {
            int count = Math.min(chunk.length, length - start);
            values.copyTo(start, chunk, 0, count);
            bytes.clear();
            for (int i = 0; i < count; i++) {
                bytes.putInt(chunk[i]);
            }
            digest.update(bytes.array(), 0, count * Integer.BYTES);
            start += count;
        }
    }
}
