# JDK-8374808 — Java 21 additive KeyStore Instant API atom

Upstream issue: `JDK-8374808`, Add new methods to KeyStore and KeyStoreSpi that return the creation date as an Instant instead of Date.

Exact donor commit:

`openjdk/jdk@264fdc5b4ed5f4e35168048533196e670c3dda6c`

## Split strategy

This packet is the first atom of the complete backport:

1. **API atom (this packet)**
   - `java.security.KeyStore#getCreationInstant(String)`
   - `java.security.KeyStoreSpi#engineGetCreationInstant(String)` default bridge
   - Java-21-adapted API regression coverage
2. **Provider atom (stacked follow-up)**
   - JKS/JCEKS/PKCS12/DKS/macOS provider-native `Instant` storage/overrides

The split is deliberate. The SPI default implementation delegates to legacy
`engineGetCreationDate(alias)` and converts the result with `Date.toInstant()`, so all existing
Java 21 providers and third-party KeyStoreSpi implementations remain behaviorally compatible before
the provider optimization atom lands.

## Java 21 adaptation

The upstream `TestKeyStoreBasic` revision also contains unrelated later-JDK imports and test changes.
M3JDK21 retains the exact Java 21 test body and adds only:

- `java.time.Instant` import;
- `8374808` bug marker;
- creation-instant equality checks adjacent to existing creation-date checks.

No later-JDK `PEMDecoder` or unrelated import cleanup is copied.

## Scope

This adds public/protected API and is therefore `LIBRARY_API` scope. It is additive: existing
`getCreationDate` and `engineGetCreationDate` behavior remains available and unchanged.

## Recipe

`M3Jdk8374808ApiBackportRecipe` composes three exact file-local
`M3VerbatimJavaPairRecipe` atoms. JUnit must prove first replay, unrelated-source preservation,
source-drift refusal and zero-change second pass before broad application.

## Promotion gates

- recipe JUnit;
- java.base build;
- API/signature review;
- focused `TestKeyStoreBasic` jtreg;
- compatibility check with a custom KeyStoreSpi that implements only legacy `engineGetCreationDate`;
- second-pass recipe fixed point;
- provider atom review.

No claim is made that the complete JDK-8374808 provider work is finished by this API atom alone.
