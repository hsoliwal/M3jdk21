# Synexia recipe application receipt

Status: **applied product state; Synexia owns the reusable recipes**.

## Canonical recipe owner

Repository: `hsoliwal/com.synexia`  
Custody PR: https://github.com/hsoliwal/com.synexia/pull/9491  
Custody branch: `m3/m3jdk21-recipe-custody-20261006`  
Pinned custody revision: `5291e3867224be653da89cd69e3b764b2fab213f`

Named recipes:

- `com.synexia.rewrite.M3Jdk21StringHistoryConvergence`
- `com.synexia.rewrite.M3Jdk21TqConvergence`

The Synexia branch owns the hash-pinned Java/text manifests, reviewed postimages, recipe
registration and custody invariant tests.

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

Do not copy the canonical recipe crates back under `m3/tooling`. To evolve this implementation,
change the Synexia recipe first, apply the new reviewed postimage to this repository, then update
this receipt and rerun M3JDK21-native verification.

Target-side source invariants enforce the absence of local duplicate String/TQ recipe crates.
