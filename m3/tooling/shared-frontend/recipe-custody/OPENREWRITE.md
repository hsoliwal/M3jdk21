<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
     SPDX-License-Identifier: Apache-2.0 -->
# Additional OpenRewrite and selected-tool recipe custody

This addendum extends the original compiler/build custody described in `README.md`. The 75 original mirror files and the 12 original provenance files retain their exact bytes. `OPENREWRITE_CUSTODY.json` identifies the four additional product datasets, their canonical SDK input/result hashes, and every exact source copy. Their snapshots preserve the receiving product licenses. The reused owner authority and effective-source epoch remain canonical Synexia.

| Dataset | Targets | Qualified preimage epoch |
| --- | ---: | --- |
| `m3jdk21-openrewrite-tool` | 4 | New paths at original M3 `c0f992009f7045c9f0d8ef4f734b3f1113c7a3f6` |
| `m3jdk21-openrewrite-pack` | 5 | New paths at original M3 `c0f992009f7045c9f0d8ef4f734b3f1113c7a3f6` |
| `m3jdk21-lock-order` | 2 | Existing build-tool postimages at M3 `1489e102d5edcb4d66bfa5c99615d7d0157f59d7` |
| `m3jdk21-selected-verifier` | 1 | New path at original M3 `c0f992009f7045c9f0d8ef4f734b3f1113c7a3f6` |

The existing `sdk-owner`, POM, SDK tests, sealing tests, and thin receiver qualify the additional datasets with Java 21, Maven 3.9 or newer, and Python 3. From this directory:

```sh
mvn -B -ntp -Dm3.dataset=m3jdk21-openrewrite-tool -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify
mvn -B -ntp -Dm3.dataset=m3jdk21-openrewrite-pack -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify
mvn -B -ntp -Dm3.dataset=m3jdk21-lock-order -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify
mvn -B -ntp -Dm3.dataset=m3jdk21-selected-verifier -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify
```

Use `-o` when pinned dependencies are cached, and a fresh `-Dm3.contract.output=/absolute/new/proof` for another retained execution. Each command executes five real SDK tests, eight sealing tests, and nine isolated receiving cases. Successful Results appear under `target/<dataset>/candidate`. The copied SDK/runtime evidence retains the scope and hashes of its original execution; fresh runs produce their own reports.

Receive each desired product dataset using the existing `receive.py --dataset <dataset> --target <isolated-checkout> --recipe-owner "$PWD/sdk-owner" --check`, then repeat without `--check`. Source pre/postimages, complete input and effective owner bindings, actual SDK Result hashes, modes, and symlink/path checks remain mandatory. A fixed-point replay writes no files. This additive transfer does not contain its own custody dataset, so proof-source generation remains finite.

On a fresh original checkout, apply the original compiler/build datasets before the later lock-order overlay. The original `m3jdk21-build` dataset deliberately preserves its old two postimages; after the overlay, rerunning that old receiver against the final checkout correctly refuses the later bytes. Replay each dataset in its encoded epoch or an isolated fixture. The latest lock-order dataset accepts its own final postimages. No original epoch or drift guard is weakened to make a superseded dataset appear current.

## Executed product evidence

The runtime evidence in this addendum records separate, linked gates:

| Directory under `evidence/runtime/` | Actual scope |
| --- | --- |
| `openrewrite-tool` | Exact emitted Java runner, strict compilation and six real tests on the source-built M3JDK |
| `openrewrite-pack` | Fifteen pinned OpenRewrite 8.17.1 production artifacts, 57 original runtime dependency JARs, eleven framework tests and nine pack contracts |
| `lock-order` | Nineteen actual lock contracts, failing-before regression, and unchanged validator admission of the native Maven4 171-file tree |
| `selected-tools` | Exact emitted portable verifier; selected native Maven plus OpenRewrite; 336 assets preserved; relocated image path containing spaces; actual native Maven compilation/capture and typed two-file recipe |
| `installed-framework` | Eleven tests against the installed manifest runtime, fourteen checked production-class origins, and 162 original OpenRewrite assets unchanged before and after execution |
| `synexia-ast-product` | Exact canonical eight-file AST result set, zero strict-compile diagnostics and nineteen tests on the complete source-built M3JDK; no full Synexia reactor claim |

`selected-tools/RECEIPT.json` binds the original source JDK, both input locks, assembly commands, shared-compiler counters and patch output. `linked-image-receipt.json` and the asset manifests preserve assembly scope independently; the assembler's generic receipt does not itself assert downstream product acceptance. The later selected and installed-framework receipts provide those explicit runtime gates. Earlier pack receipts whose selected-image gate was still pending retain their original bytes and historical scope.

The portable selected-tools verifier is installed at `m3/tooling/module-packs/verify_combined_selection.py`. Supply `--repository`, `--jdk`, `--maven-lock`, `--rewrite-lock`, `--maven-repository`, and a fresh `--out`. It creates and relocates a selected image, so allow sufficient space and time. The exact installed-framework harness pair is retained together in `evidence/runtime/installed-framework/src/test/java/`. Its Python command accepts `--image`, fresh `--output`, `--proof`, `--tool-pom`, and `--maven-repository`. The proof input contains the rebuilt runtime's `runtime/test-classes`, the two prior pack qualification receipts, and `openrewrite-lock.json`; the receiving module POMs and pack documentation explain the source build. The harness pins its actual test classes, JUnit inputs, image and installed assets in each fresh receipt.

The product documentation at `../../module-packs/build-tools/OPENREWRITE.md` describes the selected framework, runtime build and Java recipe command. The Java command operates on explicit Java sources; the full bundle also exposes its original non-Java framework APIs. The retained evidence establishes Linux x86_64 execution. Windows launcher execution, the complete Gradle source build, and broad JDK performance testing remain separate gates. No image, JAR, class file, native library or other binary is copied into this source custody.
