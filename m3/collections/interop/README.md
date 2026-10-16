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
