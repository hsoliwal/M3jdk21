<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3 String I/O and M3Compute receiver

Canonical source-sealed recipe: `com.synexia.rewrite.M3Jdk21StringIOCompiletime`, owned by
`hsoliwal/com.synexia` at `8d7acb0d9d6e6c0a59c69d7197e9308ba7292930` (PR #10866).
The complete staged compile-time receiving plan is in that revision's
`synexia-openrewrite-recipes/recipe-crates/m3jdk21-string-io-compiletime/README.md`.

## Delivered candidate

`sun.nio.cs.StreamEncoder.write(String,int,int)` encodes a confined read-only String-backed
CharBuffer rather than allocating a full `char[len]` input snapshot. It retains existing
encoder state, byte output, locks, argument/closed-state precedence and zero-length-write behavior.
No new public API, String payload owner, Synexia runtime dependency or JNI ABI.

`M3StringIOTest` covers eight charsets, malformed/unmappable actions, random/hostile UTF-16,
surrogate/chunk/flush boundaries, range confinement, an instrumented non-array input path,
a virtual-thread writer, Reader transfer and StringWriter live-buffer compatibility.

`M3ComputeConstantFactsTest` proves javac can extract exact regex/String/flags/mask constants
without running application initializers or annotation processors. It is NOT an installed compiler
plugin, persisted precompute image or runtime admission path.

## M3Compute receiving direction

Compiler-attributed constants and resolved calls -> bounded preparation in existing M3 owners ->
versioned application-local image -> target-owned admission -> dynamic input over String owner/ranges.
Prepare fixed regex programs, search needles/delimiters, immutable vocabularies, format fragments,
mask/decision tables and proven pure constant kernels. Preserve Java evaluation/failure timing,
Pattern/Matcher identity and state, exact UTF-16 and provider/flags/Unicode semantics. Do not execute
arbitrary class initializers, trust mutable static-final arrays, or turn source-looking strings into code.
Application corpus images must not become universal java.lang state. Keep fallback and byte/work budgets.

## Naming and pending mapping

- String range traversal: existing StringCharBuffer -> String/M3String, unchanged ownership.
- Output range shadow removal: existing sun.nio.cs.StreamEncoder, candidate in this PR.
- Compiler constant extraction: existing javac attribution APIs, eligibility proof only.
- Persistent M3Compute/String/regex preparation admission: PENDING source-built target evidence.
- Reader range transfer and immutable StringWriter accumulation: PENDING, existing public contracts.

## Executed evidence and limits

On Debian OpenJDK21.0.12.1 Linux x86_64, original and candidate StreamEncoder compiled strictly and
were separately patched into the host java.base. Both passed 23,641 checks; instrumented input changed
from array-backed to non-array read-only. The compiler fixture passed 20 checks: seven constants and
four nonconstant fields. Three postimages were independently kernel-replayed with a fixed point and
six source/template/occupied refusals. Sources match their canonical recipe templates exactly.

NOT_RUN: full Maven reactor; actual OpenRewrite SDK parser/application/fixed-point/refusal suite;
canonical M3 task admission; source-built M3JDK jtreg/HotSpot/JNI/native; complete Reader/Writer and
M3Compute runtime receiving; all charset providers; CPU/heap/native-memory performance benchmarks.
No speedup or full integration claim. Keep draft; no merge/promotion authority.

Recipe metadata is Apache-2.0. Runtime/test postimages retain their target GPL/OpenJDK terms.
