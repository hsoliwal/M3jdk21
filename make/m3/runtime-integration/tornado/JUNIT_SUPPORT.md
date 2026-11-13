<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Source-built JUnit preparation

`build_junit_support.py` prepares the original receiver test support for a clean
runner. The implementation and source authority remain the existing
`m3/vendor/third_party` modules and their unchanged Maven descriptors. This
preparation does not fetch released JUnit jars as a substitute, copy a second
JUnit implementation into the Tornado product, or turn support packaging into a
test result.

## Contract

The preparer first parses the existing reactor and selected module descriptors,
checks every selected module's `PROVENANCE.json` source hash, and records the
complete selected source/notice inventory. The selected closure is API Guardian
1.1.2, OpenTest4J 1.3.0, Univocity 2.9.1, JUnit Jupiter 5.11.4 and JUnit Platform
1.11.4 at their existing `-m3-source-1` coordinates. Original licenses remain
attached to each source module.

A fresh output directory receives exact copies of the existing aggregator and
every directly declared module POM so Maven can read the original reactor model.
Only the ten selected modules' source trees are copied. The POM bytes are not
rewritten and no replacement reactor owner is created. Maven uses the existing
selection `-pl junit-jupiter-5.11.4,junit-platform-launcher-1.11.4 -am` and
`-DskipTests install`. The fresh staging area prevents stale class files or
changes to an active qualification's source-built support jars. Other source
modules are model-only and are not compiled by this selection.

The preparer checks every staged input again after Maven exits successfully and
copies seven generated jars into its artifact directory. Those seven jars are
the API Guardian, OpenTest4J, Jupiter API/engine and Platform
commons/engine/launcher closure used by the unchanged receiver tests. The
existing aggregate and parameterized-test modules are still built and installed
as required by the existing source reactor, but their jars are not needed by
this exact six-class receiver suite.

`JUNIT_LOCK.json` uses `m3-source-junit-support/1`. Its artifact, source and build
receipt rows carry absolute paths and SHA-256 hashes for the same qualification
environment. The runtime harness rechecks those bytes when given `--junit-lock`.
The preparer's `PASS` means source support was built; the runtime harness must
separately compile and execute the original tests and preserve their actual
success, abort and skip counts. No JUnit jar enters the runtime JMOD/image set.

An occupied output directory, source/provenance drift, unsupported module model,
missing produced jar, source mutation or failed Maven command refuses the lock.
Failures retain their command and log evidence in the fresh output directory.
The preparer does not clean or write the original source checkout. Build support
is installed in the explicitly selected Maven repository.

`test_build_junit_support.py` checks the source/model and containment refusals,
provenance drift, bounded command failure and the rule that a failed build emits
no support lock. These fixture tests are separate from the actual source Maven
build and the receiver's original JUnit execution.

## Invocation

Run this preparation before the runtime qualification phases:

```sh
python3 m3/runtime-integration/tornado/build_junit_support.py \
  --repo-root /absolute/M3JDK21 \
  --jdk-home /absolute/jdk21 \
  --maven /absolute/apache-maven-3.9.9/bin/mvn \
  --maven-repo /absolute/m2 \
  --out /absolute/fresh-junit-support \
  --resolve-build-tools
```

Pass `/absolute/fresh-junit-support/JUNIT_LOCK.json` to the runtime harness's
`--junit-lock`. Offline Maven resolution is the default; `--resolve-build-tools`
enables the existing build plugins and compile-only support dependencies to be
resolved. It does not change the selected source-owned JUnit coordinates.
Optional `--maven-settings` and `--maven-truststore` select external proxy settings
and an existing CA store. Only their paths are recorded; their contents and
hashes are not copied. TLS verification remains enabled. The Java21 toolchain,
preparer, POMs, source inputs, actual argv, exit and output logs are sealed in
the build receipts. Maven uses one reactor worker, a 2 GiB heap, four visible
processors and a bounded command timeout.
