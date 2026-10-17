# Copy-on-write filtered Collection receiving — M3JDK21

**Target:** exact Java21 standalone M3 module commit `c5e5e90e2dcc2298a372ab39640120dd60a2edb7` from existing draft PR #1051. Preserve that branch and its earlier staged addAll, navigable bounded-range, sorted comparator and lazy-stream corrections. Only one existing runtime method is refined, with two imports; no second collection owner, new public factory, API/ABI signature or product JNI symbol.

**Canonical source-first recipe:** [Synexia PR #12041](https://github.com/hsoliwal/com.synexia/pull/12041), commit `16b1a71c8cd05e62d8bcd3031c73fb9ccd355fa6`. Exact path `synexia-openrewrite-recipes/recipes/filtered-cow-bulk-20261016/receivers/m3jdk21-20261017/`. Reuses original first-party Apache-2.0 recipe under source seal. This target only consumes the generated image and publishes a thin test/check boundary; reusable recipe remains in Synexia.

## Contract closure and costs

For backing `CopyOnWriteArrayList` and `CopyOnWriteArraySet` only, `removeMatching` delegates `backing.removeIf(element -> visible(element) && condition.test(element))`. This supports filtered `removeIf`, `removeAll`, `retainAll`, `clear` while protecting invisible/hidden elements. Former `Iterator.remove` failures become successful owner mutations, an intentional correction. The existing `Iterator.remove` loop remains unchanged for ordinary/sorted/ranged/descending owners. The view's public iterator still rejects remove.

COW `removeIf` may allocate/copy the owner array, and interference behavior belongs to the concrete JDK owner. This does not add automatic synchronization, a transactional compound filter, guaranteed atomicity for arbitrary callbacks, native acceleration, or memory-pressure integration. Predicate and comparator callbacks remain in Java. No JNI/C source is changed. Existing Java/JNI test-only peers are reused as inherited checks; neither a production native filtering engine nor new ABI is claimed.

## Source and naming custody

Product Java owner remains `com.m3.collections.M3CollectionViews` (independent Apache-2.0 module) rather than introducing a second `java.util` implementation. This is a receiver extension of the already recorded Synexia filtered Collection mapping, with no new public target name. Full java.base/OpenJDK license review and M3JDK21's ordered `String -> arrays -> collections` promotion remain **HOLD**.

Before SHA256 `115f16592a41ce78a63b4892904cb4fe934f5ee45974ffb3305ec39a7c0b9f6e`.
Generated SHA256 `7fed24335d4b642909a91974fc6c742780c3df2543835b7c518328b95800ef82`.
Exact generated Git blob `f185f2890009107cdef1f195572c49381b66efe8`.

## Reproduce
```sh
python3 m3/collections/wrappers/cow-bulk-20261017/verify.py
mvn -B -ntp -f m3/collections/pom.xml -Dtest=M3CopyOnWriteBulkTest test
# Canonical recipe checkout (with GitHub access to private Synexia):
python3 synexia-openrewrite-recipes/recipes/filtered-cow-bulk-20261016/receivers/m3jdk21-20261017/recipe.py verify
```

Verification states must be kept separate: the **isolated** Java21 rehearsal of the behavioral atom passed 21,632 checks before handoff; the actual strict full receiver/JUnit/Maven/OpenRewrite/native/source-built JDK gates require execution and reporting at the installed head. A success in one lane cannot be imputed to another. Review as DRAFT; do not merge or promote until applicable gates pass.

See `RECEIVER_PIN.tsv`, canonical `CATEGORY_REVIEW.tsv`, and source-first [Synexia #12041](https://github.com/hsoliwal/com.synexia/pull/12041).
