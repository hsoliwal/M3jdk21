# Canonical scope adapter custody refresh — 2026-10-13

## Exact failure and scope

The M3 Synexia recipe ownership gate reported retained target adapter drift. The ownership ledger had pinned three target-side compatibility adapters to earlier Git blobs, while their current source reflects the already-merged migration to Synexia-canonical scope semantics.

| Target path | Historical ledger pin | Current verified blob | Existing source change |
|---|---|---|---|
| `m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/M3ContractMode.java` | `c43764ebe0e42d2cf3d61e9eda4de4fe3624d60e` | `3ee8dd184d5c382391f6f9590fb9c773044bfaa5` | [`d2c1fb3ffd8f2fe5a8d91a6f309b769d74531edd`](https://github.com/hsoliwal/M3jdk21/commit/d2c1fb3ffd8f2fe5a8d91a6f309b769d74531edd) adds package-private reverse conversion `fromCanonical(...)`. |
| `m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/M3RecipeScopePolicy.java` | `e835666e5adc321e9f7ef8db47d8f2e652f89721` | `a2c62ee105d0fcf31de67bafb86bc4d8c1eebf6c` | Current adapter converts the canonical policy; target-only path inference remains local. |
| `m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/M3RecipeScopeRegistry.java` | `7b77af510300f56950c85259f87e87123adf591b` | `4e888371f5a7d4c2c2084352cd84674f219f160b` | Current overlay delegates portable recipe scope to Synexia and retains target-specific mappings. |

The three current hashes were read from the exact `master` files. Historical blobs were fetched and compared. The observed differences match the already-present target-adapter evolution; this PR updates only the expected blob values in the custody ledger.

## Ownership and compatibility

- Synexia remains canonical for reusable scope semantics and Apache-2.0 recipes.
- M3JDK21 retains target-only OpenJDK/JEP mappings and compatibility adapter types.
- Public enum constants and public API are unchanged by these ledger updates.
- No production Java, workflow, build, runtime, license, or Git history changes are made by this PR.
- The original target source and ledger history remain available through Git ancestry.

## Dependent canonical source

[Synexia PR #10698](https://github.com/hsoliwal/com.synexia/pull/10698) restores the canonical `M3RecipeScopePolicy` and `M3RecipeScopeRegistry` sources lost from current `develop`. This ledger correction does not substitute for merging and receiving those exact Apache-2.0 source blobs.

## Verification

Remote file reads confirmed all three current hashes and the revised ledger rows. The first CI attempt then exposed the next stale row (`M3RecipeScopePolicy`); this revision reconciles all three known adapter rows together rather than iterating one failure at a time. CI must rerun the ownership checker and receiver compile/JUnit.

Not claimed: local Maven execution, full JDK build, jtreg, runtime acceptance, or source promotion.
