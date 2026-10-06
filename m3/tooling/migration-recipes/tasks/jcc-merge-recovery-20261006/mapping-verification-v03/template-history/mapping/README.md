# Current mapping verification template — input epoch v01

This frozen v01 template retains the exact receiving input
`87590cb96fb0e2dc0f88fae8e01957f7179cd255`: 47 records, 45 non-JCC records,
17 top-level mappings, and 20 gates. Receiving master subsequently added five
collections records and eight mappings. That advance requires a separate v02
template and binder input; it must not be accommodated by weakening these assertions.

This packet prepares a focused JUnit harness for the four-current-file crate
`jcc-merge-recovery-20261006-mapping`. The Java source is an unresolved template.
It contains no synthetic publication commit, root tree, afterimage hash, or passing
execution claim. Actual publication and test execution are separate pending steps.

## Bind only the actual owner packet

Run the existing `bind_mapping.py` without `--fixture-only` against the verified
`m3-jcc-source-publication-input/1` packet. Then run this renderer against that exact
packet and its binder output:

```sh
python3 -B render_template.py \
  --audit-root /absolute/path/to/receiving-merge-audit \
  --source-input /absolute/path/to/verified-actual-source-input.json \
  --mapping-resources /absolute/path/to/actual-binder-output \
  --output /absolute/path/to/new-mapping-test-staging-directory
```

There is no fixture mode. The renderer calls the existing binder's source validation
and composition, checks all four exact current preimages, the historical map and
bindings, all actual source bodies and readback references, the root/POM, the sorted
manifest, the canonical plan digest, and the resource receipt. It finishes all these
checks before creating the staging directory.

The renderer writes exactly four repository artifacts and its own receipt:

| Artifact | Repository destination |
| --- | --- |
| Rendered JUnit test | `m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport/JccMergeMappingRecoveryTest.java` |
| Focused POM | `m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006/mapping-verification/pom.xml` |
| Named recipe | `m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-jcc-merge-mapping-recovery.yml` |
| Test input summary | `m3/tooling/migration-recipes/src/test/resources/com/m3/rewrite/backport/jcc-merge-recovery-20261006-mapping/publication-input-summary.json` |

It never writes the four operational mapping files, recipe beforeimages/afterimages,
or the original source-publication packet. Those remain under the existing binder,
text recipe, and sealed installer workflows. All source commit/root and output hash
tokens in `JccMergeMappingRecoveryTest.java.in` must be resolved together. The test's
`@BeforeAll` guard rejects unresolved identities and any non-actual publication state.

## Runtime prerequisites

The isolated execution workspace needs the unchanged existing text recipe and the
actual binder's ten resource files beneath:

`m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-merge-recovery-20261006-mapping/`

Those files are `before-00.txt` through `before-03.txt`, `after-00.txt` through
`after-03.txt`, `manifest.tsv`, and `plan.json`. Resource numbering follows the
binder's `PATHS`, while manifest and plan output order must be sorted by target path.
The POM also includes exactly two existing historical recipe resources:

| Historical resource in `jcc-merge-recovery-20261006-history-01/` | Purpose | SHA-256 |
| --- | --- | --- |
| `history-0014.txt` | Complete prior map | `eb42442866c73c38027b481a84c0473bea422e4628d0794fa31fc61d9f1a818e` |
| `history-0016.txt` | Complete prior bindings | `f8b27e3582eb494af2c8dd47225976f974be6edc2f6c26313975064d48739497` |

Use a fresh execution workspace for each harness revision. From its repository root:

```sh
mvn -f m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006/mapping-verification/pom.xml \
  -Dm3.mapping.materialized=/absolute/path/to/new-actual-result-directory test
```

The materialization property is optional. If supplied, only actual OpenRewrite
`Result` afterimages are written, using `CREATE_NEW`. The first replay test must
produce exactly four outputs and pass replay verification before writing those
result files. Preserve each failed or successful execution and its exact workspace
inputs independently; do not reuse compiled targets across harness revisions.

## Scope of the 13 JUnit tests

The harness checks exact four-output replay, meaningful PlainText identity and
metadata preservation, retained marker identity plus the exact added recipe
provenance, stale-checksum removal, fresh source/owner fixed point, all four mixed
before/after cases, the named recipe, and serialization. Every target is tested for
missing input, source drift, duplicate admitted images, and an admitted afterimage
substituted after the scanner observed its beforeimage. Refusals handle both thrown
exceptions and execution-context callbacks and require no partial results or input
text/identity/marker mutation.

Metadata checks preserve all 47 ordered IDs, every complete non-JCC record, all 17
top-level mappings, every global and migration field, M3 precompute policy, and all
20 gates. The 14 laboratory and six JNI owner nodes must equal the expected
projection of the actual source readbacks, including the appended
`M3IopPatternMechanicalPasses` owner. Current JCC targets, ownership, contracts,
dependencies, provenance, and unrelated record fields remain unchanged.

Lineage checks compare complete historical source and target JSON objects from
both current and prior epochs, with exact ordered deduplication. Both binding epochs
preserve their complete JCC records, and the older binding retains its original
execution and publication objects. Every acceptance value must be an explicit
boolean false. The original handoff prose remains an exact suffix.

Root coverage is checked against every entry of the actual complete root readback,
including mode, object type, and object ID. The test independently recomputes the Git
root tree identity, checks sorted coverage paths, all 17 TSV columns, all blocked
states, root POM module declaration contexts and accounting, and the coverage byte
hash. The separately hashed summary removes source bodies; its `source_input_sha256`
is the original complete packet's digest, not the summary digest. Full body and
readback verification belongs to the original packet and renderer/binder validation.

The POM pins OpenRewrite core **8.17.1**, JUnit **5.10.2**, compiler plugin **3.13.0**,
and Surefire **3.2.5**. It compiles only the retained text owner and this test, selects
this exact test in Surefire, and keeps warnings fatal. SLF4J **1.7.36** and annotations
**24.1.0** match the existing focused receiver harness. No canonical source owner,
existing dependency version, whole-module gate, source-export authority, or rebuilt
M3JDK21 acceptance claim is changed by this packet.
