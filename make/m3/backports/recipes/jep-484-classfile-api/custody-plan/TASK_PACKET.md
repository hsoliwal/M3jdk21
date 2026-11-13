# JEP 484 Class-File API — current-tree source-custody planning pass

Status: evidence-only third pass; no Java SE API materialization or promotion authority.

## Input authority

Reuse the merged JEP484 path map:
- JDK21 internal classfile files: 241
- JDK24 combined public+internal files: 245
- direct descendants: 238
- unmapped JDK21 residue: 3
- ambiguous descendants: 0
- added JDK24 paths: 7

Pinned refs remain JDK21 GA and JDK24 GA. The only historical spelling normalization is
`Classfile -> ClassFile`.

## Goal

Turn the path/content map into a target-side custody plan against the current M3JDK21 checkout
without mutating product source.

For every direct descendant or donor-only addition classify:
- SAME_PATH_REPLACE: donor path equals source path and current target still matches the JDK21 source hash;
- MOVED_PUBLIC_ADD: descendant moved from jdk.internal.classfile to java.lang.classfile and target path is absent;
- MOVED_INTERNAL_ADD: descendant moved to a different internal path and target is absent;
- ALREADY_PRESENT: target bytes already match the donor hash;
- SOURCE_DRIFT: mapped JDK21 source no longer matches the sealed source hash;
- TARGET_OCCUPIED: donor path exists with non-donor bytes;
- RETAIN_UNMAPPED_SOURCE: one of the three explicit unmapped JDK21 residues;
- ADD_DONOR_ONLY: one of the seven donor-only paths and target is absent.

No moved source is deleted automatically. No mapping is promoted as semantic equivalence.

## Verification

The workflow must reproduce the sealed path-map counts, execute synthetic custody-plan tests, and
emit deterministic CUSTODY_PLAN.tsv, CUSTODY_COUNTS.tsv and version-signal target lists against the
actual current checkout.

This pass must complete before any source-sealed opt-in Class-File API crate generation.
