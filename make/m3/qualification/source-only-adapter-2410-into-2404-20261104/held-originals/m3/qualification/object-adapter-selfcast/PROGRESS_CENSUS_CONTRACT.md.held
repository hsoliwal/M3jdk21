<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->

# Progress receiver current-source census — fail-closed companion

## Scope and authority

This is a read-only **diagnostic** for the historical
`m3/compatibility/check_object_adapter_progress_receiver_20261017.py`
checker. That original checker, its five history manifests, and all public
Java/JNI APIs remain unchanged. The census is a separate evidence lane, not a
new acceptance path. In particular, a successful census must **never** be
substituted for a failing historical progress or JDK/JNI release gate.

Receiving parent: M3JDK21 draft
[#2394](https://github.com/hsoliwal/M3jdk21/pull/2394) at exact head
`372f1b25848bcad8e08897f7f470e63ea610b853`.
Its `Object.java` result-only source, from existing Synexia recipe
[#15439](https://github.com/hsoliwal/com.synexia/pull/15439), is Git blob
`c703c72bea6e792a378bb6d3babc96f22dbf6cb4` and SHA-256
`bd21f3da2ff9f8365576946cfe2d5b5557acaace76de2146277e9d1baa436bd4`.

## The observed discrepancy

The historical verifier composes *five immutable manifests* to 14 expected
target SHA-256 digests. At the pinned source:
- **6/14** current receiving file contents match the historical digests.
- **8/14** have a different current digest: `Object.java`,
  `Runnable.java`, `ExecutorService.java`, `ProgressMonitor.java`,
  `ProgressMonitors.java`, `Consumer.java`, `Function.java`,
  `Supplier.java`.

The diagnostic TSV records exact path, expected historical SHA-256, current
observed SHA-256, and current Git blob SHA-1. Those are **observations**,
not proof of a legitimate original-to-current transition, runtime equivalence,
or a new admitted recipe postimage. A future source-sealed successor must
reconstruct each mismatched file's provenance independently.

## Invariants

1. Reuse the original checker's `current_targets` manifest-composition
   implementation and its existing manifest SHA-256 checks. Do not duplicate
   its transformation authority or edit any historical file.
2. The census is an exact, sorted **14-row closed set** in a single TSV. Its
   embedded SHA-256 seal must agree with the file on disk before interpretation.
   Each path must be canonical and each digest lowercase 64-digit hexadecimal.
3. For every row, the historical digest must match the original checker's
   composed digest, and the observed digest must match the actual regular file.
   Preserve original symlink/traversal rejection and refuse missing files.
4. The current Git blob identity is provenance metadata; the census verifier
   must not infer SHA-1 equivalence from SHA-256 or substitute one hash for the
   other. Git blob validation is a separate GitHub readback check.
5. All 14 rows must be reported deterministically and no history, API, JNI,
   HotSpot, `Object` layout, public signature or native symbol may change.
6. A census success prints **OBSERVED_ONLY** and the mismatch count.
   It deliberately **does not** print progress-release PASS.
7. Tests must show an exact synthetic census passes, a mutated target refuses,
   a corrupted row or manifest refuses, a symlink refuses, and an omitted row
   refuses. No tests may weaken the real historical verifier.

## Category-first donor decisions

For this **hash and source-custody** problem the canonical donors are the
existing Synexia/M3 manifest composer, OpenJDK `java.lang.Object` and SHA-256.
LeetCode 706 and HackerRank hash queries inform adversarial hash-table
categories; GeeksforGeeks hashing tutorials are conceptual. None supplies
source provenance or authorizes replacing a cryptographic source seal. No
third-party challenge solution bodies are copied or needed.

## Required verification order

Inventory -> this contract -> proposed diagnostic source -> Python syntax/lint
-> focused synthetic tests -> full receiver readback and digest check ->
existing historical gate (expected **FAIL until valid lineage is admitted**) ->
matched source-built Java/JNI runtime -> higher release gates. Do not declare
an optimization or general runtime improvement from the census.

All modifications remain inside the review branch; no merge is authorized.
