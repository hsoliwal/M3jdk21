// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;

/**
 * A domain-separated identity for an exact reusable token-prefix block.
 * Every model, tokenizer, adapter manifest, quantization, binary layout, namespace,
 * token and absolute position participates. This key does not validate that a
 * caller's declared model/layout actually describes the payload it supplies.
 * Neither token nor position arrays are retained.
 *
 * <p>The context separation follows reusable KV-block designs studied in LMCache;
 * this implementation is independently authored and does not replace live model KV.</p>
 */
public final class PmemPrefixKey {
    private static final byte[] DOMAIN = "M3PMEM_PREFIX_KEY_V1".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_CONTEXT_UTF8_BYTES = 4096;
    private final String sha256;

    /**
     * Use digests of the complete model/tokenizer artifacts and ordered adapter
     * manifest. With no adapters, use the digest of an explicit empty manifest.
     * Layout must identify dimensions, element order, scalar format and version;
     * quantization must include scales/zero-point policy or their content identity.
     * Text is case sensitive and has no implicit Unicode normalization.
     */
    public record Context(String namespace, String modelSha256, String tokenizerSha256,
                          String quantization, String layout, String adaptersSha256) {
        public Context {
            contextBytes(namespace);
            PmemBlockStore.parseHash(modelSha256);
            PmemBlockStore.parseHash(tokenizerSha256);
            contextBytes(quantization);
            contextBytes(layout);
            PmemBlockStore.parseHash(adaptersSha256);
        }
    }

    private PmemPrefixKey(MessageDigest digest) { sha256 = HexFormat.of().formatHex(digest.digest()); }

    /** Hash all tokens and their corresponding exact, nonnegative positions. */
    public static PmemPrefixKey of(Context context, int[] tokens, long[] positions) {
        Objects.requireNonNull(tokens, "tokens");
        Objects.requireNonNull(positions, "positions");
        if (tokens.length != positions.length) throw new IllegalArgumentException("token/position count differs");
        return of(context, tokens, 0, positions, 0, tokens.length);
    }

    /** Caller arrays must remain unchanged until this method returns. */
    public static PmemPrefixKey of(Context context, int[] tokens, int tokenOffset,
                                   long[] positions, int positionOffset, int count) {
        Objects.requireNonNull(tokens, "tokens");
        Objects.requireNonNull(positions, "positions");
        Objects.checkFromIndexSize(tokenOffset, count, tokens.length);
        Objects.checkFromIndexSize(positionOffset, count, positions.length);
        MessageDigest digest = start(context, count);
        ByteBuffer pair = ByteBuffer.allocate(Integer.BYTES + Long.BYTES);
        for (int i = 0; i < count; i++) {
            int token = tokens[tokenOffset + i];
            long position = positions[positionOffset + i];
            if (token < 0 || position < 0) throw new IllegalArgumentException("negative token or position");
            pair.clear();
            pair.putInt(token).putLong(position);
            digest.update(pair.array());
        }
        return new PmemPrefixKey(digest);
    }

    /** Exact equivalent of {@link #of} for consecutive absolute positions, without a position array. */
    public static PmemPrefixKey contiguous(Context context, int[] tokens, int offset,
                                           int count, long firstPosition) {
        Objects.requireNonNull(tokens, "tokens");
        Objects.checkFromIndexSize(offset, count, tokens.length);
        if (firstPosition < 0 || count > 0 && firstPosition > Long.MAX_VALUE - (count - 1L))
            throw new IllegalArgumentException("position range overflows");
        MessageDigest digest = start(context, count);
        ByteBuffer pair = ByteBuffer.allocate(Integer.BYTES + Long.BYTES);
        for (int i = 0; i < count; i++) {
            int token = tokens[offset + i];
            if (token < 0) throw new IllegalArgumentException("negative token");
            pair.clear();
            pair.putInt(token).putLong(firstPosition + i);
            digest.update(pair.array());
        }
        return new PmemPrefixKey(digest);
    }

    public String sha256() { return sha256; }

    static PmemPrefixKey tensor(PmemPrefixKey prefix, byte[] descriptor) {
        Objects.requireNonNull(prefix, "prefix");
        MessageDigest digest = PmemBlockStore.digest();
        digest.update("M3PMEM_TYPED_PREFIX_V1".getBytes(StandardCharsets.US_ASCII));
        digest.update(PmemBlockStore.parseHash(prefix.sha256));
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(descriptor.length).array());
        digest.update(descriptor);
        return new PmemPrefixKey(digest);
    }

    @Override public String toString() { return sha256; }
    @Override public int hashCode() { return sha256.hashCode(); }
    @Override public boolean equals(Object other) {
        return other instanceof PmemPrefixKey key && sha256.equals(key.sha256);
    }

    private static MessageDigest start(Context context, int count) {
        Objects.requireNonNull(context, "context");
        MessageDigest digest = PmemBlockStore.digest();
        digest.update(DOMAIN);
        field(digest, context.namespace());
        field(digest, context.modelSha256());
        field(digest, context.tokenizerSha256());
        field(digest, context.quantization());
        field(digest, context.layout());
        field(digest, context.adaptersSha256());
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(count).array());
        return digest;
    }

    private static void field(MessageDigest digest, String field) {
        byte[] bytes = contextBytes(field);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static byte[] contextBytes(String text) {
        Objects.requireNonNull(text, "context field");
        if (text.isEmpty() || text.length() > MAX_CONTEXT_UTF8_BYTES)
            throw new IllegalArgumentException("context field is empty or too long");
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text));
            if (encoded.remaining() > MAX_CONTEXT_UTF8_BYTES)
                throw new IllegalArgumentException("UTF-8 context field is too long");
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException malformed) {
            throw new IllegalArgumentException("malformed Unicode context field", malformed);
        }
    }
}
