# Synexia full-borrow intake — 2026-10-11

Status: **SOURCE_PIN_ONLY**. This intake does not claim JDK acceptance.

Canonical reusable owner: `hsoliwal/com.synexia`.

Pinned Synexia source revision:

`37e4edabea6af918a4dc9a7eabd45fbdcf101881`

Synexia handoff commit:

`3a87d17df7e5d825aa1b071c5fb05c64b854c798` (PR #10336)

This intake copies only content-addressed ownership/inventory evidence:

- `synexia-estate.tsv` — 32-family Apache-2.0 borrow estate;
- `synexia-dag.tsv` — dependency/promotion DAG;
- `promotion-phases.tsv` — ordered target receiving phases;
- `pin.tsv` — receiver provenance.

M3JDK21 remains the target-specific owner for OpenJDK integration, HotSpot/JIT/GC/CDS/JNI/JVMTI
changes, exact JDK preimages/postimages, jtreg/runtime proof, and product promotion.

Reusable code, recipes, M3Index/M3IndexDB implementations, donor convergence, atomization,
patternization/IOP and public-code-polish mechanics remain canonical in Synexia. A reusable defect
found during target qualification is repaired in Synexia first, resealed, and then re-received.

No vendored source is refreshed merely by adding this intake. Materialization is a later,
proof-gated phase and must preserve original licenses/notices and exact source revision custody.


## Canonical-master receiver law

This intake cannot transfer generic reusable ownership.

Machine-required values in `pin.tsv` are:

- `canonical_invariant=SYNEXIA-M3JDK21-CANONICAL-OWNERSHIP-1`;
- `generic_ownership_transfer=false`;
- `target_generic_reusable_owner=false`;
- `reusable_change_return_path=TARGET_DISCOVERY_TO_SYNEXIA_MASTER_THEN_RESEALED_TARGET`;
- `target_specific_authority=OPENJDK_INTEGRATION_JTREG_RUNTIME_AND_FINAL_PROMOTION`.

Therefore a reusable defect/improvement discovered during M3JDK21 qualification returns to
`hsoliwal/com.synexia` for canonical implementation/recipe proof before a new exact handoff is
received. M3JDK21 keeps target adapters and OpenJDK/HotSpot/JIT/GC/CDS/JNI/JVMTI/jtreg/runtime
qualification plus final product promotion.
