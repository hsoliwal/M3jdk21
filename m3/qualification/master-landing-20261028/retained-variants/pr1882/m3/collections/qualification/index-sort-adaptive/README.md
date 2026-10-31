<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 product receipt: adaptive physical-index sorting (2026-10-23)

This is an original M3JDK21 product-source refinement, not a compatibility overlay. The original package-private owner is m3/collections/src/main/java/com/m3/collections/M3IndexSort.java. This pass neither changes the JDK java.util public surface nor claims an installed JDK runtime.

## Exact custody
- Master source commit (before): b032048eece51c897d8623fd7a69f7f16eac5a57
- Existing source Git blob (before): c2f9716022b9b2de10bc7ec8b6729e172ea17137
- Result source Git blob: 9fdb435f3212a165262cb18ae11fcb1e9e56ae3e
- Result standalone contract Git blob: 60041005c55cfaf8e9f65ff3d83ad53ed2f4f1b9
- Result JUnit wrapper Git blob: 8c1f4d0e1e1862e432be2b85bcc542ae4d80925a
- Canonical source-sealed Synexia recipe: hsoliwal/com.synexia PR #13224, commit 9524650e3f4743bf22f79945526560277eb41d7c, recipe com.synexia.m3.Jdk21IndexSortAdaptive20261023. This product branch contains only admitted Java result sources/tests and this data-only receipt, not recipe code.

## Contract and storage
Use existing parent-owned dynamic Object[] and primitive int[] physical positions. The original M3IndexSort.stable signature is retained; no new public method, type, JNI export, callback-to-native ABI, per-row Entry object or payload copy. The default stable(...) retains the original bottom-up merge kernel and its comparator callback/exception transcript. A separate explicit stablePrepared(...) route permits a linear no-scratch path for already-ordered indexes and bounded <=32 in-place stable insertion, only when callbacks are proven pure/deterministic with no observable invocation count/order. Neither source payload nor caller comparison implementation is transferred into native memory.

## Executed local bounds
PASS: strict javac --release 21 -Xlint:all -Werror, executable Java21 oracle with 6,576 deterministic assertions, including duplicate and signed physical IDs, ties, natural/reversed/absolute-value comparators, exceptions, and the 31/32/33 boundary. Source-seal input/output git blobs match; local replay refused source drift and missing product owner, and passed second fixed point.

NOT_RUN: full Maven reactor, actual OpenRewrite Recipe.run, source-built JDK21, jtreg/JCK, JNI provider parity (native source untouched), Windows/macOS, installed image, CPU throughput and heap allocations. Default callback/exception transcript is preserved; only explicit stablePrepared is allowed a different callback schedule. No green claim for queued, unrun or skipped CI.

## Product boundary
m3/collections is an existing M3JDK21-owned Java module, but this PR alone does not prove it is included in the installed JDK image or that java.util implementation is replaced. Preserve String -> arrays -> collections promotion gates. Keep this receiver as DRAFT/HOLD until actual full qualification.

## Corrective source-receiving pass
The implicit optimization of default stable(...) was rejected after a RED comparison
showed changed user callback order and exception-time physical indexes. The
obsolete intermediate Git blob f786685aa7c7cf0e4e6a1b975f98227449fb637a
remains solely in Synexia correction memory. The default merge algorithm is
restored; no existing collection caller silently opts into stablePrepared.

New strict Java21 source contract blob: 50483be714e2fc8436729241a57ab941c73b4709
New strict JUnit wrapper blob: 52df18993bcde6be7509fbbb441149a880dda149
Original adaptive JUnit wrapper remains 8c1f4d0e1e1862e432be2b85bcc542ae4d80925a.
Corrected Synexia recipe source commit: 9524650e3f4743bf22f79945526560277eb41d7c.

Executable focused evidence: local normal/JVM interpreter adaptive oracle
6,576 assertions, strict callback/exception oracle 9 assertions; sealed
differential with original baseline 48,752 normal cases and 2,432 interpreter
cases. These do not certify full owning module Maven/JUnit, JDK image/jtreg,
JNI mask parity or runtime performance. Explicit prepared lane has no
production caller in this bounded pass; therefore no production speedup is
claimed. Keep draft/HOLD until remaining qualification runs.

## Current-head modular compile and strict callback admission

Canonical Synexia validation source: PR #13224 commit 2f14c1e734808b16ba01b96a3b5e2079bccf702c. The exact shell candidate (Git blob fb9a239f8974b02de4344afac0502ca46bb10eb3) is received as qualification/index-sort-adaptive/verify-module-java21.sh; the exact CI result (Git blob 8c76c3038edbf007050d699ea607b433c1ecd4b9) is received into .github/workflows/m3-index-sort-adaptive-java21.yml. No recipe engine or Synexia runtime dependency is imported.

The new whole-module-java21 job is dependent on the original source-sealed-java21 job, and independently compiles the real com.m3.arrays module from the existing src/main/java source, then the real com.m3.collections module with an explicit Java module path. It verifies that the collection descriptor still requires com.m3.arrays, compiles adaptive/strict contract classes through --patch-module, and executes both contracts in normal and -Xint VM modes. This tests module resolution without removing the existing requires directive.

The existing historical full collection/JNI gate remains unchanged and FAILED on this source revision: its compiler omitted com.m3.arrays from the Java module path. Its historical source manifest, packaging and native parity checks are not bypassed or treated as passed. This additional module-source proof is not a source-built JDK image, JUnit/Maven reactor, JNI parity, native library, performance, or release-qualification replacement.

Before publication, this script passed Bash syntax validation and the complete modular compile/execute sequence on a disposable two-module Java21 fixture. The full real M3JDK21 source execution on GitHub is a separate pending gate. This PR stays draft/HOLD.

## Full Java/JNI verifier — original named arrays dependency, 2026-10-24

Original full collection/JNI run 39931027347 failed at javac with
`module not found: com.m3.arrays`. The older verifier also assumed java.base
was the only possible runtime dependency in jdeps and left the arrays module
out of its test/JNI classpaths. The actual product module descriptor explicitly
requires com.m3.arrays; that contract must NOT be removed.

Canonical Synexia source-sealed text recipe: PR #13224, commit
`d9a82ffdde9a43a66286b53817ef352466244149`,
`com.synexia.m3.Jdk21FullCollectionJniModuleClosure20261024`.
The reused M3HashPinnedTextSnapshotRecipe owns one exact Python verifier target.
Product preimage blob `c6e7bbe8de8b628e8e558454fb7efb1eae557c78`;
exact emitted postimage blob `77726d987eeb1e185a64b933bc6b0049c77ff67c`.

The corrected existing verifier compiles and installs original m3/arrays from
its source POM (after pinned donor/JUnit closure), checks its real Java module
descriptor and name, supplies its jar as a named javac module and test/JNI
classpath dependency, and accepts only the exact jdeps set
`{java.base,com.m3.arrays}`. The proof receipt adds original arrays module
source hashes, descriptor hash, and compiled artifact digest.

Native library inventory, C17/UBSan, JNI headers, required-native JUnit
non-skipping and parity tests, jar legal/provenance bytes, and all original
explicit native/missing-native contract checks remain in place. No Java runtime
collection implementation, JNI export, public API or Synexia runtime
dependency changes in this receiving pass.

Execution truth: canonical Python AST/digest/refusal tests and real Text
OpenRewrite SDK replay are added in Synexia; the full Maven/Java/JNI workflow
and both new recipe tests must still RUN and PASS on their current heads.
The frozen donation source manifest and source-versioned OpenRewrite vendor
dependency need independent qualified successors, not ignored failures.
Remain DRAFT/HOLD until full JDK, JNI, jtreg/JCK and installed-image gates pass.

### Versioned/unversioned `jar --describe-module` regression

A local Java 21 named-module fixture demonstrated that an explicit module may
appear as `com.m3.arrays jar:file:...` without any `@version`. The original
first repair incorrectly interpreted the entire unversioned header as a module
name. Corrected source now reads the first whitespace token and removes
`@version` only if present. The rejected intermediate verifier blob
`dd38848e22829223838011f848b2741999571e7f` is preserved solely in the
Synexia recipe's correction history; it is **not** the current product code.

The version-neutral corrected verifier blob is
`77726d987eeb1e185a64b933bc6b0049c77ff67c`, exact canonical Synexia
source template at `d9a82ffdde9a43a66286b53817ef352466244149`.
The JDK 21 fixture passed explicit arrays module build,
`--module-path` compilation and `jdeps` closure check for
`com.m3.arrays,java.base`. Full owning M3JDK21 Maven/JUnit/JNI and
OpenRewrite SDK execution are still outstanding; this source-receipt pass
does not assert their success.

## Historical collection recipe donor-source bootstrap (2026-10-24)

Actual current-branch predecessor workflow run 39931027347 failed its
"Historical lane recipe with real SDK" job before Maven/OpenRewrite executed,
because the original collections recipe POM references the first-party
source-versioned donor artifact `org.openrewrite:rewrite-java-21:8.17.1-m3-source-1`.
Maven Central does not host that project-specific version. The repository
already owns the original unchanged published sources under
`m3/vendor/third_party/rewrite-java-21` and aggregates them with
`m3/vendor/third_party/pom.xml`.

Canonical source-sealed Synexia recipe (PR #13224) commit:
`3579b6591d0487a2ca4ba7be98eec8c9e168ad00`.
Original product workflow Git blob:
`c42954ecf99a58893a76f3aee6dbe2d258e3a7fe`;
exact accepted replacement:
`dc1903c6e6602b7fb03ff6c6ed6a6533d4a49c2f`.
The existing `M3HashPinnedTextSnapshotRecipe` owns this YAML candidate and
its predecessor/postimage SHA-256; no recipe source belongs in this product.

Only the original historical recipe job changes: its limit is raised from 20
to 45 minutes to permit a real source build, and a step immediately before
the original recipe verification performs pinned `verify_source.py`,
`mvn -f m3/vendor/third_party/pom.xml --projects rewrite-java-21 --also-make
-DskipTests install`, then `verify_source.py` on the built JAR.
The original `m3/collections/recipe/pom.xml verify` command remains in place.
Both existing installed-collections and all-JNI jobs are unchanged.
No public runtime type, Java collection owner, JNI export, Maven dependency
version or source provenance is altered.

The canonical Synexia crate includes a strict one-target before/after text
recipe, Python exact-diff and drift/fixed-point refusal oracle, and JUnit
OpenRewrite Text SDK source replay for actual execution. Building the vendored
donor may reveal further unqualified upstream module problems. This change
does **not** claim successful Maven reactor, OpenRewrite, JNI/JUnit or
installed JDK validation until the exact-head Actions job passes. Keep the
receiver DRAFT/HOLD; do not bypass dependency source or source-manifest gates.
