<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Canonical integrated M3JDK21 utility recipe

Canonical destination: `hsoliwal/com.synexia`,
`synexia-openrewrite-recipes/recipes/m3jdk21-integrated-utilities-20261018`.
Target-owned proof mirror: `m3/tooling/integrated-utilities/recipe`.

The existing `M3GpuToolJoinRecipe` and `M3GpuToolJoinManifest` are copied byte-for-byte
from the pinned M3JDK21 proof mirror recorded in `ENGINE_PROVENANCE.json`. Their
`resourceRoot` extension point selects this dataset. The small caller executes the
actual OpenRewrite SDK scheduler, verifies every returned postimage, executes a real
second SDK run, and then emits changed Result bytes serially in manifest order into
a fresh source-disjoint directory. Product sources are received separately under
their exact preimage and postimage seals.

## Run the sealed recipe

Use Java 21 and Maven. Supply an exclusively owned input checkout or source deck
containing every existing target in the manifest, including unchanged `KEEP`
targets. The output directory must be fresh, physically disjoint from the input,
and have no symbolic-link ancestors. The runner admits the complete dataset before
execution and rechecks source integrity before emission.

```sh
mvn -B -ntp -f pom.xml test
mvn -B -ntp -f pom.xml exec:java \
  -Dexec.mainClass=com.synexia.rewrite.IntegratedUtilitiesRunner \
  -Dexec.args="--emit /path/to/exclusive-source-input /path/to/fresh-output"
mvn -B -ntp -f pom.xml exec:java \
  -Dexec.mainClass=com.synexia.rewrite.IntegratedUtilitiesRunner \
  -Dexec.args="--check /path/to/received-target-checkout"
```

The production resource root is
`src/main/resources/com/synexia/rewrite/m3jdk21-integrated-utilities-20261018/`.
Its headerless four-column TSV manifest is `target_path`,
`before_sha256_or_ABSENT`, `after_sha256`, `template_relative_path_or_KEEP`.
`ABSENT` requires an unoccupied target. `KEEP` requires the exact unchanged source
hash and creates no Result. Changed paths take precedence when deduplicating the
catalogue's baseline source atoms. Templates are exact UTF-8 source postimages;
the SDK operates on sealed `PlainText` for the mixed Java, C, JSON, TSV and document
dataset. Java and native behavior are qualified by separate source-bound receivers.

The assembled dataset pins M3JDK21 baseline
`2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. The assembly receipt and actual
`RECIPE_RESULTS.json` are preserved under `qualification/recipe/`. The latter is
written only after successful actual SDK execution and its second-run fixed point.
It identifies every changed Result path and hash. The proof-mirror directory is
excluded from the manifest to avoid recursive recipe generation.

The standalone Maven tests exercise actual SDK generation, fixed point, all mixed
pre/post fixture states, cancellation, drift, duplicate/missing/occupied targets,
unrelated source preservation, output refusal and named recipe discovery. The
production receipt records the real dataset execution separately from these fixture
tests. Serial emission is not a multi-file filesystem transaction: a write failure
can leave an incomplete output directory without an admitted complete Result receipt.

## Read-only Java method source seals

`tools/M3SourceAtoms.java` uses the existing public JDK compiler parser to record
method source extents, exact UTF-8 hashes, owning declarations and UTF-16 source
offsets. Invoke it from this module with a source root and explicit relative Java
paths:

```sh
java --source 21 tools/M3SourceAtoms.java /path/to/source-root \
  src/java.base/share/classes/java/util/Collections.java \
  src/java.base/share/classes/java/util/M3CollectionViews.java \
  > /tmp/m3-source-atoms.json
```

This helper calls `JavacTask.parse`; it does not rewrite source, execute a second
convergence engine, attribute types, compile the full JDK or prove method behavior.
The native source has separate exact source, header and JNI symbol custody. Keep
method-source seals distinct from executable contract and product-image acceptance.

## Qualification boundaries

`qualification/` preserves exact source-bound utility, collection and mask receipts,
the rejected indirect-sort candidate and its measurements, module/API source
coverage checks, and the existing Collections jtreg run with a candidate
`java.base` patch. The original sparse-test-metadata refusal and the pinned metadata
materialization are preserved separately from the successful run.

The source catalogue accounts for all 70 observed source modules and representative
public owners at the pinned baseline. It does not enumerate every public type or
method or claim universal utility completeness. The receiving phase order remains
unchanged. Token-sequence matching remains a pending catalogue extension until its
own final source and contract intake is admitted.

The selected detached snapshot sorter reuses `Arrays.sort` over its own captured
visible `Object[]`. Selected physical indices are transient construction inputs;
offset and reverse views do not retain a redundant index permutation. Final
measurements do not establish a general speedup over ordinary JDK operations.
Java mask selection remains the default; native selection is diagnostic opt-in.
Stock JDK patch-module and separately compiled JNI evidence does not qualify a
complete M3JDK21 product image, product `libjava`, other platforms or stable
performance thresholds. Those acceptance gates remain pending.

The recipe is Apache-2.0 authoring tooling. Its license does not relicense generated
OpenJDK files, and no Maven/Synexia service becomes a JDK runtime dependency. Retain
per-file target licensing and exact donor notices. Repair failures in canonical
candidate templates and replay the recipe before receiving changed target bytes.
