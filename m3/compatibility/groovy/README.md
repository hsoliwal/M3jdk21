<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Groovy → M3JDK21: executable interoperability gate and donor inventory

**Status: implemented gate / external Groovy differential execution NOT RUN / product admission HOLD.**

This directory is an additive **compatibility tool** inside M3JDK21's existing `m3/compatibility` owner. It does not install a Groovy interpreter/compiler, create a replacement M3AST parser or expose new JDK public/JNI APIs. The 101-row [`GROOVY_DONOR_CANDIDATES.tsv`](GROOVY_DONOR_CANDIDATES.tsv) is a *candidate inventory*, not a claim that 101 features have been implemented. Separate language-donor designs already exist in M3JDK21 [#3340](https://github.com/hsoliwal/M3jdk21/pull/3340) and Synexia [#18611](https://github.com/hsoliwal/com.synexia/pull/18611), [#18616](https://github.com/hsoliwal/com.synexia/pull/18616). Do not duplicate their M3JEP registry entries.

## The compatibility problem

M3JDK21's additions to stock `java.lang.String` can capture calls Groovy would otherwise bind to a GDK extension. The already-merged cross-language review specifically flags `String.capitalize()`; `uncapitalize()` is another high-risk overlap; Kotlin extension binding also changes on recompilation (see [Synexia #18241](https://github.com/hsoliwal/com.synexia/pull/18241)). A behavior-only check with ordinary ASCII strings may **miss** a resolution change if two implementations happen to return the same result. This gate therefore checks both direct method signatures and Groovy language behavior.

Do **not** blindly remove the M3-added String methods: that would itself break previously compiled M3 clients. First record exact binary, source and dynamic language effects, then decide whether API relocation, a Groovy upstream adaptation, or a consciously incompatible M3 extension profile is permitted. Adding a compensating extension method does not override a real member automatically.

## How to run

Use a **stock OpenJDK 21** as baseline and a separately **source-built M3JDK21** as candidate. Supply the *same* trusted Groovy runtime jars to both. Test separately for Groovy **4.0.33**, **5.1.3**, and **6.0.0** (Apache release catalog: https://release-catalog.apache.org/groovy/index.html). Only one Groovy version may be on the classpath in each run. Groovy 6 needs JDK 17+, so JDK 21 is suitable.

The runner itself has no compile-time Groovy dependency and works on Windows or Linux. Compile with an unmodified JDK 21:

```sh
cd m3/compatibility/groovy
javac --release 21 -Xlint:all -Werror -d build GroovyInteropGate.java
java -cp build GroovyInteropGate selftest
```

Run once per Groovy version. `BASE_JAVA` and `M3_JAVA` are paths to real `java` executables, and `GROOVY_CP` is the platform-separated list of all **verified** runtime jar paths (including any required dependencies):

```sh
java -cp build GroovyInteropGate compare \
  "$BASE_JAVA" "$M3_JAVA" "$GROOVY_CP" fixtures collisions.tsv results/groovy-6.0.0
```

PowerShell uses the same Java invocation, with `$env:BASE_JAVA`, `$env:M3_JAVA` and `$env:GROOVY_CP` in place of the shell variables. On Windows the classpath uses semicolons, on Unix colons. Run with each version's own jar set, not an aggregate Groovy 4+5+6 classpath. No jars are bundled or downloaded by this repository. Validate third-party release artifacts with Apache's signatures/hashes or Maven repository checksums first.

`baseline.tsv` and `candidate.tsv` contain real child output, `input-sha256.txt` records checksums of the input Groovy jars, collision manifest and fixtures, and `differences.txt` is the pass/fail gate. A changed collision signature **fails even if** the return value of the example is unchanged. Fixture failures, a missing Groovy jar, missing Java images, duplicate keys and changing inputs fail closed. The comparator has a two-minute timeout *per Java process* and executes fixtures in a fresh child JVM for each runtime. Child JVM classpaths are built from **only the comparator's own code source and the explicit pinned Groovy jars**, never an inherited test-launcher classpath (which might silently shadow the selected Groovy version). The normalized `R:groovy-version` oracle row is compared; `M3G_META` Java version text is retained for diagnostics but excluded from semantic comparison.

The first fixtures cover dynamic/static capitalization, missing methods/properties, closure resolution, GDK filter/map/group operations, lazy `GString`, lexical category scope, Groovy truth, changing `metaClass`, regex/safe navigation and slicing. Extend the test set by adding a deterministic `.groovy` file and a source-sealed recipe. **These are trusted-code tests**, not a GroovyShell security sandbox; running attacker-controlled fixtures, build scripts, `@Grab` or AST transformations is out of scope.

## Compatibility semantics and architecture

| Receiver | Current authority | Admission proof |
| --- | --- | --- |
| `String` and `CharSequence` | Original JDK/M3String | Preserve JDK21 + M3 binary ABI, Groovy extension precedence, UTF-16/locale/null contracts |
| Arrays, collections, streams | M3Collections / M3Stream | Offset-array/bitmask ownership, lazy views where possible, callback order, identity, mutability, same exceptions |
| Groovy grammar / AST | Original `SourceUnit` and `ModuleNode`; existing `M3ASTAuthoritativeModels` from PR #1518 | One authoritative Groovy parse, source spans only when validated, no retained compiler object graph |
| MOP, `MetaClass` and `invokedynamic` | Optional M3Context / M3Object language profile (not `java.lang.Object`) | Guarded call-site keys, class-loader identity, scope, meta-generation invalidation, unload/GC/reentrancy |
| `@CompileStatic`, macros and transforms | Groovy compiler, M3AST/M3Compiler only after admission | Ordered phase/symbol attribution, classfiles, contracts and source preservation |
| GroovyShell, Gradle, Jenkins, GMavenPlus | External upstream owners | No runtime dependency or arbitrary script execution in base JDK; reuse existing Maven/m2e tooling |
| GINQ/GPath, actors/channels | M3DB, M3Scheduler candidates | Reference semantics and workload comparisons first; Groovy 6 incubating features remain opt-in |

Java's `MethodHandle`, `SwitchPoint` and `MutableCallSite` are implementation candidates for guarded MOP dispatch, not proof of Groovy ABI compatibility. JNI is restricted to profiled primitive/vector kernels; there is no reason to add native calls for ordinary closure invocation or metaclass lookup.

## Source identity and research status

- Apache Groovy 6.0 release overview: https://groovy-lang.org/releasenotes/groovy-6.0.html
- Groovy dynamic/compile-time metaprogramming: https://groovy-lang.org/metaprogramming.html
- Groovy language semantics: https://groovy-lang.org/semantics.html
- Groovy 6 GDK `StringGroovyMethods`: https://docs.groovy-lang.org/docs/groovy-6.0.0/html/api/org/codehaus/groovy/runtime/StringGroovyMethods.html
- Groovy 6 GDK `DefaultGroovyMethods`: https://docs.groovy-lang.org/docs/groovy-6.0.0/html/api/org/codehaus/groovy/runtime/DefaultGroovyMethods.html
- Product's already-merged original Groovy AST ingress: https://github.com/hsoliwal/M3jdk21/pull/1518
- Existing Groovy Maven/m2e mappings: https://github.com/hsoliwal/maven/pull/156 and https://github.com/hsoliwal/m2e-core/pull/11

The donor ledger uses compact source codes `GDK_STR`, `GDK_DEF`, `META`, `SEMANTICS`, `GROOVY6`, `PRODUCT_PR1518`, `M2E_PR11`, and `MAVEN_PR156`, corresponding to the above links. All rows require **signature/version/contract verification** before algorithm selection, licensing review and any source transfer. A candidate is not automatically an M3 API, an OpenJDK JEP, or an optimized implementation.

Groovy is Apache-2.0, but third-party components/fixtures must be separately checked. This change imports no Apache Groovy implementation source. Preserve any later attribution/NOTICE and the existing Java 21 API/behavior contract. LeetCode/HackerRank/GeeksforGeeks may inform algorithm comparison by category but editorial or submitted solutions are not copied.

## Current evidence and remaining admission gates

**Actually performed locally:** `javac --release 21 -Xlint:all -Werror` of the standalone Java gate, plus nine standalone self-checks for row parsing, exact equality, drift, missing keys, malformed/duplicate records, escape encoding, manifest parsing and duplicate collision refusal.

**Not performed:** running Groovy 4/5/6 against either JVM; M3 source-built images, Groovy compiler/static-compile oracles, Kotlin/Scala extension collision probes, OpenRewrite SDK replay, jtreg/JCK, HotSpot/JNI/GC stress, cross-platform matrix or CPU/allocation benchmarks. Maintain **HOLD** on changes to `java.lang.String` until differential compatibility evidence is available. No performance improvement or production Groovy integration is claimed.
