<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK 21 — 21.0.14 m3.1 technical preview candidate

**Preparation status: HOLD for release admission.** These notes describe the
candidate and its test scope; they do not announce a published release.

Tag: `m3jdk-21.0.14-m3.1-preview` · Branch: `release/m3jdk-21.0.14-m3.1-preview` ·
Tier: `P0_TECHNICAL_PREVIEW` ([release contract](release-contract.json)).

## Source and naming identity

| Item | Preparation identity |
| --- | --- |
| Product | M3JDK 21; machine ID `M3JDK21` |
| Inspected repository | `hsoliwal/M3jdk21` |
| Inspected master baseline | `b9cc756f726de31af10652b5583d9bba58ba27b2` |
| Baseline tree | `9b5fa265be790996a7ed665992b618f642f6f454` |
| Configured version | `21.0.14` in `make/conf/version-numbers.conf` |
| Contract upstream baseline | `openjdk/jdk21` at `890adb6410dab4606a4f26a942aed02fb2f55387` |
| Final receiving/release commit | Must be bound separately by the executed receiving and release evidence; the inspected baseline is not that final commit |

The verifier now checks the display name, version, preview tag and release
branch against the actual version configuration. Naming metadata follows the
current `jdk.m3.*` source packages and module descriptors. Standard JDK names,
JNI surfaces and the exact compatibility alias `Object.toAdpater(Class<T>)`
remain unchanged, alongside `Object.toAdapter(Class<T>)`.

The same canonical naming map retains the original stable capability IDs,
prior source hashes and target lineage. It restores the historically recorded
`synexia.lexicons.related-source-range` capability with its original
`implemented-unverified` status. Four queued naming candidates remain queued.
The [passive naming receipt](../tooling/name-contract/receipts/release-preparation/source-identity.json)
records the inspected source and restoration provenance. Naming presence is not
runtime qualification.

## Preparation changes and regression scope

| Area | Preparation change | What the tests establish |
| --- | --- | --- |
| Current naming | Exact `jdk.m3.` package/module acceptance; stale source map symbols corrected using inspected declarations | Known owner accepted; similar/arbitrary namespaces, comment-only declarations and unsafe companion boundaries refused |
| Mapping schema/history | Required signal metadata normalized; original stable-ID fixtures restored; historical related-range record preserved | Full authority validates without dropping schema requirements or renaming historical capability IDs |
| Arrays lineage | Current Arrays source pinned; two exact historical patch reversals retained as test data | Source-chain and patch mutations refused; no repository Java source is transformed by this test |
| Release identity | Display name, configured version, preview tag and branch validated together | Version drift, missing/duplicate version fields and mismatched preview identity refused |
| Evidence ownership | Canonical recipe execution belongs to Synexia; target acceptance remains M3JDK21-owned | Wrong owner, stale/malformed receipt, artifact-path and hash failures remain refused |
| Packaging and dependency checks | Selected build artifacts and the narrow passive receipt exception tested | Missing/extra platform artifact and executable external dependency paths are not silently admitted |

These are target-side **release-tooling regression tests**, not product code
coverage percentages. They execute fixtures and read pinned source. The Java API guard additionally
executes synthetic module fixtures on stock Java 21. These checks do not build
the JDK, execute a native product library, run the full jtreg suite, or
qualify any receiving phase. Canonical recipe execution is a distinct receipt.

## Current-source coverage and admission matrix

The scope below applies to this preparation. Older successful or failed runs
retain their original source pins and cannot fill these current-source rows.

| Surface | Required producer / evidence | Preparation disposition |
| --- | --- | --- |
| Naming and canonical target census | `name-contract/check.py`, `audit.py --strict`, and all `test_*.py` in that directory | Executed on the candidate text/source snapshot; no installed-image claim |
| Release tooling | All `m3/release/test_*.py` | Executed on candidate tooling and isolated fixture repositories |
| Canonical transformation | Existing Synexia source-sealed recipe and real SDK replay/fixed-point/refusal tests | Separate execution receipt required for final receiving bytes |
| Immutable text foundation | `bash ./m3/build.sh` at the exact final candidate | NOT RUN by this preparation; current-source receipt required |
| Precompute Java parity | `mvn -B -ntp -f m3/ports/precompute/pom.xml test` | NOT RUN by this preparation; current-source receipt required |
| Precompute JNI/native parity | Same POM with `-Pnative test`, matched OS/architecture/toolchain | NOT RUN by this preparation; platform-specific receipt required |
| String UTF-16 contract | Source-built M3JDK image, affected String/jtreg/VM tests | PENDING; no dedicated first-release workflow producer bound to `STRING_UTF16_CONTRACT` |
| API guard mechanism | Existing Java 21 patched-module workflow fixtures; current/legacy namespace self-tests | Executed: one positive, two deliberate internality violations refused; no product-image claim |
| Public API/module boundary | `make run-test TEST=test/jdk/java/lang/String/M3CanonicalApiBoundaryTest.java` on the matching image | NOT RUN by this preparation; synthetic stock-JDK fixtures do not satisfy it |
| SHA-256 Object adapter/JNI | `test/jdk/java/lang/Object/SHA256ObjectAdapterTest.java` and its native library on the matching image | NOT RUN by this preparation; naming inspection preserves both adapter spellings |
| Linux x64, Windows x64, macOS x64 and macOS aarch64 bundles | Selected `m3-release.yml` build jobs and exact-run artifacts; OpenJDK sanity evidence | PENDING for final candidate; a packaging test is not a platform build |
| Other architecture/mobile claims | Explicit source-built runtime and target execution receipts for each claimed platform | No qualification asserted by this preparation |
| Performance | Same-host baseline/candidate measurements with raw CPU/heap/native-memory results | NOT RUN; no superiority claim |

## Claims and mandatory gates

`immutable_text_foundation` remains
`IMPLEMENTED_WITH_REPOSITORY_TEST_SURFACE`. Full String backing, full regex
precompute, full jtreg compatibility, performance superiority, qualified arrays,
qualified collections and SWT/Eclipse integration remain `NOT_CLAIMED`.
Optional TornadoVM remains conditional on its exact matched-image gates.

All ten `mandatory_release_gates` need PASS receipts for the final commit and
tree: rights/license; source/upstream; OpenJDK sanity; foundation; Java parity;
JNI/native parity; canonical Maven recipe execution; String UTF-16; release
claims; and proof-pack SHA-256. The verifier checks artifact identity and
manifest structure; reviewers must confirm the command's contract coverage.
A gate's existence or unit test is not its execution evidence.

Release remains on HOLD until every gate, source cleanliness, retained-source
review, standalone dependency boundary and legacy-recipe custody requirement
passes. The existing single-fork-commit history requirement is preserved and
must be resolved through explicit policy/ancestry review, not history rewriting
performed by this preparation. No pending-gate publication is admitted here.

## Rights and historical evidence

First-party M3/Synexia expression retains Apache-2.0 and
Copyright 2026 Hitesh Soliwal and contributors. See
[LICENSE-M3-APACHE-2.0.txt](../../LICENSE-M3-APACHE-2.0.txt),
[M3-SYNEXIA-NOTICE.md](../../M3-SYNEXIA-NOTICE.md), and
[M3-SYNEXIA-RIGHTS.md](../../M3-SYNEXIA-RIGHTS.md). OpenJDK-derived source and
patch data retain the applicable GPL-2.0/Classpath Exception terms; recipe
licensing does not relicense their output.

Dated audit and queue receipts remain historical evidence at their recorded
source revisions. Old PR numbers, queued-run counts, platform failures and
secret setup instructions are not current release prerequisites. The
[release checklist](RELEASE_CHECKLIST.md) defines current preparation and
admission steps without assuming those historical observations still apply.
