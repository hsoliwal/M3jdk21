<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 stable descending index-run refinement — original owner

**Authority:** M3JDK21 is the integrated Java21/JNI SDK. This candidate changes the existing private `orderDescending` atom in `M3IndexedListSnapshot` and adds one Java21 contract oracle and an existing-lane JUnit5 runner. There is no external M3-on-JDK wrapper, new JNI export, element Node/Entry storage or public API alteration.

**Recipe ownership:** [hsoliwal/com.synexia#12864](https://github.com/hsoliwal/com.synexia/pull/12864) in the existing `m3-atom-pattern-multipass` Maven/OpenRewrite crate, named `com.synexia.rewrite.M3StableRunReversal20261021`. The existing `M3HashPinnedJavaSnapshotRecipe` binds exact pre/post UTF-8 SHA-256 for all three target files; it refuses mismatched source and reapplication must be a fixed point. No manual edits of its generated postimages were used.

## Source evidence

| Original path | Original blob | Received blob |
| --- | --- | --- |
| `m3/collections/src/main/java/com/m3/collections/M3IndexedListSnapshot.java` | `27c56be2bd960ea9de139766ab40960bdbe91b0a` | `b366564de9ca7d9a6a2bac938ff9d73b308b0e12` |
| `m3/collections/src/test/java/com/m3/collections/M3StableGroupReversalSelfTest.java` | ABSENT | `826f841999480cabdd0a777d201718dc06a60bb9` |
| `m3/collections/src/test/java/com/m3/collections/M3StableGroupReversalTest.java` | ABSENT | `c8bcdbbdbfe0f009920a55eccdcfe6faec8e5657` |

All three received Git blobs were compared against the canonical generated Synexia recipe resources. The source preimage also matched the original `master` owner when this branch was created.

The algorithm visits ascending equality runs from the right, preserving **exact original comparator call order**. It reverses each group in the private int[] and then reverses the entire index lane. Each equal-value run remains encounter-stable, while groups become descending. The old output staging `new int[length]` disappears: O(n) time, O(1) additional primitive-array memory in this atom. Original Object[] ownership, immutable views, slices, physical occurrence indices, comparator exceptions, state masks and optional JNI selection remain as before.

## Algorithm-category comparisons and donor rights

The Synexia catalogue records alternatives: scratch array (existing), two-pointer in-place group reversal (selected), whole-reversal-first (rejected for comparator callback ordering), boxed Collections.reverse and new JNI call (rejected), LeetCode #186, HackerRank Arrays DS and GeeksforGeeks group reversal (reference-only). No challenge/editorial or third-party implementation was copied.

## Validation commands

On a real Java21/Maven checkout, run the canonical recipe tests first:

```sh
mvn -f synexia-openrewrite-recipes/crates/m3-atom-pattern-multipass/pom.xml \
  -Dtest=ApmStableRunReversalRecipeTest test
```

Then, within M3JDK21:

```sh
mkdir -p m3/collections/target/stable-run-proof
javac --release 21 -Xlint:all -Werror \
  -d m3/collections/target/stable-run-proof \
  m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java \
  m3/collections/src/main/java/com/m3/collections/M3IndexPathMasks.java \
  m3/collections/src/main/java/com/m3/collections/M3IndexSort.java \
  m3/collections/src/main/java/com/m3/collections/M3IndexedListSnapshot.java \
  m3/collections/src/test/java/com/m3/collections/M3StableGroupReversalSelfTest.java
java -cp m3/collections/target/stable-run-proof \
  com.m3.collections.M3StableGroupReversalSelfTest
```

The JUnit5 wrapper `M3StableGroupReversalTest` invokes the same self-test inside the existing module test engine. The self-test exercises 9,265 deterministic source-list cases, comparator trace equality to the independent original copying algorithm, nulls, duplicate values, extreme compare results, slice/immutability, and Java state-mask selection. When the existing JNI library is available it also compares the NATIVE_REQUIRED state-mask selection outcome; an unavailable JNI library is not counted as a native success.

**Already executed:** a separately compiled Java21 standalone 33,410-case algorithm oracle with exact comparator transcripts, `--release 21 -Xlint:all -Werror`, PASS.

**NOT YET EXECUTED:** this actual M3JDK21 owner compile and contract self-test, real Maven/OpenRewrite SDK `Recipe.run`, full M3 collections/filtered/sorted tests, source-built JDK21/JNI/HotSpot, jtreg/JCK, Windows/Android and before/after performance measurements. Promotion stays subject to these gates. Tests need the original Maven/JDK CI to run before default-branch acceptance.

## Performance caution

Synexia's [preliminary allocation/timing receipt](https://github.com/hsoliwal/com.synexia/blob/recipe/m3-stable-run-inplace-20261021/synexia-openrewrite-recipes/crates/m3-atom-pattern-multipass/STABLE_RUN_REVERSAL_LOCAL_MEASURES_20261021.tsv)
measured one run on a stock Linux Java21 container. The original path allocated
two int[n] arrays per invocation including the common caller clone; the new
path allocated one. At n=1,024 the thread allocation was 8,224 vs 4,112 bytes.
**Latency was worse for the candidate at several sizes**, so this PR claims
only the structural elimination of O(n) temporary scratch, not a broad
throughput gain. Production promotion needs repeated source-built M3JDK21
benchmarks, memory/GC receipts and comparator/JNI contract gates.
