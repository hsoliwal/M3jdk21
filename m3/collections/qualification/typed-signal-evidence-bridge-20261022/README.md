<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 exact typed signal ↔ progressive evidence bridge

**M3JDK21 is the JDK itself**, not an M3+JDK runtime layer. This product branch extends the current M3 collections proving owner without creating a second comparator, numeric distance engine, JDK API, persistent payload array, or JNI symbol.

The existing `M3SignalColumns` typed-coordinate owner remains authoritative for exact `long` / `int` ordering. It gains a **package-private**, fail-closed `exactFiniteDoubleAt` accessor used only for a restricted progressive double reader. The new `M3SignalEvidenceBridge` validates all requested rows once, refuses missing values, NaN/∞ and `long` outside ±2^53, and then builds the existing `M3ProgressiveSignalEvidence.Plan` with caller-supplied domain ordering/metric certificates and a versioned SHA-256 provenance string. The runtime reader references the existing M3 typed columns instead of storing a second O(rows × dimensions) signal payload.

A `sourceSha256` argument is a producer-supplied identity **format**, not a hash computed/verified from arbitrary caller-owned signal data; certification remains the responsibility of the source producer, JDK compiler or verified image. `completeExactSchema` and any maximum gap are explicit caller certificates, never automatically inferred from FNV heuristics, static counts or a scalar projection. `PROFILED` maps conservatively to RUNTIME (not automatically MEASURED); HISTORY retains its tag in the source symbol.

The previous M3JDK21 #1803 typed columns, #1809 compiler image, #1832 TopK, `M3ProgressiveSignalEvidence`, javac plugin and `M3SignalOrderNative` JNI remain the canonical neighboring owners. This branch is **stacked on #1832**, and every source-changing output is byte-for-byte identical to the canonical Synexia recipe crate `m3-typed-signal-evidence-bridge-20261022`.

## Receipt/acceptance
`SOURCE_MANIFEST.tsv` binds one exact original current-owner preimage and four resulting Java21 postimages with SHA-256 and Git blob IDs. Existing public/protected method declarations remain unchanged in `M3SignalColumns`. New source/test classes only add an internal optional capability.

Required actual acceptance: source seals; strict Java21 compile and standalone exception/version/metric differential; original Maven/JUnit against the repository's custom source-vendored JUnit artifact; Synexia Maven/OpenRewrite first apply, second fixed point, changed-source refusal; source-built M3JDK21 javac/jtreg/JCK, JNI/HotSpot/JIT/GC/CDS, VM/OS resource measurement. No rewritten source or mocked compiler test proves these until executed.

The original M3JDK21 #1803 source-compile gate passed but its JUnit gate failed before tests because the custom JUnit source artifact had not been installed. The #1832 stack contains source-vendored JUnit-reactor installation; this PR reuses it, without weakening the original POM or recasting the alternate JUnit sidecar as fork acceptance. Keep DRAFT.
