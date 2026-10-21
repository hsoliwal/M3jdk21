<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Source-owned JUnit dependency bootstrap

The receiving `m3/collections/pom.xml` requires `org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1`.
Its real source owners are already in `m3/vendor/third_party`; official Maven Central binaries under
that fabricated suffix are not acceptable. No build is claimed by this document.

The minimal inspected source reactor selection is:

```sh
mvn -B -ntp -f m3/vendor/third_party/pom.xml \
  -pl junit-jupiter-5.10.2,junit-platform-launcher-1.10.2 -am \
  -DskipTests -Dmaven.repo.local=/approved/toolchain/m2-repository install
```

This builds apiguardian-api, opentest4j, univocity-parsers, junit-platform-commons-1.10.2,
junit-platform-engine-1.10.2, junit-platform-launcher-1.10.2, junit-jupiter-api-5.10.2,
junit-jupiter-engine-5.10.2, junit-jupiter-params-5.10.2 and junit-jupiter-5.10.2 from existing source.
All those modules publish their POM-declared `*-m3-source-1` versions. JUnit API/params also require
actual Kotlin 1.8.22 tooling and compile-only standard library. Preserve their upstream source,
EPL-2.0 notices, published-source provenance and existing descriptor/shading build steps.

The vendor modules explicitly skip upstream tests because published source jars lack that test suite;
this bootstrap establishes compiled dependencies, not upstream JUnit behavioral verification. The
collections module must run its own real tests afterward in the required stage order.

`m3/synexia-import/pure-int-recipe-custody/install.sh` documents the larger source closure including
OpenRewrite. Do not use it for this minimal operation: it checks hardcoded HOME/.m2 artifact paths
and also installs an unrelated custody module. Keep the user's HOME unchanged and invoke the vendor
reactor directly with the approved cache path. Its `test_source_bootstrap.py` exercises fake-Maven
installer fixtures only and cannot prove real library compilation or collections runtime behavior.

Expected cache outputs include:

- `org/junit/jupiter/junit-jupiter/5.10.2-m3-source-1/junit-jupiter-5.10.2-m3-source-1.jar`.
- `org/junit/platform/junit-platform-launcher/1.10.2-m3-source-1/junit-platform-launcher-1.10.2-m3-source-1.jar`.

Parent bootstrap execution must inventory approved output/cache namespaces, retain the real compiler
and installation logs, and stop on any missing source or dependency. No stubs or coordinate substitution.
