<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 original-owner compiler signal SCC receiving

## Product boundary and source provenance
**M3JDK21 is the JDK itself, not M3 + JDK.** This additive Java 21 capability remains in the original `m3/collections` product proving module, and consumes the existing javac signal plugin and `M3SignalRelations` primitive CSR without duplicating either owner. Strong source SHA-256 comes from the existing `M3SourceSignalImage`; the caller-provided 64-bit hint in the new projection is **not** a cryptographic provenance certificate.

Canonical reusable Maven/OpenRewrite recipe, exact generated Java source postimages, tested fixture, donor catalogue and append-only correction memory: Synexia crate `m3-source-signal-scc-20261023`. The three product Java files exactly match those source-sealed resources by Git blob and SHA-256, and were created serially after checking their target paths were absent.

## Exactly what is implemented
- Iterative two-pass strongly connected component decomposition over attributed known-source **type-level call candidate** edges. Direct-only and direct+virtual-dispatch-candidate interpretations are separate.
- Stable SCC IDs based on the minimum source row, independent of edge insertion order.
- Packed `long[]` cyclic component masks; `int[]` member offsets, member IDs, typed target coordinates and condensation DAG; `long[]` internal/cross-component source call-site totals.
- Deterministic primitive-heap Kahn topological ordering and longest condensation-DAG depth signals.
- Six lazily materialized typed STATIC columns using the current `M3SignalColumns` owner; no Java Entry/Node per edge and no new JNI export.

This **does not** claim method-level resolved dynamic dispatch, runtime invocation counts, total reflection closure, JVM equality, a semantic distance metric, or an installed JDK module integration.

## Acceptance
Current source file and unchanged dependency Git blob receipts: `SOURCE_MANIFEST.tsv` and `SOURCE_OWNER_PINS.tsv`.

1. Original real-source Java21 `javac --release 21 -Xlint:all -Werror` of `M3SignalColumns`, `M3SignalRelations`, SCC owner and independent 1,549,102-check test with `-Xss256k`.
2. Existing original javac `M3SourceSignals` plugin creates a source-attributed SHA-256-sealed image; the original `M3SourceSignalImage` parses it, and this SCC component derives a primitive graph and typed columns.
3. Original owning Maven/JUnit test; canonical Synexia actual OpenRewrite first application, zero-change replay and drift refusal.
4. Whole modified-JDK/JTreg/JCK/ABI, JIT/GC/CDS, platform native/CPU/heap/RSS, and any JNI benchmark stay separate explicit gates. A pure compiler-time SCC does not require moving callbacks into JNI.

The isolated local Java 21 test passed **1,549,102** assertions under warnings-as-errors and limited Java stack. It used API-compatible test doubles for the two unmodified owners. Actual product Java/JUnit/JNI acceptance must be observed in hosted CI before marking the PR ready. Locked String → arrays → collections → AST/compiler phase order remains unchanged.
