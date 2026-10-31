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
The source postimage is constructed by replacing the two bounded stored-occurrence methods, one bounded reentrant cursor method and metadata-only spliterator atoms; it does not overwrite later master owners or concurrent changes. Any baseline drift must refuse recipe replay rather than doing a fuzzy patch.

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

## Source-sealed third pass: sorted/Set Spliterator metadata
The prior combined 3-atom source `0b82812ca1e9069bde7d0d695f4f940fd56328b3` remains in additive history. The **current** product postimage of `src/java.base/share/classes/java/util/CollectionViews.java` is canonical Synexia source Git blob `b86464e5f381415e73bc14afb7b078a5e5f1bff0`, UTF-8 SHA-256 `9be6954fb7e4e5261a3f9932bbdd6aedf46c945fb6bb9e15e5c21727e8436d7c`.

This exact original-owner source incorporates the first-party donor's `FilteredSet.spliterator`, `FilteredSortedSet.spliterator` and private `SelectionSpliterator` guaranteed characteristics/owner-comparator propagation from [#1770](https://github.com/hsoliwal/M3jdk21/pull/1770), **without** its extra Set `contains/remove` overrides. The previous stored representative/removal and reentrant state corrections are retained. A `SortedSet` view advertises `SORTED | ORDERED | DISTINCT` and its owner's comparator, including for metadata-sparse source spliterators and split cursors. An ordinary `Set` advertises `DISTINCT`. Neither receives `SIZED/SUBSIZED` when the filtered cardinality is unknown.

Added third original JDK test `FilteredSortedSpliteratorMetadataTest.java`: Git blob `5697bc17eabcad66b6fb3725a76db0e3d2ea23f6`, SHA-256 `e3d8f4208b2f8d3f0b05f512d39e30e276ad4496632f917376f3ebb8e37957ad`; retain its GPLv2 with Classpath exception copyright header. All three tests and original source are sealed in canonical [Synexia #12867](https://github.com/hsoliwal/com.synexia/pull/12867).

**Executed locally:** bounded standalone Java 21 atom proof with `javac --release 21 -Xlint:all -Werror`, **4,010 checks PASS** (metadata sparsity, comparator, null-free filtering, split, reentrancy, Set). This is not execution of the exact full patched `CollectionViews.java`. The PR CI invokes `verify.sh`, which now includes three baseline-red and three corrected-green tests in four JVM modes, source SHA and `javap` ABI validation. Its status must be read from GitHub Actions rather than inferred. Full JDK, JNI/GC/CDS/JCK, five-provider equality/ordering tests and resource benchmarks remain **NOT RUN**.

**No duplicate payload owner:** original backing collections remain authoritative; no per-entry retained nodes, new runtime adapter, vendor package rename, JNI symbol or native object ownership change. New Spliterator metadata is an observable contract repair and requires explicit reviewer acceptance; do not present it as strict preservation of an incorrect prior metadata result.


## Fifth original-owner serial method atom: terminal isEmpty (2026-10-22)

The integrated **M3JDK21 java.base** original `java.util.CollectionViews` now receives the first-party exact `FilteredCollection.isEmpty()` atom from [M3JDK21 #1798](https://github.com/hsoliwal/M3jdk21/pull/1798). It bypasses constructing a one-use filtered iterator and scans the same backing cursor with `visible(element)`, stopping at the first accepted stored occurrence. No retained wrapper graph, copy of payload/Object[]/int[] or mask, new `Map.Entry`, JNI ABI, runtime bridge, Java callback moved into native or public method signature change.

Earlier pre/post-merge candidate SHA-256 `9be6954fb7e4e5261a3f9932bbdd6aedf46c945fb6bb9e15e5c21727e8436d7c` (Git blob `b86464e5f381415e73bc14afb7b078a5e5f1bff0`) is retained in branch history. **Current** combined product source is exact canonical Synexia [#12867](https://github.com/hsoliwal/com.synexia/pull/12867) Git blob `da15fab9b06c6f08320d63a85dabb19782e00a1f`, SHA-256 `46a17d53954bb7b259e3c7285985623028f76a942158b78bbbcc447aef0df126`. The immutable original `master` preimage is still source blob `2ff7ec57ab93c8bd118ceb320fd76aae1c0e7d66`. Four earlier semantic corrections (stored equality/removal, reentrant callback, Set/sorted metadata) remain byte-identical in their source spans.

New canonical/target exact JDK test `FilteredIsEmptyTerminalTest.java`, Git blob `dbba5a8dff2948ccc62c5ebc3e4e7cfea0261b46`, SHA-256 `ca6760ed91cc55eb948cf376d03eebcbdca39a28aeb3faab33491300b808437a`, GPLv2-with-Classpath-exception header retained. It checks 2,400 deterministic nested-predicate traces plus live owner mutations (300 follow-up trials), exact order/short-circuit, no size prepass, exception identity and code-looking/Unicode sorted contents. The exact source was independently strict-compiled and passed **10,805 checks** on both candidate-style and reference-style **isolated API-shaped stubs**, including `-Xint`; this is NOT the actual target class run.

The updated product `verify.sh` now compiles the **actual** java.base source and all **four** original JDK regression tests, checks source/tests SHA256 and public descriptors, executes the existing three baseline-red regressions and verifies isEmpty baseline-GREEN observational parity against four candidate runtime modes. Hosted CI and full source-built JDK/jtreg/JCK/JNI/GC/CDS/module packaging remain separate gates. No measured throughput/heap improvement is claimed. Keep the PR DRAFT until final-head CI and canonical OpenRewrite rerun pass. Avoid overlapping independent whole-file merges from #1770 and #1798.


## Follow-up: original opaque Collection identity-owner correction (2026-10-22)

**One integrated M3JDK21 `java.util.CollectionViews` source, not an adapter.** The previous candidate incorrectly treated every non-Set Collection like an equality-backed List. An `IdentityHashMap.values()` source is only a `Collection` but implements membership/removal by reference identity; with two different equal values, the previous candidate's generic `Objects.equals` iteration could report a hidden exact member as visible or remove the wrong one. M3JDK21 #1789 independently identified this owner-contract class.

**Canonical source recipe** [Synexia #12867](https://github.com/hsoliwal/com.synexia/pull/12867) restores the original opaque `Collection` membership and removal dispatch while retaining the validated stored-instance `Set` and physical-index `List` atoms:
- Source previous Git blob `da15fab9b06c6f08320d63a85dabb19782e00a1f` / SHA-256 `46a17d53954bb7b259e3c7285985623028f76a942158b78bbbcc447aef0df126`.
- Source **current canonical and product exact Git blob** `ef05fcd71d124bbe37a8d7d5e74d641d776403e1` / SHA-256 `d847da2d84aa1db4a7a4f9258f695eaf96136868ef37ebe043ae8166fbdff57a`.
- New JDK regression `FilteredIdentityBackingContractTest.java`: Git blob `3eba99581c83dd7448d108640aec8ccbf3f2b0c8`, SHA-256 `9f2721b3285c16bedd1aa39f24ac4704265a621cd03e26c285a2f6b6d0754242`, GPLv2 with Classpath exception, header retained.
- New seed-2400 `IdentityHashMap.values()` test plus owner-null/type exception checks and predicate-before-admission refusals. Existing four original regression test Git blobs are unchanged. Canonical recipe fixed point now expects six exact LST outputs and explicit absent-before test.
- `verify.sh` checks the original `master` source, the **prior receiver** `da15f...` and the new source under Java21 `--patch-module`. It requires prior candidate RED at exact hidden identity check 4, original owner GREEN, new candidate GREEN under mixed, interpreter, C1, C2, and unchanged public descriptors; the existing list/Set/sorted/reentrant/isEmpty regression and source checks still run.
- **Not yet claimed passed**: the new exact-head product CI, canonical Maven/OpenRewrite SDK replay/JUnit, installed JDK image/JMOD/jtreg/JCK, JNI/GC/HotSpot all-platform qualification, five-library complete contracts and comparative resource measurements. Keep this PR draft and do not merge competing #1770/#1789/#1802 `CollectionViews` whole-file postimages. The existing array/offset/mask/JNI owners remain unchanged.

This correction intentionally narrows an overbroad behavior-changing atom to its contract-qualified List/Set domain; it does not claim universal `Collection` semantic equivalence or performance superiority.
