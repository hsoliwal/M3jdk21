<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Original-compiler Gradle provider: 18 October 2026 qualification

The optional Gradle provider attaches the original `JavacTask` to the existing
public M3AST owner. Two independently discovered consumers receive the original
compiler trees and share one projection computation per parsed unit. This leaf
records actual recipe replay, compilation, tests and packaged-provider runtime
execution. It introduces no recipe engine or compiler owner.

## Qualified result

| Gate | Actual outcome |
| --- | --- |
| Existing class-backed Java/Text recipe SDK | 5 tests, 0 failures, 0 errors, 0 skipped |
| Unchanged sealing contracts | 8 cases passed |
| Unchanged receiver contracts | 9 cases passed |
| Receiver application and repeat | 9 receiving files, then 0 changes |
| Product source lint and strict Java 21 compilation | Passed; 1 main and 2 test sources, `-Xlint:all -Werror` |
| Provider tests on the real descriptor image | 20 tests, 0 failures, 0 errors, 0 skipped |
| Packaged-provider runtime | Passed; original and selected compilation have identical class bytes, and emitted code returns `42` |

The selected runtime has one original compiler task, two parsed units (original
and annotation-processor generated source), two real service consumers, two
projection computations, two projection hits and zero additional M3AST source
reads. Assertions compare callback tree identity with the original compiler's
PARSE event and compare `Trees.instance(originalTask)` directly. The ordinary
parity compilation is a separate baseline invocation.

The tests include effective plugin ownership with actual javac option operands,
`-XD` overrides, exact plugin names, six ASCII whitespace delimiters and NUL
segments. They also cover annotation processing, generated sources, syntax
retention, diagnostics, constructor/callback/finalization failures, explicit
close, a subsequent independent compilation, and a real child loader that
refuses parent-resolved consumer implementations before their constructors run.

## Exact owners and scope

| Owner | Pin or receiving path |
| --- | --- |
| Canonical recipe sources | `hsoliwal/com.synexia@140c263a90f9803b9c46103c63355a286e0515f0` |
| Existing recipe crate | `synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017` |
| New disjoint dataset | `datasets/gradle-provider-20261018` |
| Gradle host SPI | `hsoliwal/gradle@419a77cf61af3924f60678510ed2440230047671` |
| Exact SPI Git blob | `f2a08ee8074d6f6faec69543032c9176ad62ef11` |
| M3JDK receiving baseline | `hsoliwal/M3jdk21@2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49` |
| Receiving prefix | `m3/tooling/module-packs/build-support/gradle-provider/` |
| Implementation | `org.gradle.internal.m3.ast.M3GradleASTProvider` |
| Public dataset mirror | `m3/tooling/shared-frontend/recipe-custody/datasets/gradle-provider-20261018` |
| Public evidence mirror | `m3/tooling/shared-frontend/recipe-custody/enhancements/20261018-gradle-provider` |

The SPI source under `proof-inputs/gradle/` is the exact published host contract,
retained as a qualification input. The provider artifact contains only its three
implementation classes. It does not package the Gradle SPI, M3AST API, JSpecify
annotations or test consumers. The actual artifact hash and entry inventory are
in `ARTIFACT_INVENTORY.json`.

The compiler is a qualified hybrid Java 21 image: four actual M3 compiler source
owners plus the actual M3 module descriptor overlay preserved stock Java 21
modules and native payload. The supported `jmod` hash mechanism regenerates the
copied `java.base` descriptor hashes. No hash check is disabled. This establishes
compiler-module qualification; a full source-built M3JDK is unverified.

The exact Gradle SPI source was compiled and used, but the complete pinned
Gradle distribution, host wrapper, worker/daemon and incremental tasks were not
executed. A suitable source-built distribution and its required JDK 25 build
environment were unavailable. Full Gradle-distribution execution remains an
explicit separate gate.

## Evidence

`QUALIFICATION_RECEIPT.json` ties the actual command records to the nine source
hashes, sixteen dataset input hashes, compiled classes, test report and runtime
receipt. `SOURCE_LOCK.json` records each receiving path as initially absent and
its exact resulting SHA-256. `sdk/` preserves the actual SDK input/result
records, Java/text patches and test reports. `logs/` preserves every recorded
stage outcome, including failed attempts; successful later gates are identified
by the qualification receipt.

`FINAL_PREPARATION.json` records the only input normalization. The unchanged
prepare helper derives a service-template basename containing `$`; the unchanged
text engine admits `$` in a Java service target path but restricts template
references to `[A-Za-z0-9_.-]+`. The final manifest therefore references
`0008-M3AST-Consumer.txt`, with unchanged bytes and unchanged receiving
`com.sun.source.util.M3AST$Consumer` filename. The frozen dataset also retains
the identical unreferenced prepare-produced template, and both resources appear
in the successful SDK input hash record. The final local preparation helper
explicitly reproduces that sixteen-file inventory. No engine admission rule was
widened and no receiving source was hand-repaired.

An initial Maven contract-profile launcher failed because the offline cache
lacked its exec plugin. The exact unchanged Python commands configured by that
profile subsequently passed all eight sealing and nine receiver contracts. The
Maven contract-profile launcher itself is not claimed as passed. A previous
provider proof-profile compile exposed Maven compiler-plugin 3.13.0's handling
of split `--patch-module` arguments in a nonmodular test compile. The recipe-
qualified single-token option passed final main and test compilation. Historical
failures remain in the logs; they are not substituted for current test results.

The original runtime proof and test reports retain their absolute temporary
execution paths as provenance. `EVIDENCE_MANIFEST.json` maps preserved files to
their original paths and hashes. Binary build outputs are represented by their
exact class and artifact inventories; the provider and probe source live in the
nine-file receiving result and dataset.

## Receiver handoff

The final local SDK execution is under
`target/gradle-provider-20261018` in the original isolated recipe crate. From
that crate, the receiving owner can use the unchanged receiver:

```sh
python3 receive.py --dataset gradle-provider-20261018 --target /absolute/M3jdk21 --recipe-owner /absolute/synexia-openrewrite-recipes --check
python3 receive.py --dataset gradle-provider-20261018 --target /absolute/M3jdk21 --recipe-owner /absolute/synexia-openrewrite-recipes
python3 receive.py --dataset gradle-provider-20261018 --target /absolute/M3jdk21 --recipe-owner /absolute/synexia-openrewrite-recipes
```

`ROOT_HANDOFF.json` records the exact local command argument arrays and receiving
file inventory. The dataset and this enhancement leaf are new additions under
the existing canonical crate. They leave all existing engine, harness and
custody owners unchanged. Publication and receiving-owner verification remain
the parent task's responsibility.
