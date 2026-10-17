<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 purpose-collection wrappers — canonical Synexia donor recipe

Status: exact-source-sealed candidate for isolated M3JDK21 collections,
not an OpenJDK public API rewrite. The user requirement is
**M3JDK21 = compatible JDK21 superset + additive M3 capabilities**.
Never replace or narrow existing java.util, java.base, JNI, JVM contracts
or modify the JDK public API to install these first-party adapters.

## Avoid duplicating existing high-grade work

Reuses SIX existing first-party Java21 Synexia owners:
M3AdmissionWrappers, M3BoundedLists, M3CollectionWrappers,
M3ConcatenatedViews, M3FixedSizeLists, M3MappedViews.
Retains the first-party M3CollectionWrappersSelfTest and
M3CollectionWrappersTest. The selected BoundedLists is explicitly the
later #11828 source, fixing premature rejected-input materialization.
The selected ConcatenatedViews is the later #11803 source, fixing
Collection.size() saturation and untruthful SIZED when oversized.
Other owners come from merged Synexia #11796. All sources are pinned by
exact commit and Git blob in DONORS.tsv, with original Apache-2.0
headers. No third-party code is copied or relicensed.

Each target file is *only* the exact source package change
`package com.synexia.primitives;` ->
`package com.m3.collections;`; JNI test adds a single first-party
checked+readOnly composed filtered map primitive-shadow parity check.
No M3CollectionViews source or current map implementation is modified.
The public facade is additive to the target's existing collection module.

## Mandatory Maven/OpenRewrite recipe first

The canonical reusable `M3HashPinnedJavaSnapshotRecipe` is reused via
`com.synexia.m3.PurposeWrapperJdkReceiver20261017`. Manifest includes
10 ABSENT Java classes/test targets and ONE exact source-pinned
`M3MapCursorNativeProof` test-only replacement (11 total).
Templates, SHA256 receipts, Java/JNI proof, Maven JUnit real-source
replay/fixed point/partial-source/drift/duplicate/non-Java refusal,
catalogue and provenance are authored *before any target receiving*.
Any mismatch must be addressed by correcting the recipe/template,
rerunning and replacing generated output only with exact SDK Result
bytes, never hand-patching installed production targets.

The existing C17 `GetIntArrayRegion` JNI verification-only
implementation is reused byte-for-byte: Java owns predicates, reference
items, map and list lifetimes. No native predicate callbacks, GC pointer
retention, C collection store or new JNI export symbols.

## Executed local preflight (not SDK)

Original eight first-party donor Git blobs were reconstructed and
verified exactly (no incomplete Google Doc source reinterpretation).
After one exact package-name substitution, Java21 strict javac:
`--release 21 -proc:none -Xlint:all -Werror` passed for the six
owners, old/updated M3 wrappers and retained self-test.
Retained `M3CollectionWrappersSelfTest`:
`PASS M3CollectionWrappers checks=313767`.
Additional first-party checked purpose/concat/bound proof:
`M3_PURPOSE_WRAPPER_RECEIVER_PASS checks=22`.
Existing C17 fatal UBSan and Java `-Xcheck:jni`, now with purpose
composition: `M3_MAP_CURSOR_JNI_PASS checks=45`.
These are finite deterministic tests; original source/receiving SDK,
full Maven reactor, M3JDK build, JMH and provider-complete contracts
require separate executed acceptance.

## Contract caveats

- Static overload resolution matters: `readOnly(Collection)` does not
  guarantee preserving an erased runtime NavigableSet. Prefer the
  existing typed NavigableSet overload. This limitation is catalogued,
  not silently described as fixed.
- Read-only wrappers block modification through the facade but do
  not freeze external alias changes. Checked wrappers do not scan
  existing values. Synchronized wrappers require holding the same
  root monitor when iterating and during compound operations.
- Visibility-filtering and insertion-admission filters are different
  contracts. Live mapped views require stable mapping functions.
  Explicit snapshot APIs allocate copies; no blanket zero-allocation
  promise. Concatenation size saturates at Integer.MAX_VALUE.
- Ported `M3CollectionWrappers` is a first-party additive API; it
  does not establish universal compatibility with Guava, Eclipse,
  Commons, Spring, nor M3JDK21 installed java.base/jtreg/JCK.

See CATEGORY_REVIEW.tsv for per-category selection and rejected donors.
