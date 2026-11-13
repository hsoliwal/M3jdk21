<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3JDK21 generic object collection receiver

Canonical recipe owner: [Synexia PR #12148](https://github.com/hsoliwal/com.synexia/pull/12148), commit `65e99fe813ec8f2f017c51d365d1d3953bb8f95b`. The exact Maven recipe and SHA-256 Java postimage templates are mirrored beside this receiver.

M3JDK21 receiver base: `master@e913ab89056657eca4009678cfe9a657c020e79b`. This is an additive, bounded, separately built Apache-2.0 `m3/collections` module. **No modifications** to existing `java.util`, primitive owners, native C/JNI ABI, Maven module dependencies, or active branch heads.

Two serial commits: (1) canonical source-sealed Maven recipe mirror, catalogue, mapping, tests and CI; (2) the recipe-generated five Java source/test outputs with exact template Git blobs. To revise a postimage, fix the Synexia recipe/templates and re-run. An arbitrary existing-source modification must graduate to a true Maven/OpenRewrite AST recipe.

Implemented **actual generic** `M3ArrayList<E>` (mutable growable Object[] + AbstractList List contract) and `M3IndexedListSnapshot<E>` (immutable Object[] + int[] index + from/to offsets + lazily materialized physical BitSet for predicate/sort/slice) plus `M3GenericCollections` typed factories. A Java predicate and comparator are never sent through JNI. Optional explicit JNI delegates primitive positions/mask to the existing `M3IndexMaskCompactor.select`. Existing `M3Collections.arrayList()` concrete factory return type remains unchanged.

Local direct Java21 strict `--release 21 -proc:none -Xlint:all -Werror`: PASS; JDK ArrayList differential **960955 checks** Java and **960583** with real `-Xcheck:jni` and strictly built C17 UBSan. Direct recipe Java fixed-point/missing/drift/symlink oracle: PASS. Counts differ because native-required refusal and actual-native parity take different branches. Local Maven unavailable. Hosted Maven/OpenRewrite and owning module tests, full installed JDK, jtreg/JCK, GC/JMM/serialization evolution, all OS ABI and speed/heap benchmarks remain **NOT_RUN/NOT_PROVEN**.

No third-party solution/source was copied. First-party Apache-2.0 rights and upstream contract reference notices retained. **DRAFT / HOLD** pending full exact-head gates. String → arrays → collections → AST/compiler receiving order not advanced.
