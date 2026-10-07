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

## Full-family authority readback

The original full-family inventory/provenance authority was merged in Synexia PR #9648 at
`b24262a92b741c78c44ca61afc8dac7ee1376d8a`.

Because the reviewed manifest/catalogue files later disappeared from visible `develop`, current
authority recovery is tracked separately rather than rewriting provenance:

- recovery PR: Synexia #9677;
- recovery commit: `73c5e9335b821270d04f2b6912c62fc279a0ba9f`;
- recovery rule: exact #9648 Git blobs only; no production MIndex/M3Index source changes.

The receiver keeps both identities: #9648 proves original review/provenance; #9677 restores those
same authority bytes to the current source line. Neither grants automatic application of the
4,770-path inventory.

