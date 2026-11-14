// Modified for M3JDK: attributed package relocation and product module integration.
// Derivation: PINNED_JSON_HELPER_ATOM_EXTRACTION; see SOURCE_PROVENANCE.tsv.
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Extracted from PinnedGitObjects at 2ab439480ecb0643615e85b42def954dbf3b5627.
// Modification: retain only the strict UTF-8, SHA-256 and precondition atoms used by JSON.
package jdk.m3.llm.internal.json;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Internal exact helper closure for the existing strict JSON implementation. */
final class JsonSupport {
    private JsonSupport() { }

    static String text(byte[] bytes) {
        try { return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString(); }
        catch (CharacterCodingException failure) { throw new IllegalArgumentException("INVALID_UTF8"); }
    }

    static String sha256(byte[] bytes) { return hash("SHA-256", bytes); }

    private static String hash(String algorithm, byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes)); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException("DIGEST_UNAVAILABLE"); }
    }

    static void require(boolean condition, String code) { if (!condition) throw new IllegalArgumentException(code); }
}
