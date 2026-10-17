<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->

# Source-bound String contract

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

`verify_sdk_results.py` qualifies the exact fifteen-target `m3-rope-affix-effects-20261017` recipe packet against actual retained OpenRewrite SDK Results. It reads the supplied target and canonical recipe crate; all runtime staging, classes, native libraries and evidence go into a new output directory. It does not write either input checkout.

## Invocation

Run the crate's five Maven tests after installing this complete `contract/` directory and its receiver. Those tests must emit current `target/candidate/SDK_RESULTS.tsv`, `SDK_INPUTS.tsv` and the Surefire XML report. The source binding includes `pom.xml`, `receive.py`, every file under `src/` and `contract/`, and the four reused canonical recipe owner classes.

```sh
JAVA_HOME=/absolute/path/to/jdk21 \
python3 /absolute/path/to/m3-rope-affix-effects-20261017/contract/verify_sdk_results.py \
  --target /absolute/path/to/M3jdk21 \
  --crate /absolute/path/to/m3-rope-affix-effects-20261017 \
  --output /absolute/path/to/fresh-string-proof
```

The target may contain the exact sealed preimages or postimages. Its unchanged dependencies must match `parent-inputs.tsv`: 58 files and 19 required absences from reviewed baseline `3bbb4712fb20adba1405741db8457164b9f436f5`. The historical rolling proof retains its original Git-based baseline reader. The target therefore needs commit `970a34350ebdf6a3f1c5ff5acc8445d0f3515f55` and its sealed regex source blob. The gate verifies that blob before execution and supplies the target's Git metadata read-only while the proof reads current source from the isolated overlay. No network access or historical checkout is performed.

Linux, Python 3.9 or later, Git, stock JDK21, a C17 compiler, and the compiler's undefined-behavior sanitizer runtime are required. The prepared CPU/JNI gate uses `CC` when supplied. CUDA headers, toolkit and a GPU are not required for this contract; only the distinct optional CUDA JNI adapter is compiled against its small interface header.

## Admission and execution

Admission verifies the frozen six-target Java manifest and nine-target text manifest, each before/after payload, the target state, every actual SDK output byte, the exact SDK Results receipt, all five named passing Java21 tests, and the current SDK input binding. The executing gate and parent inventory must also be identical to their SDK-bound copies. Missing history, source drift, a template-only directory, stale test results, or a stale contract binding refuses before an output directory is created.

The sixteen commands cover the following distinct obligations:

| Commands | Contract |
| --- | --- |
| Complete affix before/after proof | Actual complete DP classes, cursor and fact mixers; Pattern parity; bounded near misses; exact root-cache identity; resource bound; current DP jtreg compilation. The canonical owner is an explicitly identified fixture. |
| Three retained source seals | Amalgamation, required grams, and rolling cursor successor identities and preserved matching/native seams. |
| Retained amalgamation and rolling before/after proofs | Complete historical/current DP and isolated historical/current rolling behavior, with the original baseline bytes preserved. |
| Retained row precompute proof | Its independently sealed source and existing stock-JDK21 patch-module contract. |
| Literal, regex split and codepoint source gates | The unchanged Pattern fallback and UTF-16 representation routes. |
| Retained native UTF-16 oracle | Its actual native bridge source, strict C compilation, tiered and interpreted `-Xcheck:jni` execution on stock JDK21. |
| Ledger refusal suite | Nine tests preserving existing hold, advisory, scope, ownership and incomplete-row refusal rules. |
| Strict retained jtreg compilation | Eleven `M3StringRegex*` sources and the existing `M3StringHierarchicalRegexTest`, twelve sources in total, under `--release 21 -Xlint:all -Werror`. The replacement test remains a separate full-product/test-library obligation. |
| Prepared Effects CPU/JNI gate | Java fallback, strict native execution, portable interpreted native execution, UBSan, injected native allocation failure, and optional CUDA JNI adapter compilation. It consumes generated Java, C and test sources from the overlay. |
| Baseline and candidate full String source invariant | Both must reproduce the exact known parent failure below. Any different outcome fails this packet's declared baseline contract. |

The outer gate calls the preserved prepared-batch support layout as follows:

```sh
JAVA_HOME=/absolute/path/to/jdk21 \
bash "$CRATE/contract/batch/qualify.sh" \
  --target "$OUTPUT/overlay" --crate "$CRATE" \
  --output "$OUTPUT/prepared-effects"
```

That nested gate places its exact isolated owner source under `prepared-effects/source/`, generated JNI headers under `prepared-effects/headers/`, module classes under `prepared-effects/classes/`, tests under `prepared-effects/tests/`, and native libraries and execution logs directly under `prepared-effects/`. The C allocation-failure probe includes the actual generated target C file selected by `--target`.

## Results and explicit baseline hold

Successful focused qualification returns exit code zero and records `PASS_WITH_BASELINE_HOLD`, not full String acceptance. The unchanged parent source invariant fails with this exact diagnostic:

```text
M3_STRING_INVARIANT_FAIL: M3 adaptive BMH/KMP convergence missing: skip256[pattern.charAt(index) & 255] = length - 1 - index;
```

The gate executes the original checker on a baseline assembled from sealed parent files and preimages, then on the generated overlay. It preserves both failing exit codes and complete logs, requires the diagnostic to match exactly, and records the same unresolved hold in the runtime receipt. No checker requirement is removed, weakened or silently counted as passing. The focused affix, existing integration and prepared Java/JNI checks must still pass independently.

`evidence/SDK_RUNTIME_RECEIPT.json` contains command exits and acceptance states, exact source hashes, the source-bound SDK receipt, parent identities, and the explicit hold. `evidence/` also retains all stdout/stderr logs, the five-test report, the SDK input/output inventories, and the historical rolling source. Before success, the gate rechecks generated output, parent source, target input, historical source and SDK evidence stability. A command failure or detected drift records `FAIL` and preserves the output for diagnosis.

Each command runs in a private process group. Timeout or interrupted execution terminates the whole group, including Java descendants launched by retained Python or shell proofs. The timeout receipt preserves partial output and the signals used. The active amalgamation successor retains its original 36-star missing-Q witness, proves the required literal is absent, and checks the equivalent collapsed `a*Q.*` language with Pattern. Normal cases still use their original Pattern expressions; no timeout is waived or increased.

This is a component source and stock-JDK21 JNI contract. Matched M3JDK/HotSpot execution, full product jtreg with M3 storage and CompactStrings variants, whole-product resource and throughput measurements, CUDA kernel/device qualification, and full String-stage completion remain separate gates. No later family stage or full-borrow status is advanced.
