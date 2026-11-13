<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 filtered primitive sequence contract

The actual product module now combines arrays, algorithms and collections under
`src/jdk.m3.collections/share/classes`, using its original `module-info.java`.
The existing `jdk.internal.m3.collections.PrimitiveFilteredSequences` owner
operates on current `LongArrayList`, `LongArrayDeque`, `LongList`, `LongDeque`,
`LongQueue` and `LongStack` owners. Historical side-tree Maven module descriptors
are not substituted for the current product module.

Run with Java 21 on `PATH`:

```sh
bash make/m3/collections/verification/primitive-filtered-sequences-20261017/verify.sh
```

The verifier compiles **all Java sources in that actual product module** and
the unchanged
`test/jdk/jdk/m3/collections/PrimitiveFilteredSequencesRuntimeContract.java`.
It retains Java 21 API selection, disabled annotation processing, lint-as-error
(except optional module-name warnings), and the 256 MiB runtime heap limit.
Compilation uses isolated class and source paths, and execution uses the module
path with the original product descriptor. No synthetic module, backing stub or
private recipe is executed.

The runtime contract retains its deterministic 14,000-operation list and
12,000-operation deque JDK-model comparisons, signed extremes, hidden physical
rows, sorting, binary search, deduplication, nested views, read-only mutation
refusals, queue and stack behavior. This work repairs verification paths; it
changes no runtime implementation, interface, native symbol or global receiving
phase. `PrimitiveFilteredSequencesTest` remains the JUnit entry point to the same
runtime contract.

An independently compiled actual module is still distinct from the matched
source-built M3JDK21 image. Whole Maven reactor, installed-image, jtreg, JNI/GC,
platform, provider-parity and resource qualification remain separate gates.
Historical `STATUS.tsv`, `RECEIVER.tsv`, `CATALOGUE.tsv` and `VERIFY_CONTRACT.tsv`
are preserved as their original evidence; this verifier does not promote their
coverage or the String qualification stage.

## Historical receiving lineage

The original source recipe is
[Synexia PR #12186](https://github.com/hsoliwal/com.synexia/pull/12186), pinned at
`8ede0d597f402d3ba2c77eea8d3f4d2ab8cee76a`. Its Apache-2.0 attribution and exact
historical identity remain intact. Reusable recipe source, fixtures and
correction memory stay canonical in Synexia; M3JDK21 receives executed verifier,
test and documentation results with passive provenance.
