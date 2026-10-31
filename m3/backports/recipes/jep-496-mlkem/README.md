# JEP 496 — ML-KEM inventory and dependency closure

Status: inventory/dependency packet only. No product source materialization or compatibility completion claim.

## Authorities

- JEP: 496 — Quantum-Resistant Module-Lattice-Based Key Encapsulation Mechanism
- implementation issue: JDK-8298390
- implementation commit: `13987b4244614d594dc8f94c288eddb6239a066f`
- implementation parent: `6d3becb486ab38c9c2d2a6fbc428bf794375317c`
- released donor state: OpenJDK JDK 24 GA tag `jdk-24+36`
- released donor commit: `6705a9255d28f351950e7fbca9d05e73942a4e27`
- M3JDK21 product baseline for this packet: `3df19b6f064405d787fec4f9065328003d745aff`

## Why the implementation commit is not the final packet

The JEP implementation commit touches ten paths, but later JDK 24 work changes shared security
owners before GA. In particular JEP 497 / ML-DSA and follow-up JDK-8345057 mutate
`NamedParameterSpec`, `KnownOIDs`, and the shared deterministic provider test.

Therefore M3 does not copy the implementation commit as an isolated ten-file snapshot.
The FILE pass must compare JDK21 against the JDK 24 GA cumulative state for every surviving path,
while retaining the implementation commit as provenance.

The two ACVP `internalProjection.json` files added by the implementation commit are not present at
JDK 24 GA and are recorded as `ABSENT_AT_GA`; they are not silently copied into the released packet.

## Initial implementation path denominator

The implementation commit touches exactly ten paths:

- 6 java.base production/provider/security paths;
- 2 ACVP data paths;
- 1 deterministic provider regression path;
- 1 microbenchmark path.

`GA_PATH_STATE.tsv` records the implementation-commit blob and the JDK 24 GA blob/presence for all
ten paths.

## Shared-owner dependency

Later dependency evidence is explicit:

- JEP 497 / JDK-8298387 — `8b98f958dc1afedc02b9d9c98089d6cb1ca3a5b7`;
- JDK-8345057 — `8c2b4f62714f26ab3bc4808c734502af632a1eef`.

A later JEP 497 packet must share released-state ownership of `NamedParameterSpec`,
`KnownOIDs`, and common provider tests instead of creating competing postimages.

## Java 21 compatibility boundary

JEP 496 is a security-library feature, not a Java grammar change. It is still not an automatic
default-Java21 backport.

Before product materialization the next pass must prove:

1. JDK 21 already supplies every required KEM/JCA/JCE abstraction or identify the exact dependency
   packet that must precede ML-KEM;
2. the JDK 24 GA Java sources parse/compile after explicit Java21 adaptations;
3. provider registration and standard-name additions do not break existing providers;
4. FIPS 203 vectors and deterministic tests are available in a released-state-compatible proof
   form even though two implementation-commit ACVP files do not survive to GA;
5. no post-21 language/class-file requirement enters the packet;
6. each surviving path is one source-sealed FILE atom before PACKAGE/MODULE composition;
7. Java oracle precedes any JNI/intrinsic acceleration;
8. later AArch64/AVX-512 ML-KEM intrinsics remain separate optional performance packets.

## Required product proof before promotion

- exact FILE recipe replay and fixed point;
- java.base release build;
- focused security/provider/KEM jtreg;
- FIPS 203 vector proof;
- serialization/key encoding/provider-name compatibility;
- security regression suite;
- applicable JNI/native parity only if a native/intrinsic lane is later admitted;
- whole-JDK build/jtreg compatibility gates.

Promotion remains `NOT_AUTHORIZED`.

## Shared-owner split

Whole-file JDK24-GA snapshots are permitted only for feature-private/new files. The following
existing shared owners are excluded from whole-file materialization and require targeted semantic
OpenRewrite recipes against the exact JDK21 preimage:

- `com.sun.crypto.provider.SunJCE`;
- `java.security.spec.NamedParameterSpec`;
- `sun.security.util.KnownOIDs`;
- the shared deterministic provider regression test.

This prevents unrelated post-21 changes in broad GA files from being absorbed accidentally.
`SHARED_OWNER_PATHS.txt` is authoritative for that semantic-recipe lane.
