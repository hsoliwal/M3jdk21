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
