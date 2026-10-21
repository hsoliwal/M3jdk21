<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original-owner filtered COW bulk mutation convergence

This PR is **direct M3JDK21 Java source**, not an M3-over-JDK adapter. It is stacked on the exact [integrated JVM/array/filter PR #1682](https://github.com/hsoliwal/M3jdk21/pull/1682) and preserves its original `java.base`/native `libjava`, `M3LongIndexProjection` and `M3CollectionViews.isEmpty` work byte-for-byte outside the admitted private method.

Canonical reusable Maven/OpenRewrite recipe and donor choice: [Synexia PR #12728](https://github.com/hsoliwal/com.synexia/pull/12728), `com.synexia.m3.Jdk21CowFilteredOwnerConvergence20261021`. The candidate was source-sealed from the actual product parent Git blob `a99cd0355879a2f941079e8401de00b51cd0093e` and SHA-256 `34c5ebc2cfde388f02dfee4fcaa64d1cb81058210d5dcb261bb76ef01adaf0c7`. Complete new Java source SHA-256 `7975e471f1b1b92d2a4bdbecdc3a79049dea4a3c93342da71f8e8d403cd0ca18`.

## Selected existing donor — no reimplementation

The original *private* `removeMatching` method body comes directly from the already qualified M3JDK21 PR #1630 and canonical Synexia PR #12637. Both donor first-party Apache-2.0 notices and contracts are preserved.

- For **exact** `CopyOnWriteArrayList` or `CopyOnWriteArraySet`, call the existing original JDK owner `removeIf(e -> visible(e) && condition.test(e))` to respect immutable snapshot iterators and atomic publish.
- For any other owner, especially subclasses with overridden `removeIf`, preserve original iterator-based route, callback order, partial mutations and exception behavior.
- All original dynamic array/index/offset/mask owners remain unchanged. No per-row Map.Entry or object, new JNI API, raw heap address or native pointer representation is introduced. The M3JDK21 native mask kernels remain the correct place for bulk primitive bit-vector work; generic Java callbacks stay in Java.

**Reviewable behavioral repair, not strict equivalence:** for exact-class COW bulk removal that previously threw `UnsupportedOperationException`, this atom now uses the owning collection's supported mutation and succeeds. Any caller relying on that earlier failure can observe a change. This must be approved as a contract repair before merging.

## Problem-category catalogue and tests

See [CATALOGUE.tsv](CATALOGUE.tsv) for the existing donor #1630, broader #1619 alternative rejected for subclass effects, OpenJDK COW, LeetCode 27, HackerRank Java List, GFG COW and native-mask boundary. No third-party challenge solution/editorial code copied.

Reuses the exact prior `M3FilteredCowOwnerContract`/JUnit from product #1630, including 209 previously executed checks on that donor source epoch. The current receiver must prove actual product behavior independently. Extra focus includes mixed and `-Xint`, exact Java21 strict compilation, original/candidate declared signatures, original `M3FilteredIsEmptyContract` regression, and full native/JNI/HotSpot integration gates when this stack is promoted.

```bash
bash m3/collections/qualification/cow-filtered-owner-20261021/verify.sh
```

**Current scope:** Git/source SHA-256 identity and exact canonical donor method readback verified. Actual canonical Maven/OpenRewrite SDK and product Java21 contract execution, whole owning Maven module, full source-built M3JDK21/JNI/Android acceptance and CPU/allocation benchmarks are **NOT_RUN** until results are retrieved. Draft only.

Do not overwrite sibling wrapper PRs #1619/#1657 or assume those entire files can be transplanted. Reconcile every additional original file atom at its exact new source epoch and update the canonical recipe rather than manually patching outputs.
