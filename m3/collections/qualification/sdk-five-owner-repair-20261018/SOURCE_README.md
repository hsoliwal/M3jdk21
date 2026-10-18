# M3 SDK five-provider owner repair, 2026-10-18

**Goal:** preserve and unblock the existing M3JDK21 SDK used by JDK, Guava, Eclipse Collections, Spring and Apache Commons Collections, without duplicating their storage engines or renaming their public types.

## Source-correctness defect

The **actual M3JDK21 master** at `4b24824c3aa09d802121447f8970d442a855f064` contains two identical declarations each of `M3CollectionSdk.readOnlyFilteredSortedSet` and `readOnlyFilteredSortedMap`. Duplicate Java method signatures are illegal. This source-sealed candidate retains the first exact method bodies, removes only the later duplicate declarations, and adds factories for four current M3 array/index owners.

Existing JDK `java.util` surface remains named `java.util.*`; Guava/Eclipse/Spring/Apache interfaces remain their own provider names. The four new factories return distinct **M3** types, **not vendor interfaces**:
- `m3ArrayList<E>()` — existing one-`Object[]` `M3ArrayList`, distinct from existing `jdkArrayList()` which still returns JDK `ArrayList`.
- `indexedTable<R,C,V>()` — existing object cell payload plus primitive row/column/slot-index lanes; not a Guava `Table` implementation.
- `indexedMultiValueMap<K,V>()` — existing `Object[]` value occurrences, `int[]` linked offsets and `byte[]` state masks; not Spring/Guava/Eclipse/Apache multimap-equivalent.
- `longRangeSet()` — existing sorted parallel `long[] starts/ends`, half-open signed ranges; not full Guava `RangeSet` semantics.

The two unchanged existing sorted-filter method descriptors retain their own order/range/visibility contracts. No existing production JNI C, GC object header, source allocator, native pointer, `java.base` API or vendor SDK type is modified.

## Mandatory source-first recipe

Reuse the **existing Synexia** `M3GpuToolJoinRecipe` + `M3GpuToolJoinManifest` as the sealed, serial OpenRewrite PlainText source owner. Only the task-specific `resourceRoot()` and four templates differ; this is **not** an alternate rewriting engine. Named task recipe: `com.synexia.rewrite.M3FiveSdkOwnerRepairRecipe`. The three target manifest rows are one SHA256-pinned MODIFY and two ABSENT-before test sources. Source files are emitted from reviewed templates, not hand-patched in product. Because the source preimage has duplicate declarations, source custody uses OpenRewrite PlainText; attributed Java21 compilation happens separately.

Maven: `mvn -B -ntp -f synexia-openrewrite-recipes/recipe-crates/m3-five-sdk-owner-repair-20261018/pom.xml test`. Tests exercise full candidate, all partial target presence orders, fixed point and drift/duplicate/missing-owner refusal. Product must compile actual current M3 owners with Java21 warnings-as-errors and run the SDK selftest.

## Evidence boundaries

The source SHA-256 manifest and exact Git object IDs were computed from GitHub, but **actual Maven/OpenRewrite, current-master module JUnit, JNI and provider runtime checks are not claimed locally**. Maven/third-party jars are absent from the current offline execution container; focused hosted CI must provide independent receipts. The product's optional interop currently references a missing `M3ObjectCounts` while newer provider adapters depend on a separate `M3CountedBag`; reconciliation of this incompatible count-owner lineage is **HOLD**, not silently solved by importing another count engine. Record that as a separate serial operation requiring source ownership and live provider contract proof.

Source catalogue records LeetCode Remove Element and Merge Intervals, HackerRank array/bit sets and GeeksforGeeks masking as **problem categories**, not copied solutions. All original M3 changes ©2026 Hitesh Soliwal and contributors / Apache-2.0; original external libraries keep their upstream licenses, not copied or relicensed.

Keep PR DRAFT until exact-head CI/compilation passes. STRING→arrays→collections→AST/compiler promotion still requires independent whole-JDK/JTReg/JCK and installed-image acceptance.
