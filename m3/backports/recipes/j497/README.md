# JEP 497 — ML-DSA intake packet

Status: Java-21 receiver candidate materialized from a hash-pinned recipe. Verification/promotion is **not authorized**.

## Upstream authority

JEP 497 / JDK-8298387 adds the FIPS 204 Module-Lattice-Based Digital Signature Algorithm.

Required correctness lineage:

1. shared named-key/signature framework — JDK-8340327:
   `3f53d571343792341481f4d15970cdc0bcd76a5e`
2. internal SHAKE/XOF surface — JDK-8338587:
   `c54fc08aa3c63e4b26dc5edb2436844dfd3bab7c`
3. JEP 497 integration — JDK-8298387:
   `8b98f958dc1afedc02b9d9c98089d6cb1ca3a5b7`
4. coexistence repair — JDK-8345057:
   `8c2b4f62714f26ab3bc4808c734502af632a1eef`
5. final FIPS 204 semantics — JDK-8345533:
   `fb95a5394413dba7352a7ad2ebd39a3da42308a6`

JDK-8345057 is required when ML-DSA and ML-KEM coexist because the original ML-DSA integration
temporarily removed the ML-KEM `NamedParameterSpec` constants. JDK-8345533 is required so the
receiving implementation follows final FIPS 204 rather than the earlier draft behavior.

Later performance work such as JDK-8347606 remains a separate optimization lane after functional
Java/security proof. Native/JIT intrinsics remain out of scope until Java is the semantic oracle.

## Receiving denominator

`SOURCE.tsv` records 20 paths: ten shared framework paths and ten JEP-497 paths.

On current M3JDK21 master the ML-DSA implementation classes and FIPS 204 ACVP vectors are absent.
Java-21 also lacks the nested `SHA3.SHAKE128` / `SHA3.SHAKE256` XOF surface consumed by
ML-DSA. The receiver retains a standalone SHAKE256 used by current PKCS7/SignerInfo, so
JDK-8338587 requires compatibility-aware adaptation rather than blind file deletion. Existing owners
`NamedParameterSpec`, `SunEntries`, and `KnownOIDs` require bounded exact-delta adaptation.
The shared named-key framework has prior merged proof (#158/#160) but is not treated as current
product materialization unless current-tree source/receipt evidence proves it.

## M3/A3 rule

Every Java target is prepared through the canonical backport DAG:

```text
exact file delta
 -> A3 atomize
 -> patternize / IOP
 -> document
 -> second-pass fixed point
 -> hash-pinned backport recipe
 -> recipe JUnit
 -> diff
 -> lint
 -> Java21 compile
 -> jtreg/security proof
 -> runtime/full-image proof
 -> serial promotion
```

FIPS 204 ACVP JSON data is not Java and remains in the source-sealed data lane.

## Security acceptance

Required before materialization/promotion:

- JDK-8340327 Java-21 framework compile/tests;
- JDK-8338587 SHA3/SHAKE128/SHAKE256 XOF compile + squeeze known-answer tests + current PKCS7/SignerInfo compatibility;
- exact source-21 compile of framework + ML-DSA closure;
- JDK-8345057 coexistence proof with ML-KEM parameter constants retained;
- JDK-8345533 final FIPS 204 semantics;
- FIPS 204 ACVP keyGen/sigGen/sigVer vectors;
- ML-DSA-44/65/87 keygen/sign/verify round trips;
- PKCS8/X509 key encoding/factory validation and refusal behavior;
- SUN provider registrations, aliases, OIDs and provider ordering;
- deterministic provider regression;
- relevant java.base/security jtreg;
- whole-image startup/provider smoke;
- second-pass fixed point and canonical readback.

No JDK 24+ public `AsymmetricKey` surface is admitted by this packet. Any public
`NamedParameterSpec` additions require explicit Java-21 LIBRARY_API review.

## Authority

This packet grants no source-copy, product-mutation or promotion authority. It advances JEP 497 from
`NO_RECIPE_EVIDENCE` to `PACKET_EVIDENCE` only.


## Current materialized candidate — 2026-10-07

The Java-21 receiver now carries a seven-target, recipe-owned ML-DSA candidate stacked on the
materialized JEP 496/SHAKE/named-key state:

- released JDK24-GA `ML_DSA.java` and `ML_DSA_Impls.java`;
- bounded ML-DSA additions to `NamedParameterSpec`, `SunEntries`, and `KnownOIDs`;
- focused Java-21 provider/encoding/signature smoke and deterministic-random regression tests.

The exact postimages are owned by
`com.m3.rewrite.backport.M3Jep497MlDsaBackportRecipe` /
`jdk24-jep497-mldsa`. `CURRENT_TREE_READBACK.tsv` records byte-for-byte canonical readback.

The released JDK24-GA ACVP `internalProjection.json` files are absent and therefore are not
invented. The broad later deterministic harness and performance microbenchmark are also not copied;
focused Java-21 tests prove this candidate first, and optimization remains a later lane.

Promotion remains `NOT_AUTHORIZED` until recipe JUnit, JEP 496/SHAKE prerequisite proof,
Java-21 image build, ML-DSA security jtreg, fixed point, and canonical readback execute.
