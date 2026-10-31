<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 catalogue-first natural-run refinement

This is the **original M3JDK21 product**: the existing `M3LongIndexProjection` Java21 owner uses a private immutable `long[]` payload and primitive `int[]` index directory. No wrapper layer or extra native library is installed.

## Source inventory and canonical recipe

- Product source before Git blob: `d18055008f6eabbd6603e080ff32f8d5a1da8b60`; SHA-256 `7bf939ea374ce6c0b13c403f5e154c0f8d376f9cddb8e623cf00ef299183a77e`.
- Received source SHA-256: `3569c0a8ffd94c6e91b87b9316bbaf20583f7dd56810a7bee57a81c8a9975a8d`, copied byte-for-byte from canonical [Synexia recipe PR #12700](https://github.com/hsoliwal/com.synexia/pull/12700).
- Two Java21 deterministic differential/JUnit test sources are exact copies of the source-sealed Synexia templates.
- `M3LongAddressKernel` and `M3IndexMaskCompactor` remain unchanged; no JNI ABI, M3String owner or HotSpot interaction changes. Existing cross-platform JNI mask kernels and filtered/sorted wrapper work remain in their current source owners and review PRs.
- Consult [category decisions](CATALOGUE.tsv), then the canonical Maven/OpenRewrite recipe and tests. Subsequent passes must update recipe/canonical catalogue and prove the same fixed point rather than manually patching each file.

## Algorithm and contract

A private `alreadyOrdered(descending)` reads the current logical `[from,to)` physical index window with the original signed-long address kernel. It stops on the first inversion. For already monotonic encounter data, `sorted(boolean)` returns a new view with requested sorted metadata but shares the same immutable payload and index array, without copying or merging. For unsorted data, unchanged stable merge remains; known opposite order retains stable equal-value group reversal. Equal-value occurrences never reverse within a tie. Complexity: O(n) reads/O(1) temporary state for admitted ordered case, O(n log n) for unsorted stable merge plus bounded prefix check.

This is not a replacement for backed filtered, sorted, navigable Set/Map/List wrappers. Their comparator, identity, null, mutator, callback, iterator and range semantics remain authoritative. Bulk JNI mask scanning stays native where qualified; no element-by-element JNI predicate crossings are introduced.

## Focused build and proof

```bash
bash m3/collections/qualification/natural-run-20261021/verify.sh
```

The focused product script refuses source SHA-256 drift, compiles the *exact* original Java21 owner and dependencies and original baseline blob with strict javac, verifies public descriptors against baseline, and runs the same standalone JDK stable-index oracle under mixed and interpreter modes. The JUnit adapter also participates in the existing `m3/collections/pom.xml` owning module when that module's pinned Maven dependencies are available.

**Separate acceptance gates:** canonical Maven/OpenRewrite recipe fixed point, full owning module Maven/JUnit, matched JDK/HotSpot images, jtreg/JCK, Android cross-build, existing JNI library parity, and measured microbenchmark for arrays/allocations. The presence of an algorithmic shortcut does not prove an end-to-end speedup.

These are reviewable isolated atoms; do not automatically merge or declare the whole collections/Java API superset complete.
