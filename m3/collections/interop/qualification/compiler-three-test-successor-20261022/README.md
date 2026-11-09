<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 strict Java 21 five-library compiler test-source successor

**Product**: standalone M3JDK21. **Recipe authoring**: Synexia only.
This receiving proof changes exactly three existing **Java test** sources and
no Java/JNI production implementation, JDK API, native ABI, collection payload
storage or linked-index/sorted-mask algorithm.

The canonical Synexia `M3Jdk21InteropThreeTestCompile20261022`
Java/OpenRewrite recipe owns the before/after source images. One atom per
file fixes the three actual Maven testCompile errors in GitHub workflow
`39765922097`:

- `M3ApacheCollectionsTest`: use scoped deprecation suppression on its
  Apache provider test class. `-Xlint:all,-processing -Werror` stays active.
- `M3FiveLibraryKeyRangesTest`: add missing `java.util.Map` import.
- `M3FiveProviderFlagSelectionTest`: replace an undefined factory name
  with the existing original M3 owner `M3LibraryCollections.springMultiValueMap()`,
  which returns `M3SpringArrayMultiValueMap` with original
  `M3LinkedHashMap` + `M3LinkedList` storage.

## Preserve each source generation separately

The historical M3JDK21 36-owner source census (`a720780b`) and the next
36+7+40 source generation (`4bd27a6`) are separately immutable. The
existing `verify_superset.py` and all its 36-source, seven-revision,
forty-addition and original one-test receipts remain unchanged.

The new `verify_compile_successor.py` runs the **unmodified earlier
verifier against its actual pinned predecessor checkout and historical
checkout**, then compares current against previous source bytes for all 76
production/test Java paths. Only the three names in
`receipts/changed-compile-03.tsv` may differ; each must match both
before/after Git object and SHA-256 and exactly one approved source-atom
replacement. Any additional/removed/aliased source, invalid receipt,
wrong source epoch, missing license, or change to any of the other 73 Java
files fails. The new receipt never relabels earlier hashes as current.

Six independent Python negative tests cover path sets, missing/symlinked
receipts, forged hashes/recipe IDs, reordered/duplicated rows, exact
single-atom changes, and Git blob checksum framing.

## Qualification

The original full `M3 four-library receiver admission` workflow retains
the original six Maven/OpenRewrite recipe checks, Java21 core compilation,
the real Guava/Eclipse/Apache/Spring/JDK vendor collection test suite,
original junit result test count, and existing JNI/native proof. No
assertions or tests are deleted to make it pass.

The source-snapshot gate is **not** equivalent to vendor API/serialization
or JNI compatibility proof. A newer explicit 158-method Surefire method
census is separately being qualified in PR #1842; the legacy
`55`-test expectation is not modified by this bounded compiler repair.

Original donor names remain in the optional interop tests only; the standalone
JDK product keeps canonical JDK names. No third-party challenge solution or
external donor code was copied. Problem-category choices and rejects are
recorded in the canonical Synexia `ALGORITHM_CATALOGUE.tsv`.
