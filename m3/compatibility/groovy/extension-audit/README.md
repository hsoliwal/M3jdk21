<!-- SPDX-License-Identifier: Apache-2.0 -->

# Groovy extension method collision audit — 21 Nov 2026

**Status: locally qualified, candidate-only. Not merged or repository-verified.**

This Java 21 standalone tool extends the existing M3JDK21 `m3/compatibility/groovy/`
acceptance lane; it is not a replacement for the merged `GroovyInteropGate`, the
Groovy compiler, the JDK method resolution rules, or the original M3 runtime
and M3AST owners. It creates no Java/JNI/HotSpot API, user-visible String
method, Groovy compiler, or runtime dependency.

## Why

The existing 16 selector/arity collision probes catch known hot spots, but
new JDK methods can shadow *previously unknown* Groovy GDK extension methods.
The merged M3 String `capitalize()` family was explicitly called out in
Synexia's cross-language review. Groovy extensions are static provider methods
whose first parameter is their receiver, e.g.
`StringGroovyMethods.capitalize(CharSequence self)`.

The new scanner indexes only explicitly selected public JDK receiver and primitive-array types
and public static donor methods, both under **two distinct, independently launched Java executables**
with **the exact same, externally approved SHA-256-locked Groovy jars**. It
blocks added Java members with the same receiver, selector and arity as an
extension (including varargs and primitive/boxed receiver matches), and
removed/changed public member signatures, retaining full before/after evidence.
The candidate JVM is launched with `--add-modules jdk.m3.collections`, and
its child probe verifies that module resolves in the boot layer; a second
copy of stock Java cannot serve as an M3 compatibility witness. This does
not replace the independently sealed full-image provenance gate.
Additive members that do not overlap a scanned GDK method are recorded as
informational, never silently treated as universally safe.

## Run without WSL

Compile with Java 21:

```powershell
javac --release 21 -Xlint:all -Werror -d build GroovyExtensionCollisionAudit.java
java -cp build GroovyExtensionCollisionAudit selftest
java -cp build GroovyExtensionCollisionAudit defaults out\profiles
```

Once Apache Groovy jars have **independently verified release signatures** and
a `jar-lock.tsv` with one `basename<TAB>sha256` per exact jar, sorted by filename:

```powershell
java -cp build GroovyExtensionCollisionAudit compare-locked `
  'C:\stock-jdk21\bin\java.exe' 'C:\M3JDK21\bin\java.exe' `
  'C:\groovy\groovy-6.0.0.jar;C:\groovy\groovy-json-6.0.0.jar' `
  out\profiles\owners.txt out\profiles\providers.txt `
  approved-groovy-6.0.0-jars.tsv out\groovy6 6.0.0
```

The accompanying `OFFICIAL_GROOVY_RELEASE_PINS.tsv` records upstream ZIP-level SHA-256
values for Groovy 4.0.33, 5.1.3 and 6.0.0, as read from Apache's official
checksum pages on 21 Nov 2026. **No distribution was downloaded or PGP
signature-verified here**, and a ZIP checksum does not substitute for per-jar
locks after extraction.

That classpath is **illustrative, not a verified sufficient jar list**. Use the
real, complete Groovy distribution, its matching dependencies, and the
independently verified per-jar hashes; never copy the two shown filenames into
an artifact lock without checking the actual distribution. Execute the same
command separately for each qualified Groovy 4, 5, and 6 release using its own
classpath, lock, and output folder. Use the existing `GroovyInteropGate`
behavioral scripts as a separate mandatory oracle.

Shell users may run `./run-selftest.sh`. Windows users can run the standalone
`run-selftest.ps1` without WSL. These smoke tests compile with Java 21, use
synthetic extension classes and mock launchers, check fixed-point manifest
emission and lock tampering, and show how a candidate `String.capitalize()`
addition is blocked. They **do not** execute Apache Groovy.

## Important limits

- A name/arity match is conservative hazard evidence, not proof of the exact
  Groovy call target; semantics depend on overload selection, categories,
  MetaClass changes, extension configuration, coercion and `@CompileStatic`.
- Scans only configured owner/provider classes; external extension modules,
  category scopes and metaClasses are not automatically discovered.
- Reflection does not execute scripts or call donor methods, but Groovy jars
  themselves must be trusted and signature-verified before classloading. This
  tool is **not** a sandbox.
- Exact JDK bytecode ABI conformance, jtreg/JCK, Groovy 4/5/6 execution,
  Kotlin/Scala interoperability, actual source-built M3JDK21 module tests,
  classloader/GC/MethodHandle/JNI tests and performance metrics remain HOLD.
- Java executables in `compare-locked` must be concrete regular files, not
  unresolved symbolic links. The source manifest and jar lock are SHA-sealed
  before, between and after executions, but host-side filesystem tampering still
  requires trusted build infrastructure.
- The command's report is evidence, never a waiver. Any blocking row requires
  a source/behavior resolution, not simply excluding a colliding extension.

## Source and provenance

- Groovy 6 release notes: https://groovy-lang.org/releasenotes/groovy-6.0.html
- `StringGroovyMethods` official API: https://docs.groovy-lang.org/latest/html/api/org/codehaus/groovy/runtime/StringGroovyMethods.html
- Existing product PR: https://github.com/hsoliwal/M3jdk21/pull/3344
- Groovy 6 product PR: https://github.com/hsoliwal/M3jdk21/pull/3350
- Synexia review: https://github.com/hsoliwal/com.synexia/pull/18241

Use a named, SHA-256-sealed `M3HashPinnedJavaSnapshotRecipe` in Synexia to admit
the Java source. The attached recipe crate is **prepared but not executed with
OpenRewrite SDK**. A future PR must revalidate the exact M3JDK21 preimage and
perform the official recipe, JDK and real-Groovy gates before merging.

## 22 November admission hardening

The **real** `compare-locked` command now requires a final exact Groovy version
argument (`4.0.33`, `5.1.3`, or `6.0.0`), refuses the same Java executable as
both baseline and candidate, and refuses a pre-existing report directory. Real candidate execution also
requires the actual `jdk.m3.collections` module on that JVM's module path;
missing modules cause refusal rather than a false PASS.
Neither an unreviewed release nor synthetic evidence can be promoted to a
real-runtime PASS. `compare-synthetic-locked` remains separate for mock tests
and retains the explicit `MOCK_PASS` marker. The new negative shell checks
exercise all three refusal paths.
