<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# M3 progress percentage refinement

`M3ProgressAdapters` previously reported 0% for a completed task whose worked and
total counts both equal `Long.MAX_VALUE`. The original multiplication by 100
overflowed. The retained baseline witness compiles, then fails on that exact
valid state. The generated adapter reports 100% and passes the same witness.

The refinement changes only the existing private percentage calculation and
adds one selftest plus one JUnit wrapper. It reuses the exact checked scaling
pattern already present in the Apache-2.0 `M3ChildProgressMonitor` family. For
positive total work, the result is exact `min(100, floor(worked * 100 / total))`.
Unknown (`-1`) or zero totals keep returning 0. Negative worked counts and totals
below `-1` remain invalid state-constructor inputs. Delegate state is observed
exactly once; the calculation does not mutate the monitor or change adapter
identity, legacy integer saturation, callback failures or public interfaces.

The selected implementation first handles completed/over-completed tasks,
then uses checked long multiplication and the donor's exact `BigInteger`
fallback. The fallback corrects formerly wrapped results. It adds bounded
temporary arithmetic and exception allocation in the overflow case; no
throughput improvement is claimed. The task adds no production field, public
descriptor, JNI entry point or object layout dependency.

## Evidence

The finite M3 closure compiles under Java 21 with `-Xlint:all -Werror`. It passes
53,475 numerical input cases under a 128m heap, including selected percentage
transition boundaries, full-width seeded values and original-formula parity
where multiplication is safe. Each case checks an independent exact oracle,
one delegate state callback and a result in `[0,100]`. Additional checks cover
null and invalid states, exception identity, existing adapter identity and
legacy snapshot saturation. All 12 existing production class public/protected
descriptors and constants are byte-identical in the `javap` comparison.

The dataset `m3jdk21-progress-percent` passed the original canonical host's
5 real OpenRewrite SDK tests, 8 input-sealing cases and 9 receiving cases. The
actual SDK Results were received in an isolated tree; check/apply/fixed-point
reported 3 pending changes, 3 applied changes and 0 further changes respectively.
No generated product file was hand patched.

The original eight-source finite proof is pinned to
`4b24824c3aa09d802121447f8970d442a855f064`. The adapter source was rechecked at
`0efc1719ca5766aead09f4f7c49f5067975bd946` and has the identical original blob.
The clean owning collections module at that newer commit ran 225 tests with
4 failures already present: 3 in `M3LongTreeSetTest` and 1 in
`M3StoredRepresentativeTest`. That full-module baseline gate is **FAIL**;
the independent finite percentage proof does not turn it into a module PASS.
The receiving PR supplies its after-change module result separately. No
failing existing test is disabled or excluded by this task.

## Catalogue and replay

`progress-percentage-foss.tsv` records the selected GitHub donor and comparisons
against unchecked arithmetic, divide-first truncation, floating-point ratios,
always-wide arithmetic and a new native engine. Official LeetCode and
HackerRank problems and the GeeksforGeeks overflow category were reviewed as
problem-discovery references; no unlicensed solution or editorial code was
copied. The existing Synexia/M3 scale owner supplies the reused implementation.

`progress-percentage-public-results.json` binds source and proof hashes. The
task reuses the existing public recipe host under
`m3/arrays/qualification/empty-segments-20261017/recipe-custody`; its location is
historical, while its dataset-parameterized engines and tests are shared.
The progress handoff records the identical original controls and engines and
the qualified configurable 768m Maven test heap. Historical receipts remain
attached to their original epochs. Follow `CUSTODY-progress-percent-20261018.md`
for replay commands with the explicit existing `sdk-owner` path.

This finite task does not qualify every monitor's concurrent state protocol,
the whole collections SDK, all Eclipse facilities or the entire JDK. Threaded
arithmetic samples own separate fixtures. Native and runtime work stays with
its actual owning recipes and gates.
