<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 String structured-text CSV/TSV receiver — 2026-10-15

## Ownership and exact receiver

Canonical source and OpenRewrite templates: `hsoliwal/com.synexia`,
review branch `m3/structured-text-csv-mindex-20261015`.

- Canonical Java donor:
  `synexia-m3-jmods/modules/com.synexia.m3.text.csv/src/main/java/com/synexia/m3/text/csv/CsvTools.java`,
  SHA-256 `72c88dc6880829f6530d6d6e8f4ce15cb32c532d49b59e35b109f4b655b895de`.
- Target-owned port:
  `src/jdk.m3.text/jdk/m3/text/CsvTools.java` (relative to
  `m3/tooling/module-packs/text-tools/`), SHA-256
  `f07c5ef868f075ccae4b0732b20ecbb20634afbd73bd68a39e69a2ebf50d7a1b`.
- Canonical target recipe:
  `com.synexia.rewrite.M3StructuredTextCsvReceiver20261015`
  in `synexia-openrewrite-recipes`. The existing exact-source
  `M3HashPinnedJavaSnapshotRecipe` admits the new port from `ABSENT`,
  and the existing `M3TextSmoke.java` only from SHA-256
  `8033103404ef14c35e1fc0690ff8d604437c9e823bb314ec1fc909c6627383c2`.
  No existing M3 text source or native symbol is replaced.

## Runtime placement and semantics

The existing opt-in `jdk.m3.text` module remains the single target owner.
The public `CsvTools.parse(CharSequence, delimiter, limits)` accepts a
JDK String or an M3 String-shaped immutable indexed `CharSequence`.
`MIndexString` satisfies this interface via `IndexStringText`. Logical
fields are UTF-16 ranges plus compact quote-unescape indexes. Parse, field
projection and regex filter do not create a flat copy of the *whole* input;
explicit serialization materializes a new result.

The 19 existing text/file atoms, `jdk.m3.io`, POSIX `mkfifo` JNI ABI,
and their baseline smoke assertion remain unchanged. The smoke additionally
invokes `CsvTools.main("--self-test")` under the linked-image test path.
The builder still emits two target JMODs; this addition does **not** create
another target Java module, change `java.base`, replace `String`, or
advance the String-to-arrays-to-collections receiving order.

## Verification commands

Use the existing packager rather than making a new image system:

```sh
python3 m3/tooling/module-packs/text-tools/build.py \
  --jdk-home "$JAVA_HOME" \
  --sources m3/tooling/module-packs/text-tools/src \
  --native-source m3/tooling/module-packs/text-tools/native/m3textio.c \
  --smoke m3/tooling/module-packs/text-tools/fixtures/src/test/java/M3TextSmoke.java \
  --legal-notices m3/tooling/module-packs/text-tools/legal \
  --packager m3/tooling/jmod-recipes/tools/jmod_pack.py \
  --out build/m3-text-tools-csv
```

A standalone JDK 21 module compile, executable CSV self-test and
byte-repeat JMOD proof have been run on the canonical Java implementation.
The package-relocated target Java source separately compiles and its self-test
passes as a minimal `jdk.m3.text` module. The **complete** image build above,
original POSIX/JNI regression, full modified JDK build, jtreg, JDK String
layout/identity and external library parity are not established by those
standalone passes; review them as pending until their logs actually exist.

The external `hsoliwal/structured-text-tools` repository is a project
catalogue, not copied upstream implementation source. Neither its listed
third-party licenses nor their feature sets are implicitly adopted by this
first-party Apache-2.0 receiver.
