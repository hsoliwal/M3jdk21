<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Runtime progress recipe host

This is a separately qualified successor of the existing Synexia Maven task
host. The original Java/text recipe engines, five SDK tests, eight sealing
tests and nine receiver cases are retained. The parser dependency is the
source-built OpenRewrite 8.17.1 module under `parser-source/rewrite-java-21`.
An additional four-case contract checks the exact parser artifact identity.

The original Java21 parser cannot losslessly parse Runtime's existing `@spec`
blocks or local `##fragment` links. The source-built module adds two bounded
visitor branches using the existing opaque Javadoc tag model and cursor
helpers. Its tests preserve exact Runtime preimage/postimage text, line endings,
whitespace, inline title markup and ordinary Java type/member references.
The original donor source/Apache license remains alongside the Maven build;
the original Lombok configuration is staged byte-for-byte during that build.

## Replay

Use the qualified Java21 toolchain and a Maven cache populated with the pinned
dependencies. `M3_SOURCE_ROOT` denotes the receiving repository and
`M3_RECIPE_CACHE` a task-specific Maven cache. Set `M3_HOST` to this directory.

```sh
mvn -f "$M3_HOST/parser-source/rewrite-java-21/pom.xml" \
  -Dmaven.repo.local="$M3_RECIPE_CACHE" install

mvn -f "$M3_HOST/pom.xml" -Dmaven.repo.local="$M3_RECIPE_CACHE" \
  -Drecipe.owner.root="$M3_SOURCE_ROOT/m3/arrays/qualification/empty-segments-20261017/recipe-custody/sdk-owner" \
  -Dm3.dataset=fixture -Pcontract verify

mvn -f "$M3_HOST/pom.xml" -Dmaven.repo.local="$M3_RECIPE_CACHE" \
  -Drecipe.owner.root="$M3_SOURCE_ROOT/m3/arrays/qualification/empty-segments-20261017/recipe-custody/sdk-owner" \
  -Dm3.dataset=m3jdk21-runtime-facade -Pcontract verify

python3 "$M3_HOST/receive.py" --target "$M3_SOURCE_ROOT" \
  --recipe-owner "$M3_SOURCE_ROOT/m3/arrays/qualification/empty-segments-20261017/recipe-custody/sdk-owner" \
  --dataset m3jdk21-runtime-facade --check
```

The Maven validate phase checks the source-qualified parser SHA-256 before
compilation or tests. Offline replay uses `-o` after the declared dependencies
and source-built parser have been populated. Source-origin pins and the
receiving integration baseline are recorded in the generated receiving map.
Historical host receipts are kept under `history/`; fresh qualification results
belong to this parser-host epoch.

## Evidence scope

The parser's paired source builds reproduce three failing syntax witnesses in
the original four-test corpus and pass all four with the repair. All 21 compiled
class owners are retained: one outer visitor changes behavior, two nested
visitors differ only in source line numbers, and the other 18 class files are
byte-identical. The donor's two existing main-source compiler diagnostics are
recorded in both builds; the recipe host, new parser tests and target Runtime
compiler/doclint checks retain their separate strict gates.

The Runtime recipe adds the two opt-in facade methods and their compatibility
tests. Its current map keeps the existing STRING phase and pending runtime
promotion. Full-image and platform results are separate from the finite patch,
frozen-client and scoped/executor/child evidence.
