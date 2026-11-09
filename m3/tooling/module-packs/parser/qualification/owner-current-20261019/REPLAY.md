<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Replaying the parser owner qualification

`QUALIFICATION.md` distinguishes the current ordered parser-pack proof from the earlier integrated owner proof and states the measured costs. `SOURCE_SEALS.json` and the SDK receipt identify the eleven exact current targets. The adjoining `recipe-custody/` contains the original runnable canonical recipe snapshot and immutable source hashes.

The integrated runtime driver and its required sibling Java probe are exact copies of the executed proof inputs. They reuse the already checked-in `m3/tooling/shared-frontend/recipe-custody/enhancements/20261018-source-seals/runtime/qualify_runtime.py` implementation for source custody, module descriptors, hashes and native payload preservation. No source text transformation occurs in either driver.

First replay the selected SDK recipe as described by `recipe-custody/CUSTODY.md`. Apply only its exact returned eleven files to a checkout that satisfies their preimages. Then run the checked-in parser pack driver with its ordinary ordered lint, compile, test and runtime phases in a fresh proof directory. Each phase must pass before advancing; retain its STATE.json and hash-linked phase receipts. The parser module's own README/build.py describe the explicit JDK and output arguments.

For the integrated proof, retain the genuine M3JDK21 source-build checkout, its original complete JMOD estate and native Java executable. The proof validates source preimages, original module descriptors and hash coverage. A stock JDK image is not an admitted substitute for these source-built runtime inputs. Supply the existing complete ordered proof and actual SDK output to the exact receiving driver:

```sh
python3 proof-drivers/qualify_integrated.py \
  --repo /absolute/M3jdk21-receiving-checkout \
  --source-build /absolute/sealed-M3jdk21-source-build \
  --built-jmods /absolute/source-built/jmods \
  --built-java /absolute/source-built/image/bin/java \
  --tool-jdk /absolute/Java21-build-tool \
  --ordered-proof /absolute/current-ordered-proof \
  --sdk-output /absolute/actual-sdk-output \
  --out /absolute/fresh-integrated-proof
```

The source-build baseline used by the retained owner receipt originated at M3JDK21 `677437be295b23f7ccf7409be287008944d0e8b0`. It is used only where the driver verifies unchanged source equality or the exact M3ASTPC before hash; the current receiving recipe is based on `9a164c31b38c7013c179a3ff38b0bb60c199c7fd`. The old integrated parser artifact omits later upstream ingress/index additions, which have their own current ordered and real JDT ingress receipts. Preserve these scopes rather than assigning the old artifact a later source identity.

`EVIDENCE_FILES.json` records the raw copied receipt and driver hashes. Raw log/JSONL files use a `.txt` custody suffix; their contents are unchanged. Absolute temporary paths inside historical receipts are observed input locations, not promises that those transient paths exist on another machine. Reproduce into new paths with the explicit driver arguments. Fresh full JDK/HotSpot source compilation, complete jtreg/JCK and other architectures remain separate gates.
