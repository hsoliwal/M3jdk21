<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Receive array/mask refinement on the existing collection convergence

The receiving sequence preserves the complete M3JDK21 #1426 parent, including its newer filtered/sorted views, native selector and direct `java.base` map implementations. It applies the original Synexia array/mask recipe's six outputs, followed by the nine source-admission successor outputs. The fifteen generated files were received through actual qualified OpenRewrite Results. The composed owner sources passed 1,941,255 aggregate focused assertion counters across the retained Java/JNI and patched JDK-map oracles.

## Exact PR parents

| Repository | PR base branch | Immutable parent | Parent tree |
| --- | --- | --- | --- |
| `hsoliwal/com.synexia` | `codex/m3jdk21-string-array-recipes-20261018-308dd41c` (#12403) | `1124f5b72744b306884beedebafd8558d49d4c4c` | `4247615410371ff78f1e6985af3f540897a944fa` |
| `hsoliwal/M3jdk21` | `m3/doop-javabase-collections-convergence-20261019` (#1426) | `22e7a14ccd2998d994cb9a74abcdc7abd2086201` | `b36ec7ff7217d1909dca145197f5065bfc9e8e54` |

Create task-owned child branches from these exact parents. Keep both original branches and their complete ancestry. The sorted-recipe compiler repair in Synexia #12450 and M3JDK21 #1440 is a separate pair; this stack does not change that recipe owner.

The canonical parent already contains the complete `m3-array-mask-lookup-20261018` crate, source preimages/postimages, named recipe, actual SDK harness and unchanged receiver. It also owns the [refinement invariant](https://github.com/hsoliwal/com.synexia/blob/1124f5b72744b306884beedebafd8558d49d4c4c/docs/M3-SCALE/invariants/SYNEXIA-M3-CATALOGUE-PER-FILE-REFINEMENT-1.md), [problem catalogue](https://github.com/hsoliwal/com.synexia/blob/1124f5b72744b306884beedebafd8558d49d4c4c/synexia-openrewrite-recipes/catalogs/m3-string-index-reverse-20261018.tsv), and [donor/license context](https://github.com/hsoliwal/com.synexia/blob/1124f5b72744b306884beedebafd8558d49d4c4c/synexia-openrewrite-recipes/recipe-crates/m3-string-index-reverse-20261018/NOTICE.md). They remain canonical dependencies. The target receives its existing name-mapping fragment and contract document through the original six-output recipe.

## The two transformations

`com.synexia.m3.ArrayMaskLookup20261018` emits four implementation/oracle files and two documents. It improves immutable bitmap membership using the existing ascending/descending order and keeps the encounter-order scan. Its native histogram batches eligible signed strides through the existing bounded JNI region scratch. Public descriptors, JNI exports, checked geometry and output publication remain fixed.

The `array-mask-successor` dataset in the existing `m3-collections-contract-refinement-20261017` proof host emits four existing gate-file changes and five new qualification files. It separates current bitmap/histogram source admission from historical installer evidence. Original installers, templates, source manifests and earlier receipts remain historical. The old bitmap installer must still refuse the current source; the new checker must refuse old or arbitrary current bytes.

The complete successor recipe resource postimages are unchanged from the first 47981356-based qualification. The new trial changes only `DATASET.json` and `ENGINE_EFFECTIVE.tsv`: the target baseline is the exact #1426 parent and the effective engines are the canonical #12403 branch's inherited owners. Both original and successor crates were compiled and executed with their default canonical owner location. No global recipe source is modified by this PR.

| Compiled canonical engine | Git blob | SHA-256 |
| --- | --- | --- |
| `M3HashPinnedJavaSnapshotRecipe.java` | `75e0a6d360e371ee54f828a405acf38b4955e15e` | `d73c80e1ae6f47a827fec593bce6df30a55c5987099be44f52952442c65336b1` |
| `M3HashPinnedTextSnapshotRecipe.java` | `9d0788d3717e017d58345c5c3e62022c2915a3c8` | `fbf1ea9efad784afef5ac201ff7fc324551f77a099ebfe2f79b70c3a17c731ef` |
| `M3ScopeInference.java` | `21efff8ab0f3d71923b13614cb9084c0008f2eaf` | `be0677db4aa932e1b0d34489306e60dfa63a0140c14a6b01897b5a8bbcf00b62` |
| `M3EditScope.java` | `274360816db1e44a2f1796a3bc5cdeff6b0ce6db` | `962d37d62d740c3e8055c365a048ae7ee0479c26a9f6bbfe741e540d6c1134ab` |

The first trial's byte-exact proof files, SDK evidence and two prior metadata files remain recoverable from `evidence/history/47981356-proof.json`. Each UTF-8 carrier includes its original path, byte count and SHA-256. This preserves the original source binding instead of treating an earlier SDK run as execution with different engines.

## Receiving commands

Use a Java 21 installation, the canonical PR checkout and a target checkout at the exact #1426 parent. Set these three paths for the local environment. The Maven dependency cache location is optional; the recorded execution reused an isolated existing cache with `-o` and did not change either POM.

```bash
M3_RECIPE_CHECKOUT=/path/to/canonical-pr-checkout
M3_TARGET_CHECKOUT=/path/to/m3jdk21-child-checkout
M3_JAVA21_HOME=/path/to/java-21
export JAVA_HOME="$M3_JAVA21_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

M3_ARRAY_CRATE="$M3_RECIPE_CHECKOUT/synexia-openrewrite-recipes/recipe-crates/m3-array-mask-lookup-20261018"
M3_SUCCESSOR_CRATE="$M3_RECIPE_CHECKOUT/synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017"

mvn -B -ntp -f "$M3_ARRAY_CRATE/pom.xml" test
mvn -B -ntp -f "$M3_SUCCESSOR_CRATE/pom.xml" -Dm3.dataset=array-mask-successor test

python3 "$M3_ARRAY_CRATE/receive.py" --target "$M3_TARGET_CHECKOUT" --check
python3 "$M3_ARRAY_CRATE/receive.py" --target "$M3_TARGET_CHECKOUT"
python3 "$M3_ARRAY_CRATE/receive.py" --target "$M3_TARGET_CHECKOUT"

python3 "$M3_SUCCESSOR_CRATE/receive.py" --dataset array-mask-successor --target "$M3_TARGET_CHECKOUT" --check
python3 "$M3_SUCCESSOR_CRATE/receive.py" --dataset array-mask-successor --target "$M3_TARGET_CHECKOUT"
python3 "$M3_SUCCESSOR_CRATE/receive.py" --dataset array-mask-successor --target "$M3_TARGET_CHECKOUT"

python3 "$M3_TARGET_CHECKOUT/m3/collections/qualification/array-mask-successor-20261019/verify_successor.py" --root "$M3_TARGET_CHECKOUT"
```

On the exact preimage, original check/apply must report six changes and the repeat zero. Successor check/apply must report nine changes and its repeat zero. Both receivers accept their exact postimages as fixed points. All source inputs are admitted before the first serial write. Drift requires a separately inventoried refinement; do not force an earlier snapshot over it.

The reusable contract tools remain `contract/test_prepare.py` and `contract/verify_receiver.py` in the existing successor crate. The latter takes `--crate`, `--dataset array-mask-successor`, `--recipe-owner` pointing to `synexia-openrewrite-recipes`, and a fresh `--output` directory. These executed directly in this trial: eight sealing tests and nine receiver-integrity cases. This is not a claim that an unexecuted combined Maven profile passed.

## Composed-source execution

The initial bounded fixture used the exact #1426 changed files plus 308 unchanged source dependencies byte-checked against its `a3e88976` base. All fifteen receiving preimages matched. After both recipes, 339 pre-existing input files were unchanged. All 354 final source/gate inputs retained their bytes, modes and timestamps across runtime qualification. `SOURCE_INTAKE.json`, `SOURCE_ADMISSION.tsv` and `SOURCE_INPUTS.tsv` bind this inventory to the exact source revisions.

| Actual composed oracle | Observed counters |
| --- | ---: |
| Bitmap Java / checked JNI | 119,456 / 135,714 |
| Histogram Java / checked JNI | 403,492 / 421,946 |
| Read-only / mutable direct-slot views | 266,236 / 478,161 |
| Indexed fusion Java / required JNI | 9,962 / 19,911 |
| Deep filtered predicate composition | 3,119 |
| Actual patched `EnumMap` / `IdentityHashMap` | 4,411 / 78,847 |
| Aggregate focused assertion counters | **1,941,255** |

The Java and native commands are the retained source-gated bitmap/histogram commands plus the unchanged `run` bodies of #1426's `m3-doop-product-convergence-20261019.yml`. They compile the actual collection owners, require the existing native selector for the fusion oracle, and compile/patch the exact two `java.base` map sources. Histogram and bitmap JNI checks also retain strict C17, UBSan and `-Xcheck:jni`.

The final #12403-engine SDK Results were independently received from the same exact preimages and compared byte-for-byte with every corresponding tested file. This establishes source identity between the executed composed fixture and the final publication outputs. The two newer view-owner blobs and the newer indexed-list owner are never overwritten by older #1412 snapshots.

The independent receiver pins still hold: `M3LongIndexProjection` is Git blob `317136e66200db1d8fc72d47ff9f8cf52b4566b5` and `M3StateMaskArrays` is `06dc86a44df0e962613273011ff7d653f20d6ea9`. The successor preserves those checks.

## Review evidence and remaining gates

Start with `RECEIPT.json` and `COMPOSITION_CATALOGUE.tsv`. The receipt identifies every generated target and hashes every packaged evidence artifact. `evidence/stack1124/` contains both actual SDK input/result manifests, both JUnit XML reports, Maven logs, the serial receiving proof, source custody, receiver-integrity cases and composed runtime stdout/stderr. Original absolute command paths are retained as historical facts; the repository-relative artifact paths and commands above provide a fresh-layout reproduction route.

The tested environment is Linux with Java 21, checked JNI and patched stock `java.base` owners. Full installed M3JDK21 image execution, whole-module Maven qualification, full jtreg, the complete Synexia reactor and outer recipe admission, platform-wide qualification and performance measurement remain open. These passing correctness checks do not establish RAM-speed lookup or uniform performance nonregression. Original donor/license notices and Java/JNI ownership stay in force.
