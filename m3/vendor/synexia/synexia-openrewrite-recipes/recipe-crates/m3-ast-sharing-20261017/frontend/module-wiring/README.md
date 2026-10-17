# Normal compiler module integration

This crate makes the shared AST frontend and cache reachable through the ordinary M3
compiletime Maven module. The reviewed templates are emitted by the existing typed
Java and hash-pinned text recipe providers; receiver files are written only from
actual OpenRewrite Results after exact preimage admission.

## Module ownership

| Owner | Behavior |
| --- | --- |
| `m3/ports/compiletime/pom.xml` | Keeps ordinary `src/main/java` and existing precompute dependencies, adds the exact AST vendor closure, and registers the target compiler plugin. |
| `m3/ports/compiletime-openrewrite` | Separate optional artifact containing the package-mapped, qualified OpenRewrite 8.17.1 provider. The core module has no dependency on this artifact or OpenRewrite. |
| `synexia-mindex/compiler/pom.xml` | Keeps the existing canonical dependency/processor configuration and adds explicit native cache test opt-in. |
| `synexia-mindex/compiler-openrewrite/pom.xml` | Separate optional canonical module with Java 21 compiler exports and pinned OpenRewrite dependencies. Its parent reactor is unchanged. |

`VENDOR_CLOSURE.json` lists every admitted Java dependency. It contains 77 original
AST/runtime files, seven cache/native-loader dependencies, eight original
namespace frontend/cache sources, and eleven hierarchy sources: 103 vendor sources
in total. The hierarchy increment contains nine existing partial AST owners,
including its qualified cache successor, and two summary owners. The target module also compiles its ordinary sources and
ten target namespace frontend/cache/hierarchy sources. Maven includes name each
vendor file explicitly; `-implicit:none`
prevents implicit source emission. The verifier compares Maven's actual compiler
input list with this closure and records each input hash. Unrelated vendor source
trees are not registered.

The ordinary primitive-image compiler path remains enabled by its existing compiler
integration. This wiring changes source/dependency reachability and plugin service
discovery; it does not require `-Xplugin:M3ASTIndex` for ordinary compilation. The
explicit M3ASTIndex plugin requires the qualified compiler bridge and exports an
immutable syntax image from the owning compilation. The module proof compares the
class bytes produced with and without explicit plugin selection.

## Native tests

The default module test run excludes only `M3ASTWorkspaceCacheNativeTest` in addition
to Surefire's ordinary inner-class exclusion. Select native cache testing explicitly:

```sh
mvn -f m3/ports/compiletime/pom.xml -Pm3-ast-native \
  -Dm3.cache.native=/absolute/path/libsynexia_mindex_swap.so test
```

The profile restores the native test to discovery. Its existing assertions still
fail when the supplied library is absent or invalid. No assumptions, conditional
skips, or replacement native implementations were added. `verify.py` exercises
both the missing-library failure and the real SQLite JNI positive path.

## Build and replay

The existing `m3-precompute` artifact is a prerequisite. Build/install it from the
ordinary source module, then run the core module. Projects with the vendored artifact
universe use the dependency versions already declared by the receiver POM. That POM
also already documents CI overrides `-Dasm.version=9.6` and
`-Djunit.artifact.version=5.10.2`; this environment uses that documented mode and
records it explicitly. Availability of the custom source artifact defaults is a
separate qualification.

The optional receiver module repeats the existing ASM default in its own
dependency management. This lets the documented `-Dasm.version=9.6` CI override
also select the three ASM dependencies reached through the installed core POM.
Without that local management, Maven resolves the core POM's custom versions even
when the optional build receives the override. The first dependency-resolution
failure and an exact optional-POM successor are retained.

```sh
mvn -f m3/ports/precompute/pom.xml install
mvn -f m3/ports/compiletime/pom.xml install
# Optional provider, after the core artifact is installed:
mvn -f m3/ports/compiletime-openrewrite/pom.xml package
```

For recipe replay, first run `prepare.py --target /absolute/M3jdk21` to recreate
the sealed classpath resources from retained preimages and reviewed inputs. It
never admits a fresh preimage over the retained baseline. The active providers are
the exact canonical upstream inputs recorded in `CURRENT_PROVIDERS.json`. The
earlier narrow service-gate extension is retained as historical candidate evidence;
it never writes a current provider source. Upstream already supports Maven service
descriptors. Run:

```sh
mvn -f pom.xml -Dtest=com.synexia.rewrite.ModuleWiringRecipeTest \
  -Dm3.wiring.canonical=/absolute/com.synexia \
  -Dm3.wiring.receiver=/absolute/M3jdk21 \
  -Dm3.sharedAst.plugin=/absolute/frontend/target/m3-ast-sharing.jar test
# The existing ordinary harvester also needs the independently sealed local-scope fix:
mvn -f pom.xml -Dtest=com.synexia.rewrite.OrdinaryLocalScopeRecipeTest \
  -Dm3.wiring.ordinary=/absolute/M3jdk21 test
```

Replay uses actual OpenRewrite 8.17.1 recipes. `SOURCE_PINS.json` identifies the
current provider pair, historical provider snapshots, every metadata/source preimage
and postimage, and the optional adapter's package-only target mapping. This task's
sealed manifests name exactly two `META-INF/services/com.sun.source.util.Plugin`
owners. The SDK verifies their scope, refuses manifest owner drift, and preserves
other service files unchanged. The shared upstream provider retains its broader
Maven service, native, structured-text, Gradle, and launcher capabilities.

The canonical plugin descriptor already contained the compiler and experimental
compiler providers at both pinned revisions. Its primary recipe admits that exact
two-provider preimage and appends M3ASTPlugin, preserving the original provider order.
The independent target descriptor introduces only its mapped M3ASTPlugin provider.
An exact successor repairs the intermediate local M3-only canonical descriptor;
the primary recipe never treats a missing local hydration as repository absence.

The first normal-module attempt exposed a bootstrap ordering defect: javac found
the provider's service descriptor before its class had compiled. The final POMs
exclude only that descriptor from early resource copying and publish it at
`process-classes`. The exact intermediate POMs and their text-recipe successor are
retained as evidence; normal replay starts at the original module POM and emits the
correct final lifecycle directly.

The retained 92-source lifecycle POM has a separate exact successor to the final
103-source closure. Run `ModuleSuccessorRecipeTest` with
`-Dm3.wiring.applySuccessors=true` and the same canonical/receiver roots only when
materializing these retained intermediate inputs. Ordinary fresh-base replay uses
`ModuleWiringRecipeTest` directly.

Run an intermediate successor before the primary recipe's current-source replay.
The primary intentionally refuses an intermediate optional POM; it admits its
original absent input or its exact final output. `ModuleSuccessorRecipeTest`
also covers the optional module's transitive-version correction.

The real normal build exposed an unchanged baseline compilation defect in
`M3GvasLiteralHarvester.main`: a `String destination` and a later
`Path destination` occupied the same local scope. The separate
`ASTOrdinaryLocalScope20261017` Java snapshot recipe renames only the latter local
and its two uses. `local-scope-fix/SOURCE_PIN.json` binds the original Git blob and
both source hashes. Its two SDK tests include fixed-point and refusal checks;
the existing harvester tests also pass in the ordinary module. Staging and
no-clobber publication behavior are preserved.

Run `verify.py --help` for the normal-module proof parameters. It checks normal
Maven compilation/tests, exact compiler inputs, separate optional compilation,
native profile refusal/positive behavior, service discovery from the ordinary
Maven JAR, and the existing adversarial optional adapter fixture in both namespaces
with the qualified `jdk.compiler` patch. The fixture retains the same-task, exact
source, full-attribution, bytecode-oracle, lifecycle, and lazy source-completion
refusal checks. It also checks that the standalone OpenRewrite parser class was
never loaded, and that the core works without OpenRewrite on the classpath.

`--canonical-checkout /absolute/com.synexia` is optional. When supplied, the
verifier checks the actual canonical service descriptor and attempts the parent
reactor. A public receiver-only reproduction uses the actual canonical SDK
candidate for the three-provider preservation check and records the omitted
reactor as unavailable; it does not require private checkout paths.

## Qualification boundaries

`evidence/RESULTS.json` is authoritative for executed checks and their tool/input
hashes. A sparse canonical checkout may lack modules referenced by the exact pinned
parent reactor; the verifier records that as `UNAVAILABLE`, without claiming a full
canonical module/parent build. The receiver module build is an independent actual
Maven proof. Full JDK builds, jtreg/JCK, other platforms, performance improvements,
and hosted CI are not implied by this crate.

The completed AST-only proof compiled exactly 138 sources: 103 admitted vendor
sources, ten target AST/cache/hierarchy sources, and 25 ordinary sources including
the exact upstream `M3ASTLeafIndex`. It passed 52 normal JUnit tests and 54 tests
with the native profile, while the missing-library profile failed exactly the two
native tests. The upstream leaf self-test passed 86,095 checks. Both optional
namespaces passed the 841-assertion same-task proof against the final compiler
patch, and explicit M3ASTIndex discovery preserved reference class bytes.
The separately added Lexicon integration has its own 104-vendor POM successor;
this primary recipe and its 138-source receipt remain unchanged by that successor.

The leaf-first hierarchy used by the surrounding task is L0 per-file facts/atoms,
L1 file summary, L2 package, L3 module, L4 project, and L5 repository. This crate
wires the underlying frontend/cache into its module; higher-level summaries retain
their own owners and invalidation contracts.
