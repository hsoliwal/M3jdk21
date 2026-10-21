<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original java.util owner — stored representative regression recovery

## Authority and inventory
M3JDK21 is the integrated JDK 21 SDK, **not an M3-to-JDK layer**.
This bounded change restores three existing `java.util.CollectionViews` implementation method bodies from first-party product donor [M3JDK21 #1116](https://github.com/hsoliwal/M3jdk21/pull/1116), merged into a feature branch rather than current master. JDK public method names, descriptors, SortedSet/NavigableSet behavior and source payload remain in their existing owners. No retained Entry/Node, independent Object[] backing, JDK-vendor wrapper, Synexia runtime dependency or new JNI export.

**Canonical Synexia source-sealed OpenRewrite recipe:** [#12867](https://github.com/hsoliwal/com.synexia/pull/12867), `com.synexia.m3.JavaUtilStoredOccurrenceMaster20261021` with exact source preimage, reviewed postimage, red/green oracle, Maven JUnit fixed-point, absent-input, duplicate-input, code-looking decoy and drift-refusal fixtures. This target receives exact canonical template text, not a manual production rewrite.

## Source and test seals
| Surface | Prior SHA-256 | Reviewed SHA-256 |
|---|---|---|
| `src/java.base/share/classes/java/util/CollectionViews.java` | `a6ea3101c0573d33808b5a25262e4bf26d72d2a8882ee544cfbbe3e693f7145d` | `51ebd300f5c469aa2ab4f59692f2a74360610d9a917fa7c184bcffd8c556dbe3` |
| `test/jdk/java/util/CollectionViews/StoredRepresentativeOracle.java` | ABSENT | `f32eeb5079bcd6322e597303a937614931b7f33779a58d0ed03b53ae0a8302c6` |
| `test/jdk/java/util/CollectionViews/FilteredSpliteratorReentrantTest.java` | ABSENT | `f35b8547a5163aa79cee603e0e8bdcbf7361515d435914ac205feb9a037990e2` |

Original source Git blob: `2ff7ec57ab93c8bd118ceb320fd76aae1c0e7d66`.
Oracle donor Git blob: `491b55c10f829b35f1cdcc5e2233d27564617d17`.
The source postimage is constructed by replacing the two bounded stored-occurrence methods and one bounded reentrant cursor method only; it does not overwrite later master owners or concurrent changes. Any baseline drift must refuse recipe replay rather than doing a fuzzy patch.

## Behavioral atoms
- `contains(Object)`: validate the backing membership first; evaluate visibility on actual stored representative. Identity-set exact-reference match precedes ordinary equals fallback.
- `remove(Object)`: for Lists remove the first selected **physical** index, including COW lists; for Sets preserve stored representative identity; for other Collection owners use iterator removal with a limited safe fallback when no earlier hidden equal item could be deleted.
- `SelectionSpliterator.tryAdvance` preserves the enclosing callback and delivery state during nested consumer reentry using the same first-party atom as [#1770](https://github.com/hsoliwal/M3jdk21/pull/1770). No retained per-element object.
- Source index/offset, backing arrays, masks and JNI selector remain authoritative. The operation is a correctness repair to previous observable wrong results, not strict behavior preservation of a known defect.
- Existing sorted/navigable overrides retain original class shape; PR #1770 contains additional Set overrides not automatically received and therefore needs reconciliation before merging. JNI mask and shared lexicon changes are separate ([#1765](https://github.com/hsoliwal/M3jdk21/pull/1765), [#1746](https://github.com/hsoliwal/M3jdk21/pull/1746)). Do not conflate branch membership or claim merged integration.

## Verification
Run on JDK21 / Git checkout with the original historical blob locally available:
```sh
bash m3/qualification/javautil-stored-master-20261021/verify.sh /tmp/new-m3javautil-proof
```
The script checks exact source seals, strict JDK21 `--patch-module` compilation of actual before/after java.base owners, stable `javap` public ABI, reproduces original red before-source failure and executes the original owner's 736,018-assertion oracle under interpreter, C1, C2 and mixed modes. No generic surrogate silently replaces the actual owner.

**NOT RUN in authoring:** this exact receipt script, canonical Maven/OpenRewrite Recipe.run, full OpenJDK build, jtreg/JCK/installed-image/HotSpot/GC/native parity, five-library acceptance and CPU/heap/native memory benchmarks. CI is a configured independent acceptance path, not a completion claim. Keep draft until gates pass. History remains append-only; no rebase, force-push or auto-merge.
