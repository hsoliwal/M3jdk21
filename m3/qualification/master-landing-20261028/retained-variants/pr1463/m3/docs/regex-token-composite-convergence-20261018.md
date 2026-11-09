<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 lexical and composite RX7 convergence

This successor of PR #1338 extends the existing `jdk.internal.mindex.M3RX7`
runtime owner and existing collection views. Canonical recipes, preimages,
postimages, the donor catalogue and detailed receipts live in
`hsoliwal/com.synexia`, under
`synexia-openrewrite-recipes/recipes/m3jdk21-token-composite-convergence-20261018`.
The exact receiver parent is `3e1a0b7b7d2498a62550861a6a5a9bd88a2314d7`;
the canonical parent is `710588f0ca8ad1c2b0d6d9536f16aa881078b647`.
The accompanying generated replay receipt binds this handoff to its source seals.

## Lexical tokens and physical partitions

`M3RX7.fromLexicalNfa` receives an already-compiled graph in primitive CSR arrays.
Consuming edges test token kind, exact canonical spelling ID and required/forbidden
32-bit flags. Epsilon edges express concatenation, alternatives and repetition
through graph topology. The plan snapshots its inputs and stores offsets, edge
lanes and closure masks. It retains no input token objects or text payload.

`LexicalPlan.matches` supports full sequences and half-open token regions.
`initialStates`, `advance` and `accepts` preserve the NFA frontier across token
partitions. `find` uses an explicit **leftmost-longest** token contract and returns
a packed start/end interval; `utf16Range` maps validated nonoverlapping token
coordinates back to the original UTF-16 source. Compiler and caller must share
the same token schema and canonical dictionary namespace/generation. A spelling
ID is an identity, not a possibly colliding hash.

The existing character-DFA `TokenIndex` treats physical chunks/token spellings as
storage partitions. Its language and `allowedMaskWord` remain unchanged. A new
`VocabularyFilter` computes exact reachability using only the available vocabulary.
`TokenIndex.transfer` summarizes a token sequence with one short per DFA state;
`Transfer.then` composes two summaries and `repeated` uses exponentiation without
expanding the sequence. Composition requires the same original plan identity.
Transfer objects share the immutable accepting-state snapshot and do not retain
the original ASCII DFA table or source spellings.

Existing `String`, `Pattern`, `Matcher`, M3TQ, M3RegexDpPlan and the v7 image codec
retain their contracts. Lexical leftmost-longest search does not claim Java
Matcher's ordered alternation, captures or greediness. Attributed compiler and
recipe verification still control source edits; a lexical span alone does not
authorize renaming a symbol.

## JNI and resource selection

The existing `libjava/M3RX7.c` now also implements bounded primitive composition
and repetition. The native boundary validates dimensions, states, counts and
output aliasing before writes. Host tests compile the declarations with
`javac -h`, compile C17, verify symbols, and execute direct JNI parity under
`-Xcheck:jni`.

Measured JNI composition/repetition was slower than Java for the tested supported
state counts, so Java remains the production default for these new operations.
The native kernels are retained for later platform qualification. Existing
packed-token precomputation dispatch remains unchanged. Prepared structures use
primitive arrays, offsets and masks; they do not add a retained object per token
or per collection entry.

## Filtered sorted and navigable views

The existing `M3FilteredViews` now handles a comparator that places `null` between
non-null members. Lower/floor/ceiling/higher use bounded owner cursors to distinguish
a null member from the API's no-result sentinel, including nested and descending
views. Natural and reverse-natural order retain the cheap absent-result path.
Custom comparators may see additional bounded comparisons during disambiguation;
predicate and entry callback compatibility are tested separately.

Comparator-equivalent entry membership is a recorded **HOLD** counterexample.
Changing the existing equals-based entry scan would alter predicate, entry and
value-equality callbacks. This contract-preserving pass keeps that behavior and
the public collection API descriptors unchanged. No phase in the existing
String -> arrays -> collections -> AST/compiler receiving sequence is advanced.

## Reproduce receiver qualification

Use the same Java 21 toolchain for `JAVA_HOME`, `M3_JAVA_HOME` and `PATH`, then run:

```sh
bash m3/runtime-integration/check-m3rx7-convergence.sh
```

The command runs the repaired existing receiver/codec gates, new lexical and
transfer contracts, existing token-index tests, filtered collection contracts,
native boundary qualification, and full-borrow custody verification. Its result
is a host component proof. Source-built M3JDK21, actual jtreg/libjava/HotSpot,
JIT/GC/CDS, retained-heap/native pressure, other platforms and the full canonical
Synexia source SDK/reactor remain separate acceptance gates.
