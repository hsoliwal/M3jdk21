# Synexia recipe application receipt

Status: **applied product state; Synexia owns the reusable recipes**.

## Canonical recipe owners

### M3 String history convergence

Repository: `hsoliwal/com.synexia`  
Custody PR: https://github.com/hsoliwal/com.synexia/pull/9530  
Custody branch: `m3/m3jdk21-string-convergence-donor-20261006`  
Pinned custody revision: `ad5df8458ffeba643a6c343e82dd61a338facf29`

Named recipe:

`com.synexia.rewrite.m3jdk.M3StringHistoryConvergence`

The Synexia donor owns:

- hash-pinned Java/text snapshot recipe engines;
- the `synexia-m3-string-history-convergence` Java crate;
- the `m3-string-history-convergence` text/native/workflow crate;
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

`26a13d9fd94ef762127262a67c86b9d7bb3c8cf0`

Direct manifest comparison after moving recipe custody to Synexia:

- Java recipe targets: **18/18 exact postimage hashes**.
- Text/native/workflow targets: **6/6 exact postimage hashes**.
- Recipe result on this runtime head: **zero changes / fixed point**.

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
