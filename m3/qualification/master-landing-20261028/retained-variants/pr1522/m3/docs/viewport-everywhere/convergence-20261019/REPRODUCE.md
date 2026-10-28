<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Reproduce the published collections qualification

Canonical source is [Synexia PR #12528](https://github.com/hsoliwal/com.synexia/pull/12528)
at commit `74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd`.
The published executions qualify the selected payload derived from M3JDK21
`9a164c31b38c7013c179a3ff38b0bb60c199c7fd`, with current manifest
`5bb24c679cea5b4d6daddd870323401466419f90ad105a845efe94352e708819`.

These are portable documented commands. They were not executed during this
metadata recovery, and no new reproduction launcher was created. Any later
source changes need their own source admission and appropriate acceptance.

## Exact support-file mapping

The current 60-target recipe does not install these two original qualification
support files. Receive their unchanged canonical bodies in the M3 PR or its
explicit stack; do not rely on the still-open #1480 being implicitly present.

| Receiving target in M3JDK21 | Canonical template at the pinned commit |
| --- | --- |
| `m3/collections/qualification/collection-refinement-20261019/verify.py` | [`synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3-collection-refinement-20261019/templates/verify.py.txt`][verify] |
| `m3/collections/qualification/collection-refinement-20261019/probe-plan.json` | [`synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3-collection-refinement-20261019/templates/probe-plan.json.txt`][plan] |

Exact SHA-256 identities:

- `verify.py` — `fd40d196efbb462243eba6b43d9e78ebc4162c9e2b0f13beeafac804ae8c228e`.
- `probe-plan.json` — `91b07ee6bec984b2229eaaa1041662e4d93a72a3e222b5111277d6c5da9e7a39`.
- Canonical [`verify_all_native.py`][native-helper] — `c7d9612fce125813980ce31f7566f0aab28045d95897b6c60690544558c80dfe`.

The native helper stays at
`synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/proof/verify_all_native.py`.
It imports the unchanged receiving verifier's existing command, inventory,
receipt and native-declaration owners. The receiving verifier uses Python's
standard library and the explicit probe plan; it does not import
`verify_donation.py` or read `runtime-manifest.tsv`.

## Prerequisites

Use Linux x86_64, Python 3, a complete JDK 21 with JNI headers and `libjvm`,
Maven, GCC and `nm`. Provision the original module dependency/build-plugin
closure for an offline run. Preserve the POM and
`org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1` coordinate.
[EVIDENCE.json](EVIDENCE.json) records all eight test JARs actually loaded.
It does not enumerate the entire transitive Maven cache.

The independent console is `junit-platform-console-standalone-1.12.2.jar`,
SHA-256 `329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a`.
The original Maven plugins are resources 3.3.1, compiler 3.13.0 and surefire 3.2.5.
Keep all seven original POM resources and the complete current main/test sources.
The selected native fixture
`m3/collections/src/test/native/m3_mask_empty_regions.c` and missing frozen
proof classes are already current recipe outputs.

The unchanged probe plan contains five Java, three absent-library and eleven
native executions across fifteen unique proof mains. Its exact class/argument/
expected-output contract is in the pinned plan. Do not replace dependencies,
edit the POM or remove tests to fill gaps in a reproduction environment.

## Complete module and native commands

Replace the example paths. Use a fresh output outside both source checkouts.
The canonical checkout must be at the published commit shown above, and the
receiving module must match the admitted payload before these commands run.

```bash
set -eu
m3_source=/path/to/received-M3JDK21
m3_canonical=/path/to/com.synexia
m3_jdk=/path/to/jdk-21
m3_maven_repository=/path/to/original-m3-maven-repository
m3_junit_console=/path/to/junit-platform-console-standalone-1.12.2.jar
m3_output=/path/to/fresh-collections-qualification

test "$(git -C "$m3_canonical" rev-parse HEAD)" = 74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd
mkdir "$m3_output"

python3 "$m3_source/m3/collections/qualification/collection-refinement-20261019/verify.py" \
  --source-root "$m3_source" \
  --probe-plan "$m3_source/m3/collections/qualification/collection-refinement-20261019/probe-plan.json" \
  --java-home "$m3_jdk" \
  --maven-repository "$m3_maven_repository" \
  --junit-console "$m3_junit_console" \
  --offline --ubsan --output "$m3_output/full"

python3 "$m3_canonical/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/proof/verify_all_native.py" \
  --verifier "$m3_source/m3/collections/qualification/collection-refinement-20261019/verify.py" \
  --compiled-root "$m3_output/full" \
  --java-home "$m3_jdk" \
  --junit-console "$m3_junit_console" \
  --output "$m3_output/all-native"
```

The second command requires the first receipt to pass and uses its complete
classes, tests, generated headers and source snapshot. It builds ten existing
libraries with fatal UBSan and enables all four existing opt-ins:
`m3.mask.spans.native.required`, `m3.collections.all.native.required`,
`m3.collections.rank.native` and `m3.collections.signal.native`, with
`-Xcheck:jni`. It retains source/class/header identities and rejects native
warnings and assumption aborts.

The published [combined receipt][combined] contains the seven additional
projection/window/tree commands, exact expected lines and baseline API/header
comparisons. Reproducing that lane requires its own fresh output and matching
source/class/native identities; the module and all-native commands above do not
implicitly rerun it.

## Separate canonical SDK and kernel acceptance

The actual SDK uses the existing scoped POM
`synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/current-master/pom.xml`
and its original SDK dependency closure:

```bash
mvn -B -ntp -o \
  -s /path/to/original-maven-settings.xml \
  -Dmaven.repo.local=/path/to/original-sdk-maven-repository \
  -f "$m3_canonical/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/current-master/pom.xml" \
  test
```

The [SDK receipt][sdk] records three passing PlainText tests; JavaLST and the
whole reactor were not run. The separate [kernel log][kernel] records 7,734
bounded checks with `exhaustiveSubsets=0`; that result is not SDK execution.
The original driver and immutable preimages stay in the canonical crate.

New commands write new evidence. They do not overwrite or relabel the published
9a164c31 receipts, and they do not build an installed JDK, run jtreg/JCK, or
qualify SWT/Skija or mobile hosts.

[verify]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3-collection-refinement-20261019/templates/verify.py.txt
[plan]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3-collection-refinement-20261019/templates/probe-plan.json.txt
[native-helper]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/proof/verify_all_native.py
[combined]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-master-reconciliation/combined-proofs/receipt.json
[sdk]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-master-sdk/receipt.json
[kernel]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-master-reconciliation/kernel-qualify.log
