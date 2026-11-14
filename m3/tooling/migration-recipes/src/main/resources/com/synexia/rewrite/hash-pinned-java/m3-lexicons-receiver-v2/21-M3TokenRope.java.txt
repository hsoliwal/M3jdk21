// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.lexicon;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Immutable token DAG. Concatenation is O(1); flattening/materialization is deferred to boundaries.
 */
//FLLDCIM: RELOCATE - donor blob a9846f166598 at commit 3385d79a2968 (repository, path, URL and digests: m3/lexicon/recipe/interned-language-20261106/PROVENANCE.tsv); recipe m3/lexicon/recipe/interned-language-20261106/relocate.py v1
public sealed interface M3TokenRope permits M3TokenRope.Leaf, M3TokenRope.Concat {
    long tokenCount();
    long fingerprint();

    static M3TokenRope leaf(M3PackedText text) {
        return new Leaf(text);
    }

    static M3TokenRope concat(M3TokenRope left, M3TokenRope right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return new Concat(
            left, right,
            Math.addExact(left.tokenCount(), right.tokenCount()),
            mix(left.fingerprint(), right.fingerprint(), right.tokenCount()));
    }

    default void forEach(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        ArrayDeque<M3TokenRope> stack = new ArrayDeque<>();
        stack.push(this);
        while (!stack.isEmpty()) {
            M3TokenRope node = stack.pop();
            if (node instanceof Leaf leaf) {
                leaf.text.forEach(consumer);
            } else {
                Concat concat = (Concat) node;
                stack.push(concat.right);
                stack.push(concat.left);
            }
        }
    }

    record Leaf(M3PackedText text) implements M3TokenRope {
        public Leaf {
            Objects.requireNonNull(text, "text");
        }
        @Override public long tokenCount() { return text.size(); }
        @Override public long fingerprint() {
            long h = 0xcbf29ce484222325L;
            for (long token : text.toArray()) {
                h ^= token;
                h *= 0x100000001b3L;
            }
            return h;
        }
    }

    record Concat(
            M3TokenRope left, M3TokenRope right, long tokenCount, long fingerprint) implements M3TokenRope {
        public Concat {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
        }
    }

    private static long mix(long left, long right, long rightLength) {
        long h = left ^ Long.rotateLeft(right, 23) ^ rightLength * 0x9e3779b97f4a7c15L;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        return h ^ (h >>> 33);
    }
}
