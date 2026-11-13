<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 direct typed domain policy + progressive evidence

**M3JDK21 is the modified JDK itself.** No M3+JDK compatibility runtime or new third-party provider was added. This branch is stacked on the existing typed Top-K PR #1832, whose parent compiler image #1809 and original columns #1803 were qualified separately. This PR adds one internal source-time fact consumption policy, not a second metric engine.

The existing `M3SignalColumns` product owner received one package-private `finiteMetricAt` method. It admits only valid, finite numeric facts, rejecting long coordinates outside [−2^53,2^53] before lossy conversion. All existing public/protected declarations, exact typed ordinal comparison, selection and stable sort remain unchanged.

`M3SignalDomainPolicy` declares independent typed ordering priorities and weighted L1 metric columns; each column retains its source `Origin` and evidence ID. Exact domain order is delegated to the existing `M3SignalTopK`. Progressive pairwise distance and conservative bounds are delegated to the existing `M3ProgressiveSignalEvidence.Plan/Workspace`, with noncertifying `ordered=false` and `completeExactSchema=false`. It retains only O(dimensions) policy metadata, primitive columns are still owned by the original `M3SignalColumns`. No `Entry`, per-row node or alternative Java/HotSpot/JNI owner.

Compiler-derived source identity is the previously **verified SHA-256** from the original `M3SourceSignalImage` parser; a separately caller-declared digest is not falsely promoted to verified authority. All pair/workspace and Top-K operations require both source epoch and complete digest to match the immutable policy namespace. Declared max-gap intervals are rechecked by the existing progressive engine; unknown upper bounds are never guessed; no numeric similarity is treated as domain object identity.

### Test and custody
The canonical Synexia recipe `m3-typed-signal-domain-evidence-20261022` stores one exact existing-owner preimage, four postimages and actual OpenRewrite SDK application/fixed-point/source-drift refusal. The four target Java files match those resources exactly by Git blob identity; `SOURCE_MANIFEST.tsv` pins SHA-256 before/after and resulting Git blobs. Every source change was applied serially, preserving append-only develop history and original licenses.

A strict local Java21 compiler **dependency-contract harness** and independent seeded arithmetic oracle passed 80,527 checks, after correcting a tie-order expectation. This is **not a full original-owner compile**; the branch's real CI compiles actual M3/JDK owners, executes the seed test, and uses a separately labeled standard-JUnit qualification POM. The original collections Maven POM uses the unresolved `junit-jupiter:5.10.2-m3-source-1` custom artifact, so original reactor JUnit is BLOCKED_DEPENDENCY until qualified source resolution; the alternate POM is not that original reactor.

No new native C function is justified in this policy-only atom. Existing `M3SignalOrderNative` and `M3IndexMaskCompactor` JNI tests, complete source-built JDK, jtreg/JCK, javap/ABI, JIT/GC/CDS, platform and memory-pressure/resource measurements remain independent NOT_RUN/HOLD gates. Compiler facts are not runtime invocation counts; the String→arrays→collections→AST/compiler global receiving order is not advanced.
