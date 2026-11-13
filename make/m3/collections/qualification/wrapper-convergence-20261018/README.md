<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Wrapper, stable-index and JNI qualification

The completed series qualifies **three production atoms** at the tested integration base **`f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`**. Its reusable recipe sources remain canonically owned by Synexia, with exact public custody copies and an existing public four-source engine closure. [ACCEPTANCE.json](ACCEPTANCE.json) holds the current execution acceptance; [HANDOFF.json](HANDOFF.json) records pins, manifests and scope.

## Two stacked review units

| Review unit | Production change | Verification boundary |
| --- | --- | --- |
| Wrapper PR | Guard ordinary `StrictReadOnlyList` iterator removal in `M3FilteredLists.java`. | The existing owner promises unconditional mutation refusal. The repair changes the erroneous pre-next removal exception to `UnsupportedOperationException`; it preserves the prior read cursor and effects. The isolated wrapper tree passed 232 tests across 78 suites and retains the inherited comparator-failure witness. |
| Companion index/JNI PR | Stable equal-group reversal in `M3LongIndexProjection.java`, followed by suffix-only row transfer in `m3_index_mask_window.c`. | Preserves physical duplicate order, current address-kernel ownership, unknown-order merge behavior, native output batching, bounds and exports. The complete series includes its independent index/window proofs and native region instrumentation. |

This overview and catalogue cover the **complete series**. The index/JNI production and verification changes are delivered in the companion PR. Their inclusion in the documentation is not a claim that they are present in the wrapper-only review unit.

Full pre/post hashes and the three exact behavioral atoms are in [ACCEPTED_ATOMS.tsv](../../algorithm-catalog/wrapper-convergence-20261018/ACCEPTED_ATOMS.tsv). The original 139-owner census and tested-f29 143-owner census remain separate, exact archives in the [catalogue](../../algorithm-catalog/wrapper-convergence-20261018/README.md). These counts exclude later master additions and do not imply that every inventoried owner was qualified.

## Executed qualification

| Evidence layer | Recorded result | Scope |
| --- | --- | --- |
| Canonical recipe host | **12 passes, 60 SDK tests**; zero failures/errors/skips | Two fixtures, three production atoms, six verification-file passes and the invariant policy pass. |
| Copied public host | **Two fixtures, 10 SDK tests**; zero failures/errors/skips | Separate Java/text portability proof using only public engine and host source paths. |
| Complete-series Java mode | **233 tests, 79 suites**; zero failures/errors/skips | Actual owning collections module at the recorded complete-series source state. |
| Complete-series native-enabled mode | **233 tests, 79 suites**; zero failures/errors/skips | Separate owning-module execution with the real candidate native library and `-Xcheck:jni`. |

The public portability tests are an additional execution of the fixture lanes. They do not increase the twelve canonical pass count. Java and native totals describe their separate runs; they are not counts of different test definitions.

[SDK_QUALIFIED_PASSES.tsv](../../algorithm-catalog/wrapper-convergence-20261018/SDK_QUALIFIED_PASSES.tsv) indexes the twelve executed pass receipts and exact SDK binding counts. The [evidence directory](evidence/) retains the actual SDK, receiving, runtime and portability receipts. Public/protected API, JNI ABI and receiving fixed-point checks are linked from the acceptance record.

Each pass used the unchanged snapshot engine and proof host for actual replay, zero-change fixed point, input-order comparison, source drift/duplicate refusal, post-scan drift refusal and guarded retention. Present preimages additionally exercise missing-required-source refusal; a target expressly admitted as `ABSENT` has no missing-before-source case. Java targets additionally refuse a plain-text representation. Input reversal for a newly added target with only the unrelated decoy is a one-element case; existing-target fixtures and production passes exercise distinct input orders.

The latest documentation-pass native run reused an auxiliary suite-list filename. The ordinary run's command, exit/count receipt and raw logs remain retained, but its overwritten auxiliary per-suite hash list is not claimed retained. The native list and the corresponding evidence note disclose that limit. This does not replace or alter earlier per-file receipts.

## Reused public engine closure

The four unchanged owners already exist at:

```text
m3/arrays/qualification/empty-segments-20261017/recipe-custody/sdk-owner
```

[ENGINE_SOURCES.tsv](recipe-custody/recipe-host/ENGINE_SOURCES.tsv) pins their canonical commit `98368e107f4a205d852ef958cd48c1ee633219b1`, Git blobs and SHA-256 values. The public portability proof checked all four bodies before compilation. Actual compiler input lists contained those four public files and the copied host test only; executed Surefire properties confirmed the public roots. No additional engine implementation or production runtime dependency was introduced.

The copied POM must be given an **absolute public owner root** through `recipe.owner.root`. From the target repository root, after checking the copy/engine hashes and the recorded lint requirements:

```sh
m3_recipe_owner_root="$(pwd)/m3/arrays/qualification/empty-segments-20261017/recipe-custody/sdk-owner"

mvn -f m3/collections/qualification/wrapper-convergence-20261018/recipe-custody/recipe-host/pom.xml \
  -Drecipe.owner.root="$m3_recipe_owner_root" \
  -Dm3.pass=fixture-java -DskipTests test-compile

mvn -f m3/collections/qualification/wrapper-convergence-20261018/recipe-custody/recipe-host/pom.xml \
  -Drecipe.owner.root="$m3_recipe_owner_root" \
  -Dm3.pass=fixture-java surefire:test
```

Run the same pair with `fixture-text` only after the Java fixture passes. The recorded proof used Java 21, offline cached OpenRewrite 8.17.1/JUnit 5.10.2, and the unchanged strict `-Xlint:all -Werror` compiler configuration. Its exact settings, cache and toolchain paths are execution-environment details retained in the raw commands. The Maven output remains below the ignored public host `target` directory and is excluded from publication.

## Retained limits and attribution

The iterator repair qualifies its declared mutation-refusal boundary and preserved read effects. Comparator exception identity in bounded navigation, lawful interior-null navigation, ordinary-iterator predicate work and backing fail-fast remain unresolved. The numeric bounded-map/set witnesses pass at f29; that does not establish arbitrary comparator failure preservation. The [reconciliation](../../algorithm-catalog/wrapper-convergence-20261018/catalogue-review/RECONCILIATION.md) retains the exact execution/source distinction and the original failing modes.

Selected implementation reuse is first-party Synexia/M3 history. External implementations remain licensed comparison references in the exact [donor catalogue](../../algorithm-catalog/wrapper-convergence-20261018/index-candidates/CATALOGUE.tsv). [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), the existing [module LICENSE](../../LICENSE) and [NOTICE](../../NOTICE) retain the source/dependency attribution.

The public [catalogue copy manifest](../../algorithm-catalog/wrapper-convergence-20261018/CANONICAL_COPY_MANIFEST.tsv) and [recipe copy manifest](../../algorithm-catalog/wrapper-convergence-20261018/RECIPE_CUSTODY_COPY_MANIFEST.tsv) identify exact canonical bytes. Historical catalogue rows and earlier pending statuses remain unchanged. New acceptance is stated separately, and the published canonical source commit is recorded in the handoff.

