# M3 primitive-long sorted snapshot — 17 October 2026

**Product receiver:** `hsoliwal/M3jdk21`, pinned baseline `master@970a34350ebdf6a3f1c5ff5acc8445d0f3515f55`.

**Source-changing unit:** existing `M3HashPinnedJavaSnapshotRecipe`, named
`com.synexia.m3jdk.LongSortedSnapshot20261017`. Exact `ABSENT` preimages, SHA-256 sealed
Java and proof templates, Java LST creation, replay/fixed-point and occupied/drift/non-Java
refusal are defined in `m3/collections/snapshot-recipe-20261017`.
The matching source-producing recipe is first-party and committed before installing any output.
Do not manually edit generated product Java; change the template + manifest, re-run recipe and
qualify again. Synexia remains a convergence/donor workspace; M3JDK21 is the product owner.

## API and data structure

`M3LongSortedSnapshot` is an opt-in **detached immutable snapshot**. It does not replace
`M3LongSortedViews.filter`, `M3LongFilteredView`, or any pending generic filtered/sorted PR.
At construction it reads the primitive sorted source once, validates signed-natural strictly
increasing uniqueness, evaluates any filter once per source value, temporarily stages through
the existing `M3LongArrayList` and retains only one immutable `long[]`.
`fromAlignedMask` reuses the existing `M3IndexMaskCompactor.selectLong`. The exported
snapshot never retains the source or predicate and does not add per-row Entry objects.

Source `M3LongSortedSet` → transient `M3LongArrayList` / existing signed mask compactor →
immutable dense `long[]` → O(log n) lower/upper bound, rank/range count/navigators;
O(1) select; bounded slices share the primitive backing.

Construct: O(n) time and O(k) final primitive data for k retained values. Navigation, rank,
contains and range count: O(log k). Select/size: O(1). Creating a bounded view: O(log k)
time and O(1) *additional* retained payload. A narrow view retains the **whole** underlying
snapshot array, intentionally avoiding a second copy; `toArray` explicitly materializes.
No ever-valid live index is claimed for externally mutable owners without a version contract.

The sorted/immutable/sized spliterator bits are valid only because this output is detached.
All mutators, including no-op mutators, throw. No existing public interface/method or
`java.base` type changes. The source's sorted iterator is required to be interference-free
during one traversal; external mutation during construction is unsupported.

## Java, JNI and performance boundary

No new JNI symbol: arbitrary Java predicates do not cross JNI. Existing primitive signed-mask
JNI stays in the separate qualified `M3IndexMaskCompactor` owner, and this feature calls
its established `selectLong` Java primitive reference lane for mask inputs. Performance
advantage is only expected when snapshot construction can be amortized over repeated queries.
There is **no measured JMH/JFR/RSS speedup** and no claim of better performance for one query.

## Focused proof and mandatory gates

Local JDK21 `javac --release 21 -Xlint:all -Werror` and standalone oracle:
`M3_LONG_SORTED_SNAPSHOT_PASS|assertions=472524|randomRounds=256`.
This isolated proof used the actual candidate Java with lightweight supporting-contract
shims, **not** the entire product source or Maven module. Target exact-template Git blob
readback matches the locally tested source.

The exact-head workflow `m3-long-sorted-snapshot-20261017.yml` requests:
1. source manifest SHA-256 and installed target/template identity;
2. strict compilation of real M3 product dependencies and the standalone oracle;
3. interpreted `-Xint` replay of the randomized oracle;
4. actual incumbent Maven/OpenRewrite Java LST/JUnit generation + second-pass fixed point.

Do not claim those hosted gates passed until results are inspected. JNI full-platform, full
source-built JDK, jtreg/JCK, GC/native lifetime and all existing collection contract suites
remain separate verification obligations. Keep draft until admitted.

## Problem-category and donor evidence

See `CATEGORY_CATALOGUE.tsv` and `DONOR_PROVENANCE.tsv`. JDK and first-party M3 mechanisms
were chosen; Guava documentation and LeetCode/HackerRank/GeeksForGeeks are comparison and
adversarial-test *evidence only*. No external problem solution or third-party implementation
source is copied. OpenJDK per-file licensing remains unchanged; Apache files do not relicense
the JDK.
