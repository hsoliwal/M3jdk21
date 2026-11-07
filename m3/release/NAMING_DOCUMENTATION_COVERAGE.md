<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 release preparation: names, documents and test coverage

Status: **preparation / not a release admission receipt** (2026-11-07).
Product: `M3JDK21`, Java 21 compatibility target.
Current preview identifier: `m3jdk-21.0.14-m3.1-preview`.
The release name is a candidate; no tag or product-image qualification is asserted here.

## Naming freeze

Keep public Java 21 owners in their original packages and names (`java.lang.String`,
`java.util.*`, `java.util.regex.*`), with source/binary/behavior compatibility
in the M3JDK21 direction. Internal M3 owner names are distinct from public Java
APIs. Never rename a public type into an `M3*` facade; never rename an existing
JNI symbol or serialized/reflected name mechanically.

The existing authority is [name-mapping.json](../docs/name-mapping.json) and
[the name contract](../docs/M3JDK21_NAME_AND_PACKAGE_CONTRACT.md); the existing
name-plan is a *queue*, not an admission receipt. Run:

```sh
python3 m3/tooling/name-contract/check.py
python3 m3/tooling/name-contract/audit.py --strict
python3 -m unittest discover -s m3/release -p 'test_*.py' -v
```

Do not bulk-reformat `src/**` during release hardening. Any approved internal
rename must have an exact mapping ID, donor/rights evidence, deterministic
recipe execution and binary/JNI/reflection/serialization compatibility proof.
Naming transformations and their recipe custody must follow the repository
policy resolved at review time; there is a current conflict between `AGENTS.md`
(Synexia-only authoring/execution) and the standalone release contract
(M3JDK21-local recipe execution). This **blocks sign-off**, not just documentation.
The historical private-repository token is not a substitute for the owner decision.

## Documentation freeze

Review [the claims contract](release-contract.json), [release notes](RELEASE_NOTES.md),
[publication checklist](RELEASE_CHECKLIST.md), upstream OpenJDK version, licensing
and Synexia NOTICE on the **same release commit**. Avoid describing queued
adapters/companions as installed-JDK features. Separate P0 foundation tests
from proof of an integrated `java.lang.String` replacement.

Verify all branch/tag references against the contract; `test_release_metadata.py`
now guards this drift. Re-pin all examples, source links, generated reports and
SHA-256 evidence to the final product commit after the last merge.

## Coverage qualification: do not substitute test counts for code coverage

| Area | Existing test surface | Required release evidence |
| --- | --- | --- |
| Naming/receipts | `m3/release/test_*.py`, name-contract tests | executed Python checks, zero failures, source-bound logs |
| M3 text foundation | `m3/build.sh` | JDK 21 test executions including `-Xint`, C2 and UTF-16/unpaired surrogate cases |
| Java precompute | `m3/ports/precompute` Maven tests | differential result, corpus/case count, failures and raw logs |
| JNI/native | precompute `-Pnative`, checked JNI receivers | compiled native build, parity/lifetime tests on matched images and OS/arch |
| JDK public behavior | OpenJDK sanity workflows, jtreg tests | tested **M3JDK21 image**, jtreg group/case counts, fails, skips, regression diffs |
| Arrays and collections | module tests and independent provider-contract tests | contract-by-contract JUnit, mutations/views/concurrency/serialization and ABI |
| Swing/SWT coupling | separate SWT backend matrix | exact SWT revision, matched Java/native artifacts, GUI, resource and JNI evidence |

For each contract-bearing **atom**, record its source file/method, behavior
branches, matching tests, error/null/overflow/concurrency boundaries, baseline
oracle and raw result. A 90%+ line/branch **engineering target** is not an achieved
coverage statistic. Aim for 99% in safety-critical atoms, but first collect
instrumented line **and branch** reports (JCov/Jacoco as applicable), with total
covered/missed counts and explicit exclusions. Treat unavailable instrumentation
as `PENDING`, never as 0% or 100%. Count unique tests, skips and disabled tests
separately. For JDK modules, do not substitute library-JAR test coverage for
a source-built installed-image/jtreg run.

## Release refusal conditions

No publication-ready assertion until the independent product build, ABI/source
compatibility, platform/JNI/jtreg gates, no-loss history audit, legal notices
and source-bound artifact SHA-256 receipts pass. `policy_valid` does not mean
`release_ready`; use the existing verifier:

```sh
python3 m3/release/verify_release_admission.py --root . --release \
  --evidence /absolute/path/to/release-evidence.json \
  --json-out admission-release.json
```

A failing or absent gate is recorded as **PENDING/HOLD** and stays in the queue.
The [SWT release-readiness pack](https://github.com/hsoliwal/eclipse.platform.swt/tree/master/docs)
is a separate downstream qualification and never upgrades an M3JDK runtime claim.
