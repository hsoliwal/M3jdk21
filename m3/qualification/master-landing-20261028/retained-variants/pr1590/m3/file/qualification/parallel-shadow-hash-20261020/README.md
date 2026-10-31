<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Bounded parallel observation receipt

Canonical recipe and correction memory: [Synexia PR 12610](https://github.com/hsoliwal/com.synexia/pull/12610), exact commit [`0521053c44d3cb91db87e2bf7b54df827b1b2a4b`](https://github.com/hsoliwal/com.synexia/tree/0521053c44d3cb91db87e2bf7b54df827b1b2a4b/synexia-openrewrite-recipes/tasks/m3-file-parallel-hash-20261020). Recipe name `com.synexia.m3.M3FileParallelHash20261020`; crate `m3-file-parallel-hash-20261020`. Existing M3FileTree/shadow-stream receiving base: `4d9f189b3516b85ae9bde28d8779e89fcdf75bf6` (PR 1601, stacked on PR 1590).

The four Java files were materialized by the unchanged canonical OpenRewrite engine. `SOURCE_RECEIPTS.tsv` records exact pre/post SHA-256 and the canonical commit. This directory is a target receipt; the reusable recipe, rejected candidates, algorithm catalogue and append-only correction memory remain Synexia-owned.

`M3FileTree.hash(entries, maxBytes, progress, provider, parallelism)` feeds a bounded number of concurrent one-pass shadow observations from the existing filtered traversal. Completed results waiting for earlier rows count against the bound. The caller alone commits hash/failure lanes in traversal order; workers receive private child monitors with serialized parent callbacks. The operation joins workers on cancellation/interruption/failure. The first parser/provider error remains primary when sibling reads are cancelled.

```java
try (M3FileTree tree = M3FileTree.open(root)) {
    int hashed = tree.hash(tree.walk().types("txt", "json", "bin"),
            64L * 1024 * 1024, progress, provider, 4);
}
```

A selected directory is an existing subtree walk. Lazy traversal only sees scanned rows. The caller must own the stable tree exclusively; parallel sources/providers must allow independent concurrent reads and fresh shadows. Uncooperative I/O can delay termination. Existing attributes and provider change checks remain heuristic unless content is observed. No new JNI API, memory owner, parser, progress type, filesystem mirror or runtime dependency is introduced.

Executed on Linux x86_64, Azul Zulu 21.0.10, Maven 3.9.11:

- Real OpenRewrite 8.17.1: 4/4 SDK cases, all 16 partial application states, fixed point, exact output, drift/missing/duplicate/non-Java refusal and memory profile validation.
- `mvn -o -B -ntp -f m3/file/pom.xml clean test`: 60/60, no skips.
- `mvn -o -B -ntp -f m3/file/pom.xml -DargLine=-Xint -Dtest=M3FileParallelHashTest,M3FileLazyTest,M3FileShadowTest test`: 21/21, no skips.
- Public `javap` comparison: all previous signatures retained; only the new overload added.
- Independent review found no unresolved Important/Critical issue after the interpreter-discovered failure-masking correction.

Failures are retained in the canonical packet: baseline 53-case suite exposed a directory-order-dependent test; missing API compiled RED; one SDK diagnostic expectation was corrected; candidate-v2 passed the full suite but failed interpreter error propagation; an application guard refused an unexpected source appearance before recovery from an admitted partial state. Final receipts distinguish each failure from a successful execution.

Deliberate compatibility choices: the lazy test now verifies exact rank deltas, paths and facades for every existing row regardless of filesystem enumeration order. Serial observations allocate one result record. Unchecked structure-hash failure leaves prior lanes intact rather than partially publishing new values. No measured performance improvement is claimed.

Deferred minor: a dedicated generation-change abort regression is absent; the implementation has the guard and the API excludes concurrent tree mutation. Multi-drive discovery, concurrent lazy-directory expansion, watchers, downstream delta-ledger integration, full installed JDK/JNI/platform matrices and performance/retained-memory gates remain NOT_RUN/HOLD. This later-family proving-ground receipt does not advance the mandatory M3 phase order or claim whole-system indexing completion.
