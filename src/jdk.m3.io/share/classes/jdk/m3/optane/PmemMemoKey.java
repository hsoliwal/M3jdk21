// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * An exact, versioned memo identity with a bounded canonical representation.
 * Names are sorted in Java String lexical order and every UTF-8 field has an
 * explicit byte length. Text is case sensitive, with no Unicode normalization.
 * Neither caller maps nor arrays are retained. Equality compares complete
 * canonical bytes, not only their SHA-256 digest.
 *
 * <p>All inputs that affect a computation must participate. In particular, an
 * AI caller must identify model weights, tokenizer, adapters, prompt/tool input,
 * policy and external-state validity as applicable. Similar inputs do not imply
 * equivalent results. This class establishes identity, not result correctness.</p>
 */
public final class PmemMemoKey {
    public static final int MAX_DIMENSIONS = 64;
    public static final int MAX_CANONICAL_BYTES = 64 * 1024;
    private static final byte[] DOMAIN =
            "M3PMEM_MEMO_KEY_V1".getBytes(StandardCharsets.US_ASCII);
    private final byte[] canonical;
    private final String sha256;

    /**
     * Explicit validity boundary; incomplete dependency declarations can never establish safe
     * reuse.
     */
    public enum SourceScope {
        /** Conservative default: any change to the complete source image changes identity. */
        IMAGE_GENERATION,
        /**
         * Reuse across images only when this exact file/range and all declared
         * dependencies remain equal. The caller must supply a complete dependency
         * root, including any transitive or external state consulted by analysis.
         */
        FILE_DEPENDENCIES
    }

    /**
     * Content identities of the complete analysis contract and its environment.
     * Analyzer includes parser/compiler versions; configuration includes language
     * mode and flags; dependencies include classpath/module and dependency roots.
     * Recipe identifies its complete versioned transformation or prompt contract.
     * A digest of an explicit empty manifest represents an absent dimension.
     */
    public record SourceContext(String analyzerSha256, String recipeSha256,
                                String dependenciesSha256, String configurationSha256,
                                String resultSchemaSha256) {
        public SourceContext {
            PmemBlockStore.parseHash(analyzerSha256);
            PmemBlockStore.parseHash(recipeSha256);
            PmemBlockStore.parseHash(dependenciesSha256);
            PmemBlockStore.parseHash(configurationSha256);
            PmemBlockStore.parseHash(resultSchemaSha256);
        }
    }

    private PmemMemoKey(byte[] ownedCanonical) {
        canonical = ownedCanonical;
        sha256 = PmemBlockStore.sha256(canonical);
    }

    /**
     * Encode up to 64 named dimensions. Names are 1..128 UTF-8 bytes; values
     * are 0..4096 bytes. The operation is 1..256 bytes. The complete encoding
     * cannot exceed 64 KiB. The caller must not mutate the map during this call.
     */
    public static PmemMemoKey of(String operation, Map<String, String> dimensions) {
        byte[] operationBytes = utf8(operation, 256, false);
        Objects.requireNonNull(dimensions, "dimensions");
        if (dimensions.size() > MAX_DIMENSIONS)
            throw new IllegalArgumentException("too many memo dimensions");
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : dimensions.entrySet()) {
            String name = Objects.requireNonNull(entry.getKey(), "dimension name");
            String value = Objects.requireNonNull(entry.getValue(), "dimension value");
            utf8(name, 128, false);
            utf8(value, 4096, true);
            if (sorted.put(name, value) != null || sorted.size() > MAX_DIMENSIONS)
                throw new IllegalArgumentException("duplicate or excessive memo dimensions");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.write(DOMAIN);
            field(output, operationBytes);
            output.writeInt(sorted.size());
            for (Map.Entry<String, String> entry : sorted.entrySet()) {
                byte[] name = utf8(entry.getKey(), 128, false);
                byte[] value = utf8(entry.getValue(), 4096, true);
                if ((long) bytes.size() + 8 + name.length + value.length > MAX_CANONICAL_BYTES)
                    throw new IllegalArgumentException("canonical memo key exceeds byte budget");
                field(output, name);
                field(output, value);
            }
        } catch (IOException impossible) {
            throw new AssertionError("in-memory canonical encoding failed", impossible);
        }
        return new PmemMemoKey(bytes.toByteArray());
    }

    /** Identity for a complete file in an existing immutable source image. */
    public static PmemMemoKey source(PmemSourceImage image, int fileIndex, SourceContext context) {
        return source(image, fileIndex, context, SourceScope.IMAGE_GENERATION);
    }

    /** Whole-file identity with an explicit, caller-established validity boundary. */
    public static PmemMemoKey source(PmemSourceImage image, int fileIndex,
                                    SourceContext context, SourceScope scope) {
        Objects.requireNonNull(image, "image");
        synchronized (image) {
            return source(image, fileIndex, 0, image.lengthAt(fileIndex), context, scope);
        }
    }

    /**
     * Identity for an exact file/atom range. The image root conservatively binds
     * the complete source generation; a change elsewhere in that generation
     * invalidates this key too. The full file digest plus range binds atom bytes.
     * This does not parse again or re-read the source. The source image's existing
     * immutability and verification contract remains the caller's responsibility.
     */
    public static PmemMemoKey source(PmemSourceImage image, int fileIndex,
                                    long firstByte, int byteCount, SourceContext context) {
        return source(image, fileIndex, firstByte, byteCount, context,
            SourceScope.IMAGE_GENERATION);
    }

    /**
     * Exact range identity with an explicit scope. FILE_DEPENDENCIES deliberately
     * omits the complete image root and uses a different operation domain. Its
     * caller must establish that the supplied dependency root contains every
     * other input consulted by the analysis. This method cannot infer dependency
     * completeness and grants no validity from a digest alone.
     */
    public static PmemMemoKey source(PmemSourceImage image, int fileIndex, long firstByte,
                                    int byteCount, SourceContext context, SourceScope scope) {
        Objects.requireNonNull(image, "image");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(scope, "scope");
        synchronized (image) {
            int length = image.lengthAt(fileIndex);
            if (firstByte < 0 || byteCount < 0 || firstByte > length
                    || byteCount > length - firstByte)
                throw new IndexOutOfBoundsException("memo atom range exceeds source file");
            Map<String, String> dimensions = new TreeMap<>();
            if (scope == SourceScope.IMAGE_GENERATION)
                dimensions.put("source.root", image.sourceRoot());
            dimensions.put("source.path", image.relativePathAt(fileIndex));
            dimensions.put("source.sha256", image.sha256At(fileIndex));
            dimensions.put("source.firstByte", Long.toString(firstByte));
            dimensions.put("source.byteCount", Integer.toString(byteCount));
            dimensions.put("analysis.sha256", context.analyzerSha256());
            dimensions.put("recipe.sha256", context.recipeSha256());
            dimensions.put("dependencies.sha256", context.dependenciesSha256());
            dimensions.put("configuration.sha256", context.configurationSha256());
            dimensions.put("resultSchema.sha256", context.resultSchemaSha256());
            String operation = scope == SourceScope.IMAGE_GENERATION
                    ? "source-analysis-v1" : "source-analysis-file-dependencies-v1";
            return of(operation, dimensions);
        }
    }

    public String sha256() { return sha256; }
    public int canonicalLength() { return canonical.length; }

    /** A detached copy; modifying it cannot change this key. */
    public byte[] canonicalBytes() { return canonical.clone(); }

    @Override public String toString() { return sha256; }
    @Override public int hashCode() { return Arrays.hashCode(canonical); }
    @Override public boolean equals(Object other) {
        return other instanceof PmemMemoKey key && Arrays.equals(canonical, key.canonical);
    }

    static byte[] utf8(String value, int maximum, boolean emptyAllowed) {
        Objects.requireNonNull(value, "memo text");
        if ((!emptyAllowed && value.isEmpty()) || value.length() > maximum)
            throw new IllegalArgumentException("memo text is empty or exceeds byte budget");
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
            if (encoded.remaining() > maximum)
                throw new IllegalArgumentException("memo UTF-8 text exceeds byte budget");
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException malformed) {
            throw new IllegalArgumentException("malformed Unicode memo text", malformed);
        }
    }

    private static void field(DataOutputStream output, byte[] bytes) throws IOException {
        output.writeInt(bytes.length);
        output.write(bytes);
    }
}
