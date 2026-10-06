# JEP 496 / 497 post-quantum provider compatibility inventory

Status: **inventory / dependency proof only**. No JDK product source is materialized by this packet.

## Goal

Evaluate JEP 496 (ML-KEM) and JEP 497 (ML-DSA) as Java-21-compatible security-provider
enhancements while preserving the default Java SE 21 public API identity.

## Upstream provenance

- JDK-8318096 — AsymmetricKey public API prerequisite:
  `9123961aaa47aa58ec436640590d2cceedb8cbb1`
- JDK-8340327 — shared named-key/KEM/signature internal framework:
  `3f53d571343792341481f4d15970cdc0bcd76a5e`
- JEP 496 implementation:
  `13987b4244614d594dc8f94c288eddb6239a066f`
- JEP 497 implementation:
  `8b98f958dc1afedc02b9d9c98089d6cb1ca3a5b7`
- composition donor state: `jdk-24+36`

## Java 21 findings

Java 21 already contains:

- `javax.crypto.KEM` and `KEMSpi` (JEP 452);
- `sun.security.util.RawKeySpec`;
- SHAKE OIDs in `KnownOIDs`.

Java 21 does not contain:

- `java.security.AsymmetricKey`;
- `NamedKEM`;
- `NamedKeyFactory`;
- `NamedKeyPairGenerator`;
- `NamedSignature`;
- `NamedPKCS8Key`;
- `NamedX509Key`.

The JDK-8340327 framework imports `java.security.AsymmetricKey`, therefore it cannot be copied
verbatim into the default Java21 surface without importing a post-21 public API.

## Adaptation law

Default M3JDK21 candidate:

1. do **not** add `AsymmetricKey` or change the public key interface hierarchy;
2. adapt the internal named-key framework to Java21 key interfaces/internal key types;
3. do **not** add JEP496/497 `NamedParameterSpec` public constants by default;
4. use JDK24 GA postimages for shared owners such as `KnownOIDs` and `Deterministic.java`;
5. retain the provider algorithms through existing JCA/JCE string-based lookup;
6. run ACVP vectors plus existing KEM/Signature/KeyFactory regressions;
7. only after Java21 compile/test proof decide whether a separate opt-in public API packet is useful.

## Scope

Physical product scope: `MODULE (java.base)`.

Public API exclusions are `LIBRARY_API` review items and do not gain default mutation authority.

## Next pass

Author and test the Java21 adaptation of JDK-8340327 first. JEP496/497 materialization is blocked
until that prerequisite reaches compile/test fixed point.
