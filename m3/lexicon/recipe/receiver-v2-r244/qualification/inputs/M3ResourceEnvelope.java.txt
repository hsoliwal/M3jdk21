// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Lossless, explicitly marked resource representation for existing hash-pinned templates.
 *
 * <p>A matching raw digest always wins, including literal envelope text. Unmarked
 * resources remain raw so each caller retains its existing hash-drift refusal.
 * Decoding is single-level and never reads or writes a file, network or source tree.
 * The ASCII wrapper's license does not replace the decoded payload's rights.
 */
final class M3ResourceEnvelope {
    private static final String MARKER = "M3_RESOURCE_ENVELOPE\n";
    private static final String COPYRIGHT =
            "# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project";
    private static final String LICENSE = "# SPDX-License-Identifier: Apache-2.0";
    private static final String RIGHTS =
            "# Wrapper only; decoded payload rights and notices are unchanged.";

    private M3ResourceEnvelope() { }

    /** Returns exact UTF-8 payload text or the unchanged legacy raw resource. */
    static String decode(String raw, String expectedSha256) {
        if (sha256(raw.getBytes(StandardCharsets.UTF_8)).equals(expectedSha256)
                || !raw.startsWith(MARKER)) {
            return raw;
        }
        String[] lines = raw.split("\n", -1);
        if (lines.length != 9 || !lines[0].equals("M3_RESOURCE_ENVELOPE")
                || !lines[1].equals("version=1") || !lines[2].equals(COPYRIGHT)
                || !lines[3].equals(LICENSE) || !lines[4].equals(RIGHTS)
                || !lines[5].startsWith("sha256=") || !lines[6].startsWith("bytes=")
                || !lines[7].startsWith("base64=") || !lines[8].isEmpty()) {
            throw new IllegalStateException("invalid M3 resource envelope schema");
        }
        String declaredSha256 = lines[5].substring("sha256=".length());
        String length = lines[6].substring("bytes=".length());
        String encoded = lines[7].substring("base64=".length());
        if (!declaredSha256.matches("[0-9a-f]{64}")
                || !declaredSha256.equals(expectedSha256)
                || !length.matches("0|[1-9][0-9]*")) {
            throw new IllegalStateException("invalid M3 resource envelope identity");
        }
        try {
            long declaredBytes = Long.parseLong(length);
            byte[] payload = Base64.getDecoder().decode(encoded);
            if (declaredBytes != payload.length
                    || !Base64.getEncoder().encodeToString(payload).equals(encoded)
                    || !sha256(payload).equals(declaredSha256)) {
                throw new IllegalStateException("invalid M3 resource envelope payload");
            }
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(payload)).toString();
        } catch (IllegalArgumentException | CharacterCodingException failure) {
            throw new IllegalStateException("invalid M3 resource envelope encoding", failure);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
