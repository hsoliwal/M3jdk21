<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3JDK21 CPU tool atoms — static compilation and runtime admission

Status: **isolated opt-in Java 21 port / targeted tests**. This is not the completion
of every tool enumerated in the supplied CPU-tools attachment, not a replacement of
`java.base`, and not a default Java command/shell surface.

## Ownership and scope

Canonical recipe/candidate catalogue: `hsoliwal/com.synexia`,
`M3JdkCpuToolPrecompute20261014` and
`synexia-m3-recipe/catalogues/M3_CPU_TOOLS_PRECOMPUTE_20261014.tsv`.
M3JDK21 receiver: `m3/ports/precompute` (existing Apache-2.0 module).
This is an independently licensed opt-in program under `m3/**`, not a GPLv2
OpenJDK source transplant. Existing native C11/JNI and regex/text/collections
owners remain unchanged. `M3CpuToolNativeSignals` is a thin typed receiver to
the **existing** `M3CodeTextSignalBatchJava` and optional
`M3CodeTextSignalBatchNative` provider. No duplicate native ABI.

The attachment carries 91 category entries (summary and detailed groups). They
remain **inventory/candidates**, not 91 executable implementations. The mirrored
catalogue lists these plus 13 narrowly defined executable Java primitives.
Existing Synexia CPU-Camel built-in operators, algorithm runtime, M3 fast-search,
Unix command orchestration, memory residency and other families are independently
owned and must pass their own qualifiers before a target binding becomes executable.
No GitHub/LeetCode/HackerRank/GeeksforGeeks code was imported by this packet.

## Operational contract

`M3CpuToolPlan.compile` performs deterministic admission of a small, fully specified
DAG of stages with literal operator IDs. Only the 13 `M3CpuToolRuntime.operations()`
IDs can execute. All other catalogue entries reject before effects.

1. Bound DAG to 128 stages, 512 dependency edges, at most 16 parents per stage.
   Refuse duplicate names/edges, unknown tools, arity mismatch, cycles, dangling
   references and unreachable nodes.
2. Canonical sorted definitions determine SHA-256 identity; dependency order
   remains caller-specified and governs fan-in. The runtime topological schedule is
   deterministic. No LLM/agent input is execution permission.
3. Stable literal UTF-8 is validated against malformed UTF-16; the exact literal
   bytes and KMP prefix automaton are computed **once at plan compilation**.
   Subject bytes remain runtime inputs: no false compile-time precomputation of
   data not known at compile time.
4. The versioned `M3CPU001` image contains only tool IDs, stable parameters,
   dependencies and an exact checksum. Decode checks SHA-256, bounds,
   canonical re-encoding, type/graph admission and absence of trailing bytes.
   A content checksum does not establish a trusted signer's authority.
5. Runtime input is snapshotted, intermediate buffers are retired after their
   last DAG consumer, and stage output is bounded by `-Xmx` and current JVM
   residency. Budget refusal is explicit; no forced GC, speculative memory
   swapping, arbitrary Java array resizing, JNI pointer retention or bypass.
6. Tool outputs are exact binary contracts: SHA-256 = 32 raw bytes, literal hits
   = big-endian signed-int byte indexes with overlaps, unsigned byte sort and
   distinct, histogram = 256 big-endian int counts, length/entropy = big-endian
   64-bit, RLE = two-byte (unsigned count 1..255, byte value) runs, and XOR
   = one compile-time byte mask. These are **not** GNU/BSD locale text semantics.
7. `text.code-signals` decodes strict UTF-8 and calls the existing M3 UTF-16
   lexical signal oracle. If `-Dm3.cpu.tools.native.signals=true` is set, the
   same input is passed to the existing JNI provider and compared byte-exactly;
   mismatch fails closed. It never promotes lexical hints into a parser result.
8. No subprocess launches, filesystem scans, arbitrary reflection, model calls,
   remote access, rewriting or code execution can be requested by the plan.

## Build-time compilation and invocation

This is an example one-stage search plan. Column 6 is unpadded Base64URL for
literal `aba` (`YWJh`). Header and each row are tab-separated:

```text
M3_CPU_TOOL_DAG_V1\toutput\tstep\toperation\tinputs\tconstantBase64Url
M3_CPU_TOOL_DAG_V1\tmatch\tmatch\tsearch.literal\t\tYWJh
```

Save it as `plan.tsv` with **real tab characters**. Compile using Java 21:

```sh
mvn -B -ntp -f m3/ports/precompute/pom.xml test
java -cp m3/ports/precompute/target/classes \
  com.m3.precompute.M3CpuToolImageCompiler plan.tsv plan.m3cpu
```

The compiler accepts only explicit paths and refuses overwriting an existing
image. `M3CpuToolImage.decode(Files.readAllBytes(path))` validates and compiles the
static KMP matcher on load. No Java classfile bytes are generated; the output
is a versioned operator-plan image, not JVM bytecode.

The Maven test module already contains JUnit 5. The built-in
`M3CpuToolPrecomputeTest` invokes the same standalone seeded suite used by
`M3CpuToolPrecomputeSelfTest`, including deterministic program-order permutations,
12,000 random literal search trials, serialization fixed point, refusal, aliasing,
RLE parity, Unicode and memory budgets. JNI/hardware availability is *not*
inferred from the Java-only run.

## Quality gates and limitations

| Gate | Requirement |
| --- | --- |
| Java 21 | `--release 21 -Xlint:all -Werror -proc:none` |
| Precompute | stable digest, independent order, immutable input, KMP overlap |
| Image | exact SHA-256, canonical replay/fixed point, corrupt-source refusal |
| Java runtime | test seeded oracle under `-Xmx16m`, `32m`, `64m` |
| JNI | native C11 library loaded explicitly, Java oracle parity, ABI symbol proof |
| OpenRewrite | class-backed Synexia recipe, precise `ABSENT` preimages, refusal/fixed point |
| M3JDK acceptance | target Maven/JUnit, provenance checks; jtreg only if JDK sources touched |
| Cross-platform | Win/WSL/macOS/Unix host qualification remains separate |

Promotion must not claim that 91 candidate categories have implementations.
Do not merge until Maven/OpenRewrite/target checks that are required by repository
policy have actually executed. Preserve original OpenJDK license headers for
`src/**`; this packet does not touch that tree. Do not alter `master` directly.
