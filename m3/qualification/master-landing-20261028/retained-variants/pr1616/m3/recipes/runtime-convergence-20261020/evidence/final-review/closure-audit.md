<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Qualification and SDK evidence closure audit — 2026-10-20

## Result

No new production or qualification-script finding under the agreed exclusive source-writer assumption. The existing contract supports the newer upstream collection dependency inventory. Fresh receiving and full contract execution on the preserved current base remain required; this audit did not run acceptance tests.

Reviewed packet: `/workspace/scratch/53bcd62c244c/canonical-runtime-convergence-20261020`.
Observed receiving checkout: `/workspace/scratch/53bcd62c244c/M3JDK21`, HEAD `2779aba3f98f31c324be43f731be7914e1a05935`.

## Exact SDK evidence checks

- Both source manifests validate every retained preimage and postimage digest: **19 Java targets and 10 text/native targets**.
- `evidence/sdk-execution/generated/SDK_INPUTS.tsv` contains **55 inputs**, all matching the current packet and its four pinned `sdk-owner` sources. The corresponding `target/candidate/SDK_INPUTS.tsv` also matches all 55 inputs.
- Both retained and current `SDK_RESULTS.tsv` files contain **29 results** whose generated files match their recorded postimage digests.
- Retained and current Surefire XML each record **five executed tests, zero failures, zero errors, and zero skipped tests**. The five named cases cover refusal, result retention, post-scan refusal, mixed states, and exact replay/fixed point/input order.
- `evidence/sdk-execution/maven.log` records `clean`, fresh compilation of four engine sources and one test source, five passing tests, and `BUILD SUCCESS`.
- At inspection, all 29 receiving paths still matched the admitted preimage/ABSENT state on HEAD `2779aba…`. The change in upstream base therefore does not invalidate the recorded transformation inputs by itself.

## Support for current upstream dependencies

The observed pre-receive collection inventory contains **270 main Java files and 259 test Java files**. These are observations of the current tree before this packet's additional test files are received, not final qualification counts.

The owning qualifier payload `004-verify.py.txt` cleans the collection build (line 75), discovers all main and test Java files with `rglob` (lines 95–99), compiles the complete main inventory, and runs the owning Maven suite in Java fallback and native-required modes. It does not hard-code the historical collection source or JUnit counts. Its final `PROOF.json` records the discovered source counts, actual JUnit totals, current HEAD, source hashes, JAR identity, and native library inventory (lines 194–203).

The current collection POM has no test exclusions and is unchanged from the original witness base. `M3ProgressMonitor` now calls `M3ShadowStreams`, and the new owner is inside the discovered main source tree. Its dependencies are in `java.base`; no additional external runtime dependency is needed by this change. The same whole-module build includes the newer `M3ArrayList` and `M3LinkedHashMap`.

`contract/verify.py` invokes that full qualifier, installs the tested collection artifact, then runs fresh `clean verify` builds of the two Eclipse owners (lines 67–89). Composing the packet's postimages onto HEAD `2779aba…` yields exactly the two-file core-runtime and ten-file framework Java inventories in the successor source seals, with no missing paths or digest mismatches. No fixed dependency source list needs expansion for the identified upstream changes.

## Reporting and completion notes

1. Preserve `d6d76b563050fc13c4fa79bc494edf29f1614b37` as the original witness/preimage provenance. The README still labels it simply as the receiving base; final receiving evidence should explicitly identify preserved base `2779aba3f98f31c324be43f731be7914e1a05935` and the actual resulting commit when available.
2. The existing exact SDK evidence proves the transformation inputs and outputs. Historical focused runtime logs do not establish compatibility with the newer collection dependencies. Use the fresh full contract outcome for that claim and retain its owning collection `PROOF.json` and reports together with the outer contract receipt.
3. Full built M3JDK21-image, jtreg, whole-platform, and phase-promotion claims remain outside the declared contract scope, as the current script and README already state.

## Method and limits

Read-only file inspection, digest comparisons, XML/JSON parsing, source inventory checks, and a bounded Git diff. No Python source imports, heavy test reruns, canonical-packet edits, receiving-tree edits, or remote writes were performed. This file is the only audit artifact written.
