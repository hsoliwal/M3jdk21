<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 product source receipt — bounded JNI long multiset intersection

This is **M3JDK21**, not an M3 compatibility layer. The original product Java and C owners are refined in place. Synexia remains the convergence-workspace owner of reusable OpenRewrite recipes, catalogue decisions and source templates. No Synexia library, recipe, handler or runtime dependency is introduced here.

## Exact canonical source custody
Synexia PR #13224 commit `4c43888aa815b3e231ce199634a9d066d7122692`; source recipe `com.synexia.m3.Jdk21JniLeetCode35020261025` with Java AST and C text hash-pinned snapshots. Product receiver base `ce5bc144337774d46c4b2ac4da0cf42e08f20a13`.

| Original product path | Before Git SHA-1 | Canonical received Git SHA-1 |
|---|---|---|
| `src/main/java/com/m3/collections/M3LongRankBatchNative.java` | `20446b00731fdd0b9524ac35be3860fbc0bb062b` | `fe4bd1751126d1a5c21c359bf9f028013d380a4f` |
| `src/main/java/com/m3/collections/M3LongRankIndex.java` | `fe87db69813f63bf60beeff702eab10cfe306f99` | `f9de7b74124cd1e2f6f32c4e9a34af4178c1d787` |
| `src/main/native/collections/m3_long_rank_batch_jni.c` | `ac7f123549b2b7e0a3788fe9176bb56dee8efe85` | `496e04bc57c00ba66895379229f3dc4e4dc0aa2c` |
| `src/test/java/com/m3/collections/M3LongRankIntersectionContract.java` | ABSENT | `6285beb025d699b8739c75b51ce39b3d3dc5498e` |
| `src/test/java/com/m3/collections/M3LongRankIntersectionTest.java` | ABSENT | `715deec71b1b08ea4beae17a554e187e2b380ee7` |
| `.github/workflows/m3-jni-leetcode350-java21.yml` | ABSENT | `8f5cd5acef1ff1eaed676c9bcd09657e72d92c0c` |

All relative src paths above are under `m3/collections/`. Only the six listed sources/CI results and this data-only receipt are added/replaced; every existing JNI export and all other Java runtime code remains retained.

## JNI and category
LeetCode #350 (duplicate-aware sorted intersection) is a **problem category**, not a copied donor implementation. The product's immutable signed-long rank arrays already hold sorted payloads. Independent two-pointer code counts each common occurrence once, returns `long` cardinality (not a per-entry Long), and computes multiset union cardinality by `(long)left.size()+right.size()-overlap`. New public `M3LongRankIndex.intersectionCount(other)` and `unionCount(other)` are additive. Existing methods, exceptions and signature contracts remain unchanged.

`M3LongRankBatchNative.intersectionCount` retains Java fallback. For opt-in `m3.collections.rank.native=true` and both lanes at least 128 positions, existing `m3rankbatch` JNI performs one native transition. Two 256-`jlong` region pages = 4 KiB native scratch; no full heap payload materialization, pinned GC address, Java callbacks, unsafe pointer identity or per-entry object. Input arrays remain immutable.

## Executed, pending, refusal
**Executed locally:** Linux OpenJDK 21 strict Java harness and gcc C17/Werror/UBSan, 2,008 independent hash-frequency/differential assertions each in Java, `-Xcheck:jni` native and `-Xint -Xcheck:jni` native, PASS. This isolated harness validates native algorithm but does NOT certify compilation of the full original 17 KiB JNI owner or the installed M3JDK image.

The exact-head workflow compiles the five original Java product classes plus the standalone new contract and generates JNI headers, compiles the **entire original C JNI file**, requires the new symbol, and executes the full signed-long/multiplicity oracle in Java-only, JNI and interpreted JNI modes. JUnit wrapper joins the collection Maven suite; actual full Maven/JNI/native, Linux/Windows/macOS, installed JDK, jtreg/JCK, JCK, allocation and throughput gates remain pending.

**Qualification policy: DRAFT/HOLD.** No merge, approval, protected branch update, replacement of `java.base` without contract tests, or release claim until actual current-head SDK/source/JNI proof passes.
