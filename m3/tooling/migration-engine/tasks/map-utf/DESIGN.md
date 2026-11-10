# MapUtf — bind mapped UTF8 to its canonical UTF16 owner

## Boundary and evidence
The programme remains the full M3JDK21 workstream. This leaf closes one concrete
text-ingress obligation; it is not ordinary String/HotSpot/JNI/CDS completion.
The immutable parent is PR99, commit 3a433416ae7981301844682896e5aefc6f0f8901.
An existing user Library MapFacts patch is recovered, source-checked and rerun
before extension. Its six scalar/alias fact guards are retained, not reinvented.

The actual reader validates CRC independently for UTF16 and sibling UTF8, but
neither successful CRC nor equal byte lengths proves those payloads encode the
same text. A well-framed, CRC-valid byte record can return unrelated bytes.
The unchanged original fixture writer defines this wire path using
String.getBytes(StandardCharsets.UTF_8). Unpaired UTF16 units remain lossless in
the canonical UTF16 payload; their UTF8 projection uses the encoder replacement,
not JNI modified UTF8, CESU8, WTF8, or normalization.

## Allowed change
Reuse java.base CharsetEncoder with REPLACE actions. On open, one 4096-byte
scratch buffer and one encoder are reused across scalar rows. Compare its bounded
output chunks directly to the mapped byte view; no flatten/materialize call and
no payload-sized temporary array. Existing verified UTF16 facts/CRC run first.
Aliases retain prior-row fact/handle validation and reuse the scalar admission.
Only private methods/imports/one constant in the existing backing owner change.
No new runtime registry, parser, public API, native ABI or retained field.

Malformed text-byte mismatches refuse with `text UTF-8 payload at OFFSET` before
row publication. This is an intentional corrupt-input repair, not a claim that
past malformed-input behavior was equivalent. Valid successful format and the
old tests stay locked. External rewrite/truncate of a mapped file is still
forbidden by the managed-file contract; this does not make mapping a sandbox.
The encoder adds a UTF16 traversal at admission, so no performance improvement
or zero-allocation claim is made.

## Recipe and gates
Configure the existing M3Jdk21HashPinnedSnapshotRecipe as MapUtf, with exact
pre/post images and ABSENT new-test admission. Keep MapGuard and MapFacts
postimages convergent; keep their original preimages and tests. Do not alter
Maven/POM/thresholds or bypass failed gates. Default methods/compare/null behavior
are unchanged by this leaf; broader contract review stays explicit.

Verify diff/static checks, strict Java21 compile, original tests, MapGuard,
MapFacts and MapUtf under normal/interpreter/noncompact/C2-focused modes.
Test malformed payloads after recomputing both CRC and FNV metadata, byte-count
mismatch, codec boundaries/surrogates, 4096-byte chunk seams and source immutability.
Recipe scheduler/JUnit and coverage are separate required unexecuted gates when
Maven/dependencies are unavailable. Native configure/make and jtreg require the
full matched JDK tree; a package patch on stock JDK21 is not that proof.
