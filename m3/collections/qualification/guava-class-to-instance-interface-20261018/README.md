# Existing ClassToInstanceMap interface registration

This increment registers the existing no-argument factory
`com.m3.collections.interop.M3GuavaClassToInstanceMaps` for the original Guava
33.4.8 ClassToInstanceMap interface. The factory still creates Guava's checked
MutableClassToInstanceMap over a privately owned M3LinkedHashMap. Its source,
the committed seeded oracle and its existing JUnit wrapper are unchanged.

Only the ClassToInstanceMap row's existing-owner/disposition cells and their
two generated M3CollectionSdkCatalog Java lines change. The other 106 source
rows remain byte-identical, including the prior BiMap registration and the
separate native-fork ArrayListMultimap status. The historical candidate role
M3ClassIndexMap remains metadata; no alias or new collection owner is installed.

Recorded executed proof: existing JUnit 1/1 with 3,000 seeded differential
operations; new focused tests 7/7; unchanged official generated Map/testlib
cases 898/898, with no suppressed cases; five actual SDK recipe test methods;
both source-result fixed points; and all 107 compiled catalogue lookups.
The lookup invokes the registered factory and checks the actual interface,
CodeSource and previous BiMap registration.

Scope is this existing no-argument mutable factory and the ClassToInstanceMap
interface. Supplied-map overloads, immutable factories, concrete
MutableClassToInstanceMap identity/serialization parity, and generic
TypeToInstanceMap tokens are excluded. The full collection superset remains
in progress. The original mutable feature profile is retained except for
SERIALIZABLE; the actual M3 delegate is not serializable. The frozen original
sample Impl implements all five sampled key interfaces. Null values, primitive
and wrapper token separation, invalid writes, checked entry projections,
putAll validation, defaults, live removals and independent ownership are covered.

This PR is stacked on [M3 PR #1304](https://github.com/hsoliwal/M3jdk21/pull/1304),
branch `codex/bimap-interface-registration-20261018-32aa3815`, parent
`a96cf692bee86fbe19a056a80e052833c1bd8c39`.
The predecessor canonical dependency is
[Synexia PR #12312](https://github.com/hsoliwal/com.synexia/pull/12312).
The ClassToInstanceMap recipe successor is a separate canonical draft.
Native core is the previously qualified Guava/M3 artifact with SHA-256
`84050590fa79bd8e6c40bb30cdfa33dad96ac96c0f77645f5723088863e066eb`.

Final report SHA-256:
`3bebc12fc628eca04f4eef43056627ca18ebf751b2c8037a6e6da97dfd210e17`.
SOURCE_PROVENANCE.tsv binds actual emitted postimages to exact predecessor
bytes and recipe manifests. FOSS ledgers, prepared dependency snapshots,
source-manifest and the final report retain the source/test/JAR identities.
The snapshots precede execution; successful receipts in the final report are
the execution authority. Independent source review found no concrete defect.
Configured autoreview returned no verdict under its required no-tools mode;
draft review and hosted CI are separate publication checks.

## Explicit proof replay

The proof/fixture Java files here are outside normal Maven source roots.
Reuse Java 21, bounded output directories and the exact dependency JAR hashes
in the prepared snapshots. Use the qualified native artifact in place of the
original Guava runtime JAR and the original guava-testlib 33.4.8 JAR.
Do not reuse reclaimed product target/classes.

1. Strictly compile the committed interop factory, committed
   M3GuavaClassToInstanceMapsSelfTest and M3GuavaClassToInstanceMapsTest,
   `proofs/com/m3/collections/interop/M3ClassToInstanceFocusedTest.java` and
   `proofs/CollectionTestLauncher.java` into a fresh owner output. Use
   `javac --release 21 -proc:none -Xlint:all -Werror` with pinned dependencies.
   Put that owner output first on the runtime classpath. Run CollectionTestLauncher
   separately with selectors
   `com.m3.collections.interop.M3GuavaClassToInstanceMapsTest 1` and
   `com.m3.collections.interop.M3ClassToInstanceFocusedTest 7`.
   Set `-Dm3.classInstance.ownerClasses=<owner-output>` and
   `-Dm3.classInstance.guavaJar=<qualified-native-jar>` before the launcher name.
2. Compile the two byte-identical original fixture files under `fixtures/`
   into a separate helper output. Keep `-Xlint:all` enabled; this fixture-only
   phase omits -Werror because the unchanged Serializable Impl has one measured
   serialVersionUID warning. Production and new proof code remain strict.
3. Strictly compile `proofs/com/google/common/collect/M3ClassToInstanceUpstreamSuiteLauncher.java`
   against the helper output, owner output, native artifact and original testlib.
   Add the launcher output to the classpath and run
   `com.google.common.collect.M3ClassToInstanceUpstreamSuiteLauncher <owner-output> <testlib-jar> <native-jar> <helper-output> 898`.
   FROZEN_GENERATED_SUITE.json fixes the observed successful count for replay;
   CONTRACT_FEATURES.json was frozen before first JVM execution.
4. Compile the committed M3CollectionSdkCatalog together with
   `proofs/M3ClassInstanceCatalogueLookupCheck.java` into a distinct catalogue
   output. Put it first on the classpath and run
   `M3ClassInstanceCatalogueLookupCheck <FAMILY_CATALOG.tsv> <catalogue-output> <owner-output> <native-jar>`.

Expected markers include `RUNTIME_TESTS found=1 passed=1 failed=0 aborted=0 skipped=0`,
the same JUnit marker with 7, `UPSTREAM_CLASS_INSTANCE_DISCOVERED=898`,
`UPSTREAM_CLASS_INSTANCE_TESTS found=898 run=898 failed=0 errors=0`, and
`CLASS_INSTANCE_CATALOGUE_LOOKUP_OK rows=107 factory_interface_verified=true previous_bimap_preserved=true`.
No committed test is replaced and no generated case is mirrored or suppressed.
