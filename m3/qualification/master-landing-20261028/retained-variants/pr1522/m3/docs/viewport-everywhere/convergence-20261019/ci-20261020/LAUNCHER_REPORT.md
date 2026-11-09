<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Restore the existing JUnit launcher source dependency closure

**The proposed repair supplies the already-vendored launcher to the existing
full collection qualifier. Source-seal checks pass; Java 21/Maven/JNI execution
of this repair remains a CI gate.**

## Observed failure and exact source

[PR #1522's full-collection job 118410115990](https://github.com/hsoliwal/M3jdk21/actions/runs/39355658542/job/118410115990) checked out
`066b7a15c29388f3444ab3cf0550ecf1df698c6d`. It successfully built the nine listed
source dependencies, compiled 268 production Java and 264 test Java files,
and reached Maven Surefire 3.2.5. Surefire failed to resolve
`org.junit.platform:junit-platform-launcher:1.10.2-m3-source-1` from Central.
The consumer JUnit suite did not run in that job.

The exact required launcher module already exists at
`m3/vendor/third_party/junit-platform-launcher-1.10.2`. Its unchanged POM has
the required coordinate and depends on API Guardian, Platform Commons and
Platform Engine, all of which precede it in the repaired build order.
The qualifier's dependency tuple omitted the launcher.

The launcher's existing source verifier would then fail before Maven: it
resolves repository-relative provenance beneath `m3/vendor` and expects a
nonexistent `revision` field. The already-working Platform Engine and Commons
verifiers contain the needed repository-root and receipt-field fixes.

## Three proposed source changes

| File | Change |
|---|---|
| `m3/collections/qualification/full-java-jni-20261018/verify.py` | Add `junit-platform-launcher-1.10.2` immediately after Platform Engine. The existing loop verifies source, builds/installs the unchanged module POM, verifies packaged legal resources and records its provenance/JAR hashes. |
| `.github/workflows/m3-collections.yml` | Include the launcher's directory in both pull-request and master-push path filters. Every job, command, permission, timeout and assertion remains byte-equivalent after removing these two added filter lines. |
| `m3/vendor/third_party/junit-platform-launcher-1.10.2/verify_source.py` | Reuse the exact current Platform Engine verifier with only its module-specific resource path changed to launcher. This resolves provenance from the repository root, reports existing coordinate/source-JAR hash fields and follows the declared license filename. |

No POM, dependency version, Java/JNI production body, JUnit test body or donor
Java source changes are proposed. The existing full consumer `verify` calls,
no-test failure, Java/native sequence, required-native no-skip check and
native contract list remain unchanged. The vendor-only `-DskipTests install`
operation already existed; this repair adds no assertion skip or alternate
consumer test launcher.

## Executed validation

The input recovery verified exact Git blobs for 100 fetched files at the
pinned receiving head. All 88 launcher provenance entries match SHA-256,
and the complete remote source tree contains exactly the same 80 production
Java paths declared by that provenance.

The unmodified launcher verifier was executed against the complete exact
input closure and failed with `FileNotFoundError`, reproducing its incorrect
repository-relative path. The candidate verifier was then executed against
byte-identical donor/POM inputs and passed:

```json
{"coordinate":"org.junit.platform:junit-platform-launcher:1.10.2-m3-source-1","exact_files":88,"exact_production_files":80,"sources_jar_sha256":"d3c84fbec86b224e0d2bd49e9335978bdeb6aead9f17cbee4f0e2e8885c7ab89"}
```

Python AST comparison confirms that the full qualifier changes only its
`DEPENDENCIES` tuple. YAML parsing confirms that the two event filters gain
the launcher path and all jobs remain unchanged. POM parsing confirms that
all three direct launcher dependencies precede it without coordinate changes.
This is source/metadata validation, not a Java or JNI runtime execution.

## Qualification still required

The available local environment contains OpenJDK 17 `java`, with no `javac`,
Maven or JDK 21. No artifact or consumer runtime result has been fabricated or
substituted. The unchanged existing workflow must run its owning qualifier
with a real JDK 21, Maven and the actual receiving commit:

```sh
JAVA_HOME=<actual-JDK-21> EXPECTED_HEAD=<receiving-commit> \
  python3 m3/collections/qualification/full-java-jni-20261018/verify.py
```

That execution builds the launcher using its existing source POM and keeps
the full consumer suite and required JNI checks intact. The built-artifact
branch of the launcher verifier was not executed locally; it remains part
of that qualification.

## Custody and reuse

`preimage/` and `postimage/` retain exact before/after files. `CHANGES.json`
contains each path, byte count, SHA-256 and Git blob ID. `CHANGE.patch` is the
three-file review diff. `REPAIR_RECEIPT.json` records executed checks and
limits. `INPUT_INDEX.json` pins recovered source files and URLs;
`EXACT_INPUTS.json` retains their exact UTF-8 content. `validation/` retains
the baseline failure and candidate source-verifier output.

The reused Platform Engine verifier is Apache-2.0 integration code at Git
blob `4a961825eb1e70ca7e1fcc52d4bdcca5426e24eb`. Launcher donor source remains
under Eclipse Public License v2.0 with its existing notices and source-JAR
SHA-256. `SOURCE_RIGHTS.tsv` records these boundaries. No remote mutation,
workflow rerun or runtime source change was performed by this preparation.
