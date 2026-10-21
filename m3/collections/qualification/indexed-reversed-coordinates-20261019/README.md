<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 indexed reversed-coordinate receiver — 2026-10-19

This is a **stacked review** on M3JDK21 PR #1501, not a parallel runtime, renamed JDK, or Synexia dependency. The canonical Synexia build-time recipe lives at `synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3IndexedSnapshotReversedCoordinatesRecipe.java`.

## Exact source admission

- Target preimage Git blob: `06e8f656cad998472d759d9a385592329121cf6d`
- Generated target postimage Git blob: `aa49494b9216757d9547322635328b839c9eb8c3`
- Canonical recipe branch: `m3/recipe-indexed-snapshot-reversed-coordinates-20261019`
- Recipe rule: exact preimage only; already-generated postimage fixed-point; reject all other preimages. Preserve historical receipts, do not rerun older recipe versions over this successor.
- Sequence: inventory → Inline → Atomize → Patternize → Comment → Java 21 compile/tests → optional real JNI parity → review.

## Mechanism

The existing privately owned GC-visible `Object[]` and immutable physical index `int[]` remain the only payload and index owners. A reversed projection stores the existing half-open `from/to` range and one orientation boolean; logical ordinal `i` maps to `positions[to - 1 - i]` rather than copying an index array. Reversing twice restores the same logical values and original physical indices. A reversed subrange `[begin,end)` becomes physical-directory bounds `[to-end,to-begin)` with orientation retained.

The existing `M3IndexPathMasks.select` accepts signed strides. Reversed staged selection uses `first=to-1, stride=-1` when nonempty; an empty reversed slice uses `first=from` and `count=0`. Java and the **existing** C17/JNI backend share this coordinate contract. No Java object, predicate, comparator, or reference passes JNI. Required JNI does not silently fall back when absent.

Predicate filtering invokes Java callbacks exactly once in reversed logical encounter order. Stable sort takes the already reversed index directory, sorting integer physical slots only. No public JDK `java.util` contract or native symbol is changed. The independent `m3/collections` receiver remains separately gated; this proves neither installation into `java.base` nor promotion past the active String stage.

## Verification scope

Focused JUnit `M3IndexedReversedCoordinates20261019Test` covers ownership identity, reversed double pass, nested slices, physical occurrence IDs, null/duplicate adjacency through the canonical class, callback order, stable sorted ties, signed high bit, staged mask order, zero-count negative stride, 240 deterministic random masks/slices, strict mutation refusal, and required JNI parity if available (or explicit refusal if absent). Running JNI-enabled tests with the compiled existing library remains an independent required gate; a skipped native path is **not** JNI parity proof.

Run with Java 21 and the module's established dependency source repository:

```sh
mvn -B -f m3/collections/pom.xml -Dtest=M3IndexedReversedCoordinates20261019Test test
```

Pending outside this bounded PR: whole SDK rebuild, installed-image source/ABI proof, HotSpot/GC/jtreg, native C17 sanitizers and real JNI parity, CPU/heap/native measurements, vendor five-family contract matrix, package/naming admission, and complete M3 String-first phase closure. Do not claim these gates passed from a finite unit test.

## Algorithm catalogue and license

See `algorithm-catalog/indexed-reversed-coordinates-20261019.tsv`. LeetCode, HackerRank and GeeksforGeeks are problem-taxonomy/evaluation donors only. No third-party solution code is copied. The selected atom is original M3 target-owned Apache-2.0 code; preserve OpenJDK and third-party per-file rights.
