# M3 String token-aware regex — 2026-10-20

## Product boundary

This work belongs to **M3JDK21**. Synexia is provenance/donor evidence only; there is no Synexia
runtime dependency.

## Problem shape

The problem is **regular-language DFA transition composition over canonical token/leaf chunks**.
It is not tokenization of regex syntax and not a second regex language.

M3 String already stores text as immutable canonical scalar owners/ranges composed through tuple
owners. M3RX7 already stores a bounded capture-free ASCII DFA and can precompute the state transfer
of an arbitrary ASCII token slice. The missing atom was the bridge between those two existing
owners.

## Selected representation

For one M3-backed String:

1. walk existing M3 scalar owner/range coordinates with the existing LeafCursor;
2. split only when a visible scalar range exceeds the already-owned M3RX7 token-length budget;
3. deduplicate repeated exact owner/range coordinates with primitive open addressing;
4. copy only distinct visible ASCII token spellings into an invocation-owned packed precompute image;
5. retain an int[] sequence of vocabulary rows;
6. use M3RX7.TokenIndex to compose DFA transitions over the sequence.

The M3 String itself retains no token arrays, regex plan, Entry objects or copied text.

## Semantic law

For DFA transition function delta, token transition T(s, token) is the fold of delta over the
token's characters. Therefore T(T(s, A), B) equals the character fold over A+B. Changing M3
tuple/atom segmentation cannot change full-region DFA truth.

Non-ASCII input, non-M3-backed String, excessive budgets, or unavailable optional metadata return
unsupported and preserve the existing character/JDK Pattern path.

## JNI

The existing java.base JNI symbol `M3RX7.buildPacked0` remains the native transition-row builder.
No second native library or JNI registry is introduced. The jtreg proof includes a 128-state x
512-token image (65,536 cells), crossing the existing native threshold when the symbol is available;
the Java fallback remains exact for older/missing binaries.

## Contract

- no public String/Pattern/Matcher API change;
- JDK Pattern remains regex authority;
- M3RX7 token candidate disagreement is an invariant failure;
- no whole-String flattening;
- no per-value retained regex metadata;
- bounded arrays/offsets/indexes only;
- forward/inverse recipe is hash-pinned and fixed-point.

## Provenance

Synexia comparison revision: `f04fbac91a7891e64b77c7b24cef1ab75ec88144`.

Forward recipe: `m3-string-token-aware-regex-20261020`.

Inverse recipe: `m3-string-token-aware-regex-20261020-inverse`.
