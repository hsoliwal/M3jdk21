<!-- SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors -->
# Executed recipe qualification

The canonical sorting recipe passed all **20 actual OpenRewrite/JUnit tests** on
Java 21, then created the 14 sealed target files through the actual recipe results.
A separately executed read-only check found all 14 exact postimages and zero
changes. These receipts establish source application and recipe machinery. The
receiving JDK's compilation, public API behavior and runtime witnesses have their
own evidence.

## Exact identities

| Input or output | Recorded identity |
| --- | --- |
| Receiving repository base and checked revision | 2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49 |
| Existing canonical engine source revision | 140c263a90f9803b9c46103c63355a286e0515f0 |
| Named OpenRewrite recipe | com.synexia.m3.JavaUtilSorts20261018 |
| Java manifest, 8 targets | d8ebd90bce3c617b9598670f34b76ccda1a1fb4dba85075492ad0bc239ae2c8d |
| Text manifest, 6 targets | f68d13347b8b2e0298e33109200e35d685659883c3313922c7743e933caad69f |
| Deterministic target path/postimage root | 2558322dea284d16fedfe332450c1d7d32abfac3a843b153495bce6d61d69c23 |
| Qualified recipe input manifest, 51 inputs | a2ea44634f61b748d5d4d4e8b2bbb2f5956bc9752ff8ff444d2b8e7d0ecc239c |
| JDK runtime | Eclipse Adoptium 21.0.12.1+1-LTS |
| Maven / OpenRewrite | 3.9.11 / 8.90.4 |

[recipe-source-inputs.tsv](verification/recipe-source-inputs.tsv) records exact
hashes of the POM, Java recipe and test sources, frozen fixture resources,
payloads, copied engine provenance and retained source notice. The JSON
[qualification receipt](verification/recipe-qualification.json) retains each
executed test name, elapsed time, original Surefire report digest and these
source/output bindings.

The target root is SHA-256 of the sorted UTF-8 rows
`target-path<TAB>postimage-sha256<LF>`. It is an exact output identity, not a
claim about arbitrary inputs or platform behavior.

## Executed gates

| Gate | Result and scope |
| --- | --- |
| Java 21 Maven compilation | PASS; six main Java sources and two test sources; release 21, annotation processing disabled, all javac lint warnings treated as errors |
| Generic recipe replay suite | PASS; 13 tests, zero failures/errors/skips; 70.967 seconds |
| Sealed product packet suite | PASS; 7 tests, zero failures/errors/skips; 105.468 seconds |
| Complete Maven verify/package | PASS; all 20 tests; approximately 2 minutes 59 seconds |
| Actual application to pinned Git checkout | PASS; 14 new files; every original target path was absent at the pinned base; exact fixed point with zero changes |
| Independent read-only postimage check | PASS; 14 exact files, zero changes; actual checked Git revision recorded |
| Exact copied engine custody | PASS; all four original canonical Git blobs and SHA-256 digests match |
| TimSort source derivation | PASS; exact retained donor reproduced the frozen M3TimSort candidate through the read-only deterministic verifier |

The [application receipt](verification/source-application.tsv) records each
observed preimage and generated postimage. The
[check receipt](verification/source-check.tsv) records the independent zero-change
execution. Both were written outside the receiving checkout. Raw source bytes
were independently read back and compared with every sealed postimage digest.

Apply and dry-run require the original target Git base. Read-only check accepts
a published revision only when every sealed postimage already exists, source
custody validates and actual recipe replay changes nothing. It records that
checked revision separately from the original base. None of these paths promotes
the ordered runtime receiving stage.

## What the 20 tests exercised

The generic suite executes the real Recipe.run lifecycle over Java LSTs and the
existing plain-text lane. It covers absent-only source creation; every subset of
three exact postimages in both input orders; fixed-point replay; serial repetition
and bounded worker/input permutations; Java/text child-order invariance; and
preservation of unrelated source. A real Java 21 compiler and isolated executable
oracle check a frozen before/after fixture, public callable declarations, ordinary
results, empty inputs, signed overflow and the exact explicit null failure.

Code-looking comments, string literals, regex strings and text blocks remain
lossless payloads. Negative cases exercise malformed Java, wrong source
representation, duplicate paths, missing required preimages, unsorted manifests,
template/hash drift, foreign occupants and an edit introduced between scan and
visit. Refusals are checked on actual execution errors and result sets.

The packet suite activates the named YAML recipe and materializes the complete
sealed Java/text packet. It checks dry-run filesystem immutability, exact
application/readback, additive replay from a partial exact subset, repeated
fixed points, refusal before writing when any foreign occupant or symlink is
present, and a real temporary Git revision showing that check remains read-only
on a later commit while apply/dry-run retain the original-base requirement.

The finite corpus qualifies this recipe and its receiver. It does not establish
every conceivable recipe schedule or the behavior of all possible Java programs.

## Retained counterexample

The first full 20-test run exposed one refusal-classification defect. The
receiver correctly rejected a symbolic-link ancestor before writing, but a
preceding non-directory check reported the wrong failure fingerprint. The
unchanged symlink regression required the explicit symlink diagnosis.

The correction checks symlink identity before ancestor directory type. The exact
regression then passed alone, and the complete 20-test gate was rerun successfully.
The original source/report hashes, scenario, expected and observed outcomes,
correction and executed results remain in the
[counterexample receipt](verification/recipe-failures/symlink-refusal-classification.json).
No production source was applied during the failed run.

## Separate algorithm and runtime evidence

The independent [comparison-kernel audit](verification/comparison-kernel-audit/README.md)
retains its own fixture context, source digests, portable reproducer and 358,597
executed cases. It establishes the recorded isolated kernel behavior. Production
Sorts API and progress contracts are verified separately on the actual received
JDK sources.

The exact original TimSort donor, its OpenJDK notices, the deterministic
specializer and the [derivation receipt](provenance/TimSort-derivation.tsv) preserve
that source's custody. The authoring specializer verifies a candidate .java.txt
file; the canonical OpenRewrite recipe owns receiving-source materialization.

A rebuilt whole JDK image, the full platform jtreg suite, hosted CI, other
operating systems and any performance admission require their own executed
receipts. The original String-to-array-to-collection receiving order remains
unchanged. A source or contract change requires a new bounded qualification
against the affected oracle and environment.

## Actual generated java.util receiver qualification

The actual emitted six runtime sources and two test sources compile with Java 21 strict lint. All 34 algorithms passed 11,398,470 default-VM contract checks and 171,243 interpreted smoke checks. The exact existing progress owners were compiled from the pinned target source; no fixture replaces the API, kernels or progress classes in this gate. Public API output, source hashes, compiler logs, raw contract logs and the 140-row exploratory benchmark are retained in [verification/receiver-qualification](verification/receiver-qualification/README.md). See that receipt for commands, measurement scope and the remaining product-image/platform gates.
