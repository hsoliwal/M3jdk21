<!-- SPDX-License-Identifier: Apache-2.0 -->
# 100 MB JSON acceptance for M3AST

The opt-in M3LargeJsonAcceptance class tests the existing Jackson-backed
M3ObjectMapper and M3AST; it does not implement another parser. The original
fixture came from the canonical Synexia owner at
synexia-openrewrite-recipes/recipes/m3-json-scale-20261018/inputs/ and is retained
in the PR #1289 qualification archive. The active fixture selects the named
limits factory required by the combined current mapper API.

## Run after the existing strict four-stage Java 21 source build

Run the default full Jackson pack build.py command in a fresh output directory
as documented in README.md. It executes lint, compile, host-contract and linked
runtime gates in order, and compiles all src/test/java sources including this
optional acceptance main. Small-input contracts still run normally.

On a POSIX machine, using the previously qualified same-source build output:

    BUILD=/absolute/path/to/jackson-build-output
    JAVA21=/absolute/path/to/java21/bin/java
    SCALE_OUT=/absolute/path/to/absent/scale-proof-100mb
    "$JAVA21" -Xmx2g --module-path "$BUILD/jars" \
      --add-modules java.management \
      --add-reads jdk.m3.jackson=java.management \
      --patch-module "jdk.m3.jackson=$BUILD/test-classes" \
      -m jdk.m3.jackson/jdk.m3.jackson.tests.M3LargeJsonAcceptance \
      "$SCALE_OUT" 100000000

Use appropriate shell quoting and path syntax on Windows. The management read
is test-only. Also run 104857600 with a second fresh output directory.

The fixture is generated locally with exact size and over 500,000 ordered
records; it is never committed to Git. The test verifies count, first/middle/
last indexed lookup, ordered duplicate field occurrences, nested arrays, an
entire streamed Jackson serialization SHA-256 match, and structural identity.
Only after those checks succeed does it emit M3_JSON_SCALE_PASS and RESULT.txt
with parsing time, serialization time, atom/child counts, bytes, throughput,
SHA-256 and peak Java heap-pool use.

## What is not yet proven

Adding this test is NOT evidence that 100 MB was parsed successfully. This
fixture uses the explicit byte-bounded source snapshot path; the separate
bench/README.md workload uses readASTStreaming and independent token comparison.
Passing either functional gate would not prove strict DOOP/no-entry semantics,
bounded memory, on-demand source materialization or near-zero object churn.
Record process RSS and GC; add dense/unique-value/deep/Unicode adversarial
100 MB fixtures before declaring general large-JSON support.

Canonical source and transformation recipe ownership belong to Synexia; the
M3JDK21 runtime owns the eventual optimized implementation. Do not weaken
the default limits, original contracts or scope gates to obtain PASS.
