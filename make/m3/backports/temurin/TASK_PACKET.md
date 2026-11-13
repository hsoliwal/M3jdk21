# Adoptium / Temurin donor intake

Status: evidence-only donor intake. No source-copy, distribution, compatibility or promotion authority.

## Exact pins

- adoptium/jdk21u master: bb6b279495ceff8442dbd0225756b92d466d5309
- adoptium/temurin-build master: cc31225e0aad72a5598d94e174cdd5c09e0d85a8
- adoptium/aqa-tests master: 6772f0835e011b175b4c1ad3a708f35912b6d249

The Adoptium JDK21u mirror pin is exactly equal to the canonical openjdk/jdk21u baseline pin already
used by the community-fork inventory. It therefore supplies mirror/release-distribution evidence,
not a unique HotSpot/JDK source optimization at this pin.

Temurin build orchestration and AQAvit test/performance mechanisms are separate tooling/test donors.
They must not be fed through the JDK source-fork cherry-equivalence algorithm.

## Licensing

- adoptium/jdk21u: OpenJDK GPLv2 source terms; path-level OpenJDK licensing still governs.
- temurin-build: repository LICENSE is Apache-2.0.
- aqa-tests: repository LICENSE is Apache-2.0.

License identity is evidence for review, not blanket permission to copy arbitrary third-party material
that those repositories may reference or download.

## Intended extraction

Temurin-build candidates:
- reproducible build orchestration and exact build-input recording;
- platform/toolchain/DevKit handling;
- packaging/SBOM/release evidence;
- source-repository pinning and verification.

AQAvit/aqa-tests candidates:
- interleaved baseline/candidate benchmark execution;
- performance-regression gates;
- exact test-material revisions;
- change-based test selection and platform matrix mechanics.

The default action is ADAPT/WRAP mechanism after bounded comparison with existing M3 build/test
owners. Do not replace OpenJDK configure/make with Maven and do not count already-upstream HotSpot
changes twice.

## Verification

Catalogue syntax -> pin/license tests -> mirror equality assertion -> downstream design review.
Any actual copied/adapted source requires a new source-bound packet and its own provenance/license
review.
