# Synexia M3* canonical-family receiver

Status: target-side admission contract. Synexia PR 9763 is the source policy change under review.

M3JDK21 receives reusable first-party M3 code from `hsoliwal/com.synexia`; it does not become a
second generic implementation or recipe home.

## Authority split

- **Synexia:** canonical reusable M3* implementations, collection/data-structure algorithms,
  String/text/AST/DAG/object/precompute/search/JNI atoms, Maven/OpenRewrite recipes, donor convergence,
  and public-code polish.
- **M3JDK21:** exact OpenJDK preimages/postimages, JDK/HotSpot/JIT/GC/CDS/JNI/JVMTI integration,
  jtreg/runtime qualification, compatibility adapters, and product promotion.
- **Legacy MIndex / MatIndex / MAT:** compatibility/history inputs until an exact contract is
  migrated and proved; they are not forward generic owners.

## Naming

New independently authored reusable first-party types use `M3*`.

Preferred small packages:

| Family | Canonical Synexia package |
| --- | --- |
| text / String | `com.synexia.m3.text` |
| AST | `com.synexia.m3.ast` |
| DAG | `com.synexia.m3.dag` |
| object/context | `com.synexia.m3.object` |
| data structures | `com.synexia.m3.ds` |
| collections | `com.synexia.m3.collection` |
| precompute | `com.synexia.m3.precompute` |
| search / regex | `com.synexia.m3.search` |
| JNI/native API | `com.synexia.m3.nativeapi` |
| recipes | `com.synexia.rewrite` |

For collections, one contract-proved measured representation is the primary implementation.
Alternative/compatibility representations stay behind a small focused package or adapter. New
non-JDK names should be short and JDK-like when unambiguous.

## Admission sequence

1. pin the exact merged Synexia revision;
2. inventory source paths and licenses;
3. accept only first-party Apache-2.0 bytes into the automatic lane;
4. retain third-party/upstream license and NOTICE boundaries;
5. materialize the pinned Synexia handoff;
6. inventory the target JDK contract;
7. apply only a thin target adapter/backport/integration recipe;
8. run compiler/JUnit/jtreg/runtime/JNI parity gates as applicable;
9. require fixed point and exact target readback before promotion.

A target-discovered reusable defect is fixed in Synexia first and then re-received.

## Copyright and license boundary

Independently authored Synexia/M3 source, tests, documentation, recipes and other copyrightable
expression retain their authorship/copyright and are distributed under Apache-2.0 where so marked.
This does not claim copyright over abstract ideas, algorithms as such, or third-party material.
OpenJDK and donor code retain their own governing licenses and notices.

## Current full-borrow successor snapshot

The historical family table above remains an ownership map. The current exact Synexia source-tree
snapshot is received separately under `m3/synexia-import/current-full-borrow/`.

That successor is pinned to Synexia revision
`296323958b1019edd59b60b9c05cb148d024cfe5` and starts every family at
`SOURCE_PIN_ONLY`. It adds exact source-tree custody for String/IndexString, AST/grammar,
precompute, MIndex runtime/foundation, data structures, collections, compiler, algorithms, DB,
JNI/native, search/regex, loaders, M3Index aliases/bridges, MAT history, native interop,
fast-search and canonical recipes.

It is not target acceptance and does not modify the ordered receiving plan. Advance a family only
through an additive successor receipt with target-native proof.

## Operational import hardening

The existing manifest/source-hash contract remains the portable data contract. Operational callers
can now request a stronger checkout gate:

- `SynexiaImportCli verify-strict`
- `SynexiaImportCli materialize-strict`
- `SynexiaImportPlanCli stage-strict`

These actions require the Synexia checkout's Git `HEAD` to equal the manifest
`source_revision` and require the tracked worktree to be clean before the existing importer or
stager runs. The receiver accepts the manifest's existing 40- or 64-hex Git object-id grammar.
Existing non-strict actions remain source-compatible for synthetic fixtures, replay tooling and
already-sealed source trees.

Direct materialization remains additive: an existing divergent target fails closed, while an
identical target is a no-op. A multi-file invocation now records only targets it created. If a later
write or final verification fails, it walks those newly-created targets in reverse order and removes
one only when it is still a regular non-symlink file with the exact expected SHA-256. A target that
changed after creation is never deleted during rollback; that rollback failure is retained as a
suppressed failure on the original exception.

Source and destination path resolution also rejects symlink or non-directory ancestors. This
prevents a lexically in-root path from escaping through an intermediate link.

The staged `ADD/REPLACE/KEEP/STALE` planner remains the authority for replacement candidates.
This hardening does not turn the direct importer into a replacement engine and does not grant
promotion authority.
