<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 source-sealed byte-mask algebra receiver

Parent product source is draft [#1121](https://github.com/hsoliwal/M3jdk21/pull/1121)
at `2155e443be691aa45386313889987c0a5ddd9552`. The reusable authoring
recipe, Maven SDK tests, original preimages and candidate source live in
canonical [Synexia #12123](https://github.com/hsoliwal/com.synexia/pull/12123).
This PR mirrors those exact recipe input/output files; **never patch receiver
postimages independently**. Existing `M3IndexMaskCompactor`, its C17 JNI
library, `M3LongDag`, `M3LongFilteredView`, sorted views, backing arrays
and `java.base` are unchanged.

Two public source-level replacements only:
`M3LongMaskRows` and its `M3LongMaskRowsContract`. Additive methods:
`excludeFlags`, `rankBefore`, `sourceRowAtRank`. The JAVA source-row
selection branch uses two scans of immutable parent-owned `byte[]` masks,
writing one exact `int[]` result; no O(range) intermediate index/state
arrays. The pre-existing JNI backend stays explicitly opt-in, using temporary
primitive eligibility metadata to express forbidden state masks. Absolute
original row ordinals, stable ordering, live-view mutation limits and
same-contract behavior remain specified.

The Maven qualifier mirrors canonical `M3Jdk21LongMaskAlgebraRecipe` over
the existing `M3HashPinnedJavaSnapshotRecipe` and its JUnit fixed-point,
drift and four mixed-resume tests. The exact before/after SHA-256 manifest
is shared byte-for-byte with Synexia. Existing owner Git blobs are pinned
in `OWNER_GUARDS.tsv`; `outputs.sha256` pins resulting Java postimages.

From a complete current receiver checkout:

```sh
sh m3/collections/qualification/m3-long-mask-algebra-20261017/verify.sh
mvn -f m3/collections/qualification/m3-long-mask-algebra-20261017/pom.xml test
mvn -f m3/collections/pom.xml test
```

The first command actually compiles Java 21 against current real owners and
builds/runs the existing C17 JNI kernel with UBSan and `-Xcheck:jni`.
It **does not** assert OpenRewrite SDK execution: the separate Maven task
must pass. The isolated Java probe from canonical authoring passed 2,602
checks at 16/32 MB heaps; the simulated-native-branch test passed 3,253,
but did **not** invoke native C. Full product checks remain pending until
executed; keep this PR draft. No force-push/rebase of append-only mainline.
