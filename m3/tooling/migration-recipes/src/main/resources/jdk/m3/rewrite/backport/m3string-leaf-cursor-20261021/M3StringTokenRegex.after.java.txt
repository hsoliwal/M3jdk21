/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.Objects;

/**
 * Exact regular-expression algebra over canonical M3String leaf-token occurrences.
 *
 * <p>This is deliberately distinct from character regex. A token is one visible scalar
 * owner/range in the persistent M3String DAG; no spelling is copied and tuple nodes are traversal
 * structure, not regex symbols. Character regex remains owned by Pattern/M3StringRegexDp.</p>
 *
 * <p>The compiled shape is structure-of-arrays: token kind, quantifier, owner, coordinate and
 * representation-independent (String.hashCode,length) fingerprint. Patterns contain at most 62
 * atoms so the active NFA frontier is one long mask. JNI may reject a match from fingerprints
 * exactly; a native positive is always exact-verified against canonical owner/range text.</p>
 */
final class M3StringTokenRegex {
    static final int MAX_ATOMS = 62;
    private static final int NATIVE_MIN_TOKENS = 64;
    private static final byte EXACT = 0;
    private static final byte ANY = 1;

    enum Quantifier {
        ONE,
        ZERO_OR_MORE,
        ONE_OR_MORE,
        OPTIONAL
    }

    private final byte[] kinds;
    private final byte[] quantifiers;
    private final M3StringOwner[] owners;
    private final long[] coordinates;
    private final long[] fingerprints;
    private final long startState;
    private final long acceptBit;
    private static volatile boolean nativeUnavailable;

    private M3StringTokenRegex(
            byte[] kinds,
            byte[] quantifiers,
            M3StringOwner[] owners,
            long[] coordinates,
            long[] fingerprints) {
        this.kinds = kinds;
        this.quantifiers = quantifiers;
        this.owners = owners;
        this.coordinates = coordinates;
        this.fingerprints = fingerprints;
        this.acceptBit = 1L << kinds.length;
        this.startState = close(1L);
    }

    static Builder builder() {
        return new Builder();
    }

    int atomCount() {
        return kinds.length;
    }

    boolean matches(M3String source) {
        TokenSequence tokens = TokenSequence.capture(Objects.requireNonNull(source, "source"));
        if (!nativeUnavailable && tokens.size >= NATIVE_MIN_TOKENS) {
            try {
                int candidate =
                        matches0(tokens.fingerprints, fingerprints, kinds, quantifiers);
                if (candidate == 0) return false;
                if (candidate != 1) throw new InternalError("invalid M3 token regex native result");
            } catch (UnsatisfiedLinkError unavailable) {
                nativeUnavailable = true;
            }
        }
        try {
            return matchesExact(tokens);
        } finally {
            Reference.reachabilityFence(source);
        }
    }

    boolean lookingAt(M3String source) {
        TokenSequence tokens = TokenSequence.capture(Objects.requireNonNull(source, "source"));
        try {
            long state = startState;
            if ((state & acceptBit) != 0L) return true;
            for (int token = 0; token < tokens.size; token++) {
                state = advance(state, tokens, token);
                if ((state & acceptBit) != 0L) return true;
                if (state == 0L) return false;
            }
            return false;
        } finally {
            Reference.reachabilityFence(source);
        }
    }

    boolean find(M3String source) {
        TokenSequence tokens = TokenSequence.capture(Objects.requireNonNull(source, "source"));
        try {
            long state = startState;
            if ((state & acceptBit) != 0L) return true;
            for (int token = 0; token < tokens.size; token++) {
                state = close(state | 1L);
                state = advance(state, tokens, token);
                if ((state & acceptBit) != 0L) return true;
            }
            return false;
        } finally {
            Reference.reachabilityFence(source);
        }
    }

    private boolean matchesExact(TokenSequence tokens) {
        long state = startState;
        for (int token = 0; token < tokens.size; token++) {
            state = advance(state, tokens, token);
            if (state == 0L) return false;
        }
        return (state & acceptBit) != 0L;
    }

    private long advance(long state, TokenSequence source, int sourceToken) {
        long next = 0L;
        long remaining = state & (acceptBit - 1L);
        while (remaining != 0L) {
            int atom = Long.numberOfTrailingZeros(remaining);
            remaining &= remaining - 1L;
            if (!accepts(atom, source, sourceToken)) continue;
            switch (Quantifier.values()[quantifiers[atom]]) {
                case ONE, OPTIONAL -> next |= 1L << (atom + 1);
                case ZERO_OR_MORE -> next |= 1L << atom;
                case ONE_OR_MORE -> next |= (1L << atom) | (1L << (atom + 1));
            }
        }
        return close(next);
    }

    private boolean accepts(int atom, TokenSequence source, int sourceToken) {
        if (kinds[atom] == ANY) return true;
        if (fingerprints[atom] != source.fingerprints[sourceToken]) return false;
        return exactTokenEquals(
                owners[atom],
                coordinates[atom],
                source.owners[sourceToken],
                source.coordinates[sourceToken]);
    }

    private long close(long bits) {
        long previous;
        do {
            previous = bits;
            long active = bits & (acceptBit - 1L);
            while (active != 0L) {
                int atom = Long.numberOfTrailingZeros(active);
                active &= active - 1L;
                Quantifier quantifier = Quantifier.values()[quantifiers[atom]];
                if (quantifier == Quantifier.ZERO_OR_MORE || quantifier == Quantifier.OPTIONAL) {
                    bits |= 1L << (atom + 1);
                }
            }
        } while (bits != previous);
        return bits;
    }

    private static boolean exactTokenEquals(
            M3StringOwner leftOwner,
            long leftCoordinate,
            M3StringOwner rightOwner,
            long rightCoordinate) {
        int leftStart = (int) (leftCoordinate >>> Integer.SIZE);
        int leftLength = (int) leftCoordinate;
        int rightStart = (int) (rightCoordinate >>> Integer.SIZE);
        int rightLength = (int) rightCoordinate;
        if (leftLength != rightLength) return false;
        if (leftOwner == rightOwner && leftStart == rightStart) return true;
        if (fingerprint(leftOwner, leftStart, leftLength)
                != fingerprint(rightOwner, rightStart, rightLength)) {
            return false;
        }
        for (int index = 0; index < leftLength; index++) {
            if (leftOwner.charAt(leftStart + index) != rightOwner.charAt(rightStart + index)) {
                return false;
            }
        }
        return true;
    }

    private static long fingerprint(M3StringOwner owner, int start, int length) {
        int hash;
        if (start == 0 && length == owner.length) {
            hash = owner.javaHash;
        } else {
            hash = 0;
            for (int index = 0; index < length; index++) {
                hash = 31 * hash + owner.charAt(start + index);
            }
        }
        return ((long) hash << Integer.SIZE) | Integer.toUnsignedLong(length);
    }

    private static native int matches0(
            long[] sourceFingerprints,
            long[] patternFingerprints,
            byte[] kinds,
            byte[] quantifiers);

    static final class Builder {
        private byte[] kinds = new byte[8];
        private byte[] quantifiers = new byte[8];
        private M3StringOwner[] owners = new M3StringOwner[8];
        private long[] coordinates = new long[8];
        private long[] fingerprints = new long[8];
        private int size;

        Builder exact(M3String token) {
            return exact(token, Quantifier.ONE);
        }

        Builder exact(M3String token, Quantifier quantifier) {
            TokenSequence sequence = TokenSequence.capture(Objects.requireNonNull(token, "token"));
            if (sequence.size != 1) {
                throw new IllegalArgumentException(
                        "exact token atom must contain one canonical M3 leaf token");
            }
            ensureCapacity(size + 1);
            kinds[size] = EXACT;
            quantifiers[size] = (byte) Objects.requireNonNull(quantifier, "quantifier").ordinal();
            owners[size] = sequence.owners[0];
            coordinates[size] = sequence.coordinates[0];
            fingerprints[size] = sequence.fingerprints[0];
            size++;
            return this;
        }

        Builder exactSequence(M3String sequence) {
            TokenSequence tokens = TokenSequence.capture(Objects.requireNonNull(sequence, "sequence"));
            for (int index = 0; index < tokens.size; index++) {
                ensureCapacity(size + 1);
                kinds[size] = EXACT;
                quantifiers[size] = (byte) Quantifier.ONE.ordinal();
                owners[size] = tokens.owners[index];
                coordinates[size] = tokens.coordinates[index];
                fingerprints[size] = tokens.fingerprints[index];
                size++;
            }
            return this;
        }

        Builder any() {
            return any(Quantifier.ONE);
        }

        Builder any(Quantifier quantifier) {
            ensureCapacity(size + 1);
            kinds[size] = ANY;
            quantifiers[size] = (byte) Objects.requireNonNull(quantifier, "quantifier").ordinal();
            owners[size] = null;
            coordinates[size] = 0L;
            fingerprints[size] = 0L;
            size++;
            return this;
        }

        M3StringTokenRegex build() {
            if (size == 0) throw new IllegalStateException("token regex requires at least one atom");
            return new M3StringTokenRegex(
                    Arrays.copyOf(kinds, size),
                    Arrays.copyOf(quantifiers, size),
                    Arrays.copyOf(owners, size),
                    Arrays.copyOf(coordinates, size),
                    Arrays.copyOf(fingerprints, size));
        }

        private void ensureCapacity(int required) {
            if (required > MAX_ATOMS) {
                throw new IllegalArgumentException("M3 token regex exceeds " + MAX_ATOMS + " atoms");
            }
            if (required <= kinds.length) return;
            int capacity = Math.min(MAX_ATOMS, Math.max(required, kinds.length << 1));
            kinds = Arrays.copyOf(kinds, capacity);
            quantifiers = Arrays.copyOf(quantifiers, capacity);
            owners = Arrays.copyOf(owners, capacity);
            coordinates = Arrays.copyOf(coordinates, capacity);
            fingerprints = Arrays.copyOf(fingerprints, capacity);
        }
    }

    private static final class TokenSequence {
        M3StringOwner[] owners = new M3StringOwner[8];
        long[] coordinates = new long[8];
        long[] fingerprints = new long[8];
        int size;

        static TokenSequence capture(M3String source) {
            TokenSequence result = new TokenSequence();
            M3StringLeafCursor cursor = new M3StringLeafCursor(source, false);
            while (cursor.next()) {
                result.add(cursor.owner(), cursor.start(), cursor.length());
            }
            return result;
        }

        private void add(M3StringOwner owner, int start, int length) {
            if (size == owners.length) {
                int capacity = Math.multiplyExact(owners.length, 2);
                owners = Arrays.copyOf(owners, capacity);
                coordinates = Arrays.copyOf(coordinates, capacity);
                fingerprints = Arrays.copyOf(fingerprints, capacity);
            }
            owners[size] = owner;
            coordinates[size] =
                    ((long) start << Integer.SIZE) | Integer.toUnsignedLong(length);
            fingerprints[size] = fingerprint(owner, start, length);
            size++;
        }
    }


}
