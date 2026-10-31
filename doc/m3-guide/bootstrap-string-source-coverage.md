<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Bootstrap String source-file coverage

[Guide](README.md) · [Ownership](bootstrap-string-ownership.md) · [String contracts](bootstrap-string-contracts.md) · [Precompute contracts](bootstrap-string-precompute.md) · [Coverage](coverage.md)

This is an additive P01 coverage map for the **twelve physical M3-prefixed Java
source files** in `src/java.base/share/classes/java/lang` at inventory pin
`278b221e8365ebeb6095a3a5643b38a0564258b5`. It does not count nested classes as
extra source owners, and it does not count these implementation types as public
Java APIs. The original inventory is historical evidence, not rewritten here.

The ownership and String/facts references inspected six complete bodies at
`ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2`; the precompute reference inspected
the other six at `68bfe667943f4a231f3627b56d34b0be53a4e4af`. All twelve blobs
match their original inventory identities. The documentation/navigation base
for this increment is `1dce82bfb7f1058d74f7503c4cd54fa7d431c032`.

## Owner-to-contract map

Method/constructor/type totals below include explicitly declared private and
nested implementation members. They exclude inherited, implicit, synthetic and
compiler-generated record members. They measure declaration routing, not test
coverage or public API size. Fields and record components are counted separately
below; the contract pages describe their state/ownership and validation roles.

| Physical source owner | Git blob | Methods / constructors / named types | Contract reference |
| --- | --- | --- | --- |
| [M3CodeTextFacts](https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/src/java.base/share/classes/java/lang/M3CodeTextFacts.java) | `a24c1a7d9a3b73d4693b390387b7d9734f5423d8` | 25 / 3 / 3 | [Contracts](bootstrap-string-precompute.md#scalar-code-text-receiver-and-approximate-signatures) |
| [M3CodeTextSignalBatch](https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/src/java.base/share/classes/java/lang/M3CodeTextSignalBatch.java) | `99d084a47b36a2588bd79c850d7d647ffd98bd7f` | 3 / 2 / 2 | [Contracts](bootstrap-string-precompute.md#packed-lexical-rows-and-the-separate-provider-boundary) |
| [M3String](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java) | `42399d411c1516a56a47dcdb10242960157b4677` | 92 / 4 / 4 | [Contracts](bootstrap-string-contracts.md) |
| [M3StringAtom](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringAtom.java) | `7e7645497d08d55cf9b5174e4f04e11c87a06bae` | 19 / 1 / 1 | [Contracts](bootstrap-string-ownership.md) |
| [M3StringCodePointPrecompute](https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/src/java.base/share/classes/java/lang/M3StringCodePointPrecompute.java) | `6cbb277119f387aac772373d31621af64bea68d5` | 8 / 3 / 3 | [Contracts](bootstrap-string-precompute.md#exact-code-point-geometry) |
| [M3StringFacts](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java) | `d8dd7dc30db6a9e3897146572bd4a7755c954ffb` | 23 / 1 / 1 | [Contracts](bootstrap-string-contracts.md) |
| [M3StringOwner](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringOwner.java) | `acd2a9584c65c384ddebd9159a8775d196ea0f8c` | 9 / 2 / 2 | [Contracts](bootstrap-string-ownership.md) |
| [M3StringPool](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringPool.java) | `514247a2e6c3704e03a9b47682f74e481f66585f` | 54 / 6 / 8 | [Contracts](bootstrap-string-ownership.md) |
| [M3StringPositionPrecompute](https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/src/java.base/share/classes/java/lang/M3StringPositionPrecompute.java) | `c0074f4b144da23040e63e0ba8866d56d0d304ac` | 10 / 4 / 4 | [Contracts](bootstrap-string-precompute.md#lazy-block-position-precompute) |
| [M3StringRegexDp](https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/src/java.base/share/classes/java/lang/M3StringRegexDp.java) | `9a4df49bb40d3a9e3ff87df3c27dd5d9fffb36df` | 3 / 1 / 1 | [Contracts](bootstrap-string-precompute.md#conservative-dotstar-full-match-dp) |
| [M3StringSearchPrecompute](https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java) | `ac68eacd6ee4e6048ab424542ccf5c2cf5595908` | 23 / 6 / 5 | [Contracts](bootstrap-string-precompute.md#search-plans-and-exact-utf-16-matching) |
| [M3StringTuple](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringTuple.java) | `0d4cfb75317e68d54244588a474eba98bf7df27b` | 6 / 1 / 1 | [Contracts](bootstrap-string-ownership.md) |

The twelve-file declaration inventory contains **275 methods,
34 constructors, 35 named types (including the twelve outers),
229 fields and 6 record components**. The new six-file precompute portion
accounts for 72 methods, 19 constructors, 18 named types, 100 fields and three
record components. This document routes every physical owner; the linked
references cover each declared constructor/method and named helper. It does not
assert that every member has inline Javadoc or an executed test.

## Evidence levels that remain separate

- **Source-path inventory:** twelve M3-prefixed production files at the pinned
  scope. This is not the total number of Java files in java.lang.
- **Complete-body contract review:** all twelve now have a linked implementation
  reference, including their declared methods, constructors and named helpers.
  The earlier six-body/declarations-only statements remain descriptions of the
  preceding bounded passes; this map supplies their additive successor status.
- **Caller/dependency review:** selected String/System/native/HotSpot/M3TQ and
  standalone-provider boundaries only. It does not close the inherited regex package, P03
  internal backing, java.lang, java.base or repository dependencies.
- **Test source evidence:** the companion pages record the actual assertions,
  conditional branches, model-only proofs and source-observed gaps. Eight new
  precompute-related Java test bodies supplement the ten foundational Java
  bodies and native test helper; source inspection is not a test pass.
- **Executed qualification:** builds, repository Java/native tests, jtreg,
  policy/recipe verifiers, benchmarks and matched-runtime acceptance are
  **NOT_RUN by these documentation authoring passes**; hosted CI is separate exact-commit evidence. Source-observed import/test
  contradictions and allocation-fallback limits remain explicit in the
  [precompute reference](bootstrap-string-precompute.md#source-observed-gaps).

## Package documentation and remaining work

The inherited [java.lang package-info](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/package-info.java)
exists with blob `882a610c1a08e37cbea6c7b49dc23f8672237b53` and describes
general java.lang/encodings. The original M3-prefix selection excluded it, so
the earlier `native_package_doc=false` did not establish physical absence.
Physical package-info presence is true; M3-specific package-info prose is still
absent. The [additive correction](coverage.md#p01-inherited-package-documentation-correction-2026-10-14)
preserves that distinction.

Remaining work includes source-observed discrepancy resolution, exact-head
compilation and behavior testing, cache identity/eviction/concurrency/GC and
allocation-fault coverage, provider/native/platform qualification, cold/warm CPU
and retained/native/process-memory measurements, and full public contract and
inline/package-documentation review. The existing runtime image, JNI lifetime,
GC/JIT/CDS/SA and ordered receiving gates remain authoritative. No phase,
runtime acceptance or whole-package/module/repository completion advances from
this source-file documentation milestone.

The adjacent [P02 Pattern query reference](bootstrap-pattern-query-contracts.md)
has its own one-file adapter scope and selected caller/test evidence. It does not
add a thirteenth P01 owner or broaden this twelve-file map into all java.lang.
