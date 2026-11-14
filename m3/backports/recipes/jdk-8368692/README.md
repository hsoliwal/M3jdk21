# JDK-8368692 — Java 21 compatible password System.in policy

Upstream issue: `JDK-8368692`, **Restrict Password::readPassword from reading from System.in**.

Pinned donor commit:

`openjdk/jdk@9131c72d63cac7d2a0e845952cee0e3c7edbfc93`

## Compatibility decision

This packet is additive policy behavior with a compatibility-preserving default:

- adds `jdk.security.password.allowSystemIn`;
- default is `true`, preserving ordinary JDK 21 behavior;
- system property overrides the security property;
- only echo-off password fallback through the current `System.in` is gated;
- console input and other password sources remain unchanged;
- invalid property values fail during `Password` initialization.

JDK 21 keeps `SecurityProperties.getOverridableProperty` private and still supports the Security
Manager-era privileged access path. The backport therefore adapts the donor to
`SecurityProperties.privilegedGetOverridable(...)` instead of widening that internal helper.

No Java grammar, class-file version, JNI/JVMTI ABI, persistent format, GC, JIT or public Java SE API
is changed.

## Recipe atoms

`M3Jdk8368692PasswordSystemInBackportRecipe` composes:

`M3Jdk21HashPinnedSnapshotRecipe(jdk27-password-systemin-8368692-java)`

- `Password.java`
- additive `AllowSystemIn.java` jtreg

`M3Jdk21HashPinnedTextSnapshotRecipe(jdk27-password-systemin-8368692-text)`

- `java.security`

All existing targets are bound to exact current-master SHA-256 preimages. The jtreg test has an
explicit `ABSENT` preimage. Recipe JUnit requires exact replay, stale-preimage refusal and a
zero-change second pass.

## Scope

The physical edit spans source, configuration and test content inside `java.base`, so the packet is
classified as MODULE scope. It does not promote to multi-module or public-library API authority.

## Promotion gates

Status remains `candidate-adapted` until:

1. recipe JUnit passes;
2. backport artefact lint/catalogue checks pass;
3. Java 21-compatible JDK image build passes;
4. focused `AllowSystemIn.java` jtreg passes in all four modes;
5. a default-policy compatibility smoke proves redirected System.in still works with no property;
6. diff/lint checks pass;
7. recipe second-pass fixed point passes.

No whole-JDK completion or security-hardening-default claim is made by this packet.
