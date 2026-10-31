<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Swing viewport binding — current receiving record

Receive against M3JDK21 `9a164c31b38c7013c179a3ff38b0bb60c199c7fd` using
the unchanged `com.synexia.rewrite.M3SwingConvergence20261019` recipe in
[canonical PR #12528](https://github.com/hsoliwal/com.synexia/pull/12528), commit
`74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd`. Its manifest SHA-256 is
`11f3d4890741deaa42323a11069c8c2287613dbf1f57db450e80cd8404d3781f`.
Canonical publication and [M3JDK21 receiving PR #1525](https://github.com/hsoliwal/M3jdk21/pull/1525)
are open drafts. The receiving PR is stacked on [collection PR #1522](https://github.com/hsoliwal/M3jdk21/pull/1522).
The mapping fragment records both the source and metadata receiving PRs.

The private repair makes `M3ViewportBinding` unsubscribe from its former
`JViewport` and subscribe once to its actual new viewport on installation,
detachment or reparenting. Public/protected binding and nested `Target`
interfaces, geometry and synchronization bodies remain unchanged. The packet
changes this owner and adds two proofs, retaining four exact KEEP dependencies.

## Source and qualification epochs

The owner is
`m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/M3ViewportBinding.java`.

| Identity | SHA-256 |
| --- | --- |
| Current receiving preimage | `94101f8fe12a9dc4d5856fc3aff1006fbbd5722cbca17b7cede65b3d5a669b21` |
| Qualified postimage | `b5954005074667174f2254c77cbe531c41ed7dcf12a8c05811b5e341d0a11115` |
| Original module POM, retained | `54448ae1defe035ed9f4ac6ead6f4c9eafd47a1340b173c3fdb95d4bf0a18da1` |

The recorded compiler/runtime proof used parent
`c2a8733f6eebaadcc206a61d08906bfd8b6d60dc`. Exact current source admission
confirmed the same complete 73-file module tree and 75-file binding-only
candidate source closure. Both baseline epochs identify the 73-file Git tree
`d1e0a2113d425d30037f27af66cf11c36e5db200`, including unchanged file modes.
These source identities permit reuse of the earlier tested bytes. They do not
claim a new runtime execution or broaden platform qualification. Adding the
separate five UI `jdk-reproducers` files produces an 80-file combined receiving
module; no fresh 80-file Maven execution is claimed by this record.

The exact admissions are published beneath the canonical evidence prefix:

- `delivery-metadata/current-swing-9a164c31/ADMISSION.json`, SHA-256
  `af080f1d211aa7082f4efa6b3f7b138733e2f0bfae1064b0195d39199f025a93`.
- `delivery-metadata/current-swing-9a164c31/MODULE_ADMISSION.json`, SHA-256
  `e996fa1ab799a1017b4175d14f13d1ab6e8e4b41eaf3b05a836b017d221a4208`.

See `m3/docs/viewport-everywhere/convergence-20261019/MASTER_ADMISSION.md`
for current collection receipts and the separate integration boundary.

## Reproduce with the original module

These are reproduction instructions, not additional executed commands. Use
Bash from a complete reviewed checkout containing the postimage and proofs.
Set `JAVA_HOME` to a real Java 21 installation. The accepted earlier host was
Ubuntu OpenJDK `21.0.12+8-1-24.04-Ubuntu` on Linux x86_64.

Keep the original `5.10.2-m3-source-1` JUnit dependency closure and Maven
plugins. A complete vendored-source checkout can build that closure with the
first command; do not substitute different dependency coordinates in the POM.

```bash
set -euo pipefail
: "${JAVA_HOME:?Set JAVA_HOME to the Java 21 installation}"
"$JAVA_HOME/bin/java" -version
M3_SWING_MAVEN_REPO="$PWD/build/m3-swing-receiving/maven-repository"
mvn -B -ntp -f m3/vendor/third_party/pom.xml \
  -pl junit-jupiter-5.10.2 -am -DskipTests \
  -Dmaven.repo.local="$M3_SWING_MAVEN_REPO" install
mvn -B -ntp -f m3/ports/swing-viewport/pom.xml \
  -Dmaven.repo.local="$M3_SWING_MAVEN_REPO" test
M3_SWING_CP="m3/ports/swing-viewport/target/test-classes:m3/ports/swing-viewport/target/classes"
"$JAVA_HOME/bin/java" -Djava.awt.headless=true -cp "$M3_SWING_CP" \
  com.m3.swing.viewport.M3ViewportBindingLifecycleProof
"$JAVA_HOME/bin/java" -Djava.awt.headless=true -cp "$M3_SWING_CP" \
  com.m3.swing.viewport.M3DesktopViewportProof
```

Earlier execution recorded 603 real Swing EDT checks, 20 unchanged desktop
control checks and 18 original-POM JUnit tests with no failures, errors or
skips. The old owner failed the witness because the former viewport retained
one listener where zero were expected. `javap -protected -s` comparison covered
the binding and nested `Target` class files.

## Published evidence and limits

The immutable evidence prefix at the canonical commit above is
`synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/`.

| Receipt | SHA-256 | Scope |
| --- | --- | --- |
| `swing-lifecycle-validation/RECEIPT.json` | `0e66d1441c298f3d003a2adc8a591914be44a2a63d4a4c1c8c07daa2d630027b` | Source, listener and public/protected API checks. |
| `swing-lifecycle-validation/MAVEN_RUNTIME.json` | `89615bd0b831f7a440e8ce2e46cf615c23b88911640099aef2b1fa013bf9392e` | Actual earlier Java runtime, source-built JUnit classpath and 18-test totals. |
| `successor-sdk-final-selection/receipt.json` | `2b2e935ceb68b54d4d1a3d053533e1fec31b0e72c2ca1d481c3845e4733db82c` | Nine historical OpenRewrite 8.17.1 PlainText SDK tests across three unchanged recipes; JavaLST NOT_RUN. |

The new current collection SDK/module/native receipts do not replace these
Swing records or turn them into a new native-UI execution. Visible-row overflow
arithmetic remains a separate deferred atom. Installed M3JDK21, jtreg/JCK,
native UI, Android/iOS runtime/host/device and broad resource gates remain
separate. The lifetime change adds one private viewport reference; no broad
CPU, retained-heap or native-memory improvement was measured.

Retain this module's Apache-2.0 `LICENSE`, `NOTICE` and file headers. Separate
`java.desktop` payloads retain their GPLv2/Classpath Exception terms. The module
POM does not compile `jdk-reproducers`; see
`m3/docs/swing-viewport/convergence-20261019/RECEIVING.md` for that packet.
This document is recreated from published records and prior notes; it does not
claim exact restoration of a former unpublished template. Preserve
STRING / QUALIFICATION_REQUIRED and all later waiting phases.
