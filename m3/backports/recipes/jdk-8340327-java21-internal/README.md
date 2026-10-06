# JDK-8340327 Java 21 internal adaptation

Status: source-sealed candidate prerequisite for JEP 496 / JEP 497.

## Upstream

- JBS: JDK-8340327
- upstream implementation commit: `3f53d571343792341481f4d15970cdc0bcd76a5e`
- composition donor: JDK 24 GA

## Java 21 adaptation

The upstream framework depends on the post-21 public `java.security.AsymmetricKey` API.
Default M3JDK21 does not import that API.

The adaptation therefore:

1. keeps `NamedPKCS8Key`, `NamedX509Key`, `NamedKEM`,
   `NamedKeyFactory`, `NamedKeyPairGenerator`, and `NamedSignature`;
2. keeps parameter access on the internal Named* concrete key classes;
3. removes invalid `@Override` claims on Named* `getParams()`;
4. retains RAW-key translation by algorithm name / single-parameter factory;
5. does not inspect arbitrary foreign RAW keys through a public key-parameter interface that Java 21
   does not have;
6. adapts `KeyUtil.fullDisplayAlgName` to recognize the internal Named* classes while preserving
   existing EC/EdEC behavior;
7. adapts `SignatureUtil` to recognize `NamedPKCS8Key`, including default ML-DSA-style
   same-name signature selection while returning no signature default for KEM keys;
8. adapts the upstream regression test so the generic RAW getParams-only case is explicitly rejected
   on Java 21.

No default public Java SE API is added.

## Scope

Physical mutation scope: MODULE (`java.base` plus focused JDK tests).

This prerequisite does not grant JEP 496/497 provider promotion. Those provider packets remain
blocked until this framework compiles, passes jtreg, and reaches fixed point.

## Recipe-first execution

Canonical recipe:

`com.m3.jdk21.Jdk8340327Java21Internal`

It is backed by one exact 10-target hash-pinned Java crate:

- 8 ABSENT additions;
- exact current-tree preimages for `KeyUtil.java` and `SignatureUtil.java`;
- exact reviewed postimages;
- drift refusal;
- second-pass fixed point.

The Git branch carries no product-source materialization. CI applies the same sealed manifest only
inside the disposable Actions checkout.

## Proof

Required before this prerequisite can unblock JEP 496/497:

1. OpenRewrite replay/fixed-point JUnit;
2. no `java.security.AsymmetricKey` import/use in the adapted closure;
3. exact materialization from current M3JDK21 preimages;
4. OpenJDK Java 21 configure/image build;
5. `NamedKeyFactoryTest`;
6. `NamedEdDSA`;
7. exact changed-path fence.

A green result proves only the internal prerequisite. It does not by itself prove ML-KEM or ML-DSA.
