# M3JDK21 array/index/mask, filtered navigation, and native JNI convergence

Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0.

M3JDK21 remains **one integrated Java21/HotSpot/JNI SDK**, not an M3
layer or adapter to a separate JDK. This reviewable draft is stacked on
[one-read M3File/V2 SQLite receiver #1782](https://github.com/hsoliwal/M3jdk21/pull/1782)
and receives twelve *existing source-qualified* Java/C and test files
through separate serial GitHub commits. No alternate collection backing
Object[], tree, native state owner or filesystem/database is installed.

## Original receiver and canonical recipes

| Existing owner | Selected change | Canonical donor/recipe |
| --- | --- | --- |
| M3TreeMap / M3TreeSet / M3FilteredViews / M3MutableFilteredViews | Distinguish ordinary endpoint rejection from user-comparator exceptions; preserve exact stored representative, bounding, callback and backing semantics | [M3JDK21 #1763](https://github.com/hsoliwal/M3jdk21/pull/1763) / [Synexia #12860](https://github.com/hsoliwal/com.synexia/pull/12860) |
| M3ArrayifiedIndexOps | Primitive-gather, detached rotation, owned JNI selector ordinal remap and stable sorted-merge tail copies | [M3JDK21 #1760](https://github.com/hsoliwal/M3jdk21/pull/1760) / [Synexia #12857](https://github.com/hsoliwal/com.synexia/pull/12857) |
| Original jdk.internal.m3.M3IndexMaskWindow and libjava C JNI | Count state predicates without allocating selected-row IDs; preserve both original scan exports and require separately proven count capability | [M3JDK21 #1761](https://github.com/hsoliwal/M3jdk21/pull/1761) / [Synexia #12856](https://github.com/hsoliwal/com.synexia/pull/12856) |

**All seven production files' exact preimage Git blobs matched**
the selected original target before receipt. Five new test/resource
sources had an ABSENT preimage. Every donor postimage was verified
against GitHub readback. Source paths, blob IDs, donor PRs and
canonical recipe IDs are in `RECEIVED_DONORS.tsv`. Its read-only
`verify_received_blobs.py` refuses drift without modifying
the SDK. Original donor Maven/OpenRewrite recipes, rather than a
new wrapper or an unqualified direct rewrite, remain the source
of the target changes.

## Algorithm catalogue and contract invariants

- **Collections:** one original parent-owned Object[] / primitive-array
  payload; physical index/offset lanes and bit masks retain filter,
  sort, insertion policy, comparator, exception and observable
  iteration contract. Do not add nodes/wrappers for every row.
- **Native:** batch primitive array/mask operations through original
  jdk.internal.m3 + libjava JNI. Do not retain GC pointers, call
  per-element arbitrary Java predicates or assume native is
  faster for small workloads. Preserve legacy binary entrypoints,
  stable ordering, duplicate logical positions and explicit
  native capability refusal.
- **Recipes:** categorize donor problem (binary search, stable
  merge, array compaction, bitmap count, masked selection) then
  compare correctness, time/space/JNI crossings, licenses and
  regression receipts. The original GitHub/Synexia source is
  first-party Apache-2.0 where identified. LeetCode,
  HackerRank and GeeksforGeeks are problem taxonomy only; no
  competitive solution or unlicensed source was copied.

## Evidence status and mandatory holds

The original donor PRs report scoped execution that was
**not re-run in this combined branch**:

- **Comparator donor #1763**: 8 canonical recipe tests,
  6,590 ordinary navigation cases, a 3,509-case comparator
  injection suite and an independent 26,412-position probe;
  the original positive exception-identity and byte-result
  receipts are preserved. The donor hosted CI hit a naming
  census failure *before* its Java steps; not a clean suite.
- **JNI mask donor #1761**: Java21 + C17 `-Xcheck:jni`;
  16,777,216 mask truth cases and 86,000 count cases; native
  symbol/capability and legacy-library tests. Actual full
  source-built HotSpot/libjava/JMOD integration remains pending.
- **Index donor #1760**: saved Java21/C17, runtime/legacy-mode,
  JNI/resource receipts; native allocated-byte improvements
  for selected benchmark sizes, **mixed CPU timings including
  regressions**. The original packet was not rerun on current
  master and has missing complete JNI header/run receipts.

The new **combined branch** has source-integrity admission only.
Actual canonical OpenRewrite rerun/fixed point, full collections
Maven/JUnit, native C17/JNI parity, full M3JDK21 image/jtreg/JCK,
cross-platform and CPU/heap/native-memory tests remain
unexecuted or unverified. Do not merge or claim blanket
performance/superset API admission on source custody alone.

The parent #1782 one-read M3File/V2 scanner and its profile
SHA, no-symlink and original SQLite observed-vs-successful
protocol are unchanged by this receiver. This is a
**separate, reviewable stacked draft**.
