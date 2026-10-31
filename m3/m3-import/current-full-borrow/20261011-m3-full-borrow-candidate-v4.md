# Synexia full-borrow candidate v4 — target custody, not admission

Status: **CANDIDATE MIRROR ONLY / NO TARGET PROMOTION**. Source owner: `hsoliwal/com.synexia`. Receiver/product owner: `hsoliwal/M3jdk21`.

This record is subordinate to `m3/docs/m3jdk21-porting-invariant.md`, `m3/docs/name-mapping.json`, `m3/docs/synexia-intake-policy.tsv` and `AGENTS.md`. It adds no new naming, licensing, execution, runtime, or recipe owner.

## Exact lineage

1. Retained authoritative M3 receiver inventory `m3/synexia-import/current-full-borrow/synexia-estate.tsv` originates at Synexia commit `296323958b1019edd59b60b9c05cb148d024cfe5`. Preserve it unchanged.
2. Synexia candidate v3 was pinned at `ecfc155eea38b9d4c85e644761a3b2fd2d2f9d1f`, source tree `e50bae0540647e48bb4b58810ee7994a144c1084`. Its historical custody record also remains unchanged.
3. Synexia candidate v4 is pinned at commit `68322e2b40c3d87ca1e3c1566616d2387fd4e847`, tree `949bfb4f82316dce72edca16d2921422d9faef2e`. Canonical source review: [Synexia PR #10345](https://github.com/hsoliwal/com.synexia/pull/10345). The source candidate inventory Git blob is `93037d99f9dcebfcbd4371a7c98405375f6d71f1`.

The adjacent `20261011-synexia-full-borrow-candidate-v4.tsv` is intended to be a byte-identical custody mirror of that source candidate. It is **not** a replacement for `synexia-estate.tsv`, and it does not update the M3 receiver's active source revision. Target baseline for this documentation: M3jdk21 master `985aadda29756c0fe988d7b70c8240d4eb554dc7`.

The 32-family source comparison has 29 unchanged identities and three changes: `TEXT_INDEXSTRING` `8535757300a93d56df9db5c66e81e2ba81ecde01` -> `37aca513e40cb93b6ee646750a2a2a1c68a0a787`; `COMPILER` `b604a3b6f7527f1e479e02ef9b72e0ec3de43c6c` -> `5f41088925fe21bc5cf7891a46706a3a88e941f7`; `OPENREWRITE_RECIPES` `460491ea7ce48580ba9ba1f7373e190464666a56` -> `91bd246e4480e5a7d8bb9e821c63761f826287b9`. All 32 remain `INVENTORY_PINNED`, not accepted.

## Borrowing rules

Synexia-original, verified Apache-2.0 Java/JNI/native code, OpenRewrite recipes, atomizers, patternizers, tests, fixtures, templates, documentation and qualified generated expression may be reused, with exact commit/path/hash, LICENSE, NOTICE, attribution and change records. Generic reusable recipe implementation stays mastered in Synexia; this JDK uses the exact recipe handoff as authoring/proof tooling, not a runtime dependency.

The source root `LICENSE` Git blob is `29f81d812f3e768fa89638d1f72920dbfd1413a8` and `NOTICE` Git blob is `97bb5d49f8bd3bc573c19afe4ea2392a3d02704e`. These root identities **do not classify every descendant file**. Re-check mixed-language/native donor files and OpenJDK/third-party licenses individually. Existing `m3/docs/synexia-intake-policy.tsv` distinguishes independent Apache modules, external recipe tooling, target-owned ports, explicit authorized relicensing and historical direct-copy review. No direct Apache copy into `src/**` is authorized by this candidate.

## Receiver admission obligations

- Verify the mirrored inventory's Git blob exactly equals the pinned source candidate blob; reject a mismatch.
- Preserve the original receiver estate and intermediate 2026-10-08 source history; compare every target preimage before recipe application.
- Require actual Synexia recipe identity/manifest, fixed-point and refusal proof for each source-changing atom; no direct production-file edits to simulate recipe output.
- Require a reviewed target mapping and licensing disposition for each affected JDK owner; retained ABI/public contracts and any OpenJDK file headers remain authoritative.
- Run target diff -> lint -> compile -> recipe replay/refusal/fixed point -> tests -> relevant jtreg/HotSpot/JNI/native/runtime gates. Store command receipts and output hashes; do not infer a runtime pass from source matching.
- Promote serially through `STRING -> ARRAYS -> COLLECTIONS -> AST_COMPILER -> REMAINING_FAMILIES`. This candidate advances **zero** phases.

This review runs no compiler, Maven/OpenRewrite, jtreg, HotSpot/JNI, performance or release qualification and therefore cannot be reported as full JDK integration.
