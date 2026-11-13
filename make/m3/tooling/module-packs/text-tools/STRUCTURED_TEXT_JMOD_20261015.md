<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3SDK structured text tools: optional JMOD receiver

This is an **additive, review-only** receiving slice for the [structured-text-tools](https://github.com/hsoliwal/structured-text-tools) *catalogue*. The source catalogue lists other projects; it is not a source library and none of the listed third-party implementations was imported.

## Reused implementation owners

The existing `jdk.m3.text` module already owns immutable `TextFrame` and `TextLine` framing, `Limits`, 17 CPU text atoms, and a deterministic pipeline model; `jdk.m3.io` owns the two opt-in filesystem atoms. Their JMOD build is `build.py` delegating to the canonical `m3/tooling/jmod-recipes/tools/jmod_pack.py`. The new `jdk.m3.structured` **requires transitive** `jdk.m3.text` and exports only `jdk.m3.structured`. The builder now creates three JMODs and packages the developer image with existing tooling. The native Linux FIFO implementation is unchanged; no additional JNI symbols are introduced.

Portable donor and recipe source: `hsoliwal/com.synexia`, review branch `m3/structured-text-jmods-20261015`. The canonical named OpenRewrite recipe is `com.synexia.m3.StructuredTextJmods` using the **existing** `M3HashPinnedJavaSnapshotRecipe` owner and six ABSENT-before manifest/template pins. Receiver transformation is the deterministic package mapping `com.synexia.unix4jm3` -> `jdk.m3.structured`, substituting the existing target API imports, and the existing module descriptor/JMOD packager. No original M3 text type is renamed or discarded. The prior `SOURCE_REF.json`, `PROVENANCE.json`, and `COVERAGE.json` remain **historical 19-atom receipts**, not proof of this successor. New qualification is recorded separately.

**M3 String ownership:** public signatures continue to use `String`/TextFrame. When executed on a qualified M3JDK21 runtime, String semantics and storage are owned by the existing `java.lang.M3String` integration under `java.base`. This optional JMOD introduces *no* second canonical string store or Synexia runtime dependency; the module alone is not evidence that M3 String has replaced every JDK String path.

## Implemented

| Command | Stream semantics | Library API |
| --- | --- | --- |
| `csv-cut 0,2` | Zero-based CSV column projection, quoted multiline values, escapes, canonical LF | `StructuredTextTools.delimited`, `selectDelimited` |
| `tsv-cut 1` | Identical CSV state machine with tab field delimiter | `selectDelimited` |
| `jsonl-pointer /a/0` | Strict JSON Lines and RFC 6901 pointer; exact encoded selected JSON lexemes | `JsonLinesTools.select` |
| `ini-get section key` | Strict literal INI subset; duplicate key refused | `StructuredTextTools.ini` |
| `env-get NAME` | Strict literal .env subset, optional `export`; no interpolation | `StructuredTextTools.dotenv` |

CLI takes bounded UTF-8 bytes from stdin and writes only to stdout on success; malformed input or invalid arguments cause exit 2 with diagnostic on stderr, I/O failure exit 1. No subprocess launch, shell evaluation, filesystem write, HTTP, native pointer retention or dynamic execution.

Example from an image linked with this module:

```sh
printf '%s\n' '"a,b",42' | image/bin/java -m jdk.m3.structured/jdk.m3.structured.StructuredCli csv-cut 0
printf '%s\n' '{"a":[1,2]}' | image/bin/java -m jdk.m3.structured/jdk.m3.structured.StructuredCli jsonl-pointer /a/1
```

Run the existing opt-in image builder from the repository root:

```sh
python3 m3/tooling/module-packs/text-tools/build.py \
  --jdk-home "$JAVA_HOME" \
  --sources m3/tooling/module-packs/text-tools/src \
  --native-source m3/tooling/module-packs/text-tools/native/m3textio.c \
  --smoke m3/tooling/module-packs/text-tools/fixtures/src/test/java/M3TextSmoke.java \
  --legal-notices m3/tooling/module-packs/text-tools/legal \
  --packager m3/tooling/jmod-recipes/tools/jmod_pack.py \
  --out build/m3-text-tools-structured
```

The existing builder refuses occupied output and non-Linux native qualification. Build logs and image JMODs are generated locally; this PR does **not** commit binaries or install anything into JAVA_HOME.

## Qualification ledger

| Gate | Status |
| --- | --- |
| Existing text-tool source inventory and ownership | Inspected; two prior modules and 19 atoms confirmed |
| Donor Java 21 lint compilation and focused 3-suite standalone behavior checks | PASS using minimal API stubs |
| Isolated named-module Java 21 compilation (strict lint with `-module` suppressed per existing builder) | PASS, with a minimal test `jdk.m3.text` |
| Manual `jmod create/describe`, `jlink` and runnable `csv-cut` / `jsonl-pointer` | PASS in an isolated Java 21 image with minimal test dependency |
| Exact OpenRewrite recipe application/fixed point/drift-refusal via Maven | PENDING: Maven unavailable in local execution environment |
| Full M3JDK21 text-module builder with **real** `jdk.m3.text` sources, and full linked-image smoke | PENDING |
| Full JVM/JNI build, jtreg, Windows/macOS, CPU/heap/native budget and benchmark evidence | PENDING; existing JNI boundary unchanged |

**The isolated linked image must not be described as the full M3SDK product image.** No `java.base`/HotSpot/String migration phase advances from these optional tools. Keep the receiving pull request in draft until the full required gates execute and pass.

## Beyond this slice

External catalogue coverage for YAML, TOML, XML, HTML, SQL, jq/JSONPath, Awk, file databases and interactive applications is not yet implemented. Each requires a concrete owner, license/NOTICE and baseline contract, source/recipe seal, tests and opt-in module admission rather than a bulk copy.
