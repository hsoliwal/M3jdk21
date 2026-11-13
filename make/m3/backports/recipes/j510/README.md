# J510 — Key Derivation Function API intake

Status: inventory/dependency packet. No security product source is changed here.

JEP 510 is a final public `javax.crypto` API and therefore an explicit M3JDK21
`OPT_IN_SE_API_EXTENSION`, not stock Java SE 21 identity.

## Corrected upstream lineage

The packet does not stop at the JEP commit:

1. JDK-8331008 / JEP 478 — preview KDF API + SunJCE HKDF implementation.
2. JDK-8343772 — correct delayed provider selection / IAPE behavior.
3. JDK-8347289 — non-extractable PRK delayed-provider selection.
4. JDK-8353888 / JEP 510 — graduate KDF from preview.
5. JDK-8370082 — clear intermediate secret material in HKDF and related crypto paths.
6. JDK-8377914 — HKDFParameterSpec documentation correction (documentation-only).

JDK-8353578, which refactors existing TLS/DHKEM internal HKDF callers to the new API, is not required
to expose or implement the KDF API itself. It is a separate absorption candidate and must not be
silently bundled into J510.

## Java 21 compatibility split

Current M3JDK21 already contains the substrate needed by the corrected HKDF cleanup:

- `JavaxCryptoSpecAccess.clearSecretKeySpec`;
- `SecretKeySpec` registration into SharedSecrets;
- Mac / HMAC providers;
- standard Provider service infrastructure.

The packet therefore proposes:

- add final `KDF`, `KDFParameters`, `KDFSpi`, `HKDFParameterSpec`;
- add the corrected `HKDFKeyDerivation` provider implementation;
- add the KDF engine type to `Provider`;
- register HKDF-SHA256/384/512 in the current SunJCE provider;
- add KDF to the security-debug engine list.

Do **not** copy JDK25 module-preview cleanup mechanically. Current M3JDK21 owns its own preview
participation state; J510 is final and needs no PreviewFeature entry.

Do **not** copy the JDK25 SunJCE info string that assumes unrelated ML-KEM state. Update only the
actual receiving provider capabilities.

## Security acceptance

Before materialization:

- source-21 compile of the exact packet;
- provider delayed-selection semantics;
- RFC 5869 known-answer tests for SHA-256/384/512;
- extract-only, expand-only and extract-then-expand boundaries;
- non-extractable PRK behavior;
- invalid/null/length/refusal contracts;
- provider concurrency/delayed-provider tests;
- secret-intermediate cleanup review;
- full java.base/security jtreg and provider ordering proof.

No promotion is authorized by this inventory packet.
