<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Nebula selection occurrence recovery

This packet repairs the existing parent-owned `GridSelectionList` while preserving duplicate selection order and the original Grid widget. It binds three exact target paths at Nebula commit `91c474935eb4a06e46716a0188b9082f0af0eda9`: the selection owner, a new native regression test, and the existing test POM with two added default test includes. The POM change also admits the prior `GridSelectionList_Test` into the ordinary target test selection.

The recipe retains the existing compact primitive selection lane and membership mask. It tracks repeated occurrences so removing one occurrence cannot clear membership while another selected occurrence remains. Iteration is based on logical list indices and remains consistent when a read compacts dead storage. The implementation stays inside the original Grid-owned selection object; no viewer, Control, native row model, public widget API, or runtime dependency is added.

## Executed qualification

`evidence/sdk` retains actual Maven/OpenRewrite SDK results: five JUnit methods pass with no failure, error, or skip. The existing shared tests execute the named YAML recipe, prove byte-exact Java LST and text emission, replay to a no-change fixed point, change input order, mix pre/postimages, and reject missing, duplicate, drifted, incorrectly represented and post-scan changed sources. Code-shaped decoy text remains unchanged.

The receiving adapter was applied to the real Nebula checkout: three changed paths, followed by zero changes on the second check. The new native test alone gives eight executed methods and seven failures on the untouched baseline; the corrected suite, including the three prior selection tests, passes all eleven methods. A seeded 20,000-operation duplicate-inclusive differential compares the original owner against `ArrayList`. The actual GTK Display capture mismatch of 8,021 pixels before the repair becomes zero after the repair. Logs and persisted PNGs are in `evidence/target`.

The initial SDK trial rejected a manifest sorted by Python Path components rather than complete source path text. The packet assembly was corrected to use the same full-path lexical order as the canonical recipe. Its five failing test records and exact manifest remain under `evidence/refinements/manifest-order`; the existing SDK test continues to reject that malformed input. No target source was published from the failed trial.

## Reproduce

From this crate, with Java 21 and an approved Maven cache:

```sh
mvn -B -o -ntp verify
python3 receive.py --target /absolute/path/to/nebula --check
python3 receive.py --target /absolute/path/to/nebula
python3 receive.py --target /absolute/path/to/nebula --check
```

The receiver uses only the retained **OpenRewrite SDK Results**, verifies the executed test report and source/input/output digests, admits all target paths before writing, and writes each file serially. An unknown source revision or changed template requires a new reviewed packet. The exact final target paths and SHA-256/Git blob identities are in `crate.json` and the lane manifests. Native tests remain the target's authority; the SDK test is not a native widget test.

## Custody and limits

The canonical engine is `M3HashPinnedJavaSnapshotRecipe` plus `M3HashPinnedTextSnapshotRecipe` in `hsoliwal/com.synexia`, pinned to `892e3f6c51d78bc0948e8a8cf736055b4c70eafa`. This revision identifies reused engines and the existing SDK harness/receiver; the new recipe packet itself is identified by the containing new commit. OpenRewrite 8.17.1, Java 21, Maven 3.9.11 and JUnit Jupiter 5.10.2 were used for SDK qualification; target native proof uses real JUnit 4.13.2 and GTK.

Synexia recipe/proof code retains Apache-2.0. SWT/Nebula input, postimage and test payloads retain their individual Eclipse notices and EPL-2.0 rights. The recipe does not relicense them. This is routine correction of an already-public list/selection contract; no unpublished invention-specific runtime is introduced.

The evidence covers one selection owner and its actual GTK behavior. Full Nebula/Tycho reactor, Win32/Cocoa, all widgets, complete accessibility/IME/DPI coverage and a controlled retained-heap/native-memory performance campaign remain separate gates. Recorded process timing/RSS describes this finite test invocation and does not establish a viewport performance claim. Keep target promotion gated on its required build and platform checks.
