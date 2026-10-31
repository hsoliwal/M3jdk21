# Current mapping verification template — input epoch v03

This packet targets the separately frozen receiving input
`c0a14387009aefc7d62bd3268055d526e04f9074`, root
`491bbd80a51483d0710fc12c1bad3a037b385ba0`. It preserves **52 ordered records,
50 complete non-JCC records, all 25 current top-level mapping objects, and 20 gates**.
Relative to the preceding V02 input, only the TQ record at index 46 changed.
All 25 mapping nodes and the other 51 records are unchanged. Comparisons preserve
every complete current non-JCC record, including that updated TQ record.

The original 47-record/45-other-record/17-mapping expectations remain frozen in
`candidate/mapping/java-template/TEMPLATE_PACKET.json`, SHA-256
`5fe25ca79a45bd4e499fd6171dfe5375390f91f2331330797b93aec3c5eec142`.
V02 is also retained unchanged at `candidate/mapping-v02/java-template/TEMPLATE_PACKET.json`,
SHA-256 `24b0adba2b80ee105ba9904e4d00b96d441cddf4b548b5400d72ca09f1d26136`.
The receiving input advance did not change either historical oracle. The current
input epoch is `candidate/mapping-v03/input-epoch/INPUT_EPOCH.json`, SHA-256
`027e47d20c7fb854e028487ceef341c929877a6421dbb75f6433b5cd007fe5ee`.

The Java file remains an unresolved template. Its eleven publication/output pins
contain explicit unresolved tokens. It carries no synthetic source commit/root,
actual-publication claim, JUnit pass, or operational mapping output.

## Render from the actual source publication only

First run `candidate/mapping-v03/bind_mapping.py` without `--fixture-only`, supplying
the verified `m3-jcc-source-publication-input/1` packet. Then render this test from
that exact source input and the resulting actual-mode binder output:

```sh
python3 -B render_template.py \
  --audit-root /absolute/path/to/receiving-merge-audit \
  --source-input /absolute/path/to/verified-actual-source-input.json \
  --mapping-resources /absolute/path/to/actual-v03-binder-output \
  --output /absolute/path/to/new-v03-test-staging-directory
```

The renderer has no fixture mode. It verifies the frozen receiving epoch, its four
current bodies and seven unchanged canonical guards, the historical map/bindings,
the actual source owner/readback packet, the exact composed afterimages, sorted
manifest, canonical plan digest, and matching binder receipt. It also requires the
binder's `mapping-input-epoch.json` to match the pinned current epoch byte for byte.
Every validation completes before the staging directory is created.

It writes four repository artifacts and `RENDERED_TEMPLATE_RECEIPT.json`:

| Artifact | Repository destination |
| --- | --- |
| JUnit test | `m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport/JccMergeMappingRecoveryV03Test.java` |
| Focused POM | `m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006/mapping-verification-v03/pom.xml` |
| Named recipe | `m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-jcc-merge-mapping-recovery-v03.yml` |
| Test input summary | `m3/tooling/migration-recipes/src/test/resources/com/m3/rewrite/backport/jcc-merge-recovery-20261006-mapping-v03/publication-input-summary.json` |

The renderer does not write the four operational mapping files, beforeimages,
afterimages, or the original source packet. Its summary strips source bodies and
retains a separate summary digest. `source_input_sha256` remains the digest of the
complete original source packet; it is never replaced with the summary's digest.

## Separate build component

The actual source packet must also contain exactly one module-POM compile-edge
component at `synexia-openrewrite-recipes/pom.xml`, with 203,000 bytes, SHA-256
`7846d9447016ab714a4d1c82d7f3814efa7a94049cb99bb59ba09f6f6d8c7a80`, and Git blob
`1a1c0cc8017b9ce9eadd6eba7e2ead3400a2266f`. The binder validates its full body and
common actual source commit/readback. The renderer uses
`binder.validate_build_components(packet)` to project exactly ten body-free fields
into `source_components`. The Java test checks every field and exact equality with
the four-file output binding’s `source_build_components`. This component stays
separate from the 20 Java owners and the 14+6 capability counts.

## Focused execution

Create a fresh execution workspace for this exact harness revision. It needs the
unchanged `M3Jdk21HashPinnedTextSnapshotRecipe`, the actual v03 binder's ten crate
resources, and the four rendered artifacts above. The crate resource directory is:

`m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-merge-recovery-20261006-mapping-v03/`

The ten resources are `before-00.txt` through `before-03.txt`, `after-00.txt` through
`after-03.txt`, `manifest.tsv`, and `plan.json`. Their numbering follows binder
`PATHS`; manifest and plan output rows are sorted by target path. The focused POM
also includes these two existing historical resources from
`jcc-merge-recovery-20261006-history-01/`:

| Resource | Contents | SHA-256 |
| --- | --- | --- |
| `history-0014.txt` | Complete prior map | `eb42442866c73c38027b481a84c0473bea422e4628d0794fa31fc61d9f1a818e` |
| `history-0016.txt` | Complete prior bindings | `f8b27e3582eb494af2c8dd47225976f974be6edc2f6c26313975064d48739497` |

From the execution workspace's repository root:

```sh
mvn -f m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006/mapping-verification-v03/pom.xml \
  -Dm3.mapping.materialized=/absolute/path/to/new-actual-result-directory test
```

The materialization property is optional. Only actual OpenRewrite `Result`
afterimages reach that directory, using `CREATE_NEW`, after the four-output and
replay checks pass. Do not reuse generated Maven targets across harness revisions.
Keep failed and successful runs, their input manifests, and their scopes separately.

## Verification boundary

The 13 authored JUnit methods verify exact four-output replay, original PlainText
UUID/path/markers/charset/BOM/file attributes, stale-checksum removal, exact added
recipe provenance, fresh fixed point, every mixed pre/postimage case, named recipe,
and serialization. Every current target is tested for missing input, drift,
duplicate admitted images, and an admitted postimage substituted after the scanner
observed its preimage. Both thrown refusals and execution-context error callbacks
are handled; refusals require no partial results or input text/identity/marker changes.

Complete JSON-node comparisons preserve all current global and migration fields,
M3 precompute ownership, all 52 ordered IDs, all 50 unrelated records, all 25 mapping
nodes, and all 20 gates. The current JCC sources must be the actual 14 laboratory
and six JNI owner projections, including `M3IopPatternMechanicalPasses`, with exact
hashes, Git objects, signatures, readback identities, and the common actual source
revision. Existing JCC target/owner/contract/dependency/provenance fields are retained.

Historical source/target objects are checked using exact ordered deduplication over
both current and prior lineage. Both binding epochs retain complete JCC records;
the prior epoch retains its original source execution/publication objects. The
current input epoch pin, the intermediate d1 52-record epoch, and the older
47/45/17/20 epoch remain explicit. Both historical binding epochs remain intact. All acceptance
values must be boolean false, and the earlier handoff prose remains an exact suffix.

Root coverage is checked against every actual root entry, with an independent Git
tree identity calculation, sorted paths, exact type/mode/object IDs, all 17 TSV
columns, blocked states, root POM module declarations and contexts, accounting, and
coverage byte hash. Source bodies are validated by the original binder/renderer
input; this summary-based JVM proof does not claim a second GitHub acquisition.

The POM uses OpenRewrite core **8.17.1**, JUnit **5.10.2**, compiler plugin **3.13.0**,
and Surefire **3.2.5**. SLF4J **1.7.36** and annotations **24.1.0** match the existing
focused receiver harness. It compiles only the unchanged text owner and the V03
test, selects the V03 test explicitly, and keeps all compiler warnings fatal.
Source export, whole-module validation, JNI/runtime/platform gates, and rebuilt-JDK
acceptance retain their existing authority and requirements.

## Prepared-state evidence

`compile-check-v02/receipt.json` records a clean Java 21 compile with fatal warnings
against the exact retained owner and cached dependencies. `self-review-v02/receipt.json`
records all 13 test methods, all 11 unresolved tokens, and the renderer’s refusal of
the V03 synthetic fixture before any output directory was created. **No JUnit test
has executed and no operational mapping afterimage has been materialized by this
template packet.** Actual publication binding and fresh-workspace execution remain
required.

The first V03 prepared revision and its original receipts remain frozen under
`previous-input-epoch-01/`. Its epoch pin `2a4878ce…` carried an inaccurate inherited
claim that all 47 original record nodes were unchanged. Revision 2 uses corrected
epoch `027e47d2…`: all 47 original IDs remain ordered, 46 original complete nodes
remain unchanged, and the TQ update is explicit. All 11 frozen operational and guard
bodies are identical between those epochs. Fresh revision-2 compile/refusal receipts
retain their own pin; no earlier PASS receipt has been relabelled.
