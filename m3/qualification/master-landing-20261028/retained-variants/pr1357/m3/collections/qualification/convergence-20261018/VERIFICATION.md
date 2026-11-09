<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Verification and replay

## Source identity and scope

M3JDK21 baseline: `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.
Canonical Synexia base: `140c263a90f9803b9c46103c63355a286e0515f0`.
The four canonical engines, POM, preparation/receiver scripts, SDK test and two
contract scripts are reused unchanged. The final SDK input receipts bind these
files and each dataset. Canonical checks ran in a bounded assembly, not a full
Synexia checkout/reactor. The public mirror contains the same reusable inputs.

## Results

| Gate | Actual result |
| --- | --- |
| Strict Java 21 compile | 134 main sources; 175 test sources |
| Owning JUnit, Java-only | 245 tests, zero failures/errors, two explicit native-only skips |
| Owning JUnit, required native | 245 tests, zero failures/errors/skips |
| Source test dependencies | Ten repository source modules verified and built; no donor Java changed |
| JNI | Nine libraries / ten C files, generated headers, C17 warnings as errors, fatal UBSan, `-Xcheck:jni` |
| Native runtime contracts | 17 explicit contracts plus missing-library refusal; span/region traffic fixtures passed |
| Product packaging | 323 classes; exact class and notice inventory; runtime dependency `java.base` |
| Effective public/protected API | 313 callable members across nine public owners match baseline |
| JNI ABI | Ten generated headers byte-identical; 17 expected exported native symbols match |
| Recipe SDK | Five tests per final dataset; actual replay, mixed states, fixed point and drift/refusal checks |
| Input sealing and receiver | Eight sealing cases and nine receiver cases per final dataset |
| Navigation reconciliation | 154,239 checks, zero failures |
| Immutable indexed snapshots | 70,132 checks passed |
| Stored representatives | 2,454 assertions passed |

The API check includes inherited callable members. New explicit overrides change
reflection declaration ownership; `getDeclaredMethods()` identity is not claimed.
The API probe source is under `verification/src/main/java`.

`evidence/OWNING_MODULE_PROOF.json` describes the dirty worktree that was tested;
its `head_sha` is the baseline, not a fabricated post-publication commit.
Final manifests and SDK receipts bind the exact tested source bytes. Runtime
work/traffic assertions support the selected changes; no wall-clock speedup is claimed.

## Retained unresolved cases

`evidence/WRAPPER_MODE_COMPARISON.json` records baseline and received runs of the
same unmodified standalone `M3WrapperConvergenceContract` modes.

| Mode | Baseline | Candidate |
| --- | --- | --- |
| readonly-policy | FAIL | PASS |
| readonly-effects | PASS | PASS |
| comparator-failure | FAIL | FAIL |
| bounded-map | PASS | PASS |
| bounded-set | PASS | PASS |
| interior-null | FAIL | PASS |
| readonly-linear | FAIL: 787456 predicate calls | FAIL: 787456 predicate calls |
| readonly-failfast | FAIL: missing ConcurrentModificationException | FAIL: same |

Bounded navigation can swallow a comparator's IllegalArgumentException. A direct
neighbor rewrite can change the frozen comparator/predicate trace; classifying
exceptions by message or stack would not be a valid fix. This remains an explicit
future contract-refinement problem. The iterator demand and fail-fast modes are
also pending; the inherited indexed access behavior is preserved in this pass.
The existing JUnit wrapper deliberately runs only `readonly-policy`; the full
standalone default mode fails. These are not reported as passed or hidden by skips.

## Failures retained

The logs retain initial missing Maven dependency/proxy failures, the actual SDK
rejection of extensionless LICENSE/NOTICE targets, a donor trailing-blank lint
failure, and the first owning build's missing source-built platform launcher.
The final legal names use `.txt`; the same existing engine admits them. The
launcher is now built from its existing source module. No admission or integrity
gate was weakened. Frozen failed dataset inputs and SDK XML are retained in the
canonical crate history. An offline `-Pcontract` attempt lacked exec-maven-plugin;
the final SDK tests and exact Python contract scripts were executed explicitly.

## Patch lint

Runtime/build sources and authored review metadata pass scoped patch whitespace checks.
Raw Maven/SDK logs and exact preserved recipe preimages retain upstream trailing
whitespace. An unfiltered `git diff --check` therefore reports evidence formatting;
those bytes are not silently rewritten or treated as executable-source lint failures.

## Reproduce

From the target packet's `recipe` directory, with Java 21 and a populated Maven
cache (or network access), repeat for each final dataset listed in README.md:

```sh
mvn -Drecipe.owner.root="$PWD/sdk-owner" -Dm3.dataset=converged-owners-v2-20261018 test
python3 contract/test_prepare.py
python3 contract/verify_receiver.py --crate . --dataset converged-owners-v2-20261018 --recipe-owner sdk-owner --output target/converged-owners-v2-20261018/receiver-proof
python3 receive.py --help
```

Use `receive.py` check mode against the intended target root before requesting
application; it verifies all bound inputs and actual successful SDK XML. A full
canonical checkout uses its existing owner root instead of `sdk-owner`.

From the M3JDK21 root run the full qualification with Java 21, Maven, Python and
a Linux C17 compiler:

```sh
JAVA_HOME=/path/to/jdk21 MVN=mvn python3 m3/collections/qualification/full-java-jni-20261018/verify.py
```

Logs `43-*`, `44-*`, `45-*` and the final SDK result/input/XML records are the
publication-source recipe checks. Earlier numbered successes remain historical.
Whole OpenJDK build, jtreg, non-Linux qualification, hosted CI and the full Synexia
reactor are NOT RUN. This pass leaves the STRING receiving phase unchanged.
