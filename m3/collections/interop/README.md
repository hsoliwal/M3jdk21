<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 four-library collections: qualified optional runtime slice

The existing OpenRewrite recipe generated the three runtime classes and two test classes
now present under `src/`. Their exact bytes were promoted from successful receiver run
`38763952509`, head `777ba2b97d15ea2ef2e5ea4340f19c83fd7896bd`, after authenticating the
uploaded artifact and comparing all SHA-256 and Git blob identities. No manual production
source transformation was used. Canonical source remains Synexia PR #11676, now at
`ce510939bde1be4d6f76f014ef6e92e81bbab05b`. See `recipe-receipt.tsv` and
`qualification/qualified-recipe-output.json` for the exact bounded evidence.

## Implemented and tested

18 factories expose selected JDK, Guava, Eclipse Collections and Spring interfaces over
existing M3 map/set/list/tree owners. 11 ordered stream collectors retain M3 accumulation
and result storage for the supported contracts. Spring-created value lists and single-value
copies use M3 owners; caller-provided lists deliberately preserve identity and aliasing.
The optional artifact carries its third-party dependencies; the existing core still needs
only java.base. Ordinary donor operations such as Eclipse collect/toImmutable can still
allocate donor-backed results: use explicit M3 collectors when M3 result ownership is needed.

Actual Linux ARM64 / Java21 evidence: **six recipe tests and 42 runtime tests passed, with
zero failures, errors or skips**. The full unchanged M3 core and all new runtime/test source
compiled with warnings as errors. The core java.base-only dependency boundary and optional
jar packaging also passed. Source promotion does not imply an installed M3JDK distribution,
whole-library API compatibility, universal speedup, native qualification or receiving-phase
promotion. Earlier failed runs are retained, including the missing SLF4J host dependency,
test inference errors and corrected donor-specific equality assertion.

## Important Spring compatibility limit

`springSingleValueMap()` preserves Spring 6.2.0 SingleToMultiValueMapAdapter, including its
null-equality anomaly: get(key) returns null for a null scalar, but entrySet exposes a
singleton-null list. Independent JDK-backed donor peers also compare unequal in that state.
Its hashCode delegates to the scalar backing map. It is therefore a version-specific bridge,
not a general Map-equality substitute for null-containing data. The focused regression tests
that behavior explicitly rather than silently changing the donor. See
`qualification/spring-single-value-donor-parity.md`. `springMultiValueMap()` is a different,
map-of-lists implementation; preserve the distinction.

## Build and verify

After installing the exact existing `com.m3:m3-collections:0.1.0-SNAPSHOT` core artifact:

```sh
mvn -B -f m3/collections/interop/pom.xml verify
```

The exact-head workflow compiles and installs the unchanged core itself, avoiding unrelated
historical core test-coordinate resolution. It validates all installed postimage hashes,
runs all six recipe proofs, reapplies the same recipe at fixed point, runs all 42 runtime
tests, checks that production sources remain unchanged, and retains jars, dependency hashes,
generated-source receipts and actual test reports. Reproduce the recipe directly with:

```sh
mvn -B -f m3/collections/interop/recipe/pom.xml test
mvn -B -f m3/collections/interop/recipe/pom.xml \
  '-Dtest=M3CollectionsLibraryInteropTest#exactOutputsFixedPointAndOptionalMaterialization' \
  -Dm3.collections.target="$PWD" test
```

Use an exclusively owned checkout. The existing receiver engine is not upgraded, and its
PlainText fallback is not accepted: the shared proof requires real Java compilation units.
Only absent or exact-output targets are admitted. Changes after this delivery require a
reviewed canonical before/after recipe; do not delete installed files to force admission.
Preparation identities in donor-origin.json are historical and are supplemented by current
qualification receipts.

## Usage

```java
import com.m3.collections.interop.M3LibraryCollectors;
import java.util.function.Function;
import java.util.stream.Stream;

var words = Stream.of("red", "rose", "blue").parallel().collect(
    M3LibraryCollectors.toGuavaListMultimap(s -> s.substring(0, 1), Function.identity()));
var counts = Stream.of("red", "blue", "red").parallel().collect(
    M3LibraryCollectors.toMap(Function.identity(), s -> 1L, Long::sum));
```

Collectors do not claim CONCURRENT or UNORDERED. Parallel collection merges independently
owned partial results; it does not make the returned mutable collection thread-safe.
Null policies, duplicate/merge rules, views, exceptions and borrowed ownership remain explicit.

## Remaining full scope

Bags/multisets, bimaps, ranges, primitive Eclipse APIs, further Spring utilities, complete
intrinsic allocating-return closure, concurrency/reference/serialization variants, JNI batch
specialization, performance measurements, dependency security and installed-image delivery
remain unqualified. A compatible factory interface does not qualify every concrete donor
class or serialized form. All unlisted APIs remain unqualified. Pinned dependency versions
are compatibility inputs, not latest-version or production-security endorsements. Existing
M3 and upstream licenses and attribution remain intact.

## Publication HOLD: third-party consumer dependencies (2026-10-27)

The independent `com.m3:m3-collections-interop:0.1.0-SNAPSHOT` artifact currently
exposes Guava, Eclipse Collections, Spring Core and Commons Collections types in
its public interfaces, and its own POM declares all four as direct compile
dependencies. This is **not** a zero-third-party downstream-dependency
implementation. The `com.m3:m3-collections` core remains separate; do not
conflate these two artifacts or claim that its optional interop bridge is
fully source-inlined.

Until every provider API is implemented using appropriately licensed,
per-file provenance-controlled Synexia/M3 source with independently verified
consumer compile/runtime dependencies, the interoperability artifact is
**quarantined from ordinary Maven `install` and `deploy`**. Its POM sets
`maven.install.skip=true` and `maven.deploy.skip=true`. These Maven
defaults do not remove, shade or silently make optional the existing four
external dependencies; they do not rewrite any Java/JNI contract. Maven
`verify` can still execute the existing bounded tests. Explicit command-line
overrides are possible and are not approval for production distribution.

Admission requires the canonical Synexia OpenRewrite per-file recipe
inventory and source-license receipts, equal public API/behavior and
provider-specific tests, `mvn dependency:tree` and an isolated downstream
consumer compile/runtime proof establishing **zero external transitive
runtime dependencies**. Preserve provider licenses and the previous runtime
regression evidence. See
`qualification/PUBLICATION_HOLD_20261027.tsv`; this source-only
quarantine is not completion of the inlining or of the larger M3JDK21 build.
