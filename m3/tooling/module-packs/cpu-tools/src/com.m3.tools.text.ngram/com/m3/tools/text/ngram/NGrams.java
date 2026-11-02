// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK21 module name.
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.text.ngram;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
/** Deterministic UTF-16 n-gram frequency; exact corpus fact, not fuzzy proof. */
public final class NGrams {
    private NGrams() { }
    /** A source-owned primitive positional index; the legacy count map remains unchanged. */
    public static NGramIndex prepare(String text, int n) { return NGramIndex.prepare(text, n); }

    /** Identical indexed semantics with cooperative progress/cancellation checkpoints. */
    public static NGramIndex prepare(String text, int n, Runnable checkpoint) {
        return NGramIndex.prepare(text, n, checkpoint);
    }

    /** Exact UTF-16 multiset n-gram overlap; no hash-only equality inference. */
    public static NGramIndex.Overlap overlap(NGramIndex left, NGramIndex right) {
        return NGramIndex.overlap(left, right);
    }
    public static Map<String,Integer> count(String text, int n) {
        Objects.requireNonNull(text);
        if (n < 1 || n > 64) throw new IllegalArgumentException("n outside 1..64");
        Map<String,Integer> result = new LinkedHashMap<>();
        for(int i=0;i<=text.length()-n;i++) result.merge(text.substring(i,i+n),1,Integer::sum);
        return Map.copyOf(result);
    }
    public static void main(String[] args) {
        if(args.length==1&&args[0].equals("--self-test")){
            if(!count("banana",2).get("an").equals(2)) throw new AssertionError("gram count");
            if(!count("",3).isEmpty()) throw new AssertionError("empty");
            NGramIndex indexed = prepare("banana",2);
            if(indexed.frequency("an") != 2 || !java.util.Arrays.equals(indexed.positions("an"), new int[]{1,3}))
                throw new AssertionError("indexed gram offsets");
            System.out.println("M3_JMOD_TEST_PASS text.ngram");
        } else if(args.length>=2) System.out.println(count(args[0],Integer.parseInt(args[1])));
        else throw new IllegalArgumentException("text and gram length required");
    }
}
