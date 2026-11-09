<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3Lexicons: checked and composable word-transfer precomputation

This pass extends the **existing** `com.m3.lexicon.M3RegexWordTransferRows` owner.
M3JDK21 owns the receiving implementation. Synexia owns the reproducible source recipe;
there is no new Synexia runtime dependency and no replacement string/lexicon identity.

## Implemented

- Check CWID and state separately before flattening coordinates. Invalid states could
  previously alias another row; an out-of-vocabulary accept lookup could access padding.
  Integer multiplication overflow could also turn an invalid CWID into a valid array slot.
- Reject non-positive DFA state counts and out-of-range transitions before accepting
  their results. Valid UTF-16 spelling behavior is unchanged, including NUL, unpaired
  surrogates, case, combining sequences, spaces, Hindi and CJK test data.
- Add `read(image, expectedGeneration64, expectedPatternKey)` to bind intake to all 64
  generation bits and all 32 pattern-key bytes, in addition to geometry/digest/state
  checks. The existing unbound reader is preserved, not silently promoted to bound.
- Add `summarize(cwids)` and its range overload, `Summary.then(right)` and
  `Summary.repeat(copies)`. Each retains one `int[Q]` transition lane and a packed
  acceptance bitset, not strings, token lists or per-word entry objects.
- Add a range overload of `run`. Every public state lookup is checked, including an
  empty range; CWID 0/OOV is never silently skipped.

## Composition law

For segment A: exit function fA(q) and accepting-inside predicate aA(q).
For A followed by B:

    fAB(q) = fB(fA(q))
    aAB(q) = aA(q) OR aB(fA(q))

The identity is f(q)=q and a(q)=false. This operation is associative, not commutative.
Every summary is tied to the exact table instance; equal dimensions, equal truncated
atom tags or equal numeric CWIDs do not authorize combining different owners.

Construction from N tokens costs O(N*Q) table work. Joining summaries costs O(Q).
Binary powering costs O(Q*log(copies)) work without expanding the token sequence;
a query of an already-built summary is one transition lookup/bit test. These are
algorithmic bounds, not measured end-to-end throughput gains. Summary retention pins
its table owner. The original vocabulary*Q table construction/storage cost remains.
Use run for one-off single-state scans; precompute summaries for repeated/composite use.

## Example

```java
M3RegexWordTransferRows rows = M3RegexWordTransferRows.read(
        image, expectedGeneration64, expectedPatternKey);
var prefix = rows.summarize(cwids, 0, split);
var suffix = rows.summarize(cwids, split, cwids.length);
var composed = prefix.then(suffix);
int exitState = composed.step(startState);
boolean acceptedInside = composed.acceptsInside(startState);
var repeated = composed.repeat(1_000_000L);
```

No spelling/DFA callback is used by composition, repetition or lookup after the table
is built. Caller token lanes may be changed after summarize returns. No array escapes.
Concurrent calls are read-only; input lanes and DFA/spellings must be stable during
construction. Input image bytes must not change during read, including via aliases
of a read-only ByteBuffer. Position/mark/byte order are preserved; admitted tables are
independent of subsequent buffer writes. A read-only view itself is not immutability.

## Boundaries that must not be blurred

Generation64 and the pattern key are caller-owned labels. This does not prove the
relationship between a key and its automaton, prevent all generation collisions, or
provide a signature. Use admitted immutable source manifests upstream. The historical
128-byte header and valid M3RXW001 byte spelling are unchanged.

The transfer covers the **concatenation of the supplied words**. Input tokens must
include all separators/unknown spans needed to represent the intended text. A word-only
separator projection is not automatically equivalent to the original full text. Keep
upstream exact fallback for OOVs rather than dropping them. This pass does not wire
M3WordIdSequence into java.lang.String or invent a full regex engine.

Acceptance is reached after consuming at least one UTF-16 unit. Initial/zero-width
acceptance is deliberately not invented; empty words/ranges contribute no inside bit.
Captures, backreferences, lookarounds, Unicode grapheme segmentation, general Pattern
compatibility and R6 automaton serialization are not established by this table algebra.

## Run

JDK 21+ and Python 3 (Windows/Linux, no WSL requirement):

    python m3/lexicon/receiver/tools/verify-transfer.py --output transfer-proof --interpreter

The output directory must be new. This compiles the four actual owner/dependency files
and two direct Java proofs; compares 256 valid-image/direct-state observations against
an immutable baseline hash; then runs the composed, identity, repetition, wide-bit-lane,
permission-free binding, ownership and concurrency controls.

The existing receiver Maven verify retains its original execution and recipe-boot
profile, and adds both new proof mains:

    mvn -f m3/lexicon/receiver/pom.xml verify

Maven and the full receiver suite were not executed in this environment. The focused
Java proof is not a full M3JDK image/jtreg, JNI, multilingual corpus or performance gate.
No new dictionary/corpus content, model weights, donor libraries or native ABI is added.
