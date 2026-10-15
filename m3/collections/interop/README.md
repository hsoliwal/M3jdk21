<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 four-library interop: receiver admission packet

**This is recipe-input preparation, not installed runtime delivery.** The canonical source is
Synexia PR #11676 at `0ff2ee68607e02b0ec2d1bc699e93653a740ab87`. The five Java templates,
manifest, named recipe and six-method proof host are exact mirrors. `donor-origin.json`
records identities and pending gates. Canonical improvements must be made in Synexia's
existing crate and re-received with provenance; do not independently fork these templates.

The candidate contains 18 typed JDK/Guava/Eclipse/Spring factories, 11 explicit M3-backed
stream collectors, and 42 runtime tests. Spring-created lists and single-value copies use
existing M3 owners; caller-provided lists remain aliased. Ordinary donor allocating methods,
such as Eclipse collect/toImmutable, are not silently reclassified as M3-backed. The optional
interop artifact carries third-party dependencies; existing core remains separately built.

## Source generation and proof

No `interop/src/main/java` or `interop/src/test/java` outputs are committed by this admission
packet. The only runtime-source generator is the existing receiver
`m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java`.
It is not upgraded or replaced. The portable shared test checks actual Java compilation units
and exact printed postimages, so the older engine's optional PlainText fallback is not an
acceptable proof. Six recipe tests exercise exact/named execution, hashes, fixed point,
occupied/drifted targets, duplicates and non-Java refusal.

From an exclusively owned checkout, with Java 21 and Maven, first run all recipe proofs:

```sh
mvn -B -f m3/collections/interop/recipe/pom.xml test
mvn -B -f m3/collections/interop/recipe/pom.xml \
  '-Dtest=M3CollectionsLibraryInteropTest#exactOutputsFixedPointAndOptionalMaterialization' \
  -Dm3.collections.target="$PWD" test
```

Only the second invocation materializes the actual tested OpenRewrite results. Existing
non-identical outputs and symlink destinations are refused. No runtime source is copied
from a template by shell/Python instead of the recipe. Source delivery is not a multi-file
transaction; the checkout must be exclusively owned.

The exact-head `M3 four-library receiver admission` workflow runs on a standard Linux ARM64
runner as a separate platform gate. It checks the existing engine identity, requires all six
recipe tests before generation, compiles the unchanged core, checks java.base-only dependencies,
installs that locally compiled core into the runner cache, then builds this optional module.
It requires exactly 42 runtime tests with no failures, errors or skips, and uploads generated
sources, source hashes, public API and actual test reports. It neither accesses the private
Synexia repository nor writes back to GitHub. A queued job or a mirrored template is not a
successful recipe/runtime gate.

## Evidence and remaining scope

Canonical local evidence covers the unchanged map and list/deque owners: 160,000 + 120,000
differential operations and 960,020 + 600,005 comparisons, respectively. A deterministic JDK
census recorded 86 public collection-related types and 3,470 members on the recorded host.
These are not proof that these new external-library adapters compiled or passed their tests.
Adapter compilation, actual OpenRewrite execution and all 42 runtime tests were still pending
when this packet was prepared. Inspect the exact-head workflow reports, not this sentence,
for later execution evidence.

Full family delivery remains open: bags/multisets, bimaps, ranges, primitive Eclipse APIs,
remaining Spring utilities, intrinsic result-owner closure, concurrency/reference/serialization
variants, JNI specialization, performance measurements and installed-image delivery are not
qualified here. All unlisted APIs remain unqualified. Neither core ownership nor the global
receiving phase is promoted, and java.base is not replaced. Serializability, concrete donor
class identity and a universal performance advantage are not promised. Pinned dependency
versions are compatibility inputs, not latest-version or production-security endorsements.
