<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Direct M3JDK21 sorted-long page compaction receiver

**Product, not an M3+JDK integration layer.** This changes the original `com.m3.collections.M3LongTreeSet` owner in place. It introduces no replacement collection backend, retained per-element object, extra payload lane or JNI symbol. The existing `M3IndexMaskCompactor` JNI implementation continues to own already-precomputed mask kernels.

## Canonical source
Reusable Maven/OpenRewrite engine: `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe`; named recipe `com.synexia.m3.M3LongTreeMaskCompaction20261022` from Synexia branch `work/m3-longtree-mask-compact-recipe-20261022`. Serial, per-file SHA-256 verified postimages:
- `M3LongTreeSet.java`: before `d9a2059a27e8dd3a15ab01a3d18f26035a01a523a9025b2f1e232d41a9991625`, after `2e8510d8abe6b9e65b9bf47e0e4548c7a351c5dfb5e70d4d9bd6975284f9f638`.
- `M3LongTreeSetMaskCompactionContract.java`: create-only `7f1f00afbec94d40b24c25256ac531ec754fe6bae856f2001d6f8d3944576e25`.
- `M3LongTreeSetMaskCompactionTest.java`: create-only `78daa6148900e6645b7b824df4a50ce8ab40ca073d5679aae6e59fd0c52968ec`.

## Contract
The previous filter evaluated callbacks against the untouched `long[]` owner and retained an entire second `long[size]` array for accepted values. New code retains accepted decisions in compact bit masks, then writes the surviving ordered run into original physically indexed pages **only after callbacks have finished**. If a callback throws, earlier rejected positions are removed; the throwing position and untested suffix remain. Page-slot identity and free-list geometry, `pageMaxima`, `pageCounts` and iterator `modCount` are updated without another payload array. Signed-long sorting and public signatures stay as before.

## Actual / pending proof
- **PASS (isolated algorithm model, not checked-out product class):** Java21 `javac --release 21 -Xlint:all -Werror` and 3,600 deterministic cases with permuted physical page slots, throwing callback prefixes and callbacks reading the owner before commit. This does not qualify original-source compilation or JUnit.
- **PENDING:** GitHub Actions exact product Java21 compile and 1,280 deterministic original-owner scenarios in mixed and `-Xint` modes. Product JUnit remains required.
- **PENDING:** Canonical Maven/OpenRewrite SDK actual execution, replay fixed point, drift/duplicate/missing-source refusal.
- **PENDING:** Full M3JDK21 source build, jtreg, JNI parity/ABI, all collections and FS providers, multi-platform stress and JMH. No memory or throughput speedup claimed.

The old transient *payload* budget is `8*n` bytes; the new temporary primitive mask payload is `8*ceil(n/64)` bytes (excluding array headers/owner lanes). This is analytical allocation geometry, not measured GC/memory performance. Existing C/JNI paths are unchanged because invoking arbitrary Java predicates from C would break effect and exception boundaries.

See the canonical Synexia problem category catalogue for GitHub first-party donors, LeetCode #27/#283, HackerRank Dynamic Array and GeeksforGeeks removal shapes, with independent contract/license decisions. **No challenge or editorial code has been copied.**
