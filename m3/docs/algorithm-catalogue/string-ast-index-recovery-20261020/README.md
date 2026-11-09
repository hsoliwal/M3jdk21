<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21: String, AST and indexed collection refinement

M3JDK21 is one SDK. These changes refine the existing `java.base`,
`jdk.compiler` and collection JNI owners. Synexia owns the reusable convergence
recipes; receiving M3JDK21 source retains its existing APIs and licenses.

## Synexia M3 invariant

Maintain reusable, validated transformation recipes. Apply them through serial,
contract-preserving per-file transformations. Continuously refine their algorithm
choices through problem-category comparison and pinned donor evaluation. A useful
behavioral atom may be replaced independently when its composition preserves
the original public contracts, callbacks, error ordering and lifetime. There is
no arbitrary limit on internal atom count. A finite proof packet bounds execution
resources, not the permissible decomposition of an implementation.

Before each pass, identify the existing owner and problem category; search the
catalogue; compare algorithms and license compatibility; freeze the preimage and
observations; qualify the canonical recipe; apply its exact output; run the
receiving runtime gates; append the selection and rejected alternatives. Preserve
failed candidates and evidence so the next pass can reuse the investigation.

## Search and decisions

`catalogue.tsv` is a tab-separated category and decision index. It records existing
owners, donor references, complexity, contract requirements and selection status.
No new search engine is introduced. For example, from the repository root:

```sh
rg -n 'array-growth|bit-mask|source-seal' m3/docs/algorithm-catalogue/string-ast-index-recovery-20261020
rg -n 'REJECTED|REFERENCE_ONLY|EXISTING' m3/docs/algorithm-catalogue/string-ast-index-recovery-20261020/catalogue.tsv
```

The collection model remains dynamic arrays with primitive offsets, indexes and
state masks. Metadata lanes must always cover the committed values capacity after
an allocation failure. The zero-mask JNI path copies the selected range in the
existing bounded chunks without redundant self-compaction. It preserves region
calls, validation, output order, allocation and detached-output ownership.

The compiler rereads source through the existing file-object path. A live weak
reference to a verified immutable String permits digest reuse only after exact
equality. Equal length and equal Java hash are insufficient. Cleared references
and changed content retain the original hashing and refusal behavior. No mutable
AST or bindings are shared. Between callbacks, the seal holds the source payload
weakly; an active callback still owns its source String for its existing lifetime.

## Reuse and rights

The selected native branch reuses the existing M3JDK21 window-selector pattern.
The array refinement preserves the existing primitive-lane allocator. The compiler
refinement uses the existing String owner and digest contract. Four previously
qualified filtered/sorted endpoint and reversed-view overrides
are restored from the existing licensed qualification history. Current surrounding
navigation, predicate and split metadata behavior is preserved.

Pinned GitHub references are evaluations: Guava's filtered views (Apache-2.0),
OpenJDK's bitmap/stable-sort owners (GPLv2 with Classpath exception), protobuf's
RopeByteString (BSD-3-Clause), and rowan's immutable-node cache (MIT/Apache-2.0).
LeetCode, HackerRank and GeeksforGeeks supply problem categories and fixture ideas;
no challenge solution or editorial implementation is copied. Existing source
headers and per-file target licenses remain authoritative.

## Evidence and recovery boundaries

This is the fresh recovery packet after an interrupted upload reset the earlier
temporary workspace. The earlier reported 30-phase proof, raw benchmark files and
source-built image are not presented as recovered artifacts. Current candidate
files receive a fresh canonical OpenRewrite proof and fresh Java/JNI tests. The
canonical packet's final qualification record identifies actual source hashes,
commands, outcomes, application receipts and any unexecuted gates.

The category decisions preserve earlier rejected approaches as investigation
history, without fabricating their missing raw measurements. They must be measured
again before a future admission changes their disposition. Related published
convergence work remains available in
[M3JDK21 #1577](https://github.com/hsoliwal/M3jdk21/pull/1577) and
[Synexia #12576](https://github.com/hsoliwal/com.synexia/pull/12576); those are
separate donors and their receipts are not this packet's tests.

The comparator exception-origin defect in bounded navigation remains open under
the exact callback-schedule requirement. The prior boundary-anchor alternatives
change which comparator calls occur. The readonly linear-traversal and fail-fast
experimental modes are not silently promoted. This pass does not declare a
complete collection/compiler family, full jtreg matrix, retained-heap comparison,
whole-build performance result or cross-platform native acceptance.
