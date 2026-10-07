# Synexia shared-array/JNI source intake

Status: **pinned Apache custody and M3 target mapping only**. No java.base, HotSpot, or native JDK
implementation was changed by this intake.

## Exact donor

- repository: `hsoliwal/com.synexia`
- revision: `857c4cc0ddeb73e5ea5269a1d0ef2d30057dcfa8` (current `develop` at intake)
- family receiver: `INDEXSTRING` -> `com.synexia:synexia-m3index-jdk-bridge`
- target base observed: `ed5f4ed1771dbc0cf5cfc6d0944de2092326bd6d`
- source paths and Git blob identities: `SOURCE_PROVENANCE.tsv`
- semantic owner mapping: `TARGET_MAP.tsv`

The selected Apache-2.0 source files are mirrored at the exact source-relative path below
`m3/vendor/synexia/`. The original `THIRD_PARTY_NOTICES.md` is retained. The vendor copy is
source custody/proof; it is not a runtime dependency and is not claimed to be a complete standalone
Synexia module. In particular, the copied native CMake target has external Mapbox JNI header inputs;
those are not included in this scoped source slice, so this intake makes no native build claim.

## What maps to JDK names

- Synexia `SharedArrayNative.compare(SharedChars, SharedChars)` maps semantically to the public
  `java.lang.String.compareTo(String)` contract and the internal `java.lang.M3String` owner path.
- M3 keeps scalar payload addresses in `M3StringAtom.address` and packed range coordinates in
  `M3String`; tuples remain a persistent reference DAG. M3 does not receive Synexia's
  `ByteBuffer[]` descriptor-array owner.
- `copyTo` maps to caller-owned projections through `M3String.getChars/getBytes` and the existing
  JNI-created `nativeCharShadow/nativeByteShadow` boundaries. Hash/facts map to M3 owner facts;
  no new target route is admitted here.
- The target mapping deliberately uses JDK names. The copied donor source retains its original
  package/class names under vendor custody.

## Current-state correction

Synexia PR #9579 (`1a5f6c9abbf00887550f7c3d5647ebb20ef7ad99`) historically added a per-view owned
UTF-16 address directory and measured the compare path. Current `develop` has since moved forward;
at `857c4cc0ddeb73e5ea5269a1d0ef2d30057dcfa8`, `SharedArrayNative.compare` still calls
`SharedSegments.preparedBuffers()`, and native comparison resolves direct-buffer addresses during
the call. The historical cache is not present in this copied postimage. Its old measurements do not
prove an M3JDK speedup.

The current M3JDK target stores an address on each scalar `M3StringAtom`, while
`String.compareTo` still traverses code units through `M3String`. A direct address-plus-offset
fast path remains a target-specific candidate. It must preserve owner reachability, coordinate and
bounds checks, UTF-16BE decoding, equal-owner alias behavior, tuple fallback, and exact JDK return
values, then pass matched JDK build/runtime tests and end-to-end small/large/fragmented benchmarks.

## License boundary

The mirrored Synexia files keep Apache-2.0 and their original notices in
`m3/vendor/synexia/`. Existing `java.base` files keep their GPLv2-with-Classpath-Exception
headers. This intake neither changes those headers nor copies the Apache implementation into them.
