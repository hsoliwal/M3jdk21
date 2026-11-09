<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File source delta contract

The existing immutable `jdk.m3.parser.M3File` remains the sole source-text owner.
This additive pass compares two retained snapshots without reading, copying,
decoding, tokenizing or parsing their text again.

`current.changesFrom(previous)` reports URI equality, a conservative changed
UTF-16 window (`start`, `previousEnd`, `currentEnd`) and independent original-byte
seal disposition. Its maximal common prefix and non-overlapping suffix reconstruct
the new text exactly. Equal character content has the canonical empty window
`[0,0)` in both snapshots. Coordinates are code units, including unpaired
surrogates; a grammar may separately widen the invalidation region.

Byte seals are `UNKNOWN` if either snapshot has no original-byte identity,
`MATCHING_SEALS` when both SHA-256 identities agree, and `DIFFERENT_SEALS`
otherwise. Missing seals never mean equal bytes. URI differences remain visible
even when text is unchanged. Both snapshots retain their original lifetime and
payload; the returned value is an API projection rather than retained node storage.

This is change evidence, not syntax/ABI equivalence or permission to skip work.
Reusing attributed facts requires the existing complete `M3FileSourceFacts`
context, provider, schema and phase identity. A changed quote, comment boundary
or Unicode escape may affect syntax beyond the returned character range.

Existing APIs, hashes, decoding, limits, exception contracts and module identity
stay unchanged. The separate raw NIO M3File owner handles `.class` bytes.
Generations, persistence, atomic publication and eviction remain owned by the
existing workspace/cache/M3SnapshotStore implementations.

Proof: exact example windows; Unicode/opaque-code carriers; URI/encoding/unknown
byte distinctions; bounded generated reconstruction and reverse comparisons;
null/bounds refusal; strict Java 21 compilation and existing parser contracts.
Full Maven/Gradle scheduling integration and comparative performance remain
separate gates.
