<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 primitive map empty-value search — one serial source atom (DRAFT / HOLD)

## Inventory and contract freeze

Canonical receiver: `m3/collections/src/main/java/jdk/m3/collections/M3LongLongHashMap.java` on frozen `master@b0fbe13b7feb437b39cdd1069ff49415f4d50bc0`. Source Git blob `9a84487f7a66257ce5e42e2e31698068496f49d9` and SHA-256 `1758a7688eb0d5b47e4b7ab06074a77042519d42a2d4ba976d3d330f108e969b`. Reviewed one-atom postimage Git blob `4ebceb9bf2f66a15c1ebc931169af8a5913fc6f5`, SHA-256 `11b444a76a3e024e6a4f4bddbd3be30b440b5e46d29be8b680252f93ef0e041c`. Comparison is precisely two added source lines, zero removed, all other source bytes unchanged.

Problem shape: primitive open-addressing hash table, *linear value search* over a parallel occupancy lane. The existing `containsValue(long)` scans every capacity slot even when `size == 0` and no value is representable. The reviewed candidate inserts **one size guard** before that scan: in an empty map `containsValue(v) == false` for every `long v`. It leaves the non-empty search loop identical. This changes the empty case from **O(capacity) to O(1)**, not the non-empty case; it introduces no array, cache, JNI symbol, branch-specific hash function, or other mutable state.

Lock public Java/JDK interfaces, method descriptors, exceptions, map capacity, size/occupied/tombstones, modCount, iteration order, zero/negative key/value, aliasing and all JNI/native contracts. The only behavior change is work avoided on a provably empty state. This is not a modification of `java.util.HashMap`, the standard JDK, or the Synexia authoritative LeetCodify catalogue.

## Donor and category review

Reusable algorithm taxonomy comes from existing Synexia `crates/m3-mop/leetcodify/collections`. LeetCode 705 (Hash Set) and 706 (Hash Map), HackerRank Hash Tables / Dictionaries, and GeeksforGeeks hash table/linear-search practice are **concept/complexity metadata only**. The actual Java candidate is first-party M3JDK21; `java.util.HashMap` is an independent behavior oracle. No challenge statement, solution, editorial, or foreign source code is copied. Existing donor licence and code-loss rules remain in force. No held backward-shift deletion or probe-reordering optimization is admitted.

## Reproducible source-sealed Maven recipe

Named recipe: `com.synexia.m3.PrimitiveMapEmptyValueScan20261105` delegates to the **existing** M3 collection Maven recipe crate `make/m3/collections/recipe/pom.xml`, which already compiles the inherited Synexia `M3HashPinnedJavaSnapshotRecipe`. The single target manifests under `m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-empty-value-scan-20261105/` pin full pre/post hashes. The test `M3LongLongHashMapEmptyValueRecipeTest` executes real OpenRewrite application, SHA-256 output, second-run fixed point, required-source and drift refusal. This adds **one named child recipe**, not a duplicate parser/catalogue/engine.

## Verification and admission boundaries

**Executed locally:** original and candidate `containsValue` method bodies (exact reviewed scan and two-line guard) in a JDK21 isolated fixture, `javac --release 21 -Xlint:all -Werror`: 8,000 differential checks under `java -Xint` and 8,000 under JIT, both pass. The empty 1M-slot table microbench was informative only: old scan 7.56s vs guarded 22.9us interpreted over 500 calls; old 151ms vs 16.4us JIT. These are **isolated method measurements**, not full-map or physical hardware throughput evidence. The newly added full-owner proof, Maven JUnit, and real OpenRewrite SDK have **NOT_RUN locally**; they are mandatory CI gates.

**CI sequence** (with exact source hash and no hidden replay): source SHA and public bytecode descriptor comparison -> Java21 full-owner compilation -> baseline/candidate seeded `HashMap` behavioral proof -> existing Maven/OpenRewrite recipe JUnit -> existing collections Java/JNI gate -> review. Until these are executed, do **not** merge/promote. The CI script creates only one confined `m3/collections/target/` evidence directory and refuses occupied output.

## Explicitly held scope

Existing fast hash probes, backward-shift deletion, JNI memory allocation, other collection classes and every branch of `hsoliwal/*` are *not* implicitly modified. No cross-repository mass merge, rename, source deletion, release or claim that all collection APIs have been optimized. Future technique substitution requires the existing per-owner catalogue, deterministic differential proof and workload contract; timing regressions remain HOLD.
