<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 multiset continuation

Canonical implementation, category comparisons and full scope map: hsoliwal/com.synexia PR11791, commit `9b0151a2e747fffd80a1a0f2bd88b830ec2c2440`, path `synexia-openrewrite-recipes/recipe-crates/collections-library-interop/multiset`. Its full24-file subtree is `2efcd27a484bb8f692deab62a41c9f56483dbe3f`.

The 17 execution files in `multiset-recipe/` are byte-identical to that canonical crate's execution files; their exact subtree is `c2a7160857e5754c61d7ea2ae32eae7ab875c52e`. The recipe and both proof classes are not forked or replaced. Five inherited interop templates and both existing snapshot engines remain unchanged. The source profile deliberately uses the existing SDK exact-PlainText mode, then strictly compiles/tests its Results; it is not an attributed Java refactoring claim.

The six added runtime/test/native files are the exact output of actual local Maven/OpenRewrite execution `actual-sdk-05`. `qualification/multiset-recipe-receipt.tsv` binds all11 outputs, including the unchanged five inherited files. That local run passed8 recipe tests,1 gated delivery test, all43 runtime JUnit tests, strict compilation of78 core sources with java.base-only dependencies, Java3,608,412 assertions /19,200 seeded operations, and checked JNI plus fatal UBSan3,608,442 assertions each. Repetition does not multiply coverage. The current-head receiving workflow independently repeats actual SDK execution, runtime proof and comparison against the committed source; inspect its observed outcome before claiming hosted qualification.

## Usage

```java
var bag = java.util.stream.Stream.of("alpha", "beta", "alpha")
    .collect(com.m3.collections.interop.M3Multisets.toMultiset());
int previous = bag.add("alpha", 4); // 2
int count = bag.count("alpha");     // 6
long total = bag.longSize();         // 7
```

`M3Multiset<E>` implements Guava Multiset over the existing M3 stable-slot map and a primitive int occurrence lane with recycled slots. It preserves null support, zero no-ops, old-count add/remove/setCount returns, per-element int limits, live remove-through views, iterator removal and insertion order. Collection.size saturates; longSize is exact. Counts have no boxed storage; **the generic directory still stores one boxed Integer row ID per distinct key**. No retained Entry node is used for storage; entrySet creates immutable boundary snapshots. Elements retain caller ownership and stable hash/equality requirements. No new interner or universal OS-backed residency is proved.

`M3Multisets` supplies independent M3-owned max/min/checked-sum/floored-difference snapshots, copies and occurrence containment. These are not Guava live algebra views. Its collector preserves ordered result semantics and uses private parallel accumulators rather than concurrent mutation.

`M3CountAlgebra.combine` is Java. Native calls require explicit `loadNativeLibrary` followed by `combineNative`; linkage failure is not hidden. JNI operates on already key-aligned int lanes, not unrelated row IDs. Bounded region transfers use3x1024 int scratch arrays (12,288 payload bytes, excluding frame/JVM overhead), never retain heap pointers, return new output and do not mutate inputs. Inputs must remain stable. The native helper is optional and is not automatically invoked for ordinary collection operations. No speedup is claimed.

## Reproduction and boundaries

Run `multiset-recipe/pom.xml verify` with the Maven properties used in `.github/workflows/m3-collections-multiset.yml`; Java21, Python3, GCC/C17, nm and JNI headers are required. Provider versions are compatibility pins, not security/latest-version endorsements. The preview jar contains only main classes, not tests. Preserve repository LICENSE/NOTICE and dependency notices. All new implementation is Apache-2.0; existing M3 owners and engines retain their original notices. Synexia PR8976 object-bag algebra and bounded JNI region atoms were adapted with their different API profile documented in canonical THIRD_PARTY_NOTICES.md. No Guava/Eclipse/Spring or competitive-platform implementation body is copied.

This does not implement every collection family, concurrent or serialized Multiset, fully primitive generic directories, universal String residency, full donor allocation closure, installed-JDK admission, or benchmarks. Existing first-release/static/whole-repository gates are not replaced by this focused proof. The private Synexia hosted attempt failed before any job steps ran; local execution is not labelled as a successful private-repository workflow. Keep both PRs draft and unmerged. No develop rebase, force-push or parent-branch edit.
