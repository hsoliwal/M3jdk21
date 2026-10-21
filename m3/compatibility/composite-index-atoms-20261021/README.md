<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Composite precompute and indexed collection atoms — 2026-10-21

M3JDK21 is the product. This packet refines its existing `java.base` state-relation and
Object[] mask-compaction owners, and its existing primitive `M3SpanIndex` Java/JNI owner.
It does not add an overlay, a Synexia runtime dependency, another text store, a node-per-entry
collection hierarchy, or public API. It is a bounded continuation, not completion of the
entire String/collections/path/AST scope.

## Recipe and convergence ownership

Canonical source and postimage templates live in `hsoliwal/com.synexia`, under
`synexia-openrewrite-recipes`. `M3CompositeIndexReplay` delegates source seals, resource
validation and scan admission to the existing `M3GpuToolJoinManifest` (Git blob
`38df5f97bbeec8a8a8cfc77091d56bda925c07f2`). The thin host checks every input before writes,
then applies each selected file serially. Drift, missing required files, unsafe paths,
occupied additions and bad templates refuse. Exact postimages replay with zero changes.
The host requires an exclusive candidate checkout; it is not a multi-file transaction.
Unsupported atomic rename/hard-link filesystems refuse rather than silently overwrite.

The canonical Synexia `MIndexStateRelation` Java and C kernels receive the same improvements.
Its progress/domain/native-loading wrappers are unchanged. The target's stronger existing
single-transfer-lane JNI wrapper is preserved rather than overwritten by the donor wrapper.
Kernel correspondence is separate from full Synexia module qualification, which remains pending.

## Contract and selected atoms

Sparse relation composition reuses consecutive equal active rows, with only scalar locals.
Native composition visits set bits using a nonzero-guarded ctz and retains validation,
staging, alias safety, dense ceiling saturation, and unsigned arithmetic.

For sorted starts and monotone prefix-maximum ends, the first prefix >= query identifies the
first interval ending at/after the query. Before that index every end is smaller. A final
strict start < query check is therefore sufficient; a second starts search is unnecessary.
Stable ordering, duplicate starts, long ends and detached snapshot semantics remain intact.

The private compaction prepass needs only to decide whether its existing fragmentation
threshold is exceeded. It stops at that proof; payload compaction, GC-safe arraycopy, stable
survivor identity/order and discarded-tail behavior remain unchanged.

## Reproduce finite receiver tests

From a checkout containing the generated packet:

```sh
bash m3/compatibility/composite-index-atoms-20261021/run_contracts.sh "$PWD" /tmp/m3-atoms-proof
python3 m3/compatibility/composite-index-atoms-20261021/work_probes.py "$PWD" /tmp/m3-atoms-work --require-pass
```

Work probes instrument scratch copies only. They are operation counts, not timing benchmarks.
Independent dense relation, linear interval, and reference compaction oracles are retained.
Baseline and every one-file mutation were compiled and executed before the next mutation.
See `receipt.json` for actual counts, environment, and unexecuted gates.

## Catalogue and invariants

`catalogue.tsv` is an additive, grep-searchable per-task slice of the existing Synexia
problem-category catalogue, not a new catalogue engine. Search by category, owner, contract,
donor or pending gate, for example `grep -i 'PREFIX_MAXIMUM' catalogue.tsv`.
It records selected/rejected/reference-only alternatives and unimplemented conversation scope.
LeetCode, HackerRank and GeeksforGeeks provide category evidence only; no challenge, editorial
or submission bodies were copied. The exact existing Synexia and target sources are the donors.

Preserve inventory -> dated additive convergence -> category catalogue -> Inline -> Atomize ->
Patternize -> Comment -> compile/behavior/ABI/refusal/fixedpoint -> review. No forced inlining or
arbitrary helper fragmentation was needed: the existing file-local atoms remain their owners.
The existing AGENTS/porting invariants remain authoritative. No receiving phase is advanced.

## Rights and disclosure

All original per-file headers are retained. First-party recipe/tooling and Apache fixtures
retain Hitesh Soliwal/Synexia attribution; GPL/ClassPath source and fixtures keep their own
rights. A recipe's licence does not relicense its templates or generated target sources.
This pass maintains already-public numeric and collection atoms; it does not publish new
private invention-specific path/interner layouts. Existing `src/**` donor-intake/licensing
review obligations remain open; this finite test packet does not waive them.

## Open gates

The PR is draft. Source-built M3JDK21 image/jtreg/HotSpot, actual Maven/OpenRewrite SDK,
whole-module/reactor, cross-platform native, comparative wall-clock/heap/native memory,
provider-wide views, licensing/provenance and receiving-order gates have not all run.
A patched stock JDK and a finite green corpus must not be called complete M3JDK21 integration.
