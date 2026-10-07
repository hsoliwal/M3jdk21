# JEP 510 KDF recipe-first preparation

This packet advances the existing J510 inventory from dependency analysis to a source-pinned
OpenRewrite candidate. It does **not** authorize product materialization.

## Upstream lineage

The reviewed cumulative lineage is:

1. JDK-8331008 / `2a1ae0ff89a8ac364206b09059d9dc884adcc5ac` — preview KDF API + HKDF provider.
2. JDK-8343772 / `2c7bea1cb2acd768e57f460440228fee914255a6` — delayed-provider exception correction.
3. JDK-8347289 / `db7fa6a2c65d11e5bd790073d345f37b5ec356b6` — non-extractable PRK correction.
4. JDK-8353888 / `079fccfa9a03b890e698c52c689dea0f19f8fbee` — final JEP 510 graduation.
5. JDK-8370082 / `012b4eb6cea6e1756a589a6c17a805867ed60686` — HKDF intermediate-secret cleanup.
6. JDK-8377914 / `79456110fb6dd11ef19e9637c6f40ee7ce329481` — HKDFParameterSpec documentation repair.

JDK-8353578 (TLS/DHKEM callers migrated onto KDF) remains a separate absorption candidate.

## Exact Java product target set

Additions:

- `javax.crypto.KDF`
- `javax.crypto.KDFParameters`
- `javax.crypto.KDFSpi`
- `javax.crypto.spec.HKDFParameterSpec`
- `com.sun.crypto.provider.HKDFKeyDerivation`

Current-master integrations:

- `java.security.Provider` — add KDF engine constructor parameter type;
- `com.sun.crypto.provider.SunJCE` — register HKDF-SHA256/384/512 and describe only capabilities
  actually present on the Java-21 receiver;
- `sun.security.util.Debug` — include KDF in provider engine filtering help.

No module preview-participation cleanup is imported. Current M3JDK21 module descriptors remain
authoritative because JEP 510 is final and the Java-21 receiver has its own preview state.

The broader JDK-8370082 changes to Mac, HmacCore, DHKEM, XDH, ECDH and EdDSA are not silently bundled.
Only the HKDF cleanup required by this packet is included.

## Recipe authority

The product-specific OpenJDK postimages are GPLv2+Classpath target resources and remain in M3JDK21.
The reusable mechanical owner is the existing
`M3Jdk21HashPinnedSnapshotRecipe`; no second recipe engine is introduced.

Composite:

`M3Jep510KdfBackportRecipe -> M3Jdk21HashPinnedSnapshotRecipe("jdk25-jep510-kdf")`

Minimum scope:

`LIBRARY_API + EXPLICIT_CONTRACT_CHANGE`

The crate refuses missing/drifted current-master inputs, parses every Java postimage through the Java
21 OpenRewrite parser, applies from exact preimages/ABSENT only, and must be a second-pass fixed point.

## Security gates before any product apply

Recipe success is necessary but not sufficient. Materialization remains blocked until:

- source-21 compilation of the exact candidate set;
- RFC 5869 SHA-256/SHA-384/SHA-512 known-answer tests;
- extract-only / expand-only / extract-then-expand boundaries;
- non-extractable PRK delayed-provider behavior;
- delayed-provider sync/threading/exception contracts;
- null/length/invalid-input refusal;
- intermediate-secret cleanup review;
- provider registration and provider-order proof;
- relevant java.base security jtreg;
- whole-image provider smoke;
- A3 preparation/fixed-point for affected Java files;
- target fixed-point and canonical readback.

No performance claim or native acceleration is part of this packet.
