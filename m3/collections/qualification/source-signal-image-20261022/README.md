<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 direct source/compiler signal receiving

**M3JDK21 is the JDK itself.** This additive source-time intake is inside the same modified JDK product, not an extra Synexia runtime or collection wrapper.

### Existing owners reused
- `M3SourceSignalPlugin` in `m3/compiler/source-signals` (current product) already emits attributed scalar (`S`), hashed lexical (`T`), and candidate-call (`E`) records plus a verified SHA-256 trailer. It remains unchanged.
- `M3SignalColumns` source in pending M3JDK21 #1803 is the typed validity/provenance owner, and the existing `M3SignalDimensions`, `M3SignalOrderNative` and `M3ProgressiveSignalEvidence` remain unchanged.
- The new `M3SourceSignalImage` validates the complete source-time v1 protocol and maps the existing compiler scalars to 17 typed primitive columns (15 exact counts plus two known-graph site-count columns); token hashes remain unsigned heuristics; source/type identity remains SHA256+UTF8 type directory.
- `M3SignalRelations` stores the attributed known-type candidate-call graph in CSR primitive lanes, with DIRECT vs DISPATCH_CANDIDATE separate, external edge candidates counted but not invented or dropped, and bounded BFS. This is neither a complete method-level CFG nor dynamic invocation truth.

### Full receiving contract
All four Java owner/tests are exact canonical Synexia recipe postimages, with SHA-256 and Git blob pins in `SOURCE_MANIFEST.tsv`. Source-generation is independent of user domain ordering and execution profiling; the latter require separately versioned evidence and policy. No Java application object pointer passes into JNI; no native symbol or original JDK API is changed.

The product PR is intentionally **stacked on #1803**, not a faux merge to `master`. It must not merge before its exact typed-column prerequisite and canonical recipe #12911 are accepted. Canonical new recipe: `hsoliwal/com.synexia` crate `m3-source-signal-compiler-image-20261022`.

### Actual acceptance gates
1. Java 21 strict compilation of actual product owner; execute source-sealed and random graph differential.
2. Compile/execute the existing `M3SourceSignals` plugin on real Java source, emit a v1 sidecar, verify checksum and consume it through this new typed image.
3. Original Maven JUnit of both the compiler plugin and M3 collections module, plus canonical Synexia Maven/OpenRewrite SDK apply, second fixed point and collision refusal.
4. Full source-built modified JDK21, original javac/jtreg/JCK/ABI, Windows/macOS/Linux, memory pressure, JIT/GC/CDS, allocation/CPU/native profiling if claiming improvements.
5. Static compiler call edges never qualify as real dynamic invocation counts, unique hashed tokens never prove equality, and a numeric separation never certifies semantic distance.

Local isolated Java21 compiler + seeded differential **277,157 assertions passed** with `-Xlint:all -Werror`. End-to-end product Maven/JNI/source-built installation gates remain NOT_RUN until independently executed. The globally locked receiving sequence String → arrays → collections → AST/compiler is not advanced by this focused intake.
