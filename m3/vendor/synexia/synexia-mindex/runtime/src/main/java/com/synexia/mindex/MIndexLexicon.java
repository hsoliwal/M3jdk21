// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/** Common read-only contract for heap and memory-mapped dictionaries. */
public interface MIndexLexicon {
    int dictionaryVersion();
    int languageId();
    String languageTag();
    DictionaryProvenance provenance();
    int size();
    LexicalRef spaceRef();
    Optional<LexicalRef> lookup(String surface);

    /**
     * Exact UTF-8 slice lookup. Arena-backed implementations override this to avoid creating a
     * temporary String; the default preserves compatibility for third-party lexicons.
     */
    default Optional<LexicalRef> lookupUtf8(byte[] utf8, int offset, int length) {
        Objects.requireNonNull(utf8, "utf8");
        Objects.checkFromIndexSize(offset, length, utf8.length);
        return lookup(new String(utf8, offset, length, StandardCharsets.UTF_8));
    }

    /**
     * Primitive UTF-8 lookup for hot parser/compiler paths.
     *
     * <p>Returns the local lexical rank or {@code -1}. Arena-backed implementations override this
     * method so successful lookup does not allocate Optional or LexicalRef wrappers.</p>
     */
    default int lookupRankUtf8(byte[] utf8, int offset, int length) {
        return lookupUtf8(utf8, offset, length)
                .map(ref -> Math.toIntExact(ref.lexicalRank()))
                .orElse(-1);
    }

    /**
     * Primitive lookup using a caller-supplied canonical MIndex content hash.
     *
     * <p>The compatibility default ignores the hash. Frozen MIndex dictionaries override this to
     * avoid hashing the same token twice when a scanner already retained the hash.</p>
     */
    default int lookupRankUtf8Prehashed(
            byte[] utf8, int offset, int length, long contentHash64) {
        return lookupRankUtf8(utf8, offset, length);
    }

    LexicalRef ref(int rank);
    String materialize(LexicalRef ref);

    /**
     * Read-only UTF-8 view of one canonical spelling.
     *
     * <p>Implementations backed by immutable heap or mapped arenas should override this method to
     * return a zero-copy slice. The returned buffer must never permit mutation of dictionary
     * storage.
     */
    default ByteBuffer utf8View(LexicalRef ref) {
        return ByteBuffer.wrap(materialize(ref).getBytes(StandardCharsets.UTF_8))
                .asReadOnlyBuffer();
    }

    long frequency(LexicalRef ref);
    SemanticRef semantic(LexicalRef ref);

    /** Exact UTF-16 code-unit length frozen with the lexical coordinate. */
    default int utf16Length(LexicalRef ref) {
        return materialize(ref).length();
    }

    /** Exact Unicode code-point length frozen with the lexical coordinate. */
    default int codePointLength(LexicalRef ref) {
        String value = materialize(ref);
        return value.codePointCount(0, value.length());
    }

    /** Exact java.lang.String hashCode for the frozen spelling. */
    default int javaHashCode(LexicalRef ref) {
        return materialize(ref).hashCode();
    }

    /** 31^utf16Length with Java int overflow semantics, for exact hash composition. */
    default int javaHashMultiplier(LexicalRef ref) {
        return TextMetadata.pow31(utf16Length(ref));
    }

    /**
     * Half-open coordinate interval containing every lexeme whose spelling starts with prefix.
     *
     * <p>The coordinate belongs to the lexicon's frozen forward-spelling order and need not equal
     * the lexical rank (for example when rank 0 is reserved for SPACE).
     */
    default LexicalRange prefixRange(String prefix) {
        throw new UnsupportedOperationException("prefix range is not indexed by this lexicon");
    }

    /** Half-open coordinate interval in the frozen reverse-spelling ordering. */
    default LexicalRange suffixRange(String suffix) {
        throw new UnsupportedOperationException("suffix range is not indexed by this lexicon");
    }

    default LexicalRef prefixRefAt(int prefixCoordinate) {
        return ref(prefixCoordinate);
    }

    default LexicalRef suffixRefAt(int suffixCoordinate) {
        throw new UnsupportedOperationException("suffix coordinate is not indexed by this lexicon");
    }

    /**
     * Coordinate of an exact ref in the frozen forward-prefix ordering.
     *
     * <p>Format-4 dictionaries override this with a single primitive-lane read. The default keeps
     * third-party and legacy lexicons source-compatible.
     */
    default int prefixCoordinate(LexicalRef ref) {
        requireLocal(ref);
        for (int coordinate = 0; coordinate < size(); coordinate++) {
            if (prefixRefAt(coordinate).equals(ref)) return coordinate;
        }
        throw new IllegalStateException("local ref missing from prefix coordinates: " + ref);
    }

    /** Coordinate of an exact ref in the frozen reverse-spelling/suffix ordering. */
    default int suffixCoordinate(LexicalRef ref) {
        requireLocal(ref);
        for (int coordinate = 0; coordinate < size(); coordinate++) {
            if (suffixRefAt(coordinate).equals(ref)) return coordinate;
        }
        throw new IllegalStateException("local ref missing from suffix coordinates: " + ref);
    }

    /** Frozen forward-prefix interval for an exact dictionary ref. */
    default LexicalRange prefixRange(LexicalRef prefix) {
        requireLocal(prefix);
        return prefixRange(materialize(prefix));
    }

    /** Frozen reverse-spelling/suffix interval for an exact dictionary ref. */
    default LexicalRange suffixRange(LexicalRef suffix) {
        requireLocal(suffix);
        return suffixRange(materialize(suffix));
    }

    /**
     * Exact immutable-dictionary prefix relation.
     *
     * <p>Format-4 dictionaries answer this with primitive coordinate/range reads only; no String,
     * UTF-8 conversion, hashing, binary search or allocation is required.
     */
    default boolean startsWith(LexicalRef value, LexicalRef prefix) {
        requireLocal(value);
        requireLocal(prefix);
        return prefixRange(prefix).containsCoordinate(prefixCoordinate(value));
    }

    /** Exact immutable-dictionary suffix relation with the same format-4 fast path. */
    default boolean endsWith(LexicalRef value, LexicalRef suffix) {
        requireLocal(value);
        requireLocal(suffix);
        return suffixRange(suffix).containsCoordinate(suffixCoordinate(value));
    }

    default int compare(LexicalRef left, LexicalRef right) {
        requireLocal(left);
        requireLocal(right);
        return Long.compareUnsigned(left.lexicalRank(), right.lexicalRank());
    }

    default void requireLocal(LexicalRef ref) {
        if (ref == null
                || ref.dictionaryVersion() != dictionaryVersion()
                || ref.languageId() != languageId()
                || ref.lexicalRank() >= size()) {
            throw new IllegalArgumentException("LexicalRef does not belong to this dictionary: " + ref);
        }
    }
}
