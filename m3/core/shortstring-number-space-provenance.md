<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Shortstring reuse in the numeric vocabulary

The existing `com.m3.text.M3NumberSpace` constructor adapts decimal sizing and digit-emission
atoms from [tools4j shortstring](https://github.com/hsoliwal/shortstring/tree/383d2d17d5d1942673976db34cda3695991ca422/src/main/java/org/tools4j/shortstring).
The donor is pinned at `383d2d17d5d1942673976db34cda3695991ca422` and is MIT licensed,
copyright 2023 tools4j.org (Marco Terzer). The complete notice is in `LICENSE-shortstring.md`.

| Donor | Original Git blob | Adaptation |
| --- | --- | --- |
| `StringLengths.java` | `9549a7a5558f0f8b81d2f8978fdb13244a6f3b8a` | Five precomputed decimal maxima for the closed 0..10000 domain. |
| `NumericCodec.java` | `03a807ab25bd1e5ed36e1089deae3bf30071438a` | Quotient/remainder digit emission directly into the existing UTF-16 ingress array. |

The constructor previously called `Integer.toString` for every value during sizing and again
while emitting: 20,002 calls over the fixed domain. It now computes each length and offset once
and writes the digits without materializing those temporary strings. The existing backing,
cached views, source identities, parser, and public API retain their contracts. The current
Synexia numeric source mapping remains `m3/lexicon/synexia-number-target-map.tsv`; the shortstring
mapping fragment records algorithm provenance and does not replace the numeric data source.

Canonical authoring and proof: `hsoliwal/com.synexia`,
`synexia-openrewrite-recipes/recipe-crates/m3-shortstring-number-space-20261019`.
Named OpenRewrite composition: `com.synexia.m3.ShortstringNumberSpace20261019`.
Target preimage: M3JDK21 commit `677437be295b23f7ccf7409be287008944d0e8b0`.
The templates are applied by the existing hash-pinned Java/text SDK recipes; there is no new
runtime dependency or production transformation engine.

The canonical crate pins all twelve donor classes and preserves executable witnesses for
negative-input sizing, append-count, Unicode-narrowing and packed-boundary preconditions.
Those observations limit broader reuse; this change admits only the closed-domain constructor
atoms. `M3NumberSpaceConstructorProof` checks every value using JDK decimal spelling as an
independent oracle, shared storage, contiguous offsets, cached references and parsing views.

Local constructor measurements and replay receipts belong to the canonical proof crate.
This receiver-level change does not establish installed M3JDK21, OS interning or general
forward/reverse string-index throughput. Its `LocalM3Arena` ownership is unchanged.
