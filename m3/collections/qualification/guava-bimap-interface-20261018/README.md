# Existing M3 BiMap interface qualification and catalogue registration

This increment registers the existing `com.m3.collections.interop.M3BiMap`
owner and `M3BiMaps` companion for the original Guava 33.4.8 `BiMap` interface.
It changes only the BiMap row's owner/disposition cells and the corresponding
two lines of generated M3CollectionSdkCatalog metadata. All other 106 catalogue
rows, including ArrayListMultimap, retain their exact bytes and order.
The historical candidate-role string `M3BiIndexMap` is retained as metadata;
no class or alias with that name is introduced.

The production BiMap owners and the committed M3BiMapTest source are unchanged.
The separate M3GuavaBiMap factory/variant, native Guava ArrayListMultimap work,
and previous proof records are preserved. This is existing-owner reuse plus
metadata registration, not a new bidirectional-map architecture.

The final report records 22/22 existing tests with 48,000 seeded differential
operations, 2,539/2,539 unchanged Guava-generated interface cases with no
suppressed tests, five executed recipe methods, both emitted-file fixed points,
and compiled lookup checks for all 107 catalogue rows. Runtime CodeSources
bind the actual owner, original testlib and qualified native core artifact.
The frozen feature descriptor includes null keys/values/queries, all sizes,
general-purpose mutations, iterator removal, and justified forward order.
Its SERIALIZABLE feature is excluded because this is an interface qualifier;
no concrete HashBiMap identity or serialization compatibility is claimed.
Existing staged replaceAll failure behavior remains its documented stronger
guarantee. The full collection superset remains in progress.

This PR is stacked on [M3 PR #1298](https://github.com/hsoliwal/M3jdk21/pull/1298),
branch `codex/guava-capacity-trim-20261018-84050590`, parent
`2dbbbd4448a5be901083fd6315c2da79ab6e7449`.
The qualified native-core artifact SHA-256 is
`84050590fa79bd8e6c40bb30cdfa33dad96ac96c0f77645f5723088863e066eb`.
Canonical recipe dependency:
[Synexia PR #12305](https://github.com/hsoliwal/com.synexia/pull/12305).
The BiMap metadata recipe successor is a separate companion canonical draft.

`BIMAP_QUALIFICATION_REPORT.json` SHA-256:
`32aa3815f7a366a221f8733259556ca45c055bfe4912416036df1df40b19be75`.
`SOURCE_PROVENANCE.tsv` binds the two emitted files to exact source preimages,
recipe manifests and the already-committed owner/test identities. FOSS ledgers
separately record owner, upstream test-library, and metadata-generator reuse.
Prepared dependency snapshots archive the exact tested JAR hashes and commands;
the final report records their successful execution. Hosted CI and draft review
are separate publication checks.

Replay with Java 21, explicit `javac --release 21 -proc:none -Xlint:all -Werror`
and bounded output directories; none of these archived launchers is a default
Maven test source. Use the exact JAR hashes in the dependency snapshots.
Keep the qualified native artifact on the classpath in place of the original
Guava runtime JAR, and use the unchanged guava-testlib 33.4.8 JAR. The original
22-test source stays at
`m3/collections/interop/src/test/java/com/m3/collections/interop/M3BiMapTest.java`.

1. Compile the two committed interop owner sources, that committed test,
   `proofs/CollectionTestLauncher.java`, and
   `proofs/M3BiMapQualificationLauncher.java` into an owner output directory.
   Put that directory first on the runtime classpath, followed by the qualified
   artifact and pinned JUnit/Eclipse/Spring dependency JARs. Run
   `M3BiMapQualificationLauncher <owner-output> <qualified-native-jar>`.
2. Compile `proofs/M3BiMapUpstreamSuiteLauncher.java` against that owner output,
   the native artifact and unchanged Guava testlib/JUnit JARs. Run
   `M3BiMapUpstreamSuiteLauncher <owner-output> <testlib-jar> <native-jar> 2539`.
   Add the launcher output directory to the classpath. It delegates case
   generation/execution to the unmodified official builder and testers.
3. Compile the committed M3CollectionSdkCatalog production source together with
   `proofs/M3BiMapCatalogueLookupCheck.java` into a distinct catalogue output.
   Put it first on the classpath and run
   `M3BiMapCatalogueLookupCheck <FAMILY_CATALOG.tsv> <catalogue-output> <owner-output> <native-jar>`.

No replacement 22-test copy or mirrored generated test cases are installed.
The compact launchers reuse the existing tests and official generation policy.
