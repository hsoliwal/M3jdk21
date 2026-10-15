<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# Qualified M3 regex effect images

This is an explicit, bounded metadata export/restore API on the existing
`jdk.internal.mindex.M3RegexDpPlan` and its existing `Effect`. It is not a full
Synexia image import, Java serialization, automatic filesystem cache, or complete
M3String product qualification.

Source parent: `hsoliwal/com.synexia@f6dfd4d1ea9902e97f7e257f023ec78ce759447e`.
Target parent: `hsoliwal/M3jdk21@d6cae6863b754f3fc1f1ec5a47bb275a7951bc45`.
Canonical recipe: `com.synexia.rewrite.M3Jdk21RegexImageRecipe` in Synexia.

## Responsibility and trust

`Effect.toImage(byte[32], Runnable)` creates caller-owned bytes. The producer supplies
an exact, immutable canonical-source fingerprint, including its generation/range
meaning. The API does not read source text, mint canonical identities, or store a
source array. The separately qualified producer stores SHA-256 of the ENTIRE image
in a trusted receipt. A source-text hash alone does not attest to effect correctness.

`plan.restoreEffect(ByteBuffer, byte[32], byte[32], Runnable)` consumes the selected
remaining buffer region, the expected canonical-source identity, and that TRUSTED
whole-image digest. It restores under the receiving plan's identity only when all
bytes, semantic version, derived program fingerprint, source identity, geometry,
flags and lanes are valid. The current plan must have identical primitive predicates
and transitions. Equivalent fresh plans may receive an image; a different program
with the same state count must not. Restored effects retain the existing per-plan
composition ownership checks.

**The digest must not be accepted merely because it is stored alongside untrusted
image bytes.** A malicious producer can generate valid rows and a matching digest.
These checks establish integrity, context binding and structural admission, NOT a
proof that an arbitrary effect corresponds to a claimed source. Receipt production,
authentication/signing, distribution and attachment to actual canonical String
owners are still responsibilities of a future qualified cache/compiler receiver.
Neither a checksum nor an image is a replacement for that qualification.

## Version 1 layout

All integers and rows use little-endian order, independent of the caller's buffer.
The total bound is 104 + 2*64*8 = 1128 bytes. No arbitrary-sized metadata is accepted.

| Offset | Bytes | Field |
|---:|---:|---|
| 0 | 8 | Domain magic 0x31464f455852334d |
| 8 | 4 | Semantic/image version 1 |
| 12 | 4 | Header size 104 |
| 16 | 4 | Complete image size |
| 20 | 4 | Receiving plan state count |
| 24 | 4 | Bit 0: leading-low alternative; bit 1: deferred trailing high |
| 28 | 4 | Reserved, must be zero |
| 32 | 8 | Nonnegative logical UTF-16 length |
| 40 | 32 | SHA-256 of exact versioned derived program metadata |
| 72 | 32 | Producer-supplied canonical source identity |
| 104 | q*8 | Open transition rows |
| 104+q*8 | q*8, optional | Relation omitting the initial low surrogate |

The program fingerprint includes semantic version, state geometry, literal masks,
dot/star masks, initial/accepting states and nullable bridges, not object addresses.
A future semantic or representation change must update image-version admission.
There are no untrusted Java class names, object deserializers or native addresses.

Restore snapshots the bounded region before inspecting its contents. Returned effects
own new primitive rows: they do not retain buffers, mappings, receipt arrays or source
text. Closing the input channel after restore is safe; the mapping must remain readable
during the call. Snapshot integrity checks do not promise synchronization or safe
unmapping of an actively accessed mapping. Caller position/limit/mark/order are not
changed by the API. Supplied callbacks may of course change caller-owned objects.

Rows must fit the receiving state universe. Zero-length effects must be identities
without surrogate flags; single-unit boundary geometry and the leading-low alternative
are checked. Invalid bytes, unknown versions, truncation, trailers, mismatched receipts,
programs or sources are refused, never treated as a negative regex match. Source and
receipt identities are cloned before callbacks. Both APIs call an optional checkpoint
before bounded work and before publishing the result; interruption status is untouched.

## Existing runtime and JNI remain authoritative

Export/restore adds two internal Java methods, but no instance fields or retained cache.
It preserves the existing ASCII literal/dot/star grammar, UTF-16 seam algebra, public
String/Pattern/Matcher APIs and older method contracts. Native source and the three
composition/power symbols are unchanged. A restored effect composes and repeats through
those same Java/JNI implementations. No parallel native codec is added for a <=1128-byte
format without evidence that a second implementation is worthwhile.

The implementation independently adapts the image geometry, fingerprint and lane
admission responsibilities reviewed in Synexia `MIndexRegexAtomAlgebra.write/map`.
Its format is intentionally NOT binary-compatible with that richer table format,
which also carries prefix/restart/search-hit lanes. Existing JDK ByteBuffer,
FileChannel and MessageDigest APIs perform byte/mapping/hash work. No challenge
solution or Apache implementation body is transplanted into this GPL target.

## Qualification and open work

The paired source task runs roundtrip, deterministic reexport, arbitrary-frontier,
UTF-16 seam and repeated-power tests; hostile byte/header/geometry/identity cases;
position/limit/order/mark and input mutation tests; checkpoints and concurrency;
actual JNI use of restored rows; fresh JVM producer and read-only mapped consumer;
previous UTF-16 and native regression suites; exact-source recipe replay and mutants.
Tests use the actual generated plan and existing native implementation. They do not
install an automatic disk cache or prove native M3String/HotSpot owner lifecycle.

Whole compiled parser/table images, authenticated receipt delivery, automatic
canonical-owner attachment and eviction, full Maven/OpenRewrite SDK/reactor/JDK builds,
real M3/CompactStrings jtreg, Windows/MSVC, strict LRU/payload spill, compiler lowering,
DOOP/SWT/Swing and product performance remain outside this completed component scope.
