# JEP 497 — ML-DSA intake packet

Status: inventory/dependency/security-proof packet. Product materialization is **not authorized**.

## Upstream authority

- JDK-8340327 framework: `3f53d571343792341481f4d15970cdc0bcd76a5e`
- JDK-8298387 / JEP 497 ML-DSA: `8b98f958dc1afedc02b9d9c98089d6cb1ca3a5b7`

The feature uses the shared named-key/signature framework and is kept as a separate product packet
from JEP 496.

## Receiving denominator

`SOURCE.tsv` records 20 paths: ten prerequisite framework paths and ten JEP-497 paths.

Current M3JDK21 lacks the new framework and ML-DSA implementation classes. Existing
NamedParameterSpec, SunEntries, KnownOIDs and Deterministic owners require bounded adaptation.

## M3/A3 rule

Every Java target is prepared through atomize -> patternize/IOP -> document -> fixed point before a
hash-pinned source-changing recipe is authored. FIPS 204 ACVP JSON remains source-sealed data.

## Security acceptance

Required before materialization/promotion: framework compile/tests; Java-21 compile; FIPS 204
keyGen/sigGen/sigVer vectors; ML-DSA-44/65/87 round trips; encoded-key validation/refusal; provider
registration/order; deterministic regression; security jtreg; full image smoke; fixed point/readback.

Native/JIT acceleration is outside this packet until Java is the proven oracle.

This packet grants no mutation or promotion authority.
