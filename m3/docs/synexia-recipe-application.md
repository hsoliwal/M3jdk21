# Synexia recipe application receipt

Status: **applied product state; Synexia owns the reusable recipes**.

## Canonical recipe owners

### M3 String history convergence

Repository: `hsoliwal/com.synexia`  
Custody PR: https://github.com/hsoliwal/com.synexia/pull/9597  
Custody branch: `m3/m3jdk21-string-recipe-canonicalize-v2-20261007`  
Pinned custody revision: `e0845c4fd0fe56ecf1fa0e88981620062d9dafee`

This supersedes the earlier custody receipt for PR #9529. PR #9597 is based directly on the
then-current Synexia `develop` head and forward-seals the live M3JDK21 product postimages without
moving runtime ownership out of M3JDK21.

Named recipe:

`com.synexia.rewrite.M3Jdk21StringHistoryConvergence`

The Synexia donor owns:

- hash-pinned Java/text snapshot recipe engines;
- the `m3jdk21-string-history-convergence` hash-pinned Java crate;
- the `m3jdk21-string-history-convergence` hash-pinned text/native/workflow crate;
- reviewed postimages and original preimage SHA-256 values;
- fixed-point / missing-target / drift-refusal recipe tests.

### M3 TQ convergence

Existing TQ recipe custody remains under the earlier Synexia handoff:

- PR: https://github.com/hsoliwal/com.synexia/pull/9491
- branch: `m3/m3jdk21-recipe-custody-20261006`
- pinned revision: `5291e3867224be653da89cd69e3b764b2fab213f`
- recipe: `com.synexia.rewrite.M3Jdk21TqConvergence`

## Applied M3JDK21 state

M3JDK21 runtime branch:

`m3/m3string-runtime-precompute-fix-20261005`

Applied/fixed-point revision:

`7dc72fdf9733951bcc802f499a13871188d2c34a`

Direct manifest comparison after moving recipe custody to Synexia:

- Java recipe targets: **19 canonical targets**.
- Text/native/workflow targets: **11 canonical targets**.
- Current custody was refreshed from the live M3JDK21 branch; unchanged targets remain byte-identical and evolved targets were resealed with their original preimage hashes preserved.

The original `before` hashes remain the exact stacked recipe preimages inherited from the former
M3JDK-local crate. They are not a license to overwrite arbitrary current `master`: the donor recipe
fails closed on any source that matches neither its exact preimage nor its exact postimage.

## M3JDK21 ownership

This repository owns the result after recipe application:

- `java.lang.String` / internal **M3 String** representation;
- canonical owner/atom/tuple/pool implementation;
- String facts, search/position/TQ precompute;
- regex integration;
- HotSpot/JNI/native integration;
- JDK/jtreg/runtime verification.

No Synexia code is loaded by `java.base`, HotSpot or JNI at runtime.

## Custody invariant

Do not copy the canonical recipe crates back under `m3/tooling`. To evolve this implementation:

1. change/prove the Synexia recipe first;
2. apply the reviewed postimage to the exact admissible M3JDK21 source state;
3. update this receipt;
4. rerun M3JDK21 compiler, jtreg, HotSpot, JNI and platform gates.

Target-side source invariants enforce the absence of local duplicate String/TQ recipe crates.


## Authority split after recipe move

- **Synexia owns** `com.synexia.rewrite.M3Jdk21StringHistoryConvergence`, its hash-pinned crates, donor/history evidence, fixed-point/refusal semantics, and reusable recipe evolution.
- **M3JDK21 owns** every applied Java/HotSpot/JNI/native implementation, all running M3 String precompute, JDK semantics, build/jtreg/runtime verification, and product promotion.
- This move creates **no Synexia runtime dependency** in `java.base`, HotSpot, JNI, or native String code.


## Pending canonical receiver refresh

Follow-up Synexia PR: https://github.com/hsoliwal/com.synexia/pull/9672  
Branch: `m3/m3jdk21-string-recipe-receiver-refresh-20261007`  
Pinned refresh revision: `0584a6713e70860dca42410088085d980f1044c2`

This follow-up promotes the receiver's latest workflow and fail-closed invariant gate back into the
already-canonical Synexia recipe established by merged PR #9597.

Readback at M3JDK21 receiver head
`7dc72fdf9733951bcc802f499a13871188d2c34a`:

- Java/runtime/test targets: **19/19 exact**;
- text/native/HotSpot/governance targets: **11/11 exact**;
- total: **30/30 exact postimage SHA-256**.

### Machine-readable receipt

```text
recipe_name	com.synexia.rewrite.M3Jdk21StringHistoryConvergence
synexia_repository	hsoliwal/com.synexia
synexia_commit	0584a6713e70860dca42410088085d980f1044c2
synexia_java_manifest	synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3jdk/jdk21-hash-pinned/synexia-m3-string-history-convergence/manifest.tsv
synexia_text_manifest	synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3jdk/jdk21-hash-pinned-text/m3-string-history-convergence/manifest.tsv
synexia_java_manifest_sha256	b5ae63e4af47ed5c9ff10645d479d5081e8370990602094ab26da718f100dc6a
synexia_text_manifest_sha256	795ff4342c9e5ed61cdc38123b9df6de0c335d4fb14bfbc9e6c3078d7cae6361
m3jdk21_applied_result_commit	7dc72fdf9733951bcc802f499a13871188d2c34a
runtime_owner	M3JDK21
recipe_owner	Synexia
runtime_dependency_on_synexia	false
target_recipe_disposition	THIN_RECEIVER_ONLY
verification_status	SYNEXIA_CANONICAL_PACKET_30_OF_30_TARGETS_HASH_MATCHED
```


### Superseded local descriptor cleanup

The old target-local descriptors

- `m3-jni-newstring-admission.yml`
- `m3-string-char-boundary.yml`

were removed because their behavior is already subsumed by the canonical Synexia
`M3Jdk21StringHistoryConvergence` packet. M3JDK21 source invariants now forbid those duplicate
recipe-owner entry points from returning.


### Sealed target-state boundary

The hash-pinned recipe target state is
`7dc72fdf9733951bcc802f499a13871188d2c34a`.

Receipt-only commits after that state are deliberately outside the recipe target inventory, so
updating this document does not recursively change the sealed applied-result identity.
