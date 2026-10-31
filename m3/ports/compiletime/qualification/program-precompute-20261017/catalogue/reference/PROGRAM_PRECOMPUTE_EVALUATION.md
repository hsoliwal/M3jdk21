<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Program and array precomputation: donor review and design evaluation

Research date: 2026-10-17. Status: **bounded source review and candidate design**. This document does not claim a compiler/runtime test, received source change, CDS image, GPU execution or performance result. The implementing recipe must bind its own exact preimages, postimages and executed gates.

## Scope and source identity

The selected compiler optimization belongs in the existing `com.m3.compiletime.M3EscapeFlow` and its `M3ItemReferenceScan` caller. It prepares stable bytecode dependency facts once per method. It does not execute application code or replace javac, ASM, HotSpot, collection payload owners or native lifetimes.

Private donor revision: `hsoliwal/com.synexia@81c4f19a5ce5a68da316ff6f2802f2041a684f31`. Public comparison revision: `hsoliwal/M3jdk21@d47a4b4fdb8e3794ec56768b06aa9e23730c6b8e`. Exact source paths and Git blob identities are in [DONOR_CATALOGUE.tsv](DONOR_CATALOGUE.tsv). A later target revision requires its own source binding; merge ancestry alone does not establish the receiving bytes.

The private `synexia-mindex`, `synexia-indexstring`, `synexia-index-precompute` and `synexia-indexstring-compiler` tree objects at the final donor pin match the previously inspected `a62746fce1aca5e93b4d34d9d99a28165c213243` snapshot. This permits the already read blobs to be attributed to the final pin without asserting that all repository content is unchanged.

The public checkout is sparse. Missing working files were classified **NOT_MATERIALIZED**, then resolved through exact Git trees and connector reads. `M3AST`, `M3ASTPC` and `M3ComputeConstantFactsTest` are present in Git at the comparison revision. No absence or missing-feature claim follows from a sparse filesystem search.

## What is already implemented as an owner

| Existing owner | Stable work it already represents | Boundary to retain |
| --- | --- | --- |
| D01: `M3StaticUsePlan` | Compile-time local-use, escape, effect, ordering and ownership admission; constant-step int-loop condition bounds | Source-use sites are not execution counts or GC reference counts. Unknown facts refuse. The final int induction update must not wrap. |
| D02: `M3StaticFactsProcessor` | Real javac syntax traversal and analyzer/source/method-bound loop summaries | An annotation expresses intent. Escape, owner lifetime and dependency effects remain explicitly unproven. |
| D11/D12: `M3EscapeFlow` / `M3ItemReferenceScan` | ASM source-provenance frames and current escape-kind classification | Reuse the existing frame interpretation, obtaining sites, callee policy, analysis-failure disposition and output order. |
| D13/D14: `M3AST` / `M3ASTPC` | One actual javac task and identity-keyed projections within a frontend event | Projections, including null/failure, belong to one event and callback thread. A source digest alone cannot authorize attributed facts across classpaths or toolchains. |
| D15: `M3ComputeConstantFactsTest` | A test of compiler-resolved literal, flag and bit-mask constants | It uses attributed `VariableElement.getConstantValue()`; mutable arrays, environment reads, new objects and user calls do not become constant facts. This is a test/oracle, not a general Java interpreter. |
| D05-D08: existing rank/prefix/cache owners | Frozen mask geometry, cumulative counts, rank/select pages and bounded derived metadata | Immutable snapshot identity and exact indexing contracts are prerequisites. Live arbitrary predicate results are not admitted. |
| D09: `MIndexAtomProgram` | Immutable, bounded UTF-16 classification, transition, count and mask tables | A finite admitted atom does not establish that arbitrary application control flow can be pre-executed. |

The existing target `M3CompileTimeRegexPrecomputeRecipe` remains a historical authoring path for the compile-time module (Git blob `06935b9016a7d79f8f1fe04a77947aa64cf9a9c4` at the public pin; named YAML blob `992bec003139faab52ed3220c057439857a4fa00`). D16, `M3RegexPrecomputeMastery`, is an existing differential-qualification owner. Its report expressly does not grant source-mutation or promotion authority. Reuse the canonical recipe framework for the new bounded extension.

## Selected compiler candidate: prepare the dependency graph once

### Current repeated work

`M3ItemReferenceScan.method` already invokes ASM `Analyzer<SourceValue>` once for a method. For each obtaining site, it then calls `M3EscapeFlow.escape`, which builds a new instruction-sized taint lane, repeatedly scans instructions to a forward fixed point, and scans uses to accumulate that site's escape mask. Repeated sites in the same method redo the same source-dependency extraction and traversal.

Only those stable analysis facts should move out of the per-site loop. The application's operations are not being moved in time.

### Equivalent mask formulation

Let an edge `p -> q` mean that instruction `q` propagates a tracked value if its source provenance contains instruction `p`, using the existing propagation rules. Let `direct[p]` contain the OR of every existing consumer-kind mask attached directly to a provenance occurrence of `p`.

The forward implementation reports, for a site `s`, the OR of `direct[q]` over nodes reachable from `s`. The same answer is the least solution of:

```text
answer[p] = direct[p] OR OR(answer[q] for every propagation edge p -> q)
```

Build CSR rows from `q` to its source predecessors `p`. Start a bounded primitive queue with nodes having a nonzero direct mask. When a node's answer gains a bit, OR its answer into each predecessor; enqueue a predecessor only if its answer changed and it is not already queued. After convergence, obtaining sites read `answer[siteIndex]`.

The equivalence is specific to the current OR-based source propagation. Every bit travels backward along exactly the paths traversed by the forward taint closure. Cycles are safe because bits only accumulate over a finite domain. This argument would need revision if a future propagation rule required conjunction, negation, path-sensitive killing or a state other than this OR lattice.

The current owner names eleven result bits, including its analysis-failure lane. Preserve the whole mask and existing final classification; do not stop after the first retaining flag, collapse unknown to false, or retain only the final three-way class. A method that exceeds the prepared graph budget should keep the corrected existing fallback and its established failure behavior.

### Why reuse the local CSR/worklist patterns

D03, `MIndexHornClosure`, already demonstrates checked finite CSR adjacency and monotone worklist processing while retaining all alternative evidence. D04, `MIndexCsrGraph`, demonstrates degree counting, prefix offsets and primitive scatter. They are **pattern references** for the implementation in the existing escape owner.

The Horn conjunction counters, evidence objects, graph labels, edge weights and sorting do not belong in this compiler operation. No donor rule engine, graph facade or new parser is needed. ASM remains the source-provenance authority.

For `n` instructions, `e` provenance edges and a fixed `b`-bit mask, the prepared representation needs O(n + e) temporary storage. Propagation is bounded by bit additions, with O(b(n + e)) work after dependency extraction. This is an algorithmic bound, not a measured speedup. It avoids an O(number-of-sites × number-of-instructions) retained matrix. Edge extraction and all allocations still need explicit budgets; a small method or a single site may favor the existing path.

### Required implementation and verification properties

- Preserve source identities, empty/dead frames, category-2 stack values, duplicate provenance and exact JVM instruction semantics. Operand positions must follow ASM's `Frame<SourceValue>` model, not hand-counted raw stack slots.
- Keep all existing callee and annotation classification. Analysis of a call is not permission to execute that call.
- Keep exact obtaining-site order, BCI fallback/ordinal rules, sidecar schema and deterministic records.
- Check offsets, edge totals and queue capacity before allocation or publication. Absent, malformed or over-budget preparation must have an explicit fallback/failure result.
- Retain an independent forward oracle for a meaningful differential gate. Include multi-site aliasing, loops, merges, arrays, exact `requireNonNull` overloads, receiver identity uses, monitors, invokedynamic, unknown callees, exception handlers and all mask combinations.
- Measure preparation and per-site work separately on the same inputs; record peak temporary/retained storage and fallback counts. Do not infer time improvement from fewer source lines or a passing small fixture.

These are acceptance requirements for the implementing recipe. This research did not execute them.

## Arrays, offsets and state masks: what can be prepared

| Fact | Valid preparation lifetime | What stays dynamic |
| --- | --- | --- |
| Opcode/operand geometry and proven source dependencies | One exact immutable method plus its classification policy | New bytecode, changed callees/hierarchy or different analysis context |
| Array element width and admitted layout arithmetic | Exact JVM/layout context | Array identity, capacity, current head/offset, contents and allocation outcome |
| CSR offsets for frozen edges/tuple rows | Exact immutable plan or source epoch | Mutations or different ranges; native session leases and backing addresses |
| Rank/select directory over frozen bits | Exact mask snapshot and index convention | Writes, changed view range/order, predicate effects and live epoch checks |
| Nonnegative prefix count totals | Immutable count lane with overflow policy | Count changes; signed values invalidate monotone lower-bound assumptions |
| Fixed operation parameters, flags and pure constants | Compiler-proven or caller-supplied immutable plan | Input data, environment and callback execution |
| A loop's mathematical condition bound | Proven no-wrap induction plus stable bound binding | Early exits, throwing bodies, callback counts and externally changed state |

D05 can answer rank/select pages over a virtual OR of two frozen planes without creating the OR payload. D06 offers a simpler per-word directory. D07's prefix index deliberately uses checked signed-long arithmetic and guards monotone searches. Those are different contracts and memory tradeoffs, not interchangeable speed labels.

D08 already supplies a bounded cache over canonical immutable collection metadata, with external synchronization and a documented primitive-byte budget. Its budget excludes fixed cache arrays, object overhead, source arenas and caller-owned derived indexes. Retained-byte claims must keep those exclusions visible.

D10's generation fence is another important boundary: an explicit owner-edge count is not a JVM reference count, and a zero transition is not authority to free shared backing. Numerical offsets may remain reusable while raw native addresses and owner leases do not.

A normal `Predicate`, comparator or iterator callback is not proven pure. Precomputing a whole filter mask can change the number and order of calls, which exception is observed, and what a live view sees after mutation. Even an apparently empty operation may have an established null/range/read-only/epoch check order. Geometry can be prepared while callbacks remain at their original observation points.

## JVM self-precomputation: JDK 21 and newer AOT work

[JDK 21 CDS documentation](https://docs.oracle.com/en/java/javase/21/vm/class-data-sharing.html) describes preprocessed class metadata that can be mapped and shared at startup; AppCDS extends this to application classes. D21 is a concrete JDK-owned example: `ArchivedModuleGraph` restores a selected boot-layer configuration through CDS. This is useful JVM self-precomputation, subject to its actual module and archive admission rules.

[JDK 21's java command specification](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html) defines archive compatibility and command-line constraints. Archive identity must include the actual JDK and relevant class/module configuration. A source-fixture run with patched modules does not prove CDS use. A future CDS experiment should use the actual produced image and report cold startup, warm startup and memory separately. No archive experiment is part of this source review.

[Project Leyden](https://openjdk.org/projects/leyden/) identifies AOT class loading/linking in JDK 24, command-line ergonomics and method profiling in JDK 25, and object caching with any GC in JDK 26. [Inside.java's explanation](https://inside.java/2026/01/09/run-aot-cache/) distinguishes the training/cache stage from ordinary application runs. These are useful architectural comparisons, not features to assume in the current JDK 21 product. Native-code caching and newer JEP delivery status require their own version-specific qualification; this document makes no current-mainline or JDK 21 native-AOT claim.

Class initialization is not an arbitrary static computation hook. [JLS 21, chapter 12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2) specifies initialization timing, synchronization and error states. [JLS 21, chapter 15](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.7) specifies evaluation order, abrupt completion and arithmetic behavior. A final reference to an array does not make its contents a compiler constant. Source hashes and annotations do not prove stable environment values or side-effect-free initializers.

D19's `ArraysSupport.newLength` illustrates why even capacity arithmetic needs care: it can throw `OutOfMemoryError` without allocating. Hoisting that computation ahead of existing argument or callback evaluation can change observable behavior. Likewise, [`Arrays.parallelPrefix`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html) requires an associative, side-effect-free operator; its algorithm is not permission to parallelize ordinary callback-driven collection traversal.

## External donors, categories and selection decisions

The catalogue records two bounded external rank candidates: Sux4J `Rank9` and RoaringBitmap `FastRankRoaringBitmap`. Their exact revisions, source blobs and licences were read. Neither was executed or imported. Existing MIndex directories cover the immediate need, so they remain comparative references. Sux4J's borrowed bit-vector lifetime, endpoint behavior and directory cost need target review; Roaring's unsigned domain, large cardinalities and complete inherited mutation surface need review before cache reuse.

OpenJDK's existing array growth, prefix scan and archive owners are version-pinned references. In particular, the inspected HotSpot `escape.cpp` header is **GPL-2.0-only**; the historical target donor ledger's Classpath-exception label is too broad for that file. This document records the actual header and preserves the historical ledger. No HotSpot source text is copied into the Apache compiler owner.

[CATEGORY_REFERENCES.tsv](CATEGORY_REFERENCES.tsv) maps LeetCode, HackerRank, GeeksforGeeks and licensed GitHub categories. It includes immutable versus mutable range sums, state-dependent dynamic arrays, offline range updates and rank/select. These are problem-contract references. No challenge solution, discussion submission or unqualified tutorial implementation is copied. In particular, an offline difference-array batch cannot replace an observable sequence of live collection updates.

## Document schema and custody

`DONOR_CATALOGUE.tsv` records: stable row ID, category, repository/revision/path/blob, observed per-file licence, inspection scope, selection, useful atom, constraints and custody. `FULL_SOURCE_READ` and `TARGETED_SOURCE_READ` describe review scope, not executed tests. `SELECT_PATTERN` means independently adapt the algorithm within an existing owner; it is not approval to paste all donor source.

`CATEGORY_REFERENCES.tsv` records problem categories and evaluation constraints. It is not an algorithm implementation catalogue or licence grant.

Only this `reference/` directory is proposed as recipe documentation. The adjacent `donor-review/` directory contains exact private source copies for engineering inspection and is **not selected for publication or public mirroring**. It must not be included by a blanket recursive copy.

The next concrete step is the bounded, recipe-generated escape-owner extension with independent source-level differential proof. Broader collection mask preparation and JVM archive work remain separate candidates until their target semantics and actual measurements are qualified.
