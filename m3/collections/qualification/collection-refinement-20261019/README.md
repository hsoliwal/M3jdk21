<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# M3JDK21 collection refinement qualification

This packet refines the existing M3JDK21 collection owners: dynamic array slots,
integer links, offsets, state masks, live filtered/sorted wrappers and JNI mask
selection. Public/protected interfaces are unchanged. Runtime ownership stays
in M3JDK21; reusable recipes, exact templates and accumulated decisions live in
the [canonical Synexia packet](https://github.com/hsoliwal/com.synexia/tree/codex/m3-collection-refinement-20261019/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019).

The checked-in `runtime-manifest.tsv` binds the runtime/test/build-support
preimages and postimages. The canonical recipe emits each file serially through
the unchanged manifest admission kernel. This projection has no publication
self-hash: the receiving PR records the exact canonical commit.

## Selected implementation

- Dense linked-list clear fills only the physical used payload/mask prefix,
  retaining the current sparse branch, free list, reversed views and index splits.
- Four filtered terminal scans and primitive filtered counts operate directly
  through current source cursors, retaining callback and exception order.
- Snapshot filtering counts matches during its existing byte-state predicate
  pass. It copies fully selected spans into a fresh index array, skips emission
  for empty selections, and preserves current scalar stable sorting. A filtered
  sort never mutates a parent or sibling directory.
- Empty native compaction repeats state verification without reading row
  regions. Fresh results, drift exceptions, unsigned masks and all current
  compactor exports are preserved. The qualification mask library also links the
  existing mask-window translation unit and checks the combined generated ABI.
- Saturated keyed execution lanes use bounded offers and completion polling.
  A failed worker can report its original exception even when its queue is full,
  and a freed slot can admit work without waiting for a mapper to finish. Global
  admission limits, equal-key processing order and source-order results remain.

The raw current source has duplicate filtered-method declarations. The repair
retains the strict JDK reversed-view guards. Five incumbent test files are
reconciled with current typed-key, high-bit mask, split and bounded live-entry
cache contracts. Paired original failures and corrected observations remain in
the canonical evidence; an uncompilable raw input is not an executable oracle.

## Catalogue and rejected alternatives

Search the [current category ledger](../../algorithm-catalog/collection-refinement-20261019.tsv)
by category, exact shape, owner, source revision or decision. The canonical
`M3ProblemShapeCatalogue` adds the missing linked-index-segments and
bounded-keyed-admission shapes while preserving its original forty entries.
Existing catalogue/invariant components
remain the retrieval and admission owners; this TSV does not silently modify
another catalogue's ingestion behavior.

The segmented linked candidate and two packed predicate candidates remain
deferred with their exact source and resource evidence. Lower allocation alone
did not justify measured CPU costs. The final snapshot keeps byte scratch and
has the incumbent median allocation in the bounded matrix. The linked-clear
2048/4096 refill workloads used less measured CPU; sparse controls and the
16384 refill overlap. These observations are not universal speed guarantees.

GitHub donor pins include first-party Synexia/M3 owners, fastutil and Guava.
LeetCode 707/27, HackerRank Day 15 and GeeksforGeeks bit-count material provide
category comparisons. No challenge/editorial/submission body is copied; exact
rights and contract mismatches are recorded in the ledger.

## Run the current Java and JNI gate

Use an actual JDK 21, GCC and the repository's existing source-built Maven
dependencies. The module POM is unchanged and retains its
`5.10.2-m3-source-1` JUnit coordinates. The independent console is the real
`junit-platform-console-standalone-1.12.2.jar`, SHA256
`329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a`.

From the M3JDK21 repository root:

```sh
python3 m3/collections/qualification/collection-refinement-20261019/verify.py \
  --source-root "$PWD" \
  --probe-plan "$PWD/m3/collections/qualification/collection-refinement-20261019/probe-plan.json" \
  --java-home /absolute/path/to/jdk21 \
  --maven-repository /absolute/path/to/verified/maven-repository \
  --junit-console /absolute/path/to/junit-platform-console-standalone-1.12.2.jar \
  --ubsan \
  --output /absolute/path/to/a-new-result-directory
```

Add `--offline` when the complete dependency closure is already present. The
runner refuses an occupied output directory, implicit Java injection and input
drift. It copies the local module inputs, runs the unchanged Maven POM, then
independently compiles all Java sources/tests and executes real JUnit. It builds
the mask/window and path-mask libraries with strict C17 and optional fatal
UBSan, checks exports against generated headers, runs a distinct JNI-table
fixture, and starts fresh actual JNI JVMs with `-Xcheck:jni`.

`probe-plan.json` declares Java, unavailable-backend and native-required runs.
The checked-in `qualification-summary.json` records the actual qualification
receipt and source pins. The two native-only Java-suite assumptions remain
visible; mask spans execute explicitly in the native plan. The complete
all-native-backends lane and installed JDK/jtreg/JCK gates remain separate.

Resource samplers are separate from correctness gates. Run the two resource
runners with `--help`; both require explicit source/class pins and a fresh
output. `probes/M3PackedSnapshotResourceProbe.java` retains its historical name
for consistent comparisons; the selected v3 owner uses byte-state scratch.
These classpath-only samplers add no production module dependencies.

## Evidence limits

This campaign compiled 268 production and 243 test Java files, three
production C translation units in two libraries, and a separate JNI table test
fixture. It does not assert a pass over every JDK source
or auxiliary repository file. Full Synexia reactor, Java-LST attribution,
installed M3JDK21 image, HotSpot, jtreg/JCK and phase progression remain NOT_RUN.
The existing `SYNEXIA-CATALOGUE-FILE-REFINEMENT-1` receiving invariant is reused.
Publication metadata is emitted after runtime verification; tested code,
native sources, tests and the executable probe plan keep their exact hashes.
