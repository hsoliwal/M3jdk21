<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# Exact JDK javac signal-quality receiver (result-only)

M3JDK21 is the actual integrated JDK21 product. Canonical transformation
mastery, Maven/OpenRewrite executable recipe, permanent fixtures, correction
memory and source hash templates reside exclusively in private Synexia,
`M3SourceSignalQualityFactoryRecipe`, crate
`m3-source-signal-quality-factory-20261024`.

This source receiver contains:
1. `M3SourceSignalQualityFactory.java`: binds all 17 original
   `M3SourceSignalImage.Scalar` columns, their original type/source SHA,
   original values, recorded compiler semantics and source-time reliability
   without a duplicate compiler or source parser.
2. `M3SourceSignalQualityFactorySelfTest.java`: emits a synthetic
   SHA-256-correct original compiler S-row image, calls the real original
   `M3SourceSignalImage.parse`, exercises the actual quality factory,
   source-version/epoch invalidation and corrupt-image refusal.
3. `M3SourceSignalQualityFactoryTest.java`: original JUnit adapter,
   not a replacement test framework.

Product source Git blobs equal the original canonical Synexia template blobs:
`f097c9505a15be8f94e3b26ade413febd98c58b5`,
`eb960b5d4e74a6d44b02130727f339e43a976421`,
`6e7d5fdcab3c4ba74f7fb04432884ef2681c3f9c`.

The original Java21 workflow source SHA-256 succession is exact:
`2596cc2d39c02e0a5838df24ba0dd1a7593d7a3683131a19af0543c25ced73b5`
→ `8815d927a9d2ce035fc2cfbed4fb5fbec283da49d7e082b1244dee68ef32d72e`.
The test job still compiles the actual existing Java 21 source and invokes its
original Maven module and source-vendored JUnit after building the original
m3-arrays artifact. Original collections Maven/JUnit is not skipped. Native,
HotSpot, GC, JCK, jtreg, linked JDK and platform performance are not silently
admitted.

**Evidence state:** local strict Java21 implementation rehearsal and normal
plus interpreted tests (116 assertions each) with an API-compatible compiler
image double: PASS. Actual modified JDK source compilation, original parser
integration, source-vendored JUnit, Synexia SDK fixed-point/refusal and all
source-built product release gates: PENDING. Retain DRAFT/HOLD until executed.

All "exact" claims are restricted to **compiler-recorded source facts**,
not actual runtime invocation or allocation, proven object equality,
completed dynamic dispatch, or validated distance bounds. External/native
dependency changes require explicit conservative invalidation.
