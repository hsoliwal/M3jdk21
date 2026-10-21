# Source-qualified TQ reactor

SPDX-License-Identifier: Apache-2.0

This Maven build recipe composes the existing vendored producer reactor and unchanged TQ module. It changes no module dependencies, public Java interfaces, source ownership or historical hashes. Maven performs dependency sorting; this module supplies composition only.

From the public receiver root, after source custody and storage admission:

```sh
mvn -B -ntp -f m3/tooling/tq-sync/source-reactor/pom.xml \
  -Dm3.receiver.relative=../../../.. -pl com.m3:tq-sync -am verify
```

The property is a **relative path from this POM to the receiver root**. It has no default. For canonical authoring input stored outside the receiver, calculate the corresponding relative path. Absolute module values have no supported contract.

For a separate later Maven invocation, use `install` in place of `verify` and reuse the same explicitly controlled local repository. A fresh real compile/test run must also select the source-built `junit-platform-launcher:1.10.2-m3-source-1` needed by Surefire, include TQ's two external engine source files under `m3/tooling/migration-recipes/src/main/java`, admit external plugin/binary resolution, and check real nonzero test reports. The declared TQ graph alone does not prove those implicit dependencies.

The bounded qualification lane deliberately uses **offline `validate` with POM-only inputs**. It checks current source custody, actual Maven selection of 56 producers plus TQ, missing property, missing child POM, missing selector, and the counterexample that a stale producer can still yield Maven success with an incomplete selected set. No compilation, artifact availability, JUnit, OpenRewrite execution, original private model, overflow, JNI or full-JDK result is implied.

The existing `-m3-source-1` producer coordinates remain unchanged. They do not qualify the private canonical `-synexia-source-1` model. Current and historical M3String owner guards remain unchanged.
