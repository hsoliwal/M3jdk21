# JEP 497 — ML-DSA inventory and dependency closure

Status: inventory/dependency packet only. No product source materialization or compatibility completion claim.

## Authorities

- JEP: 497 — Quantum-Resistant Module-Lattice-Based Digital Signature Algorithm
- implementation issue: JDK-8298387
- implementation commit: `8b98f958dc1afedc02b9d9c98089d6cb1ca3a5b7`
- implementation parent: `21e0fb8648d61f041a04d44ad6c46fc5efd86261`
- released donor state: OpenJDK JDK 24 GA tag `jdk-24+36`
- released donor commit: `6705a9255d28f351950e7fbca9d05e73942a4e27`
- M3JDK21 product baseline for this packet: `3df19b6f064405d787fec4f9065328003d745aff`

## Released-state rule

The implementation commit is provenance, not the final postimage authority.

Before JDK 24 GA:

- JDK-8345057 reconciles ML-KEM named parameters after ML-DSA integration;
- JDK-8345533 switches the implementation to the FIPS 204 final specification;
- shared `NamedParameterSpec`, `KnownOIDs`, provider registration, deterministic tests and
  implementation classes converge further.

Three ACVP `internalProjection.json` files from the implementation commit are absent at GA and are
not silently retained.

`GA_PATH_STATE.tsv` records all ten implementation-commit paths and their exact JDK24-GA state.

## Shared-owner relationship with JEP 496

JEP 496 and JEP 497 share released-state ownership of:

- `java.security.spec.NamedParameterSpec`;
- `sun.security.util.KnownOIDs`;
- provider regression infrastructure.

The two feature packets may have independent FILE atoms for feature-private classes, but shared
owners must converge to one released-state postimage before MODULE composition.

## Java 21 compatibility boundary

JEP 497 is a security-library feature, not a Java grammar change. It still requires explicit
Java21 compatibility review before product materialization.

The next pass must prove:

1. required JCA/signature abstractions already exist or are dependency-packeted;
2. JDK24-GA sources parse/compile under the Java21 baseline after explicit adaptations;
3. FIPS 204 final semantics, provider names, OIDs and deterministic behavior are preserved;
4. each surviving GA path is one source-sealed FILE atom;
5. shared-owner atoms reconcile with JEP 496 rather than compete;
6. Java oracle precedes any AArch64/x86 intrinsic acceleration.

## Required product proof before promotion

- exact FILE recipe replay and fixed point;
- java.base release build;
- focused security/provider/signature jtreg;
- FIPS 204 vectors and negative tests;
- provider/OID/key encoding compatibility;
- existing signature/security regression suite;
- JNI/intrinsic parity only if a native optimization packet is later admitted;
- whole-JDK build/jtreg compatibility gates.

Promotion remains `NOT_AUTHORIZED`.

## Shared-owner split

Whole-file JDK24-GA snapshots are permitted only for feature-private/new files. These existing
shared owners require targeted semantic OpenRewrite recipes against the exact JDK21 preimage:

- `java.security.spec.NamedParameterSpec`;
- `sun.security.provider.SunEntries`;
- `sun.security.util.KnownOIDs`;
- the shared deterministic provider regression test.

`SHARED_OWNER_PATHS.txt` is authoritative for that lane. This prevents unrelated post-21 changes
from broad GA files entering the Java21 receiver accidentally.
