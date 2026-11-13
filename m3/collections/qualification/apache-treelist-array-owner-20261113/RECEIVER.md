<!-- SPDX-License-Identifier: Apache-2.0 -->
# Arrayified Apache TreeList — M3JDK21 receiver DRAFT/HOLD

The existing all-throw `M3TreeList` provider scaffold has been replaced
in this **review-only branch** with a complete, formatted Java 21 class
in `src/jdk.m3.collections/share/classes/jdk/m3/collections/apache/M3TreeList.java`.

**Canonical source:** [Synexia #17275](https://github.com/hsoliwal/com.synexia/pull/17275),
dataset `m3-apache-treelist-array-treap-20261113`, named
`com.synexia.m3jdk.CollectionsContractRefinement20261017` recipe.

## Exact owner, tests and donor

- M3JDK21 baseline `0b7bb3ab0e0e1fec6c5818428dad81ae30e86fbb`.
- Old vendor scaffold at
  `m3/collections/implementations/apache-tree-list/src/main/java/jdk/m3/collections/apache/M3TreeList.java`:
  original Git blob `7202d6ed806037e353516ec13e4a5d30527acac2`,
  SHA-256 `623bf403734c80152bcf59d9b31f733662aece96151536e7c7edc343bec4fa72`.
- Replacement JDK module Java blob
  `06dc7501a8f6efc837ff3e9f716058b9b0bab76e`,
  SHA-256 `68fce448cdc052e43a059f9b1e71c2deb228c5e6c6dd0d3b7789b150cccc4a0a`.
- Target differential `test/jdk/jdk/m3/collections/ArrayTreeListContract.java`,
  blob `5cbd18b9fad8e641a8136fceb19068ef60538896`,
  SHA-256 `4c8236ab15dce9c95b15128a9ed25b1ba354c0ec3d1f57d2651cba1ec007cd43`.
- Original Apache Commons 4.5.0 TreeList remains an independently
  licensed donor/behavior oracle in the received vendor tree, not copied
  into the JDK module source.

## Array payload, offsets and masks — actual code

`Object[] elements`, `int[] left/right/weight/priority`, and
`long[] markMask` implement an implicit order-statistic treap with
rank-based indexing, split/merge, physical free-slot recycling and
detached serialized sequence. No per-element TreeNode, Entry, adapter
or wrapper object is stored. A split returns a pair of coordinates in
one packed `long`, not a heap tuple. Expected O(log n) indexed
lookup/add/remove; amortized storage growth with checked capacity.
Original List/SequencedCollection methods, nulls, fail-fast iterators,
backed reversed/subList, self add and serialization are implemented.

Java 21 source follows four-space JDK styling with Javadoc, `@Serial`
and explicit imports. `module-info.java` already exports
`jdk.m3.collections.apache` due to this PR's first linked-list
change, and no second module or standalone JAR was created.

## Locally executed scope

- OpenJDK 21.0.12.1 `javac --release 21 -Xlint:all -Werror` on the
  **complete candidate** and independent contract source: PASS.
- Deterministic JDK `ArrayList` parity:
  **569,967 assertions / 60,000 operations PASS**.
- Isolated `jdk.m3.collections` module and external test client:
  **569,967 assertions PASS**, using test-only `--add-opens` to inspect
  storage lanes; this is not the whole JDK image.

## Mandatory before merging product draft to master

The actual named Synexia Maven/OpenRewrite recipe has NOT executed;
this branch is not a verified SDK output receipt despite identical
source hashes. Real target source-built Maven reactor, JMOD/jtreg,
JDK API/ABI/reflection and serialization, JNI/GC, original Apache
TreeList semantics, Windows source build and comparative JMH
CPU/heap/native memory all remain HOLD. Do not merge before satisfying
those gates. No canonical recipe code or engine is present here.

## JDK21 strict style successor — 13 November 2026

The source identity `06dc7501a8f6efc837ff3e9f716058b9b0bab76e` (SHA-256 `68fce448cdc052e43a059f9b1e71c2deb228c5e6c6dd0d3b7789b150cccc4a0a`)
remains pinned above as its historical exact implementation receipt.
JDK-style Javadoc-only successor: Git blob `9da0a6ad147c423a0cfa718a8159890cc2c25977`, SHA-256
`ad155c871d40d03dca1412443814e8670e8819c1ccd690f60f09f18e2ce9ca86`; canonical private source packet [Synexia #17302](https://github.com/hsoliwal/com.synexia/pull/17302).

Executed on the full Java owners: OpenJDK 21 strict compilation, `javadoc
-Xdoclint:all -Werror` with zero warnings, and byte-for-byte classfile
parity at `-g:none`. All three seeded 60,000-operation tests remain green.
No Java storage, algorithm, ABI, JNI or public API changes. The real
Maven/OpenRewrite SDK and source-built M3JDK21/JTReg gates are still HOLD.
