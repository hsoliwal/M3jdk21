# JEP 496 — ML-KEM intake packet

Status: inventory/dependency/security-proof packet. Product materialization is **not authorized**.

## Upstream authority

JEP 496 / JDK-8298390 adds the FIPS 203 Module-Lattice-Based Key Encapsulation Mechanism.

Integrated OpenJDK commits:

1. prerequisite named-key framework — JDK-8340327:
   `3f53d571343792341481f4d15970cdc0bcd76a5e`
2. prerequisite internal SHAKE/XOF surface — JDK-8338587:
   `c54fc08aa3c63e4b26dc5edb2436844dfd3bab7c`
3. JEP 496 implementation — JDK-8298390:
   `13987b4244614d594dc8f94c288eddb6239a066f`

The feature depends on both prerequisites. M3JDK21 must not absorb ML-KEM until the Java-21
named-key framework and the SHA3/SHAKE XOF adaptation are independently proven.

## Current M3JDK21 receiving state

The new named-key framework classes, ML-KEM implementation classes, and SHA3Parallel are absent on
current master. Java-21 also lacks the nested `SHA3.SHAKE256` XOF/squeeze API and public static
`SHA3.keccak(long[])` used by ML-KEM. It retains a standalone `SHAKE256` used by current
`PKCS7` and `SignerInfo`, so JDK-8338587 requires a compatibility-aware receiver adaptation
rather than blind upstream file deletion. Existing receiving owners such as KeyUtil, SignatureUtil,
SunJCE, NamedParameterSpec, KnownOIDs and Deterministic remain the Java-21 oracle and require
exact-delta adaptation rather than whole-file replacement.

The full receiving denominator is recorded in `SOURCE.tsv`: 10 prerequisite paths plus 10
JEP-496 paths.

## M3 preparation rule

Every Java target is prepared through the canonical A3 lane before source-changing recipe
application:

```text
exact file delta
 -> A3 atomize
 -> patternize / IOP
 -> document
 -> second-pass fixed point
 -> hash-pinned backport recipe
```

ACVP JSON data is not Java and remains in the source-sealed data lane.

## Security acceptance

Before any product materialization/promotion:

- prerequisite named-key framework Java-21 compile/tests;
- JDK-8338587 SHA3/SHAKE XOF compile + squeeze known-answer tests + current PKCS7/SignerInfo compatibility;
- exact source-21 compile of the combined closure;
- FIPS 203 ACVP key-generation and encap/decap vectors;
- ML-KEM-512/768/1024 round trips;
- PKCS8/X509 and key-factory refusal/validation behavior;
- SunJCE registrations, aliases, OIDs and provider ordering;
- deterministic provider regression;
- relevant java.base/security jtreg;
- full image/provider smoke;
- second-pass fixed point and canonical readback.

Native/SIMD acceleration is explicitly out of scope until the Java implementation is the proven
semantic oracle.

## Authority

This packet grants no source-copy, product-mutation or promotion authority. It exists so the residue
queue can advance from `NO_RECIPE_EVIDENCE` to `PACKET_EVIDENCE` while keeping the security gates
visible and fail-closed.
