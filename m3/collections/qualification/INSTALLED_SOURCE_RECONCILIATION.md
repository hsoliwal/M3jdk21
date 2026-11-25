<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->

# Installed collection source: exact inventory and reconciliation boundaries

The **independent installed-collections** acceptance gate differs from the
green source-built JDK native workflow. [M3 native run 45411453449](https://github.com/hsoliwal/M3jdk21/actions/runs/45411453449)
passed the actual source-built full/link image, 12/12 native CTests, combined
countByMany/GINQ, 5 jtreg, all **119 original provider JUnit invocations**
and the **22 original final stream Maven/Surefire tests**. That is evidence
for its finite selected native pipeline, **not** a global installed-source,
provenance/history or cross-platform qualification.

The original [installed collections job](https://github.com/hsoliwal/M3jdk21/actions/runs/45411453388)
is separately red: it refuses the first `M3WeakHashMap.java` source SHA-256,
before attempting its complete Java/JNI acceptance. Do not replace that
expected hash in isolation.

## Independent fail-closed current-head audit

The actual [workflow 45528200040](https://github.com/hsoliwal/M3jdk21/actions/runs/45528200040)
used the *unchanged* `m3/collections/verify_donation.py` manifest admission,
the historical installed source manifest SHA-256, and **12 original additive
source successor functions** in the exact original order. It checked all
source paths using the original `located()/recorded()` relocation mapping,
recomputed source SHA-256 and full Java source membership, ran its pure-data
self-tests, then deliberately exited **1 / HOLD** on differences. No receipts,
products, tests or vendor licenses were modified.

| Exact original source inventory | Count |
| --- | ---: |
| Current expected installed source rows | 1,957 |
| Expected Java source rows | 1,951 |
| Existing files with correct or mismatching source bytes | 1,846 |
| Existing source files with stale expected SHA-256 | **120** |
| Missing or aliased expected source paths | **111** |
| Physically present Java sources absent from the receipts | **361** |
| Declared Java paths absent from current source roots | **111** |
| Aliased/symlinked source paths | **0** |

The first 120 SHA-256 differences break down as **102 main Java owners**
and **18 test Java owners**. The 111 Java declarations missing from the
current roots are **104 main / 7 test**. The 361 unlisted current Java
sources are **114 main / 247 test**. These categories **overlap** (a missing
declared Java source also appears in the missing-file category); their counts
must not be added as if each were a separate source file.

The original gate's first failing `M3WeakHashMap.java` source has expected
SHA-256 `0d2f9f30efc83a1374231cf1d4007e06efb9e860a306979fa417a3531a5e6d06`
and observed SHA-256
`aae6c0ae77e1c7754bc63ddd84d8ef0cf8ef82dceaabedb21292c91d6ffec745`.
This is only **one** of the 120 existing-source mismatches.

The source audit's subsequent refinement records every missing historical
expected SHA-256 and every unlisted current Java source's actual SHA-256 in
separate `missing_source_preimages` and `unlisted_java_postimages` arrays,
without admitting those values. The 120 changed-source records already
contain both hashes, yielding **592 candidate change/removal/addition
records** for separate review (120 + 111 + 361), subject to the same
source-location/alias refusal. Do not treat this as 592 independent API
breaks or approve any of these source transitions automatically.

The complete original machine-readable report and exact checkout identity
were uploaded as [artifact 14983502633](https://github.com/hsoliwal/M3jdk21/actions/runs/45528200040/artifacts/14983502633).
The gate's current head was
`dfdd8cbe90d7ecd0e104f006168ff51a84594264`.
The original installed manifest SHA-256 is
`88ee9ceaa346a8fc2a24a84569ba852f5ad450dea26fe414d0095c31395272f7`.

## Convergence pattern: suggested but not yet admitted

An independent read-only grouping of this actual artifact identifies
**103 possible** same-directory `MatFoo.java` → `Foo.java` name pairs
among the missing and unlisted source sets. This pattern is consistent
with the planned donor-name convergence, but **matching names do not prove**
equivalent Java source, API, implementation behavior, licensing or history.
The revised standalone audit now includes these matches as
`candidate_mat_prefix_renames` with the mandatory
`UNVERIFIED_SOURCE_SEMANTICS` label. It does not change any receipt,
accept a rename, or reduce the HOLD counts. There are **eight remaining
unmatched declared-old Java paths** and **258 other unlisted current Java
paths** that require independent categorization.

A canonical successor must distinguish the original **renamed**, **truly
removed**, **changed in-place**, and **new** source owners from each other.
Preserve earlier source histories, source attribution and project license
boundaries, and identify independently compiled/qualified Java targets
before any retirement or aggregation. Do not assume a deleted class is
unused just because the file is no longer present.

## Required receiving procedure

1. Use the exact report and recorded source preimages, not a guessed current
   repository shape. Verify SHA-256, Git blobs, and source-set identity.
2. Inventory each of the 103 candidate name pairs and the other 266
   unmatched/missing sources with full source, API and donor-history checks.
3. Produce a **new** canonical Synexia source-sealed successor with
   explicit, reviewed `before → after`, `ABSENT`, and `REMOVED` transitions.
   Retain every original historical receipt and original production owner.
4. Execute original Java 21 compilation, full donor differential/API tests,
   JNI and packaging, selected jtreg and the complete installed Maven
   acceptance, including bounded refusal and mutation cases.
5. Allow source-identity `PASS` only after a real rerun shows no missing,
   changed or unlisted owners. Do not suppress the original
   `verify_donation.py` gate or claim success from an empty test count.

**Disposition: HOLD.** Neither the historical source receipts nor the
original installed-collections acceptance has been repaired by this audit.
Current partial native successes remain real but limited to their reported
exact commits and executed methods.
