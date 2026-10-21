// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

/**
 * Packed semantic coordinate: upper 48 bits are a language-independent concept
 * identity and lower 16 bits are fast feature flags.
 */
public record SemanticRef(long bits) implements MIndexSymbolReference {
    public static final long MAX_CONCEPT_ID = (1L << 48) - 1L;
    private static final long FLAG_MASK = 0xffffL;

    public static final int NOUN = 1 << 0;
    public static final int VERB = 1 << 1;
    public static final int ADJECTIVE = 1 << 2;
    public static final int ADVERB = 1 << 3;
    public static final int PROPER_NOUN = 1 << 4;
    public static final int SINGULAR = 1 << 5;
    public static final int PLURAL = 1 << 6;
    public static final int HUMAN = 1 << 7;
    public static final int ANIMATE = 1 << 8;
    public static final int NAMED_ENTITY = 1 << 9;
    public static final int LEMMA = 1 << 10;
    public static final int INFLECTED = 1 << 11;

    public static final SemanticRef NONE = new SemanticRef(0L);

    public static SemanticRef of(long conceptId, int flags) {
        if (conceptId < 0 || conceptId > MAX_CONCEPT_ID) {
            throw new IllegalArgumentException("conceptId must fit 48 unsigned bits");
        }
        return new SemanticRef((conceptId << 16) | (flags & FLAG_MASK));
    }

    public long conceptId() {
        return bits >>> 16;
    }

    public int flags() {
        return (int) (bits & FLAG_MASK);
    }

    @Override
    public MIndexSymbolRef symbolRef() {
        return new MIndexSymbolRef(
                MIndexReferenceDomain.SEMANTIC,
                Integer.toUnsignedLong(flags()),
                conceptId());
    }

    public boolean has(int mask) {
        return (flags() & mask) == mask;
    }

    public boolean sameConcept(SemanticRef other) {
        return other != null && conceptId() == other.conceptId();
    }

    public SemanticRef withFlags(int moreFlags) {
        return of(conceptId(), flags() | moreFlags);
    }
}
