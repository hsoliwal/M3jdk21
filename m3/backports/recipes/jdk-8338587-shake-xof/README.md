# JDK-8338587 — Java 21 SHAKE/XOF compatibility packet

Status: **recipe candidate, unverified**. Product materialization is not authorized.

## Upstream authority

- issue: JDK-8338587
- commit: `c54fc08aa3c63e4b26dc5edb2436844dfd3bab7c`
- title: Internal XOF Methods for SHAKE128 and SHAKE256

The upstream commit is a nine-file change and assumes a later SHA3 internal representation based on
`long[]` state and VarHandle byte views. M3JDK21 still uses the Java-21 `byte[] state + long[]
lanes` implementation.

## Compatibility adaptation

The M3 candidate therefore does **not** replay the later SHA3 implementation wholesale.

It preserves:

- Java-21 byte-state SHA3;
- existing standalone `sun.security.provider.SHAKE256`;
- existing PKCS7/SignerInfo callers;
- existing SHA3 fixed-digest semantics.

It adds only the shared surfaces required by ML-KEM / ML-DSA:

- nested `SHA3.SHAKE128`;
- nested `SHA3.SHAKE256`;
- streaming `squeeze`;
- fixed-output SHAKE digest constructors;
- update-after-squeeze phase guard;
- public static `SHA3.keccak(long[])` over exactly 25 lanes.

## Recipe

`com.m3.rewrite.backport.M3Jdk8338587ShakeXofBackportRecipe`

Two Java targets:

1. current `SHA3.java` exact preimage -> Java-21-compatible XOF postimage;
2. additive `SHAKEXofCompatibility.java` jtreg.

No standalone SHAKE file is deleted.

## Proof

The focused test checks:

- SHAKE128 and SHAKE256 known-answer vectors;
- chunked squeeze versus fixed-output digest;
- nested SHAKE256 versus current Java-21 standalone SHAKE256;
- reset semantics;
- update-after-squeeze refusal;
- fixed-digest/squeeze mode refusal;
- second recipe pass fixed point;
- stale SHA3 preimage rejection.

## Consumers

JEP 496 and JEP 497 both depend on this packet. Their product recipes remain held until this XOF
candidate passes recipe JUnit, Java-21 build, focused jtreg, fixed point and canonical readback.

Promotion: **NOT_AUTHORIZED**.
