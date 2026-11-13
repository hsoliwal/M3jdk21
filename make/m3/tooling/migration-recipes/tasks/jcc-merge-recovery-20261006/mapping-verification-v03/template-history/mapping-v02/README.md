# Current mapping verification template — input epoch v02

This packet targets the separately frozen receiving input
`d1b9162cd568108f4c8d82f6b6a03cccfdb91bd2`, root
`0812573f02908d8078d13f5f49346865cdf1c0e0`. It preserves **52 ordered records,
50 complete non-JCC records, all 25 current top-level mapping objects, and 20 gates**.
The complete current mapping list includes eight additions and two updates to
earlier mapping nodes. Comparisons preserve the whole current list; they never
collapse entries by donor name.

The original 47-record/45-other-record/17-mapping expectations remain frozen in
`candidate/mapping/java-template/TEMPLATE_PACKET.json`, SHA-256
`40e7f6ef1c30496b64c81574cfa8c358db9a82b9124003f07c1daecc550e795a`.
The receiving input advance did not change that historical oracle. The current
input epoch is `candidate/mapping-v02/input-epoch/INPUT_EPOCH.json`, SHA-256
`12432df37e8bb0c890ab63ae009c576720c40bcda0b927dc53b70fbbd612f84a`.

The Java file remains an unresolved template. Its eleven publication/output pins
contain explicit unresolved tokens. It carries no synthetic source commit/root,
actual-publication claim, JUnit pass, or operational mapping output.

## Render from the actual source publication only

First run `candidate/mapping-v02/bind_mapping.py` without `--fixture-only`, supplying
the verified `m3-jcc-source-publication-input/1` packet. Then render this test from
that exact source input and the resulting actual-mode binder output:

```sh
python3 -B render_template.py \
  --audit-root /absolute/path/to/receiving-merge-audit \
  --source-input /absolute/path/to/verified-actual-source-input.json \
  --mapping-resources /absolute/path/to/actual-v02-binder-output \
  --output /absolute/path/to/new-v02-test-staging-directory
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
| JUnit test | `m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport/JccMergeMappingRecoveryV02Test.java` |
| Focused POM | `make/m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006/mapping-verification-v02/pom.xml` |
| Named recipe | `m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-jcc-merge-mapping-recovery-v02.yml` |
| Test input summary | `m3/tooling/migration-recipes/src/test/resources/com/m3/rewrite/backport/jcc-merge-recovery-20261006-mapping-v02/publication-input-summary.json` |

The renderer does not write the four operational mapping files, beforeimages,
afterimages, or the original source packet. Its summary strips source bodies and
retains a separate summary digest. `source_input_sha256` remains the digest of the
complete original source packet; it is never replaced with the summary's digest.

## Focused execution

Create a fresh execution workspace for this exact harness revision. It needs the
unchanged `M3Jdk21HashPinnedTextSnapshotRecipe`, the actual v02 binder's ten crate
resources, and the four rendered artifacts above. The crate resource directory is:

`m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-merge-recovery-20261006-mapping-v02/`

The ten resources are `before-00.txt` through `before-03.txt`, `after-00.txt` through
`after-03.txt`, `manifest.tsv`, and `plan.json`. Their numbering follows binder
`PATHS`; manifest and plan output rows are sorted by target path. The focused POM
also includes these two existing historical resources from
`jcc-merge-recovery-20261006-history-01/`:

| Resource | Contents | SHA-256 |
| --- | --- | --- |
| `history-0014.txt` | Complete prior map | `a91ad1cb7d7f358fb7c4e280b7bf3756db9dd2d6f387f4c7e622e232ff03c0bf` |
| `history-0016.txt` | Complete prior bindings | `a0b20e45bb5d5e8f4a684ee0d4daa7a9cf8730529ae784a09778b17aceabea11` |

From the execution workspace's repository root:

```sh
mvn -f make/m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006/mapping-verification-v02/pom.xml \
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
current input epoch pin and the older 47/45/17/20 epoch remain explicit. All acceptance
values must be boolean false, and the earlier handoff prose remains an exact suffix.

Root coverage is checked against every actual root entry, with an independent Git
tree identity calculation, sorted paths, exact type/mode/object IDs, all 17 TSV
columns, blocked states, root POM module declarations and contexts, accounting, and
coverage byte hash. Source bodies are validated by the original binder/renderer
input; this summary-based JVM proof does not claim a second GitHub acquisition.

The POM uses OpenRewrite core **8.17.1**, JUnit **5.10.2**, compiler plugin **3.13.0**,
and Surefire **3.2.5**. SLF4J **1.7.36** and annotations **24.1.0** match the existing
focused receiver harness. It compiles only the unchanged text owner and the V02
test, selects the V02 test explicitly, and keeps all compiler warnings fatal.
Source export, whole-module validation, JNI/runtime/platform gates, and rebuilt-JDK
acceptance retain their existing authority and requirements.
