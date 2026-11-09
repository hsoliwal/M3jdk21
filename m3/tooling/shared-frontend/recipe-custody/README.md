<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
     SPDX-License-Identifier: Apache-2.0 -->
# Shared frontend recipe custody

This optional tooling directory carries the exact canonical Synexia snapshot owners, Maven/OpenRewrite SDK harness, contract tests, thin receiver, and two source datasets used to generate the shared compiler frontend and optional build packs. Synexia owns the reusable transformations. `OWNER_CUSTODY.json` records their original and effective identities; `ENGINE_SOURCES.tsv` preserves the original source epoch. The JDK runtime has no dependency on this tooling directory.

## Reproduce the source transformations

Use Java 21, Maven 3.9 or newer, and Python 3. The POM pins OpenRewrite 8.17.1 and JUnit 5.10.2. From this directory, run:

```sh
mvn -B -ntp -Dm3.dataset=m3jdk21-build \
  -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify
mvn -B -ntp -Dm3.dataset=m3jdk21-compiler \
  -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify
```

Use `-o` when these Maven dependencies are already cached. Each dataset has a separate `target/<dataset>` directory. For another retained execution, provide a fresh `-Dm3.contract.output=/absolute/new/proof` directory. After altering dataset membership, clean its generated Maven output before rerunning so old resources cannot remain on the classpath.

Each invocation executes five real SDK tests, eight input-sealing tests, and nine isolated receiver cases. These cover exact replay, fixed point, reversed input order, mixed pre/postimages, absent inputs, per-target drift, wrong Java representation, post-scan changes, source/result tampering, symlink refusal, and file-mode/timestamp preservation. Results are retained under `target/<dataset>/candidate` with `SDK_RESULTS.tsv` and complete source bindings. Existing receipts in `evidence/` record the qualified authoring execution; fresh executions produce their own reports.

## Receive into a checkout

Set `M3_TARGET` to an isolated M3JDK21 checkout. The receiver admits all current pre/postimages before serial atomic file replacement. For each dataset:

```sh
python3 receive.py --dataset m3jdk21-build --target "$M3_TARGET" \
  --recipe-owner "$PWD/sdk-owner" --check
python3 receive.py --dataset m3jdk21-build --target "$M3_TARGET" \
  --recipe-owner "$PWD/sdk-owner"
python3 receive.py --dataset m3jdk21-compiler --target "$M3_TARGET" \
  --recipe-owner "$PWD/sdk-owner" --check
python3 receive.py --dataset m3jdk21-compiler --target "$M3_TARGET" \
  --recipe-owner "$PWD/sdk-owner"
```

Repeat application at the fixed point writes no files. A checkout containing different source bytes is refused for review. Serial per-file replacement does not provide a multi-file filesystem transaction under concurrent writers or interrupted I/O.

The two embedded datasets cover 29 product source files and exclude this directory. A separate source-owned custody dataset transfers this directory after those datasets are qualified. This separation keeps source receipt generation finite. Correct target behavior by updating canonical recipe inputs and regenerating through the existing engines.

## Runtime evidence

`evidence/runtime/` records 70 shared frontend compiler checks and five actual existing-unit OpenRewrite integration tests on the supplied modified compiler image. The wrapper baseline receipt records current filtered/sorted collection and bounded JNI parity gates, with their exact source hashes. These receipts describe their tested scope. A complete source-built JDK image and build-tool distribution remain separate product acceptance gates, and no performance improvement is inferred from transformation hashes.

Run the receiving `../verify_frontend.py` against the intended JDK image for product frontend acceptance. Its README and the optional build-pack documentation describe the runtime commands and controls. `LICENSE.txt` applies to the new harness and first-party recipe owners; receiving snapshots preserve their existing source notices, and Maven dependencies retain their own licenses.
