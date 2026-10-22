<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 — javac method signals → exact typed columns / CSR

**M3JDK21 is the modified JDK21 product, not M3 + JDK.** The direct product
source in `m3/collections` reuses the existing typed `M3SignalColumns` and
primitive CSR `M3SignalRelations` owners and consumes the optional source-time
`M3JDK21-METHOD-SIGNALS/1` image from the existing javac M3SourceSignals plugin.

This PR is **stacked on M3JDK21 #1838**, which adds the opt-in `methodOut=FILE`
producer. The legacy SHA-256 S/T/E class-level image remains byte-identical.
The canonical reusable transformation recipe, source templates, correction memory
and category/donor catalogue live in `hsoliwal/com.synexia` crate
`m3-method-signal-image-receiver-20261022`.

## Product source facts

The new `M3MethodSignalImage` parses all 14 exact syntactic method counters,
verifies each `M` method identity and source-site `C` call edge, checks
consistent direct/candidate/unresolved counts **even when invalid data has
been re-hashed**, and refuses changed/unsupported schema or malformed Unicode.

Known attributed method targets use the existing CSR `int[]` offsets,
targets, source-site multiplicity and `byte[]` DIRECT vs DISPATCH_CANDIDATE.
Unknown/external call targets and compiler-unresolved expressions are counted
separately. Neither all-internal recorded calls nor exact source signatures imply
runtime call graph closure, method purity, hotness or semantic method equality.
Names are packed into UTF-8 `byte[]` and offsets; the method image carries a
full SHA-256 identity plus a non-certifying 64-bit index hint.

## Verification

- Source custody: four targets have original ABSENT preimages; all Java/POM
  outputs and Git blobs match canonical source-sealed recipes.
- Host API-shaped local Java21 compilation with `-Xlint:all -Werror` passed
  **125,020** focused parser/CSR assertions and 1,500 seeded graph fixtures.
  This was **not** compilation of the exact M3JDK21 original owners.
- Real Actions gates compile the actual owner, execute seeded regressions,
  compile and run the original `M3SourceSignals` plugin using both `out=`
  and `methodOut=` on source, and check the resulting method image.
- The test-only POM uses a separately published JUnit 5.11.4 to execute the
  actual product sources; it does **not** replace the original collections
  POM or erase the unresolvable custom JUnit 5.10.2-m3-source-1 dependency.
- Canonical Synexia Maven/OpenRewrite first-apply, second fixed point,
  existing-file refusal, source checksum and SDK namespace gates remain
  independent of product Java/JUnit.
- Whole modified-JDK source/JMOD/jtreg/JCK, JNI/HotSpot/JIT/GC/CDS,
  Windows/macOS, cancellation/incremental compile, runtime profiling and
  CPU/heap/native memory qualification are not implied by source receipt.

Global String → arrays → collections → AST/compiler receiving order is unchanged.
