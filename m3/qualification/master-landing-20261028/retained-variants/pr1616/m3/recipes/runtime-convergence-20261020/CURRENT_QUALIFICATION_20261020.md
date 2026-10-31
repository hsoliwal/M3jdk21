<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Received callback, filtered-view and mask refinement: 20 October 2026

The **29 declared product files passed their receiving Java/JNI and Eclipse
qualification** on M3JDK21 parent `2779aba3f98f31c324be43f731be7914e1a05935`.
This is the current result for the frozen recipe source published in Synexia
commit `7ef4f0eb76eedebf81cdcfed4dacf2e908c2c8b5`.

Earlier `README.md`, `SOURCE_PROVENANCE.json`, pass-02 catalogue rows and correction
events describe their capture-time state, including pending checks. They remain
unchanged historical evidence. This dated supplement records the subsequently
executed receiving results; it does not rewrite a failed attempt as a success.

## Exact scope and execution

The canonical recipe is `com.synexia.m3jdk.RuntimeConvergence20261020` in
`synexia-openrewrite-recipes/recipe-crates/m3-runtime-convergence-20261020`.
Its 55 bound inputs, four reused SDK engine/support sources and 29 result seals
are unchanged. Actual Maven/OpenRewrite tests passed all five qualification
tests, including replay, fixed point and refusal. The unchanged receiver applied
29 files serially, then applied zero changes on a second execution; its final
check also required zero changes. No production output was manually patched.

The receiving parent includes all 473 upstream path changes since the earlier
receiving-parent review. Every original target preimage still matched; no
upstream source was replaced merely to recover an older base. Exact source maps
are in [RECEIPT.json](evidence/receiving-20261020/RECEIPT.json) and
[COLLECTION_PROOF.json](evidence/receiving-20261020/COLLECTION_PROOF.json).
The latter accurately records a dirty worktree during testing: the received
sources were tested before the publication commit.

| Executed gate | Actual result |
| --- | --- |
| Canonical Maven/OpenRewrite qualification | 5 tests, zero failures/errors/skips |
| Owning collections, Java | 485 tests, zero failures/errors; two native-only skips |
| Owning collections, native required | 485 tests, zero failures/errors/skips |
| Owning module source build | 270 production and 267 test Java files; 512 packaged classes; runtime dependency `java.base` only |
| Existing JNI libraries | All ten built and exercised; generated headers, strict C17, fatal UBSan and `-Xcheck:jni` |
| Eclipse core runtime | 16 tests against real Eclipse dependencies, zero failures/errors/skips |
| Eclipse framework | 21 tests including real Equinox 3.24.300, zero failures/errors/skips |
| Exact current ArrayList/Vector/internal compactor source patch | 199,294 independent array/mask oracle cases |
| Supplemental index/order/window direct fixture | Java: 2,990,400 checks; native: 3,267,362 checks; same observation digest |
| Existing PCollections donor source build | 117 tests, zero failures/errors/skips |
| Existing M3 PCollections module | 4 tests, zero failures/errors/skips |
| Existing collection module pack | 3 tests, zero failures/errors/skips |
| Existing PCollections view direct fixture | Missing-library refusal: 6 assertions; required-native parity: 159,106 assertions |

Counts overlap between enclosing suites and direct fixtures. They are not a sum
of unique cases. The Java/native index-window digest is
`e15c399b7be3d8c0fbd38bc6e2cd4c63b93bb0b95d1272bce9acdcc7bec5445b`.
Original logs, parsed suite records and source seals live under
[`evidence/receiving-20261020`](evidence/receiving-20261020).

## Contracts and algorithm decisions

Filtered cursor mutation now preserves provider invalidation for normal,
exhausted, empty and nested ranges while retaining the existing writable
snapshot-iterator fallback. Read-only sorted endpoints refuse consistently;
reversed views remain backed by their providers. These are disclosed defect
corrections, not a claim of equality to the defective baseline.

Known sorted reversal uses stable equal-value run reversal in O(n), retaining
one final primitive index array and shared payload ownership. Arbitrary ordering
retains the existing stable merge. Java/C compaction reuses private checked
window-emission atoms and existing JNI signatures. The C owner avoids payload
region reads for wholly rejected chunks. The retained function-table fixture
measures payload reads 4 -> 0 / 1 / 4 for empty / sparse / dense selection with
state reads unchanged at eight. These are work counts, not CPU or memory gains.

Progress callbacks use guarded begin/finish transitions and preserve fractional
work from successful nested callbacks when an enclosing callback fails. OSGi
lifecycle state retains registration/context ownership through callbacks and
deferred stop, with external callbacks outside the lifecycle monitor. The
callable API is preserved. Explicit endpoint overrides change declared-method
reflection relative to inherited methods; the API evidence discloses that fact.

The speculative direct JDK threshold-exit candidate remains **rejected** by the
recorded cost gate. ArrayList, Vector and the internal JDK compactor retain
their exact production bytes; only the independent oracle is added there.
No second payload owner, scheduler, recipe engine or JNI object-reference store
is introduced. M3JDK21 remains the integrated SDK product; Synexia owns the
reusable convergence recipe and this evidence.

## Related open work and remaining gates

The separately authored Synexia [PR 12599](https://github.com/hsoliwal/com.synexia/pull/12599)
and M3JDK21 [PR 1598](https://github.com/hsoliwal/M3jdk21/pull/1598) /
[PR 1599](https://github.com/hsoliwal/M3jdk21/pull/1599) have different recipe and
product frontiers. [The pinned comparison](evidence/final-review/SIBLING_SERIES_20261020.md)
identifies four identical and six differing product intersections. The competing
native payload-span candidate and combined framework source set require a
forward composition recipe and new matched qualification. This packet does not
merge, overwrite or claim to supersede that series.

An additional run of the existing five-family interop test module failed at
compilation on a compound `var` declaration. The actual failed log is retained
as `evidence/receiving-20261020/interop.log`. A separate recipe follow-up owns
that test investigation, including donor-contract mismatches; this packet
does not claim that module passed or that any whole provider family is admitted.

The full source-built M3JDK21 image was attempted and stopped at missing
`autoconf`; other image prerequisites and workspace capacity are recorded in
the existing image investigation. Full-image/jtreg, interpreter/C1/C2/GC/CDS,
all-platform JNI, installed SDK/GUI, and comparative CPU/retained-heap/native
memory gates remain distinct and unfinished. Existing file/NLP/search and
SWT/Nebula source inventories do not establish their complete integration.
The global String -> arrays -> collections -> AST/compiler -> remaining
families sequence remains at String qualification. No phase is promoted.

## Reuse and improvement memory

Selected donor revisions, copied/adapted files, original rights and rejected
alternatives remain in `provenance/`, `catalogue/` and `licenses/`. GitHub donor
code is revision pinned. LeetCode, HackerRank and GeeksforGeeks nominate problem
categories; their availability is not treated as a copying license.

The 16 immutable recipe-owned correction events retain their triggering failures,
behavioral atoms, reusable patterns, structured annotations, before/after seals,
fixtures and capture-time verification. The later receiving observation in
[`memory/observations/RECEIVED_20261020.json`](memory/observations/RECEIVED_20261020.json)
links those events to the actual receiving evidence. It is a dated observation,
not a new event under the earlier closed capture schema. The pass-03 catalogue
supplements pass-02 without modifying historical decisions or source rows.
