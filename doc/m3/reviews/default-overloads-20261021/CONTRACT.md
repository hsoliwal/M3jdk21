# M3JDK21: whole-SDK default-method and overload convergence

## Status and scope

User directive: default overloaded methods across the entire M3JDK.
Status: documented pass contract and bounded source inspection; NOT an implemented whole-SDK transformation. No product method, canonical recipe source or remote branch changed. This packet is not an executable OpenRewrite recipe.

Scope is every actual source module and relevant method family in M3JDK21, including java.base, other java.* and jdk.* modules, and M3-owned SDK modules. Module/type enumeration is still pending. Existing disabled/excluded paths remain inventoried exclusions, not mutation targets. Collections are one family, not the scope boundary.

## Intended transformation

1. Interface operations receive useful default overloads and derived operations that delegate to existing contractual primitives. Required abstract primitives and functional-interface contracts remain intact.
2. Classes receive ordinary concrete overloads; static utility owners retain static overloads. Do not apply the Java default modifier to class methods, constructors, native declarations or annotation elements.
3. Keep one implementation authority per semantic operation. Convenience overloads supply documented defaults or compose qualified operations. Preserve legacy dispatch; do not blindly redirect old overloads through newly overridable methods.
4. Preserve existing public names, descriptors, generic contracts, checked exceptions and observable behavior. Treat additions as explicit compatibility-gated API deltas, not as proof of compatibility.
5. Internal M3 storage, indices, offsets, masks and JNI remain concealed. No public M3-prefixed duplicate hierarchy, required Synexia runtime dependency, or universal wrapper/service is introduced.
6. Each default is documented: range, offset, limit, comparator, charset, monitor, cancellation/ownership or other operation-specific policy. Do not guess a default from a parameter type or generate every parameter combination.
7. Progress-aware overloads reuse M3JDK's existing java.util.concurrent.ProgressMonitor/ProgressMonitors. No com.synexia.job.IProgressMonitor dependency is imported into the product. A no-monitor legacy call must not acquire new cancellation, threading or progress behavior by accident.
8. Avoid no-op/null/unsupported-operation implementations invented merely to make an interface concrete. Existing documented optional-operation refusals remain preserved.

## Source inspection completed

M3JDK21 master was read at 5eeb841168ed420085223806fefcfca8d1b6b3ca.
- Runnable.java already declares default progressMonitor() and run(ProgressMonitor), and retains abstract run().
- Callable.java already declares default call(ProgressMonitor), and retains abstract call(). Git blob: 7cd9b5d7d538b4dffe426cacc9a03651f635e005.

Existing canonical inventory owner:
com.synexia.rewrite.M3AlgorithmProgressSurfaceInventoryRecipe
Path: synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3AlgorithmProgressSurfaceInventoryRecipe.java
Observed Git blob: 9517a667335e8ce7738a230a7dcce5bc17bc6500.
Original tests: synexia-openrewrite-recipes/src/test/java/com/synexia/rewrite/M3AlgorithmProgressSurfaceInventoryRecipeTest.java
Observed Git blob: f73e51a2ae3d657ec05341e4248f1cb5ef2bbf97.

These are static source findings, not an executed recipe result:
- The class-named boolean in shared ExecutionContext is set after the first compilation unit and suppresses subsequent units sharing that context.
- isPublicStatic accepts only explicit public static declarations; interface instance/default methods and implicit interface visibility are omitted.
- Type identification uses printed text containing iprogressmonitor; it does not recognize the actual M3JDK ProgressMonitor type and can mistake lexical text for semantic identity.
- withoutMonitor removes the last comma-split textual parameter even though detection accepts a monitor anywhere; generic commas and non-tail monitor parameters require typed handling.
- The two inspected tests assert only an empty source changeset, not complete or correct DataTable rows. A visitor that inventories nothing can satisfy those assertions.

Reuse and enhance this existing owner through its canonical Maven/OpenRewrite task and fixtures. Do not create another parser, inventory engine, runtime annotation framework, or competing catalogue.

## Required inventory successor

Use attributed method symbols, not regex/name similarity, to record:
module; source path and Git/SHA-256 identities; binary owner; owner kind; visibility; method name; JVM descriptor; generic signature; declared/inherited origin; abstract/default/static/concrete/native classification; overload family; selected delegate; default argument bindings; documented effects/throws/ownership; candidate recipe; proof status.

Traverse every admitted compilation unit and nested owner; retain inheritance/override context. Missing attribution is UNRESOLVED, not MISSING or PRESENT. Match monitor types by approved fully-qualified identity and parameter position. Compare generic/return/throws contracts separately from erased linkage identity. Deterministically sort emitted rows. Record all method families as EXISTING, CANDIDATE, EXCLUDED or UNRESOLVED; source presence is not behavioral qualification.

## Compatibility fixtures required before application

- Multiple compilation units and nested types; implicit-public interface methods; static, instance, abstract and default declarations; zero-argument and generic/varargs methods; source-order permutations; source byte preservation.
- Exact monitor identities, foreign same-simple-name types, parameter-name/comment decoys, non-tail monitor position, parameterized types containing commas, unresolved types and inherited overloads.
- Runnable/Callable and other SAM lambdas; method references; classes implementing several interfaces; default diamonds; accidental overrides; erasure clashes and covariant return/throws checks.
- null, lambda, method-reference, boxing, primitive widening, varargs and generic-inference overload-resolution probes. Compare resolved callees as well as successful compilation.
- Previously compiled clients and implementers executed against changed APIs, separately from recompiled-client checks.
- Callback order/count, exception identity/order, cancellation, monitor borrowing/cleanup, mutation/read-only policy, laziness and close/ownership behavior.
- Java/JNI header/export parity where touched; real applicable runtime tests. No blanket performance claim from a forwarding body or default modifier.

## Serial recipe execution contract

Inventory actual module/source closure -> document exact per-family delta -> qualify canonical recipe and frozen fixtures -> generate per-file diff -> lint -> compile -> tests -> runtime -> compare API/behavior evidence -> verify zero-change replay -> additive review/merge.

Canonical authoring belongs to com.synexia; emitted product code belongs to its existing M3JDK21 owner. Every failed/corrected recipe attempt appends correction memory. The existing source test suite must be strengthened rather than replaced with only no-change checks.

Whole-SDK completion requires a closed inventory and a disposition for every eligible family, with all required transformation/consumer gates executed. No current count or completion percentage is asserted.

## Evidence boundary

This turn performed connected source reads and a local toolchain availability inspection. The local dependency archive contains Maven and OpenRewrite core/java artifacts but the required parser/test execution closure has not been qualified; Maven Central retrieval failed at DNS resolution. No Maven, javac, JUnit, JNI, installed-JDK or jtreg gate was run for this task. Historical execution reports are not reused as current proof.
