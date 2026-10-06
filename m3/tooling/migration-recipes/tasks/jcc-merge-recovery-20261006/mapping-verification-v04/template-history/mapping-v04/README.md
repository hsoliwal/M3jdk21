# V04 mapping verification template — admitted 090 input

This separately versioned packet targets receiving commit `09075810b55229662329a158923287e8a7118f86`, root `bba8647ea80d1cb4101394b4c0fc5a97f0b8eb52`, and input epoch SHA-256 `4fd3b3bd4fb09759d8bb02ca9ba45d0b2934625f24bcc4ae195f3e49394dff70`.

The 13 Java methods retain the V03 exact four-output replay, PlainText identity/provenance/checksum preservation, fresh and mixed fixed points, named recipe/serialization, all-target drift/missing/duplicate/scan-swap refusals, historical lineage and full root-coverage controls. Current expectations are exactly 55 ordered IDs, 53 complete non-JCC records, 34 complete top-level mappings and 20 gates. Every existing global node is compared in full, with explicit presence and preservation of `donor_artifact_policy`, `porting_invariant` and `family_name_mapping`. MR, RXM and RXA remain complete unchanged non-JCC nodes.

The 20 source Java owners remain 14 laboratory plus six JNI owners, including M3IopPatternMechanicalPasses. The separately qualified module POM remains a ten-field body-free component, checked independently and by full equality with the binding output. All acceptance fields stay false. The new `previous_qualified_input` object retains the exact C0/V03 commit, root, epoch, publication-manifest hash and finite qualification scope. Earlier d1 and 87590 epochs remain explicit. No earlier result is relabelled as qualification of 090.

## Render and execute

The template retains 11 explicit unresolved actual-source/output pins. Invoke `render_template.py` with `--audit-root`, the verified actual `--source-input`, actual V04 `--mapping-resources`, and a fresh `--output`. It revalidates the source packet and exact four binder afterimages before staging Java, focused POM, named YAML and a body-free publication summary. Synthetic fixture inputs refuse before staging.

Repository destinations use V04 exclusively:

- Test: `m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport/JccMergeMappingRecoveryV04Test.java`
- POM: `m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006/mapping-verification-v04/pom.xml`
- YAML: `m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-jcc-merge-mapping-recovery-v04.yml`
- Summary: `m3/tooling/migration-recipes/src/test/resources/com/m3/rewrite/backport/jcc-merge-recovery-20261006-mapping-v04/publication-input-summary.json`

The recipe is `com.m3.rewrite.backport.JccMergeMappingRecoveryV04`; its crate is `jcc-merge-recovery-20261006-mapping-v04`. Repository task documents are `source-publication-input-v04.json` and `mapping-input-epoch-v04.json`. The binder's local filenames remain `source-publication-input.json` and `mapping-input-epoch.json`.

In a fresh execution workspace, run the focused POM with `-Dm3.mapping.materialized=/absolute/new/result-directory verify`. The result directory contains only four actual OpenRewrite Result afterimages, using CREATE_NEW. Resources remain before-00 through before-03, after-00 through after-03, manifest.tsv and plan.json; manifest and plan paths are sorted independently of resource numbering. The POM also requires the existing history-0014.txt and history-0016.txt oracle resources supplied by the separately retained history component.

Dependencies remain OpenRewrite core 8.17.1, JUnit 5.10.2, compiler plugin 3.13.0, Surefire 3.2.5, SLF4J 1.7.36 and annotations 24.1.0. The canonical text owner is unchanged. `compile-check-v01/receipt.json` records a fresh successful Java21 compile with fatal warnings; `self-review-v01/receipt.json` records the fixture refusal and unresolved pins. Zero JUnit methods have executed as part of this prepared template packet.

All V03 templates, actual outputs and qualification receipts remain unchanged and separately retained. Source export, whole-module, JDK image, JNI/native/platform, memory and performance acceptance remain governed by their existing gates.
