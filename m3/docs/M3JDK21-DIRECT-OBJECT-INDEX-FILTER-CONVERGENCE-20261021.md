<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 direct Java/HotSpot/JNI and array-index/filter convergence

## Product identity
This source is the **actual M3JDK21 Java21/HotSpot/JNI SDK**, not M3 layered above a stock JDK. Synexia retains first-party source-sealed Maven/OpenRewrite recipe/donor convergence only. No Synexia runtime dependency is installed into java.base.

## Inventory before receiving

Current product **master** is the baseline for this combined candidate. Each existing Java target must match its independently pinned original master Git blob **before** receiving the canonical candidate, otherwise refuse:
- `M3LongIndexProjection.java` = `d18055008f6eabbd6603e080ff32f8d5a1da8b60` (primitive sorted-index natural run, source-sealed in Synexia #12700 / product #1667).
- `M3CollectionViews.java` = `7b777fcabcf7c47ab75ad0df1095663b11dcc31c` (filtered/sorted inherited isEmpty, source-sealed in Synexia #12720 / product #1678).
- New JDK `M3ObjectMemory.java`, native `libjava/M3ObjectMemory.c` and jtreg `M3ObjectMemoryTest.java` were absent at inventoried master. Source-sealed in Synexia #12668 / product #1648.
- No original `java.util`/public Java SE method signatures, `Map.Entry` retained row storage, JNI headers/exports or existing generic predicate contracts are replaced. This is a narrow superset of three **independent** source atoms.

## Direct product ownership

| Owner | Java/native behavior | Existing canonical recipe | Verification risk |
| --- | --- | --- | --- |
| `java.base/jdk.internal.misc.M3ObjectMemory` and `libjava/M3ObjectMemory.c` | JVMTI shallow sizeof, GC-stable opaque tag ID, expensive root-edge snapshot, fail-closed raw address | [Synexia #12668](https://github.com/hsoliwal/com.synexia/pull/12668) | Whole matched libjava, HotSpot bootstrap, GC/JIT, JCK/jtreg **not qualified** |
| `M3LongIndexProjection` | Read `long[]` payload by `int[]` indexes+window; avoid stable merge for already monotonic ENCOUNTER | [Synexia #12700](https://github.com/hsoliwal/com.synexia/pull/12700) | Java21 own module/API/benchmark pending |
| `M3CollectionViews` | Original backing Iterator short-circuit for isEmpty of collection/set/sorted/navigable wrappers | [Synexia #12720](https://github.com/hsoliwal/com.synexia/pull/12720) | Exact Java callback/exception oracle and native/Android integration pending |

All output files must be **byte-identical** to their original Synexia reviewed templates or M3JDK21 standalone product branch postimages. This combination does not reimplement any algorithm or create a new Maven/OpenRewrite engine.

## JNI/native distinction

Object metadata and GC facts belong **inside the VM's JVM TI/native libjava**, not an external JNI bridge. Signed primitive index and state-mask native kernels already exist within M3JDK21. Java predicates and arbitrary comparator callbacks remain Java to preserve side effects, timing and exceptions. Do not copy Java object pointers into untracked native arrays, reuse raw changing GC addresses as IDs, interpret a JVMTI referrer snapshot as O(1) exact reference count or treat cooperative native leases as Java heap aliases.

## Reconciliation outstanding

The larger mixed filtered/sorted/JNI owner candidate [M3JDK21 #1619](https://github.com/hsoliwal/M3jdk21/pull/1619), M3String/JNI [#1657](https://github.com/hsoliwal/M3jdk21/pull/1657), and mask-window classification [#1654](https://github.com/hsoliwal/M3jdk21/pull/1654) have separate evolving preimages, donor catalogues and incomplete acceptance gates. **Do not merge their whole source snapshots into this combined branch without reconciling each existing atom, exact source identity, and its compiler/runtime proof.** This review branch retains the canonical approvals as dependencies, not a claim that all M3JDK21 work is fully integrated.

## Evidence policy

Source/blob seals and isolated reference tests prove custody and bounded behavior, not full M3JDK21 release compatibility or performance. Run product Java21 baseline/candidate oracles, original native/JVMTI C compilation and later full image plus jtreg/JCK/multiple collectors/OS/Android. Record PASS/FAIL/NOT_RUN per exact branch head and retain append-only history. Keep draft until the mandatory Synexia recipe and full product gates complete.
