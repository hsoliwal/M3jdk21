# Synexia full-borrow snapshot refresh — 2026-10-11

Status: documentation-first receiver update; **inventory custody only**.

## Provenance

- Canonical repository: `hsoliwal/com.synexia`
- Existing full-estate origin PR: [#9860](https://github.com/hsoliwal/com.synexia/pull/9860)
- Reviewed source snapshot revision: `ecfc155eea38b9d4c85e644761a3b2fd2d2f9d1f`
- Estate manifest refresh commit: [`897b05a4798f514a82b9939851209d2b271926b2`](https://github.com/hsoliwal/com.synexia/commit/897b05a4798f514a82b9939851209d2b271926b2)
- Estate Git blob at the refresh commit: `2ae93e0bd4fd03760df5aa5d99b90aafb36f1ba0`
- Existing dependency DAG and promotion-phase blobs remain unchanged: `3e6c56b8555231fb36d0ac620ab112aa5970d45a`, `c4e8582f99caba32d718b2c1e86dae0854cdce3d`.

The estate's `snapshot_revision` identifies the source tree being inventoried; `source_manifest_commit` identifies the commit that publishes the inventory bytes. Those are separate facts and must not be collapsed into one field.

## Delta

The estate still has 32 families. The source-tree identity changed for exactly two rows:

- `TEXT_INDEXSTRING`: `3d1bbf3bc944e827b8b82aa6841bc0720d618e93` → `8535757300a93d56df9db5c66e81e2ba81ecde01`
- `OPENREWRITE_RECIPES`: `bb1935e5b0412717b5d9af6a8715993fbfa70a4d` → `460491ea7ce48580ba9ba1f7373e190464666a56`

All other family identities are unchanged. The receiver-status rows must be rebound to these exact estate identities without upgrading any target state or receipt.

## Non-negotiable boundary

- Keep the Apache-2.0 notice and first-party attribution for eligible Synexia material.
- Preserve OpenJDK/GPL+CPE and all third-party donor licenses; no blanket relicensing.
- Do not copy reference-only problem statements/editorials.
- Do not treat a source inventory pin as target acceptance.
- Keep `family_completion=false`, `repository_completion=false`, and `automatic_application=false`.
- Runtime code, public APIs, JNI behavior, and product files are outside this refresh.

## Verification to run on the receiving checkout

1. `python3 m3/compatibility/check_synexia_full_borrow_receiver.py`
2. The existing full-borrow / string-phase verification workflow.
3. Confirm source and target estate Git blobs match, all 32 family identities join exactly, and the target-state/proof columns are byte-preserved except for source identity rebinding.
4. Keep the PR draft until the hosted receiver checks pass.

This file is a review contract; it is not itself a claim that those commands have run or passed.
