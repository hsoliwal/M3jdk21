<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Portable wrapper recipe contract

Place `contract/verify_sdk_results.py`, `contract/parent-inputs.tsv`, and `contract/MANIFEST_SEALS.json` beside the existing recipe crate POM. The root-owned POM can include the profile in `contract-profile.xml` under its existing profiles element. This uses the existing Maven/OpenRewrite recipe and the standard exec-maven-plugin verification phase; it introduces no workflow or alternate recipe engine.

From the canonical private recipe crate, run:

```sh
JAVA_HOME=/absolute/path/to/jdk-21 mvn -B -ntp -Pcontract clean verify \
  -Dm3.contract.target=/absolute/path/to/M3jdk21
```

The default canonical `recipe.owner.root` continues to identify the four existing Synexia SDK owners. A target mirror supplies its source-pinned SDK owner directory using `-Drecipe.owner.root=/absolute/path/to/recipe/sdk-owner`. Maven dependency settings and local repository overrides remain ordinary Maven options. `m3.contract.python` defaults to `python3`; `m3.contract.output` defaults to `target/contract-proof`.

An already qualified crate can also invoke the same runner directly:

```sh
JAVA_HOME=/absolute/path/to/jdk-21 python3 contract/verify_sdk_results.py \
  --target /absolute/path/to/M3jdk21 \
  --crate /absolute/path/to/recipe-crate \
  --output /absolute/path/to/a-new-proof-directory
```

The output directory must be fresh. A repeated Maven verification can use `clean verify` or a new `-Dm3.contract.output=...`; the runner never overwrites earlier evidence. The retained native and collector gates require Linux, JDK 21, Python, and the shell/native compiler tools already required by the historical wrapper gates. Set JAVA_HOME explicitly; the runner checks the compiler major version before making its output.

## Admission and evidence

Admission is read-only. It verifies 34 exact required parent inputs from baseline 10ce8abaa279292c73e1c118628344cf0851d14a. Each target must be its sealed preimage or postimage; an absent target is accepted only where that recipe row specifies ABSENT. This permits the intended partially converged per-file states while refusing unrelated source drift.

The five Java rows remain sealed by their original manifest hash. The text manifest may contain the four existing rows plus exactly the existing `m3/docs/name-mapping.json` owner. Removing that optional metadata row must reproduce the original text manifest hash. Therefore changing the seal file cannot admit additional target paths or different source outputs among the original nine. The current manifest seal file records five Java rows and five text rows.

The runner compares every actual SDK candidate byte-for-byte with its recipe postimage, checks the exact SDK_RESULTS ledger and five-test Surefire result, and requires SDK_INPUTS to bind this exact source set: crate POM, receive.py, all regular src files, all regular contract files, and the four named source SDK owners. The owner location comes from the actual Surefire report. Missing, duplicated, extra, or changed binding rows require a fresh Maven qualification.

After admission, the runner constructs a minimal proof overlay from those retained parent inputs, recipe preimages/resources, and actual SDK outputs. It preserves original public source snapshots and before/after javap descriptors for three owners. The 16 unchanged command gates cover strict Java 21 compilation, deep tiered and interpreter filtering, the old owner overflow control, all three public ABI comparisons, exact gate-successor refusals, the retained List/native/five-collector proof, and writable Map AVL behavior. The generated SDK output bytes and original target input states are rechecked after execution. No target source files are written.

The evidence directory retains command stdout/stderr, SDK Results, SDK input bindings, Surefire report, seal files, and a machine-readable receipt. Temporary compilation output is removed after success. The source overlay and original source snapshots remain available to reproduce the ABI and runtime checks.

## Current handoff result

The first portable Maven run passed five SDK tests and all 16 wrapper gates. Its receipt is `portable-maven-proof/target/contract-proof/evidence/SDK_RUNTIME_RECEIPT.json`, and its full Maven log is `portable-maven.log` in the task handoff directory. This run used the original nine-output recipe and narrower SDK input schema. It is retained as actual execution evidence, not relabeled as the later ten-output schema.

`PORTABLE_HANDOFF_CHECK.json` confirms the final five-plus-five manifest seals, the unchanged original nine recipe rows, and refusal of that prior narrower SDK binding. Root performs the final canonical Maven run after integrating the mapping successor, final binding schema, profile and runner. That final execution is the gate before target materialization.

The private repository remains the concept convergence workspace before JDK or SWT promotion. The unrelated frozen #992 umbrella inventory already held on the #1031 parent and remains separately identified; the runner qualifies only the two exact historical gate successors required by this change.

