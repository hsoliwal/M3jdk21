<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0 -->

# M3 text module receiver

These are first-party Apache-2.0 source snapshots emitted by the Synexia-owned
`m3-text-jmods` recipe. The target owns SDK naming and acceptance. Public Java SE
APIs are unchanged. The modules `jdk.m3.text` and `jdk.m3.io` are opt-in extensions;
this packet does not replace java.base or advance the ordered runtime receiving phase.
`COVERAGE.json` defines every tool contract and pending broader family. The native
FIFO path is Linux-qualified; filesystem behavior stays explicit.

Run from the M3JDK21 repository root with an intact Java 21 JDK and C compiler:

```sh
python3 m3/tooling/module-packs/text-tools/build.py \
  --jdk-home "$JAVA_HOME" \
  --sources m3/tooling/module-packs/text-tools/src \
  --native-source m3/tooling/module-packs/text-tools/native/m3textio.c \
  --smoke m3/tooling/module-packs/text-tools/fixtures/src/test/java/M3TextSmoke.java \
  --legal-notices m3/tooling/module-packs/text-tools/legal \
  --packager m3/tooling/jmod-recipes/tools/jmod_pack.py \
  --out build/m3-text-tools
```

The thin build driver calls the existing JMOD packager. A fresh output directory is
required. It produces two JMODs, a developer image and compiler/native/runtime logs.
The image includes java, javac, jar, jmod, jlink, jdeps and jpackage; the probe uses
javac and java to execute all 19 atoms, not every feature of every developer tool.
Use `-source 21 -target 21` with the image's javac for SDK extensions. Standard
`--release 21` does not expose these additional modules.

`SOURCE_REF.json` pins the exact canonical Synexia commit and recipe manifests;
`PROVENANCE.json` binds each received blob to its source or executed recipe.
The target's canonical name map contains the two module mappings. Reusable recipe
changes go back to Synexia; target tooling mirrors are not a second source authority.
Local linked-image qualification is separate from full M3JDK21 product build/jtreg,
full source reactor admission, cross-platform behavior and performance qualification.
