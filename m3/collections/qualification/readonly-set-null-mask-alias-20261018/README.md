<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Readonly set null navigation and path-mask alias receiving evidence

Two existing collection owners were refined through the canonical Synexia recipe,
with four additional Java test files. Local qualification passed **234 module
tests**, **four real OpenRewrite SDK tests**, the new readonly-set oracle's
**6,076,493 checks**, the existing bounded-navigation control's **90,431 checks**,
and **28 Java/JNI runtime processes**. Public/protected descriptors for all **320
types** and `module-info` remain equal to the baseline. These results qualify the
six source files in [SOURCE_RECEIPT.tsv](SOURCE_RECEIPT.tsv) for this bounded
independent-module repair.

The **exact source is qualified, and the canonical and catalogue publication
pins are verified**. [RECIPE_PIN.json](RECIPE_PIN.json) records those immutable
identities. This receiver binds to its **containing Git commit** on branch
`m3/readonly-set-null-mask-receiver-20261018`; resolve the commit and associated PR
from Git/GitHub metadata and verify its six source blobs against this packet.
The receipt does not embed its own future commit hash or invent a PR number.
Hosted CI is **NOT_ASSERTED_IN_SOURCE_RECEIPT**: resolve the actual commit's checks
from GitHub metadata separately. Local execution preceded publication and remains
bound to the exact executed source hashes.

## Admitted source and behavioral scope

| Existing owner / fixture | Change or responsibility |
| --- | --- |
| `M3FilteredViews.java` | Repair only the readonly navigable-set atom that distinguishes an actual stored `null` candidate from absence. |
| `M3IndexPathMasks.java` | Reject `scanWindow` output-array identity with either `requiredMasks` or `forbiddenMasks` before mutation or native dispatch. |
| `M3FilteredSetNullContract.java` | Deterministic independent set oracle and baseline failure witness. |
| `M3FilteredSetNullTest.java` | JUnit adapter for the set oracle. |
| `M3PathMaskAliasContract.java` | Deterministic Java/native/missing-native mask oracle and baseline failure witness. |
| `M3PathMaskAliasTest.java` | JUnit adapter for the Java mask oracle. |

The four fixtures were admitted first. `M3FilteredViews` and then
`M3IndexPathMasks` were each materialized and qualified serially. Four new test
files add two JUnit test methods to the existing 232-test module. The two
production rows are `EXISTING_OWNER_REFINEMENT_NO_RENAME`; the four fixture rows are
`ADDITIVE_TEST_ONLY_NO_RUNTIME_OWNER`. This local source receipt is the receiving
mapping and publication allowlist. Existing global maps and historical receipt
hashes retain their original meaning.

### Interior null in readonly navigable sets

A null-admitting comparator can place `null` between two non-null extrema.
Navigation may then return `null` because the stored element is the candidate,
even though `null` is also the API's absence sentinel. The old atom required a
null extreme and could stop before reaching a retained predecessor or successor.

The accepted atom preserves the existing comparator, emptiness, first-element and
conditional last-element probe order. If an extreme is null, its original null
comparison is preserved. If both extremes are non-null, comparison of the stored
directional extreme with the query determines whether a candidate exists in that
direction. This supports an interior null without introducing a fabricated null
probe for a null-rejecting comparator. The surrounding navigation and filtering
algorithm continues to operate on stored representatives and the existing live
view.

The oracle covers ordinary and null orderings, comparator-equivalent aliases,
bounded and descending views, the readonly facade, composed collection views,
live changes, immutability, stored-representative predicate behavior and callback
failures within the owning contract. The existing bounded-navigation oracle is a
separate control. The additional decision requires constant auxiliary space and
a bounded number of comparisons; it does not change the underlying owner's
navigation complexity. For tree-backed owners the existing bound/scan shape is
retained. No speed or memory improvement is claimed from these finite checks.

### Path-mask output aliases

An output buffer that is also a required or forbidden mask array can overwrite
the filter used by a later window and can expose partial mutation. The two new
identity checks reject that input with `IllegalArgumentException`. Existing
shape, index and output-window validation precedence remains in place; rejection
also applies to zero-capacity and no-op windows. Existing row, physical-offset and
state-plane alias checks remain authoritative.

The oracle exercises 32-bit mask values, signed strides, offsets, duplicate row
identities, multiple state planes, stable output order and resumable windows. The
extra guard is constant work and space. Input arrays retain their existing
stability requirement during a call. The existing JNI kernel is reused with no
native source, native symbol, public API or runtime owner added. Java predicates
and comparators remain on the Java side of the existing boundary.

## Rejected generic navigation candidate

The proposed generic readonly-map and mutable-set navigation replacement was
**REJECTED_SEMANTIC_ADMISSION**. Its eager empty-owner admission guard evaluated a
nested predicate outside the requested navigation range. For example, an outer
`lower(0)` over an inner filtered view of `[1, 3, 5]` must not acquire a new call to
an inner predicate that throws on `1`. Replacing the empty check with membership
testing still probes the excluded query key for strict `lower(1)`.

The generic public navigable interface does not expose predicate-free hidden
bound admission. Self-comparison alone cannot preserve the historical empty,
bounded wrong-domain exception, while catching arbitrary `IllegalArgumentException`
cannot distinguish a valid range rejection from a comparator failure. No such
heuristic was admitted. Readonly-map and mutable-view algorithms retain their
original implementations. The stronger rejected oracle was retained as canonical
research; it was not weakened or installed as a failing product test.

The rejected evidence archive is identified only by its SHA-256 manifest seal,
`df28ea858e0e1ebc438f6460f681430ace37feea48abe97bf3390c55c82eba85`.
Its implementation and private research contents are not part of this receiver.
SDK syntax, replay and partial-state success did not establish semantic admission
for that candidate. Historical stopped gates are kept in
[VERIFICATION.tsv](VERIFICATION.tsv). A resource-withdrawal discrepancy also
stopped qualification: the observed active resource inventory still had ten
manifests after an earlier deletion report. The final serial controller checked
the exact six-dataset frontier before continuing.

## Canonical custody and ordered receiving

The named recipe is `com.synexia.rewrite.m3.NavigationMask20261018`. The user-named
authority `hsoliwal/com.synexia` owns reusable recipes, fixtures and convergence;
M3JDK21 owns target acceptance. [RECIPE_PIN.json](RECIPE_PIN.json) records minimal
identity metadata for the unchanged canonical engine, descriptor, six manifests
and executed results. [evidence/sdk-summary.json](evidence/sdk-summary.json)
records the real SDK's exact outputs, fixed point, named discovery, all 64 partial
states, reversed order and exercised refusal cases.

The actual canonical GitHub head is
`8d465b3383b5b3a16cc43af5372e7c21d69cd42e`, tree
`7d92a0415bee9c2d776a4442c6b8678bff9cf619`. The publication controller verified
that all 80 materialized files equal that declared head and that its GitHub tree
equals the local Git tree. All 32 executed inputs remain exact. The unchanged
canonical checker passed for 80 changed files with zero violations. The 78-entry
task manifest seal is
`4f6e0c1a340e5595609f558915b7381c1008d26866a65dd76aee0f3efb23152c`;
the 80-entry publication manifest seal is
`4de573ab62d15ad63dbcbdcc4cfa5e4652fce8daf6da60e3fe0307805e8953b0`.
These are opaque custody identities; no private canonical PR URL or private
implementation is exported.

Regeneration requires authorized access to the canonical artifact. This public
packet contains no engine implementation, private source inventories, private
commit bodies or restricted repository URLs. Target compilation and runtime
verification use the received Java files without a Synexia runtime dependency.
Source intake is `INDEPENDENT_APACHE_MODULE`; these bounded identity and result
records are `DATA_OR_METADATA_EXPORT`; canonical authoring uses
`EXTERNAL_RECIPE_TOOL`.

The existing ordered receiving plan remains **String → arrays → collections →
AST/compiler → remaining families**. Its active phase is **STRING**; arrays and
collections remain `WAITING_FOR_PREDECESSOR`. This independent module repair does
not promote a family, the repository, an optional `jdk.m3.collections` publication
pack, or an installed JDK image. The relevant existing authorities are the
[porting invariant](../../../docs/m3jdk21-porting-invariant.md),
[naming and receiving map](../../../docs/name-mapping.json),
[intake policy](../../../docs/synexia-intake-policy.tsv) and
[collection product scope](../../M3_COLLECTION_PRODUCT_SCOPE.md).

## Evidence and reproducible target checks

[VERIFICATION.tsv](VERIFICATION.tsv) distinguishes historical stopped gates,
expected-negative baseline witnesses, the final serial gates, native preparation,
runtime cases and API equality. [evidence/receiver.log](evidence/receiver.log) is
an explicitly labelled public projection of actual output. Retained lines are
exact; command-path substitutions and JSON field projections are labelled
separately and bound to their original receipt hashes. The canonical SDK log is
represented by metadata and selected result lines only.

The host used JDK `21.0.12.1`, GCC `13.3.0`, strict Java compilation and eight
strict C17 UBSan libraries. The runtime matrix has **25 loaded-native**, **two
Java-only**, and **one missing-native** processes, all passed with checked JNI.
The new native mask oracle reports 34,244 checks; the unavailable-native oracle
reports 50 checks. Native library binaries are host-specific evidence identities,
not portable artifacts included in this packet.

Run the following from the repository root. `JAVA_HOME` must identify a real JDK
21 installation. `M3_SOURCE_MAVEN_REPO` must identify a repository containing the
genuine source-built dependencies required by the unchanged module POM, including
`org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1` and its matching test platform.
That custom coordinate is a prerequisite, not an instruction to substitute a
stub or silently change the POM to a central-release version. The recorded SDK
used the real OpenRewrite 8.17.1 implementation; target-only verification does
not require that SDK on the product classpath.

```bash
mvn -f m3/collections/pom.xml \
  -Dmaven.repo.local="${M3_SOURCE_MAVEN_REPO:?source-built dependency repository required}" \
  -DskipTests clean test-compile
mvn -f m3/collections/pom.xml \
  -Dmaven.repo.local="${M3_SOURCE_MAVEN_REPO:?}" test
M3_RECEIVER_CP="m3/collections/target/classes:m3/collections/target/test-classes"
"${JAVA_HOME:?}/bin/java" -ea -cp "$M3_RECEIVER_CP" \
  com.m3.collections.M3FilteredSetNullContract --all
"$JAVA_HOME/bin/java" -ea -cp "$M3_RECEIVER_CP" \
  com.m3.collections.M3BoundedFilteredNavigationContract
"$JAVA_HOME/bin/java" -ea -cp "$M3_RECEIVER_CP" \
  com.m3.collections.M3PathMaskAliasContract java
```

The following Linux command sequence recreates the affected JNI library from the
unchanged public C source and a header generated from the received Java owner.
It is a path-substituted, affected-library reproduction of the recorded strict
native build, not a claim to reproduce all eight libraries or all 28 processes.
Use a fresh output directory and GCC with the UBSan runtime installed.

```bash
M3_RECEIVER_NATIVE="$(mktemp -d)"
mkdir -p "$M3_RECEIVER_NATIVE/headers" "$M3_RECEIVER_NATIVE/classes" \
  "$M3_RECEIVER_NATIVE/libraries" "$M3_RECEIVER_NATIVE/empty" "$M3_RECEIVER_NATIVE/tmp"
"${JAVA_HOME:?}/bin/javac" -J-XX:-UsePerfData --release 21 \
  -proc:none -implicit:none -Xlint:all,-module -Werror -sourcepath '' \
  -classpath m3/collections/target/classes \
  -h "$M3_RECEIVER_NATIVE/headers" -d "$M3_RECEIVER_NATIVE/classes" \
  m3/collections/src/main/java/com/m3/collections/M3IndexPathMasks.java
gcc -std=c17 -O2 -g -Wall -Wextra -Werror -Wconversion \
  -Wsign-conversion -Wshadow -pedantic -fPIC -fsanitize=undefined \
  -fno-sanitize-recover=all -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
  -include "$M3_RECEIVER_NATIVE/headers/com_m3_collections_M3IndexPathMasks.h" \
  -shared m3/collections/src/main/native/collections/m3_index_path_masks.c \
  -o "$M3_RECEIVER_NATIVE/libraries/libm3_collections_pathmask.so"
nm -D --defined-only "$M3_RECEIVER_NATIVE/libraries/libm3_collections_pathmask.so"
UBSAN_OPTIONS=halt_on_error=1 "$JAVA_HOME/bin/java" -ea -Xcheck:jni -XX:-UsePerfData \
  -Djava.io.tmpdir="$M3_RECEIVER_NATIVE/tmp" \
  -Djava.library.path="$M3_RECEIVER_NATIVE/libraries" -cp "$M3_RECEIVER_CP" \
  com.m3.collections.M3PathMaskAliasContract native
UBSAN_OPTIONS=halt_on_error=1 "$JAVA_HOME/bin/java" -ea -Xcheck:jni -XX:-UsePerfData \
  -Djava.io.tmpdir="$M3_RECEIVER_NATIVE/tmp" \
  -Djava.library.path="$M3_RECEIVER_NATIVE/empty" -cp "$M3_RECEIVER_CP" \
  com.m3.collections.M3PathMaskAliasContract missing
```

The expected path-mask JNI export is
`Java_com_m3_collections_M3IndexPathMasks_selectNative`. The `native` mode asserts
actual availability; `missing` asserts absence and native-required refusal.
An empty `java.library.path` must remain free of the library under test.

For the two admitted negative controls, compile the original production snapshot
at `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49` into a separate
`M3_BASELINE_CLASSES` directory using the same strict Java toolchain, then run the
new fixtures against those original classes. Both commands are expected to exit
with `AssertionError`; that expectation applies only to these negative controls.

```bash
"$JAVA_HOME/bin/java" -ea \
  -cp "${M3_BASELINE_CLASSES:?original production classes required}:m3/collections/target/test-classes" \
  com.m3.collections.M3FilteredSetNullContract --baseline-interior-null
"$JAVA_HOME/bin/java" -ea \
  -cp "${M3_BASELINE_CLASSES:?}:m3/collections/target/test-classes" \
  com.m3.collections.M3PathMaskAliasContract witness
```

Observed baseline messages were `interior-null candidate must continue to retained
predecessor expected=-1 actual=null` and `required mask alias did not refuse`.
Do not count an unrelated compile or classpath failure as an observed witness.
The publication packet was assembled from completed qualification records; its
author did not rerun or invent runtime results while projecting this evidence.

From the repository root, check the bounded packet and all six received files:

```bash
sha256sum -c m3/collections/qualification/readonly-set-null-mask-alias-20261018/SHA256SUMS
```

`SHA256SUMS` covers the six admitted Java files, the category table and all packet
payloads except the manifest itself. The canonical and catalogue pins are sealed
here; receiver commit/PR identity is resolved from the containing Git/GitHub
metadata. No self-referential digest is required.

## Catalogue and convergence workspaces

The [five-row decision table](../../algorithm-catalog/readonly-set-null-mask-alias-20261018.tsv)
uses the existing
[candidate-comparison schema](../../algorithm-catalog/collection-candidate-comparison-20261017.tsv).
Existing [path-mask comparisons](../../algorithm-catalog/index-path-masks-20261017.tsv)
and [filtered-view receiving evidence](../../docs/FILTERED_SORTED_VIEWS_RECEIVING_20261017.md)
remain applicable. Search the existing category tables with, for example:

```bash
rg -n 'FILTERED_NAVIGATION|ROW_MASK_FILTER|JNI_OBJECT_PREDICATE' m3/collections/algorithm-catalog
```

The separate public challenge/fork catalogue packet is sealed by manifest SHA-256
`5c0c71817c392c6b79d0536c4aa4c927e9142aad2c5a390f3e8518b1b7b3d36c`.
The catalogue is reviewable in [PR #1323](https://github.com/hsoliwal/M3jdk21/pull/1323) at
head `08181cf261f3b9224ec6916e352435ed0494a9a7`. Its
[immutable public tree](https://github.com/hsoliwal/M3jdk21/tree/08181cf261f3b9224ec6916e352435ed0494a9a7/m3/collections/algorithm-catalog/challenge-forks-20261018)
is pinned in `RECIPE_PIN.json`, independently of whether that separate PR has
merged. It inventories 9,996 public donor blobs, including 6,423 Java files and 3,186 READMEs,
and records five source-reviewed reference comparisons plus screened public fork
metadata. Inventory rows remain `NOT_EVALUATED`; metadata verification is not
algorithm admission, performance qualification or completion of LeetCode,
HackerRank, GeeksforGeeks or the wider M3 algorithm programme.

**Public forks may be convergence workspaces.** Parent/source identity, exact
branch head, applicable license and actual changes must be recorded before reuse.
Workspace status alone admits no implementation. The separately screened public
fork rows carry that discovery evidence; private fork contents are not exported
by this receiver.

## Explicit remaining qualification

The native receipt preserves three specific gaps: no production bitlane
loader/runtime proof; the BiMap native lane lacks its POM/header/interop
prerequisites; and long-path JNI coverage is absent from this eight-library
matrix. Other native platforms, the full reactor, the optional publication pack
and installed-image delivery were not qualified here.

A custom M3 JDK image, jtreg/JCK, interpreter/C1/C2/GC/CDS behavior and representative
CPU, retained-heap and native/process-memory benchmarks remain outside the
executed evidence. Generic map/mutable navigation exception repair remains
deferred. No family or repository completion is claimed. The receiver's exact
commit and PR are resolved from the containing Git/GitHub metadata. Hosted CI is
not asserted by this receipt and remains distinct from these completed local
owning gates.

All six received files retain their original qualified copyright and license
bytes. Newly authored receiver records are Apache-2.0 first-party expression;
the module's [LICENSE](../../LICENSE) and [NOTICE](../../NOTICE) remain applicable.
No third-party donor source was newly copied into the two production refinements.
