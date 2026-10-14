# JNI String ingress into canonical MIndex storage

## Current target contract and reading order

The 2026-10-02 narrative below is retained as historical ingress evidence. Its
`MIndexString`, `String.mindex` and canonical Compact-String array descriptions are
superseded for the current target. The donor name remains valid in provenance; the
current implementation names are `java.lang.M3String`, `String.m3`, `ensureM3()` and
`M3StringPool`. Do not globally rename donor records or rewrite historical receipts.

Source landmarks:

- [`jni.cpp`](../../src/hotspot/share/prims/jni.cpp): `m3_admit_jni_string`, String
  constructors, acquire/release pairs, region copies and critical access.
- [`String.java`](../../src/java.base/share/classes/java/lang/String.java):
  `m3AdmitNative`, `ensureM3` and the VM-visible compatibility field.
- [`M3String.java`](../../src/java.base/share/classes/java/lang/M3String.java):
  owner/coordinate, `materialize` and `compatibilityValue`.

### Construction and ownership

`NewString` and `NewStringUTF` first use the existing HotSpot construction helpers.
Before creating the returned local JNI reference, the VM calls `m3_admit_jni_string`.
It returns early for null input, disabled `UseM3StringStorage`, incomplete module
initialization or an uninitialized `M3String` class. The Java helper then applies
`ensureM3()`'s boot/admission/nonempty gates.

On successful admission, the helper returns `M3String.compatibilityValue()`, a shared
empty VM-layout sentinel. HotSpot installs that `byte[]` in `String.value`; the text
remains behind the canonical owner/coordinate. On refusal or unavailable admission,
it returns the ordinary backing. This does not transfer ownership of a caller's
native input pointer or let a native pointer masquerade as a Java array.

### Acquire, use, release

| JNI boundary | Source behavior | Caller/lifetime obligation |
| --- | --- | --- |
| `GetStringChars` / `ReleaseStringChars` | Allocates a native UTF-16 copy and frees it on release | Check failure; pair the matching release; do not retain the pointer afterward |
| `GetStringUTFChars` / `ReleaseStringUTFChars` | Allocates a native modified-UTF-8 copy and frees it on release | JNI modified UTF-8 is not an ordinary UTF-8 file format; do not free via an unrelated allocator |
| `GetStringRegion` / `GetStringUTFRegion` | Copies the requested region to caller-provided storage | Caller owns and sizes the buffer; validate UTF-16 range and encoded-byte capacity separately |
| `GetStringCritical` / `ReleaseStringCritical` | Copies whenever M3 is enabled or text is Latin1; otherwise pins the UTF-16 value array | Pair the critical release and obey JNI critical-region restrictions even when the implementation copies |

The critical acquire/release branch deliberately uses the storage flag, not just the
presence of a descriptor: lazy admission must not change who releases a pointer.
The stock pin path recovers the original array from the acquired address because
String deduplication could replace `String.value`. Never cache a native address as a
portable MIndex identity, free canonical storage when releasing a shadow, or retain
local JNI references across their scope without the appropriate JNI reference owner.

Lengths for Java text are UTF-16 code units. Embedded NUL, supplementary characters,
isolated surrogates, empty strings and split-surrogate regions need explicit regression
cases. Match the exact JNI encoding contract; a standard UTF-8 conversion is not an
acceptable replacement for `NewStringUTF`/`GetStringUTFChars` semantics.

### ABI and verification boundary

Keep public JNI signatures and Java API behavior intact. Any change to the descriptor,
field layout, VM symbols or String readers requires a matched Java/HotSpot image and
review of GC, intrinsics, CDS, serviceability and native consumers. The current enabled
experiment is interpreter-only and retains guards for incompatible facilities; see
[the build/configuration matrix](native-build.md#configuration-and-platform-evidence-matrix).

`run-api.sh` exercises default, interpreter, C2, enabled and enabled/noncompact modes
under checked JNI, but its C2 mode does not enable M3. Use
[the reproduction commands](native-build.md#build-and-test-a-complete-runtime) and
record exact-head output. The historical oracle count below is a script expectation,
not a claim that this documentation revision executed or passed the probe.

## Historical ingress design (superseded representation)

Date: 2026-10-02

## Purpose

The modified JDK already routes ordinary Java constructors and String operations through the
optional MIndexString storage layer after VM/module bootstrap. Before this increment, JNI
NewString(jchar*, len) and NewStringUTF(const char*) constructed an ordinary Compact-String value
and returned it with a null MIndex descriptor. A later Java method could attach MIndex lazily, but a
native-only round trip could remain flat indefinitely.

This increment closes that ingress asymmetry without introducing a second native interner.

## Contract

JNI construction remains two-phase:

1. HotSpot performs the existing OpenJDK String construction and validation.
2. When UseM3StringStorage is enabled, module initialization is complete, and MIndexString is
   initialized, HotSpot invokes the VM-private String.m3AdmitNative(String) helper.

The helper delegates to the existing String.mindex() admission path. That path retains the existing
MIndexString.admissionEnabled() and recursion guards and uses the existing MIndexStringPool as the
only canonical content authority.

The helper returns the canonical compatibility byte[] selected by the admitted storage. Before the
new String escapes JNI, HotSpot installs that array as String.value:

- a new local atom adopts the just-created Compact-String byte[] without another payload copy;
- an equal live local atom reuses the already-canonical local byte[];
- a mapped lexicon hit drops the temporary flat payload and uses the shared empty compatibility
  sentinel;
- empty/pre-bootstrap/disabled Strings retain ordinary OpenJDK behavior.

No java.lang.String contract is changed and no native pointer is forged into a Java array.

## Why this is the correct array boundary

The JNI char-array concatenation examples demonstrate how to construct a required flat Java char[]
by allocating one final array and copying independent ranges into it. MIndex uses that principle at
compatibility boundaries only. Canonical concat remains atom/range composition.

For JNI ingress the inverse rule applies: NewString still creates a valid ordinary String first, but
the temporary flat backing is replaced with the same canonical MIndex-compatible byte[] Java
constructors would have selected before the object becomes visible to JNI callers.

## Bootstrap safety

MIndexString activation occurs from System init phase 2 after java.base/module bootstrap. HotSpot
therefore refuses JNI admission unless:

- UseM3StringStorage is enabled;
- Universe::is_module_initialized() is true; and
- java.lang.MIndexString is initialized.

Strings created earlier in VM bootstrap keep the stock representation. This preserves the existing
startup circularity boundary.

## Proof

StringApiProbe inspects private String.mindex and String.value through the existing --add-opens test
boundary before calling equals, length, hashCode, or another method that could trigger lazy
admission.

In enabled modes and for non-empty results it requires:

- NewString output already has a non-null MIndex descriptor;
- a full round trip reuses the exact same canonical compatibility backing object as its source;
- NewStringUTF("jni-utf") is admitted before Java-level access;
- GetStringRegion -> NewString preserves immediate admission for non-empty regions.

The checks are deliberately outside the historical semantics counter/digest so the existing
808,685-check oracle remains directly comparable across default, interpreter, C2, enabled, and
enabled-without-CompactStrings modes.

## Verification boundary

Source review is not a build result. The increment remains draft until the exact-head OpenRewrite,
HotSpot/OpenJDK image build, JNI API probe, and existing runtime gates execute on a matched image.
