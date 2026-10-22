<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 compiler-manufactured signal facts (2026-10-22)

**M3JDK21 is the Java/HotSpot/JNI product itself**, not M3 plus a separate JDK. This is an additive, isolated source-time capability under its existing `m3/collections/signal-codegen` owner; it does **not** assert that javac, java.base or whole-JDK acceptance has already been completed.

The compiler's existing `@Sortable` processor retains its six deduplicated unsigned hash token lanes and optional numeric `absoluteSignal()` extractor, including original generated class/method naming. This additional source-manufacturing pass emits `M3CompilerFacts` with **22 primitive integer count coordinates**, validity bits and family/provenance tags.

The known model-level coordinates are fields, methods, constructors, implemented interfaces, distinct *hashed type tokens*, method-parameter and declared-throws counts. With a javac `Trees` path, the AST also generates invocation-site/hashed resolved-callee, branch, loop, ternary, lambda, allocation, synchronization, throw/try/catch and return-site counts. The nested type is excluded from its parent's syntax counts. Hashed token counts are hints, not collision-free graph identities. Incoming callers and dynamic invocation frequency are explicitly **UNKNOWN** (mask bit clear), never incorrectly reported as zero.

The selected domain may use `M3CompilerFacts.compareBy` over known **exact int counts** or explicitly request a heuristic `project(weights)` score. That scalar is **not** canonical object order; it returns NaN if any weighted coordinate is missing. The existing `M3ProgressiveSignalEvidence` retains authoritative exact-verifier and metric-bound ownership. Neither source-time manufacturer nor this helper asserts purity/alias freedom, HotSpot inlining or JNI profitability without evidence.

All five implementation/test files are byte-identical to the canonical Synexia `m3-compiler-signal-facts-20261022` postimages, with source SHA-256 and Git blob identities recorded in `SOURCE_MANIFEST.tsv`. Before-image pins are the original M3JDK21 master; unrelated evolving source is never overwritten.

Execution gates:
- `signal-codegen/verify.sh` compiles the actual owner with javac21 warnings-as-errors and runs the original `SortableProcessorSelfTest` with generated-source, permutation, unsupported shape and source-time fact checks. It invokes `M3CompilerFactsContract`, whose independently run primitive proof has passed **24,980 assertions**.
- `M3CompilerFactsJniProof` consumes only known primitive projected numeric lanes and requires the **existing** `m3signalorder` C/JNI sort under `-Xcheck:jni`. No new JNI symbol or Java heap reference crosses into C; missing JNI is a failure, not a hidden Java fallback.
- Existing `StructuralSignalsExtremeWeightsContract` and previous source/method signal semantics must remain passing. Canonical Synexia Maven/OpenRewrite first-pass, fixed-point, missing/drift refusal and source ownership must execute separately.
- Installed modified-JDK, full JCK/jtreg, javac/JDT integration with persistent indexes, full method/call graph, HotSpot GC/CDS, multiple OS, memory and performance gates are still **NOT_RUN/HOLD** until real outputs exist.

This is an actual receiving slice, not a claim that all 22 signal kinds are semantic proofs or that all Synexia families have been imported.
