# Contract-mode adapter custody refresh — 2026-10-13

## Exact failure

The M3 Synexia recipe ownership gate failed at:

`retained target adapter drift: m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/M3ContractMode.java`

The ledger still pinned the target adapter to Git blob `c43764ebe0e42d2cf3d61e9eda4de4fe3624d60e`, but the current target file is Git blob `3ee8dd184d5c382391f6f9590fb9c773044bfaa5`.

## Provenance and semantic scope

The delta was introduced by the already-merged M3JDK21 commit
[`d2c1fb3ffd8f2fe5a8d91a6f309b769d74531edd`](https://github.com/hsoliwal/M3jdk21/commit/d2c1fb3ffd8f2fe5a8d91a6f309b769d74531edd),
`m3: make contract mode a bidirectional Synexia adapter`. Its exact patch adds only package-private
`static M3ContractMode fromCanonical(com.synexia.rewrite.M3ContractMode mode)`, mapping by enum name
and rejecting null. The existing public enum constants, constructor visibility, and public surface are unchanged.

The new blob was fetched from the current `master` file and independently compared to the commit's
recorded target path. The ledger update changes only the expected Git blob for the existing
`scope-contract-mode` row; it does not change the target adapter or canonical Synexia implementation.

## Ownership boundary

- Synexia remains the canonical owner of reusable contract-mode semantics and Apache-2.0 recipes.
- M3JDK21 retains only its target-side adapter and JDK-specific integration.
- This is a custody-ledger reconciliation, not source promotion or a runtime behavior change.
- Canonical scope-policy/registry restoration is tracked separately in
  [Synexia PR #10698](https://github.com/hsoliwal/com.synexia/pull/10698); that PR must merge and its
  exact source receipt must be received before the dependent migration-recipe build can be accepted.

## Verification status

The ownership checker previously failed on the stale blob pin. This PR's CI must rerun the checker
and the receiving migration-recipe tests. No local Maven execution is claimed for this ledger-only
change. Full JDK build, jtreg, and runtime acceptance remain separate gates.
