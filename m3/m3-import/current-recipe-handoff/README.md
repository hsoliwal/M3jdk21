# Current Synexia canonical-recipe handoff

Status: **source-pin receipt only; no materialization or product promotion authority**.

This packet is additive to the historical full-borrow estate. It does not rewrite or supersede old
receipts.

Canonical source repository: `hsoliwal/com.synexia`.

Pinned source revision:

`25ccf1bea2a07e0b59f00b91d933f10785eedd5d`

The checked-in TSV binds exact Synexia Git blobs for the current recipe-ownership policies,
machine invariant ledger, scope/multipass kernel, exact hash-pinned replay, verbatim Java baseline
inventory, and atom/hash database recipe.

M3JDK21 remains a receiver:

- reusable recipes and generic M3/M3Index implementation evolve in Synexia first;
- this target may retain thin adapters, exact OpenJDK preimage/postimage fixtures and JDK-specific
  backport/runtime integration;
- Apache-2.0 is required for the automatic handoff lane;
- OpenJDK and third-party licenses are not changed by the handoff;
- this source-pin packet grants no copy/materialization authority by itself;
- a later `SynexiaImportManifest` must bind exact content SHA-256 before bytes may be copied under
  `m3/vendor/synexia/`;
- target compiler/jtreg/runtime/JNI/fixed-point proof remains required before product promotion.

A reusable defect discovered in M3JDK21 must be repaired/mastered in Synexia, then received again
under a new additive handoff receipt.
