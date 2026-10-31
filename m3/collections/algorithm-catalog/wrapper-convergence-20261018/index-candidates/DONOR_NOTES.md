<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Donor evaluation and index qualification notes

The selected source donors are the existing first-party Synexia/M3 owners.
`CATALOGUE.tsv` records the primary sources, immutable revisions, source/license
blobs, problem categories and candidate decisions. Its external rows are
comparison evidence; no third-party implementation body was imported in this
packet. This extends the enclosing crate's catalogue evidence and is intended
for parent consolidation, not a separate authoritative algorithm database.

## Selected Java atom

The historical projection in [M3jdk21 #1164](https://github.com/hsoliwal/M3jdk21/pull/1164)
and the canonical template in
[Synexia #12156](https://github.com/hsoliwal/com.synexia/pull/12156)
share Git blob `114d6bff738ffeee0c432637538b482ef856e60c`. The current owner has
retained same-direction reuse, direct mask-range filtering and the newer
`M3LongAddressKernel` routing, but the private group-reversal atom is absent.

A sorted index consists of contiguous equal-value groups. Reversing the order
of groups gives the opposite signed-long order; copying each group forward
retains the physical occurrence order of equal values. The private `[from,to)`
window determines group boundaries, so slices through a group and filters
within it remain stable. Each index is copied once into one result array. The
candidate's payload comparisons use `M3LongAddressKernel.loadUnchecked`; all
current public addressing, gather, filter and unknown-order merge statements
remain unchanged.

The oracle deliberately uses boxed `Arrays.sort` on physical IDs and a signed
comparator. It never computes expected order by group reversal. It enumerates
every length-zero-through-six sequence over `{-1,0,1}`, both starting sort
directions, every slice of the resulting sorted view and repeated
sort/filter/slice compositions. Separate fixtures include signed extremes and
split tie groups. Native filtering samples actual nonzero offset windows with
the same independent physical-ID selection oracle. Private identity checks
verify the existing one-payload, shared-slice and same-direction/all-selected
Java reuse invariants; public materializations are mutated to check isolation.

## Native alternatives and selected refinement

The historical two-word bitmap/prefix-compaction loop is **rejected on this
base**. It writes each admitted source region separately, while the current
owner batches 128 selected IDs across regions. A one-hit-per-region source
therefore has an output-call regression. A historical smaller-stack receipt
does not establish a net improvement against the current bounded owner.

The selected change keeps the current scan and its batching. Once
`first_match < take` is known, the row transfer starts at `next + first_match`
and contains `take - first_match` IDs. The emit loop reads local slot
`i - first_match`. For every used slot:

`newValues[i - first_match] = rows[next + first_match + i - first_match] = rows[next + i]`.

Because `0 <= first_match < take <= end - next`, the new region is a nonempty
suffix of a validated region, the source offset cannot overflow, and every
local array access stays in range. Rejected prefixes cannot produce output.
No new native storage, callback, array pin, retained pointer or JNI call is
introduced. State reads, predicate decisions, the output flush loop, exception
checks, source/output alias refusal, validation order, cursor packing,
all-pass/contradictory shortcuts and both exports stay on the current code path.
Dense regions with `first_match == 0` transfer the same IDs as before.

The instrumented region fixture links the actual production C owner. It measures
calls and transferred elements through an instrumented JNI table; it does not
replace real JVM/JNI qualification. The independent Java scalar oracle checks
both real exports, raw JNI invalid inputs, all 65,536 required/excluded mask
pairs, every byte-state value, signed/duplicate row IDs, output guards, bounded
and full-remainder cursors, and repeated sparse resumption.

### Frozen region-work expectations

These values are authored expectations, not executed observations.

| Case | Baseline row IDs transferred | Candidate row IDs transferred | Unchanged calls: row/state/output |
| --- | ---: | ---: | --- |
| Reject all 1,024 rows | 0 | 0 | 0 / 8 / 0 |
| Single hit at ordinal 1,000 | 128 | 24 | 1 / 8 / 1 |
| One tail hit in each of 256 chunks | 32,768 | 256 | 256 / 256 / 2 |
| Sparse early output limit of one | 128 | 1 | 1 / 1 / 1 |
| Dense output window of 4,096 | 4,096 | 4,096 | 32 / 32 / 32 |
| Dense early output limit of one | 128 | 128 | 1 / 1 / 1 |
| Bounded `[7,270)` with three tail hits | 263 | 3 | 3 / 3 / 1 |
| Bounded `[3,65)` with last-position hit | 62 | 1 | 1 / 1 / 1 |

The fixture also preserves the original all-pass and contradictory cases and
adds zero work, zero result limit and bounded shortcut cases. Compile the same
fixture against the baseline owner with `--baseline`, and against the candidate
owner with `--candidate` (the default). Running the baseline in candidate mode
should fail specifically at the reduced row-transfer assertion; it is a work
counterexample, not a semantic-failure claim about the baseline.

## External donor conclusions for wrapper passes

Guava's live filtered collections are useful for visible-only bulk mutation,
complete admission validation and nested predicate composition. Their
`contains` predicate can receive the query object, which differs from an M3
contract requiring the stored comparator-equivalent representative. Mutable
predicates and backing changes also prevent an unversioned cached selection
mask from being a generally valid live-view optimization. Filtered spliterators
must not advertise an unproved exact size; Guava's implementation drops size
characteristics while retaining applicable order/sort characteristics.

Spring's internal `FilteredCollection` is a concrete counterexample to a
universal filtered-wrapper contract: its add/remove methods mutate the backing
collection before combining the result with the predicate, and `clear` clears
the whole backing collection. Apache predicated collections instead validate
existing elements and new admissions. Neither is an interchangeable template
for a visible-only or strictly read-only M3 wrapper.

Guava and Eclipse Collections demonstrate immutable sorted storage with range
metadata and binary endpoint search. Those set implementations impose unique
comparator keys, while `M3LongIndexProjection` retains every duplicate physical
occurrence. They are range-view references, not replacements for the current
projection owner. Eclipse's verified EDL is the BSD-style alternative recorded
in its notice; any future source copy must retain the actual conditions,
copyright and disclaimer. Apache/Guava/Spring copies similarly require their
actual Apache notices. No such external copy is selected here.

LeetCode Remove Element allows reordering and therefore cannot justify
swap-with-last compaction of an ordered M3 index. Move Zeroes provides a stable
compaction problem shape. LeetCode Search Insert Position, HackerRank Lower
Bound and the GeeksforGeeks lower/upper-bound article supply endpoint-search
categories; their constraints and presentation conventions do not define M3's
half-open offsets, exceptions or comparator behavior. The catalogue explicitly
classifies these references as non-code donors.

## Execution handoff

The five template paths and exact before/after hashes are in `POSTIMAGES.tsv`;
`SOURCE_RECEIPT.json` records the contract at freeze. Product source preimages
were read back and still matched when these candidates were frozen. This
subtask ran no compiler, test, benchmark, native library or recipe application.

The standalone Java proof supports `--java`, `--native`, `--missing` and
`--old-native`. The last mode expects an available legacy mask library without
the bounded symbol: legacy work succeeds, positive bounded work refuses, and
the existing zero-work shortcut remains as documented. Missing-library mode
checks that Java shortcuts do not bypass the explicit native requirement.
The new proof's observation digest is independent of native-mode extra
assertions, so exact common observations can be compared between modes and
between baseline/candidate owners.

The resource probe supports `sort`, `mask-java`, and `mask-jni`, with optional
row count and repetition count. It separates cold preparation from warm
operations, reports three thread allocation/CPU/process CPU/time samples, heap
usage after a requested GC, Linux RSS/VmHWM and direct/mapped buffer usage.
These process-memory observations are not isolated native-heap measurements;
unsupported metrics are `-1`. Native C stack usage still requires the parent's
actual compiler receipt. No universal speedup or retained-memory reduction is
asserted from source arithmetic.

The parent owns public/API/JNI header checks, Maven/OpenRewrite compilation,
JUnit entrypoints, serial target application, runtime/JNI/sanitizer execution,
resource measurements, drift/duplicate/refusal/fixed-point proofs, and target
publication. It also owns the canonical provenance ledger rows for new TSV,
JSON/JSONL metadata and template custody. Existing native/product copyright
headers remain intact; newly authored Java proof/probe and documentation use
the canonical Synexia SPDX copyright.
