<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 collection convergence contract

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0

Prepared before runtime source changes, against M3JDK21 master
`2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.

M3JDK21 is the target JDK product. Synexia is the reusable concept and recipe
workspace; it is not a runtime integration layer. This receiving pass changes
existing collection owners and their JNI implementation. It adds no second
collection engine or Synexia runtime dependency.

## Frozen behavior

Public interfaces and inherited Java 21 collection contracts remain unchanged.
Preserve live owner visibility, comparator representatives, null policy, readonly
mutation rejection (including no-op/reversed operations), callback order and
demand, iterator behavior, encounter order, duplicate row IDs, and range bounds.
Do not cache predicate results. Mutable owners still control their lifetimes,
mutation epochs and concurrency requirements.

Storage remains primitive arrays, offsets, indices and state masks. Temporary
arrays of wrapper predicates are permitted; retained per-element nodes are not.
JNI keeps its existing exported symbols, unsigned byte-mask semantics, bounded
region copies, checked geometry, pointer lifetimes and explicit native refusal.
No Java predicates, comparators or object graphs cross this JNI boundary.

## Serial file review and donor selection

Use the pinned PR heads in INPUTS.tsv as donor candidates. Select only their
declared changed owners and tests, never replace the whole tree with an older
branch. In particular, retain the current M3BagRankIndex and its tests, which
are absent from older PR snapshots.

1. #1260: immutable indexed/object snapshots and no-op mutation contracts.
2. #1284: indexed sorted-set algebra navigation and work-bound contracts.
3. #1291: filtered-list predicate/accessor demand and wrapper contracts.
4. #1314: stack-safe collection filter composition, readonly Java 21 reverse
   views, null navigation, stored-key entry membership and path-mask aliases.
5. #1303: truthful lazy spliterators, wraparound deque span copies, and JNI
   selected-payload spans. Merge only these atoms into the #1314 wrapper.
6. #1328: retain its independent null-navigation and alias regression tests;
   its owner changes overlap #1314 and must not replace that newer composition.
7. #1345: retain its stored-representative contracts; compare its entry-membership
   atom with #1314 and record equivalence before choosing one owner.

All runtime outputs must be produced by the existing class-backed Synexia
M3HashPinnedJavaSnapshotRecipe/M3HashPinnedTextSnapshotRecipe engines. Preimages,
outputs, candidate sources and source licenses are sealed. Drift, missing inputs,
duplicates, wrong source kind and second-run changes must be rejected or tested.

## Gates and limits

Record patch/lint, compile, tests, then runtime evidence in order, retaining
failed attempts. Run the combined collection module and native parity where
the environment permits, not merely each donor's historical tests. Compare
Java public contracts and generated JNI headers/symbols. Measure work counts
and JNI payload traffic rather than infer speed from asymptotic complexity.

This is a collection qualification candidate. It does not advance the repository's
STRING receiving phase, certify all 4,770 receiving paths, replace full OpenJDK
jtreg/build gates, or claim that all other open PRs have been merged.

## Source-built test dependency closure

The current source verifiers fail before checking donor bytes because their
repository root is two levels too shallow; their receipt also names a missing
provenance field. Reuse PR #1303's eleven build/provenance-file corrections as
the separate `source-junit-20261018` text dataset. Preserve every donor source
byte, source-set equality check, hash comparison and packaged-license check.
API Guardian's existing upstream LICENSE is the legal owner; package that exact
file and correct its provenance path. Do not substitute stock binary JUnit
coordinates for the module's source-built test dependency closure.

The clean owning build additionally failed to resolve
`junit-platform-launcher:1.10.2-m3-source-1`. The reused driver omitted the
existing launcher source module. Admit it after platform-engine and before
the consumer test run; apply the same reviewed source-verifier correction to
its verifier. Preserve its upstream Java bytes, descriptors and notices. This
raises the source dependency closure from nine modules to ten; the nine native
libraries are unchanged. The failed clean-build log remains evidence.
