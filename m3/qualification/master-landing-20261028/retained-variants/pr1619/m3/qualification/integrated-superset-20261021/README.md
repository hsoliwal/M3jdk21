# M3JDK21 integrated Java 21/HotSpot/JNI superset — review receiver
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

**M3JDK21 is the one integrated SDK product.** This is not an M3 plus JDK
wrapper, separate file system, additional AST, duplicate collection payload
owner or JNI service. The existing Java and JNI implementation owners are
the destination; Synexia contains exact reusable transformation recipes,
algorithm catalogues, reviewed donor source and proof custody.

## Real branch ancestry and custody

This **stacked draft** begins at
[hsoliwal/M3jdk21 #1653](https://github.com/hsoliwal/M3jdk21/pull/1653)
commit `1a0d32bde2aa0dab48486a7a5dc0259920dd946b`, already above
[original combined #1624](https://github.com/hsoliwal/M3jdk21/pull/1624),
which retained #1619 collections/JNI, #1613 M3FileTree and #1597 M3AST
donor components. The stacked base includes exact-progress child monitors,
bounded consumption-driven shadow streams, ZIP/JAR directory traversal and
parent-owned collection array/index/mask work.

This continuation receives **20 original Java, C, test and qualification
files**, plus this independent provenance packet. All 20 exact Git blob
postimages were read back from the target branch after *serial, per-file*
source changes. No unrelated Java/C owner was replaced.

| Owner retained in M3JDK21 | Received work | Canonical source authority |
| --- | --- | --- |
| Original M3File and M3FileShadowInputStream | Existing one-read monitored input/output shadows, bounded count and exact integer subprogress from base #1653 | [Synexia #12676](https://github.com/hsoliwal/com.synexia/pull/12676) |
| Original `M3StateMaskHistogram` and `m3_collections_statehist.c` | Capability-probed direct JNI gather of physical int[] offsets and byte[] state masks, old aligned-native ABI retained | [Synexia #12702](https://github.com/hsoliwal/com.synexia/pull/12702), [product #1669](https://github.com/hsoliwal/M3jdk21/pull/1669) |
| Original M3FilteredViews / M3MutableFilteredViews | Two private direct first-visible behavioral atoms, read-only comparator and backed views preserved | [Synexia #12711](https://github.com/hsoliwal/com.synexia/pull/12711), [donor #1643](https://github.com/hsoliwal/M3jdk21/pull/1643) |
| Original `M3FilteredDeques` | **Already merged master** #1633 query-first asymmetric equality fix, with exact current-master regression test | [Synexia #12641](https://github.com/hsoliwal/com.synexia/pull/12641) |
| Original `M3FileCharShadow` and `M3FileTree` | Add one-read `M3Grammar` with immutable `M3Paragraphs` primitive offset, index, state-mask and sorted views | [Synexia #12688](https://github.com/hsoliwal/com.synexia/pull/12688), [product donor #1661](https://github.com/hsoliwal/M3jdk21/pull/1661) |
| Original javac `M3AST` PARSED event + `M3JavacTree` | Add `M3JavacFactImage`, immutable 16-byte-per-node source/build-context-sealed geometry facts, not a new parser or ABI authority | [Synexia #12671](https://github.com/hsoliwal/com.synexia/pull/12671), [product donor #1649](https://github.com/hsoliwal/M3jdk21/pull/1649) |
| Original JDK collection contracts | Add original [#1640](https://github.com/hsoliwal/M3jdk21/pull/1640) jtreg-compatible live sorted/checked/unmodifiable baseline | [Synexia #12656](https://github.com/hsoliwal/com.synexia/pull/12656) |

## Invariants that the receiver keeps

- **Storage:** The parent dynamic `Object[]` or primitive-array owner remains
  authoritative. Row IDs, physical offsets, child/order arrays and byte/long
  state masks are metadata and views, not independently retained element
  objects. Sorted and filtered backed views never copy the backing payload.
- **JNI:** Cross the boundary in batches, on existing primitive lanes; no
  per-predicate JNI callback, global Java reference, unbounded critical pin,
  pointer lifetime extension, or silent `NATIVE_REQUIRED` -> Java fallback.
  Preserve generated headers and original binary export contracts; capability
  probes protect additive exports.
- **File I/O:** No second filesystem or per-file double read. Source-owned
  char shadows consume the decoder's original callbacks; incomplete results,
  change detection and progress limits must remain truthful. Archive and
  provider paths remain owned by original `M3FileTree`.
- **AST/build:** javac parses once. `M3JavacFactImage` is syntax/geometry
  evidence only; it **cannot** by itself authorize semantic/ABI-based Maven
  or Gradle rebuild avoidance. The compiler owns attributed dependencies.
- **Contracts:** Preserve public method descriptors and observe precise
  null, comparator, stored-reference identity, exception/throwable identity,
  callback order, sorted/reversed/descending backed views, ownership, thread
  confinement, serialization and concurrent modification where specified.
  New APIs in this branch are *additive*, not silent public API replacements.
- **Recipes:** For each changed file, use an existing reusable OpenRewrite
  or Maven recipe, source preimage seal, deterministic candidate, differential
  oracle, source drift refusal and fixed-point replay. Never patch generated
  target source to bypass a failing recipe. Improve the recipe and record any
  validated correction in append-only MOP `RECIPE_MEMORY.tsv`.
- **Catalogue:** Distinguish problem categories, admissible GitHub donor code
  and LeetCode/HackerRank/GFG *reference-only* taxonomy. Retain upstream source
  license, author notices, complexity tradeoffs, and rejected choices. Do not
  copy challenge submissions or reclassify JDK GPL Classpath code.

## Exact receipt and test steps

- `RECEIVED_DONORS.tsv`: all 20 selected product file/blob identities, donor
  PR and role.
- `verify_received_blobs.py`: offline, read-only Git-blob identity and
  symlink/path refusal checker. It performs **no source transformation**.
- `ALGORITHM_DECISIONS.tsv`: selected and rejected algorithm families.
- `GATES.tsv`: separation of historical donor receipts from this
  **combined branch's unexecuted** qualification.
- `SOURCE_MAP.json`: precise source and recipient identity.

In a fresh checkout of the combined branch:
```sh
python3 m3/qualification/integrated-superset-20261021/verify_received_blobs.py
mvn -B -f m3/file/pom.xml test
mvn -B -f m3/collections/pom.xml test
# Then original parser module-pack lint -> compile -> tests -> runtime,
# full M3JDK21 make images, -Xcheck:jni, JUnit/jtreg, native-ABI,
# multi-platform/GC and CPU/heap/native-memory regression matrices.
```
The Maven and image commands are **not claimed executed** here. A recipe
manifest and successful blob verifier do not establish semantic preservation.
The existing Synexia Maven/OpenRewrite SDK recipes must actually be run,
including their fixed-point and source-drift-refusal gates, before promotion.

## Deliberate holds — not silently merged or discarded

1. [#1661](https://github.com/hsoliwal/M3jdk21/pull/1661) also changes
   `ThreadPerTaskExecutor`, `M3FileParallel` and introduces
   `M3FileTaskWindow`. These scheduler/HotSpot lifecycle atoms are **not
   received here**: they require their own original-source, failure, interrupt,
   worker cleanup and full product JDK tests.
2. [#1659](https://github.com/hsoliwal/M3jdk21/pull/1659) changes stored
   representative membership in the **same** M3FilteredViews file. Its
   comparator/null/exception impact needs a forward, source-sealed behavioral
   atom recipe after the #12711 transformation; do not copy its whole file.
3. [#1608](https://github.com/hsoliwal/M3jdk21/pull/1608) uses a detached
   `M3File` for provider version and byte observation. Adapt its useful
   metadata/ctime/version/change providers to the existing tree-owned
   M3File/M3FileSource when contracts prove equivalent, without reinstating
   a second file owner. Full Maven/Gradle source/class/ABI invalidation
   is still separately gated.
4. Later commits on `master` beyond the stacked base require exact forward
   reconciliation. Already-merged #1633's two critical source/test blobs were
   received, but that **is not full master convergence**.
5. Android DEX/ART is an optimization donor for compact indexed tables,
   immutable shared data, startup/runtime profiling and appropriate native
   batch kernels—not a directly interchangeable JNI/HotSpot ABI. No Android
   image or phone compatibility gate is claimed.
6. The full Guava, Eclipse Collections, Apache Commons, Spring and JDK
   filtered/sorted/checked/synchronized/reversed contract matrix and
   compiler-driven build-tool/plugin AST consumers remain open.

## Admission state

**DRAFT, do not merge yet.** Target source blob identity and reviewable
Git history are verified. Historical donor tests are carried with provenance,
not represented as fresh execution. No new combined Maven/OpenRewrite,
full source-built Java21/HotSpot/JNI, jtreg/JCK, other-OS or performance
evidence has been collected in this receiving pass. No blanket speedup,
cross-library closure or complete SDK proof is asserted.
