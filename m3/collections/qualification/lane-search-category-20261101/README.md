<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 primitive sorted-lane category qualification

Canonical Maven/OpenRewrite hash-pinned recipe: [Synexia draft PR #15080](https://github.com/hsoliwal/com.synexia/pull/15080), crate `m3-primitive-bisection-category-20261101`. Only generated, source-sealed test artifacts are received here; no fork-local mutation engine or duplicate binary-search implementation is installed.

- **Source owner retained**: `com.m3.collections.M3LaneSearch`, blob `cf0395b89a6eb875a866982f8de390957c239406` at the review baseline. All four existing overloads (`int[]` / `long[]`, lower/upper bound) keep their original public signatures and production bodies.
- **Challenge taxonomy only**: LeetCode LC34/LC35/LC704, HackerRank Searching and GeeksforGeeks Binary Search. Challenge source code and submissions are not copied.
- **Generated tests**: two Java test sources under `m3/collections/src/test/java/com/m3/collections/`. The standalone source compares against independent *linear* lower/upper-bound oracles, sorted primitive lanes, duplicates, all-equal and extreme keys, empty/offset/full windows and verifies no payload mutation. JUnit calls the same oracle in the actual module.
- **Isolated executed evidence**: `javac --release 21 -Xlint:all -Werror` plus `java` passed 80,640 bound comparisons on the original method bodies (no JNI required for this pure primitive kernel). This does not establish a whole-JDK benchmark or performance improvement.
- **Target CI**: existing `.github/workflows/m3-collections.yml` will compile the actual `M3LaneSearch.java` with the exact proof source and execute it; full Maven, JVM/JNI installed-image, comprehensive family and performance gates remain distinct.
- **No merge** until target source CI and the relevant original module acceptance gates pass. No change to String -> arrays -> collections receipt order, JDK21 superset, Java runtime ABI, per-entry allocation, or native lifetimes.

Provenance is first-party test code under Apache-2.0, with no challenge-code borrowings. The existing JDK21 source retain its original licensing and history.
