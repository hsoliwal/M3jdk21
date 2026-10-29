<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Collections and arrays receiving handoff

The 36 receiving outputs consist of 32 collection/wrapper outputs and four array
qualification outputs, owned directly by M3JDK21. Existing filtered/sorted owners keep
their callback demand, range/navigation, mutation and stable duplicate-position
contracts; packed selection and bounded JNI buffer processing use the existing owners.

The fresh collection SDK passed four original tests. Strict compilation covered 136
main sources per side. Source-built JUnit discovered 284 tests, executed 282 and skipped
two, with no failures/errors. Effective public/protected callable API remained 199 rows
with no new callable signatures. Declared reflection does change:
M3FilteredNavigableSet explicitly declares inherited Spliterator<E> spliterator(); declared method sets are not identical. Declared method sets are not identical.

Native proof used 46 commands, 27 actual JVMs and 15 checked-JNI JVMs, plus separately
instrumented production C under UBSan. The bounded allocation matrix used 36 fresh JVMs,
three paired forks and six workloads; all 18 pairs had equal checksums and nonincreasing
allocation. It does not qualify throughput: the largest elapsed after/before ratio
was 4.759300. Timing improvement is not claimed.

Fresh array module execution passed 13 plain-Java tests and 13 required-native tests
with checked JNI, plus 187,008 CTest differential cases. The earlier missing-plugin
native attempt is retained as failed; successful attempt002 supplies the native proof.
These fresh module results are distinct from the older focused-fixture receipts, whose
documentation remains unchanged.

Runtime datasets are m3-catalogue-collections-recovery-v1-20261018 and
m3-catalogue-arrays-target-20261018. Original Apache-2.0 source and fixture notices remain.

The canonical runtime recipes and fresh qualification packet are pinned at
[Synexia C1 0a3e7f9f4b2fe5067532cc79b75f4dea2aa6c5b2](https://github.com/hsoliwal/com.synexia/tree/0a3e7f9f4b2fe5067532cc79b75f4dea2aa6c5b2).
The direct runtime owners remain in M3JDK21, based on a0e9d6c0c95960deb21d45f3800e592b890a4d28; canonical recipes
author exact source postimages, without a Synexia runtime dependency. This is a draft
review receipt and does not promote the current STRING phase or mark completion.

The adjacent receiving.json contains exact before/after SHA256 and Git blobs for this
lane's runtime files and C1 paths for their recipe manifests/templates. The original
48-output intake excludes these four new receipt documents, avoiding self-hashes.
This handoff uses the existing text engine (blob 9d0788d3717e017d58345c5c3e62022c2915a3c8) in the new
m3-catalogue-receiving-handoff-recovery-20261018 dataset. Its metadata-only recipe is published after C1; these documents
intentionally do not invent a C2 self-pin.

A new combined M3 VM image, whole jtreg/platform matrix, HotSpot/GC/CDS/JVMTI,
product dispatch/lifetime integration, all eight backends/bitlane and all-phase parity
remain separate gates. The older executor's raw logs are unavailable; fresh recovery
records carry the stated tests. Existing sealed runtime documentation stays unchanged.

## Canonical evidence

- [FINAL_QUALIFICATION.json](https://github.com/hsoliwal/com.synexia/blob/0a3e7f9f4b2fe5067532cc79b75f4dea2aa6c5b2/synexia-openrewrite-recipes/recipe-crates/m3-catalogue-refinement-20261018/collections/recovery-20261019/FINAL_QUALIFICATION.json) — SHA256 a38e8e806048bcce7e3879b6bf9bcafcdfb6fa0addb566127523d59078cedfc9
- [OWNING_SOURCE_JUNIT_SUMMARY.json](https://github.com/hsoliwal/com.synexia/blob/0a3e7f9f4b2fe5067532cc79b75f4dea2aa6c5b2/synexia-openrewrite-recipes/recipe-crates/m3-catalogue-refinement-20261018/collections/recovery-20261019/evidence/OWNING_SOURCE_JUNIT_SUMMARY.json) — SHA256 f8f8f5ef449945077913a1d4880f784a642cb78fb47e70f48ff9dab8bba4a246
- [NATIVE_QUALIFICATION_SUMMARY.json](https://github.com/hsoliwal/com.synexia/blob/0a3e7f9f4b2fe5067532cc79b75f4dea2aa6c5b2/synexia-openrewrite-recipes/recipe-crates/m3-catalogue-refinement-20261018/collections/recovery-20261019/evidence/NATIVE_QUALIFICATION_SUMMARY.json) — SHA256 c4529df80dbbe9b7ffde99af6177baed8b7726284e2c54ab46f99aafea3a78ba
- [ALLOCATION_QUALIFICATION_SUMMARY.json](https://github.com/hsoliwal/com.synexia/blob/0a3e7f9f4b2fe5067532cc79b75f4dea2aa6c5b2/synexia-openrewrite-recipes/recipe-crates/m3-catalogue-refinement-20261018/collections/recovery-20261019/evidence/ALLOCATION_QUALIFICATION_SUMMARY.json) — SHA256 4f96e1fd2e92ef513f50be68c15b17f22fbf3de0512342f21e31a31d757969e0
- [ARRAYS_OWNER_QUALIFICATION_001.json](https://github.com/hsoliwal/com.synexia/blob/0a3e7f9f4b2fe5067532cc79b75f4dea2aa6c5b2/synexia-openrewrite-recipes/recipe-crates/m3-catalogue-refinement-20261018/arrays/convergence-recovery-v1-20261018/evidence/ARRAYS_OWNER_QUALIFICATION_001.json) — SHA256 5e658b007133aea0442f79053593e9d5d12009ff44790217fbd680117fe47b43
- [ARRAYS_NATIVE_QUALIFICATION_002.json](https://github.com/hsoliwal/com.synexia/blob/0a3e7f9f4b2fe5067532cc79b75f4dea2aa6c5b2/synexia-openrewrite-recipes/recipe-crates/m3-catalogue-refinement-20261018/arrays/convergence-recovery-v1-20261018/evidence/ARRAYS_NATIVE_QUALIFICATION_002.json) — SHA256 217bd18dbdfbed2ca3dba376892feb796045fc1076ab52263ebc1a5855fa3bdd
