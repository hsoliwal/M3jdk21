# M3JDK21 historical source restoration candidate — 2026-10-16

**Review status: draft / incomplete integration / DO NOT MERGE before qualification.**

## Defect and pinned receiver

- Canonical target: `hsoliwal/M3jdk21` `master` at `855afe57a679aeb454052630de7beddcb25f5f8f`.
- Ancestry-only multiparent merge: `855afe57a679aeb454052630de7beddcb25f5f8f`, 11 parents and zero changes relative to first-parent tree.
- The following ten original PRs are marked merged but their 37 distinct added file paths were absent from the target tree: [#889](https://github.com/hsoliwal/M3jdk21/pull/889), [#891](https://github.com/hsoliwal/M3jdk21/pull/891), [#894](https://github.com/hsoliwal/M3jdk21/pull/894), [#896](https://github.com/hsoliwal/M3jdk21/pull/896), [#897](https://github.com/hsoliwal/M3jdk21/pull/897), [#907](https://github.com/hsoliwal/M3jdk21/pull/907), [#916](https://github.com/hsoliwal/M3jdk21/pull/916), [#934](https://github.com/hsoliwal/M3jdk21/pull/934), [#947](https://github.com/hsoliwal/M3jdk21/pull/947), [#952](https://github.com/hsoliwal/M3jdk21/pull/952).
- This recovery copies the **exact historical Git blob SHA** for each missing file into its original source path. It does not substitute or rewrite Java/JNI implementations, drop current code, or claim semantic equivalence. For the same source path added in #891 and updated through stacked #896, use the #896 later stacked blob.

## Canonical recipe

1. **Inventory** every merge parent and merge base, exact parent/active path modes and Git blob IDs, overlapping methods and API/ABI owners.
2. **Inline** original historical implementations by reusing pinned Git object SHA, without regenerating from incomplete prose.
3. **Atomize** into absent-file restoration versus conflicting existing-file modifications; never overwrite an existing file with a donor snapshot.
4. **Patternize** through the Synexia-owned reusable Maven/OpenRewrite source-sealed recipe. Reconcile modified owners serially by FILE → PACKAGE → MODULE → REACTOR, verifying no contract changes before promotion.
5. **Comment** every acceptance/refusal with original PR, blob SHA, target blob SHA, build tests, JVM/JNI parity and donor/license record.

## Exact newly restored Git blobs (37)

| Source path | Original blob SHA | PR |
| --- | --- | --- |
| `m3/runtime-integration/regex-state-mask-20261014/receipt.json` | `969631b8d10ebb1b999100ae656d7d133fd12e6a` | #889 |
| `test/jdk/java/util/regex/M3PatternStateMaskTest.java` | `ebaf8c4b91dd8e65ddc525864e2c92e69bad9f42` | #889 |
| `m3/compatibility/regex-relations-20261014/M3RelationPortProof.java` | `e9a6beee8916cf97d5d4aac2383e7ac2fa8843e0` | #896 |
| `m3/docs/name-mappings/regex-relations-20261014.json` | `63c0143abf061391d269303daf74909521464fee` | #896 |
| `src/java.base/share/classes/jdk/internal/mindex/M3DotStarRelations.java` | `780c7887aaa6b0892a76f4eee1dcb17086a60289` | #896 |
| `src/java.base/share/classes/jdk/internal/mindex/M3StateRelation.java` | `ea904c0962fb976f43b45575f06f46ad198e29a2` | #896 |
| `src/java.base/share/native/libjava/M3StateRelation.c` | `610ca8bab47065a49b9b58814d6392a287f4fd8b` | #896 |
| `src/java.base/share/native/libjava/m3_state_relation.h` | `18a02e228129827da486edc5b316790af6135310` | #896 |
| `test/jdk/java/lang/String/M3StringHierarchicalRegexTest.java` | `5d47787597630efc70a4e3b173f2e788dfb6aa3a` | #896 |
| `m3/docs/name-mapping-regex-effects-20261014.json` | `17cadc255d96c3d21d3134da7eb72357c2efe3b4` | #894 |
| `m3/docs/regex-effects-receiving.md` | `38ddbb2c533e7baf72f54f94f23fbfc84661bb4d` | #894 |
| `m3/docs/regex-power-receiving-20261015.md` | `4c7b237f01f9cd231d5af7f4dc4361810bb39ece` | #894 |
| `m3/docs/regex-pressure-receiving-20261014.md` | `2a06459bc418cde5b9e9d63934c53ea52d1debf6` | #894 |
| `m3/docs/regex-utf16-receiving-20261015.md` | `c75ab7e9bb1845c39df02d9e6cc4d11416e5c0f0` | #894 |
| `src/java.base/share/classes/jdk/internal/mindex/M3RegexDpPlan.java` | `650b5506b13c4180cae48a99eb0f866b0eceea7a` | #894 |
| `src/java.base/share/native/libjava/M3RegexDpPlan.c` | `a64bceb4379d14fa668d1b247b8ad12181d0cffd` | #894 |
| `test/jdk/java/lang/String/M3StringRegexEffectsTest.java` | `5386449b7a1bcbec0c6e6b3c2293cac02cc0f69c` | #894 |
| `test/jdk/java/lang/String/M3StringRegexPowerTest.java` | `88f9ba07382735f3b9bbe8b9a3cde302b654f411` | #894 |
| `test/jdk/java/lang/String/M3StringRegexReuseTest.java` | `a913b9dc61447efafad9d68e534b838e9ada66f3` | #894 |
| `test/jdk/java/lang/String/M3StringRegexUtf16Test.java` | `2bc23aab599cc15008d6faefc41c4fc2499f3a6d` | #894 |
| `m3/compatibility/relation-images-20261014/RelationImageProof.java` | `8b87b9a3c80c591cdd40423356bc30ff3ef60cb2` | #896 |
| `m3/docs/name-mappings/relation-images-20261014.json` | `09054ddba5a2fa2e765785048b0e0a1cdda5516b` | #896 |
| `src/java.base/share/classes/jdk/internal/mindex/M3RelationImage.java` | `9f2a56451d74d95f03d34df69832225d45e8e551` | #896 |
| `m3/tooling/module-packs/text-tools/CSV_M3_STRING_RECEIVER_20261015.md` | `9ee2c2bd70de6b4e1e75b3ee6db93781db0b1a14` | #916 |
| `m3/tooling/module-packs/text-tools/src/jdk.m3.text/jdk/m3/text/CsvTools.java` | `dd2c3635c87cad4fcad50c7295011ddbacb7b722` | #916 |
| `m3/backports/NEBULA_PROOF_GATED_ADMISSION_20261015.tsv` | `96891444042f6dd4cb51c9d8e3236b038474cc42` | #934 |
| `m3/backports/NEBULA_UPSTREAM_PROOF_ADMISSION_20261016.tsv` | `ffff78cb1e9def6fadea247c3667961bf0539892` | #934 |
| `m3/backports/nebula_proof_admission.py` | `f0ee29290dcefdf19f3a7a59d378946a08da0432` | #934 |
| `m3/backports/nebula_upstream_proof_admission.py` | `f0b5e322578ffbe25616f340cd093a3ade805153` | #934 |
| `m3/backports/test_nebula_proof_admission.py` | `41e23d01d0436f9a1495dff0913fda6582e257ff` | #934 |
| `m3/backports/test_nebula_upstream_proof_admission.py` | `6afcc68289d988a7b9fad48f65dc30942ae8c4e7` | #934 |
| `.github/workflows/m3-cpu-byte-pattern-set-20261016.yml` | `0ed9d20d064954db08ed2ba6e79b043fe99cf430` | #947 |
| `m3/ports/capability-runtime/M3_BYTE_PATTERN_SET.md` | `aabed6eddd62ceeeb07e3f47b2e8d8ad778e2cf5` | #947 |
| `m3/ports/capability-runtime/native/byte_pattern_set_jni.c` | `c1f9786702881c3ffaaa3d3bdb14ccd9118c1a3d` | #947 |
| `m3/ports/capability-runtime/src/main/java/com/m3/runtime/capability/BytePatternSet.java` | `382154b23923ae7c7910e8c0fdd273e82add3ff9` | #947 |
| `m3/ports/capability-runtime/src/test/java/com/m3/runtime/capability/BytePatternSetChecks.java` | `3196dd3b5ea276df9a3d44bfcefd33513bd01192` | #947 |
| `test/jdk/java/lang/String/M3StringCharArrayCompositionTest.java` | `57cea18b1e6599286b74c075c5ec31489f84ce48` | #952 |

## Existing-file modifications NOT blindly applied (20 unique paths)

These are source-conflict/integration candidates, not proof that changes are missing. Compare per-method contract and correct owner with the current target; do not overwrite current `M3String`, regex engines, JNI integration, native build lists, Kotlin/Maven module packing or backport tools.

| Existing modified path | Incoming PRs |
| --- | --- |
| `m3/docs/synexia-string-precompute-port-map.tsv` | #889 |
| `src/java.base/share/classes/java/util/regex/M3PatternQuery.java` | #889, #897 |
| `src/java.base/share/classes/java/lang/M3StringRegexDp.java` | #891, #894, #896, #907 |
| `AGENTS.md` | #894 |
| `m3/compatibility/m3-pattern-ascii-receive-20261014.json` | #897 |
| `m3/docs/name-mapping.d/m3-pattern-ascii-frontier.json` | #897 |
| `test/jdk/java/util/regex/M3PatternAsciiFrontierTest.java` | #897 |
| `test/jdk/java/util/regex/libM3PatternAsciiFrontierTest.c` | #897 |
| `test/jdk/java/lang/String/M3StringRegexDpTest.java` | #907 |
| `m3/tooling/module-packs/text-tools/fixtures/src/test/java/M3TextSmoke.java` | #916 |
| `.github/workflows/m3-jdk21-backport-dag.yml` | #934 |
| `m3/backports/BACKPORT_PROGRAMME_STATUS.md` | #934 |
| `m3/backports/SYNEXIA_NEBULA_RECIPE_DAG_HANDOFF.tsv` | #934 |
| `m3/backports/pom.xml` | #934 |
| `m3/ports/capability-runtime/src/test/java/com/m3/runtime/proof/CapabilityImageSmoke.java` | #947 |
| `m3/ports/capability-runtime/verify_native.py` | #947 |
| `m3/synexia-import/current-full-borrow/check_string_phase.py` | #952 |
| `m3/synexia-import/current-full-borrow/string-target-successor-chain.tsv` | #952 |
| `m3/synexia-import/current-full-borrow/test_string_phase_successors.py` | #952 |
| `src/java.base/share/classes/java/lang/M3String.java` | #952 |

## Mandatory gates before merging

- Verify exact 37 hashes and file modes, and zero deletions of current `master` files, with `git diff --check`, `git ls-tree`, `git diff --name-status`.
- Run source/recipe sealing and Maven/OpenRewrite fixed-point tests on canonical Synexia owner; test JDK21 `java.base` and libjava native compilation, **jtreg** String/regex suites, both Java/JNI path proofs and module-pack compilation.
- Run C11 sanitizer, UTF-16/regex and multithread pressure regressions, current Nebula fail-closed admission tests, and all existing M3 capability tests.
- For absent source dependencies or modified existing owners, mark `HOLD` and split implementation into reviewable follow-ups rather than claiming complete runtime admission.

All qualification is **NOT RUN** in this recovery operation. This draft is an exact source-custody step, not a green build claim.
