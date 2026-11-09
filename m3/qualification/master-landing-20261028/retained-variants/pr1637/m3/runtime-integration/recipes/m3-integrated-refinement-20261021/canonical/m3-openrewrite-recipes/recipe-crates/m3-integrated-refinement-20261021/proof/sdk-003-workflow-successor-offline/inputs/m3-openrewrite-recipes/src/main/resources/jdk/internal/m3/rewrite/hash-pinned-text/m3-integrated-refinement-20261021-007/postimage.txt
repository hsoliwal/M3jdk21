<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Selected physical-index sort and exact JNI packaging

This packet targets M3jdk21 `2779aba3f98f31c324be43f731be7914e1a05935` through the canonical Synexia Maven/OpenRewrite receiver. It changes the existing `sortedMaskedByPhysicalState` method to sort the new selection's privately owned `int[]` directly, using the same stable bottom-up sorter already used by `filteredSorted`. Canonical object storage, source indexes, slice boundaries and lazy membership remain owned by the existing snapshot. The full 32-bit state lane is borrowed only during selection. No comparator or object reference enters JNI.

The separate strict endpoint restoration receives four exact files from PR1554 at `987f743658055252bdaf056610a3a903eba0bfee`. PR1616 and PR1619 contain byte-identical overlapping files; their other changes remain independent. PR1603 restores only the private sorted owner. Current baseline private and public-facade witnesses both produce `NoSuchElementException` on an empty filtered result. The intended repair restores immediate `UnsupportedOperationException` before callbacks, guarded reversed traversal and sorted-split metadata. These are declared defect repairs, not blanket behavioral equivalence. `M3FilteredNavigableSet.removeFirst/removeLast` become explicit concrete declarations; effective erased callable descriptors are preserved, while declaring-class and `Method.isDefault()` reflection intentionally change.

The existing module pack builds the unchanged compactor and window C translation units into `libm3_collections_mask.so` and the unchanged path selector into `libm3_collections_pathmask.so`. It checks exact named generated-header and export sets: eleven mask functions (nine compactor plus two window), and one path-mask function. Missing, extra, duplicate and same-count substituted symbols refuse. Every C unit compiles with its own actual generated header. The original packager and its `objcopy --strip-debug` linked-image comparison remain in use. Raw pre-strip and installed hashes are deliberately recorded separately.

The source-built JUnit launcher prerequisite receives the exact Apache-2.0 verifier from PR1618 at `6198bd110f16a09c572324d716f181c92ca84d38`. It fixes repository-relative provenance lookup and the existing coordinate/source-jar receipt schema. The source Java, EPL2 license, POM and provenance manifest remain unchanged. The existing full Java/JNI gate adds that owner after the platform engine, and its existing workflow includes the launcher path in both pull-request and push triggers. Native inventory, fatal UBSan flags, owning tests and zero-native-skip checks remain intact.

## Verification

Run the emitted packet's `verify.py` with an exact frozen baseline, Java21 development toolchain, fresh output directory and the target checkout. It compiles all actual main sources under strict warnings, executes the independent insertion oracle, compares the incumbent comparator trace and failure behavior, checks effective API/reflection observations, builds both actual libraries and the linked image through the owning pack builder, verifies Java/native/missing-library behavior, and measures per-thread allocated bytes. The owning Maven suite and full C/JNI lane are separate gates; their actual receipts must accompany publication. Historical PR results and a recipe pass cannot replace target execution.

```sh
python3 m3/collections/qualification/masked-sort-fusion-20261021/verify.py \
  --repo-root . --baseline-root /path/to/frozen/baseline/repo \
  --jdk-home /path/to/jdk21 --out /path/to/fresh/qualification
python3 -m unittest discover -s m3/tooling/module-packs/collections/tests -p 'test_*.py'
```

The stable insertion oracle is algorithmically independent from the merge sorter; the existing two-step selection/sort pipeline is used only to check exact callback order and exception identity. Fixtures cover repeated references, equal values, nulls, empty and singleton selections, high-bit and conflicting masks, reordered/sliced inputs, caller-array detachment, source and membership preservation, exported-directory isolation, bad alignment and missing required JNI. The required-native process must fail if its actual library is unavailable. Packaging runs six existing/new native contracts in both Java and native modes on the host and installed image, alongside the three existing projection contracts.

## Resource and acceptance limits

With `n` logical input rows and `k` survivors, selection remains `O(n)`, stable sorting remains `O(k log k)` with `O(k)` scratch, and persistent result indexing remains `O(k)`. The method removes an extra `k`-element directory copy and temporary snapshot wrapper. The packet records raw allocation observations using `ThreadMXBean` under `-Xint`, a volatile escaping result, identical inputs, warm-up, backend and sample count. Elapsed nanoseconds are diagnostic only. Those observations do not establish JIT throughput, cold-start savings, reduced retained heap, native/process-memory savings or all-workload speedup. No native algorithm is replaced.

This independently licensed collection module is a proving ground. It does not promote a `java.util` backend, change the global STRING qualification stage, establish a full custom HotSpot/JDK build, or transfer prior PR/CI status to the current receiver. Other OS/architecture/GC/JIT/concurrency/provider gates remain pending unless separately executed. Source-built JUnit owner evidence is separate from cached Maven/SDK plugin dependencies. Publication and exact-head CI status are recorded by the receiving work order.

## Provenance and reusable lessons

The scoped source identities and newer-PR comparisons accompany the receiver handoff. Existing Apache-2.0 headers and notices remain intact. The first-party test and tooling additions retain Copyright 2026 Hitesh Soliwal and contributors. No third-party challenge/editorial/submission code is copied. The linked category decisions are in `m3/collections/algorithm-catalog/masked-sort-fusion-20261021.tsv`; they distinguish licensed existing-owner reuse from problem-pattern discovery.

The canonical recipe's correction memory must retain the observed baseline endpoint failures, stale one-export ABI refusal, the rejected compactor-only nine-symbol closure, unavailable source dependency/environment attempts, exact pre/post source seals, regression/refusal fixtures and fresh target results. Retain unsupported-layout fixture refusal if observed; Java probes belong under `src/test/java`. A missing or stale prior workspace receipt remains missing and is not recreated as a claimed historical pass.
