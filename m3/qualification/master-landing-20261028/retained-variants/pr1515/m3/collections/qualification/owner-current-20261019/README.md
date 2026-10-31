# Current collection owner proof support

This directory accompanies the seven-target collection receipt on baseline `9a164c31b38c7013c179a3ff38b0bb60c199c7fd`: three existing production owners and four donor test additions. `QUALIFICATION.md` states the behavior, finite proof results, native-lane boundary and measured tradeoffs. `SOURCE_SEALS.json` is the seven-target product manifest, not a list of support files.

The saved gates passed 88 strict Java/JNI commands, 21 unchanged surrounding contracts, and the full current Maven module (139 suites, 467 tests, zero failures/errors and two existing explicit native-lane assumption skips). A separate nine-command gate used the actual integrated M3JDK21 image, the emitted external `com.m3.collections` module and real required JNI. The full optional five-library native lane is distinct from these selected JNI proofs.

## Reproduce from explicit baseline and emitted inputs

Use separate source roots. The baseline checkout must match the recorded current commit before this change. The emitted root can be a fresh canonical SDK export or the receiving checkout containing the exact seven emitted postimages. The drivers reject occupied new baseline paths and before/after source drift. No production checkout is modified: compilation, native libraries and Maven execution are isolated under new output directories.

Prerequisites match the recorded Linux qualification: Python 3, a JDK 21, GCC with UBSan, Maven, and the owning module's exact dependencies. The published pom uses the project's `5.10.2-m3-source-1` JUnit artifact; supply a Maven repository containing that artifact and the pinned plugins. Do not silently replace it with a public JUnit version. The measured vendor was Temurin 21.0.12.1; the second runtime was the recorded fastdebug M3JDK21 image.

Set task-specific paths:

```sh
m3_support=/path/to/receiving-checkout/m3/collections/qualification/owner-current-20261019
m3_baseline=/path/to/baseline-checkout-at-9a164c31
m3_emitted=/path/to/canonical-sdk-output-or-receiving-checkout
m3_jdk=/path/to/jdk-21
m3_maven=/path/to/apache-maven
m3_maven_repo=/path/to/prepared-project-maven-repository
m3_output=/path/to/new-proof-output
```

Run the strict standalone differential and required-JNI proof:

```sh
python3 "$m3_support/verify_candidates.py" \
  --repo "$m3_baseline" --candidate "$m3_support" \
  --emitted "$m3_emitted" --jdk "$m3_jdk" \
  --out "$m3_output/standalone"
```

Run the complete owning Maven module with its explicit deadline:

```sh
python3 "$m3_support/qualify_current_module.py" \
  --repo "$m3_baseline" --candidate "$m3_support" \
  --emitted "$m3_emitted" --sdk-receipt "$m3_support/SDK_EXECUTION.tsv" \
  --jdk "$m3_jdk" --maven "$m3_maven" --maven-repo "$m3_maven_repo" \
  --out "$m3_output/owning-module" --timeout 600
```

After the standalone output exists, use an independently built or qualified M3JDK21 image for the module/JNI bridge:

```sh
m3_image=/path/to/integrated-m3jdk21-image
python3 "$m3_support/qualify_integrated_native.py" \
  --runtime "$m3_output/standalone" --image "$m3_image" \
  --out "$m3_output/integrated-native"
```

That bridge resolves the emitted collection module from the explicit module path and records image/module/native hashes. It does not assert that collections was already linked into the supplied image.

## Executed evidence and portability

The two drivers that needed packaging adaptations have their exact executed originals retained under `proof/executed-drivers`. The receiving standalone driver reads the byte-identical bundled resource probe. The receiving Maven driver adds explicit Maven-repository and SDK-receipt paths; its original defaults remain available. `PORTABILITY_ADAPTERS.json` records both executed and receiving hashes. No assertion, algorithm, command selection, test selection or timeout changed, and no suite was rerun merely for packaging. Existing PASS receipts remain attributed to the executed versions.

`proof-src` contains the exact independent JDK mutation witness and existing donor resource probe. `proof/emitted-verification-1` retains the current baseline counterexamples, recorded commands, source/native custody, parity digest index and resource samples. `proof/emitted-owning-module-1` retains the complete Maven log, source snapshot receipt and exact suite/skip accounting. `proof/integrated-java-native-1` retains the actual image/module custody and selected native proof logs. `EVIDENCE_MAP.json` maps receiving copies to the original execution files; absolute paths inside historical receipts identify where those executions occurred.

The carry-forward table preserves the current upstream fresh-entry owner and stronger tests, and the exact already-present key/mask repair atoms. These historical supersessions are additive; none of the four dropped production/test targets is rewritten by this pass.

The sort resource sample cuts allocation from 26,207,984 to 13,106,384 bytes per 100 reversals over 32,750 rows. Median RSS rises from 58,832 to 59,372 KiB; no general RSS or throughput improvement is claimed. See the raw resource records and finite scope in `QUALIFICATION.md`.
