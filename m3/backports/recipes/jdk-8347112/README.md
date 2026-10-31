# JDK-8347112 — Java 21 compatible javadoc doc-files backport

Upstream issue: `JDK-8347112`, **Copy nested directories in doc-files by default**.

Primary donor commit:

```text
openjdk/jdk@b221cb6ba138672802644f37eebf368521a0a6f4
```

Upstream follow-up reviewed:

```text
JDK-8383824
openjdk/jdk@c2074e46a429b0f45454632de81d3d2b9a92101d
```

The follow-up adjusts later-javadoc HTML expectations. M3JDK21 does not copy those later HTML
expectations because the Java 21 output contract differs; the focused Java 21 jtreg test is adapted
against the local baseline instead.

## Compatible leaf

M3JDK21 imports:

1. recursive copying of nested `doc-files` directories by default;
2. `-excludedocfilessubdir '*'` as an explicit restore-old-default control;
3. help/manual text describing those semantics;
4. Java 21 regression tests.

M3JDK21 deliberately **does not remove or repurpose** the existing
`-docfilessubdirs` option processing. The option remains accepted and the pre-existing option-based
tests remain in the suite. This preserves the JDK 21 command-line contract while making recursion
the default.

## Recipe DAG

`M3Jdk8347112BackportRecipe` composes two independent atoms:

```text
M3Jdk21HashPinnedSnapshotRecipe(jdk27-javadoc-8347112-java)
  -> structured Java sources/tests

M3Jdk21HashPinnedTextSnapshotRecipe(jdk27-javadoc-8347112-text)
  -> properties + generated manual
```

Both are exact-preimage, exact-postimage, one-cycle candidates. JUnit replays the complete DAG a
second time and requires zero changes.

## Scope

All product changes remain inside `jdk.javadoc`; the jtreg proof is in the corresponding langtools
test tree. No Java grammar, class-file, VM/JNI/JVMTI or public Java SE API contract changes.

## Promotion gates

Candidate status remains until all of the following execute successfully:

- recipe JUnit;
- backport packet verifier;
- focused `jdk.javadoc` build;
- `TestCopyFiles.java` jtreg;
- diff/lint checks;
- second-pass recipe fixed point.

`adaptation.tsv` is the machine-readable provenance and target-hash receipt.
