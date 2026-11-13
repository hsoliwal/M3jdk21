<!-- SPDX-License-Identifier: Apache-2.0 -->
# Source-vendored JUnit prerequisite for indexed M3 object snapshots

Receives an existing M3 build dependency, *not* a renamed public JUnit binary.
`m3/collections/pom.xml` deliberately depends on `org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1`, whose exact upstream sources are vendored with EPL-2.0 provenance at
`m3/vendor/third_party/junit-jupiter-5.10.2`.

Prior M3 JDK [#914](https://github.com/hsoliwal/M3jdk21/pull/914) already qualified using `-pl ...junit-jupiter-5.10.2 -am -DskipTests install` before module tests. Reuse that source-reactor install sequence for this exact new CI workflow, not public `5.10.2` aliasing, fake-local JAR fabrication or `pom.xml` mutation.

This is the one-file, source- SHA256-pinned TEXT recipe `com.synexia.m3.Jdk21IndexedObjectPositionCI20261018`. Source file is `.github/workflows/m3-indexed-object-position-20261018.yml`. The canonical OpenRewrite text engine enforces preimage, postimage, drift refusal, fixed point and no unexpected source input. Workflow YAML is not a Java semantic-atom mutation. The owner JDK21/JNI oracle and all other gates are preserved unchanged.

Do not claim the Maven source reactor passes until an actual hosted run does.
