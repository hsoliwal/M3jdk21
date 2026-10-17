<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Exact upstream shared-frontend contract in a linked Java 21 image

The exact upstream `M3ASTSharedFrontendTest.java` passed unmodified: **70 checks across all ten main-invoked test families**. A separate **9-check coexistence probe** passed with the builtin `M3AST` and external `M3ASTIndex` plugins on one real compiler task, with one parse, two top-level analysis events, and one immutable syntax export.

This packet preserves upstream test and module descriptor bytes from M3jdk21 commit `4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`. Their original GPL and Classpath-exception notices remain intact. `SOURCE_LINEAGE.json` records exact paths, Git blobs, and SHA-256 values. Proof helpers are Apache-2.0 source; they are not product files.

## What was exercised

The unmodified upstream test invokes `sharing`, `lazyAndFailureMemoization`, `editedSource`, `separatePhases`, `callbackFailure`, `explicitCallbackClose`, `cleanupFailure`, `pluginAndImage`, `providerClassLoader`, and `generatedSources`. It checks the actual compiler task's shared event-local projections, retained source structure, thread and callback lifetime, stale source refusal, exception identity and cleanup, split compiler phases, builtin plugin service discovery, primitive image parity with its existing CLI oracle, external consumer discovery on the processor path, and annotation-generated source participation.

The separate coexistence probe loads both real Plugin services and invokes both on the same actual `JavacTask`. It verifies real class outputs, a single PARSE event, builtin completion/error statistics, the external plugin's matching parse/type/capture statistics, and one `.m3ast` export. It does not modify or select a subset of the upstream test.

| Qualified input/output | SHA-256 |
| --- | --- |
| Exact upstream contract test | `47349ef82633f724f3ca694521250048ba70486b094b069fc51c13b5e64978a5` |
| Exact upstream compiler module descriptor | `ae52d1734570fee7404aeb68b65ef86ea23ad322b72ff0b137c9039c71aafb09` |
| Final composed compiler patch | `3f50d59cca1cf7cf2811c0bb1da9314b71240c7f81f37854ed6fc021f03681b4` |
| Final external frontend artifact | `f4baf0ae24e58c290a7de403c0571dccbe35c825694729aca40c3408db5b905c` |
| Linked image `lib/modules` used for these checks | `f4bb3a453e067a76d6821fb08116288255d824a8b1112955607d5ef5c8eadd04` |

The linked fixture contains exactly five modules: `java.base`, `java.compiler`, `jdk.compiler`, `jdk.internal.opt`, and `jdk.zipfs`, all based on the recorded Java 21.0.2 toolchain. The proof uses the actual linked `jdk.compiler` descriptor. It does not depend on `--patch-module` supplying module service declarations that a stock runtime would ignore.

## Fixture assembly and module integrity

`qualify.py` checks the exact patch and custody source pins before assembly. It extracts the original `jdk.compiler.jmod`, overlays only the class files of the four qualified source owners, compiles the exact upstream descriptor, and recreates the compiler jmod with the original version, platform, and sections. Every original entry outside the exact class overlay and descriptor is checked byte-for-byte by SHA-256.

The stock `java.base` descriptor records module hashes, including the original compiler. The composed compiler needs its actual hash recorded before linking. The proof recreates a scratch `java.base.jmod` using the stock JDK's standard `jmod --hash-modules` implementation. All **65 original hash names remain**, and **only the `jdk.compiler` hash changes**. Every other `java.base` entry is identical, and its structural module descriptor is unchanged. Hash checks remain enabled throughout linking and execution. No product `java.base` source or JDK installation is edited.

Java 21's `ModuleInfoExtender` appends generated attributes when recreating an already decorated descriptor. The small `JmodDescriptorInputs` fixture helper uses that JDK's own classfile API to remove only the old `ModulePackages` and `ModuleHashes` attributes before standard jmod immediately recomputes them; the existing version and target fields are retained. The output descriptor and all hash records are then independently compared. The initial duplicate-attribute failure is preserved in `evidence/history/stock-jmod-duplicate-attributes/`.

Builds use a new isolated scratch generation directory. This also avoids reusing stale output observed when an earlier workspace generation was replaced; the observation and failed assembly attempt are retained in `evidence/history/stale-output-generation/`. Proof classes, jmods, the linked image, and test outputs do not enter any frontend product build directory. Only source custody, proof code, logs, and receipts belong in the published packet.

## Reproduce

Use a complete Java 21 JDK with `javac`, `jmod`, `jlink`, and its original `jmods`. The exact qualification used the Java 21.0.2 JDK whose original jmod hashes are in `evidence/QUALIFICATION.json`. The finite proof needs Python's standard library and the already qualified frontend/patch artifacts; it does not need an unpublished canonical checkout or Maven dependencies.

From this directory, or using absolute script paths:

```sh
python3 qualify.py --jdk "$JAVA_HOME" --patch "$M3_AST_PATCH"
python3 coexistence.py \
  --frontend-jar "$M3_AST_PLUGIN" \
  --frontend-sha256 f4baf0ae24e58c290a7de403c0571dccbe35c825694729aca40c3408db5b905c \
  --patch "$M3_AST_PATCH"
```

`M3_AST_PATCH` must identify the pinned final `m3-javac-patch.jar`; `M3_AST_PLUGIN` identifies the qualified `m3-ast-sharing.jar`. The first command creates a fresh directory under `/tmp` and records it in its receipt. `--work-dir` may instead specify a new, nonexistent isolated directory. The second command uses that receipt's exact image and rechecks both final artifact hashes. The commands never delete or rewrite an existing JDK or product build directory.

For a fresh source-qualified build on another Java 21 distribution, pass the actual `ARTIFACTS.json` emitted by `frontend/build_candidates.py` to both commands:

```sh
python3 qualify.py --jdk "$JAVA_HOME" --patch "$M3_AST_PATCH" \
  --artifact-receipt ../target/ARTIFACTS.json
python3 coexistence.py --frontend-jar "$M3_AST_PLUGIN" --patch "$M3_AST_PATCH" \
  --artifact-receipt ../target/ARTIFACTS.json
```

Receipt admission requires the supplied patch's actual digest, exactly four compiler source paths and hashes, the pinned compiler manifest, and all four retained templates. It refuses a digest without the matching source map. The coexistence proof requires the identical receipt used for the linked image and checks that receipt's frontend digest. The source-qualified builder must be run first; this local receipt is an integrity record, not a signature. JAR manifest metadata can differ between JDK distributions even for the same qualified source inputs, so a fresh receipt authorizes a fresh proof with its own recorded artifact and image identities. Omitting `--artifact-receipt` retains the exact historical patch pin.

`evidence/QUALIFICATION.json` and `COMMANDS.json` record the 70-check exact-source proof, tool commands, stock module preservation, image identity, and logs. `evidence/COEXISTENCE.json` records the separate 9-check final-artifact test and its two actual task receipts. A new run records its actual toolchain and image hashes; byte identity across different host JDK distributions is not asserted.

The drivers used for those original 70+9 checks are preserved under `evidence/history/exact-79-driver/`, with their original packet hashes. The receipt-admission successor has a separate finite test receipt under `evidence/receipt-admission/`; it does not relabel the original compiler execution. Its metadata checks can be repeated without assembling an image using `python3 -m unittest discover -s tests -p 'test_artifact_admission.py'`.

## Qualification boundary

This is a finite linked-image compiler/API/service proof. It is not a full M3 JDK build, jtreg harness run, JCK result, or global receiving-phase promotion. The complete upstream test's `main` is run directly with the internal export declared by its `@modules` requirement. Full repository, module, native, and migration gates retain their separate authorities in `m3/compiler/ast-sharing-20261017/handoff.json`.
