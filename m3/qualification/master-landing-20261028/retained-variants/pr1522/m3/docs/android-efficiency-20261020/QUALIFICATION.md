<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0 -->
# Receiving metadata qualification

Canonical commit `517d49739dca95ba5caad4a3f14f94171a984b71`
([PR #12564](https://github.com/hsoliwal/com.synexia/pull/12564)) supplies the
exact SDK outputs received at immutable M3JDK21 runtime commit
`329cd9116eca5a3b2ec081fb6760ca3e6508da7e`
([PR #1572](https://github.com/hsoliwal/M3jdk21/pull/1572)).
[PROVENANCE.json](PROVENANCE.json) records their identities and retained rights.

## Actual validation

The unchanged current `migration.py validate` and schema ran with isolated
`jsonschema==4.26.0` and `--previous` the untouched original name-map.
The first attempt returned **exit 0**, **47 stdout bytes**, and **empty stderr**:

```text
MIGRATION_MANIFEST_VALID completion=INCOMPLETE
```

The [command](evidence/migration-validator-command.json) records exact argv,
environment and 66 input hashes. The [receipt](evidence/migration-validator-receipt.json)
and raw logs preserve the result. Every input was unchanged after execution.

## Preserved receiving meaning

All 60 pre-existing validator paths were checked against the actual stack base:
57 historical files matched current blobs; the current M3CI and Java/native
M3BitLane28 files were fetched and hashed. Their stale metadata pins were
reconciled without changing runtime bytes.

The final map has **77 mappings / 63 migration records**. All original mapping
fields and all 60 original migration IDs/statuses remain. Only declared
refinement fields and records were added. Records 30/50 reconcile pre-existing
pins; record 56 receives the intended state C atom. Their complete old records
and previous target artifacts are retained in [history](history/) and the
[preservation receipt](evidence/metadata-preservation.json).

The three new records remain `implemented-unverified`, with no added test
receipts or phase promotion. Validation establishes metadata and file integrity.
Prior host proofs and both native timing series remain in the canonical reports
linked from [README.md](README.md); no runtime test was rerun during this pass.

**STRING / QUALIFICATION_REQUIRED** and all integrated-image, Java SE, Android,
Apple and mobile GUI acceptance gates described in the existing receiving
report and [common JNI contract](COMMON_JNI.md) remain open.
