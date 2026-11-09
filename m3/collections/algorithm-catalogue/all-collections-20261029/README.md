<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 collection source-inventory and serial convergence

Frozen baseline: `8c8dd70e659b99c23a2d0e7daeb2adadf95429c2`.
Exactly **298 Java files** under `m3/collections/src/main/java/com/m3/collections/`
are listed once with source Git object SHA-1, byte size, and name-derived
algorithm-family review cue.

This is an **inventory**, not a claim to have refactored or benchmarked every
file. Categories are filename heuristics and must be validated against actual
source code and public/JNI contracts before applying a recipe. Legacy names
are preserved while consumers depend on them; record canonical M3 names and
aliases before any relocation. Third-party source intake still needs individual
licenses/provenance, not copied LeetCode solutions.

Already open receiving PRs have ownership notes in the TSV. The two files
under this change are `M3DisjointIndex` (validated-index union-find) and
`M3LongHashSet` (atomic replacement-lane publication); their reusable
recipe resides in [Synexia #14097](https://github.com/hsoliwal/com.synexia/pull/14097).
The preceding queue tranche remains [M3JDK21 #2135](https://github.com/hsoliwal/M3jdk21/pull/2135).
Avoid rebasing/force-pushing either side of this recipe-driven stack.

Run `python3 m3/collections/verification/verify_complete_collection_inventory.py`
from the Git repository root. The verifier reads Git objects only; it never
rewrites any source or changes Git refs. New files after this frozen baseline
require the next append-only inventory revision.

Name-derived family breakdown:
- bitmap-state: 32
- hash-cache: 21
- indexed-view: 20
- map-table: 35
- native-backend: 7
- other: 58
- progress: 9
- queue: 22
- sequence: 37
- set-bag: 18
- tree-index: 20
- view-interop: 19

## Workflow for each source

Record the API/contract, index/mask/lane representation, donor lineage,
algorithm category and benchmark shape. Execute a hash-pinned source recipe,
test a deterministic reference model and failure paths, compare unchanged
public/protected/JNI contracts, and run Java21 compile/JUnit plus native gates.
Promote only measured non-regressions; otherwise retain the candidate as
review evidence and leave the public owner unchanged.
