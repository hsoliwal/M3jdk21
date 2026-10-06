# Synexia recipe application receipt

Status: **applied product state; Synexia owns the reusable recipes**.

## Canonical recipe owners

### M3 String history convergence

Repository: `hsoliwal/com.synexia`  
Custody PR: https://github.com/hsoliwal/com.synexia/pull/9529  
Custody branch: `m3/m3jdk21-string-canonical-dag-synexia-home-20261006`  
Pinned custody revision: `aeeda5a77e2c2a1743be0961e8052f7654a8e697`

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

`82ee9917ef56e3e297402aea563e08d8763fe03d`

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
