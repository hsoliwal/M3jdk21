<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Primitive collection core source-pin receipt (draft)

Canonical recipe: https://github.com/hsoliwal/com.synexia/pull/14097
Canonical source commit: 1221d9c93c5e5f6b8cc53e31684751c43f7c88e0.
Preimages at master:
- M3DisjointIndex: 44215f4c8e31770786c67066878df4e8d41d3ac0
- M3LongHashSet: c2f04bb08698169862180e3759146e31c60d84b5

Both product owner files are exact byte-for-byte copies of the canonical
source-sealed Java templates. API names, public/protected signatures, JNI
contracts and downstream runtime dependencies remain unchanged.

The set rehash stages aligned arrays before publication and checks rebuilt
cardinality; the disjoint-index change reuses validated indices rather than
rechecking them during connected/union while preserving full compression.
Regression tests include seeded independent models, zero/extreme keys,
tombstone compaction, array-lane invariants and revision observations.

Qualification pending: actual OpenRewrite SDK execution, strict full Maven
collection test suite, source-built M3JDK21/jtreg, native/platform validation
and representative performance measurements. This draft is not a release gate.
