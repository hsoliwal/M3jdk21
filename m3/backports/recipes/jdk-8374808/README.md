# JDK-8374808 — Java 21 compatible KeyStore creation Instant API

Upstream issue: `JDK-8374808`, **Add new methods to KeyStore and KeyStoreSpi that return the
creation date as an Instant instead of Date**.

Pinned donor commit:

```text
openjdk/jdk@264fdc5b4ed5f4e35168048533196e670c3dda6c
```

## Compatibility split

The full upstream commit also rewrites built-in provider entry storage from `Date` to `Instant`.
That storage rewrite is not required to expose the additive API on Java 21.

M3JDK21 imports the smaller compatible leaf:

- `KeyStore.getCreationInstant(String)`;
- `KeyStoreSpi.engineGetCreationInstant(String)` as a default method;
- one focused jtreg compatibility test.

The SPI default delegates to the existing Java 21
`engineGetCreationDate(String)` method and converts the result with `Date.toInstant()`.
Existing provider subclasses therefore require no source change and retain their existing Date
storage, serialization and persistent-format behavior.

The following upstream provider rewrites are deliberately excluded from this packet:

- `JavaKeyStore`;
- `JceKeyStore`;
- `PKCS12KeyStore`;
- `DomainKeyStore`;
- macOS `KeychainStore`;
- provider tests whose only purpose is to validate the rewritten internal Instant storage.

Those changes may be reviewed independently, but they are not dependency requirements for the
additive API leaf.

## Java 21 contract

This packet:

- does not add Java source grammar or later javac semantics;
- does not change class-file version requirements;
- does not remove or alter `getCreationDate` / `engineGetCreationDate`;
- does not change provider storage or keystore file formats;
- does not change JNI/JVMTI/native behavior;
- adds public/SPI methods only.

`KeyStore.getCreationInstant` retains the existing uninitialized-keystore failure contract.

## Recipe atom

`M3Jdk8374808BackportRecipe` owns one hash-pinned Java crate:

```text
jdk27-keystore-creation-instant-8374808
  -> src/java.base/share/classes/java/security/KeyStore.java
  -> src/java.base/share/classes/java/security/KeyStoreSpi.java
  -> test/jdk/java/security/KeyStore/CreationInstant.java
```

The two product files carry exact JDK21 SHA-256 preimages. The focused jtreg test has an explicit
`ABSENT` preimage. JUnit requires exact replay, stale-preimage rejection and a zero-change second
pass.

## Proof gates

The packet remains `candidate-adapted` until:

1. OpenRewrite recipe JUnit passes;
2. the backport catalogue verifier passes;
3. `java.base` builds on the M3JDK21 tree;
4. `test/jdk/java/security/KeyStore/CreationInstant.java` passes jtreg;
5. existing KeyStore compatibility tests remain green;
6. diff/lint checks pass;
7. recipe replay reaches fixed point.

No whole-JDK compatibility claim follows from this focused packet.
