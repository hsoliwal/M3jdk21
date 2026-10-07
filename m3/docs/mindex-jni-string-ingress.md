# JNI String ingress into canonical MIndex storage

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
