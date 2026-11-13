<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->

# Executed nonfinal Object.toAdpater qualification

This forward qualification preserves the implementation in PR #2320 and executes
it on a rebuilt M3JDK image. Reusable authoring and full receipts are published in
[Synexia PR #15542](https://github.com/hsoliwal/com.synexia/pull/15542), commit
`80b3629d883572012575234b9663abd29a37c82c`. The exact required method is:

```java
public <T> T toAdpater(Class<T> clazz) {
    return toAdapter(clazz);
}
```

Both spellings remain overridable. The inherited alias delegates once to the
selected canonical implementation without additional validation or casting.
Object's canonical implementation returns assignable receiver identity, supplies
the exact lazy M3Object capability, returns null for unsupported requests, and
rejects null. A subclass can define different null handling or result behavior;
the alias preserves the selected implementation. The corrected Javadoc states
that distinction explicitly.

The existing seven-entry Object vtable reservation remains intact. The historical
five virtual methods plus `toAdapter` and `toAdpater` must fit both ordinary and
array klasses. The source-built image contains this actual native layout.

## Changes in this follow-up

The existing `ObjectToAdpaterNativeTest.java` and
`libObjectToAdpaterNativeTest.c` owners gain broad coverage of both spellings,
exact generic metadata, all primitive array families and reference-array ranks,
class-loader identity, canonical and alias overrides, exception identity,
null-accepting overrides, unchecked generic results and checked Java consumers.
An interface declaring the abstract alias is satisfied by inherited Object
behavior. A class overriding both methods tests `super.toAdpater` followed by
virtual canonical dispatch, with independent invocation counters.

JNI uses the erased descriptor `(Ljava/lang/Class;)Ljava/lang/Object;` and the
virtual direct/A/V call families. Each call selects its method explicitly.
Repeated calls bound native local references and preserve pending exceptions.
No native Object method, receiver field, adapter registry or additional capability
owner is introduced.

The unchanged historical-subclass fixture retains its exact embedded Java 21
bytecode. The original `RECEIPT.json`, stock-JDK oracle, workflow and source files
for that bytecode remain historical evidence. The source verifier now seals this
reviewed successor's five product/test blobs. It remains a source-only gate; the
build and runtime evidence below is separate.

## Executed results

| Gate | Observed result |
| --- | --- |
| Existing Synexia SDK engines | Five admission outputs reproduce #2320; five enhancement outputs qualify the follow-up; exact bytes and empty second passes. |
| Final shared harness | Admission before/current, enhancement before/received, and original canonical dataset pass. Fresh output directories have exact manifest inventories; a refused source gate emits no output. |
| Source image | `make images JOBS=4` passes on the full pinned source with release/server warnings as errors, reusing successful baseline artifacts. Java base classes and the changed HotSpot owner are recompiled and complete images recreated. |
| Full runtime-source audit | All 23,505 archived `src` files checked; only the two admitted Object/classLoader postimages differ. |
| Actual candidate compiler and JNI build | Candidate `javac -h -Xlint:all -Werror` and strict C17 compilation pass with matching generated declarations and actual-image headers. |
| Six direct Java/JNI modes | Default, interpreted and forced C2, each with observation off/on and `-Xcheck:jni`: 10,770 assertions and 1,032,192 repeated calls. |
| Actual jtreg | Two tests, all eight actions pass: six enhanced native actions and two unchanged historical-binary actions. Native actions independently report 10,770 assertions and 1,032,192 repeated calls; legacy actions add 24 checks. |
| Additional historical binary | Forced C2 execution passes 12 checks without recompiling the historical class. |
| Explicit CDS | `-Xshare:on -Xlog:cds=info -Xcheck:jni`, observation enabled: the new archive opens and static regions map; 1,795 assertions and 172,032 calls pass. |
| Retained regressions | ObjectAdapterDispatchTest and enabled/disabled M3ObjectTest and M3ObjectNativeTest pass. The existing native event fixture's inclusion of bridge source does not prove installed-export binding. |
| Expected negative oracles | Old-image reflection reports the missing alias; the enhanced fixture fails at its first inherited-interface call. Incompatible typed assignment and matching interface defaults are rejected. Raw tokens are rejected by the strict warnings gate. |

Direct-run, jtreg and explicit-CDS counts describe separate executions. They are
not throughput measurements. No runtime patch was used for acceptance.

## Exact image and source identities

The receiving parent is
`008a84b7e5592888f78e67318a1c08863a517109`. The full source-build base is
`ce005d25b29fb6246e013bffb462afd5ed070362`. The two executed runtime postimages are:

```text
Object.java SHA-256
d825f24d0177ed49097b958dbee94a8303a3e03e9343693bb8e81165f8a75040

classLoader.cpp SHA-256
2bc21af7465fb483302dc69b1f9934b177f4dae72c59361d275ac8003fbe28af
```

The rebuilt image preserves the configure version label, so its actual contents
are identified by these hashes:

```text
lib/modules
78bcf89582f2a3258af6c2afa1628689b112c653d90b946e50abc15bebbcaa7f

lib/server/libjvm.so
e7a492b434d9f0509abf60d5a8cc8bb95a3ff3705a6a5e6d9a62fc0a3443d0d8

lib/server/classes.jsa
f5aff9e221299fb410c0932d3b379606da7170a6804b2add6d2959d50c75106e
```

The frozen historical subclass has SHA-256
`4eff8d88c8154820bee15152e0f2cb4791b305cc67b5675b34276310aa568e36`.
The standard and uncompressed-oop CDS archives were both generated. Explicit
native-suite archive use was measured only for the standard compressed-oop
archive.

[QUALIFICATION.json](QUALIFICATION.json) records all five source seals, image,
header and native-library identities, source-verifier result, counts and recipe
references. Reusable engines, templates, input manifests, donor evaluation and
detailed command/log receipts remain in the Synexia authoring crate. This product
tree receives the executed source/test outputs and passive qualification only.

## Compatibility, donor decisions and remaining gates

The newly qualified positive cases are a pre-existing public generic subclass
alias and an abstract interface declaration inherited from Object. A matching
interface default remains prohibited by Java's Object-method rules. Strict raw
token rejection is a warnings policy, not a statement that all raw calls are
language errors. Existing static, inaccessible or incompatible-return declarations
and all external dependencies have not been comprehensively qualified.

Eleven bounded donor decisions cover existing GitHub owners, Eclipse's typed
adaptation contract, official JNI/JLS references, LeetCode hash-table design,
HackerRank subtype/reflection categories and GeeksforGeeks adapter/hashing
categories. Relevant classification and reflection ideas become these tests.
Hash maps, caches and unrelated indexing algorithms require their own target
workloads. No challenge solution or editorial code is copied, and no speedup is
claimed.

The historical Object receiver still refuses its old source seal; the pinned
master already differs from that seal. The full naming receiver's known Arrays
companion hash mismatch also remains. These checks are preserved rather than
relabelled as passing. The method-only naming exclusion and scoped receiving-diff
check do not replace those repository-wide gates.

Qualification is limited to the listed Linux x86-64 image and tests. Hosted CI on
the published commit, the whole JDK suite, other platforms, every external
source/binary consumer, separate uncompressed-oop native/CDS execution, the full
canonical Synexia ledger and performance benchmarks remain separate gates. The
PR remains a reviewable draft until the applicable release gates are resolved.
