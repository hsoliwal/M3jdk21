// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.lexicon;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongConsumer;

/** Immutable paragraph leaf containing token IDs only. */
//FLLDCIM: RELOCATE - donor blob b13b076cb382 at commit 3385d79a2968 (repository, path, URL and digests: m3/lexicon/recipe/interned-language-20261106/PROVENANCE.tsv); recipe m3/lexicon/recipe/interned-language-20261106/relocate.py v1
public final class M3PackedText {
    private final long[] tokens;
    private final int hash;

    public M3PackedText(long[] tokens) {
        this.tokens = Objects.requireNonNull(tokens, "tokens").clone();
        this.hash = Arrays.hashCode(this.tokens);
    }

    public int size() { return tokens.length; }

    public long token(int index) {
        return tokens[index];
    }

    public long[] toArray() {
        return tokens.clone();
    }

    public void forEach(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (long token : tokens) consumer.accept(token);
    }

    @Override public boolean equals(Object other) {
        return other instanceof M3PackedText text && Arrays.equals(tokens, text.tokens);
    }

    @Override public int hashCode() {
        return hash;
    }
}
