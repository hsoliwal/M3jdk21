<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 — primitive signal quality and dependency freshness receiver

This is **M3JDK21**, the existing modified Java/JNI/JDK21 product. It receives only
three exact Java source/test postimages and a CI workflow postimage from Synexia's
canonical Maven/OpenRewrite recipe `M3SignalQualitySchemaRecipe`, crate
`m3-signal-quality-schema-20261024`. No recipe engine, recipe fixture or convergence
memory is stored in this public product.

## Retained existing authorities
`M3SignalColumns`: original immutable int/long/double signals and validity masks.
`M3SignalDependencyEpochs`: original reverse dependency CSR and change generations.
`M3SourceSignalImage`: original compiler-emitted SHA-256-checked source image.
`M3SignalDomainPolicy`, `M3SignalTopK`, `M3ProgressiveSignalEvidence`,
JNI and HotSpot owners: unchanged; no second ranking or metric engine introduced.

## New original-owner source
- `M3SignalQualitySchema.java` — only O(dimensions) primitive metadata sidecars,
  24 families, SOURCE/COMPILER/INDEX/RUNTIME phase, reliability, invalidation,
  unit, producer, optional claim digest and estimate confidence; original typed
  value lanes and evidence IDs are reused, not copied.
- `M3SignalQualitySchemaSelfTest.java` — seeded differential validity/freshness
  checks, negative admission and descriptor tests, original epoch invalidation.
- `M3SignalQualitySchemaTest.java` — original Maven/JUnit adapter.

Three original GitHub blob hashes match canonical Synexia recipe resources:
`df7be1d4460d362c1c5c425ef2ec09b485639789`,
`8de9b5b967e5cdf6580a78997b4dee4a73d402fa`,
`eaa29e06a26553f6ef8fa3d4539ac9e10b9a3fa7`.

The original JDK workflow SHA-256 source-sealed succession:
`bba2b2716b79377b795a427491e78cceb02853575da0ad86829b0b04ef3b2373`
(original failed missing arrays artifact) →
`347b3bf2b28e7db27a5f656f916356da32fef497228d416e90c1a584f1d2f21f`
(existing arrays-module build) →
`2596cc2d39c02e0a5838df24ba0dd1a7593d7a3683131a19af0543c25ced73b5`
(new quality source and original JUnit execution). CI invokes the original
sources with Java21 `-Xlint:all -Werror` and exercises normal/-Xint oracles.

## Qualification status — draft/hold
LOCAL Java21 **PASS**: strict compile and 25,036 deterministic assertions per
normal/-Xint execution with original dependency epoch source and API-compatible
test doubles for other unchanged M3 types. This does **not** prove original
M3JDK21 source/JUnit passing. Original product exact-head hosted CI, Synexia
OpenRewrite first apply/fixed-point/refusal, source-built modified JDK, JCK,
jtreg, installed jmods, JNI/HotSpot/GC and multi-OS CPU/memory proofs require
independent verification. A CI workflow file is not test evidence.

The declared `SOUND_LOWER_BOUND` / `SOUND_UPPER_BOUND` values remain
**producer claims** until an independent domain certificate establishes them;
an SHA does not certify equality, ranking, or graph completeness. Unknown
reflection, dynamic loading and native/external effects require
`M3SignalDependencyEpochs.invalidateAll()` or a more conservative owner.
