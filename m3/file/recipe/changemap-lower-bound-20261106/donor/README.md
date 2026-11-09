<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File ChangeMap lower-bound optimization — source-sealed Java 21 recipe crate

## Scope and authority
- **Canonical source-changing recipe owner:** existing `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe` in `synexia-openrewrite-recipes`; this crate only supplies its new exact target image, task manifest, proof and donor references.
- **Product owner (not changed here):** `hsoliwal/M3jdk21`, original `m3/file/src/main/java/com/m3/file/M3FileEditBatch.java`.
- **Pinned product master:** `0ab28ea86ccb2b7c1997e662d1555ece229c05de`.
- **Preimage Git blob:** `87dd9d8293de46d8843c09b2109063b11e682b54`.
- **Source SHA-256:** before `7612ebe067c2602e4c3efcf6df2824e173ca4d858e85f5395abd0e5e60b890b9`; candidate `6329ee71cc7921d6480d83b80a9e5d528d834adbb37f9a2e24349ad24e11f52d`.
- **Mechanical scope:** ONE Java file; replace only `ChangeMap.oldToNew` and `newToOld` and add one private primitive lower-bound helper. Public API, record layout, constructors, descriptors, source ranges and `touchesOldSpan` remain untouched.

The old per-coordinate query scans up to E ordered changes (O(E)); the candidate uses O(log E) first-boundary binary search, no allocations, while preserving first-equality affinity semantics for repeated `newStart` values after adjacent deletions. This is a complexity analysis, **not** a measured runtime speedup.

## Executed local Java 21 proof
```sh
mkdir -p target/classes
javac --release 21 -proc:none -Xlint:all -Werror -d target/classes \
  src/main/java/com/synexia/m3/verification/M3FileChangeMapBinarySearchProof.java
java -cp target/classes com.synexia.m3.verification.M3FileChangeMapBinarySearchProof
```
A deterministic 50,001-image / 9,341,740-comparison differential smoke run completed on JDK21, including both affinity directions, insertion/deletion/adjacent ranges, duplicate destination coordinates, valid endpoints, out-of-bounds and null-affinity exception classes. `mvn -B -ntp -f synexia-openrewrite-recipes/crates/m3-file-changemap-lower-bound-20261029/pom.xml verify` binds the same proof to Maven. **Maven was unavailable in the local proof environment**, so Maven verify is not claimed executed.

## Apply the canonical OpenRewrite recipe to a disposable full-source clone
The reviewed class, manifest and postimage are in `synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-file-changemap-lower-bound-20261029/`. The canonical descriptor is `synexia-openrewrite-recipes/src/main/resources/META-INF/rewrite/m3-file-changemap-lower-bound-20261029.yml`. It may be activated by its ID `com.synexia.m3.M3FileChangeMapLowerBound20261029` with the repository's existing OpenRewrite plugin and classpath, on a clone where the Java source path is repository-relative `m3/file/src/main/java/com/m3/file/M3FileEditBatch.java`.

**Dry-run first**. The existing Java snapshot recipe refuses source-path drift, missing preimage, incorrect hashes, duplicated source and non-Java parsing. Run the actual OpenRewrite integration, postimage JDK21 compile, M3File unit and regression tests, full M3JDK21 build and runtime acceptance before sending postimage to the product owner. Re-run from the original source; check fixed point and hash exactly. Keep the original immutable generation for comparison.

## Canonical SDK replay tests (not yet executed)
The committed `M3FileChangeMapLowerBoundRecipeTest` uses the existing Java hash-pinned
OpenRewrite recipe and the exact original Git source as a parseable `J.CompilationUnit`.
It checks the one-file scope, exact before/after Java source, independent fixed-point replay,
drift/missing-original refusal and unchanged source outside the two queried methods.

```sh
./mvnw -B -ntp -pl synexia-openrewrite-recipes -am \
  -Dtest=M3FileChangeMapLowerBoundRecipeTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## JNI, licensing and subsequent file-local passes
This scalar O(log E) query deliberately stays Java: JNI crossing overhead can overwhelm microqueries; benchmark a future **batched** native query only with direct canonical buffers and Java parity. LeetCode/HackerRank/GeeksForGeeks provide category and edge-case ideas only; no challenge code, editorials or submissions are copied. `CATEGORY_REVIEW.tsv` records dispositions; any GitHub source reuse must have exact revision, license and NOTICE evidence first. Next possible atom is `touchesOldSpan` but it is excluded until its own proof and recipe. No default branch was merged or rewritten by this crate.
