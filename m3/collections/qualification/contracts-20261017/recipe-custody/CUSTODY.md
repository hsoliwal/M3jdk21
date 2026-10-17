<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Portable collection recipe custody

This directory carries the exact source needed to reproduce the finite collection recipe independently of a private Synexia checkout. [SOURCE.json](SOURCE.json) pins the canonical source commit and [OWNER_CUSTODY.json](OWNER_CUSTODY.json) maps every copied source/evidence file to its canonical path, Git blob, SHA-256, mode and delivery alias. The four existing Java/text snapshot and scope owners remain unchanged from their original `10e8628b2479e56f6f841f160233d3a8bf04a6db` pin. The task harness reuses the existing qualified AST/build harness; [HARNESS_LINEAGE.json](HARNESS_LINEAGE.json) records its identity-only adaptation.

The included datasets are exactly:

| Dataset | Targets | Purpose |
| --- | ---: | --- |
| `fixture` | 6 | Java and native/text transformation machinery, source sealing and refusal gates |
| `m3jdk21-collections-final` | 16 | Final collection production/test source from M3 baseline `e913ab89056657eca4009678cfe9a657c020e79b` |

The final dataset retains exact baseline preimages, final postimages, file modes and the ordered intermediate composition. The canonical task crate retains the historical navigation, successor and generic passes and their counterexamples. Their original preimages are intentionally not replayed against a final checkout. The public array recipe is independently packaged with the array module.

## Reproduce the SDK proof

Use Java 21, Maven 3.9 or newer, and Python 3. Run from this `recipe-custody` directory. The unchanged POM keeps the canonical default owner location, so the explicit local owner override below is required:

```sh
mvn -B -ntp -Drecipe.owner.root="$PWD/sdk-owner" -Dm3.dataset=fixture -Pcontract verify
mvn -B -ntp -Drecipe.owner.root="$PWD/sdk-owner" -Dm3.dataset=m3jdk21-collections-final -Pcontract verify
```

Set `JAVA_HOME` to the Java 21 image being tested. OpenRewrite 8.17.1, JUnit Jupiter 5.10.2 and Maven plugins are pinned in the copied POM. Add `-o` and a task-specific `-Dmaven.repo.local=/absolute/cache` for an already populated dependency cache. The source-built M3 JDK is used by the recorded owning-module acceptance gates; the portable recipe itself is a Java 21 SDK task.

Each dataset executes five actual OpenRewrite SDK tests, eight source-sealing tests, and nine isolated receiver cases. They establish exact replay, fixed point, mixed pre/postimage admission, source/result binding, input-order stability, wrong-representation/drift/missing/duplicate refusal, post-scan changes, tampering and symlink refusal, and preserved replacement file modes with unchanged timestamps on no-write/refusal paths. Fixture targets exercise the machinery and are never installed into the product checkout.

Build products and new execution reports are written beneath `target/<dataset>`. To repeat an already completed contract run, use a fresh absolute `-Dm3.contract.output=/absolute/new-proof-directory`, or clean only that dataset's generated output before rebuilding. Archived evidence log names end in `.log.txt` so the receiving text policy admits them; their raw bytes and original canonical names are recorded in the custody map.

After the final dataset's SDK proof has passed, check and replay it against an isolated M3 checkout:

```sh
python3 receive.py --dataset m3jdk21-collections-final --recipe-owner "$PWD/sdk-owner" --target /absolute/M3JDK21 --check
python3 receive.py --dataset m3jdk21-collections-final --recipe-owner "$PWD/sdk-owner" --target /absolute/M3JDK21
python3 receive.py --dataset m3jdk21-collections-final --recipe-owner "$PWD/sdk-owner" --target /absolute/M3JDK21
```

The sixteen final receiving source bytes must equal the retained actual SDK Results. Replay against the final checkout changes zero files. Unknown source bytes require a separately reviewed successor dataset and actual SDK qualification. The receiver validates all bound task inputs before serial per-file replacement; it does not claim a filesystem transaction across concurrently edited files.

## Source and proof boundaries

Only the transformation source closure, exact fixture/final datasets, compact SDK receipts, and selected proof context are copied here. The canonical commit linked from `SOURCE.json` retains the complete 248-file baseline index, twenty final postimage fingerprints, raw historical counterexamples, accepted unique Maven Java/native/API receipts, and donor evaluations. The copied compact overview covers both proving modules for provenance context; it does not install an array dataset or array production payload in this collection custody.

The collection runtime gate is separate from SDK replay. The accepted original owning module passed 214 JUnit tests in each Java and required-native mode, across 61 suites, with zero failures/errors/skips. Its sealed sources, compiled classes, native-library selection and public API proof are described in the adjacent review evidence. Earlier shared archives containing stale generated artifacts are explicitly excluded from acceptance. Reproducers for those module gates use the original source-derived module and its declared JUnit/JUnit Platform support.

Catalogue hashes are candidate-discovery signals. Structural/logic/SimHash/LSEM/DESM values alone establish neither behavioral equivalence nor donor permission. Ten existing source files have named operations evaluated in this pass; other operations remain pending in the full inventory. The final collection implementation preserves original generic membership callback demand, including quadratic repeated scans in the documented deep single-member case. Stack safety, finite runtime parity and source custody do not imply whole-JDK promotion or a general throughput improvement.

The source copies retain their Apache-2.0 license and per-file notices. [LICENSE.txt](LICENSE.txt), [NOTICE.md](NOTICE.md), and [FOSS_REUSE_DECISION.tsv](FOSS_REUSE_DECISION.tsv) preserve attribution and the distinction between reused first-party atoms and external contract/discovery references. No Synexia or OpenRewrite runtime dependency is introduced into the collection production module.
