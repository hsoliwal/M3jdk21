<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 — opt-in attributed javac method signals /2

**M3JDK21 is one modified JDK product, not an M3 library layer.** This bounded product receiving change uses the existing `M3SourceSignalPlugin`, `M3SourceSignalImage`, `M3SignalColumns`, `M3SignalRelations` and `M3ProgressiveSignalEvidence` owners. It does not introduce a new AST parser, annotation processor, public JDK class name, JNI symbol or runtime Synexia dependency.

## Contract and schema
- **Default** `-Xplugin:M3SourceSignals out=FILE include=all` still writes byte-stable `M3JDK21-SOURCE-SIGNALS/1` S/T/E rows plus SHA-256 trailer. Explicit `methods=off` must be identical.
- **Opt-in** `methods=on` writes version `/2` with all original S/T/E rows followed by sorted **M** attributed declaring-method source facts and **R** method caller-edge candidate rows.
- Declared method metrics: modifier bit flags, parameter count, invocation expressions, allocation sites, method references, direct/dispatch-candidate/unresolved calls, loops, branches, try/catch and lambdas.
- `M3SourceMethodImage` owns typed primitive `int[]` method counters, packed `long[]` validity and provenance, compressed UTF8 name offsets, primitive CSR target/site indices and `byte[]` DIRECT vs DISPATCH_CANDIDATE labels. All known-method edges are source-time candidates. External callee signatures and multiplicity remain explicitly counted and never represented as a complete runtime call graph.
- No method facts are inferred for v1; requesting them from a v1 image must refuse. Strong SHA-256 remains the source artifact identity; FNV hashed lexical candidates never prove equality, and no static count is equated with runtime frequency.

## Recipe and donor custody
Source preimages are exact current product Java owners from before this PR. Six output Java files match canonical Synexia recipe `m3-source-method-signal-v2-20261022`, including the unchanged-v1 javac regression, strict corrupted-v2 refusal, known/external graph checks and JNI-preserving owner JUnit. The `SOURCE_MANIFEST.tsv` includes actual SHA256, Git blob and canonical resource names. `verify.sh` rejects source drift. Original donor/version/algorithm decisions and recipe failure memory reside in the corresponding Synexia crate.

## Actual acceptance
- Compile **actual** Java21 javac plugin and test through its original ServiceLoader. Exercise annotated-only/default and opt-in /2 with source declaration/order permutations and SHA-256.
- Compile original M3JDK21 type-column/CSR/image sources and the new method reader under `javac --release 21 -Xlint:all -Werror`. Run both v1 reader and v2 selftests, plus a real plugin-emitted /2 sample.
- Maven original producer/consumer JUnit, canonical OpenRewrite SDK first-apply, fixed point and source drift refusal.
- Modified JDK21 jdk.compiler/java.base, jtreg/JCK, JVM/JNI/JIT/HotSpot/GC/CDS, vendor licence/source map and cross-platform qualification remain separately required before installed-image promotion. These gates must be reported PASS, FAIL, NOT_RUN or HOLD based on actual execution.

The locked String → arrays → collections → AST/compiler receiving order does **not** advance automatically; this is an opt-in candidate in the compiler proving module.
