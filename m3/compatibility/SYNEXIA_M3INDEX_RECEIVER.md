# M3Index Synexia receiver pin

Status: pinned canonical source authority after merge of Synexia PR #9644.

M3JDK21 is a receiver/product-integration target, not the canonical owner of generic M3Index
implementation.

Canonical Synexia authority:

- repository: `hsoliwal/com.synexia`
- merged revision: `9389db87e3545ad8a352ff47969691799b5f1f38`
- source PR: `#9644`
- policy: `docs/M3-SCALE/invariants/SYNEXIA-M3INDEX-OWNERSHIP-1.json`
- ownership ledger: `.m3/donor-convergence/M3INDEX_OWNERSHIP.tsv`
- donor ledger: `.m3/donor-convergence/M3INDEX_DONOR_PLAN.tsv`

The stable global recipe-home pin remains unchanged until the Synexia PR is merged and canonical
tree readback succeeds.

## Receiver rule

Generic M3String/M3AST/M3Dag/data-structure/precompute and reusable Java/JNI algorithms stay
canonical in Synexia. M3JDK21 owns only exact JDK-specific integration:

- java.lang.String representation and bootstrap;
- HotSpot/interpreter/JIT/GC/CDS/JVMTI integration;
- JNI ABI and JDK-native lifecycle;
- exact OpenJDK source/test/build fixtures;
- product image, jtreg and runtime qualification.

A target-local discovery that is reusable must be returned to Synexia first, mastered there, then
re-consumed through a new content-addressed handoff.

## License rule

Synexia-authored copyrightable implementation is Apache-2.0 under its source notice. OpenJDK and
third-party code are never relicensed by this handoff. Abstract algorithms/ideas are provenance,
not an ownership claim over non-copyrightable subject matter.

No source materialization or JDK product admission follows from this ownership pin alone; target build/runtime proof remains independent.
