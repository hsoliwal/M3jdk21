<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 release queue

Release preparation has a visible home for intermediate results and unresolved
work. Generate `current/admission.json`, `current/README.md` and `current/SHA256SUMS`
with the commands in [the release guide](../README.md#inspect-readiness-and-publish-the-work-queue).
The current report is tied to the inspected Git commit/tree. CI uploads these
files and exposes the Markdown report in its run summary, including failures.
A saved report is a snapshot; rerun it after any source or receipt changes.

## Queue sources

| Work | Existing authority | Disposition |
| --- | --- | --- |
| Release build/test/Java/JNI/UTF-16 proof | [release-contract.json](../release-contract.json) | Every mandatory gate needs a matching execution receipt |
| Package/class/method naming | [name-plan.json](../../tooling/name-contract/name-plan.json) and [name-mapping.json](../../docs/name-mapping.json) | Queued proposals stay excluded until contract and caller closure are proved |
| Recipe custody | [AGENTS.md](../../../AGENTS.md) and [porting invariant](../../docs/m3jdk21-porting-invariant.md) | Preserve historical copies and callers until executed Synexia cleanup; retained recipe source blocks final release |
| Historical code loss and branch conflicts | [retained_source_queue.py](../retained_source_queue.py) plus existing Synexia PR-stack/history recipes | Compare actual trees and source atoms; merged ancestry alone does not prove recovery |
| Optimizations and donor candidates | Canonical Synexia algorithm catalogue and serial recipe crates | Compile/JUnit/JNI parity precedes promotion; cold/warm and memory measurements precede performance claims |

The naming table above links the existing queue. It does not duplicate its IDs,
change Java public names or authorize a package/class/method rename.

## Intermediate output and final claims

`current/` is generated and ignored by Git. Retain exact reviewed snapshots and
raw logs in the canonical Synexia task evidence and CI artifacts. The queue
keeps failed and unexecuted work visible; it never converts a missing result into
PASS. Product release/tag creation remains a separate action after all gates.

The October 29 preparation exposed a false-positive readiness result in the old
verifier: it checked policy files without consuming mandatory execution evidence.
The regression suite now requires absent, stale, failed or malformed receipts
to keep `release_ready` false. Its positive synthetic fixture tests the verifier,
not M3JDK21 runtime readiness.

## October 29 validation and custody queue

The [validation snapshot](2026-10-29/validation-summary.json) records the actual
array native and collections runs with their original source pins. The
[collections failure catalogue](2026-10-29/collections-failures.tsv) lists the
eight remaining focused-run assertions and contract decisions. The focused
JNI rerun covers only the previously failing classes; it does not replace a
full-suite result. Raw logs, commands and module input hashes remain in the
canonical Synexia task evidence linked by the snapshot.

The workflow's canonical Maven control gate requires an exact executed
`M3_MAVEN_CONTROL_REACTOR` receipt and valid policy. Missing evidence is HOLD.
Historical target-local recipe executors remain preserved migration debt and
are not invoked by this release workflow. Foundation, precompute Java/JNI and
full-family product checks still run; naming and retained-source queues remain
active. No gate is waived while recipe custody is reconciled.
