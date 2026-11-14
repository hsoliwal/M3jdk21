# Selected producer recipe: visible caller damage

This Maven recipe reuses the existing `M3WebGen.emit` producer and the two qualified generator templates for `M3WebLanes` and `M3WebChecks`. The generator templates are the owning implementation sources. Generated Java is produced by this recipe and must not be patched separately.

The caller repair ignores unresident item rectangles, clips each damage input before accumulation, calculates padded intersections with wide intermediates, and preserves the existing virtual damage hook and backend exception/output effects. It adds no retained fields to the production owner and changes no public or protected API.

## Reuse and custody

Producer: `hsoliwal/M3jdk21@b98bf8c065818aac3447be165909e2a013325d9a`, `m3/ports/swing-viewport/webwidgets/gen/M3WebGen.java`, SHA-256 `0ea3cf99fb09e346774715f69f1b47565ade39c2121a5e470565b2d55401a150`.

The template after-images come unchanged from the previously qualified Synexia caller repair. Its generated Java after-images receive only the mechanical `com.m3.` to `jdk.m3.` namespace substitution. Original Apache-2.0 OR EPL-2.0 notices are retained. The producer differs from the historical qualified producer only in its two namespace strings.

`bindings.tsv` closes the scope to one producer, two templates, two generated Java files, fifteen unchanged compilation dependencies and one unchanged approved oracle. Template sources must already equal the pinned after-images before apply; this controller does not bypass a snapshot engine to edit templates.

## Running

Use JDK 21 and Maven through an admitted storage/process runner. All three roots must be absolute paths. Use a fresh output root for each invocation and keep temporary files, JVM home/preferences and build output on that same admitted lane. Local runs use provisioned offline artifacts and the existing reserve/output guards. The hosted workflow resolves pinned Maven artifacts inside its bounded runner repository.

```sh
mvn -o -B -ntp -f /absolute/recipe/pom.xml verify \
  -Dmaven.repo.local=/admitted/provisioned-repository \
  -Dm3.caller.receiverRoot=/absolute/M3jdk21 \
  -Dm3.caller.recipeRoot=/absolute/recipe \
  -Dm3.caller.outputRoot=/admitted/fresh/qualification \
  -Dm3.caller.mode=qualify
```

Run again with another fresh output root and `-Dm3.caller.mode=apply` after qualification. Commit the exact two generated Java outputs and the ledger candidate produced by the named recipe, together with the owning template edits and this recipe. Repeating apply must leave their bytes and modification times unchanged.

Qualification emits before and after sources in isolation, compares exact hashes, verifies same-instance and fresh-instance zero-write replay, compiles each seventeen-source closure with `--release 21 -Xlint:all -Werror`, and compares the protected API of `M3WebLanes`. A failed preflight must not modify receiving Java. The compiler uses an explicitly empty classpath and sourcepath directory, so the selected seventeen-source closure cannot borrow the recipe's libraries.

The generated-file ledger has a separate seal in `resources/manifest-pins.tsv`. Maven checks that ledger before generating Java. The existing OpenRewrite `FindAndReplace` recipe then updates only the two selected full rows, including their current namespace paths and actual emitted source hashes. Qualification verifies exact after-bytes, unchanged header/order/every other byte, fresh replay and refusal of missing, duplicated or changed rows. Apply retains ledger bytes and modification time when already current. The producer's whole-ledger `manifest()` operation is never called with a two-file map.

## Runtime dependency

Rewrite 8.17.1's `YamlResourceLoader` uses SLF4J, although its published POM omits that dependency. The controller explicitly pins `slf4j-api:2.0.9` with runtime scope, reusing the already provisioned compatible artifact. A logging provider is unnecessary; its three NOP-provider diagnostic lines are retained. The selected application compilation remains isolated from all recipe libraries. See `THIRD_PARTY_NOTICES.md`.

## Qualification limits

Historical evidence is not fresh namespace qualification. The previous generator proof established exact two-file emission and zero-write replay; the previous API proof found identical protected surfaces. The repaired Maven recipe passed current-namespace qualification on 2026-11-13 with Java 21.0.9 and Maven 3.9.14 using offline artifacts. The complete native receipt and exact source/ledger reports are in `evidence/runtime-20261113/`. This proves selected emission, strict compilation, protected-API equality, same/fresh replay, the named ledger recipe and its refusal cases.

The approved eighteen-method `M3ViewportCoreApiTest` is retained byte-for-byte under `fixtures/` and is outside the two generated targets. It is compiled into a separate test output, ahead of the ordinary four-method test of the same name, and run in a separate CI job. All failures and the nonzero process status remain visible. No test is removed or weakened.

This caller change does not implement the separate full-domain accumulator repair. That repair still requires exact wide unions, checked legacy projection with retained state, and `takeWide(long[])`. Passing the caller/public suite cannot qualify those outstanding contracts or complete the wider Synexia UI goal.

The original hosted run `43250472845` reached selected-producer PASS and then failed loading SLF4J; it ran no JUnit tests. After the dependency correction, local `qualify-001` completed successfully and emitted the exact Java and ledger candidates now supplied for review. Subsequent local receiving apply attempts were stopped by the existing physical-memory reserve; they are not successful apply or behavior evidence. The eighteen-method oracle, caller tests and full public suite remain required in the focused hosted workflow. All original failures, counter values, and storage/process protections remain in force.

The subsequent focused Java/JUnit source compilation passed with zero diagnostics and produced three classes from the unchanged helper and two existing test classes. This is compilation evidence only. Caller/public JUnit requests and the separate unchanged-oracle compiler were refused before process creation when available commit fell below the existing 8.25 GiB reserve. A later Maven apply continuation was likewise stopped by that commit guard and closed all owned handles. `FOCUSED_COMPILE.json`, `TEST_PREFLIGHT_HOLD.json` and the native receipts retain these separate outcomes. No failures were suppressed and no foreign processes were stopped.

On 2026-11-13 the existing seven caller tests and four unchanged original core tests executed successfully, without skipped or aborted tests. The exact unchanged eighteen-method oracle then ran all methods: twelve passed and six failed. Three failures expose accepted-damage loss; three report the still-missing `takeWide(long[])` API. Raw reports/logs and `FOCUSED_BEHAVIOR.json` preserve the failing exit status. These tests used the existing offline Console 1.10.1 identity, separately from hosted vendored 1.10.2. The complete 307-production/151-test source closure is now hash-verified for full-suite qualification, whose compiler launch is held by the physical-memory guard. Focused success does not qualify the full suite or the remaining wide/native/JNI/platform/performance contracts.

Full module compilation then passed: all 307 production sources produced 357 classes, and all 151 test sources produced 192 classes, with zero diagnostics under the existing runner's lint policy. The first complete headless scan was stopped by the existing physical-memory reserve before it printed a summary. Its discovery/pass/failure counts remain unknown. `full-module/` retains the exact 458-source manifest, compiler arguments, class hashes and native receipts; no full-suite success is claimed from compilation or the stopped run.

The complete first executable public-suite run found 375 tests, with 374 passes and one `M3AlertBannerTest.sampleContract` failure. Dismissal intentionally preserves its item's width but makes its height zero. The old shared test precondition checked width alone before demanding visible damage. The separately qualified `caller-item-damage-contract-20261113` Maven/OpenRewrite recipe now requires positive width and height, retaining all positive-item assertions. The existing generator emitted the corrected helper; the full unchanged 375-test suite then passed. The unchanged eighteen-test oracle was rerun with that helper and retained all six failures. The main producer/ledger recipe has also been requalified with the enrolled helper/template/ledger hashes. Native/JNI, hosted vendored1.10.2, performance and full-domain accumulator proof remain distinct and incomplete.

The exact receiving runtime has now also been qualified locally: all 475 vendored Java sources compiled with the receiving script's `--release 17` lane, its 16 resources were copied in the original owner order, and all 151 test sources were recompiled against those classes. The complete public suite passed all 375 tests with source-built Platform 1.10.2/Jupiter 5.10.2. The unchanged eighteen-test oracle retained twelve passes and six failures. This Windows/JDK21 result establishes the source-built runtime locally; it does not establish the queued hosted Linux job or native toolkit/JNI/SCREEN/performance/platform gates. Exact commands, source/resource/class seals and native receipts are retained in the canonical recipe's `evidence/runtime-20261113/receiving-junit-1.10.2/` directory.
